package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.view.Gravity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView chat;
    EditText input;
    TextToSpeech tts;

    final int VOICE = 10;
    final int MIC_PERMISSION = 20;

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

        // Krushna AI Logo
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.krushna_ai_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        int size = (int) (120 * getResources().getDisplayMetrics().density);

        FrameLayout.LayoutParams logoParams =
                new FrameLayout.LayoutParams(size, size);

        logoParams.gravity =
                Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        logoParams.topMargin =
                (int) (12 * getResources().getDisplayMetrics().density);

        addContentView(logo, logoParams);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("mr", "IN"));
            }
        });
    }

    void send() {
        String q = input.getText().toString().trim();

        if (q.isEmpty()) return;

        chat.append("You: " + q + "\n");
        chat.append("Krushna AI: विचार करतोय...\n\n");
        input.setText("");

        new Thread(() -> {
            String answer;

            try {
                answer = askServer(q);

                if (answer == null || answer.trim().isEmpty()) {
                    answer = "Server कडून उत्तर मिळाले नाही.";
                }

            } catch (Exception e) {
                answer = "Internet/Server connection उपलब्ध नाही.";
            }

            String finalAnswer = answer;

            runOnUiThread(() -> {
                chat.append("Krushna AI: " + finalAnswer + "\n\n");
                speak(finalAnswer);
            });

        }).start();
    }

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

        byte[] data =
                body.toString().getBytes(StandardCharsets.UTF_8);

        OutputStream output = connection.getOutputStream();
        output.write(data);
        output.flush();
        output.close();

        int responseCode = connection.getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            connection.disconnect();
            return null;
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()
                        )
                );

        StringBuilder response = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        JSONObject result =
                new JSONObject(response.toString());

        return result
                .optString("reply", "")
                .trim();
    }

    void voice() {

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
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

        startVoice();
    }

    void startVoice() {

        Intent i =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        startActivityForResult(i, VOICE);
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

                input.setText(results.get(0));
                send();
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
