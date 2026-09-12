package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText input;
    private TextView chat;
    private TextView status;
    private Button mic;
    private Button send;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private EdgeGlowView edgeGlow;

    private static final int MIC_PERMISSION = 100;
    private static final int CONTACT_PERMISSION = 101;

    private static final String SERVER_URL =
            "https://krushna-ai-hseh.onrender.com/chat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layoutId = getResources().getIdentifier(
                "activity_main",
                "layout",
                getPackageName()
        );

        setContentView(layoutId);

        input = findViewById(getResources().getIdentifier(
                "input", "id", getPackageName()));

        chat = findViewById(getResources().getIdentifier(
                "chat", "id", getPackageName()));

        status = findViewById(getResources().getIdentifier(
                "status", "id", getPackageName()));

        mic = findViewById(getResources().getIdentifier(
                "mic", "id", getPackageName()));

        send = findViewById(getResources().getIdentifier(
                "send", "id", getPackageName()));

        edgeGlow = new EdgeGlowView(this);

        addContentView(
                edgeGlow,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        edgeGlow.setVisibility(View.GONE);

        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("mr", "IN"));
            }
        });

        mic.setOnClickListener(v -> startVoice());

        send.setOnClickListener(v -> {
            String message = input.getText().toString().trim();

            if (!message.isEmpty()) {
                input.setText("");
                processCommand(message);
            }
        });

        if (status != null) {
            status.setText("Ready");
        }
    }

    private void startVoice() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION
            );

            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Voice recognition उपलब्ध नाही.");
            return;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (edgeGlow != null) {
            edgeGlow.setVisibility(View.VISIBLE);
            edgeGlow.startGlow();
        }

        if (status != null) {
            status.setText("Listening...");
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        if (status != null) {
                            status.setText("Listening...");
                        }
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                    }

                    @Override
                    public void onError(int error) {
                        stopEdgeGlow();

                        if (status != null) {
                            status.setText("Ready");
                        }

                        speak("पुन्हा बोला.");
                    }

                    @Override
                    public void onResults(Bundle results) {

                        stopEdgeGlow();

                        if (status != null) {
                            status.setText("Ready");
                        }

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (matches != null &&
                                !matches.isEmpty()) {

                            String command = matches.get(0);

                            input.setText(command);

                            processCommand(command);
                        }
                    }

                    @Override
                    public void onPartialResults(
                            Bundle partialResults) {
                    }

                    @Override
                    public void onEvent(
                            int eventType,
                            Bundle params) {
                    }
                }
        );

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
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        try {
            speechRecognizer.startListening(intent);

        } catch (Exception e) {

            stopEdgeGlow();

            if (status != null) {
                status.setText("Ready");
            }

            speak("Mic सुरू करता आला नाही.");
        }
    }

    private void stopEdgeGlow() {

        if (edgeGlow != null) {
            edgeGlow.stopGlow();
            edgeGlow.setVisibility(View.GONE);
        }
    }

    private void processCommand(String command) {

        if (command == null) return;

        command = command.trim();

        addChat("You: " + command);

        String lower =
                command.toLowerCase(Locale.ROOT);

        if (containsAny(
                lower,
                "camera",
                "open camera",
                "कॅमेरा",
                "कैमरा"
        )) {

            openCamera();
            return;
        }

        if (containsAny(
                lower,
                "call",
                "phone",
                "फोन कर",
                "फोन लाव",
                "कॉल कर",
                "कॉल लाव",
                "फोन करा",
                "कॉल करा"
        )) {

            callContactFromCommand(command);
            return;
        }

        if (containsAny(
                lower,
                "contact",
                "contacts",
                "कॉन्टॅक्ट",
                "कॉन्टॅक्ट्स",
                "नंबर"
        )) {

            openContacts();
            return;
        }

        if (containsAny(
                lower,
                "settings",
                "setting",
                "सेटिंग",
                "सेटिंग्स"
        )) {

            try {

                startActivity(
                        new Intent(
                                android.provider.Settings.ACTION_SETTINGS
                        )
                );

                speak("Settings उघडले.");

            } catch (Exception e) {

                speak("Settings उघडता आले नाही.");
            }

            return;
        }

        if (containsAny(
                lower,
                "home",
                "होम"
        )) {

            Intent home =
                    new Intent(Intent.ACTION_MAIN);

            home.addCategory(Intent.CATEGORY_HOME);

            home.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(home);

            return;
        }

        if (containsAny(
                lower,
                "open",
                "उघड",
                "उघडा",
                "ओपन",
                "चालू कर",
                "चालू करा",
                "खोल"
        )) {

            String appName =
                    extractAppName(command);

            if (!appName.isEmpty()) {

                boolean opened =
                        openInstalledApp(appName);

                if (opened) {

                    speak(
                            appName +
                            " उघडले."
                    );

                } else {

                    speak(
                            appName +
                            " app सापडले नाही."
                    );
                }

                return;
            }
        }

        if (looksLikeAppCommand(command)) {

            if (openInstalledApp(command)) {

                speak(
                        command +
                        " उघडले."
                );

                return;
            }
        }

        askServer(command);
    }

    private void openCamera() {

        try {

            Intent camera =
                    new Intent(
                            android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                    );

            startActivity(camera);

            speak("Camera उघडला.");

        } catch (Exception e) {

            speak("Camera उघडता आला नाही.");
        }
    }

    private void callContactFromCommand(String command) {

        if (checkSelfPermission(
                Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_CONTACTS
                    },
                    CONTACT_PERMISSION
            );

            Toast.makeText(
                    this,
                    "Contacts permission द्या.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String contactName =
                extractContactName(command);

        if (contactName.isEmpty()) {

            speak(
                    "कुणाला call करायचा ते सांगा."
            );

            return;
        }

        findAndDialContact(contactName);
    }

    private String extractContactName(String command) {

        String name = command;

        String[] removeWords = {

                "hello",
                "krushna",
                "krishna",

                "call",
                "phone",

                "फोन",
                "कॉल",

                "कॉल कर",
                "कॉल करा",
                "कॉल लाव",

                "फोन कर",
                "फोन करा",
                "फोन लाव",

                "ला कॉल",
                "ला call",

                "को call",
                "को कॉल",

                "कर",
                "करा",

                "करायचा",
                "करायची",

                "please",
                "pls"
        };

        for (String word : removeWords) {

            name = name.replace(
                    word,
                    " "
            );
        }

        name = name.replaceAll(
                "\\s+",
                " "
        ).trim();

        return name;
    }

    private void findAndDialContact(
            String wantedName
    ) {

        Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .CONTENT_URI,

                            new String[]{
                                    ContactsContract
                                            .CommonDataKinds
                                            .Phone
                                            .DISPLAY_NAME,

                                    ContactsContract
                                            .CommonDataKinds
                                            .Phone
                                            .NUMBER
                            },

                            null,
                            null,
                            ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .DISPLAY_NAME
                    );

            if (cursor == null) {

                speak(
                        "Contacts सापडले नाहीत."
                );

                return;
            }

            String wanted =
                    normalizeText(wantedName);

            while (cursor.moveToNext()) {

                String displayName =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        ContactsContract
                                                .CommonDataKinds
                                                .Phone
                                                .DISPLAY_NAME
                                )
                        );

                String phone =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        ContactsContract
                                                .CommonDataKinds
                                                .Phone
                                                .NUMBER
                                )
                        );

                String normalDisplay =
                        normalizeText(displayName);

                boolean matched =
                        normalDisplay.contains(wanted)
                                ||
                        wanted.contains(normalDisplay)
                                ||
                        devanagariMatches(
                                displayName,
                                wantedName
                        );

                if (matched) {

                    speak(
                            displayName +
                            " सापडला. Dialer उघडतो."
                    );

                    openDialer(phone);

                    return;
                }
            }

            speak(
                    wantedName +
                    " contact सापडला नाही."
            );

        } catch (Exception e) {

            speak(
                    "Contact शोधताना error आला."
            );

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void openDialer(String phone) {

        try {

            Intent dial =
                    new Intent(Intent.ACTION_DIAL);

            dial.setData(
                    Uri.parse(
                            "tel:" +
                            Uri.encode(phone)
                    )
            );

            startActivity(dial);

        } catch (Exception e) {

            speak(
                    "Dialer उघडता आला नाही."
            );
        }
    }

    private void openContacts() {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            ContactsContract
                                    .Contacts
                                    .CONTENT_URI
                    );

            startActivity(intent);

            speak("Contacts उघडले.");

        } catch (Exception e) {

            speak(
                    "Contacts उघडता आले नाही."
            );
        }
    }

    private boolean openInstalledApp(
            String requestedName
    ) {

        PackageManager pm =
                getPackageManager();

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );

        String wanted =
                normalizeText(requestedName);

        for (ApplicationInfo app : apps) {

            Intent launch =
                    pm.getLaunchIntentForPackage(
                            app.packageName
                    );

            if (launch == null) {
                continue;
            }

            String label =
                    app.loadLabel(pm)
                            .toString();

            String normalizedLabel =
                    normalizeText(label);

            if (normalizedLabel.equals(wanted)
                    ||
                    normalizedLabel.contains(wanted)
                    ||
                    wanted.contains(normalizedLabel)
                    ||
                    devanagariMatches(
                            label,
                            requestedName
                    )) {

                launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(launch);

                return true;
            }
        }

        String pkg =
                findCommonAppPackage(
                        wanted,
                        pm
                );

        if (pkg != null) {

            Intent launch =
                    pm.getLaunchIntentForPackage(pkg);

            if (launch != null) {

                startActivity(launch);

                return true;
            }
        }

        return false;
    }

    private String findCommonAppPackage(
            String wanted,
            PackageManager pm
    ) {

        String[] names = {

                "whatsapp",
                "instagram",
                "youtube",
                "facebook",
                "chrome",
                "gmail",
                "google",
                "maps",
                "play store",
                "spotify",
                "telegram",
                "camera",
                "settings"
        };

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );

        for (String name : names) {

            if (!wanted.contains(
                    normalizeText(name)
            )) {

                continue;
            }

            for (ApplicationInfo app : apps) {
                String label =
                        app.loadLabel(pm)
                                .toString()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                if (label.contains(
                        name.toLowerCase(
                                Locale.ROOT
                        )
                )) {

                    return app.packageName;
                }
            }
        }

        return null;
    }

    private String extractAppName(
            String command
    ) {

        String name = command;

        String[] words = {

                "hello",
                "krushna",
                "krishna",

                "open",
                "ओपन",

                "उघड",
                "उघडा",

                "उघड ना",

                "चालू कर",
                "चालू करा",

                "खोल",
                "खोल ना"
        };

        for (String word : words) {

            name = name.replace(
                    word,
                    " "
            );
        }

        return name.replaceAll(
                "\\s+",
                " "
        ).trim();
    }

    private boolean looksLikeAppCommand(
            String command
    ) {

        String lower =
                command.toLowerCase(
                        Locale.ROOT
                );

        return lower.contains("instagram")
                ||
                lower.contains("whatsapp")
                ||
                lower.contains("youtube")
                ||
                lower.contains("facebook")
                ||
                lower.contains("chrome")
                ||
                lower.contains("gmail")
                ||
                lower.contains("telegram")
                ||
                lower.contains("spotify")
                ||
                lower.contains("camera")
                ||
                lower.contains("maps");
    }

    private String normalizeText(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                text.toLowerCase(
                        Locale.ROOT
                );

        text =
                Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                );

        text =
                text.replaceAll(
                        "\\p{M}",
                        ""
                );

        text =
                text.replaceAll(
                        "[^\\p{L}\\p{N}]",
                        ""
                );

        return text.trim();
    }

    private boolean devanagariMatches(
            String contact,
            String spoken
    ) {

        if (contact == null ||
                spoken == null) {

            return false;
        }

        try {

            if (android.os.Build.VERSION.SDK_INT >= 24) {

                android.icu.text.Transliterator trans =
                        android.icu.text.Transliterator
                                .getInstance(
                                        "Devanagari-Latin; Latin-ASCII"
                                );

                String c =
                        trans.transliterate(
                                contact
                        );

                String s =
                        trans.transliterate(
                                spoken
                        );

                c = normalizeText(c);
                s = normalizeText(s);

                return c.contains(s)
                        ||
                        s.contains(c);
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private void askServer(
            String message
    ) {

        speak("थोडं थांबा.");

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

            try {

                URL url =
                        new URL(
                                SERVER_URL
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "POST"
                );

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        30000
                );

                connection.setDoOutput(
                        true
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                String safeMessage =
                        message
                                .replace(
                                        "\\",
                                        "\\\\"
                                )
                                .replace(
                                        "\"",
                                        "\\\""
                                );

                String json =
                        "{\"message\":\"" +
                        safeMessage +
                        "\"}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        json.getBytes("UTF-8")
                );

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
                }

                if (stream == null) {
                    throw new Exception(
                            "No response"
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while (
                        (line =
                                reader.readLine())
                                != null
                ) {

                    result.append(line);
                }

                reader.close();

                String response =
                        result.toString();

                String reply =
                        extractJsonReply(
                                response
                        );

                if (reply.isEmpty()) {
                    reply = response;
                }

                final String finalReply =
                        reply;

                runOnUiThread(() -> {

                    addChat(
                            "Krushna AI: " +
                            finalReply
                    );

                    speak(finalReply);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    addChat(
                            "Krushna AI: Online server unavailable."
                    );

                    speak(
                            "Online AI सध्या उपलब्ध नाही."
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String extractJsonReply(
            String json
    ) {

        try {

            String key =
                    "\"reply\"";

            int start =
                    json.indexOf(key);

            if (start < 0) {
                return "";
            }

            start =
                    json.indexOf(
                            ":",
                            start
                    );

            if (start < 0) {
                return "";
            }

            start++;

            while (
                    start < json.length()
                            &&
                    Character.isWhitespace(
                            json.charAt(start)
                    )
            ) {

                start++;
            }

            if (start < json.length()
                    &&
                    json.charAt(start) == '"') {

                start++;

                int end =
                        json.indexOf(
                                "\"",
                                start
                        );

                if (end > start) {

                    return json.substring(
                            start,
                            end
                    )
                            .replace(
                                    "\\\"",
                                    "\""
                            )
                            .replace(
                                    "\\n",
                                    "\n"
                            );
                }
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    private void addChat(
            String message
    ) {

        runOnUiThread(() -> {

            if (chat == null) {
                return;
            }

            String old =
                    chat.getText().toString();

            if (!old.isEmpty()) {
                old += "\n\n";
            }

            chat.setText(
                    old + message
            );
        });
    }

    private void speak(
            String text
    ) {

        if (textToSpeech == null ||
                text == null ||
                text.isEmpty()) {

            return;
        }

        runOnUiThread(() -> {

            try {

                textToSpeech.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "krushna_reply"
                );

            } catch (Exception ignored) {
            }
        });
    }

    private boolean containsAny(
            String text,
            String... words
    ) {

        for (String word : words) {

            if (text.contains(
                    word.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return true;
            }
        }

        return false;
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

        if (requestCode == MIC_PERMISSION) {

            if (grantResults.length > 0
                    &&
                    grantResults[0]
                    ==
                    PackageManager.PERMISSION_GRANTED) {

                speak(
                        "Microphone permission मिळाली."
                );

                startVoice();

            } else {

                speak(
                        "Microphone permission आवश्यक आहे."
                );
            }
        }

        if (requestCode == CONTACT_PERMISSION) {

            if (grantResults.length > 0
                    &&
                    grantResults[0]
                    ==
                    PackageManager.PERMISSION_GRANTED) {

                speak(
                        "Contacts permission मिळाली."
                );

            } else {

                speak(
                        "Contacts permission दिली नाही."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopEdgeGlow();

        if (speechRecognizer != null) {

            speechRecognizer.destroy();

            speechRecognizer = null;
        }

        if (textToSpeech != null) {

            textToSpeech.stop();

            textToSpeech.shutdown();

            textToSpeech = null;
        }

        super.onDestroy();
    }

    private static class EdgeGlowView
            extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        private AlphaAnimation animation;

        EdgeGlowView(
                android.content.Context context
        ) {

            super(context);

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    12f
            );

            paint.setColor(
                    Color.rgb(
                            255,
                            120,
                            0
                    )
            );

            paint.setShadowLayer(
                    35f,
                    0f,
                    0f,
                    Color.rgb(
                            255,
                            70,
                            0
                    )
            );

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );
        }

        @Override
        protected void onDraw(
                Canvas canvas
        ) {

            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();
            float p = 8f;

            canvas.drawRoundRect(
                    p,
                    p,
                    w - p,
                    h - p,
                    28f,
                    28f,
                    paint
            );
        }

        void startGlow() {

            animation =
                    new AlphaAnimation(
                            0.25f,
                            1.0f
                    );

            animation.setDuration(
                    700
            );

            animation.setRepeatMode(
                    AlphaAnimation.REVERSE
            );

            animation.setRepeatCount(
                    AlphaAnimation.INFINITE
            );

            startAnimation(
                    animation
            );

            invalidate();
        }

        void stopGlow() {

            if (animation != null) {

                clearAnimation();

                animation = null;
            }
        }
    }
    }

            
