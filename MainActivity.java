package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;

    private TextView status;
    private TextView chat;
    private EditText input;
    private Button micButton;

    private TextToSpeech tts;

    private static final String SERVER_URL =
            "https://krushna-ai-hseh.onrender.com/chat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUI();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
            }
        });
    }

    private void buildUI() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(10, 10, 10));
        root.setBackground(background);

        TextView title = new TextView(this);
        title.setText("KRUSHNA AI 🤖");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 20, 10, 20);

        root.addView(title);

        TextView logoText = new TextView(this);
        logoText.setText("🟠  K R U S H N A  A I  🟠");
        logoText.setTextSize(20);
        logoText.setTextColor(Color.rgb(255, 140, 0));
        logoText.setGravity(Gravity.CENTER);
        logoText.setPadding(5, 5, 5, 20);

        root.addView(logoText);

        status = new TextView(this);
        status.setText("Ready");
        status.setTextSize(16);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        status.setPadding(10, 10, 10, 15);

        root.addView(status);

        ScrollView scrollView = new ScrollView(this);

        chat = new TextView(this);
        chat.setText(
                "Krushna AI ready आहे. 🤖\n\n" +
                "Try:\n" +
                "• YouTube उघड\n" +
                "• Chrome उघड\n" +
                "• WhatsApp उघड\n" +
                "• Camera उघड\n" +
                "• Calculator उघड\n" +
                "• Hello\n"
        );

        chat.setTextSize(17);
        chat.setTextColor(Color.WHITE);
        chat.setPadding(15, 15, 15, 15);

        scrollView.addView(chat);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        root.addView(scrollView, scrollParams);

        input = new EditText(this);
        input.setHint("Type your message...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(16);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.rgb(35, 35, 35));
        inputBg.setCornerRadius(25);

        input.setBackground(inputBg);
        input.setPadding(25, 10, 25, 10);

        root.addView(
                input,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, 12, 0, 0);

        Button sendButton = new Button(this);
        sendButton.setText("SEND");
        sendButton.setTextColor(Color.WHITE);

        micButton = new Button(this);
        micButton.setText("🎤 MIC");
        micButton.setTextColor(Color.WHITE);

        buttons.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        buttons.addView(
                micButton,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        root.addView(buttons);

        sendButton.setOnClickListener(v -> {

            String message = input.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(
                        this,
                        "Message type कर bro 😄",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            input.setText("");
            handleCommand(message);
        });

        micButton.setOnClickListener(v -> startVoice());

        setContentView(root);
    }

    private void startVoice() {

        try {

            Intent intent =
                    new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

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
                    "Krushna AI ला command बोला"
            );

            startActivityForResult(intent, VOICE_REQUEST);

            setStatus("Listening... 🎤");

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Voice recognition available नाही",
                    Toast.LENGTH_LONG
            ).show();

            setStatus("Voice unavailable");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == VOICE_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null && !results.isEmpty()) {

                String command = results.get(0);

                input.setText(command);

                handleCommand(command);
            }
        }
    }

    private void handleCommand(String command) {

        if (command == null) {
            return;
        }

        String lower = command.toLowerCase(Locale.ROOT).trim();

        addChat("You: " + command);

        /*
         * HOME COMMANDS FIRST
         * त्यामुळे Gemini quota वाया जाणार नाही.
         */

        private boolean runHomeCommand(String command) {

    if (command == null) {
        return false;
    }

    String text = command.toLowerCase(Locale.ROOT).trim();

    // HOME SCREEN
    if (containsAny(
            text,
            "home",
            "home screen",
            "होम",
            "होम स्क्रीन",
            "घरी जा"
    )) {
        openHomeScreen();
        return true;
    }

    // CAMERA
    if (containsAny(text, "camera", "कॅमेरा")) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(intent);
            reply("Camera उघडत आहे 📷");
        } catch (Exception e) {
            reply("Camera उघडता आला नाही.");
        }
        return true;
    }

    // SETTINGS
    if (containsAny(
            text,
            "settings",
            "setting",
            "सेटिंग",
            "सेटिंग्स"
    )) {
        try {
            Intent intent =
                    new Intent(android.provider.Settings.ACTION_SETTINGS);
            startActivity(intent);
            reply("Settings उघडत आहे ⚙️");
        } catch (Exception e) {
            reply("Settings उघडता आले नाही.");
        }
        return true;
    }

    // PHONE
    if (containsAny(
            text,
            "phone",
            "dialer",
            "फोन",
            "डायलर"
    )) {
        openDialer();
        reply("Phone उघडत आहे 📞");
        return true;
    }

    // GALLERY / PHOTOS
    if (containsAny(
            text,
            "gallery",
            "photos",
            "photo",
            "गॅलरी",
            "फोटो"
    )) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            startActivity(intent);
            reply("Gallery उघडत आहे 🖼️");
        } catch (Exception e) {
            reply("Gallery उघडता आली नाही.");
        }
        return true;
    }

    // CALCULATOR
    if (containsAny(
            text,
            "calculator",
            "calc",
            "कॅल्क्युलेटर"
    )) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
            startActivity(intent);
            reply("Calculator उघडत आहे 🧮");
        } catch (Exception e) {
            reply("Calculator app सापडली नाही.");
        }
        return true;
    }

    // ANY INSTALLED APP
    if (openAnyInstalledApp(text)) {
        return true;
    }

    return false;
    }
        if (containsAny(
                command,
                "settings",
                "setting",
                "सेटिंग",
                "सेटिंग्स"
        )) {

            try { 
                if (openAnyInstalledApp(text)) {
    return true;
                }

                Intent intent =
                        new Intent(
                                android.provider.Settings.ACTION_SETTINGS
                        );

                startActivity(intent);

                reply("Settings उघडत आहे ⚙️");

            } catch (Exception e) {

                reply("Settings उघडता आले नाही.");
            }

            return true;
        }

        /*
         * Camera
         */

        if (containsAny(
                command,
                "camera",
                "कॅमेरा"
        )) {

            try {

                Intent intent =
                        new Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE
                        );

                startActivity(intent);

                reply("Camera उघडत आहे 📷");

            } catch (Exception e) {

                reply("Camera उघडता आला नाही.");
            }

            return true;
        }

        /*
         * Calculator
         */

        if (containsAny(
                command,
                "calculator",
                "calc",
                "कॅल्क्युलेटर"
        )) {

            boolean opened =
                    openPackage(
                            "com.google.android.calculator",
                            "Calculator"
                    );

            if (!opened) {

                try {

                    Intent intent =
                            new Intent(
                                    Intent.ACTION_MAIN
                            );

                    intent.addCategory(
                            Intent.CATEGORY_APP_CALCULATOR
                    );

                    startActivity(intent);

                    reply("Calculator उघडत आहे 🧮");

                } catch (Exception e) {

                    reply("Calculator app सापडली नाही.");
                }
            }

            return true;
        }

        /*
         * Gallery / Photos
         */

        if (containsAny(
                command,
                "gallery",
                "photos",
                "photo",
                "गॅलरी",
                "फोटो"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW
                        );

                intent.setType("image/*");

                startActivity(intent);

                reply("Gallery उघडत आहे 🖼️");

            } catch (Exception e) {

                reply("Gallery उघडता आली नाही.");
            }

            return true;
        }

        /*
         * Phone
         */

        if (containsAny(
                command,
                "phone",
                "dialer",
                "फोन"
        )) {

            openDialer();

            reply("Phone उघडत आहे 📞");

            return true;
        }

        return false;
    }

    private boolean containsAny(
            String text,
            String... words
    ) {

        for (String word : words) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private boolean openPackage(
            String packageName,
            String appName
    ) {

        try {

            PackageManager pm =
                    getPackageManager();

            Intent intent =
                    pm.getLaunchIntentForPackage(
                            packageName
                    );

            if (intent != null) {

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);

                reply(
                        appName +
                        " उघडत आहे 📱"
                );

                return true;
            }

        } catch (Exception ignored) {
        }

        reply(
                appName +
                " app फोनमध्ये सापडली नाही."
        );

        return false;
    }

    private void openDialer() {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_DIAL
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Dialer उघडता आला नाही.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void askServer(String message) {

        setStatus("Online AI thinking... 🤖");

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(SERVER_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);

                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                JSONObject body =
                        new JSONObject();

                body.put(
                        "message",
                        message
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

                if (responseCode >= 200
                        && responseCode < 300) {

                    stream =
                            connection.getInputStream();

                } else {

                    stream =
                            connection.getErrorStream();
                }

                String response =
                        readStream(stream);

                final int code = responseCode;
                final String finalResponse = response;

                runOnUiThread(() -> {

                    if (code == 429) {

                        setStatus(
                                "AI quota limit reached"
                        );

                        reply(
                                "Gemini ची सध्याची quota limit संपली आहे. " +
                                "थोड्या वेळाने पुन्हा try कर bro. ⏳"
                        );

                        return;
                    }

                    if (code == 503) {

                        setStatus(
                                "AI temporarily unavailable"
                        );

                        reply(
                                "Online AI server सध्या busy आहे. " +
                                "थोड्या वेळाने पुन्हा try कर. 🔄"
                        );

                        return;
                    }

                    if (code < 200
                            || code >= 300) {

                        setStatus(
                                "Online AI Error"
                        );

                        reply(
                                "Online AI Error: HTTP " +
                                code
                        );

                        return;
                    }

                    String answer =
                            extractReply(
                                    finalResponse
                            );

                    if (answer.isEmpty()) {

                        answer =
                                finalResponse;
                    }

                    setStatus("Online AI Ready 🤖");

                    reply(answer);
                });

            } catch (Exception e) {

                String error =
                        e.getMessage();

                if (error == null
                        || error.isEmpty()) {

                    error =
                            "Connection failed";
                }

                final String finalError =
                        error;

                runOnUiThread(() -> {

                    setStatus(
                            "Offline / Connection error"
                    );

                    reply(
                            "Online AI Error: " +
                            finalError
                    );
                });

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }

    private String readStream(
            InputStream stream
    ) throws Exception {

        if (stream == null) {
            return "";
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

        return result.toString();
    }

    private String extractReply(
            String response
                         ) {

        if (response == null
                || response.trim().isEmpty()) {

            return "";
        }

        try {

            JSONObject object =
                    new JSONObject(response);

            /*
             * Our server normally returns:
             * {"reply":"..."}
             */

            if (object.has("reply")) {

                return object
                        .optString("reply")
                        .trim();
            }

            /*
             * Other common response names
             */

            if (object.has("response")) {

                return object
                        .optString("response")
                        .trim();
            }

            if (object.has("text")) {

                return object
                        .optString("text")
                        .trim();
            }

            if (object.has("message")) {

                return object
                        .optString("message")
                        .trim();
            }

        } catch (Exception ignored) {
        }

        return response.trim();
    }

    private void addChat(
            String message
    ) {

        runOnUiThread(() -> {

            if (chat != null) {

                chat.append(
                        "\n\n" +
                        message
                );
            }
        });
    }

    private void reply(
            String message
    ) {

        if (message == null) {
            return;
        }

        addChat(
                "Krushna AI: " +
                message
        );

        setStatus("Ready");

        speak(message);
    }

    private void speak(
            String message
    ) {

        if (tts == null
                || message == null
                || message.isEmpty()) {

            return;
        }

        try {

            tts.setLanguage(
                    new Locale("en", "IN")
            );

            tts.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "KRUSHNA_AI"
            );

        } catch (Exception ignored) {
        }
    }

    private void setStatus(
            String message
    ) {

        runOnUiThread(() -> {

            if (status != null) {

                status.setText(message);

                if (message.contains("Listening")) {

                    status.setTextColor(
                            Color.rgb(
                                    255,
                                    140,
                                    0
                            )
                    );

                } else {

                    status.setTextColor(
                            Color.LTGRAY
                    );
                }
            }
        });
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
