package com.om.snipit.classes;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.View;

import com.andreabaccega.widget.FormEditText;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.snipit.R;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import hugo.weaving.DebugLog;

public class Helper_Methods {
    private Context context;
    private int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    public Helper_Methods(Context context) {
        this.context = context;
    }

    @DebugLog
    public static void delete_image_from_disk(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean isBookmarkOnDisk(String bookmarkImagePath) {
        try {
            //If the String was a URL then this bookmark is a sample
            new URL(bookmarkImagePath);
        } catch (MalformedURLException e) {
            //Else it's on disk
            return true;
        }

        return false;
    }

    public boolean validateFields(ArrayList<FormEditText> allFields) {
        boolean allValid = true;

        for (FormEditText field : allFields) {
            allValid = field.testValidity() && allValid;
        }

        return allValid;
    }

//    public void uploadBookDataToParse(DatabaseHelperasdasd dbHelper) {
//        List<Book> books = dbHelper.getAllBooks();
//
//        for (Book book : books) {
//            ParseObject bookObject = new ParseObject("Book");
//            bookObject.put("title", book.getTitle());
//            bookObject.put("author", book.getAuthor());
//            bookObject.saveInBackground();
//        }
//    }
//
//    public void uploadBookmarkDataToParse(DatabaseHelperasdasd dbHelper) {
//        List<Book> books = dbHelper.getAllBooks();
//        List<Bookmark> bookmarks;
//
//        for (Book book : books) {
//            bookmarks = dbHelper.getAllBookmarks(book.getId());
//            for (Bookmark bookmark : bookmarks) {
//                ParseObject bookmarkObject = new ParseObject("Bookmark");
//                bookmarkObject.put("book_id", bookmark.getBookId());
//                bookmarkObject.put("title", bookmark.getName());
//                bookmarkObject.put("page_number", bookmark.getPage_number());
//                bookmarkObject.saveInBackground();
//            }
//        }
//    }

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
            case Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_green));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.green));
                break;
            case Constants.OPEN_SOURCE_LIBS_ACTIVITY_TOOLBAR_COLORS:
                if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP)
                    activity.getWindow().setStatusBarColor(context.getResources().getColor(R.color.darker_purple));
                if (activity.getSupportActionBar() != null)
                    activity.getSupportActionBar().setBackgroundDrawable(context.getResources().getDrawable(R.color.purple));
                break;
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

    public int getCurrentapiVersion() {
        return currentapiVersion;
    }
}
