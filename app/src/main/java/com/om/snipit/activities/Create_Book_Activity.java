package com.om.atomic.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.andreabaccega.widget.FormEditText;
import com.flurry.android.FlurryAgent;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.om.atomic.R;
import com.om.atomic.classes.Book;
import com.om.atomic.classes.Constants;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.om.atomic.classes.Helper_Methods;
import com.om.atomic.classes.Param;
import com.om.atomic.showcaseview.ShowcaseView;
import com.om.atomic.showcaseview.ViewTarget;
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

public class Create_Book_Activity extends Base_Activity {

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

    private String bookImagePath;
    private int book_pages_count;
    private boolean bookImageFoundAtGoogle = false;
    private DatabaseHelper dbHelper;

    private ShowcaseView scanBookShowcase;

    private ArrayList<FormEditText> allFields = new ArrayList<>();

    private int CALL_PURPOSE;
    private Book book_from_list;
    private ProgressDialog loadingBookInfoDialog;
    private ProgressDialog findingBookImageDialog;

    private static final int SHOW_SCAN_BOOK_SHOWCASE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_book);

        ButterKnife.inject(this);

        allFields.add(titleET);
        allFields.add(authorET);

        Handler UIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SHOW_SCAN_BOOK_SHOWCASE:
                        showScanBookHintShowcase();
                        break;
                }
                super.handleMessage(msg);
            }
        };

        dbHelper = new DatabaseHelper(this);

        final Helper_Methods helperMethods = new Helper_Methods(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (helperMethods.getCurrentapiVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            scanBTN.setElevation(15f);
        }

        UIHandler.sendEmptyMessageDelayed(SHOW_SCAN_BOOK_SHOWCASE, 500);

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOK_PURPOSE_STRING, -1);

        //If it's an edit operation
        if (CALL_PURPOSE == Constants.EDIT_BOOK_PURPOSE_VALUE) {
            getSupportActionBar().setTitle(getString(R.string.edit_book_activity_title));

            helperMethods.setUpActionbarColors(this, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));

            book_from_list = getIntent().getParcelableExtra("book");

            if (book_from_list != null) {
                titleET.setText(book_from_list.getTitle());
                titleET.setSelection(titleET.getText().length());
                authorET.setText(book_from_list.getAuthor());

                if (!book_from_list.getImagePath().isEmpty())
                    Picasso.with(Create_Book_Activity.this).load(book_from_list.getImagePath()).into(bookIMG);
            }
        } else {
            getSupportActionBar().setTitle(getString(R.string.create_book_activity_title));
            helperMethods.setUpActionbarColors(this, -1);
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
                            if (bookImageFoundAtGoogle) {
                                finalizeInsertBook(bookImagePath);
                            } else {
                                new GetBookImage().execute(bookSearchString);
                            }
                        } else {
                            finalizeInsertBook(bookImagePath);
                        }
                    } else {
                        //If you are editing an existing book
                        book_from_list.setTitle(titleET.getText().toString());
                        book_from_list.setAuthor(authorET.getText().toString());

                        dbHelper.updateBook(book_from_list);

                        EventBus_Singleton.getInstance().post(new EventBus_Poster("book_added"));

                        finish();

                        if (getCurrentFocus() != null) {
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }
                    }
                }
            }
        });

        scanBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanBookShowcase != null)
                    scanBookShowcase.hide();

                if (Helper_Methods.isInternetAvailable(Create_Book_Activity.this)) {
                    IntentIntegrator scanIntegrator = new IntentIntegrator(Create_Book_Activity.this);
                    scanIntegrator.initiateScan();
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
        book.setPages_count(book_pages_count);
        book.setPage_reached(0);

        int last_insert_book_id = dbHelper.createBook(book);

        FlurryAgent.logEvent("Book_Create");

        EventBus_Singleton.getInstance().post(new EventBus_Poster("book_added"));

        Intent takeToBookmarks = new Intent(Create_Book_Activity.this, Bookmarks_Activity.class);
        takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_ID, last_insert_book_id);
        takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_TITLE, book.getTitle());
        takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_COLOR, book.getColorCode());

        startActivity(takeToBookmarks);

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

    @DebugLog
    public void showScanBookHintShowcase() {
        if (!dbHelper.getParam(null, 3)) {

            RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lps.setMargins(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.button_margin_bottom));

            ViewTarget target = new ViewTarget(R.id.scanBTN, Create_Book_Activity.this);

            String showcaseDescription = getString(R.string.scan_books_showcase);

            scanBookShowcase = new ShowcaseView.Builder(Create_Book_Activity.this, getResources().getDimensionPixelSize(R.dimen.scan_books_showcase_inner_rad), getResources().getDimensionPixelSize(R.dimen.scan_books_showcase_outer_rad))
                    .setTarget(target)
                    .setContentText(Helper_Methods.fontifyString(showcaseDescription))
                    .setStyle(R.style.CustomShowcaseTheme)
                    .hasManualPosition(true)
                    .xPostion(getResources().getDimensionPixelSize(R.dimen.scan_books_showcase_text_x))
                    .yPostion(getResources().getDimensionPixelSize(R.dimen.scan_books_showcase_text_y))
                    .build();
            scanBookShowcase.setButtonPosition(lps);
            scanBookShowcase.findViewById(R.id.showcase_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scanBookShowcase.hide();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.toggleSoftInputFromWindow(scanBookShowcase.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);

                    Param param = new Param();
                    param.setNumber(3);
                    param.setValue("True");
                    dbHelper.updateParam(param);
                }
            });
            scanBookShowcase.setShouldCentreText(true);
            scanBookShowcase.show();
        } else {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(titleET, InputMethodManager.SHOW_FORCED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {

            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();

            if (scanContent != null && scanFormat != null && scanFormat.equalsIgnoreCase("EAN_13")) {
                String bookSearchString = "https://www.googleapis.com/books/v1/volumes?" +
                        "q=isbn:" + scanContent + "&key=" + Constants.GOOGLE_BOOKS_API_KEY;

                new GetBookInfo().execute(bookSearchString);
            } else {
                Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_not_found), Style.ALERT).show();
            }
        } else {
            Crouton.makeText(Create_Book_Activity.this, getString(R.string.no_scan_data), Style.ALERT).show();
        }
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
                book_pages_count = volumeObject.getInt("pageCount");

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
                book_pages_count = volumeObject.getInt("pageCount");

                Picasso.with(Create_Book_Activity.this).load(imageInfo.getString("thumbnail")).error(getResources().getDrawable(R.drawable.notfound_1)).into(bookIMG);

                bookImagePath = imageInfo.getString("thumbnail");

                findingBookImageDialog.dismiss();

                bookImageFoundAtGoogle = true;
            } catch (JSONException jse) {
                Crouton.makeText(Create_Book_Activity.this, getString(R.string.book_image_not_found_error), Style.ALERT).show();
                bookIMG.setImageResource(0);
                bookImageFoundAtGoogle = true;
                book_pages_count = Constants.NO_BOOKMARK_PAGE_NUMBER;
                bookImagePath = Constants.NO_BOOK_IMAGE;
                findingBookImageDialog.dismiss();
                jse.printStackTrace();
            }
        }
    }
}
