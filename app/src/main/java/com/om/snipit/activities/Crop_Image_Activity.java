package com.om.snipit.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;

import com.isseiaoki.simplecropview.CropImageView;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;

public class Crop_Image_Activity extends Base_Activity {
    @InjectView(R.id.doneBTN)
    FloatingActionButton doneBTN;
    @InjectView(R.id.rotateImageBTN)
    FloatingActionButton rotateImageBTN;
    @InjectView(R.id.cropImageView)
    CropImageView cropImageView;

    private String tempImagePath_fromIntent;
    private int CALL_PURPOSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        ButterKnife.inject(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        helperMethods.setUpActionbarColors(this, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));

        tempImagePath_fromIntent = getIntent().getExtras().getString(Constants.EXTRAS_SNIPPET_IMAGE_PATH);
        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_SNIPPET_PURPOSE_STRING, -1);

        try {
            File imgFile = new File(tempImagePath_fromIntent);
            if (imgFile.exists()) {
                Picasso.with(this).load(imgFile).resize(1000, 1000).centerInside().into(picassoCropTarget);
            }
        } catch (NullPointerException e) {
            Crouton.makeText(this, R.string.retake_picture_error, Style.ALERT).show();
            finish();
        }

        doneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String finalImagePath = create_CroppedImageFile(cropImageView.getCroppedBitmap());

                Helper_Methods.delete_image_from_disk(tempImagePath_fromIntent);

                if (CALL_PURPOSE == Constants.EDIT_SNIPPET_PURPOSE_VALUE || CALL_PURPOSE == Constants.EDIT_SNIPPET_IMAGE_PURPOSE_VALUE) {
                    EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_picture_changed", finalImagePath));
                } else {
                    Intent openCreateBookmark = new Intent(Crop_Image_Activity.this, Create_Snippet_Activity.class);
                    openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
                    openCreateBookmark.putExtra(Constants.EXTRAS_SNIPPET_IMAGE_PATH, finalImagePath);
                    openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_COLOR, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));
                    startActivity(openCreateBookmark);
                }

                finish();
            }
        });

        rotateImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropImageView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
            }
        });
    }

    Target picassoCropTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            cropImageView.setImageBitmap(bitmap);
            cropImageView.setGuideShowMode(CropImageView.ShowMode.SHOW_ON_TOUCH);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @DebugLog
    public String create_CroppedImageFile(Bitmap inImage) {
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

        File f = new File(mediaStorageDir.getPath(), imageFileName);
        try {
            f.createNewFile();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] bitmapdata = bos.toByteArray();

            FileOutputStream fos;
            fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f.getAbsolutePath();
    }
}
