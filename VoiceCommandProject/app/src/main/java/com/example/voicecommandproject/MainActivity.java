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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    // Initializing all IP addresses we are to work with.
    private final String aulanIpAddress = "AulanIpAdress"; // Replace with the actual IP address
    private final String libraryIpAddress = "LibraryIpAddress"; // Replace with the actual IP address

    private TextView textOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // gets its value from the TextView UI.
        textOutput= (TextView) findViewById(R.id.textOutput);
        Log.d("Tag", "onCreateMethod");

        String userCommand = "Turn on the lamp in the Library";

        sendCommandToModel(userCommand, new ModelResponseCallback() {
            @Override
            public void onResponse(String response) {
                // This will run on the main thread due to runOnUiThread inside sendCommandToModel

                String[] parts;

                // Splitting for VERB
                parts = response.split("\"VERB\",\"text\":\"");
                String verbPart = (parts.length > 1) ? parts[1] : "";
                String verb = verbPart.split("\"")[0];

                // Splitting for DEVICE
                parts = response.split("\"DEVICE\",\"text\":\"");
                String devicePart = (parts.length > 1) ? parts[1] : "";
                String device = devicePart.split("\"")[0];

                // Splitting for PLACE
                parts = response.split("\"PLACE\",\"text\":\"");
                String placePart = (parts.length > 1) ? parts[1] : "";
                String place = placePart.split("\"")[0];

                // Output the results
                textOutput.setText("Verb: " + verb + "\nDevice: " + device + "\nPlace: " + place);
                Log.d("Model Response", response);


                String ipAddress = "";
                if (place.equals("aulan"))  {
                    ipAddress = aulanIpAddress;
                }
                if (place.equals("library")) {
                    ipAddress = libraryIpAddress;
                }
                if (!ipAddress.equals("")) {
                    String combinedCommand = "Verb: " + verb + ", Device: " + device + ", Place: " + place;
                    sendCommandToRaspberryPi(ipAddress, combinedCommand);
                }
                else {
                    //textOutput.setText("Can't send request, we don't know that place");
                }

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
        // New thread to handle the network operation. ensures the UI thread is not blocked.
        new Thread(() -> {
            // Client object for making HTTP requests
            // media type for the request as JSON
            OkHttpClient client = new OkHttpClient();
            MediaType MEDIA_TYPE = MediaType.parse("application/json");
            // Model url
            String url = "https://mymodel-service-3rhl5syp2q-lz.a.run.app/ner"; // Note the /ner endpoint

            // JSON Object fto hold data
             JSONObject postData = new JSONObject();
            try {
                // Add command to JSON object
                postData.put("text", command.toLowerCase()); // Model works either way, but better with lowerCase
            } catch (JSONException e) {
                // If there's an error with JSON processing, use the callback to handle the exception on the UI thread and exit the function
                runOnUiThread(() -> callback.onFailure(e));
                return;
            }

            // Create the request body with the JSON media type and the JSON object as a string
            RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

            // Authenticate with Google Cloud and retrieve credentials
            GoogleCredentials credentials = authenticateGoogleCloud();
            // If credentials are not successfully loaded, handle the error using the callback
            if (credentials == null) {
                IOException e = new IOException("Failed to load credentials");
                runOnUiThread(() -> callback.onFailure(e));
                return;
            }

            try {
                // Fix credentials and token
                credentials.refreshIfExpired();
                AccessToken token = credentials.getAccessToken();
                String authToken = token.getTokenValue();

                // Build the HTTP request with authToken and content type headers
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("Authorization", "Bearer " + authToken)
                        .header("Content-Type", "application/json")
                        .build();

                // Execute the HTTP request and get response
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    // Response body to string
                    final String responseData = response.body().string();
                    // Use callback to handle successful response on the UI thread
                    runOnUiThread(() -> {
                        callback.onResponse(responseData);
                    });
                }
            } catch (Exception e) {
                // If there is any exception during the request execution or processing, the callback shall handle it
                runOnUiThread(() -> callback.onFailure(e));
            }
        }).start();
    }

    private void sendCommandToRaspberryPi(String ipAddress, String command) {
        // Client object for making HTTP requests
        // media type for the request as JSON
        OkHttpClient client = new OkHttpClient();
        MediaType MEDIA_TYPE = MediaType.parse("application/json");
        // Building the URL for the API endpoint, using the provided IP address.
        String url = "http://" + ipAddress + "/api_endpoint"; /// Note: '/api_endpoint' should be replaced with the actual endpoint if testing.

        // Using JSON data for more complex data to be sent. Maybe not necessary but good practice
        JSONObject postData = new JSONObject();
        try {
            // Send command as API request
            postData.put("command", command);
        } catch (JSONException e) {
            // Handle JSON exception
            return;
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        // Printing out the request details for demonstration purposes
        System.out.println("Sending API Request to " + url + " with payload " + postData.toString());
        Log.i("MyAppTag", "Sending API Request to " + url + " with payload " + postData.toString());


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // Handle failure
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Handle unsuccessful response
                }
                // Handle successful response
            }
        });
    }

}