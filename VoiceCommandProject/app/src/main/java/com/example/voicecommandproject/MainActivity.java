package com.example.voicecommandproject;

import android.Manifest;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ActivityNotFoundException;


import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VOICE_RECOGNITION = 1;
    private SpeechRecognizer speechRecognizer;
    private LanguageServiceClient languageServiceClient;
    private TextView statusTextView;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control);

        // Initialize UI components
        initializeUI();

        // Initialize Google Cloud NLP API
        authenticateGoogleCloud();

        // Initialize SpeechRecognizer
        initializeSpeechRecognizer();

        // Test for microphone permission
        requestMicrophonePermission();

    }

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            // Permission granted. Microphone enabled
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted. Able to use the microphone.
                } else {
                    // Permission denied. Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void initializeUI() {
        statusTextView = findViewById(R.id.statusTextView);
        startButton = findViewById(R.id.startVoiceRecognitionButton);

        // Start voice recognition when the button is pressed
        startButton.setOnClickListener(v -> startVoiceRecognition());
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                statusTextView.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() { }

            @Override
            public void onRmsChanged(float rmsdB) { }

            @Override
            public void onBufferReceived(byte[] buffer) { }

            @Override
            public void onEndOfSpeech() { }

            @Override
            public void onError(int error) {
                statusTextView.setText("Error encountered, please try again.");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processCommand(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) { }

            @Override
            public void onEvent(int eventType, Bundle params) { }
        });
    }

    /*
    // v1 with pop up Google
    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_RECOGNITION);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Speech input not supported", Toast.LENGTH_SHORT).show();
        }
    }
    */
    // v2 with pop up Google
    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        // No longer use the EXTRA_PROMPT, as we won't be showing the Google prompt
        // intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        speechRecognizer.startListening(intent);
        statusTextView.setText("Listening...");
    }

    private void analyzeTextWithNLP(String text) {
        try {
            Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

            AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder()
                    .setDocument(doc)
                    .build();

            AnalyzeEntitiesResponse response = languageServiceClient.analyzeEntities(request);

            for (Entity entity : response.getEntitiesList()) {
                // Here, we should determine the intent based on entity analysis
                // Idea:
                if (entity.getName().toLowerCase().contains("lamp")) {
                    processCommand(entity.getName());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void authenticateGoogleCloud() {
        try {
            InputStream serviceAccount = getResources().openRawResource(R.raw.authentication_key);
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccount);
            LanguageServiceSettings settings = LanguageServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            languageServiceClient = LanguageServiceClient.create(settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }


    // Additional methods for processing commands, interacting with hardware, etc.
    // ...
    private void processCommand(String command) {
        if (command.toLowerCase().contains("turn on the lamp")) {
            turnOnLamp();
        } else if (command.toLowerCase().contains("turn off the lamp")) {
            turnOffLamp();
        } else {
            statusTextView.setText("Command not recognized");
        }
    }

    private void turnOnLamp() {
        // Code to turn on the lamp
        statusTextView.setText("Turning on the lamp...");

        // Add code to actually turn on the lamp
        // Sending commands to Raspberry Pi or other controller
        // Maybe use the run method you have defined
        // run("turn_on_lamp_command");
    }

    private void turnOffLamp() {
        // Code to turn off the lamp
        statusTextView.setText("Turning off the lamp...");

        // run("turn_off_lamp_command");
    }


}
