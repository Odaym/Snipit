package com.om.snipit.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.andreabaccega.widget.FormEditText;
import com.bumptech.glide.Glide;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.atomic.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.Snippet;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;

public class Create_Snippet_Activity extends Base_Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @InjectView(R.id.nameET)
    FormEditText nameET;
    @InjectView(R.id.pageNumberET)
    EditText pageNumberET;
    @InjectView(R.id.bookmarkIMG)
    ImageView bookmarkIMG;
    @InjectView(R.id.doneBTN)
    FloatingActionButton doneBTN;
    @InjectView(R.id.createNewBookmarkBTN)
    FloatingActionButton createNewBookmarkBTN;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    private ArrayList<FormEditText> allFields = new ArrayList<>();
    private int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Snippet, Integer> bookmarkDAO;

    private int CALL_PURPOSE;
    private Snippet snippet_from_list;
    private String tempImagePath, finalImagePath;
    private EventBus_Poster ebpFromEditBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_snippet);

        ButterKnife.inject(this);

        bookmarkDAO = getHelper().getSnipitDAO();

        allFields.add(nameET);

        final Helper_Methods helperMethods = new Helper_Methods(this);

        EventBus_Singleton.getInstance().register(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        helperMethods.setUpActionbarColors(this, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));

        if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, -1);

        tempImagePath = getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);
        finalImagePath = tempImagePath;

        //If it is a create operation, the path to the bookmark image is inside the extras that were sent to this activity (from Camera intent)
        try {
            Picasso.with(this).load(new File(tempImagePath)).resize(2000, 2000).centerInside().into(bookmarkIMG);
        } catch (NullPointerException NPE) {
            NPE.printStackTrace();
        }

        //If it's an edit operation, the path to the bookmark image is inside the object being sent to this activity
        if (CALL_PURPOSE == Constants.EDIT_BOOKMARK_PURPOSE_VALUE) {
            getSupportActionBar().setTitle(getString(R.string.edit_bookmark_activity_title));

            snippet_from_list = bookmarkDAO.queryForId(getIntent().getExtras().getInt(Constants.EXTRAS_BOOKMARK_ID, -1));

            nameET.setText(snippet_from_list.getName());
            nameET.setSelection(nameET.getText().length());

            if (snippet_from_list.getPage_number() != Constants.NO_BOOKMARK_PAGE_NUMBER)
                pageNumberET.setText(String.valueOf(snippet_from_list.getPage_number()));

            try {
                //If the String was a URL then this bookmark is a sample
                new URL(snippet_from_list.getImage_path());
                Glide.with(Create_Snippet_Activity.this).load(snippet_from_list.getImage_path()).centerCrop().error(getResources().getDrawable(R.drawable.notfound_1)).into(bookmarkIMG);
            } catch (MalformedURLException e) {
                //Else it's on disk
                Picasso.with(this).load(new File(snippet_from_list.getImage_path())).into(bookmarkIMG);
            }
        } else {
            getSupportActionBar().setTitle(getString(R.string.create_bookmark_activity_title));
        }

        doneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (helperMethods.validateFields(allFields)) {
                    //If you are editing an existing bookmark
                    if (CALL_PURPOSE == Constants.EDIT_BOOKMARK_PURPOSE_VALUE) {
                        try {
                            snippet_from_list.setName(nameET.getText().toString());

                            //Only try to parse if there was a number given
                            if (!pageNumberET.getText().toString().isEmpty())
                                snippet_from_list.setPage_number(Short.parseShort(pageNumberET.getText().toString()));
                            else
                                snippet_from_list.setPage_number(Constants.NO_BOOKMARK_PAGE_NUMBER);

                            if (ebpFromEditBookmark != null)
                                snippet_from_list.setImage_path(ebpFromEditBookmark.getExtra());
                            else
                                snippet_from_list.setImage_path(snippet_from_list.getImage_path());

                            bookmarkDAO.update(snippet_from_list);

                            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_name_page_edited"));

                            finish();
                        } catch (NumberFormatException e) {
                            pageNumberET.setText("");
                            Crouton.makeText(Create_Snippet_Activity.this, getString(R.string.page_number_error), Style.ALERT).show();
                        }
                    } else {
                        //If you are creating a new bookmark
                        Date date = new Date();
                        String month = (String) android.text.format.DateFormat.format("MMM", date);
                        String day = (String) android.text.format.DateFormat.format("dd", date);
                        String year = (String) android.text.format.DateFormat.format("yyyy", date);

                        try {
                            Snippet snippet = new Snippet();
                            snippet.setName(nameET.getText().toString());
                            snippet.setBookId(getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
                            snippet.setOrder(bookmarkDAO.queryForEq("book_id", getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID)).size() + 1);
                            snippet.setFavorite(false);

                            //Only try to parse if there was a number given
                            if (!pageNumberET.getText().toString().isEmpty())
                                snippet.setPage_number(Short.parseShort(pageNumberET.getText().toString()));
                            else
                                snippet.setPage_number(Constants.NO_BOOKMARK_PAGE_NUMBER);

                            if (CALL_PURPOSE == Constants.EDIT_BOOKMARK_IMAGE_PURPOSE_VALUE)
                                snippet.setImage_path(ebpFromEditBookmark.getExtra());
                            else
                                snippet.setImage_path(finalImagePath);

                            snippet.setDate_added(month + " " + day + ", " + year);

                            bookmarkDAO.create(snippet);

                            FlurryAgent.logEvent("Bookmark_Create");

                            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_added_bookmarks_activity"));
                            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_added_books_activity"));

                            finish();
                        } catch (NumberFormatException e) {
                            pageNumberET.setText("");
                            Crouton.makeText(Create_Snippet_Activity.this, getString(R.string.page_number_error), Style.ALERT).show();
                        }
                    }
                }
            }
        });

        createNewBookmarkBTN.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(helperMethods.determineFabButtonsColor(getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR)))));

        createNewBookmarkBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        if (ebp.getMessage().equals("bookmark_picture_changed")) {
            try {
                Picasso.with(this).load(new File(ebp.getExtra())).into(bookmarkIMG);
                ebpFromEditBookmark = ebp;
            } catch (NullPointerException NPE) {
                NPE.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                //If retaking the picture is being done from within an existing bookmark, set the flag to be so
                if (CALL_PURPOSE != Constants.EDIT_BOOKMARK_PURPOSE_VALUE)
                    CALL_PURPOSE = Constants.EDIT_BOOKMARK_IMAGE_PURPOSE_VALUE;

                Intent openCropImageActivity = new Intent(Create_Snippet_Activity.this, Crop_Image_Activity.class);
                openCropImageActivity.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
                openCropImageActivity.putExtra(Constants.EXTRAS_BOOK_COLOR, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));
                openCropImageActivity.putExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, CALL_PURPOSE);
                openCropImageActivity.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, tempImagePath);
                startActivity(openCropImageActivity);
                break;
        }
    }

    @DebugLog
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Atomic");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(Constants.DEBUG_TAG, "failed to create directory");
                return null;
            }
        }

        File image = new File(mediaStorageDir.getPath() + File.separator + imageFileName);

        tempImagePath = image.getAbsolutePath();

        return image;
    }
}
