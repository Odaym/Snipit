package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.edmodo.cropper.CropImageView;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.EventBus_Poster;
import com.ttco.bookmarker.classes.EventBus_Singleton;
import com.ttco.bookmarker.classes.Helper_Methods;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class Crop_Image_Activity extends ActionBarActivity {
    @InjectView(R.id.doneBTN)
    ImageView doneBTN;
    @InjectView(R.id.rotateImageBTN)
    ImageButton rotateImageBTN;
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

        if (helperMethods.getCurrentapiVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            doneBTN.setElevation(15f);
            rotateImageBTN.setElevation(15f);
        }

        helperMethods.setUpActionbarColors(this, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));

        tempImagePath_fromIntent = getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);
        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, -1);

        cropImageView.setAspectRatio(5, 10);
        cropImageView.setGuidelines(1);

        File imgFile = new File(tempImagePath_fromIntent);

        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(tempImagePath_fromIntent);
            cropImageView.setImageBitmap(myBitmap);
        }

        doneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String finalImagePath = create_CroppedImageFile(cropImageView.getCroppedImage());
                delete_previous_uncropped_image(tempImagePath_fromIntent);

                if (CALL_PURPOSE == Constants.EDIT_BOOKMARK_PURPOSE_VALUE || CALL_PURPOSE == Constants.EDIT_BOOKMARK_IMAGE_PURPOSE_VALUE) {
                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_picture_changed", finalImagePath));
                } else {
                    Intent openCreateBookmark = new Intent(Crop_Image_Activity.this, Create_Bookmark_Activity.class);
                    openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
                    openCreateBookmark.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, finalImagePath);
                    openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_COLOR, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR));
                    startActivity(openCreateBookmark);
                }

                finish();
            }
        });

        rotateImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropImageView.rotateImage(90);
            }
        });
    }

    public void delete_previous_uncropped_image(String imagePath) {
        File file = new File(imagePath);
        file.delete();
    }

    public String create_CroppedImageFile(Bitmap inImage) {
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
