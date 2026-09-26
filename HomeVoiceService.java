package com.example.aiassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class HomeVoiceService extends Service {

    private static final String CHANNEL_ID = "KRUSHNA_VOICE";

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Krushna AI")
                        .setContentText("Krushna voice assistant active")
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .build();

        startForeground(2001, notification);

        startListening();
    }

    private void startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf();
            return;
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onResults(Bundle results) {

                        ArrayList<String> list =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (list != null && !list.isEmpty()) {
                            handleCommand(list.get(0));
                        }

                        restartListening();
                    }

                    @Override
                    public void onError(int error) {
                        restartListening();
                    }

                    @Override
                    public void onReadyForSpeech(Bundle params) {}

                    @Override
                    public void onBeginningOfSpeech() {}

                    @Override
                    public void onRmsChanged(float rmsdB) {}

                    @Override
                    public void onBufferReceived(byte[] buffer) {}

                    @Override
                    public void onEndOfSpeech() {}

                    @Override
                    public void onPartialResults(Bundle results) {}

                    @Override
                    public void onEvent(int eventType, Bundle params) {}
                }
        );

        recognizerIntent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN"
        );

        speechRecognizer.startListening(recognizerIntent);
    }

    private void restartListening() {

        if (speechRecognizer == null) {
            return;
        }

        speechRecognizer.cancel();

        new android.os.Handler(
                getMainLooper()
        ).postDelayed(
                this::startListening,
                700
        );
    }

    private void handleCommand(String command) {

        if (command == null) {
            return;
        }

        String text =
                command
                        .toLowerCase(Locale.ROOT)
                        .trim();

        if (!text.contains("hey krushna") &&
                !text.contains("hey krishna") &&
                !text.contains("krushna")) {
            return;
        }

        if (text.contains("whatsapp")) {
            openApp("com.whatsapp");
            return;
        }

        if (text.contains("instagram")) {
            openApp("com.instagram.android");
            return;
        }

        if (text.contains("youtube") ||
                text.contains("you tube")) {
            openApp("com.google.android.youtube");
            return;
        }

        if (text.contains("chrome")) {
            openApp("com.android.chrome");
            return;
        }

        if (text.contains("camera")) {

            Intent intent =
                    new Intent(
                            android.provider.MediaStore
                                    .ACTION_IMAGE_CAPTURE
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            try {
                startActivity(intent);
            } catch (Exception ignored) {}

            return;
        }

        if (text.contains("settings")) {

            Intent intent =
                    new Intent(
                            android.provider.Settings
                                    .ACTION_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            try {
                startActivity(intent);
            } catch (Exception ignored) {}

            return;
        }

        if (text.contains("home")) {

            Intent intent =
                    new Intent(Intent.ACTION_MAIN);

            intent.addCategory(
                    Intent.CATEGORY_HOME
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            try {
                startActivity(intent);
            } catch (Exception ignored) {}

            return;
        }
    }

    private void openApp(String packageName) {

        try {

            Intent intent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    packageName
                            );

            if (intent != null) {

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);
            }

        } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Krushna AI Voice",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
