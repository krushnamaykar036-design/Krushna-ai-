package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText input;
    private TextView chat;
    private TextToSpeech tts;

    private static final int VOICE = 10;
    private static final int MIC_PERMISSION = 20;
    private static final int CONTACT_PERMISSION = 30;

    private static final String SERVER_URL =
            "https://krushna-ai-hseh.onrender.com/chat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getResources().getIdentifier(
                "activity_main",
                "layout",
                getPackageName()
        ));

        input = findViewById(getResources().getIdentifier(
                "input",
                "id",
                getPackageName()
        ));

        chat = findViewById(getResources().getIdentifier(
                "chat",
                "id",
                getPackageName()
        ));

        Button mic = findViewById(getResources().getIdentifier(
                "mic",
                "id",
                getPackageName()
        ));

        Button send = findViewById(getResources().getIdentifier(
                "send",
                "id",
                getPackageName()
        ));

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        send.setOnClickListener(v -> sendMessage());
        mic.setOnClickListener(v -> startVoice());

        chat.setText(
                "Hello Krushna 👋\n" +
                "Krushna AI तयार आहे 🎙️"
        );
    }

    private void sendMessage() {

        String message = input.getText().toString().trim();

        if (message.isEmpty()) {
            return;
        }

        input.setText("");
        chat.append("\n\nYou: " + message);

        if (handleCommand(message)) {
            return;
        }

        askServer(message);
    }

    private boolean handleCommand(String text) {

        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.contains("call") ||
                lower.contains("कॉल") ||
                lower.contains("फोन कर") ||
                lower.contains("ला call") ||
                lower.contains("number") ||
                lower.contains("नंबर")) {

            findContactCommand(text);
            return true;
        }

        if (lower.contains("open") ||
                lower.contains("उघड") ||
                lower.contains("उघडा") ||
                lower.contains("चालू") ||
                lower.contains("start")) {

            return openInstalledApp(text);
        }

        if (lower.contains("settings") ||
                lower.contains("सेटिंग")) {

            openPackage("com.android.settings");
            return true;
        }

        if (lower.contains("whatsapp")) {

            openPackage("com.whatsapp");
            return true;
        }

        if (lower.contains("youtube")) {

            openPackage("com.google.android.youtube");
            return true;
        }

        if (lower.contains("chrome")) {

            openPackage("com.android.chrome");
            return true;
        }

        if (lower.contains("camera") ||
                lower.contains("कॅमेरा")) {

            try {

                startActivity(new Intent(
                        "android.media.action.IMAGE_CAPTURE"
                ));

                speak("Camera उघडत आहे");

            } catch (Exception e) {

                speak("Camera उघडता आला नाही");
            }

            return true;
        }

        if (lower.contains("phone") ||
                lower.contains("फोन") ||
                lower.contains("dialer")) {

            openDialer();
            return true;
        }

        return false;
    }

    private void findContactCommand(String text) {

        if (checkSelfPermission(
                Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_CONTACTS
                    },
                    CONTACT_PERMISSION
            );

            return;
        }

        String name = getContactName(text);

        if (name.isEmpty()) {

            speak("Contact चे नाव सांगा");
            return;
        }

        searchContact(name);
    }

    private String getContactName(String text) {

        String name = text.trim();

        String[] remove = {
                "call",
                "Call",
                "कॉल",
                "phone",
                "Phone",
                "फोन",
                "number",
                "Number",
                "नंबर",
                "contact",
                "Contact",
                "ला",
                "चा",
                "ची",
                "चे",
                "कर",
                "करा",
                "करायचा",
                "करायची",
                "द्या",
                "दे"
        };

        for (String word : remove) {

            name = name.replaceAll(
                    "(?i)(^|\\s)" +
                    java.util.regex.Pattern.quote(word) +
                    "(?=\\s|$)",
                    " "
            );
        }

        return name
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void searchContact(String name) {

        Cursor cursor = null;

        try {

            cursor = getContentResolver().query(

                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,

                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },

                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                            + " LIKE ?",

                    new String[]{
                            "%" + name + "%"
                    },

                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            );

            if (cursor != null && cursor.moveToFirst()) {

                String contactName = cursor.getString(0);
                String number = cursor.getString(1);

                chat.append(
                        "\n\nKrushna AI: " +
                        contactName +
                        " - " +
                        number
                );

                Intent dial = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + number)
                );

                startActivity(dial);

                speak(
                        contactName +
                        " चा नंबर Dialer मध्ये उघडत आहे"
                );

            } else {

                speak(
                        name +
                        " नावाचा contact सापडला नाही"
                );
            }

        } catch (Exception e) {

            speak("Contact search मध्ये समस्या आली");

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean openInstalledApp(String text) {

        String search = text.toLowerCase(Locale.ROOT);

        search = search.replace("open", "");
        search = search.replace("start", "");
        search = search.replace("app", "");
        search = search.replace("उघडा", "");
        search = search.replace("उघड", "");
        search = search.replace("चालू", "");

        search = search.trim();

        if (search.isEmpty()) {
            return false;
        }

        try {

            PackageManager pm = getPackageManager();

            ArrayList<android.content.pm.ApplicationInfo> apps =
                    new ArrayList<>(
                            pm.getInstalledApplications(
                                    PackageManager.GET_META_DATA
                            )
                    );

            for (android.content.pm.ApplicationInfo app : apps) {

                Intent launch =
                        pm.getLaunchIntentForPackage(
                                app.packageName
                        );

                if (launch == null) {
                    continue;
                }

                String appName =
                        pm.getApplicationLabel(app)
                                .toString()
                                .toLowerCase(Locale.ROOT);

                if (appName.equals(search) ||
                        appName.contains(search) ||
                        search.contains(appName)) {

                    startActivity(launch);

                    speak(
                            pm.getApplicationLabel(app)
                                    .toString() +
                            " उघडत आहे"
                    );

                    return true;
                }
            }

        } catch (Exception e) {

            return false;
        }

        speak("App सापडला नाही");
        return true;
    }

    private void openPackage(String packageName) {

        try {

            Intent intent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    packageName
                            );

            if (intent != null) {

                startActivity(intent);
                speak("App उघडत आहे");

            } else {

                speak("App सापडला नाही");
            }

        } catch (Exception e) {

            speak("App उघडता आला नाही");
        }
    }

    private void openDialer() {

        try {

            startActivity(
                    new Intent(Intent.ACTION_DIAL)
            );

            speak("Phone उघडत आहे");

        } catch (Exception e) {

            speak("Phone उघडता आला नाही");
        }
    }

    private void askServer(String message) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(SERVER_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();

                body.put("message", message);

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        body.toString()
                                .getBytes("UTF-8")
                );

                output.close();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection
                                                .getInputStream()
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                JSONObject response =
                        new JSONObject(
                                result.toString()
                        );

                String reply =
                        response.optString(
                                "reply",
                                "उत्तर मिळाले नाही."
                        );

                runOnUiThread(() -> {

                    chat.append(
                            "\n\nKrushna AI: " +
                            reply
                    );

                    speak(reply);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    chat.append(
                            "\n\nKrushna AI: " +
                            "Server connect झाला नाही."
                    );

                    speak(
                            "Server connect झाला नाही"
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void startVoice() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION
            );

            return;
        }

        try {

            Intent intent =
                    new Intent(
                            RecognizerIntent
                                    .ACTION_RECOGNIZE_SPEECH
                    );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                            .LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "mr-IN"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Krushna AI ला बोला"
            );

            startActivityForResult(
                    intent,
                    VOICE
            );

        } catch (Exception e) {

            speak("Voice input उपलब्ध नाही");
        }
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
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null &&
                    !results.isEmpty()) {

                input.setText(
                        results.get(0)
                );

                sendMessage();
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

        if (requestCode == CONTACT_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            speak("Contacts permission मिळाली");
        }
    }

    private void speak(String text) {

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
