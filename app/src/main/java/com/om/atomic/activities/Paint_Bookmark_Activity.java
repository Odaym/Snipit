package com.om.atomic.activities;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.om.atomic.R;
import com.om.atomic.classes.Constants;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.panavtec.drawableview.DrawableView;
import me.panavtec.drawableview.DrawableViewConfig;

public class Paint_Bookmark_Activity extends BaseActivity {
    @InjectView(R.id.drawable_view)
    DrawableView drawableView;
    @InjectView(R.id.bookmarkIMG)
    ImageView bookmarkIMG;
    @InjectView(R.id.imageProgressBar)
    ProgressBar imageProgressBar;
    @InjectView(R.id.multiple_actions_fab)
    FloatingActionsMenu floatingActionsMenu;
    @InjectView(R.id.fab_action_color)
    FloatingActionButton fabActionColor;
    @InjectView(R.id.fab_action_undo)
    FloatingActionButton fabActionUndo;
    @InjectView(R.id.fab_action_clear)
    FloatingActionButton fabActionClear;
    @InjectView(R.id.fab_action_thickness)
    FloatingActionButton fabActionThickness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_up, R.anim.no_change);

        setContentView(R.layout.activity_paint_bookmark);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        ButterKnife.inject(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Picasso.with(Paint_Bookmark_Activity.this).load(new File(getIntent().getExtras().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH))).into(bookmarkIMG, new Callback() {
            @Override
            public void onSuccess() {
                imageProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError() {
                imageProgressBar.setVisibility(View.INVISIBLE);
            }
        });

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;

        final DrawableViewConfig config = new DrawableViewConfig();
        config.setStrokeColor(getResources().getColor(R.color.white));
        config.setStrokeWidth(20.0f);
        config.setMinZoom(1.0f);
        config.setMaxZoom(1.0f);
        config.setCanvasWidth(width);
        config.setCanvasHeight(200);
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

        fabActionColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                config.setStrokeColor(getResources().getColor(R.color.yellow));
                drawableView.setConfig(config);
                floatingActionsMenu.collapse();
            }
        });

//
//        bookmarkIMG.getViewTreeObserver().addOnPreDrawListener(
//                new ViewTreeObserver.OnPreDrawListener() {
//                    public boolean onPreDraw() {
//
//                        int height = bookmarkIMG.getMeasuredHeight();
//                        config.setCanvasHeight(500);
//
//                        Log.d("SIZE", "Canvas size set to " + height);
//                        return true;
//                    }
//                });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.no_change, R.anim.slide_down);
                return true;
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
}
