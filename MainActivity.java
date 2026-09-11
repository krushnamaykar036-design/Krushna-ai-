package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
@Override
protected void onDestroy() {
    if (tts != null) {
        tts.stop();
        tts.shutdown();
    }

    super.onDestroy();
}
    TextView chat;
    EditText input;
    TextToSpeech tts;

    static final int VOICE = 10;
    static final int MIC_PERMISSION = 20;

    final String SERVER_URL =
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
            mic.setVisibility(View.VISIBLE);
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

    void sendMessage() {

        String message = input.getText()
                .toString()
                .trim();

        if (message.isEmpty()) {
            return;
        }

        if (handleCommand(message)) {
            input.setText("");
            return;
        }

        chat.append(
                "You: " + message + "\n\n"
        );

        chat.append(
                "Krushna AI: विचार करतोय...\n\n"
        );

        input.setText("");

        new Thread(() -> {

            String answer;

            try {
                answer = askServer(message);

                if (answer == null ||
                        answer.isEmpty()) {
                    answer =
                            "Server कडून उत्तर मिळाले नाही.";
                }

            } catch (Exception e) {

                answer =
                        "Internet किंवा Server उपलब्ध नाही.";
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

    boolean handleCommand(String message) {

        String text =
                message.toLowerCase(Locale.ROOT).trim();

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

        if (text.contains("camera") ||
                text.contains("कॅमेरा")) {

            try {

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

            } catch (Exception e) {

                speak("Camera उघडता आला नाही");
            }

            return true;
        }

        if (text.contains("call") ||
                text.contains("कॉल") ||
                text.contains("फोन")) {

            speak(
                    "Call command साठी contact feature पुढच्या version मध्ये जोडू."
            );

            return true;
        }

        if (text.startsWith("open ") ||
                text.startsWith("ओपन ") ||
                text.startsWith("उघड ")) {

            String appName =
                    text.replaceFirst(
                            "^(open|ओपन|उघड)\\s+",
                            ""
                    ).trim();

            if (!appName.isEmpty()) {
                openApp(appName);
            }

            return true;
        }

        return false;
    }

    void openApp(String appName) {

        android.content.pm.PackageManager pm =
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
                normalize(appName);

        for (android.content.pm.ResolveInfo info :
                apps) {

            String label =
                    info.loadLabel(pm).toString();

            if (normalize(label).equals(wanted)) {

                Intent launch =
                        pm.getLaunchIntentForPackage(
                                info.activityInfo.packageName
                        );

                if (launch != null) {

                    startActivity(launch);

                    speak(
                            label +
                                    " उघडत आहे"
                    );

                    return;
                }
            }
        }

        speak(
                appName +
                        " App सापडला नाही"
        );
    }

    String normalize(String value) {

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "");
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
                .trim();
    }

    void startVoice() {

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
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,
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
            String askServer(String question) throws Exception {

        URL url = new URL(SERVER_URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(40000);
        connection.setDoOutput(true);
        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        JSONObject body = new JSONObject();
        body.put("message", question);

        byte[] data = body.toString()
                .getBytes(StandardCharsets.UTF_8);

        OutputStream output =
                connection.getOutputStream();

        output.write(data);
        output.flush();
        output.close();

        int code = connection.getResponseCode();

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
                new JSONObject(response.toString());

        return result.optString("reply", "").trim();
    }

    void speak(String text) {

        if (tts != null &&
                text != null &&
                !text.isEmpty()) {

            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "KRUSHNA_AI"
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

        
                

        
