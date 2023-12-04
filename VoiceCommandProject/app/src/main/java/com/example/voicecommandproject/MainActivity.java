package com.example.voicecommandproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.ActivityNotFoundException;
import android.os.Bundle;

import android.speech.RecognizerIntent;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;



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

    private static final int REQUEST_CODE = 100;
    private TextView textOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control);
        //TextOutput gets its value from TextView in layout.
        textOutput = (TextView) findViewById(R.id.textOutput);
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
                if(resultCode == RESULT_OK)
                {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("The speech has been catched: ", result.get(0));
                }
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