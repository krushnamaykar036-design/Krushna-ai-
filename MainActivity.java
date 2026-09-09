package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView chat;
    EditText input;
    TextToSpeech tts;

    final int VOICE = 10;
    final int MIC_PERMISSION = 20;

    SharedPreferences settings;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        chat = findViewById(R.id.chat);
        input = findViewById(R.id.input);

        findViewById(R.id.send).setOnClickListener(v -> send());
        findViewById(R.id.mic).setOnClickListener(v -> voice());

        // Mic वर long-press = Online AI settings
        findViewById(R.id.mic).setOnLongClickListener(v -> {
            showSettings();
            return true;
        });

        settings = getSharedPreferences("krishna_ai", MODE_PRIVATE);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("mr", "IN"));

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });
    }

    void send() {

        String q = input.getText().toString().trim();

        if (q.isEmpty()) return;

        chat.append("You: " + q + "\n");
        chat.append("Krishna AI: विचार करतोय...\n\n");

        input.setText("");

        new Thread(() -> {

            String answer;

            try {
                answer = onlineReply(q);

                if (answer == null || answer.trim().isEmpty()) {
                    answer = getOfflineReply(q);
                }

            } catch (Exception e) {
                answer = getOfflineReply(q);
            }

            String finalAnswer = answer;

            runOnUiThread(() -> {
                chat.append("Krishna AI: " + finalAnswer + "\n\n");
                speak(finalAnswer);
            });

        }).start();
    }

    String onlineReply(String question) throws Exception {

        String apiKey = settings.getString("api_key", "").trim();
        String model = settings.getString(
                "model",
                "gpt-4o-mini"
        ).trim();

        String baseUrl = settings.getString(
                "url",
                "https://api.openai.com/v1/chat/completions"
        ).trim();

        if (apiKey.isEmpty()) {
            return null;
        }

        URL url = new URL(baseUrl);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        connection.setRequestProperty(
                "Authorization",
                "Bearer " + apiKey
        );

        JSONObject body = new JSONObject();

        body.put("model", model);

        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put(
                "role",
                "system"
        );
        system.put(
                "content",
                "You are Krishna AI, a helpful assistant. " +
                "Reply in the same language as the user. " +
                "Support Marathi, Hindi and English. " +
                "Keep answers clear and useful."
        );

        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", question);

        messages.put(user);

        body.put("messages", messages);

        byte[] data =
                body.toString().getBytes(StandardCharsets.UTF_8);

        OutputStream output = connection.getOutputStream();
        output.write(data);
        output.flush();
        output.close();

        int responseCode = connection.getResponseCode();

        InputStream stream;

        if (responseCode >= 200 && responseCode < 300) {
            stream = connection.getInputStream();
        } else {
            stream = connection.getErrorStream();
        }

        if (stream == null) {
            connection.disconnect();
            return null;
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(stream)
                );

        StringBuilder response = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            return null;
        }

        JSONObject result =
                new JSONObject(response.toString());

        JSONArray choices =
                result.getJSONArray("choices");

        if (choices.length() == 0) {
            return null;
        }

        JSONObject first =
                choices.getJSONObject(0);

        JSONObject message =
                first.getJSONObject("message");

        return message.getString("content").trim();
    }

    String getOfflineReply(String text) {

        String q =
                text.toLowerCase(Locale.ROOT).trim();

        if (q.isEmpty())
            return "काहीतरी बोल किंवा लिही.";

        if (q.contains("नमस्कार") ||
                q.contains("hello") ||
                q.equals("hi"))
            return "नमस्कार bro! मी Krishna AI आहे. 😊";

        if (q.contains("तुझं नाव") ||
                q.contains("तुझे नाव") ||
                q.contains("your name"))
            return "माझं नाव Krishna AI आहे. 🤖";

        if (q.contains("कसा आहेस") ||
                q.contains("कशी आहेस") ||
                q.contains("how are you"))
            return "मी मस्त आहे bro! 😎";

        if (q.contains("अभ्यास") ||
                q.contains("study"))
            return "रोज थोडा-थोडा अभ्यास कर आणि एका विषयावर लक्ष दे. 📚";

        if (q.contains("धन्यवाद") ||
                q.contains("thanks"))
            return "Welcome bro! 😊";

        if (q.contains("वेळ") ||
                q.contains("time"))
            return "आत्ता " +
                    new java.text.SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    ).format(new Date());

        if (q.contains("तारीख") ||
                q.contains("date"))
            return "आज " +
                    new java.text.SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale.getDefault()
                    ).format(new Date());

        return "Internet किंवा Online AI उपलब्ध नसल्यामुळे मी सध्या offline mode मध्ये आहे.";
    }

    void showSettings() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding = 30;
        layout.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        EditText url = new EditText(this);
        url.setHint("AI API URL");
        url.setText(
                settings.getString(
                        "url",
                        "https://api.openai.com/v1/chat/completions"
                )
        );

        EditText model = new EditText(this);
        model.setHint("Model");
        model.setText(
                settings.getString(
                        "model",
                        "gpt-4o-mini"
                )
        );

        EditText key = new EditText(this);
        key.setHint("API Key");
        key.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        key.setText(
                settings.getString(
                        "api_key",
                        ""
                )
        );

        layout.addView(url);
        layout.addView(model);
        layout.addView(key);

        new AlertDialog.Builder(this)
                .setTitle("Krishna AI Online Settings")
                .setMessage(
                        "API key फक्त फोनमध्ये सेट कर. GitHub वर share करू नको."
                )
                .setView(layout)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Save",
                        (dialog, which) -> {

                            settings.edit()
                                    .putString(
                                            "url",
                                            url.getText()
                                                    .toString()
                                                    .trim()
                                    )
                                    .putString(
                                            "model",
                                            model.getText()
                                                    .toString()
                                                    .trim()
                                    )
                                    .putString(
                                            "api_key",
                                            key.getText()
                                                    .toString()
                                                    .trim()
                                    )
                                    .apply();

                            Toast.makeText(
                                    this,
                                    "Settings saved ✅",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
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

        startActivityForResult(
                i,
                VOICE
        );
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
                    "krishna_ai"
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
