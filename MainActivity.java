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
    private Button voiceToggle;
    private boolean voiceActive = false;

    private static final String SERVER =
        "https://krushna-ai-he2j.onrender.com/chat";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUI();

        tts = new TextToSpeech(this, r -> {
            if (r == TextToSpeech.SUCCESS)
                tts.setLanguage(new Locale("mr", "IN"));
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO
            }, MIC);

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{
                Manifest.permission.READ_CONTACTS
            }, 1003);

        status.setText("KRUSHNA AI ONLINE");
    }

    private int dp(int n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density);
    }

    private GradientDrawable bg(int c, int r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(c);
        d.setCornerRadius(dp(r));
        d.setStroke(dp(1), Color.rgb(180, 0, 35));
        return d;
    }

    private TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(18), dp(15), dp(18), dp(15));
        root.setBackgroundColor(Color.rgb(8, 4, 8));

        TextView title = text("🔥 KRUSHNA AI", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.RED);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        root.addView(text("RED DEVIL • PREMIUM AI", 13));

        status = text("ONLINE", 15);
        status.setTextColor(Color.RED);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(40)));

        // Voice Assistant ON/OFF
        voiceToggle = new Button(this);
        voiceToggle.setText("🎙️ VOICE ASSISTANT OFF");
        voiceToggle.setTextColor(Color.WHITE);
        voiceToggle.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.RED));
        root.addView(voiceToggle,
            new LinearLayout.LayoutParams(-1, dp(50)));

        voiceToggle.setOnClickListener(v -> toggleVoice());

        TextView mic = text("🦚", 65);
        mic.setBackground(bg(Color.rgb(65, 0, 15), 100));

        LinearLayout.LayoutParams mp =
            new LinearLayout.LayoutParams(dp(155), dp(155));
        mp.gravity = Gravity.CENTER;
        mp.setMargins(0, dp(20), 0, dp(15));
        root.addView(mic, mp);

        mic.setOnClickListener(v -> startVoice());

        root.addView(text("TAP TO SPEAK • HEY KRUSHNA", 13));

        ScrollView scroll = new ScrollView(this);
        chat = text("", 16);
        chat.setGravity(Gravity.TOP | Gravity.LEFT);
        chat.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(chat);

        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Ask Krushna AI...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setBackground(bg(Color.rgb(30, 15, 20), 15));
        root.addView(input, new LinearLayout.LayoutParams(-1, dp(50)));

        Button send = new Button(this);
        send.setText("SEND MESSAGE");
        send.setTextColor(Color.WHITE);
        send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.RED));
        root.addView(send, new LinearLayout.LayoutParams(-1, dp(50)));

        send.setOnClickListener(v -> {
            String q = input.getText().toString().trim();
            input.setText("");
            if (!q.isEmpty()) handleCommand(q);
        });

        setContentView(root);
    }

    // Voice Assistant Start / Stop
    private void toggleVoice() {
        if (!voiceActive) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO
                }, MIC);
                return;
            }

            Intent service = new Intent(this, HomeVoiceService.class);
            startForegroundService(service);

            voiceActive = true;
            voiceToggle.setText("🔴 VOICE ASSISTANT ON");
            status.setText("VOICE ASSISTANT ACTIVE");
            reply("Voice Assistant सुरू झाला");
        } else {
            stopService(new Intent(this, HomeVoiceService.class));
            voiceActive = false;
            voiceToggle.setText("🎙️ VOICE ASSISTANT OFF");
            status.setText("KRUSHNA AI ONLINE");
            reply("Voice Assistant बंद झाला");
        }
    }

    private void startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO
            }, MIC);
            return;
        }

        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hey Krushna");
            status.setText("LISTENING...");
            startActivityForResult(i, VOICE);
        } catch (Exception e) {
            reply("Voice unavailable");
        }
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);

        if (r == VOICE && c == RESULT_OK && d != null) {
            ArrayList<String> a = d.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS);
            if (a != null && !a.isEmpty())
                handleCommand(a.get(0));
        }
        status.setText("KRUSHNA AI READY");
    }

    private void handleCommand(String command) {
        String q = command.toLowerCase(Locale.ROOT).trim();
        addChat("YOU: " + command);

        if (q.contains("youtube")) {
            openUrl("https://youtube.com");
            return;
        }

        if (q.contains("google")) {
            openUrl("https://google.com");
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
            startActivity(new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
            reply("Camera opening");
            return;
        }

        if (q.contains("settings") || q.contains("सेटिंग")) {
            startActivity(new Intent(
                android.provider.Settings.ACTION_SETTINGS));
            reply("Settings opening");
            return;
        }

        if (q.contains("call") || q.contains("कॉल")) {
            startActivity(new Intent(Intent.ACTION_DIAL));
            reply("Phone dialer opening");
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
        Intent i = getPackageManager()
            .getLaunchIntentForPackage(pkg);

        if (i != null) {
            startActivity(i);
            reply("Opening " + name);
        } else {
            reply(name + " not installed");
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            reply("Opening website");
        } catch (Exception e) {
            reply("Website unavailable");
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
                c.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(
                        StandardCharsets.UTF_8));
                }

                InputStream is = c.getResponseCode() < 300
                    ? c.getInputStream() : c.getErrorStream();

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));

                StringBuilder out = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    out.append(line);
                br.close();

                JSONObject j = new JSONObject(out.toString());
                String answer = j.optString("reply",
                    j.optString("response", "AI response unavailable"));

                runOnUiThread(() -> reply(answer));

            } catch (Exception e) {
                runOnUiThread(() -> reply("Server error"));
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private void callContact(String name) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.READ_CONTACTS}, 1003);
            reply("Allow contacts permission first");
            return;
        }

        android.database.Cursor cursor = getContentResolver().query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            },
            null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String contact = cursor.getString(0);
                String number = cursor.getString(1);

                if (contact != null &&
                    contact.toLowerCase(Locale.ROOT)
                        .contains(name.toLowerCase(Locale.ROOT))) {

                    cursor.close();

                    Intent dial = new Intent(Intent.ACTION_DIAL);
                    dial.setData(Uri.parse("tel:" + number));
                    startActivity(dial);

                    reply("Dialer opening for " + contact);
                    return;
                }
            }
            cursor.close();
        }

        reply("Contact not found: " + name);
    }

    private void reply(String s) {
        addChat("KRUSHNA: " + s);
        if (tts != null)
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "KRUSHNA");
        status.setText("KRUSHNA AI READY");
    }

    private void addChat(String s) {
        chat.append(s + "\n\n");
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

