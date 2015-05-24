package com.om.atomic.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.flurry.android.FlurryAgent;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.om.atomic.R;
import com.om.atomic.classes.Constants;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import hugo.weaving.DebugLog;
import me.panavtec.drawableview.DrawableView;
import me.panavtec.drawableview.DrawableViewConfig;

public class Paint_Bookmark_Activity extends Base_Activity {
    @InjectView(R.id.drawable_view)
    DrawableView drawableView;
    @InjectView(R.id.bookmarkIMG)
    ImageView bookmarkIMG;
    @InjectView(R.id.imageProgressBar)
    ProgressBar imageProgressBar;
    @InjectView(R.id.multiple_actions_fab)
    FloatingActionsMenu floatingActionsMenu;
    @InjectView(R.id.color_actions_fab)
    FloatingActionsMenu floatingColorsMenu;
    @InjectView(R.id.fab_action_color)
    FloatingActionButton fabActionColor;
    @InjectView(R.id.fab_action_undo)
    FloatingActionButton fabActionUndo;
    @InjectView(R.id.fab_action_clear)
    FloatingActionButton fabActionClear;
    @InjectView(R.id.fab_action_thickness)
    FloatingActionButton fabActionThickness;
    @InjectView(R.id.savingBookmarkProgressBar)
    SmoothProgressBar savingBookmarkProgressBar;

    private DrawableViewConfig config;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint_bookmark);

        overridePendingTransition(R.anim.slide_up, R.anim.no_change);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();
        prefsEditor.apply();

        dbHelper = new DatabaseHelper(this);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        ButterKnife.inject(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.paint_bookmark_activity_title));

        Callback picassoCallback = new Callback() {
            @Override
            public void onSuccess() {
                imageProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError() {
                imageProgressBar.setVisibility(View.INVISIBLE);
            }
        };

        try {
            //If the String was a URL then this bookmark is a sample
            new URL(getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH));
            Picasso.with(Paint_Bookmark_Activity.this).load(getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH)).resize(2000, 2000).centerInside().into(bookmarkIMG, picassoCallback);
        } catch (MalformedURLException e) {
            //Else it's on disk
            Picasso.with(Paint_Bookmark_Activity.this).load(new File(getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH))).resize(2000, 2000).centerInside().into(bookmarkIMG, picassoCallback);
        }

        final float scale = getResources().getDisplayMetrics().density;
        //formula for dp
        int height = (int) (480 * scale + 0.5f);

        config = new DrawableViewConfig();
        config.setStrokeColor(getResources().getColor(R.color.white_transparent));
        config.setStrokeWidth(prefs.getInt(Constants.BRUSH_THICKNESS_PREF, 20));
        config.setMinZoom(1.0f);
        config.setMaxZoom(1.0f);
        config.setCanvasWidth(getResources().getDisplayMetrics().widthPixels);
        config.setCanvasHeight(height);
        drawableView.setConfig(config);

        fabActionClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawableView.clear();
            }
        });

        fabActionUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawableView.undo();
            }
        });

        /***
         * Save functionality for Bookmark Image and Drawing ontop of it
         */
        fabActionColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floatingActionsMenu.collapse();
                floatingActionsMenu.setVisibility(View.GONE);

                floatingColorsMenu.setVisibility(View.VISIBLE);
                floatingColorsMenu.expand();
            }
        });

        fabActionThickness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                floatingActionsMenu.collapse();

                AlertDialog.Builder alert = new AlertDialog.Builder(Paint_Bookmark_Activity.this);

                LayoutInflater inflater = (LayoutInflater) Paint_Bookmark_Activity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View setBrushThicknessAlert = inflater.inflate(R.layout.alert_change_brush_thickness, null, false);

                final SeekBar brushThicknessBar = (SeekBar) setBrushThicknessAlert.findViewById(R.id.brushThicknessSeeker);

                int brushThicknessPref = prefs.getInt(Constants.BRUSH_THICKNESS_PREF, 0);

                //First time opening this activity
                if (brushThicknessPref == 0)
                    brushThicknessBar.setProgress(20);
                else
                    brushThicknessBar.setProgress(brushThicknessPref);

                brushThicknessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    int brushThickness;

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                        brushThickness = progresValue;

                        config.setStrokeWidth(progresValue);
                        drawableView.setConfig(config);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        prefsEditor.putInt(Constants.BRUSH_THICKNESS_PREF, brushThickness);
                        prefsEditor.apply();
                    }
                });

                alert.setPositiveButton(Paint_Bookmark_Activity.this.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        prefsEditor.putInt(Constants.BRUSH_THICKNESS_PREF, brushThicknessBar.getProgress());
                        prefsEditor.apply();
                    }
                });

                alert.setNegativeButton(Paint_Bookmark_Activity.this.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.setTitle(Paint_Bookmark_Activity.this.getResources().getString(R.string.set_brush_thickness));
                alert.setView(setBrushThicknessAlert);
                alert.setMessage("");
                alert.show();
            }
        });
    }

    public void onFabColorButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.fab_color_blue:
                config.setStrokeColor(getResources().getColor(R.color.blue_transparent));
                break;
            case R.id.fab_color_green:
                config.setStrokeColor(getResources().getColor(R.color.green_transparent));
                break;
            case R.id.fab_color_yellow:
                config.setStrokeColor(getResources().getColor(R.color.yellow_transparent));
                break;
            case R.id.fab_color_red:
                config.setStrokeColor(getResources().getColor(R.color.red_transparent));
                break;
            case R.id.fab_color_white:
                config.setStrokeColor(getResources().getColor(R.color.white_transparent));
                break;
            case R.id.fab_back_to_options:
                floatingColorsMenu.setVisibility(View.INVISIBLE);
                floatingColorsMenu.collapse();

                floatingActionsMenu.setVisibility(View.VISIBLE);
                floatingActionsMenu.expand();
                break;
        }

        drawableView.setConfig(config);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.paint_bookmark, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.no_change, R.anim.slide_down);
                return true;
            case R.id.menu_action_save:
                new AlertDialog.Builder(Paint_Bookmark_Activity.this)
                        .setTitle(R.string.alert_dialog_save_title)
                        .setMessage(R.string.bookmark_update_message)
                        .setPositiveButton(R.string.alert_dialog_save_action, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                new SavePaintedBookmark_Task().execute();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
            overridePendingTransition(R.anim.no_change, R.anim.slide_down);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private String storeImage(Bitmap image) {
        File pictureFile = createImageFile();
        if (pictureFile == null) {
            Log.d("TAG",
                    "Error creating media file, check storage permissions: ");
            return "";
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 0, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("TAG", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("TAG", "Error accessing file: " + e.getMessage());
        }

        return pictureFile.getAbsolutePath();
    }

    @DebugLog
    private File createImageFile() {
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

        return new File(mediaStorageDir.getPath() + File.separator + imageFileName);
    }

    private class SavePaintedBookmark_Task extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            savingBookmarkProgressBar.setVisibility(View.VISIBLE);

            if (floatingActionsMenu.isExpanded())
                floatingActionsMenu.collapse();
            if (floatingColorsMenu.isExpanded())
                floatingColorsMenu.collapse();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                Paint mPaint1 = new Paint();
                Paint mPaint2 = new Paint();

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();

                Bitmap mBitmap1 = BitmapFactory.decodeFile(getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH), bmOptions);

                Bitmap mBitmap2 = drawableView.obtainBitmap();

                Bitmap mCBitmap = Bitmap.createBitmap(mBitmap1.getWidth(), mBitmap1.getHeight(), mBitmap1.getConfig());

                Canvas tCanvas = new Canvas(mCBitmap);

                tCanvas.drawBitmap(mBitmap1, 0, 0, mPaint1);

                tCanvas.drawBitmap(mBitmap2, 0, 0, mPaint2);

                String finalImagePathAfterPaint = storeImage(mCBitmap);

                dbHelper.update_BookmarkImage(getIntent().getExtras().getInt(Constants.EXTRAS_BOOKMARK_ID, -1), finalImagePathAfterPaint);

                publishProgress(getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH), finalImagePathAfterPaint);

            } catch (Exception e) {
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean errorSaving) {
            if (errorSaving)
                Crouton.makeText(Paint_Bookmark_Activity.this, getResources().getString(R.string.bookmark_failed_update), Style.ALERT).show();
            else {
                FlurryAgent.logEvent("Bookmark_Paint");

                savingBookmarkProgressBar.setVisibility(View.INVISIBLE);
                finish();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //Notify Bookmarks Activity to update the newly-painted image and delete the old one - send old path
            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_image_updated", values[0]));

            //Notify View Bookmarks Activity to update the newly-painted image - send new path
            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_image_needs_reload", values[1]));

            super.onProgressUpdate(values);
        }
    }
}
