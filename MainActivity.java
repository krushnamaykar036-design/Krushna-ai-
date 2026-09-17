package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;
    private static final int MIC_PERMISSION = 1002;

    private TextView status;
    private TextView chat;
    private EditText input;
    private TextToSpeech tts;

    private static final String CHAT_URL =
            "https://krushna-ai-hseh.onrender.com/chat";

    private static final String CODE_URL =
            "https://krushna-ai-hseh.onrender.com/code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUI();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("mr", "IN"));

                if (r == TextToSpeech.LANG_MISSING_DATA ||
                        r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        setStatus("Krushna AI Ready");

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION
            );
        }
    }

    private void buildUI() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setText("KRUSHNA AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(10), 0, dp(10));

        root.addView(title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(65)
                ));

        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(5), 0, dp(10));

        root.addView(status,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(45)
                ));

        ScrollView scroll = new ScrollView(this);

        chat = new TextView(this);
        chat.setTextColor(Color.WHITE);
        chat.setTextSize(17);
        chat.setPadding(dp(12), dp(12), dp(12), dp(12));

        scroll.addView(chat);

        root.addView(scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                ));

        input = new EditText(this);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint("Type your message...");
        input.setSingleLine(false);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.rgb(30, 30, 30));
        inputBg.setCornerRadius(dp(14));

        input.setBackground(inputBg);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));

        root.addView(input,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                ));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button mic = new Button(this);
        mic.setText("MIC");
        mic.setTextColor(Color.WHITE);

        Button send = new Button(this);
        send.setText("SEND");
        send.setTextColor(Color.WHITE);

        buttons.addView(mic,
                new LinearLayout.LayoutParams(
                        0,
                        dp(55),
                        1
                ));

        buttons.addView(send,
                new LinearLayout.LayoutParams(
                        0,
                        dp(55),
                        1
                ));

        root.addView(buttons);

        setContentView(root);

        mic.setOnClickListener(v -> startVoice());

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();

            if (!text.isEmpty()) {
                handleCommand(text);
                input.setText("");
            }
        });
    }

    private void startVoice() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION
            );

            return;
        }

        try {

            Intent intent = new Intent(
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
                    "Speak to Krushna AI"
            );

            setStatus("Listening...");

            startActivityForResult(intent, VOICE_REQUEST);

        } catch (Exception e) {

            setStatus("Voice input unavailable");
            Toast.makeText(
                    this,
                    "Voice input not available",
                    Toast.LENGTH_SHORT
            ).show();
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

        if (requestCode == VOICE_REQUEST &&
                resultCode == RESULT_OK &&
                data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null &&
                    !results.isEmpty()) {

                String command = results.get(0);

                input.setText(command);

                handleCommand(command);
            }
        }

        setStatus("Ready");
    }

    private void handleCommand(String command) {

        if (command == null) {
            return;
        }

        String text = command
                .trim()
                .toLowerCase(Locale.ROOT);

        if (text.isEmpty()) {
            return;
        }

        addChat("You: " + command);

        // CODE COMMAND
        if (text.startsWith("code ") ||
                text.startsWith("\u0915\u094b\u0921 ") ||
                text.contains("code command")) {

            sendCodeCommand(command);
            return;
        }

        // GREETING
        if (containsAny(
                text,
                "hello",
                "hi krushna",
                "hello krushna",
                "\u0939\u0945\u0932\u094b \u0915\u0943\u0937\u094d\u0923\u093e",
                "\u0928\u092e\u0938\u094d\u0915\u093e\u0930"
        )) {

            reply("Hello! I am Krushna AI. How can I help you?");
            return;
        }

        // CALL / PHONE
        if (containsAny(
                text,
                "call",
                "phone",
                "dial",
                "\u0915\u0949\u0932",
                "\u092b\u094b\u0928"
        )) {

            openDialer();
            return;
        }

        // HOME COMMANDS
        if (runHomeCommand(text)) {
            return;
        }

        // NORMAL AI CHAT
        askServer(command);
    }

    private boolean runHomeCommand(String command) {

        String text = command.toLowerCase(Locale.ROOT);

        // HOME
        if (containsAny(
                text,
                "home",
                "home screen",
                "\u0939\u094b\u092e"
        )) {

            openHomeScreen();
            return true;
        }

        // CAMERA
        if (containsAny(
                text,
                "camera",
                "open camera",
                "\u0915\u0945\u092e\u0947\u0930\u093e"
        )) {

            try {

                Intent intent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );

                startActivity(intent);

                reply("Opening camera.");

            } catch (Exception e) {

                reply("Camera could not be opened.");
            }

            return true;
        }

        // SETTINGS
        if (containsAny(
                text,
                "settings",
                "open settings",
                "\u0938\u0947\u091f\u093f\u0902\u0917"
        )) {

            try {

                Intent intent = new Intent(
                        Settings.ACTION_SETTINGS
                );

                startActivity(intent);

                reply("Opening settings.");

            } catch (Exception e) {

                reply("Settings could not be opened.");
            }

            return true;
        }

        // GALLERY
        if (containsAny(
                text,
                "gallery",
                "photos",
                "photo",
                "\u0917\u0945\u0932\u0930\u0940",
                "\u092b\u094b\u091f\u094b"
        )) {

            try {

                Intent intent = new Intent(
                        Intent.ACTION_VIEW
                );

                intent.setType("image/*");
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);

                reply("Opening gallery.");

            } catch (Exception e) {

                reply("Gallery could not be opened.");
            }

            return true;
        }

        // CALCULATOR
        if (containsAny(
                text,
                "calculator",
                "calc",
                "\u0915\u0945\u0932\u094d\u0915\u094d\u092f\u0941\u0932\u0947\u091f\u0930"
        )) {

            try {

                Intent intent = new Intent(
                        Intent.ACTION_MAIN
                );

                intent.addCategory(
                        Intent.CATEGORY_APP_CALCULATOR
                );

                startActivity(intent);

                reply("Opening calculator.");

            } catch (Exception e) {

                reply("Calculator could not be opened.");
            }

            return true;
        }

        // WHATSAPP
        if (containsAny(
                text,
                "whatsapp"
        )) {

            if (openAppByPackage(
                    "com.whatsapp",
                    "WhatsApp"
            )) {
                return true;
            }

            reply("WhatsApp is not available.");
            return true;
        }

        // INSTAGRAM
        if (containsAny(
                text,
                "instagram"
        )) {

            if (openAppByPackage(
                    "com.instagram.android",
                    "Instagram"
            )) {
                return true;
            }

            reply("Instagram is not available.");
            return true;
        }

        // YOUTUBE
        if (containsAny(
                text,
                "youtube",
                "you tube"
        )) {

            if (openAppByPackage(
                    "com.google.android.youtube",
                    "YouTube"
            )) {
                return true;
            }

            try {

                Intent intent = new Intent(
                        Intent.ACTION_VIEW
                );

                intent.setData(
                        android.net.Uri.parse(
                                "https://www.youtube.com"
                        )
                );

                startActivity(intent);

                reply("Opening YouTube.");

            } catch (Exception e) {

                reply("YouTube could not be opened.");
            }

            return true;
        }

        // CHROME
        if (containsAny(
                text,
                "chrome",
                "browser"
        )) {

            if (openAppByPackage(
                    "com.android.chrome",
                    "Chrome"
            )) {
                return true;
            }

            reply("Chrome is not available.");
            return true;
        }

        // ANY INSTALLED APP
        if (text.startsWith("open ") ||
                text.startsWith("launch ") ||
                text.startsWith("start ")) {

            if (openAnyInstalledApp(text)) {
                return true;
            }
        }

        return false;
    }

    private boolean openAnyInstalledApp(String command) {

        String clean = command
                .toLowerCase(Locale.ROOT)
                .replace("open ", "")
                .replace("launch ", "")
                .replace("start ", "")
                .trim();

        PackageManager pm = getPackageManager();

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );

        for (ApplicationInfo app : apps) {

            CharSequence labelObject =
                    pm.getApplicationLabel(app);

            if (labelObject == null) {
                continue;
            }

            String label = labelObject
                    .toString()
                    .toLowerCase(Locale.ROOT);

            if (clean.equals(label) ||
                    clean.contains(label)) {

                Intent intent =
                        pm.getLaunchIntentForPackage(
                                app.packageName
                        );

                if (intent != null) {

                    try {

                        startActivity(intent);

                        reply(
                                "Opening " +
                                labelObject.toString() +
                                "."
                        );

                        return true;

                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return false;
    }

    private boolean openAppByPackage(
            String packageName,
            String appName) {

        try {

            PackageManager pm =
                    getPackageManager();

            Intent intent =
                    pm.getLaunchIntentForPackage(
                            packageName
                    );

            if (intent != null) {

                startActivity(intent);

                reply(
                        "Opening " +
                        appName +
                        "."
                );

                return true;
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private void openHomeScreen() {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_MAIN
            );

            intent.addCategory(
                    Intent.CATEGORY_HOME
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

            reply("Going to home screen.");

        } catch (Exception e) {

            reply("Home screen could not be opened.");
        }
    }

    private void openDialer() {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_DIAL
            );

            startActivity(intent);

            reply("Opening phone dialer.");

        } catch (Exception e) {

            reply("Phone dialer could not be opened.");
        }
    }

    private void sendCodeCommand(String command) {

        setStatus("Sending code command...");

        try {

            JSONObject body = new JSONObject();

            body.put(
                    "command",
                    command
            );

            postJson(
                    CODE_URL,
                    body,
                    true
            );

        } catch (Exception e) {

            reply("Code command error.");
            setStatus("Ready");
        }
    }

    private void askServer(String command) {

        setStatus("Thinking...");

        try {

            JSONObject body = new JSONObject();

            body.put(
                    "message",
                    command
            );

            postJson(
                    CHAT_URL,
                    body,
                    false
            );

        } catch (Exception e) {

            reply("AI request error.");
            setStatus("Ready");
        }
    }

    private void postJson(
            String urlString,
            JSONObject body,
            boolean codeRequest) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "POST"
                );

                connection.setConnectTimeout(
                        20000
                );

                connection.setReadTimeout(
                        30000
                );

                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
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

                int responseCode =
                        connection.getResponseCode();

                InputStream stream;

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    stream =
                          connection.getInputStream();

                } else {

                    stream =
                            connection.getErrorStream();

                    if (stream == null) {

                        throw new Exception(
                                "HTTP " +
                                responseCode
                        );
                    }
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream,
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine())
                        != null) {

                    result.append(line);
                }

                reader.close();

                String raw =
                        result.toString();

                JSONObject json =
                        new JSONObject(raw);

                String answer = "";

                if (json.has("reply")) {

                    answer =
                            json.optString(
                                    "reply"
                            );

                } else if (json.has("response")) {

                    answer =
                            json.optString(
                                    "response"
                            );

                } else if (json.has("message")) {

                    answer =
                            json.optString(
                                    "message"
                            );
                }

                if (answer == null ||
                        answer.trim().isEmpty()) {

                    answer = raw;
                }

                final String finalAnswer =
                        answer;

                runOnUiThread(() -> {

                    addChat(
                            "Krushna: " +
                            finalAnswer
                    );

                    speak(finalAnswer);

                    setStatus("Ready");
                });

            } catch (Exception e) {

                final String error =
                        e.getMessage() == null
                                ? "Connection error"
                                : e.getMessage();

                runOnUiThread(() -> {

                    addChat(
                            "Krushna: Server error - " +
                            error
                    );

                    speak(
                            "Server connection error."
                    );

                    setStatus("Ready");
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void reply(String message) {

        addChat(
                "Krushna: " +
                message
        );

        speak(message);

        setStatus("Ready");
    }

    private void addChat(String message) {

        if (chat == null) {
            return;
        }

        if (chat.length() > 0) {
            chat.append("\n\n");
        }

        chat.append(message);

        chat.post(() -> {

            View parent =
                    (View) chat.getParent();

            if (parent instanceof ScrollView) {

                ((ScrollView) parent)
                        .fullScroll(
                                View.FOCUS_DOWN
                        );
            }
        });
    }

    private void speak(String message) {

        if (tts != null &&
                message != null &&
                !message.trim().isEmpty()) {

            tts.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "KRUSHNA_AI"
            );
        }
    }

    private void setStatus(String message) {

        if (status != null) {
            status.setText(message);
        }
    }

    private boolean containsAny(
            String text,
            String... values) {

        if (text == null) {
            return false;
        }

        String lower =
                text.toLowerCase(
                        Locale.ROOT
                );

        for (String value : values) {

            if (value != null &&
                    lower.contains(
                            value.toLowerCase(
                                    Locale.ROOT
                            )
                    )) {

                return true;
            }
        }

        return false;
    }

    private int dp(int value) {

        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density
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
