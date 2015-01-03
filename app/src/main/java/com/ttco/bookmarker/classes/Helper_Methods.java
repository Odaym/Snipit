package com.ttco.bookmarker.classes;

import android.content.Context;
import android.net.ConnectivityManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;

public class Helper_Methods {

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static Spannable fontifyString(String string) {
        Spannable finalSpan = new SpannableString(string);
        finalSpan.setSpan(new TypefaceSpan("sans-serif-light"), 0, string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return finalSpan;
    }
}
