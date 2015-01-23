package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import com.edmodo.cropper.CropImageView;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;


public class Crop_Image_Activity extends Activity {

    private String finalImagePath;
    private CropImageView cropImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_test);

        finalImagePath = getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);

        cropImageView = (CropImageView) findViewById(R.id.cropImageView);

        cropImageView.setAspectRatio(5, 10);
        cropImageView.setGuidelines(1);

        File imgFile = new File(finalImagePath);

        if (imgFile.exists()) {

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            cropImageView.setImageBitmap(myBitmap);
        }
    }

    public void handleDone_Pressed(View view) {
        Uri tempUri = getImageUri(getApplicationContext(), cropImageView.getCroppedImage());
        File finalPathForCreateBookmark = new File(getRealPathFromURI(tempUri));

        Intent openCreateBookmark = new Intent(Crop_Image_Activity.this, Create_Bookmark_Activity.class);
        openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
        openCreateBookmark.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, finalPathForCreateBookmark.toString());
        startActivity(openCreateBookmark);

        finish();
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }
}
