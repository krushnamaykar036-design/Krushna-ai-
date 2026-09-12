package com.example.aiassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service {

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;

    private static final String CHANNEL_ID = "krushna_ai_voice";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Krushna AI")
                .setContentText("Voice service चालू आहे")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("mr", "IN"));
            }
        });

        startListening();
    }

    private void startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Voice recognition उपलब्ध नाही.");
            return;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(android.os.Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
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
                restartListening();
            }

            @Override
            public void onResults(android.os.Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {

                    String command = matches.get(0);

                    String lower = command.toLowerCase(Locale.ROOT);

                    if (lower.contains("hello krushna")
                            || lower.contains("hello krishna")
                            || command.contains("हॅलो कृष्णा")
                            || command.contains("हॅलो क्रुष्णा")) {

                        speak("हो, बोला.");

                        Intent intent = new Intent(
                                VoiceService.this,
                                MainActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(intent);
                    }
                }

                restartListening();
            }

            @Override
            public void onPartialResults(android.os.Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, android.os.Bundle params) {
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

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
            restartListening();
        }
    }

    private void restartListening() {

        new android.os.Handler(
                android.os.Looper.getMainLooper()
        ).postDelayed(() -> {

            if (speechRecognizer != null) {
                startListening();
            }

        }, 1000);
    }

    private void speak(String text) {

        if (textToSpeech != null) {
            textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "krushna_voice"
            );
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Krushna AI Voice",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Krushna AI voice service"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
            }
