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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.andreabaccega.widget.FormEditText;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
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
    @InjectView(R.id.snippetIMG)
    ImageView snippetIMG;
    @InjectView(R.id.doneBTN)
    FloatingActionButton doneBTN;
    @InjectView(R.id.createNewSnippetBTN)
    FloatingActionButton createNewSnippetBTN;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    private Book book;

    private ArrayList<FormEditText> allFields = new ArrayList<>();

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Snippet, Integer> snippetDAO;

    private int CALL_PURPOSE;
    private Snippet snippet_from_list;
    private String tempImagePath, finalImagePath;
    private EventBus_Poster ebpFromEditSnippet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_snippet);

        ButterKnife.inject(this);

        snippetDAO = getHelper().getSnippetDAO();

        allFields.add(nameET);

        final Helper_Methods helperMethods = new Helper_Methods(this);

        EventBus_Singleton.getInstance().register(this);

        book = getIntent().getParcelableExtra(Constants.EXTRAS_BOOK);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        helperMethods.setUpActionbarColors(this, book.getColorCode());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_SNIPPET_PURPOSE_STRING, -1);

        tempImagePath = getIntent().getExtras().getString(Constants.EXTRAS_SNIPPET_TEMP_IMAGE_PATH);
        finalImagePath = tempImagePath;

        //If it is a create operation, the path to the snippet image is inside the extras that were sent to this activity (from Camera intent)
        try {
            Picasso.with(this).load(new File(tempImagePath)).resize(1000, 1000).centerInside().into(snippetIMG);
        } catch (NullPointerException NPE) {
            NPE.printStackTrace();
        }

        //If it's an edit operation, the path to the snippet image is inside the object being sent to this activity
        if (CALL_PURPOSE == Constants.EDIT_SNIPPET_PURPOSE_VALUE) {
            getSupportActionBar().setTitle(getString(R.string.edit_snippet_activity_title));

            snippet_from_list = getIntent().getParcelableExtra(Constants.EXTRAS_SNIPPET);

            nameET.setText(snippet_from_list.getName());
            nameET.setSelection(nameET.getText().length());

            if (snippet_from_list.getPage_number() != Constants.NO_SNIPPET_PAGE_NUMBER)
                pageNumberET.setText(String.valueOf(snippet_from_list.getPage_number()));

            Picasso.with(this).load(new File(snippet_from_list.getImage_path())).into(snippetIMG);
        } else {
            getSupportActionBar().setTitle(getString(R.string.create_snippet_activity_title));
        }

        doneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (helperMethods.validateFields(allFields)) {
                    //If you are editing an existing snippet
                    if (CALL_PURPOSE == Constants.EDIT_SNIPPET_PURPOSE_VALUE) {
                        try {
                            snippet_from_list.setName(nameET.getText().toString());

                            //Only try to parse if there was a number given
                            if (!pageNumberET.getText().toString().isEmpty())
                                snippet_from_list.setPage_number(Short.parseShort(pageNumberET.getText().toString()));
                            else
                                snippet_from_list.setPage_number(Constants.NO_SNIPPET_PAGE_NUMBER);

                            if (ebpFromEditSnippet != null)
                                snippet_from_list.setImage_path(ebpFromEditSnippet.getExtra());
                            else
                                snippet_from_list.setImage_path(snippet_from_list.getImage_path());

                            snippet_from_list.setBook(book);

                            snippetDAO.update(snippet_from_list);

                            EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_name_page_edited"));

                            finish();
                        } catch (NumberFormatException e) {
                            pageNumberET.setText("");
                            Crouton.makeText(Create_Snippet_Activity.this, getString(R.string.page_number_error), Style.ALERT).show();
                        }
                    } else {
                        //If you are creating a new snippet
                        Date date = new Date();
                        String month = (String) DateFormat.format("MMM", date);
                        String day = (String) DateFormat.format("dd", date);
                        String year = (String) DateFormat.format("yyyy", date);

                        try {
                            Snippet snippet = new Snippet();
                            snippet.setName(nameET.getText().toString());
                            snippet.setOrder(snippetDAO.queryForEq("book_id", book.getId()).size() + 1);

                            //Only try to parse if there was a number given
                            if (!pageNumberET.getText().toString().isEmpty())
                                snippet.setPage_number(Short.parseShort(pageNumberET.getText().toString()));
                            else
                                snippet.setPage_number(Constants.NO_SNIPPET_PAGE_NUMBER);

                            if (CALL_PURPOSE == Constants.EDIT_SNIPPET_IMAGE_PURPOSE_VALUE)
                                snippet.setImage_path(ebpFromEditSnippet.getExtra());
                            else
                                snippet.setImage_path(finalImagePath);

                            snippet.setDate_added(month + " " + day + ", " + year);

                            snippet.setBook(book);

                            snippetDAO.create(snippet);

                            FlurryAgent.logEvent("Create_Snipit");

                            EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_added_snippets_activity"));
                            EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_added_books_activity"));

                            finish();
                        } catch (NumberFormatException e) {
                            pageNumberET.setText("");
                            Crouton.makeText(Create_Snippet_Activity.this, getString(R.string.page_number_error), Style.ALERT).show();
                        }
                    }
                }
            }
        });

        createNewSnippetBTN.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(helperMethods.determineFabButtonsColor(book.getColorCode()))));

        createNewSnippetBTN.setOnClickListener(new View.OnClickListener() {
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
        if (ebp.getMessage().equals("snippet_picture_changed")) {
            try {
                Picasso.with(this).load(new File(ebp.getExtra())).into(snippetIMG);
                ebpFromEditSnippet = ebp;
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
                //If retaking the picture is being done from within an existing snippet, set the flag to be so
                if (CALL_PURPOSE != Constants.EDIT_SNIPPET_PURPOSE_VALUE)
                    CALL_PURPOSE = Constants.EDIT_SNIPPET_IMAGE_PURPOSE_VALUE;

                Intent openCropImageActivity = new Intent(Create_Snippet_Activity.this, Crop_Image_Activity.class);
                openCropImageActivity.putExtra(Constants.EXTRAS_BOOK, book);
                openCropImageActivity.putExtra(Constants.EDIT_SNIPPET_PURPOSE_STRING, CALL_PURPOSE);
                openCropImageActivity.putExtra(Constants.EXTRAS_SNIPPET_TEMP_IMAGE_PATH, tempImagePath);
                startActivity(openCropImageActivity);
                break;
        }
    }

    @DebugLog
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Snipit");

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
