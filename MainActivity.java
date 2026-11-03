
package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int VOICE = 1001;
    private static final int MIC = 1002;

    private TextView status, chat;
    private EditText input;
    private TextToSpeech tts;

    private static final String SERVER =
        "https://krushna-ai-he2j.onrender.com/chat";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        buildUI();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("mr", "IN"));
            }
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, MIC
            );
        }

        status.setText("KRUSHNA AI ONLINE");
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(int n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(18), dp(15), dp(18), dp(15));
        root.setBackgroundColor(Color.rgb(10, 5, 8));

        TextView title = text("🔥 KRUSHNA AI", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.RED);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(65)));

        TextView subtitle = text("RED DEVIL EDITION", 14);
        subtitle.setTextColor(Color.LTGRAY);
        root.addView(subtitle);

        status = text("INITIALIZING...", 14);
        status.setTextColor(Color.RED);
        root.addView(status,
            new LinearLayout.LayoutParams(-1, dp(45)));

        TextView micButton = text("🎙", 65);
        micButton.setBackground(bg(Color.rgb(120, 0, 20), 100));
        LinearLayout.LayoutParams mp =
            new LinearLayout.LayoutParams(dp(155), dp(155));
        mp.gravity = Gravity.CENTER;
        mp.setMargins(0, dp(18), 0, dp(18));
        root.addView(micButton, mp);

        micButton.setOnClickListener(v -> startVoice());

        TextView hint = text("TAP TO SPEAK", 13);
        hint.setTextColor(Color.LTGRAY);
        root.addView(hint);

        ScrollView scroll = new ScrollView(this);
        chat = new TextView(this);
        chat.setTextColor(Color.WHITE);
        chat.setTextSize(16);
        chat.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(chat);

        LinearLayout.LayoutParams sp =
            new LinearLayout.LayoutParams(-1, 0, 1);
        sp.setMargins(0, dp(12), 0, dp(12));
        root.addView(scroll, sp);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Ask Krushna AI...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(bg(Color.rgb(35, 20, 25), 15));

        root.addView(input,
            new LinearLayout.LayoutParams(-1, dp(52)));

        Button send = new Button(this);
        send.setText("SEND MESSAGE");
        send.setTextColor(Color.WHITE);
        send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(180, 0, 30)));

        root.addView(send,
            new LinearLayout.LayoutParams(-1, dp(52)));

        send.setOnClickListener(v -> {
            String q = input.getText().toString().trim();
            if (!q.isEmpty()) {
                input.setText("");
                handleCommand(q);
            }
        });

        setContentView(root);
    }

    private void startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, MIC);
            return;
        }

        try {
            Intent i = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "mr-IN");
            i.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak to Krushna AI");

            status.setText("LISTENING...");
            startActivityForResult(i, VOICE);
        } catch (Exception e) {
            reply("Voice input unavailable.");
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        if (request == VOICE && result == RESULT_OK && data != null) {
            ArrayList<String> list =
                data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (list != null && !list.isEmpty()) {
                handleCommand(list.get(0));
            }
        }
        status.setText("KRUSHNA AI READY");
    }

    private void handleCommand(String command) {
        String q = command.toLowerCase(Locale.ROOT).trim();

        addChat("YOU: " + command);

        if (q.contains("youtube")) {
            openUrl("https://www.youtube.com");
            return;
        }

        if (q.contains("google")) {
            openUrl("https://www.google.com");
            return;
        }

        if (q.contains("whatsapp")) {
            openApp("com.whatsapp", "WhatsApp");
            return;
        }

        if (q.contains("instagram")) {
            openApp("com.instagram.android", "Instagram");
            return;
        }

        if (q.contains("chrome")) {
            openApp("com.android.chrome", "Chrome");
            return;
        }

        if (q.contains("camera") || q.contains("कॅमेरा")) {
            try {
                startActivity(new Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
                reply("Opening camera.");
            } catch (Exception e) {
                reply("Camera unavailable.");
            }
            return;
        }

        if (q.contains("settings") || q.contains("सेटिंग")) {
            startActivity(new Intent(
                android.provider.Settings.ACTION_SETTINGS));
            reply("Opening settings.");
            return;
        }

        if (q.contains("call") || q.contains("कॉल")) {
            startActivity(new Intent(Intent.ACTION_DIAL));
            reply("Opening phone dialer.");
            return;
        }

        if (q.equals("home") || q.contains("होम")) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
            return;
        }

        askServer(command);
    }

    private void openApp(String pkg, String name) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            startActivity(i);
            reply("Opening " + name);
        } else {
            reply(name + " is not installed.");
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(
                Intent.ACTION_VIEW, Uri.parse(url)));
            reply("Opening website.");
        } catch (Exception e) {
            reply("Could not open website.");
        }
    }

    private void askServer(String message) {
        status.setText("THINKING...");

        new Thread(() -> {
            HttpURLConnection c = null;

            try {
                JSONObject body = new JSONObject();
                body.put("message", message);

                c = (HttpURLConnection)
                    new URL(SERVER).openConnection();

                c.setRequestMethod("POST");
                c.setConnectTimeout(20000);
                c.setReadTimeout(30000);
                c.setDoOutput(true);
                c.setRequestProperty(
                    "Content-Type", "application/json");

                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(
                        StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                InputStream is = code >= 200 && code < 300
                    ? c.getInputStream() : c.getErrorStream();

                if (is == null) throw new Exception("HTTP " + code);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is,
                        StandardCharsets.UTF_8));

                StringBuilder out = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(out.toString());
                String answer = json.optString("reply",
                    json.optString("response",
                    json.optString("message", out.toString())));

                runOnUiThread(() -> reply(answer));

            } catch (Exception e) {
                runOnUiThread(() ->
                    reply("Server error. Please try again."));
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

private void openInstalledApp(String command) {
    String q = command.toLowerCase(Locale.ROOT);

    android.content.pm.PackageManager pm =
        getPackageManager();

    java.util.List<android.content.pm.ApplicationInfo> apps =
        pm.getInstalledApplications(
            android.content.pm.PackageManager.GET_META_DATA);

    for (android.content.pm.ApplicationInfo app : apps) {
        String name = pm.getApplicationLabel(app)
            .toString().toLowerCase(Locale.ROOT);

        if (name.equals(q) || name.contains(q)) {
            Intent launch = pm.getLaunchIntentForPackage(
                app.packageName);

            if (launch != null) {
                startActivity(launch);
                reply("Opening " +
                    pm.getApplicationLabel(app));
                return;
            }
        }
    }

    reply("App not found: " + command);
}
    
    private void reply(String message) {
        addChat("KRUSHNA: " + message);
        if (tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH,
                null, "KRUSHNA");
        }
        status.setText("KRUSHNA AI READY");
    }

    private void addChat(String message) {
        chat.append(message + "\n\n");
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
