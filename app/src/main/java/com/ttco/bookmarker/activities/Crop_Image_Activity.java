package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.edmodo.cropper.CropImageView;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Crop_Image_Activity extends Activity {

    private CropImageView cropImageView;
    private String finalImagePath_forDeletion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_image_activity);

        finalImagePath_forDeletion = getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);

        cropImageView = (CropImageView) findViewById(R.id.cropImageView);

        cropImageView.setAspectRatio(5, 10);
        cropImageView.setGuidelines(1);

        File imgFile = new File(finalImagePath_forDeletion);

        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            cropImageView.setImageBitmap(myBitmap);
        }
    }

    public void handleDone_Pressed(View view) {
        String finalPathForCreateBookmark = create_CroppedImageFile(cropImageView.getCroppedImage());

        Intent openCreateBookmark = new Intent(Crop_Image_Activity.this, Create_Bookmark_Activity.class);
        openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
        openCreateBookmark.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, finalPathForCreateBookmark);
        startActivity(openCreateBookmark);

        finish();

        delete_previous_uncropped_image(finalImagePath_forDeletion);
    }

    public void delete_previous_uncropped_image(String imagePath){
        File file = new File(imagePath);
        file.delete();
    }

    public void rotateImage_Pressed(View view) {
        cropImageView.rotateImage(90);
    }

    public String create_CroppedImageFile(Bitmap inImage) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Atomic");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
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
