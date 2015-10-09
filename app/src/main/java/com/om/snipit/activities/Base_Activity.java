package com.om.snipit.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.Param;

public class Base_Activity extends ActionBarActivity {
    private String activityName = this.getClass().getSimpleName();

    private DatabaseHelper databaseHelper;
    private Param animationsParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RuntimeExceptionDao<Param, Integer> paramDAO = getHelper().getParamDAO();

        animationsParam = paramDAO.queryForId(Constants.ANIMATIONS_DATABASE_VALUE);

        performIntroAnimationCheck();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                performOutroAnimationCheck();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
            performOutroAnimationCheck();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void performIntroAnimationCheck() {
        //If Animations are enabled
        if (animationsParam.isEnabled()) {
            //Only set this animation to the Activities that are NOT Paint_Bookmark and NOT Books
            if (!activityName.equals(Constants.BOOKS_ACTIVITY_NAME) && !activityName.equals(Constants.PAINT_SNIPPET_ACTIVITY_NAME))
                overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);
                //Only set this animation to the Activities that ARE Paint_Bookmark because it takes a different animation than all the above cases (Books also gets a different animation, and that's NO animation) - 3 cases
            else if (!activityName.equals(Constants.BOOKS_ACTIVITY_NAME))
                overridePendingTransition(R.anim.slide_up, R.anim.no_change);
        }
    }

    public void performOutroAnimationCheck() {

        //If Animations are enabled
        if (animationsParam.isEnabled()) {
            //Only set this animation to the Activities that are NOT Paint_Bookmark and NOT Books
            if (!activityName.equals(Constants.BOOKS_ACTIVITY_NAME) && !activityName.equals(Constants.PAINT_SNIPPET_ACTIVITY_NAME))
                overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
                //Only set this animation to the Activities that ARE Paint_Bookmark because it takes a different animation than all the above cases (Books also gets a different animation, and that's NO animation) - 3 cases
            else if (!activityName.equals(Constants.BOOKS_ACTIVITY_NAME))
                overridePendingTransition(R.anim.no_change, R.anim.slide_down);
        }
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }
}
