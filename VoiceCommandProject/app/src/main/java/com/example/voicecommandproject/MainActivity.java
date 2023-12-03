package com.example.voicecommandproject;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ActivityNotFoundException;
import android.os.Bundle;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

// Google Cloud NLP API
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;
import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.Entity;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control);

        // Google Cloud NLP API connections
        /*
        LanguageServiceSettings settings = LanguageServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                .build();

        try (LanguageServiceClient language = LanguageServiceClient.create(settings)) {
            Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
            AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder()
                    .setDocument(doc)
                    .build();
            AnalyzeEntitiesResponse response = language.analyzeEntities(request);

            for (Entity entity : response.getEntitiesList()) {
                System.out.printf("Entity: %s", entity.getName());
            }
        }
        */

    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_RECOGNITION);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Sorry, your device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }



    private void processCommand(String command) {
        if (command.toLowerCase().contains("turn on the lamp")) {
            turnOnLamp();
        } else if (command.toLowerCase().contains("turn off the lamp")) {
            turnOffLamp();
        }
    }

    private void turnOnLamp() {
        // Code to turn on the lamp
        statusTextView.setText("Turning on the lamp... ");

        // Add code from labs

    }

    private void turnOffLamp() {
        // Code to turn off the lamp
        statusTextView.setText("Turning off the lamp... ");

        // Add code from labs
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    public void run (String command) {
        String hostname = "hostname";
        String username = "username";
        String password = "password";
        try
        {
            Connection conn = new Connection(hostname); //init connection
            conn.connect(); //start connection to the hostname
            boolean isAuthenticated = conn.authenticateWithPassword(username,
                    password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            //reads text
            while (true){
                String line = br.readLine(); // read line
                if (line == null)
                    break;
                System.out.println(line);
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        }
        catch (IOException e)
        { e.printStackTrace(System.err);
            System.exit(2); }
    }


    private void authenticateGoogleCloud() {
        try {
            // Load the service account key JSON file
            InputStream serviceAccount = getResources().openRawResource(R.raw.authentication_key);

            // Authenticate with the service account
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccount);

            // Use the credentials to initialize the client
            LanguageServiceSettings settings = LanguageServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            LanguageServiceClient languageServiceClient = LanguageServiceClient.create(settings);

            // Now you can use languageServiceClient to access the API

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}