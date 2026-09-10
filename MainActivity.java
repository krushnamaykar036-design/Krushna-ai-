boolean handleCommand(String q) {
    String text = q.toLowerCase(Locale.ROOT);

    try {
        // Chrome
        if (text.contains("chrome") || text.contains("क्रोम")) {
            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage("com.android.chrome");

            if (intent != null) {
                startActivity(intent);
                speak("Chrome उघडत आहे");
                return true;
            }
        }

        // WhatsApp
        if (text.contains("whatsapp") ||
            text.contains("व्हाट्सएप") ||
            text.contains("व्हॉट्सअॅप")) {

            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage("com.whatsapp");

            if (intent != null) {
                startActivity(intent);
                speak("WhatsApp उघडत आहे");
                return true;
            }
        }

        // Camera
        if (text.contains("camera") ||
            text.contains("कॅमेरा") ||
            text.contains("कॅमेरा उघड")) {

            Intent intent = new Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE
            );

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                speak("Camera उघडत आहे");
                return true;
            }
        }

        // Settings
        if (text.contains("settings") ||
            text.contains("सेटिंग") ||
            text.contains("सेटिंग्स")) {

            Intent intent = new Intent(
                    android.provider.Settings.ACTION_SETTINGS
            );

            startActivity(intent);
            speak("Settings उघडत आहे");
            return true;
        }

    } catch (Exception e) {
        speak("App उघडता आला नाही");
        return true;
    }

    return false;
}
