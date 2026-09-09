package com.example.aiassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    TextView chat;
    EditText input;
    TextToSpeech tts;
    final int VOICE = 10;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        chat = findViewById(R.id.chat);
        input = findViewById(R.id.input);

        findViewById(R.id.send).setOnClickListener(v -> send());
        findViewById(R.id.mic).setOnClickListener(v -> voice());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("mr", "IN"));
            }
        });
    }

    void send() {
        String q = input.getText().toString().trim();

        if (q.isEmpty()) return;

        chat.append("You: " + q + "\n");

        String answer = getOfflineReply(q);

        chat.append("Krishna AI: " + answer + "\n\n");

        input.setText("");
        speak(answer);
    }

    String getOfflineReply(String text) {

        String q = text.toLowerCase(Locale.ROOT).trim();

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

        return "Internet नसल्यामुळे मी सध्या offline mode मध्ये आहे.";
    }

    void voice() {

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    20
            );
            return;
        }

        startVoice();
    }

    void startVoice() {

        Intent i = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        startActivityForResult(i, VOICE);
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

        if (requestCode == 20 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

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

            if (results != null && !results.isEmpty()) {

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
