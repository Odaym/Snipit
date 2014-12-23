package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Book;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;

import java.util.Date;
import java.util.Random;


public class Create_Book_Activity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText titleET, authorET;
    private int CALL_PURPOSE;
    private Book book_from_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_book);

        dbHelper = new DatabaseHelper(this);

        titleET = (EditText) findViewById(R.id.titleET);
        authorET = (EditText) findViewById(R.id.authorET);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOK_PURPOSE_STRING, -1);
        if (CALL_PURPOSE == Constants.EDIT_BOOK_PURPOSE_VALUE) {
            book_from_list = getIntent().getParcelableExtra("book");
            if (book_from_list != null) {
                titleET.setText(book_from_list.getTitle());
                authorET.setText(book_from_list.getAuthor());
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

    public void handleDone_Pressed(View view) {
        if (!titleET.getText().toString().isEmpty() && !authorET.getText().toString().isEmpty()) {
            if (CALL_PURPOSE != Constants.EDIT_BOOK_PURPOSE_VALUE) {
                Random rand = new Random();

                Date date = new Date();
                String day = (String) android.text.format.DateFormat.format("dd", date);
                String month = (String) android.text.format.DateFormat.format("MMM", date);
                String year = (String) android.text.format.DateFormat.format("yyyy", date);

                Book book = new Book();
                book.setTitle(titleET.getText().toString());
                book.setAuthor(authorET.getText().toString());
                book.setDate_added(month + " " + day + " " + year);
                book.setColorCode(rand.nextInt(7 - 1));

                int book_id = dbHelper.createBook(book);

                Intent bookAdded = new Intent();
                String bookAddedIntent_String = "com.ttco.bookmarker.newBookAdded";
                bookAdded.setAction(bookAddedIntent_String);
                sendBroadcast(bookAdded);

                finish();

                Intent takeToBookmarks = new Intent(this, Bookmarks_Activity.class);
                takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
                takeToBookmarks.putExtra(Constants.EXTRAS_BOOK_TITLE, titleET.getText().toString());
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
}
