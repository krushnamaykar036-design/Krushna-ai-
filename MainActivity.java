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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

        setContentView(
                getResources().getIdentifier(
                        "activity_main",
                        "layout",
                        getPackageName()
                )
        );

        input = findViewById(
                getResources().getIdentifier(
                        "input", "id", getPackageName()
                )
        );

        chat = findViewById(
                getResources().getIdentifier(
                        "chat", "id", getPackageName()
                )
        );

        status = findViewById(
                getResources().getIdentifier(
                        "status", "id", getPackageName()
                )
        );

        Button mic = findViewById(
                getResources().getIdentifier(
                        "mic", "id", getPackageName()
                )
        );

        Button send = findViewById(
                getResources().getIdentifier(
                        "send", "id", getPackageName()
                )
        );

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("mr", "IN"));
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
            startActivityForResult(intent, SPEECH_REQUEST);
            setStatus("Listening...");
        } catch (Exception e) {
            showOrangeGlow(false);
            setStatus("Voice unavailable");
            speak("Voice recognition उपलब्ध नाही.");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

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

                input.setText(command);
                handleCommand(command);
            }
        }
    }

    private void sendMessage() {

        if (input == null) return;

        String message = input.getText().toString().trim();

        if (message.isEmpty()) return;

        handleCommand(message);
    }

    private void handleCommand(String command) {

        addChat("You: " + command);

        String lower = command.toLowerCase(Locale.ROOT);

        if (isCallCommand(lower, command)) {
            String name = extractContactName(command);

            if (name.isEmpty()) {
                speak("कोणाला call करायचा?");
                return;
            }

            callContact(name);
            return;
        }

        if (lower.contains("hello krushna")
                || lower.contains("hello krishna")
                || command.contains("हॅलो कृष्णा")
                || command.contains("हॅलो क्रुष्णा")) {

            reply("हो, बोला.");
            return;
        }

        askServer(command);
    }

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

    private String extractContactName(String command) {

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
            name = name.replace(
                    word,
                    " "
            );
        }

        name = name
                .replace("  ", " ")
                .trim();

        return name;
    }

    private void callContact(String spokenName) {

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

            setStatus("Contacts permission required");
            return;
        }

        Cursor cursor = getContentResolver().query(
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
            reply("Contacts मिळाले नाहीत.");
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

                bestName = contactName;
                bestNumber = number;
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
                            Uri.parse("tel:" + bestNumber)
                    );

            startActivity(callIntent);

            reply(
                    bestName +
                    " ला call करत आहे."
            );

        } catch (Exception e) {

            reply("Call करता आला नाही.");
        }
    }

    private boolean contactMatches(
            String spoken,
            String saved
    ) {

        String a = normalize(spoken);
        String b = normalize(saved);

        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        if (a.contains(b) || b.contains(a)) {
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

    private String normalize(String text) {

        String value = text
                .toLowerCase(Locale.ROOT)
                .trim();

        value = devanagariToLatin(value);

        value = value
                .replaceAll("[^a-z0-9]", "");

        while (value.endsWith("aa")) {
            value = value.substring(
                    0,
                    value.length() - 1
            );
        }

        if (value.endsWith("a")
                && value.length() > 4) {

            value = value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value;
    }

    private String devanagariToLatin(String text) {

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

        for (int i = 0; i < mr.length; i++) {
            value = value.replace(
                    mr[i],
                    en[i]
            );
        }

        value = value
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

    private String consonantSkeleton(String text) {

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

        if (max == 0) return 1.0;

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
        }

        for (int i = 1;
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
                                dp[i - 1][j - 1]
                                        + cost
                        );
            }
        }

        return dp[a.length()][b.length()];
    }

    private void askServer(String message) {

        // Temporary offline-safe response.
        // Render backend connection can be added here.
        reply(
                "तुमचा message मिळाला: " +
                message
        );
    }

    private void reply(String text) {

        addChat("Krushna AI: " + text);
        speak(text);
        setStatus("Ready");
    }

    private void speak(String text) {

        if (tts != null) {

            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "krushna_reply"
            );
        }
    }

    private void addChat(String text) {

        if (chat != null) {

            String old =
                    chat.getText().toString();

            chat.setText(
                    old +
                    (old.isEmpty() ? "" : "\n\n") +
                    text
            );
        }
    }

    private void setStatus(String text) {

        if (status != null) {
            status.setText(text);
        }
    }

    private void showOrangeGlow(boolean on) {

        if (!on) {
            getWindow()
                    .getDecorView()
                    .setBackgroundColor(
                            Color.WHITE
                    );
            return;
        }

        GradientDrawable glow =
                new GradientDrawable();

        glow.setColor(Color.WHITE);
        glow.setStroke(
                12,
                Color.rgb(255, 120, 0)
        );

        getWindow()
                .getDecorView()
                .setBackground(glow);

        setStatus("🎙️ Listening...");
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

        if (requestCode == CONTACT_REQUEST) {

            Toast.makeText(
                    this,
                    "Permission मिळाल्यावर पुन्हा command बोला.",
                    Toast.LENGTH_SHORT
            ).show();
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
