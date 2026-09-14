package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

    private EditText input;
    private TextView chat;
    private TextView status;
    private TextToSpeech tts;

    private static final int SPEECH_REQUEST = 100;
    private static final int CONTACT_REQUEST = 101;

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
                "input",
                "id",
                getPackageName()
        ));

        chat = findViewById(getResources().getIdentifier(
                "chat",
                "id",
                getPackageName()
        ));

        status = findViewById(getResources().getIdentifier(
                "status",
                "id",
                getPackageName()
        ));

        Button mic = findViewById(getResources().getIdentifier(
                "mic",
                "id",
                getPackageName()
        ));

        Button send = findViewById(getResources().getIdentifier(
                "send",
                "id",
                getPackageName()
        ));

        tts = new TextToSpeech(this, result -> {

            if (result == TextToSpeech.SUCCESS) {

                int languageResult =
                        tts.setLanguage(new Locale("mr", "IN"));

                if (languageResult == TextToSpeech.LANG_MISSING_DATA
                        || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {

                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        if (mic != null) {

            mic.setOnClickListener(v -> {

                showOrangeGlow(true);
                startVoiceInput();

            });
        }

        if (send != null) {

            send.setOnClickListener(v -> sendMessage());

        }

        setStatus("Ready");
    }

    // =========================
    // VOICE INPUT
    // =========================

    private void startVoiceInput() {

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
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "mr-IN"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Krushna AI ला बोला"
        );

        try {

            startActivityForResult(
                    intent,
                    SPEECH_REQUEST
            );

            setStatus("Listening...");

        } catch (Exception e) {

            showOrangeGlow(false);
            setStatus("Voice unavailable");

            speak(
                    "Voice recognition उपलब्ध नाही."
            );
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

        showOrangeGlow(false);

        if (requestCode == SPEECH_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null && !results.isEmpty()) {

                String command = results.get(0);

                if (input != null) {
                    input.setText(command);
                }

                handleCommand(command);
            }
        }
    }

    // =========================
    // SEND MESSAGE
    // =========================

    private void sendMessage() {

        if (input == null) {
            return;
        }

        String message =
                input.getText().toString().trim();

        if (message.isEmpty()) {
            return;
        }

        handleCommand(message);
    }

    // =========================
    // MAIN COMMAND SYSTEM
    // =========================

    private void handleCommand(String command) {

        addChat("You: " + command);

        String lower =
                command.toLowerCase(Locale.ROOT).trim();

        // CALL
        if (isCallCommand(lower, command)) {

            String name =
                    extractContactName(command);

            if (name.isEmpty()) {

                speak("कोणाला call करायचा?");
                return;
            }

            callContact(name);
            return;
        }

        // HOME / APP COMMAND
        if (runHomeCommand(lower, command)) {
            return;
        }

        // HELLO
        if (lower.contains("hello krushna")
                || lower.contains("hello krishna")
                || command.contains("हॅलो कृष्णा")
                || command.contains("हॅलो क्रुष्णा")) {

            reply("हो, बोला.");
            return;
        }

        // ONLINE AI
        askServer(command);
    }

    // =========================
    // HOME SCREEN RUN COMMANDS
    // =========================

    private boolean runHomeCommand(
            String lower,
            String original
    ) {

        try {

            // =====================
            // YOUTUBE
            // =====================

            if (lower.contains("youtube")
                    || original.contains("यूट्यूब")
                    || original.contains("युट्युब")) {

                Intent intent =
                        getPackageManager()
                                .getLaunchIntentForPackage(
                                        "com.google.android.youtube"
                                );

                if (intent != null) {

                    startActivity(intent);

                    reply("YouTube उघडत आहे.");

                } else {

                    reply(
                            "YouTube फोनमध्ये सापडले नाही."
                    );
                }

                return true;
            }

            // =====================
            // CHROME
            // =====================

            if (lower.contains("chrome")
                    || original.contains("क्रोम")) {

                Intent intent =
                        getPackageManager()
                                .getLaunchIntentForPackage(
                                        "com.android.chrome"
                                );

                if (intent != null) {

                    startActivity(intent);

                    reply("Chrome उघडत आहे.");

                } else {

                    reply(
                            "Chrome फोनमध्ये सापडले नाही."
                    );
                }

                return true;
            }

            // =====================
            // SETTINGS
            // =====================

            if (lower.contains("settings")
                    || original.contains("सेटिंग")
                    || original.contains("सेटिंग्स")) {

                Intent intent =
                        new Intent(
                                android.provider.Settings.ACTION_SETTINGS
                        );

                startActivity(intent);

                reply("Settings उघडत आहे.");

                return true;
            }

            // =====================
            // CALCULATOR
            // =====================

            if (lower.contains("calculator")
                    || original.contains("कॅल्क्युलेटर")) {

                Intent intent =
                        getPackageManager()
                                .getLaunchIntentForPackage(
                                        "com.google.android.calculator"
                                );

                if (intent != null) {

                    startActivity(intent);

                    reply("Calculator उघडत आहे.");

                } else {

                    reply(
                            "Calculator फोनमध्ये सापडले नाही."
                    );
                }

                return true;
            }

            // =====================
            // WHATSAPP
            // =====================

            if (lower.contains("whatsapp")
                    || original.contains("व्हॉट्सअॅप")
                    || original.contains("व्हाट्सअप")) {

                Intent intent =
                        getPackageManager()
                                .getLaunchIntentForPackage(
                                        "com.whatsapp"
                                );

                if (intent != null) {

                    startActivity(intent);

                    reply("WhatsApp उघडत आहे.");

                } else {

                    reply(
                            "WhatsApp फोनमध्ये सापडले नाही."
                    );
                }

                return true;
            }

            // =====================
            // INSTAGRAM
            // =====================

            if (lower.contains("instagram")
                    || original.contains("इन्स्टाग्राम")) {

                Intent intent =
                        getPackageManager()
                                .getLaunchIntentForPackage(
                                        "com.instagram.android"
                                );

                if (intent != null) {

                    startActivity(intent);

                    reply("Instagram उघडत आहे.");

                } else {

                    reply(
                            "Instagram फोनमध्ये सापडले नाही."
                    );
                }

                return true;
            }

            // =====================
            // CAMERA
            // =====================

            if (lower.contains("camera")
                    || original.contains("कॅमेरा")) {

                Intent intent =
                        new Intent(
                                android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                        );

                try {

                    startActivity(intent);

                    reply("Camera उघडत आहे.");

                } catch (Exception e) {

                    reply("Camera उघडता आला नाही.");
                }

                return true;
            }

            // =====================
            // GALLERY
            // =====================

            if (lower.contains("gallery")
                    || original.contains("गॅलरी")) {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW
                        );

                intent.setType("image/*");
                intent.setData(
                        Uri.parse("content://media/internal/images/media")
                );

                try {

                    startActivity(intent);

                    reply("Gallery उघडत आहे.");

                } catch (Exception e) {

                    Intent fallback =
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "content://media/external/images/media"
                                    )
                            );

                    try {

                        startActivity(fallback);

                        reply("Gallery उघडत आहे.");

                    } catch (Exception ignored) {

                        reply(
                                "Gallery उघडता आली नाही."
                        );
                    }
                }

                return true;
            }

            // =====================
            // PHONE
            // =====================

            if (lower.equals("phone")
                    || lower.contains("open phone")
                    || original.contains("फोन उघड")) {

                Intent intent =
                        new Intent(
                                Intent.ACTION_DIAL
                        );

                startActivity(intent);

                reply("Phone उघडत आहे.");

                return true;
            }

            return false;

        } catch (Exception e) {

            reply(
                    "Command चालवता आला नाही."
            );

            return true;
        }
    }

    // =========================
    // CALL COMMAND
    // =========================

    private boolean isCallCommand(
            String lower,
            String original
    ) {

        return lower.contains("call")
                || lower.contains("phone")
                || original.contains("कॉल")
                || original.contains("फोन")
                || original.contains("ला कॉल")
                || original.contains("ला फोन");
    }

    private String extractContactName(
            String command
    ) {

        String name = command;

        String[] removeWords = {

                "call",
                "phone",
                "कर",
                "करा",
                "करायचा",
                "करायचाय",
                "लाव",
                "लावा",
                "ला",
                "वर",
                "फोन",
                "कॉल",
                "नंबर",
                "number",
                "please",
                "pls",
                "कृपया",
                "मला",
                "मी",
                "तू",
                "करायचा आहे"
        };

        for (String word : removeWords) {

            name =
                    name.replace(
                            word,
                            " "
                    );
        }

        return name
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private void callContact(
            String spokenName
    ) {

        if (checkSelfPermission(
                Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.CALL_PHONE
                    },
                    CONTACT_REQUEST
            );

            setStatus(
                    "Contacts permission required"
            );

            return;
        }

        Cursor cursor =
                getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,

                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        },

                        null,
                        null,
                        null
                );

        if (cursor == null) {

            reply(
                    "Contacts मिळाले नाहीत."
            );

            return;
        }

        String bestName = null;
        String bestNumber = null;

        while (cursor.moveToNext()) {

            String contactName =
                    cursor.getString(0);

            String number =
                    cursor.getString(1);

            if (contactMatches(
                    spokenName,
                    contactName
            )) {

                bestName =
                        contactName;

                bestNumber =
                        number;

                break;
            }
        }

        cursor.close();

        if (bestNumber == null) {

            reply(
                    "मला " +
                    spokenName +
                    " नावाचा contact सापडला नाही."
            );

            return;
        }

        if (checkSelfPermission(
                Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.CALL_PHONE
                    },
                    CONTACT_REQUEST
            );

            return;
        }

        try {

            Intent callIntent =
                    new Intent(
                            Intent.ACTION_CALL,
                            Uri.parse(
                                    "tel:" + bestNumber
                            )
                    );

            startActivity(callIntent);

            reply(
                    bestName +
                    " ला call करत आहे."
            );

        } catch (Exception e) {

            reply(
                    "Call करता आला नाही."
            );
        }
    }

    // =========================
    // CONTACT MATCHING
    // =========================

    private boolean contactMatches(
            String spoken,
            String saved
    ) {

        String a =
                normalize(spoken);

        String b =
                normalize(saved);

        if (a.isEmpty()
                || b.isEmpty()) {

            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        if (a.contains(b)
                || b.contains(a)) {

            return true;
        }

        String aSkeleton =
                consonantSkeleton(a);

        String bSkeleton =
                consonantSkeleton(b);

        if (!aSkeleton.isEmpty()
                && aSkeleton.equals(bSkeleton)) {

            return true;
        }

        return similarity(a, b) >= 0.65;
    }

    private String normalize(
            String text
    ) {

        String value =
                text.toLowerCase(
                        Locale.ROOT
                ).trim();

        value =
                devanagariToLatin(value);

        value =
                value.replaceAll(
                        "[^a-z0-9]",
                        ""
                );

        while (value.endsWith("aa")) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        if (value.endsWith("a")
                && value.length() > 4) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        return value;
    }

    private String devanagariToLatin(
            String text
    ) {

        String value = text
             ) {

        String value = text;

        String[] mr = {

                "अ","आ","इ","ई","उ","ऊ","ए","ऐ","ओ","औ",
                "क","ख","ग","घ","च","छ","ज","झ",
                "ट","ठ","ड","ढ","ण",
                "त","थ","द","ध","न",
                "प","फ","ब","भ","म",
                "य","र","ल","व",
                "श","ष","स","ह",
                "ं","ः","्"
        };

        String[] en = {

                "a","aa","i","ee","u","oo","e","ai","o","au",
                "k","kh","g","gh","ch","chh","j","jh",
                "t","th","d","dh","n",
                "t","th","d","dh","n",
                "p","ph","b","bh","m",
                "y","r","l","v",
                "sh","sh","s","h",
                "n","h",""
        };

        for (int i = 0;
             i < mr.length;
             i++) {

            value =
                    value.replace(
                            mr[i],
                            en[i]
                    );
        }

        value =
                value
                        .replace("ा", "aa")
                        .replace("ि", "i")
                        .replace("ी", "ee")
                        .replace("ु", "u")
                        .replace("ू", "oo")
                        .replace("े", "e")
                        .replace("ै", "ai")
                        .replace("ो", "o")
                        .replace("ौ", "au");

        return value;
    }

    private String consonantSkeleton(
            String text
    ) {

        return text.replaceAll(
                "[aeiou]",
                ""
        );
    }

    private double similarity(
            String a,
            String b
    ) {

        int distance =
                levenshtein(a, b);

        int max =
                Math.max(
                        a.length(),
                        b.length()
                );

        if (max == 0) {
            return 1.0;
        }

        return 1.0 -
                ((double) distance / max);
    }
    return 1.0 -
                ((double) distance / max);
    }

    private int levenshtein(
            String a,
            String b
    ) {

        int[][] dp =
                new int[
                        a.length() + 1
                ][
                        b.length() + 1
                ];

        for (int i = 0;
             i <= a.length();
             i++) {

            dp[i][0] = i;
        }

        for (int j = 0;
             j <= b.length();
             j++) {

            dp[0][j] = j;
        }for (int i = 1;
             i <= a.length();
             i++) {

            for (int j = 1;
                 j <= b.length();
                 j++) {

                int cost =
                        a.charAt(i - 1)
                                == b.charAt(j - 1)
                                ? 0 : 1;

                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j] + 1,
                                        dp[i][j - 1] + 1
                                ),
                                dp[i - 1][j - 1] + cost
                        );
            }
        }

        return dp[
                a.length()
        ][
                b.length()
        ];
    }

    // =========================
    // ONLINE AI
    // =========================

    private void askServer(
            String message
    ) {

        setStatus(
                "Online AI thinking..."
        );

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(
                                "https://krushna-ai-hseh.onrender.com/chat"
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "POST"
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );
                
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setDoOutput(true);

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        30000
                );

                JSONObject request =
                        new JSONObject();

                request.put(
                        "message",
                        message
                );

                byte[] data =
                        request.toString()
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

                if (stream == null) {

                    throw new Exception(
                            "Empty server response"
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream,
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line =
                        reader.readLine()) != null) {

                    response.append(line);
                }

                reader.close();

                String raw =
                        response.toString().trim();

                String finalReply =
                        parseServerReply(raw);

                if (finalReply.isEmpty()) {

                    finalReply =
                            "AI कडून उत्तर मिळाले नाही.";
                }

                final String answer =
                        finalReply;

                runOnUiThread(() -> {

                    setStatus(
                            "Online AI Ready"
                    );

                    reply(answer);

                });

            } catch (Exception e) {

                String error =
                        e.getMessage();

                if (error == null
                        || error.isEmpty()) {

                    error =
                            "Unknown connection error";
                }

                final String finalError =
                        error;
                
        );

                    reply(
                            "Online AI Error: "
                                    + finalError
                    );
                });

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }

    private String parseServerReply(
            String raw
    ) {

        try {

            JSONObject object =
                    new JSONObject(raw);

            if (object.has("reply")) {

                return object.getString(
                        "reply"
                );
            }

            if (object.has("response")) {

                return object.getString(
                        "response"
                );
            }

            if (object.has("message")) {

                return object.getString(
                        "message"
                );
            }

            if (object.has("answer")) {

                return object.getString(
                        "answer"
                );
                }

        } catch (Exception ignored) {
        }

        return raw;
    }

    // =========================
    // CHAT
    // =========================

    private void addChat(
            String message
    ) {

        if (chat == null) {
            return;
        }

        String old =
                chat.getText()
                        .toString();

        if (old.isEmpty()) {

            chat.setText(message);

        } else {

            chat.setText(
                    old
                            + "\n\n"
                            + message
            );
        }
    }

    // =========================
    // REPLY + SPEECH
    // =========================

    private void reply(
            String message
    ) {

        addChat(
                "Krushna AI: "
                        + message
        );

        setStatus("Ready");

        speak(message);
    }

    private void speak(
            String message
    ) {
        
        if (tts == null) {
            return;
        }

        try {

            tts.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "KRUSHNA_AI_REPLY"
            );

        } catch (Exception ignored) {
        }
    }

    // =========================
    // STATUS
    // =========================

    private void setStatus(
            String message
    ) {

        if (status != null) {

            status.setText(message);
        }
    }

    // =========================
    // MIC GLOW
    // =========================

    private void showOrangeGlow(
            boolean enabled
    ) {

        if (status == null) {
            return;
        }

        if (enabled) {

            GradientDrawable background =
                    new GradientDrawable();

            background.setColor(
                    Color.rgb(
                            255,
                            140,
                            0
                    )
            );

            background.setCornerRadius(30);

            status.setBackground(
                    background
            );

            status.setTextColor(
                    Color.WHITE
            );

        } else {

            status.setBackgroundColor(
                    Color.TRANSPARENT
            );

            status.setTextColor(
                    Color.WHITE
            );
        }
    }

    // =========================
    // PERMISSIONS
    // =========================

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

        if (requestCode == CONTACT_REQUEST) {

            Toast.makeText(
                    this,
                    "Permission मिळाले. आता command पुन्हा बोला.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================
    // DESTROY
    // =========================

    @Override
    protected void onDestroy() {

        if (tts != null) {

            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
