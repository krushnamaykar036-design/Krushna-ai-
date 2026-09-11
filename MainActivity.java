package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView chat;
    EditText input;
    TextToSpeech tts;

    static final int VOICE = 10;
    static final int CONFIRM_VOICE = 11;
    static final int WAKE_VOICE = 12;

    static final int MIC_PERMISSION = 20;
    static final int CALL_PERMISSION = 30;
    static final int CONTACT_PERMISSION = 40;

    String pendingNumber = null;
    String pendingContactName = null;

    final String SERVER_URL =
            "https://krushna-ai-hseh.onrender.com/chat";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.activity_main);

        chat = findViewById(R.id.chat);
        input = findViewById(R.id.input);

        View micButton = findViewById(R.id.mic);

        if (micButton != null) {
            micButton.setVisibility(View.GONE);
        }

        View sendButton = findViewById(R.id.send);

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> send());
        }

        tts = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int result = tts.setLanguage(
                        new Locale("mr", "IN")
                );

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        new Handler().postDelayed(
                () -> startWakeListening(),
                1000
        );
    }

    // =========================
    // SEND
    // =========================

    void send() {

        String q = input.getText()
                .toString()
                .trim();

        if (q.isEmpty()) {
            return;
        }

        if (handleCommand(q)) {
            input.setText("");
            return;
        }

        chat.append("You: " + q + "\n");
        chat.append("Krushna AI: विचार करतोय...\n\n");

        input.setText("");

        new Thread(() -> {

            String answer;

            try {

                answer = askServer(q);

                if (answer == null ||
                        answer.isEmpty()) {

                    answer = "Server कडून उत्तर मिळाले नाही.";
                }

            } catch (Exception e) {

                answer =
                        "Internet किंवा Server connection उपलब्ध नाही.";
            }

            String finalAnswer = answer;

            runOnUiThread(() -> {

                chat.append(
                        "Krushna AI: " +
                                finalAnswer +
                                "\n\n"
                );

                speak(finalAnswer);
            });

        }).start();
    }

    // =========================
    // COMMAND HANDLER
    // =========================

    boolean handleCommand(String q) {

        String text = q.toLowerCase(Locale.ROOT)
                .trim();

        if (pendingNumber != null) {

            if (isYes(text)) {
                confirmCallNow();
                return true;
            }

            if (isNo(text)) {
                cancelCall();
                return true;
            }
        }

        // SETTINGS

        if (text.contains("settings") ||
                text.contains("setting") ||
                text.contains("सेटिंग") ||
                text.contains("सेटिंग्स")) {

            startActivity(
                    new Intent(Settings.ACTION_SETTINGS)
            );

            speak("Settings उघडत आहे");

            return true;
        }

        // CAMERA

        if (text.contains("camera") ||
                text.contains("कॅमेरा")) {

            Intent camera =
                    new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE
                    );

            if (camera.resolveActivity(
                    getPackageManager()) != null) {

                startActivity(camera);

                speak("Camera उघडत आहे");

            } else {

                speak("Camera सापडला नाही");
            }

            return true;
        }

        // CALL

        if (isCallCommand(text)) {

            String name =
                    extractContactName(text);

            if (!name.isEmpty()) {

                findContactAndCall(name);

            } else {

                speak("कोणाला call करायचा ते सांगा");
            }

            return true;
        }

        // OPEN APP

        if (text.contains("open") ||
                text.contains("ओपन") ||
                text.contains("उघड")) {

            String appName =
                    extractAppName(text);

            if (!appName.isEmpty()) {

                return openInstalledApp(appName);
            }
        }

        return false;
    }

    // =========================
    // CALL COMMAND
    // =========================

    boolean isCallCommand(String text) {

        return text.contains("call") ||
                text.contains("कॉल") ||
                text.contains("फोन") ||
                text.contains("फोन कर") ||
                text.contains("फोन लाव") ||
                text.contains("कॉल कर");
    }

    String extractContactName(String text) {

        String result = text;

        String[] removeWords = {

                "please",
                "call",
                "कॉल",
                "फोन",
                "कर",
                "करा",
                "करायचा",
                "करायचं",
                "करायचे",
                "करायचंय",
                "लाव",
                "लावा",
                "ला",
                "मला",
                "नंबर",
                "number"
        };

        for (String word : removeWords) {

            result = result.replace(word, " ");
        }

        return result
                .replaceAll("\\s+", " ")
                .trim();
    }

    // =========================
    // APP OPEN
    // =========================

    String extractAppName(String text) {

        String result = text;

        String[] words = {

                "please",
                "open",
                "app",
                "ओपन",
                "उघड",
                "कर",
                "करा",
                "अॅप",
                "ऍप"
        };

        for (String word : words) {

            result = result.replace(word, " ");
        }

        return result
                .replaceAll("\\s+", " ")
                .trim();
    }

    boolean openInstalledApp(String requested) {

        PackageManager pm =
                getPackageManager();

        Intent launcher =
                new Intent(
                        Intent.ACTION_MAIN,
                        null
                );

        launcher.addCategory(
                Intent.CATEGORY_LAUNCHER
        );

        List<android.content.pm.ResolveInfo> apps =
                pm.queryIntentActivities(
                        launcher,
                        0
                );

        String wanted =
                normalize(requested);

        for (android.content.pm.ResolveInfo info :
                apps) {

            String label =
                    info.loadLabel(pm).toString();

            String packageName =
                    info.activityInfo.packageName;

            String normalizedLabel =
                    normalize(label);

            String normalizedPackage =
                    normalize(packageName);

            if (normalizedLabel.equals(wanted) ||
                    normalizedPackage.equals(wanted)) {

                Intent launch =
                        pm.getLaunchIntentForPackage(
                                packageName
                        );

                if (launch != null) {

                    startActivity(launch);

                    speak(
                            label +
                                    " उघडत आहे"
                    );

                    return true;
                }
            }
        }

        speak(
                "हा App सापडला नाही: " +
                        requested
        );

        return true;
    }

    String normalize(String value) {

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "");
    }

    // =========================
    // CONTACT SEARCH
    // =========================

    void findContactAndCall(String name) {

        if (checkSelfPermission(
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            pendingContactName = name;

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_CONTACTS
                    },
                    CONTACT_PERMISSION
            );

            speak(
                    "Contacts permission आवश्यक आहे"
            );

            return;
        }

        Cursor cursor =
                getContentResolver().query(

                        ContactsContract
                                .CommonDataKinds.Phone
                                .CONTENT_URI,

                        new String[]{

                                ContactsContract
                                        .CommonDataKinds.Phone
                                        .NUMBER,

                                ContactsContract
                                        .CommonDataKinds.Phone
                                        .DISPLAY_NAME
                        },

                        null,
                        null,

                        ContactsContract
                                .CommonDataKinds.Phone
                                .DISPLAY_NAME +
                                " ASC"
                );

        if (cursor == null) {

            speak(
                    "Contacts मिळाले नाहीत"
            );

            return;
        }

        String wanted =
                normalizeContact(name);

        String foundNumber = null;
        String foundName = null;

        while (cursor.moveToNext()) {

            String contactName =
                    cursor.getString(1);

            String number =
                    cursor.getString(0);

            if (contactName == null ||
                    number == null ||
                    number.isEmpty()) {

                continue;
            }

            String normalizedContact =
                    normalizeContact(contactName);

            if (normalizedContact.equals(wanted)) {

                foundName = contactName;
                foundNumber = number;

                break;
            }

            String wantedNumber =
                    normalizePhone(name);

            String savedNumber =
                    normalizePhone(number);

            if (!wantedNumber.isEmpty() &&
                    savedNumber.endsWith(wantedNumber)) {

                foundName = contactName;
                foundNumber = number;

                break;
            }
        }

        cursor.close();

        if (foundNumber != null) {

            pendingNumber = foundNumber;
            pendingContactName = foundName;

            askCallConfirmation(foundName);

            return;
        }

        chat.append(
                "Krushna AI: Contact सापडला नाही: " +
                        name +
                        "\n\n"
        );

        speak(
                name +
                        " contact सापडला नाही"
        );
    }

    String normalizeContact(String value) {

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9\\u0900-\\u097F]",
                        ""
                );
    }

    String normalizePhone(String value) {

        return value.replaceAll(
                "[^0-9]",
                ""
        );
    }

    // =========================
    // CALL CONFIRMATION
    // =========================

    void askCallConfirmation(String contactName) {

        String message =
                contactName +
                        " ला call करायचा का?";

        chat.append(
                "Krushna AI: " +
                        message +
                        "\n\n"
        );

        speak(message);

        new Handler().postDelayed(
                () -> startConfirmationVoice(),
                1800
        );
    }

    void startConfirmationVoice() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Intent i =
                new Intent(
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );

        i.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,

                RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
        );

        i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "mr-IN"
        );

        startActivityForResult(
                i,
                CONFIRM_VOICE
        );
    }

    boolean isYes(String text) {

        return text.equals("हो") ||
                text.equals("होय") ||
                text.equals("हा") ||
                text.equals("हो हो") ||
                text.equals("yes") ||
                text.equals("yes please") ||
                text.equals("ha") ||
                text.equals("haa");
    }

    boolean isNo(String text) {

        return text.equals("नाही") ||
                text.equals("नको") ||
                text.equals("नकोय") ||
                text.equals("no") ||
                text.equals("nahi") ||
                text.equals("nahin");
    }

    void confirmCallNow() {

        if (pendingNumber == null) {
            return;
        }

        String number =
                pendingNumber;

        if (checkSelfPermission(
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.CALL_PHONE
                    },
                    CALL_PERMISSION
            );

            return;
        }

        pendingNumber = null;
        pendingContactName = null;

        makeCall(number);
    }

    void cancelCall() {

        pendingNumber = null;
        pendingContactName = null;

        chat.append(
                "Krushna AI: Call cancel केला.\n\n"
        );

        speak("Call cancel केला");
    }

    void makeCall(String number) {

        try {

            Intent call =
                    new Intent(
                            Intent.ACTION_CALL,
                            Uri.parse(
                                    "tel:" + number
                            )
                    );

            startActivity(call);

        } catch (Exception e) {

            speak(
                    "Call करता आला नाही"
            );
        }
    }

    // =========================
    // AI SERVER
    // =========================

    String askServer(String question)
            throws Exception {

        URL url =
                new URL(SERVER_URL);

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setRequestMethod("POST");

        connection.setConnectTimeout(20000);
        connection.setReadTimeout(40000);

        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        JSONObject body =
                new JSONObject();

        body.put(
                "message",
                question
        );

        byte[] data =
                body.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        OutputStream output =
               connection.getOutputStream();

output.write(data);
output.flush();
output.close();

int code =
        connection.getResponseCode();

if (code < 200 || code >= 300) {
    connection.disconnect();
    return null;
}

BufferedReader reader =
        new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()
                )
        );

StringBuilder response =
        new StringBuilder();

String line;

while ((line = reader.readLine()) != null) {
    response.append(line);
}

reader.close();

connection.disconnect();

JSONObject result =
        new JSONObject(
                response.toString()
        );

return result
        .optString(
                "reply",
                ""
        )
        .trim();} 
        @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
                    

