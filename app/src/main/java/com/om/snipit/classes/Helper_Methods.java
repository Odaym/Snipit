package com.om.snipit.classes;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.andreabaccega.widget.FormEditText;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.snipit.R;

import java.io.File;
import java.util.ArrayList;

import hugo.weaving.DebugLog;

public class Helper_Methods {
    private Context context;

    public Helper_Methods(Context context) {
        this.context = context;
    }

    public static void logEvent(String eventName, String[] eventValue) {
        CustomEvent customEvent = new CustomEvent(eventName);

        switch (eventName) {
            case "Created Book":
                customEvent.putCustomAttribute("Book name", eventValue[0]);
                break;
            case "Created Snippet":
                customEvent.putCustomAttribute("Snippet name", eventValue[0]);
                break;
            case "OCR Scan Run":
                customEvent.putCustomAttribute("OCR Language", eventValue[0]);
                break;
            case "Shared Snippet":
                customEvent.putCustomAttribute("Snippet name", eventValue[0]);
                customEvent.putCustomAttribute("Screen name", eventValue[1]);
                break;
        }

        if (Constants.APPLICATION_CODE_STATE.equals("PRODUCTION"))
            Answers.getInstance().logCustom(customEvent);
    }

    @DebugLog
    public static void delete_image_from_disk(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean validateFields(ArrayList<FormEditText> allFields) {
        boolean allValid = true;

        for (FormEditText field : allFields) {
            allValid = field.testValidity() && allValid;
        }

        return allValid;
    }

    //Used to determine whether to allow scanning of Book Barcodes
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public void setUpActionbarColors(Activity activityReference, int book_color_code) {
        AppCompatActivity activity = (AppCompatActivity) activityReference;

        switch (book_color_code) {
            case Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_green));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.green));
                break;
            case Constants.OPEN_SOURCE_LIBS_ACTIVITY_TOOLBAR_COLORS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_purple));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.purple));
                break;
            case 0:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_pink));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.pink));
                break;
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_red));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.red));
                break;
            case 2:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_purple));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.purple));
                break;
            case 3:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_yellow));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.yellow));
                break;
            case 4:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_blue));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.blue));
                break;
            case 5:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_brown));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.brown));
                break;
        }
    }

    public int determineNoteViewBackground(int book_color_code) {
        switch (book_color_code) {
            case 0:
                return R.color.pink;
            case 1:
                return R.color.red;
            case 2:
                return R.color.purple;
            case 3:
                return R.color.yellow;
            case 4:
                return R.color.blue;
            case 5:
                return R.color.brown;
        }

        return 0;
    }

    public int determineFabButtonsColor(int book_color_code) {
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
}
