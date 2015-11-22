package com.om.snipit.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.andreabaccega.widget.FormEditText;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.models.Book;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;

public class Create_Book_Activity extends ActionBarActivity {

    @InjectView(R.id.titleET)
    FormEditText titleET;
    @InjectView(R.id.authorET)
    FormEditText authorET;
    @InjectView(R.id.bookIMG)
    ImageView bookIMG;
    @InjectView(R.id.doneBTN)
    FloatingActionButton doneBTN;
    @InjectView(R.id.scanBTN)
    ImageView scanBTN;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    private static final int RC_BARCODE_CAPTURE = 9001;

    private String bookImagePath;
    private boolean bookImageFoundAtGoogle = false;

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Book, Integer> bookDAO;

    private ArrayList<FormEditText> allFields = new ArrayList<>();

    private int CALL_PURPOSE;
    private Book book_from_list;
    private ProgressDialog loadingBookInfoDialog;
    private ProgressDialog findingBookImageDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_book);

        ButterKnife.inject(this);

        bookDAO = getHelper().getBookDAO();

        allFields.add(titleET);
        allFields.add(authorET);

        final Helper_Methods helperMethods = new Helper_Methods(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanBTN.setElevation(15f);
            toolbar.setElevation(25f);
        }

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOK_PURPOSE_STRING, -1);

        //If it's an edit operation
        if (CALL_PURPOSE == Constants.EDIT_BOOK_PURPOSE_VALUE) {
            toolbar.setTitle(getString(R.string.edit_book_activity_title));
            setSupportActionBar(toolbar);

            book_from_list = getIntent().getParcelableExtra(Constants.EXTRAS_BOOK);

            if (book_from_list != null) {
                helperMethods.setUpActionbarColors(this, book_from_list.getColorCode());
            } else {
                finish();
            }

            if (book_from_list != null) {
                titleET.setText(book_from_list.getTitle());
                titleET.setSelection(titleET.getText().length());
                authorET.setText(book_from_list.getAuthor());

                if (book_from_list.getImagePath() != null && !book_from_list.getImagePath().isEmpty())
                    Picasso.with(Create_Book_Activity.this).load(book_from_list.getImagePath()).into(bookIMG);
            }
        } else {
            toolbar.setTitle(getString(R.string.create_book_activity_title));
            setSupportActionBar(toolbar);
            helperMethods.setUpActionbarColors(this, Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS);
        }

        doneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (helperMethods.validateFields(allFields)) {
                    //If you are creating a new book
                    if (CALL_PURPOSE != Constants.EDIT_BOOK_PURPOSE_VALUE) {
                        titleET.addTextChangedListener(new BookInfoTextWatcher());
                        authorET.addTextChangedListener(new BookInfoTextWatcher());

                        String bookSearchString = "https://www.googleapis.com/books/v1/volumes?" +
                                "q=intitle:" + titleET.getText().toString().replace(" ", "%20") + "?q=inauthor:" + authorET.getText().toString().replace(" ", "%20") + "&key=" + Constants.GOOGLE_BOOKS_API_KEY;
                        if (Helper_Methods.isInternetAvailable(Create_Book_Activity.this)) {
//                            if (bookImageFoundAtGoogle) {
                            finalizeInsertBook(bookImagePath);
//                            } else {
//                                new GetBookImage().execute(bookSearchString);
//                            }
                        } else {
                            finalizeInsertBook(bookImagePath);
                        }
                    } else {
                        //If you are editing an existing book
                        book_from_list.setTitle(titleET.getText().toString());
                        book_from_list.setAuthor(authorET.getText().toString());

                        bookDAO.update(book_from_list);

                        EventBus_Singleton.getInstance().post(new EventBus_Poster("book_added"));

                        finish();
                    }

                    if (getCurrentFocus() != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                }
            }
        });

        scanBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Helper_Methods.isInternetAvailable(Create_Book_Activity.this)) {
                    Intent intent = new Intent(Create_Book_Activity.this, BarcodeCapture_Activity.class);
                    intent.putExtra(BarcodeCapture_Activity.AutoFocus, true);
                    intent.putExtra(BarcodeCapture_Activity.UseFlash, false);

                    startActivityForResult(intent, RC_BARCODE_CAPTURE);
                } else {
                    Crouton.makeText(Create_Book_Activity.this, getString(R.string.action_needs_internet), Style.ALERT).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                if (getCurrentFocus() != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @DebugLog
    public void finalizeInsertBook(String foundBookImagePath) {
        Random rand = new Random();

        Date date = new Date();
        String day = (String) android.text.format.DateFormat.format("dd", date);
        String month = (String) android.text.format.DateFormat.format("MMM", date);
        String year = (String) android.text.format.DateFormat.format("yyyy", date);

        Book book = new Book();
        book.setTitle(titleET.getText().toString());
        book.setAuthor(authorET.getText().toString());
        book.setImagePath(foundBookImagePath);
        book.setDate_added(month + " " + day + " " + year);
        book.setColorCode(rand.nextInt(7 - 1));
        book.setOrder((int) (bookDAO.countOf() + 1));

        bookDAO.create(book);

        Helper_Methods.logEvent("Created Book", new String[]{book.getTitle()});

        EventBus_Singleton.getInstance().post(new EventBus_Poster("book_added"));

        Intent takeToSnippets = new Intent(Create_Book_Activity.this, Snippets_Activity.class);
        takeToSnippets.putExtra(Constants.EXTRAS_BOOK, book);
        startActivity(takeToSnippets);

        finish();
    }

    private class BookInfoTextWatcher implements TextWatcher {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            bookImageFoundAtGoogle = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String scanContent;

        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCapture_Activity.BarcodeObject);
                    scanContent = barcode.displayValue;

                    String bookSearchString = "https://www.googleapis.com/books/v1/volumes?" +
                            "q=isbn:" + scanContent + "&key=" + Constants.GOOGLE_BOOKS_API_KEY;

                    new GetBookInfo().execute(bookSearchString);
                } else {
                    Crouton.makeText(Create_Book_Activity.this, getString(R.string.no_scan_data), Style.ALERT).show();

                    Log.d(Constants.DEBUG_TAG, "No barcode captured, intent data is null");
                }
            } else {
                Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_not_found), Style.ALERT).show();

//                statusMessage.setText(String.format(getString(R.string.barcode_error),
//                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }

    private class GetBookInfo extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingBookInfoDialog = ProgressDialog.show(Create_Book_Activity.this, getResources().getString(R.string.loading_book_info_title),
                    getResources().getString(R.string.loading_book_info_msg), true);
        }

        @Override
        protected String doInBackground(String... bookURLs) {
            StringBuilder bookBuilder = new StringBuilder();
            for (String bookSearchURL : bookURLs) {
                HttpClient bookClient = new DefaultHttpClient();
                try {
                    HttpGet bookGet = new HttpGet(bookSearchURL);
                    HttpResponse bookResponse = bookClient.execute(bookGet);
                    StatusLine bookSearchStatus = bookResponse.getStatusLine();
                    if (bookSearchStatus.getStatusCode() == 200) {
                        HttpEntity bookEntity = bookResponse.getEntity();
                        InputStream bookContent = bookEntity.getContent();
                        InputStreamReader bookInput = new InputStreamReader(bookContent);
                        BufferedReader bookReader = new BufferedReader(bookInput);
                        String lineIn;
                        while ((lineIn = bookReader.readLine()) != null) {
                            bookBuilder.append(lineIn);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return bookBuilder.toString();
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject resultObject = new JSONObject(result);
                JSONArray bookArray = resultObject.getJSONArray("items");
                JSONObject bookObject = bookArray.getJSONObject(0);
                JSONObject volumeObject = bookObject.getJSONObject("volumeInfo");

                try {
                    titleET.setText(volumeObject.getString("title"));

                    StringBuilder authorBuild = new StringBuilder("");
                    try {
                        JSONArray authorArray = volumeObject.getJSONArray("authors");
                        for (int a = 0; a < authorArray.length(); a++) {
                            if (a > 0) authorBuild.append(", ");
                            authorBuild.append(authorArray.getString(a));
                        }
                        authorET.setText(authorBuild.toString());
                        loadingBookInfoDialog.dismiss();
                    } catch (JSONException jse) {
                        Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_author_not_found_error), Style.ALERT).show();
                        loadingBookInfoDialog.dismiss();
                        jse.printStackTrace();
                    }
                } catch (JSONException jse) {
                    Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_title_not_found_error), Style.ALERT).show();
                    jse.printStackTrace();
                }
                try {
                    JSONObject imageInfo = volumeObject.getJSONObject("imageLinks");
                    Picasso.with(Create_Book_Activity.this).load(imageInfo.getString("smallThumbnail")).error(getResources().getDrawable(R.drawable.notfound_1)).into(bookIMG);
                    bookImagePath = imageInfo.getString("smallThumbnail");
                } catch (JSONException jse) {
                    Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_image_not_found_error), Style.ALERT).show();
                    loadingBookInfoDialog.dismiss();
                    jse.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Crouton.makeText(Create_Book_Activity.this, getString(R.string.scan_result_error), Style.ALERT).show();
                loadingBookInfoDialog.dismiss();
                e.printStackTrace();
            }
        }
    }

    private class GetBookImage extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findingBookImageDialog = ProgressDialog.show(Create_Book_Activity.this, getResources().getString(R.string.loading_book_info_title),
                    getResources().getString(R.string.finding_book_image), true);
        }

        @Override
        protected String doInBackground(String... bookURLs) {
            StringBuilder bookBuilder = new StringBuilder();
            for (String bookSearchURL : bookURLs) {
                HttpClient bookClient = new DefaultHttpClient();
                try {
                    HttpGet bookGet = new HttpGet(bookSearchURL);
                    HttpResponse bookResponse = bookClient.execute(bookGet);
                    StatusLine bookSearchStatus = bookResponse.getStatusLine();
                    if (bookSearchStatus.getStatusCode() == 200) {
                        HttpEntity bookEntity = bookResponse.getEntity();
                        InputStream bookContent = bookEntity.getContent();
                        InputStreamReader bookInput = new InputStreamReader(bookContent);
                        BufferedReader bookReader = new BufferedReader(bookInput);
                        String lineIn;
                        while ((lineIn = bookReader.readLine()) != null) {
                            bookBuilder.append(lineIn);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return bookBuilder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject resultObject = new JSONObject(result);
                JSONArray bookArray = resultObject.getJSONArray("items");
                JSONObject bookObject = bookArray.getJSONObject(0);
                JSONObject volumeObject = bookObject.getJSONObject("volumeInfo");
                JSONObject imageInfo = volumeObject.getJSONObject("imageLinks");

                Picasso.with(Create_Book_Activity.this).load(imageInfo.getString("thumbnail")).error(getResources().getDrawable(R.drawable.notfound_1)).into(bookIMG);

                bookImagePath = imageInfo.getString("thumbnail");

                findingBookImageDialog.dismiss();

                bookImageFoundAtGoogle = true;
            } catch (JSONException jse) {
                Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_image_not_found_error), Style.ALERT).show();
                bookIMG.setImageResource(0);
                bookImageFoundAtGoogle = true;
                bookImagePath = Constants.NO_BOOK_IMAGE;
                findingBookImageDialog.dismiss();
                jse.printStackTrace();
            }
        }
    }
}
