package com.example.aiassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class HomeVoiceService extends Service {

    private static final String CHANNEL_ID = "KRUSHNA_VOICE";
    private static final int NOTIFICATION_ID = 2001;

    private SpeechRecognizer recognizer;
    private Handler handler;
    private boolean listening = false;
    private boolean destroyed = false;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(getMainLooper());
        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("KRUSHNA AI")
                        .setContentText("RED DEVIL - Voice Assistant Active")
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .setOngoing(true)
                        .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startListening();
    }

    private void startListening() {
        if (destroyed || listening) return;

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf();
            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {
                listening = false;

                ArrayList<String> list =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (list != null && !list.isEmpty()) {
                    handleCommand(list.get(0));
                }

                restartListening();
            }

            @Override
            public void onError(int error) {
                listening = false;
                restartListening();
            }

            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rms) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int event, Bundle b) {}
        });

        Intent intent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN");

        intent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false);

        try {
            listening = true;
            recognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            restartListening();
        }
    }

    private void restartListening() {
        if (destroyed || handler == null) return;

        handler.postDelayed(() -> {
            if (!destroyed && !listening) {
                startListening();
            }
        }, 1500);
    }

    private void handleCommand(String command) {
        if (command == null) return;

        String text = command.toLowerCase(Locale.ROOT).trim();

        if (text.contains("hey krushna")
                || text.contains("hey krishna")
                || text.contains("ok krushna")
                || text.contains("okay krushna")) {

            String app = text
                    .replace("hey krushna", "")
                    .replace("hey krishna", "")
                    .replace("ok krushna", "")
                    .replace("okay krushna", "")
                    .replace("open", "")
                    .trim();

            if (app.isEmpty()) {
                speak("Yes, Krushna AI is ready");
                return;
            }

            if (app.contains("whatsapp")) {
                openApp("com.whatsapp");
            } else if (app.contains("instagram")) {
                openApp("com.instagram.android");
            } else if (app.contains("youtube")) {
                openApp("com.google.android.youtube");
            } else if (app.contains("chrome")) {
                openApp("com.android.chrome");
            } else if (app.contains("camera")) {
                openCamera();
            } else if (app.contains("settings")) {
                openSettings();
            } else {
                speak("App command received");
            }
        }
    }

    private void speak(String message) {
        Intent intent = new Intent(this, MainActivity.class);

        intent.putExtra("KRUSHNA_MESSAGE", message);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        try {
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void openApp(String packageName) {
        try {
            Intent intent =
                    getPackageManager()
                            .getLaunchIntentForPackage(packageName);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                speak("App not installed");
            }
        } catch (Exception ignored) {}
    }

    private void openCamera() {
        try {
            Intent intent =
                    new Intent(
                            android.provider.MediaStore
                                    .ACTION_IMAGE_CAPTURE);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void openSettings() {
        try {
            Intent intent =
                    new Intent(
                            android.provider.Settings.ACTION_SETTINGS);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Krushna AI Voice",
                            NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        destroyed = true;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

