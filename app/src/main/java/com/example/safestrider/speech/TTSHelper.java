package com.example.safestrider.speech;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TTSHelper {

    private TextToSpeech textToSpeech;
    private boolean isReady = false;
    private boolean isMuted = false;

    private String lastSpokenText = "";
    private long lastSpeechTime = 0L;

    private static final long MIN_SPEECH_GAP_MS = 3000;
    private static final long SAME_MESSAGE_BLOCK_MS = 7000;

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
        if (!canSpeak(text)) return;

        lastSpeechTime = System.currentTimeMillis();
        lastSpokenText = text;

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "SAFE_STRIDER_TTS"
        );
    }

    public void speakPriority(String text) {
        if (!isReady || isMuted || text == null || text.trim().isEmpty()) return;

        lastSpeechTime = System.currentTimeMillis();
        lastSpokenText = text;

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "SAFE_STRIDER_PRIORITY_TTS"
        );
    }

    private boolean canSpeak(String text) {
        if (!isReady || isMuted || text == null || text.trim().isEmpty()) return false;

        long now = System.currentTimeMillis();

        if (now - lastSpeechTime < MIN_SPEECH_GAP_MS) {
            return false;
        }

        if (text.equalsIgnoreCase(lastSpokenText)
                && now - lastSpeechTime < SAME_MESSAGE_BLOCK_MS) {
            return false;
        }

        return true;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
        if (muted && textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}