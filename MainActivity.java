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

        input = findViewById(
                getResources().getIdentifier("input", "id", getPackageName())
        );

        chat = findViewById(
                getResources().getIdentifier("chat", "id", getPackageName())
        );

        Button mic = findViewById(
                getResources().getIdentifier("mic", "id", getPackageName())
        );

        Button send = findViewById(
                getResources().getIdentifier("send", "id", getPackageName())
        );

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        send.setOnClickListener(v -> sendMessage());

        mic.setOnClickListener(v -> startVoice());

        chat.setText("Krushna AI तयार आहे. 🎙️");
    }

    private void sendMessage() {

        String question = input.getText().toString().trim();

        if (question.isEmpty()) {
            return;
        }

        chat.append("\n\nYou: " + question);
        input.setText("");

        if (handleCommand(question)) {
            return;
        }

        askServer(question);
    }

    private boolean handleCommand(String text) {

        String lower = text.toLowerCase(Locale.ROOT);

        // Settings
        if (lower.contains("settings")) {
            openAppByPackage("com.android.settings");
            return true;
        }

        // Camera
        if (lower.contains("camera")) {
            try {
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                startActivity(intent);
                speak("Camera उघडत आहे");
            } catch (Exception e) {
                speak("Camera उघडता आला नाही");
            }
            return true;
        }

        // WhatsApp
        if (lower.contains("whatsapp")) {
            openAppByPackage("com.whatsapp");
            return true;
        }

        // YouTube
        if (lower.contains("youtube")) {
            openAppByPackage("com.google.android.youtube");
            return true;
        }

        // Chrome
        if (lower.contains("chrome")) {
            openAppByPackage("com.android.chrome");
            return true;
        }

        // Phone / Dialer
        if (lower.contains("phone") ||
                lower.contains("फोन") ||
                lower.contains("dial") ||
                lower.contains("कॉल")) {

            openDialer();
            return true;
        }

        // Messages
        if (lower.contains("message") ||
                lower.contains("messages") ||
                lower.contains("sms")) {

            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
                startActivity(intent);
                speak("Messages उघडत आहे");
            } catch (Exception e) {
                speak("Messages app सापडला नाही");
            }

            return true;
        }

        // Maps
        if (lower.contains("map") ||
                lower.contains("maps")) {

            try {
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=")
                );
                startActivity(intent);
                speak("Maps उघडत आहे");
            } catch (Exception e) {
                speak("Maps उघडता आला नाही");
            }

            return true;
        }

        // Generic installed app search
        if (lower.contains("open") ||
                lower.contains("उघड") ||
                lower.contains("चालू") ||
                lower.contains("start")) {

            if (openInstalledApp(text)) {
                return true;
            }
        }

        // Try installed app search for simple app name
        if (openInstalledApp(text)) {
            return true;
        }

        return false;
    }

    private void openDialer() {

        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
            speak("Phone उघडत आहे");
        } catch (Exception e) {
            speak("Phone उघडता आला नाही");
        }
    }

    private void openAppByPackage(String packageName) {

        try {

            PackageManager pm = getPackageManager();

            Intent launchIntent =
                    pm.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                startActivity(launchIntent);
                speak("App उघडत आहे");
            } else {
                speak("App सापडला नाही");
            }

        } catch (Exception e) {
            speak("App उघडता आला नाही");
        }
    }

    private boolean openInstalledApp(String command) {

        try {

            String searchName = cleanAppName(command);

            if (searchName.isEmpty()) {
                return false;
            }

            PackageManager pm = getPackageManager();

            ArrayList<ApplicationInfo> apps =
                    new ArrayList<>(
                            pm.getInstalledApplications(
                                    PackageManager.GET_META_DATA
                            )
                    );

            for (ApplicationInfo app : apps) {

                Intent launchIntent =
                        pm.getLaunchIntentForPackage(app.packageName);

                if (launchIntent == null) {
                    continue;
                }

                String appName =
                        pm.getApplicationLabel(app)
                                .toString()
                                .toLowerCase(Locale.ROOT);

                if (appName.equals(searchName) ||
                        appName.contains(searchName) ||
                        searchName.contains(appName)) {

                    startActivity(launchIntent);
                    speak(appName + " उघडत आहे");
                    return true;
                }
            }

        } catch (Exception e) {
            return false;
        }

        return false;
    }

    private String cleanAppName(String text) {

        String name = text.toLowerCase(Locale.ROOT);

        name = name.replace("open", "");
        name = name.replace("उघड", "");
        name = name.replace("चालू", "");
        name = name.replace("start", "");
        name = name.replace("app", "");
        name = name.replace("कर", "");
        name = name.trim();

        return name;
    }

    private void askServer(String question) {

        new Thread(() -> {

            try {

                URL url = new URL(SERVER_URL);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("message", question);

                OutputStream os =
                        connection.getOutputStream();

                os.write(
                        body.toString()
                                .getBytes("UTF-8")
                );

                os.close();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
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
                        new JSONObject(result.toString());

                String reply =
                        response.optString(
                                "reply",
                                "उत्तर मिळाले नाही."
                        );

                runOnUiThread(() -> {

                    chat.append(
                            "\n\nKrushna AI: " + reply
                    );

                    speak(reply);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    chat.append(
                            "\n\nKrushna AI: Server connect झाला नाही."
                    );

                    speak("Server connect झाला नाही");
                });
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

            startActivityForResult(intent, VOICE);

        } catch (Exception e) {

            speak("Voice input उपलब्ध नाही");
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
                sendMessage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

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
