package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.EventBus_Poster;
import com.ttco.bookmarker.classes.EventBus_Singleton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class Create_Bookmark_Activity extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @InjectView(R.id.nameET)
    EditText nameET;
    @InjectView(R.id.pageNumberET)
    EditText pageNumberET;
    @InjectView(R.id.bookmarkIMG)
    ImageView bookmarkIMG;

    private DatabaseHelper dbHelper;
    private int CALL_PURPOSE;
    private Bookmark bookmark_from_list;
    private String finalImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bookmark);

        ButterKnife.inject(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new DatabaseHelper(this);

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, -1);

        finalImagePath = getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);

        //If it is a create operation, the path to the bookmark image is inside the extras that were sent to this activity (from Camera intent)
        try {
            Picasso.with(this).load(new File(finalImagePath)).into(bookmarkIMG);
        } catch (NullPointerException NPE) {
            NPE.printStackTrace();
        }

        //If it's an edit operation, the path to the bookmark image is inside the object being sent to this activity
        if (CALL_PURPOSE == Constants.EDIT_BOOKMARK_PURPOSE_VALUE) {
            bookmark_from_list = getIntent().getParcelableExtra("bookmark");

            nameET.setText(bookmark_from_list.getName());
            pageNumberET.setText(String.valueOf(bookmark_from_list.getPage_number()));
            try {
                Picasso.with(this).load(new File(bookmark_from_list.getImage_path())).into(bookmarkIMG);
            } catch (NullPointerException NPE) {
                NPE.printStackTrace();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                Picasso.with(this).load(new File(finalImagePath)).into(bookmarkIMG);
                break;
        }
    }


    public void handleDone_Pressed(View view) {
        if (!nameET.getText().toString().isEmpty()) {
            if (CALL_PURPOSE == Constants.EDIT_BOOKMARK_PURPOSE_VALUE) {
                bookmark_from_list.setName(nameET.getText().toString());
                try {
                    bookmark_from_list.setPage_number(Short.parseShort(pageNumberET.getText().toString()));
                    dbHelper.updateBookmark(bookmark_from_list);

                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_added"));

                    finish();
                } catch (NumberFormatException e) {
                    pageNumberET.setText("");
                    Toast.makeText(this, getString(R.string.page_number_error), Toast.LENGTH_LONG).show();
                }

            } else {
                Date date = new Date();
                String month = (String) android.text.format.DateFormat.format("MMM", date);
                String day = (String) android.text.format.DateFormat.format("dd", date);
                String year = (String) android.text.format.DateFormat.format("yyyy", date);

                try {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setName(nameET.getText().toString());
                    bookmark.setPage_number(Short.parseShort(pageNumberET.getText().toString()));
                    bookmark.setImage_path(finalImagePath);
                    bookmark.setDate_added(month + " " + day + " " + year);

                    dbHelper.createBookmark(bookmark, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));

                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_added"));

                    finish();
                } catch (NumberFormatException e) {
                    pageNumberET.setText("");
                    Toast.makeText(this, getString(R.string.page_number_error), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void takeAnotherPicture_Pressed(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Atomic");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(Constants.DEBUG_TAG, "failed to create directory");
                return null;
            }
        }

        File image = new File(mediaStorageDir.getPath() + File.separator + imageFileName);

        finalImagePath = image.getAbsolutePath();
        return image;
    }
}
