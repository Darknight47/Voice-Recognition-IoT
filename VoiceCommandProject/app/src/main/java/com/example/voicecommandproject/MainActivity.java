package com.example.voicecommandproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.os.Bundle;

import android.speech.RecognizerIntent;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.util.Log;
import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

// Google Cloud API

import com.google.auth.oauth2.AccessToken;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private TextView textOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        // gets its value from the TextView UI.
        textOutput= (TextView) findViewById(R.id.textOutput);
        Log.d("Tag", "onCreateMethod");

        String userCommand = "Turn on the lights in Aulan";
        sendCommandToModel(userCommand, new ModelResponseCallback() {
            @Override
            public void onResponse(String response) {
                // This will run on the main thread due to runOnUiThread inside sendCommandToModel
                textOutput.setText(response);
                Log.d("Model Response", response);
            }

            @Override
            public void onFailure(Exception e) {
                // Handle the error, e.g., show a toast or update the UI
                Log.e("Model Error", "Failed to send command", e);
            }
        });
    }

    public interface ModelResponseCallback {
        void onResponse(String response);
        void onFailure(Exception e);
    }


    public void onClick(View v)
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        try {
            //startActivityForResult(intent, REQUEST_CODE); DEPRECATED
            speechResultLauncher.launch(intent);
        } catch (Exception e) {
            // TODO: handle exception
            Log.d("onClickTag", "Could Not Run the Listening Activity");
        }
    }

    ActivityResultLauncher<Intent> speechResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result ->  {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
            {
                // Get the list of spoken words
                ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                // Get the first match, which is the most likely thing that was said
                if (matches != null && !matches.isEmpty()) 
                {
                    String spokenText = matches.get(0);
                    // Update your TextView or perform other actions with the spoken text
                    textOutput.setText(spokenText);
                    Log.d("The Text IS: ", spokenText);
                }
            }
        }
    );

    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //Handling the result
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode)
        {
            case REQUEST_CODE:
            {
                if(resultCode == RESULT_OK && null != data)
                {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //Updating textOutput here.
                    Log.d("The speech: ", result.get(0));
                }
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
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

    private GoogleCredentials authenticateGoogleCloud() {
        try {
            // Load the service account key JSON file
            InputStream serviceAccount = getResources().openRawResource(R.raw.authentication_key);

            // Authenticate with the service account
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccount)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

            return credentials;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendCommandToModel(String command, ModelResponseCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            MediaType MEDIA_TYPE = MediaType.parse("application/json");
            String url = "https://mymodel-service-3rhl5syp2q-lz.a.run.app/ner"; // Note the /ner endpoint

            JSONObject postData = new JSONObject();
            try {
                postData.put("text", command);
            } catch (JSONException e) {
                // Handle JSON exception
                runOnUiThread(() -> callback.onFailure(e));
                return;
            }

            RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

            GoogleCredentials credentials = authenticateGoogleCloud();
            if (credentials == null) {
                IOException e = new IOException("Failed to load credentials");
                runOnUiThread(() -> callback.onFailure(e));
                return;
            }

            try {
                credentials.refreshIfExpired();
                AccessToken token = credentials.getAccessToken();
                String authToken = token.getTokenValue();

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("Authorization", "Bearer " + authToken)
                        .header("Content-Type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    final String responseData = response.body().string();
                    runOnUiThread(() -> {
                        callback.onResponse(responseData);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> callback.onFailure(e));
            }
        }).start();
    }



}