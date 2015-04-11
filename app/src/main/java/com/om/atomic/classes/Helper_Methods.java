package com.om.atomic.classes;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.View;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.atomic.R;

import java.util.Random;

public class Helper_Methods {
    private Context context;
    private int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    public Helper_Methods(Context context) {
        this.context = context;
    }

    //Used to determine whether to allow scanning of Book Barcodes
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    //Used for Showcase Views
    public static Spannable fontifyString(String string) {
        Spannable finalSpan = new SpannableString(string);
        finalSpan.setSpan(new TypefaceSpan("sans-serif-light"), 0, string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return finalSpan;
    }

    public void setUpActionbarColors(Activity activityReference, int book_color_code) {
        ActionBarActivity activity = (ActionBarActivity) activityReference;

        switch (book_color_code) {
            case 0:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_pink));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.pink));
                break;
            case 1:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_red));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.red));
                break;
            case 2:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_purple));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.purple));
                break;
            case 3:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_yellow));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.yellow));
                break;
            case 4:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_blue));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.blue));
                break;
            case 5:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_brown));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.brown));
                break;
        }
    }

    public int determineNoteViewBackground(int book_color_code) {
        switch (book_color_code) {
            case 0:
                return R.color.darker_pink;
            case 1:
                return R.color.darker_red;
            case 2:
                return R.color.darker_purple;
            case 3:
                return R.color.darker_yellow;
            case 4:
                return R.color.darker_blue;
            case 5:
                return R.color.darker_brown;
        }

        return 0;
    }

    public ObjectAnimator hideViewElement(final View view) {
        ObjectAnimator elementAnimator = ObjectAnimator.ofFloat(view, "Alpha", 1, 0);
        elementAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.INVISIBLE);
            }
        });

        return elementAnimator;
    }

    public ObjectAnimator showViewElement(final View view) {
        ObjectAnimator elementAnimator = ObjectAnimator.ofFloat(view, "Alpha", 0, 1);
        elementAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setVisibility(View.VISIBLE);
            }
        });

        return elementAnimator;
    }

    public int getCurrentapiVersion() {
        return currentapiVersion;
    }
}
