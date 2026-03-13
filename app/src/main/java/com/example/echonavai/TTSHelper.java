package com.example.echonavai;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TTSHelper {

    private TextToSpeech textToSpeech = null;
    private boolean isReady = false;

    public TTSHelper(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                isReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    public void speak(String text) {
        if (isReady) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ECHONAV_TTS");
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}