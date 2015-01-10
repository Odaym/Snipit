package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ResultsActivity extends Activity {

    String outputPath;
    EditText et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        et = new EditText(this);
        setContentView(et);

        String imageUrl = "unknown";

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            imageUrl = extras.getString("IMAGE_PATH");
            outputPath = extras.getString("RESULT_PATH");
        }

        // Starting recognition process
        new OCRImageAsyncTask(this).execute(imageUrl, outputPath);
    }

    public void updateResults(Boolean success) {
        if (!success)
            return;
        try {
            StringBuffer contents = new StringBuffer();

            FileInputStream fis = openFileInput(outputPath);
            try {
                Reader reader = new InputStreamReader(fis, "UTF-8");
                BufferedReader bufReader = new BufferedReader(reader);
                String text = null;
                while ((text = bufReader.readLine()) != null) {
                    contents.append(text).append(System.getProperty("line.separator"));
                }
            } finally {
                fis.close();
            }

            displayMessage(contents.toString());
        } catch (Exception e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    public void displayMessage(String text) {
        et.post(new MessagePoster(text));
    }

    class MessagePoster implements Runnable {
        private final String _message;

        public MessagePoster(String message) {
            _message = message;
        }

        public void run() {
            et.append(_message + "\n");
            setContentView(et);
        }
    }
}
