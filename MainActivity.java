
package com.example.aiassistant;

import android.app.Activity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String SERVER =
            "https://krushna-ai-he2j.onrender.com/chat";

    private TextToSpeech tts;
    private LinearLayout chatLayout;
    private EditText input;
    private Button voiceButton;
    private boolean voiceOn = false;

    private static final int MIC_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUI();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("mr", "IN"));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(new Locale("hi", "IN"));
                }
            }
        });

        checkMicPermission();
        handleIncomingIntent(getIntent());
    }

    private void buildUI() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12, 12, 12));
        root.setPadding(18, 18, 18, 18);

        TextView title = new TextView(this);
        title.setText("🔥 KRUSHNA AI");
        title.setTextColor(Color.RED);
        title.setTextSize(27);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("RED DEVIL • PREMIUM AI");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 5, 0, 15);
        root.addView(subtitle);

        voiceButton = new Button(this);
        voiceButton.setText("🎙 VOICE ASSISTANT OFF");
        root.addView(voiceButton);

        voiceButton.setOnClickListener(v -> toggleVoice());

        ScrollView scroll = new ScrollView(this);

        chatLayout = new LinearLayout(this);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        chatLayout.setPadding(5, 10, 5, 10);

        scroll.addView(chatLayout);

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setHint("Krushna AI ला विचारा...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setSingleLine(false);
        input.setBackgroundColor(Color.rgb(35, 35, 35));

        bottom.addView(input, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button mic = new Button(this);
        mic.setText("🎤");
        bottom.addView(mic);

        mic.setOnClickListener(v -> startMic());

        Button send = new Button(this);
        send.setText("SEND");
        bottom.addView(send);

        send.setOnClickListener(v -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                input.setText("");
                processMessage(message);
            }
        });

        root.addView(bottom);
        setContentView(root);
    }

    private void addMessage(String name, String message, boolean user) {

        TextView text = new TextView(this);
        text.setText(name + ": " + message);
        text.setTextColor(user ? Color.WHITE : Color.GREEN);
        text.setTextSize(16);
        text.setPadding(10, 10, 10, 10);

        chatLayout.addView(text);
    }

    private void processMessage(String message) {

        addMessage("You", message, true);

        String cmd = message.toLowerCase(Locale.ROOT).trim();

        if (cmd.equals("home") || cmd.equals("go home")) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
            return;
        }

        if (cmd.contains("call")) {
            Intent dial = new Intent(Intent.ACTION_DIAL);
            startActivity(dial);
            speak("Calling साठी Dialer उघडला आहे.");
            return;
        }

        if (cmd.contains("camera")) {
            openApp("com.android.camera");
            return;
        }

        if (cmd.contains("settings")) {
            startActivity(new Intent(
                    android.provider.Settings.ACTION_SETTINGS));
            return;
        }

        if (cmd.contains("whatsapp")) {
            openApp("com.whatsapp");
            return;
        }

        if (cmd.contains("instagram")) {
            openApp("com.instagram.android");
            return;
        }

        if (cmd.contains("youtube")) {
            openApp("com.google.android.youtube");
            return;
        }

        if (cmd.contains("chrome")) {
            openApp("com.android.chrome");
            return;
        }

        if (cmd.contains("facebook")) {
            openApp("com.facebook.katana");
            return;
        }

        if (cmd.contains("telegram")) {
            openApp("org.telegram.messenger");
            return;
        }

        if (cmd.contains("maps")) {
            openApp("com.google.android.apps.maps");
            return;
        }

        askOnlineAI(message);
    }

    private void openApp(String packageName) {

        try {

            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(packageName);

            if (launch != null) {
                startActivity(launch);
            } else {
                speak("हे App फोनमध्ये सापडले नाही.");
            }

        } catch (Exception e) {
            speak("App उघडता आले नाही.");
        }
    } 
    
private void askOnlineAI(String message) {

    addMessage("Krushna AI", "AI उत्तर तयार करत आहे...", false);

    new Thread(() -> {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(
                    "https://krushna-ai-he2j.onrender.com/chat");

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type", "application/json; charset=UTF-8");

            JSONObject request = new JSONObject();
            request.put("message", message);

            OutputStream output = connection.getOutputStream();
            output.write(request.toString().getBytes("UTF-8"));
            output.close();

            int status = connection.getResponseCode();

            java.io.InputStream stream;

            if (status >= 200 && status < 300) {
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
            }

            StringBuilder response = new StringBuilder();

            if (stream != null) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8"));

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
            }

            String result = response.toString();

            if (status >= 200 && status < 300) {

                JSONObject json = new JSONObject(result);

                String reply = json.optString("reply",
                        json.optString("response", ""));

                if (reply.isEmpty()) {
                    reply = "Server कडून उत्तर मिळाले नाही: " + result;
                }

                final String finalReply = reply;

                runOnUiThread(() -> {
                    addMessage("Krushna AI", finalReply, false);
                    speak(finalReply);
                });

            } else {

                final String error =
                        "HTTP Error " + status + ": " + result;

                runOnUiThread(() ->
                        addMessage("Server Error", error, false));
            }

        } catch (Exception e) {

            final String error = e.getClass().getSimpleName()
                    + ": " + e.getMessage();

            runOnUiThread(() ->
                    addMessage("Connection Error", error, false));

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }).start();
}
    

        

    private void speak(String message) {

        if (tts != null) {
            tts.speak(message,
                    TextToSpeech.QUEUE_FLUSH, null, "KRUSHNA_AI");
        }
    }

    private void startMic() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_REQUEST);
            return;
        }

        try {

            Intent intent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "mr-IN");

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    "mr-IN");

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Krushna AI शी बोला");

            startActivityForResult(intent, MIC_REQUEST);

        } catch (Exception e) {
            speak("Voice recognition उपलब्ध नाही.");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MIC_REQUEST &&
                resultCode == RESULT_OK && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                processMessage(results.get(0));
            }
        }
    }

    private void checkMicPermission() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_REQUEST);
        }
    }

    private Intent voiceServiceIntent() {

        return new Intent().setClassName(
                this,
                "com.example.aiassistant.HomeVoiceService");
    }

    private void toggleVoice() {

        try {

            if (!voiceOn) {

                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(voiceServiceIntent());
                } else {
                    startService(voiceServiceIntent());
                }

                voiceOn = true;
                voiceButton.setText("🔴 VOICE ASSISTANT ON");
                speak("Krushna AI Voice Assistant सुरू झाला.");

            } else {

                stopService(voiceServiceIntent());

                voiceOn = false;
                voiceButton.setText("🎙 VOICE ASSISTANT OFF");
                speak("Voice Assistant बंद झाला.");
            }

        } catch (Exception e) {
            speak("Voice Assistant सुरू झाला नाही.");
        }
    }

    private void handleIncomingIntent(Intent intent) {

        if (intent != null &&
                intent.hasExtra("KRUSHNA_MESSAGE")) {

            String message = intent.getStringExtra("KRUSHNA_MESSAGE");

            if (message != null && !message.isEmpty()) {
                processMessage(message);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
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

