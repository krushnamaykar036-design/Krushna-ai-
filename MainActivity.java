package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
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

        findViewById(R.id.send).setOnClickListener(v -> send());
        findViewById(R.id.mic).setOnClickListener(v -> voice());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("mr", "IN"));
            }
        });
    }

    void send() {

        String q = input.getText().toString().trim();

        if (q.isEmpty()) return;

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

                if (answer == null || answer.isEmpty()) {
                    answer = "Server कडून उत्तर मिळाले नाही.";
                }

            } catch (Exception e) {
                answer = "Internet/Server connection उपलब्ध नाही.";
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

    boolean handleCommand(String q) {

        String text =
                q.toLowerCase(Locale.ROOT).trim();

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

        if (text.contains("camera") ||
                text.contains("कॅमेरा")) {

            Intent camera =
                    new Intent(
                            android.provider.MediaStore
                                    .ACTION_IMAGE_CAPTURE
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

        if (text.contains("call") ||
                text.contains("कॉल") ||
                text.contains("फोन कर") ||
                text.contains("फोन लाव") ||
                text.contains("फोन करा") ||
                text.contains("कॉल कर")) {

            String name =
                    extractContactName(text);

            if (!name.isEmpty()) {
                findContactAndCall(name);
            } else {
                speak("कोणाला call करायचा ते सांगा");
            }

            return true;
        }

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

    String extractContactName(String text) {

        String result = text;

        String[] words = {

                "please",
                "call",
                "फोन",
                "कॉल",
                "कर",
                "करा",
                "करायचा",
                "करायचं",
                "लाव",
                "ला",
                "वर",
                "म्हणजे",
                "म्हणचे",
                "म्हणून",
                "कृपया"
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

        for (android.content.pm.ResolveInfo info : apps) {

            String label =
                    info.loadLabel(pm).toString();

            String packageName =
                    info.activityInfo.packageName;

            String normalizedLabel =
                    normalize(label);

            String normalizedPackage =
                    normalize(packageName);

            if (normalizedLabel.contains(wanted) ||
                    wanted.contains(normalizedLabel) ||
                    normalizedPackage.contains(wanted)) {

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
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "");
    }

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
            speak("Contacts मिळाले नाहीत");
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

            String contact =
                    normalizeContact(contactName);

            // पूर्ण नाव match
            if (contact.equals(wanted)) {

                foundName = contactName;
                foundNumber = number;
                break;
            }

            // नावाचा भाग match
            if (contact.contains(wanted) ||
                    wanted.contains(contact)) {

                foundName = contactName;
                foundNumber = number;
                break;
            }

            // प्रत्येक शब्द match
            String[] parts =
                    name.trim().split("\\s+");

            for (String part : parts) {

                String p =
                        normalizeContact(part);

                if (p.length() >= 2 &&
                        contact.contains(p)) {

                    foundName = contactName;
                    foundNumber = number;
                    break;
                }
            }

            if (foundNumber != null) {
                break;
            }
        }

        cursor.close();

        if (foundNumber != null) {

            pendingNumber = foundNumber;

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

            makeCall(foundNumber);
            return;
        }

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

    void makeCall(String number) {

        try {

            Intent call =
                    new Intent(
                            Intent.ACTION_CALL,
                            Uri.parse("tel:" + number)
                    );

            startActivity(call);

            speak("Call करत आहे");

        } catch (Exception e) {

            speak("Call करता आला नाही");
        }
    }

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
                                connection
                                        .getInputStream()
                        )
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine())
                != null) {

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
                .trim();
    }

    void voice() {

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION
            );

            return;
        }

        startVoice();
    }

    void startVoice() {

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
                VOICE
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == VOICE &&
                resultCode == RESULT_OK &&
                data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent
                                    .EXTRA_RESULTS
                    );

            if (results != null &&
                    !results.isEmpty()) {

                input.setText(
                        results.get(0)
                );

                send();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == MIC_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            startVoice();
        }

        if (requestCode == CALL_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            if (pendingNumber != null) {

                makeCall(pendingNumber);

                pendingNumber = null;
            }
        }

        if (requestCode == CONTACT_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            if (pendingContactName != null) {

                String name =
                        pendingContactName;

                pendingContactName = null;

                findContactAndCall(name);

            } else {

                speak(
                        "Contacts permission मिळाली"
                );
            }
        }
    }

    void speak(String text) {

        if (tts != null) {

            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "krushna_ai"
            );
        }
    }

    @Override
    protected void onDestroy() {

        if (tts != null) {

            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
            }
