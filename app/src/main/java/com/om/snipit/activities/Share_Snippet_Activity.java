package com.om.snipit.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.andreabaccega.widget.FormEditText;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class Share_Snippet_Activity extends Base_Activity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.titleET)
    FormEditText book_title_ET;
    @Bind(R.id.authorET)
    FormEditText book_author_ET;
    @Bind(R.id.snippetNameET)
    FormEditText snippetNameET;
    @Bind(R.id.screenNameET)
    FormEditText screenNameET;
    @Bind(R.id.snippetIMG)
    ImageView snippetIMG;

    private ArrayList<FormEditText> allFields = new ArrayList<>();

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    private ProgressDialog uploadingSnippets_AWS;

    private Book fromBook;
    private Snippet snippet;
    private Helper_Methods helperMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_snippet);

        ButterKnife.bind(this);

        EventBus_Singleton.getInstance().register(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        helperMethods = new Helper_Methods(this);

        allFields.add(snippetNameET);
        allFields.add(screenNameET);

        toolbar.setTitle(R.string.share_snippet_activity_title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        setSupportActionBar(toolbar);
        helperMethods.setUpActionbarColors(this, Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS);

        snippet = getIntent().getParcelableExtra(Constants.EXTRAS_SNIPPET);
        fromBook = getIntent().getParcelableExtra(Constants.EXTRAS_BOOK);

        book_author_ET.setText(fromBook.getAuthor());
        book_title_ET.setText(fromBook.getTitle());

        screenNameET.setText(prefs.getString(Constants.USER_LAST_USED_SCREEN_NAME, ""));

        snippetNameET.setText(snippet.getName());

        Picasso.with(this).load(new File(snippet.getImage_path())).into(snippetIMG);

        snippetNameET.requestFocus();
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        switch (ebp.getMessage()) {
            case "amazon_exception_connection_too_slow":
                Crouton.makeText(Share_Snippet_Activity.this, getString(R.string.amazon_upload_error_connection_too_slow), Style.ALERT).show();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_snippet, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_share_snippet:
                if (helperMethods.validateFields(allFields)) {
                    if (Helper_Methods.isInternetAvailable(this)) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                        final ProgressDialog checkingExistingSnippets = ProgressDialog.show(this, getString(R.string.progress_checking_existing_snippets_title), getString(R.string.progress_checking_existing_snippets_message), true, false);

                        snippet.setScreen_name(screenNameET.getText().toString());

                        final File file = new File(snippet.getImage_path());

                        ParseQuery checkIfSnippetExists = new ParseQuery("Shared_Snippet");
                        checkIfSnippetExists.whereEqualTo("snippet_id", snippet.getId()).whereEqualTo("name", snippetNameET.getText().toString()).whereEqualTo("screen_name", snippet.getScreen_name()).whereEqualTo("snippet_file_size", file.length());

                        checkIfSnippetExists.getFirstInBackground(new GetCallback<ParseObject>() {
                            public void done(ParseObject object, ParseException e) {
                                checkingExistingSnippets.dismiss();

                                if (e == null) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(Share_Snippet_Activity.this);
                                    alert.setTitle(R.string.alert_already_shared_snippet_title);
                                    alert.setMessage(R.string.alert_already_shared_snippet_message);
                                    alert.show();
                                } else {
                                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {

                                        prefsEditor = prefs.edit();
                                        prefsEditor.putString(Constants.USER_LAST_USED_SCREEN_NAME, snippet.getScreen_name());
                                        prefsEditor.apply();

                                        uploadingSnippets_AWS = new ProgressDialog(Share_Snippet_Activity.this);
                                        uploadingSnippets_AWS.setMessage(getString(R.string.uploading_snippet_server));
                                        uploadingSnippets_AWS.setMax((int) file.length() / 1000);
                                        uploadingSnippets_AWS.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                        uploadingSnippets_AWS.setCancelable(false);
                                        uploadingSnippets_AWS.show();

                                        final AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(Constants.AMAZON_ACCESS_KEY, Constants.AMAZON_SECRET_ACCESS_KEY));

                                        Thread t = new Thread(new Runnable() {
                                            @Override
                                            public void run() {

                                                final String snippetTempFilename = new BigInteger(130, new SecureRandom()).toString(32);

                                                PutObjectRequest por = new PutObjectRequest("snippet-images", snippetTempFilename, new File(snippet.getImage_path()));
                                                por.setGeneralProgressListener(new ProgressListener() {
                                                    @Override
                                                    public void progressChanged(com.amazonaws.event.ProgressEvent progressEvent) {

                                                        uploadingSnippets_AWS.setProgress((uploadingSnippets_AWS.getProgress() + (int) (progressEvent.getBytesTransferred() / 1000)));

                                                        if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                                                            uploadingSnippets_AWS.setProgress(uploadingSnippets_AWS.getProgress() + 1);

                                                            snippet.setAws_image_path(Constants.AMAZON_IMAGE_BUCKET_URL + snippetTempFilename);

                                                            ParseObject sharedSnippetObject = new ParseObject("Shared_Snippet");
                                                            sharedSnippetObject.put("snippet_id", snippet.getId());
                                                            sharedSnippetObject.put("name", snippetNameET.getText().toString());
                                                            sharedSnippetObject.put("screen_name", snippet.getScreen_name());
                                                            sharedSnippetObject.put("book_title", fromBook.getTitle());
                                                            sharedSnippetObject.put("book_author", fromBook.getAuthor());
                                                            sharedSnippetObject.put("page_number", snippet.getPage_number());
                                                            sharedSnippetObject.put("aws_image_path", snippet.getAws_image_path());
                                                            sharedSnippetObject.put("snippet_file_size", file.length());

                                                            if (snippet.getOcr_content() != null)
                                                                sharedSnippetObject.put("ocr_content", snippet.getOcr_content());
                                                            else
                                                                sharedSnippetObject.put("ocr_content", "");
                                                            if (snippet.getNote() != null)
                                                                sharedSnippetObject.put("note", snippet.getNote());
                                                            else
                                                                sharedSnippetObject.put("note", "");

                                                            sharedSnippetObject.saveInBackground();

                                                            uploadingSnippets_AWS.dismiss();

                                                            Helper_Methods.logEvent("Shared Snippet", new String[]{snippetNameET.getText().toString(), snippet.getScreen_name()});

                                                            Intent openSnippetStreamActivity = new Intent(Share_Snippet_Activity.this, Snippet_Stream_Activity.class);
                                                            startActivity(openSnippetStreamActivity);
                                                        }
                                                    }
                                                });

                                                try {
                                                    s3Client.putObject(por);
                                                } catch (AmazonClientException e) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            uploadingSnippets_AWS.dismiss();
                                                            EventBus_Singleton.getInstance().post(new EventBus_Poster("amazon_exception_connection_too_slow"));
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                        t.start();
                                    } else {
                                        //unknown error, debug
                                    }
                                }
                            }
                        });
                    } else {
                        Crouton.makeText(this, R.string.action_needs_internet, Style.ALERT).show();
                    }
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        EventBus_Singleton.getInstance().unregister(this);
        super.onDestroy();
    }
}
