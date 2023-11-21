package com.example.voicecommandproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import java.util.ArrayList;

public class VoiceControlActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            // A lot of methods and we probably don't need all of them
            // We can set textfield or other application feedback on some of these.

            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    processCommand(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }

            // Other overridden methods of RecognitionListener...
        });

        // Start listening to voice commands
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer.startListening(intent);
    }

    private void processCommand(String command) {
        if (command.toLowerCase().contains("turn on the lamp")) {
            turnOnLamp();
        } else if (command.toLowerCase().contains("turn off the lamp")) {
            turnOffLamp();
        }
    }

    private void turnOnLamp() {
        // Code to turn on the lamp, from

    }

    private void turnOffLamp() {
        // Code to turn off the lamp
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
