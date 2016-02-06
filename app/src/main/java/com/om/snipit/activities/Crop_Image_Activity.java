package com.om.snipit.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.isseiaoki.simplecropview.CropImageView;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.models.Book;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class Crop_Image_Activity extends Base_Activity {
    @Bind(R.id.doneBTN)
    FloatingActionButton doneBTN;
    @Bind(R.id.rotateImageBTN)
    FloatingActionButton rotateImageBTN;
    @Bind(R.id.cropImageView)
    CropImageView cropImageView;

    private Book book;

    private String tempImagePath_fromIntent;
    private int CALL_PURPOSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        ButterKnife.bind(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        book = getIntent().getParcelableExtra(Constants.EXTRAS_BOOK);

        helperMethods.setUpActionbarColors(this, book.getColorCode());

        tempImagePath_fromIntent = getIntent().getExtras().getString(Constants.EXTRAS_SNIPPET_TEMP_IMAGE_PATH);

        CALL_PURPOSE = getIntent().getIntExtra(Constants.EDIT_SNIPPET_PURPOSE_STRING, -1);

        Picasso.with(this).load(new File(tempImagePath_fromIntent)).resize(1000, 1000).centerInside().into(picassoCropTarget);

        File imageFileCheck = new File(tempImagePath_fromIntent);

        if (!imageFileCheck.exists() || cropImageView.getImageBitmap() == null) {
            Toast.makeText(Crop_Image_Activity.this, R.string.rare_case_scenario, Toast.LENGTH_LONG).show();
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
                    Intent openCreateSnippet = new Intent(Crop_Image_Activity.this, Create_Snippet_Activity.class);
                    openCreateSnippet.putExtra(Constants.EXTRAS_BOOK, book);
                    openCreateSnippet.putExtra(Constants.EXTRAS_SNIPPET_TEMP_IMAGE_PATH, finalImagePath);
                    startActivity(openCreateSnippet);
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
            inImage.compress(Bitmap.CompressFormat.PNG, 0, bos);
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
