package com.om.snipit.abbyy_ocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.om.snipit.R;
import com.om.snipit.activities.View_Snippet_Activity;
import com.om.snipit.classes.Constants;

import java.io.FileOutputStream;

public class AsyncTask_ProcessOCR extends AsyncTask<String, String, Boolean> {

    private ProgressDialog dialog;
    /**
     * application context.
     */
    private final View_Snippet_Activity activity;

    public AsyncTask_ProcessOCR(View_Snippet_Activity activity) {
        this.activity = activity;
        dialog = new ProgressDialog(activity);
    }

    protected void onPreExecute() {
        dialog.setMessage("Processing");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    protected void onPostExecute(Boolean result) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }

        activity.updateResults(result);
    }

    @Override
    protected Boolean doInBackground(String... args) {

        String inputFile = args[0];
        String outputFile = args[1];
        String language = args[2];

        try {
            Client restClient = new Client();

            // To create an application and obtain a password,
            // register at http://cloud.ocrsdk.com/Account/Register
            // More info on getting your application id and password at
            // http://ocrsdk.com/documentation/faq/#faq3

            // Name of application you created
            restClient.applicationId = "Snipit Android 2";
            // You should get e-mail from ABBYY Cloud OCR SDK service with the application password

            restClient.password = "6jj1l6TAq5PtC4R6rmfrZiQy";
//            restClient.password = "nDcFcmhWc5bfznqmGQKJWSSd";

            // Obtain installation id when running the application for the first time
            SharedPreferences settings = activity.getPreferences(Activity.MODE_PRIVATE);
            String instIdName = "installationId";
            if (!settings.contains(instIdName)) {
                // Get installation id from server using device id
                String deviceId = android.provider.Settings.Secure.getString(activity.getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);

                // Obtain installation id from server
                publishProgress(activity.getString(R.string.ocr_first_run_message));
                String installationId = restClient.activateNewInstallation(deviceId);
//                publishProgress("Done. Installation id is '" + installationId + "'");

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(instIdName, installationId);
                editor.apply();
            }

            String installationId = settings.getString(instIdName, "");
            restClient.applicationId += installationId;

            publishProgress(activity.getString(R.string.ocr_uploading_snippet));

            ProcessingSettings processingSettings = new ProcessingSettings();
            processingSettings.setOutputFormat(ProcessingSettings.OutputFormat.txt);
            processingSettings.setLanguage(language);

//            publishProgress("Uploading..");

            // If you want to process business cards, uncomment this
            /*
            BusCardSettings busCardSettings = new BusCardSettings();
			busCardSettings.setLanguage(language);
			busCardSettings.setOutputFormat(BusCardSettings.OutputFormat.xml);
			Task task = restClient.processBusinessCard(filePath, busCardSettings);
			*/
            Task task = restClient.processImage(inputFile, processingSettings);

            while (task.isTaskActive()) {
                // Note: it's recommended that your application waits
                // at least 2 seconds before making the first getTaskStatus request
                // and also between such requests for the same task.
                // Making requests more often will not improve your application performance.
                // Note: if your application queues several files and waits for them
                // it's recommended that you use listFinishedTasks instead (which is described
                // at http://ocrsdk.com/documentation/apireference/listFinishedTasks/).

//                Thread.sleep(5000);
                publishProgress(activity.getString(R.string.ocr_almost_done));

                task = restClient.getTaskStatus(task.Id);
            }

            if (task.Status == Task.TaskStatus.Completed) {
                publishProgress(activity.getString(R.string.ocr_downloading_result));
                FileOutputStream fos = activity.openFileOutput(outputFile, Context.MODE_PRIVATE);

                try {
                    restClient.downloadResult(task, fos);
                } finally {
                    fos.close();
                }

                publishProgress(activity.getString(R.string.ocr_setup_finished));
            } else if (task.Status == Task.TaskStatus.NotEnoughCredits) {
                throw new Exception("Not enough credits to process task. Add more pages to your application's account.");
            } else {
                throw new Exception("Task failed");
            }

            return true;
        } catch (Exception e) {
//            final String message = "Error: " + e.getMessage();
//            publishProgress(message);
            activity.displayMessage(Constants.OCR_SCAN_ERROR, activity.getString(R.string.ocr_scan_error));
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        // TODO Auto-generated method stub
        String stage = values[0];
        dialog.setMessage(stage);
        // dialog.setProgress(values[0]);
    }

}
