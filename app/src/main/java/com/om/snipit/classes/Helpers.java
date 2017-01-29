package com.om.snipit.classes;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.view.View;
import com.andreabaccega.widget.FormEditText;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.snipit.BuildConfig;
import com.om.snipit.R;
import com.om.snipit.activities.BaseActivity;
import hugo.weaving.DebugLog;
import java.io.File;
import java.util.ArrayList;

public class Helpers {

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

    if (!BuildConfig.DEBUG) Answers.getInstance().logCustom(customEvent);
  }

  public static void setupStatusBarColors(Context context, int bookColorCode) {
    switch (bookColorCode) {
      case Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_green));
        }
        break;
      case Constants.OPEN_SOURCE_LIBS_ACTIVITY_TOOLBAR_COLORS:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_purple));
        }
        break;
      case 0:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_pink));
        }
        break;
      case 1:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_red));
        }
        break;
      case 2:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_purple));
        }
        break;
      case 3:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_yellow));
        }
        break;
      case 4:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_blue));
        }
        break;
      case 5:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ((BaseActivity) context).getWindow()
              .setStatusBarColor(context.getResources().getColor(R.color.darker_brown));
        }
        break;
    }
  }

  @DebugLog public static void delete_image_from_disk(String imagePath) {
    File file = new File(imagePath);
    if (file.exists()) {
      file.delete();
    }
  }

  //Used to determine whether to allow scanning of Book Barcodes
  public static boolean isInternetAvailable(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
  }

  public static boolean validateFields(ArrayList<FormEditText> allFields) {
    boolean allValid = true;

    for (FormEditText field : allFields) {
      allValid = field.testValidity() && allValid;
    }

    return allValid;
  }

  public static int determineNoteViewBackground(int book_color_code) {
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

  public static ObjectAnimator hideViewElement(final View view) {
    ObjectAnimator elementAnimator = ObjectAnimator.ofFloat(view, "Alpha", 1, 0);
    elementAnimator.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animator) {
        view.setVisibility(View.INVISIBLE);
      }
    });

    return elementAnimator;
  }

  public static ObjectAnimator showViewElement(final View view) {
    ObjectAnimator elementAnimator = ObjectAnimator.ofFloat(view, "Alpha", 0, 1);
    elementAnimator.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationStart(Animator animator) {
        view.setVisibility(View.VISIBLE);
      }
    });

    return elementAnimator;
  }
}
