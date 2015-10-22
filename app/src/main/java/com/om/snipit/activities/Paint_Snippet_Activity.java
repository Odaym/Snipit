package com.om.snipit.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
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
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.snipit.R;
import com.om.snipit.classes.CanvasView;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.models.Snippet;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
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

public class Paint_Snippet_Activity extends Base_Activity {
    @InjectView(R.id.canvasView)
    CanvasView canvasView;
    @InjectView(R.id.snippetIMG)
    ImageView snippetIMG;
    @InjectView(R.id.imageProgressBar)
    ProgressBar imageProgressBar;
    @InjectView(R.id.multiple_actions_fab)
    FloatingActionsMenu floatingActionsMenu;
    @InjectView(R.id.color_actions_fab)
    FloatingActionsMenu floatingColorsMenu;
    @InjectView(R.id.fab_action_color)
    FloatingActionButton fabActionColor;
    @InjectView(R.id.fab_action_drawing_mode)
    FloatingActionButton fabActionDrawingMode;
    @InjectView(R.id.fab_action_undo)
    FloatingActionButton fabActionUndo;
    @InjectView(R.id.fab_action_thickness)
    FloatingActionButton fabActionThickness;

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Snippet, Integer> snippetDAO;

    private Snippet snippet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint_snippet);

        snippetDAO = getHelper().getSnippetDAO();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();
        prefsEditor.apply();

        ButterKnife.inject(this);

        getSupportActionBar().setTitle(getString(R.string.paint_snippet_activity_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        snippet = getIntent().getParcelableExtra(Constants.EXTRAS_SNIPPET);

        Callback picassoCallback = new Callback() {
            @Override
            public void onSuccess() {
                imageProgressBar.setVisibility(View.INVISIBLE);

                int canvasDrawingMode = prefs.getInt(Constants.CANVAS_DRAWING_MODE, 0);

                //If no drawing mode existed or drawing mode was set to PEN
                if (canvasDrawingMode == 0 || canvasDrawingMode == CanvasView.Drawer.PEN.ordinal())
                    canvasView.setDrawer(CanvasView.Drawer.PEN);
                else {
                    fabActionDrawingMode.setIconDrawable(getResources().getDrawable(R.drawable.ic_brush_white));
                    canvasView.setDrawer(CanvasView.Drawer.RECTANGLE);
                }

                canvasView.setPaintStrokeColor(prefs.getInt(Constants.BRUSH_COLOR_PREF, getResources().getColor(R.color.yellow)));
                canvasView.setPaintStrokeWidth(prefs.getFloat(Constants.BRUSH_THICKNESS_PREF, 60));
                canvasView.setBaseColor(Color.TRANSPARENT);
                canvasView.setOpacity(150);
            }

            @Override
            public void onError() {
            }
        };

        Picasso.with(Paint_Snippet_Activity.this).load(new File(snippet.getImage_path())).resize(1000, 1000).centerInside().into(snippetIMG, picassoCallback);

        fabActionUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canvasView.undo();
            }
        });

        fabActionDrawingMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (canvasView.getDrawer() == CanvasView.Drawer.PEN) {
                    canvasView.setDrawer(CanvasView.Drawer.RECTANGLE);
                    fabActionDrawingMode.setIconDrawable(getResources().getDrawable(R.drawable.ic_brush_white));
                } else {
                    canvasView.setDrawer(CanvasView.Drawer.PEN);
                    fabActionDrawingMode.setIconDrawable(getResources().getDrawable(R.drawable.ic_square_white));
                }
                prefsEditor.putInt(Constants.CANVAS_DRAWING_MODE, canvasView.getDrawer().ordinal());
                prefsEditor.apply();
            }
        });

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

                AlertDialog.Builder alert = new AlertDialog.Builder(Paint_Snippet_Activity.this);

                LayoutInflater inflater = (LayoutInflater) Paint_Snippet_Activity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View setBrushThicknessAlert = inflater.inflate(R.layout.alert_change_brush_thickness, null, false);

                final SeekBar brushThicknessBar = (SeekBar) setBrushThicknessAlert.findViewById(R.id.brushThicknessSeeker);

                float brushThicknessPref = prefs.getFloat(Constants.BRUSH_THICKNESS_PREF, 0);

                //First time editing brush thickness
                if (brushThicknessPref == 0)
                    brushThicknessBar.setProgress(60);
                else
                    brushThicknessBar.setProgress((int) brushThicknessPref);

                alert.setPositiveButton(Paint_Snippet_Activity.this.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        canvasView.setPaintStrokeWidth(brushThicknessBar.getProgress());
                        prefsEditor.putFloat(Constants.BRUSH_THICKNESS_PREF, brushThicknessBar.getProgress());
                        prefsEditor.apply();
                    }
                });

                alert.setNegativeButton(Paint_Snippet_Activity.this.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.setTitle(Paint_Snippet_Activity.this.getResources().getString(R.string.set_brush_thickness));
                alert.setView(setBrushThicknessAlert);
                alert.setMessage("");
                alert.show();
            }
        });
    }

    public void onFabColorButtonClicked(View view) {
        floatingColorsMenu.collapse();

        switch (view.getId()) {
            case R.id.fab_color_blue:
                canvasView.setPaintStrokeColor(getResources().getColor(R.color.blue));
                prefsEditor.putInt(Constants.BRUSH_COLOR_PREF, getResources().getColor(R.color.blue));
                break;
            case R.id.fab_color_green:
                canvasView.setPaintStrokeColor(getResources().getColor(R.color.green));
                prefsEditor.putInt(Constants.BRUSH_COLOR_PREF, getResources().getColor(R.color.green));
                break;
            case R.id.fab_color_yellow:
                canvasView.setPaintStrokeColor(getResources().getColor(R.color.yellow));
                prefsEditor.putInt(Constants.BRUSH_COLOR_PREF, getResources().getColor(R.color.yellow));
                break;
            case R.id.fab_color_red:
                canvasView.setPaintStrokeColor(getResources().getColor(R.color.red));
                prefsEditor.putInt(Constants.BRUSH_COLOR_PREF, getResources().getColor(R.color.red));
                break;
            case R.id.fab_color_white:
                canvasView.setPaintStrokeColor(getResources().getColor(R.color.white));
                prefsEditor.putInt(Constants.BRUSH_COLOR_PREF, getResources().getColor(R.color.white));
                break;
            case R.id.fab_back_to_options:
                floatingColorsMenu.setVisibility(View.INVISIBLE);
                floatingColorsMenu.collapse();

                floatingActionsMenu.setVisibility(View.VISIBLE);
                floatingActionsMenu.expand();
                break;
        }

        prefsEditor.apply();
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.paint_snippet, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_save:
                new AlertDialog.Builder(Paint_Snippet_Activity.this)
                        .setTitle(R.string.alert_dialog_save_title)
                        .setMessage(R.string.snippet_update_message)
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

        private Helper_Methods helperMethods;
        private int snippet_id = snippet.getId();
        private Snippet snippetBeingPainted = snippetDAO.queryForId(snippet_id);
        private Bitmap mBitmapOriginal, mBitmapNew;
        private ProgressDialog savePaintedSnippetDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            canvasView.setClickable(false);

            helperMethods = new Helper_Methods(Paint_Snippet_Activity.this);

            if (floatingActionsMenu.isExpanded())
                floatingActionsMenu.collapse();
            if (floatingColorsMenu.isExpanded())
                floatingColorsMenu.collapse();

            savePaintedSnippetDialog = ProgressDialog.show(Paint_Snippet_Activity.this, getResources().getString(R.string.save_painted_snippet_dialog_title),
                    getResources().getString(R.string.save_painted_snippet_dialog_message), true);

            mBitmapOriginal = ((BitmapDrawable) snippetIMG.getDrawable()).getBitmap();
            mBitmapNew = canvasView.getScaleBitmap(mBitmapOriginal.getWidth(), mBitmapOriginal.getHeight());
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                Bitmap mCBitmap = Bitmap.createBitmap(mBitmapOriginal.getWidth(), mBitmapOriginal.getHeight(), mBitmapOriginal.getConfig());

                Canvas tCanvas = new Canvas(mCBitmap);

                tCanvas.drawBitmap(mBitmapOriginal, 0, 0, null);

                tCanvas.drawBitmap(mBitmapNew, 0, 0, null);

                String finalImagePathAfterPaint = storeImage(mCBitmap);

                snippetBeingPainted.setImage_path(finalImagePathAfterPaint);

                snippetDAO.update(snippetBeingPainted);

                publishProgress(snippet.getImage_path(), finalImagePathAfterPaint);

            } catch (Exception e) {
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean errorSaving) {
            if (errorSaving) {
                Crouton.makeText(Paint_Snippet_Activity.this, getResources().getString(R.string.snippet_failed_update), Style.ALERT).show();
                helperMethods.showViewElement(snippetIMG);
                helperMethods.showViewElement(canvasView);

                savePaintedSnippetDialog.dismiss();
            } else {
                FlurryAgent.logEvent("Paint_Snipit");

                savePaintedSnippetDialog.dismiss();

                finish();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //Notify Snippets Activity to update the newly-painted image and delete the old one - send old path
            EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_image_updated", values[0]));

            //Notify Snippets Gallery Activity to update the newly-painted image and delete the old one - send old path
            EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_image_updated", values[0]));

            //Notify View Snippets Activity to update the newly-painted image - send new path
            EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_image_needs_reload", values[1]));

            super.onProgressUpdate(values);
        }
    }
}