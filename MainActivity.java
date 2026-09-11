package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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

    private TextView chat;
    private EditText input;
    private TextToSpeech tts;

    private static final int VOICE = 10;
    private static final int MIC_PERMISSION = 20;

    private static final String SERVER_URL =
            "https://krushna-ai-hseh.onrender.com/chat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        chat = findViewById(R.id.chat);
        input = findViewById(R.id.input);

        View mic = findViewById(R.id.mic);
        View send = findViewById(R.id.send);

        if (mic != null) {
            mic.setOnClickListener(v -> startVoice());
        }

        if (send != null) {
            send.setOnClickListener(v -> sendMessage());
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
    }

    private void sendMessage() {

        String message = input.getText()
                .toString()
                .trim();

        if (message.isEmpty()) {
            return;
        }

        input.setText("");

        if (handleCommand(message)) {
            return;
        }

        chat.append(
                "You: " + message + "\n\n"
        );

        chat.append(
                "Krushna AI: विचार करतोय...\n\n"
        );

        new Thread(() -> {

            String answer;

            try {

                answer = askServer(message);

                if (answer == null ||
                        answer.trim().isEmpty()) {

                    answer =
                            "Server कडून उत्तर मिळाले नाही.";
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

    private boolean handleCommand(String message) {

        String text = message
                .toLowerCase(Locale.ROOT)
                .trim();

        // SETTINGS
        if (text.contains("settings") ||
                text.contains("setting") ||
                text.contains("सेटिंग") ||
                text.contains("सेटिंग्स")) {

            try {

                startActivity(
                        new Intent(Settings.ACTION_SETTINGS)
                );

                speak("Settings उघडत आहे");

            } catch (Exception e) {

                speak("Settings उघडता आली नाही");
            }

            return true;
        }

        // CAMERA
        if (text.contains("camera") ||
                text.contains("कॅमेरा")) {

            try {

                Intent intent = new Intent(
                        "android.media.action.IMAGE_CAPTURE"
                );

                startActivity(intent);

                speak("Camera उघडत आहे");

            } catch (Exception e) {

                speak("Camera उघडता आली नाही");
            }

            return true;
        }

        // WHATSAPP
        if (text.contains("whatsapp") ||
                text.contains("व्हाट्सअप") ||
                text.contains("व्हॉट्सअॅप")) {

            if (openPackage("com.whatsapp")) {
                speak("WhatsApp उघडत आहे");
            } else {
                speak("WhatsApp फोनमध्ये नाही");
            }

            return true;
        }

        // YOUTUBE
        if (text.contains("youtube") ||
                text.contains("यूट्यूब") ||
                text.contains("युट्युब")) {

            if (openPackage(
                    "com.google.android.youtube")) {

                speak("YouTube उघडत आहे");

            } else {

                speak("YouTube फोनमध्ये नाही");
            }

            return true;
        }

        // CHROME
        if (text.contains("chrome") ||
                text.contains("क्रोम")) {

            if (openPackage("com.android.chrome")) {

                speak("Chrome उघडत आहे");

            } else {

                speak("Chrome फोनमध्ये नाही");
            }

            return true;
        }

        // PHONE
        if (text.contains("phone") ||
                text.contains("फोन")) {

            try {

                Intent intent =
                        new Intent(Intent.ACTION_DIAL);

                startActivity(intent);

                speak("Phone उघडत आहे");

            } catch (Exception e) {

                speak("Phone उघडता आला नाही");
            }

            return true;
        }

        // MESSAGES
        if (text.contains("messages") ||
                text.contains("message") ||
                text.contains("sms") ||
                text.contains("मेसेज") ||
                text.contains("संदेश")) {

            try {

                Intent intent =
                        new Intent(Intent.ACTION_MAIN);

                intent.addCategory(
                        Intent.CATEGORY_APP_MESSAGING
                );

                startActivity(intent);

                speak("Messages उघडत आहे");

            } catch (Exception e) {

                speak("Messages उघडता आले नाही");
            }

            return true;
        }

        // GOOGLE MAPS
        if (text.contains("maps") ||
                text.contains("map") ||
                text.contains("google maps") ||
                text.contains("नकाशा")) {

            try {

                if (!openPackage(
                        "com.google.android.apps.maps")) {

                    Intent intent =
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "https://www.google.com/maps"
                                    )
                            );

                    startActivity(intent);
                }

                speak("Maps उघडत आहे");

            } catch (Exception e) {

                speak("Maps उघडता आले नाही");
            }

            return true;
        }

        // INSTALLED APP SEARCH
        if (text.contains("open") ||
                text.contains("उघड") ||
                text.contains("चालू") ||
                text.contains("start")) {

            String appName = text
                    .replace("open", "")
                    .replace("उघड", "")
                    .replace("चालू", "")
                    .replace("start", "")
                    .trim();

            if (!appName.isEmpty()) {

                if (openInstalledApp(appName)) {
                    return true;
                }

                speak("हा app सापडला नाही");
                return true;
            }
        }

        // ALSO TRY APP SEARCH FOR SHORT COMMANDS
        if (openInstalledApp(text)) {
            return true;
        }

        return false;
    }

    private boolean openPackage(String packageName) {

        try {

            Intent intent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    packageName
                            );

            if (intent != null) {

                startActivity(intent);
                return true;
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean openInstalledApp(String requestedName) {

        try {

            PackageManager pm =
                    getPackageManager();

            List<ApplicationInfo> apps =
                    pm.getInstalledApplications(
                            PackageManager.GET_META_DATA
                    );

            String wanted =
                    cleanAppName(requestedName);

            for (ApplicationInfo app : apps) {

                Intent launchIntent =
                        pm.getLaunchIntentForPackage(
                                app.packageName
                        );

                if (launchIntent == null) {
                    continue;
                }

                String label =
                        pm.getApplicationLabel(app)
                                .toString();

                String cleanLabel =
                        cleanAppName(label);

                if (cleanLabel.equals(wanted) ||
                        cleanLabel.contains(wanted) ||
                        wanted.contains(cleanLabel)) {

                    startActivity(launchIntent);

                    speak(label + " उघडत आहे");

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private String cleanAppName(String name) {

        if (name == null) {
            return "";
        }

        return name
                .toLowerCase(Locale.ROOT)
                .replace("app", "")
                .replace("उघड", "")
                .replace("चालू", "")
                .replace("open", "")
                .trim();
    }

    private String askServer(String question)
            throws Exception {

        URL url = new URL(SERVER_URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(40000);
        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        JSONObject request =
                new JSONObject();

        request.put(
                "message",
                question
        );

        byte[] data =
                request.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        OutputStream output =
                connection.getOutputStream();

        output.write(data);
        output.flush();
        output.close();

        int responseCode =
                connection.getResponseCode();

        if (responseCode < 200 ||
                responseCode >= 300) {

            connection.disconnect();

            return null;
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(),
                                StandardCharsets.UTF_8
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
                .optString("reply", "")
                .trim();
    }

    private void startVoice() {

        if (checkSelfPermission(
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

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
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

        if (requestCode != VOICE ||
                resultCode != RESULT_OK ||
                data == null) {

            return;
        }

        ArrayList<String> results =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (results == null ||
                results.isEmpty()) {

            return;
        }

        String spoken =
                results.get(0).trim();

        input.setText(spoken);

        sendMessage();
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

        if (requestCode == MIC_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startVoice();

            } else {

                speak(
                        "Microphone permission दिली नाही"
                );
            }
        }
    }

    private void speak(String text) {

        if (tts == null ||
                text == null ||
                text.isEmpty()) {

            return;
        }

        tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "KRUSHNA_AI"
        );
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
