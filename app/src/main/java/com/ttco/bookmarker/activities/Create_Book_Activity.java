package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Book;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.Helper_Methods;
import com.ttco.bookmarker.classes.Param;
import com.ttco.bookmarker.showcaseview.ShowcaseView;
import com.ttco.bookmarker.showcaseview.ViewTarget;

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
import java.util.Date;
import java.util.Random;

public class Create_Book_Activity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText titleET, authorET;
    private ImageView bookIMG;
    private String bookImagePath;

    private ShowcaseView scanBookShowcase;

    private int CALL_PURPOSE;
    private Book book_from_list;
    private ProgressDialog loadingBookInfoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_book);

        dbHelper = new DatabaseHelper(this);

        titleET = (EditText) findViewById(R.id.titleET);
        authorET = (EditText) findViewById(R.id.authorET);
        bookIMG = (ImageView) findViewById(R.id.bookIMG);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        showScanBookHintShowcase();

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOK_PURPOSE_STRING, -1);

        if (CALL_PURPOSE == Constants.EDIT_BOOK_PURPOSE_VALUE) {
            book_from_list = getIntent().getParcelableExtra("book");
            if (book_from_list != null) {
                titleET.setText(book_from_list.getTitle());
                authorET.setText(book_from_list.getAuthor());
                Picasso.with(Create_Book_Activity.this).load(book_from_list.getImagePath()).error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(bookIMG);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showScanBookHintShowcase() {
        if (!dbHelper.getSeensParam(null, 3)) {

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
        }
    }

    public void handleDone_Pressed(View view) {
        if (!titleET.getText().toString().isEmpty()) {
            if (CALL_PURPOSE != Constants.EDIT_BOOK_PURPOSE_VALUE) {
                Random rand = new Random();

                Date date = new Date();
                String day = (String) android.text.format.DateFormat.format("dd", date);
                String month = (String) android.text.format.DateFormat.format("MMM", date);
                String year = (String) android.text.format.DateFormat.format("yyyy", date);

                Book book = new Book();
                book.setTitle(titleET.getText().toString());
                book.setAuthor(authorET.getText().toString());
                book.setImagePath(bookImagePath);
                book.setDate_added(month + " " + day + " " + year);
                book.setColorCode(rand.nextInt(7 - 1));

                int last_insert_book_id = dbHelper.createBook(book);

                Intent bookAdded = new Intent();
                String bookAddedIntent_String = "com.ttco.bookmarker.newBookAdded";
                bookAdded.setAction(bookAddedIntent_String);
                sendBroadcast(bookAdded);

                finish();

                Intent takeToBookmarks = new Intent(this, Bookmarks_Activity.class);
                takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_ID, last_insert_book_id);
                takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_TITLE, book.getTitle());
                takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_COLOR, book.getColorCode());

                startActivity(takeToBookmarks);
            } else {
                book_from_list.setTitle(titleET.getText().toString());
                book_from_list.setAuthor(authorET.getText().toString());
                dbHelper.updateBook(book_from_list);

                Intent bookAdded = new Intent();
                String bookAddedIntent_String = "com.ttco.bookmarker.newBookAdded";
                bookAdded.setAction(bookAddedIntent_String);
                sendBroadcast(bookAdded);

                finish();
            }
        }
    }

    public void handleScan_Pressed(View view) {
        if (scanBookShowcase != null)
            scanBookShowcase.hide();

        if (Helper_Methods.isInternetAvailable(this)) {
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            scanIntegrator.initiateScan();
        } else {
            Toast.makeText(this, getString(R.string.scan_no_internet_error), Toast.LENGTH_LONG).show();
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
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
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
                        Toast.makeText(Create_Book_Activity.this, getResources().getString(R.string.book_author_not_found_error), Toast.LENGTH_LONG);
                        loadingBookInfoDialog.dismiss();
                        jse.printStackTrace();
                    }
                } catch (JSONException jse) {
                    Toast.makeText(Create_Book_Activity.this, getResources().getString(R.string.book_title_not_found_error), Toast.LENGTH_LONG);
                    jse.printStackTrace();
                }
                try {
                    JSONObject imageInfo = volumeObject.getJSONObject("imageLinks");
                    Picasso.with(Create_Book_Activity.this).load(imageInfo.getString("smallThumbnail")).error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(bookIMG);
                    bookImagePath = imageInfo.getString("smallThumbnail");
                } catch (JSONException jse) {
                    jse.printStackTrace();
                    Toast.makeText(Create_Book_Activity.this, getResources().getString(R.string.book_image_not_found_error), Toast.LENGTH_LONG).show();
                    loadingBookInfoDialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(Create_Book_Activity.this, getResources().getString(R.string.scan_result_error), Toast.LENGTH_LONG).show();
                loadingBookInfoDialog.dismiss();
            }
        }
    }
}
