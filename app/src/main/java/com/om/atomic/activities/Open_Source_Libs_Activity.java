package com.om.atomic.activities;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.om.atomic.R;
import com.om.atomic.classes.Helper_Methods;
import com.om.atomic.classes.Open_Source_Library;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class Open_Source_Libs_Activity extends Base_Activity {
    @InjectView(R.id.librariesList)
    ListView librariesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_libs);

        ButterKnife.inject(this);

        overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.about_open_source_libraries_activity_title));

        Helper_Methods helperMethods = new Helper_Methods(this);

        if (helperMethods.getCurrentapiVersion() >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(getResources().getColor(R.color.darker_purple));

        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.purple));

        ArrayList<Open_Source_Library> libraries = new ArrayList<>();

        libraries.add(new Open_Source_Library("Picasso", "https://github.com/square/picasso", "A powerful image downloading and caching library for Android."));
        libraries.add(new Open_Source_Library("Otto", "http://square.github.io/otto/", "An enhanced event bus with emphasis on Android support."));
        libraries.add(new Open_Source_Library("NineOldAndroids", "http://nineoldandroids.com/", "Android library for using the Honeycomb (Android 3.0) animation API on all versions of the platform back to 1.0!"));
        libraries.add(new Open_Source_Library("ButterKnife", "https://github.com/JakeWharton/butterknife/", "Field and method binding for Android views which uses annotation processing to generate boilerplate code for you."));
        libraries.add(new Open_Source_Library("Calligraphy", "https://github.com/chrisjenx/Calligraphy", "Custom fonts in Android an OK way."));
        libraries.add(new Open_Source_Library("DrawableView", "https://github.com/PaNaVTEC/DrawableView", "An Android view that allows to paint with a finger in the screen and saves the result as a Bitmap."));
        libraries.add(new Open_Source_Library("PhotoView", "https://github.com/chrisbanes/PhotoView", "PhotoView aims to help produce an easily usable implementation of a zooming Android ImageView. It is currently being used in photup."));
        libraries.add(new Open_Source_Library("Cropper", "https://github.com/edmodo/cropper", "The Cropper is an image cropping tool. It provides a way to set an image in XML and programmatically, and displays a resizable crop window on top of the image."));
        libraries.add(new Open_Source_Library("Crouton", "https://github.com/keyboardsurfer/Crouton", "Context sensitive notifications for Android."));
        libraries.add(new Open_Source_Library("FloatingActionButton", "https://github.com/futuresimple/android-floating-action-button", "Yet another library for drawing Material Design promoted actions."));
        libraries.add(new Open_Source_Library("SmoothProgressBar", "https://github.com/castorflex/SmoothProgressBar", "Small library allowing you to make a smooth indeterminate progress bar."));
        libraries.add(new Open_Source_Library("Autofit TextView", "https://github.com/grantland/android-autofittextview", "A TextView that automatically resizes text to fit perfectly within its bounds."));
        libraries.add(new Open_Source_Library("Glide", "https://github.com/bumptech/glide", "Glide is a fast and efficient open source media management and image loading framework for Android that wraps media decoding, memory and disk caching, and resource pooling into a simple and easy to use interface."));
        libraries.add(new Open_Source_Library("Float Labeled EditText", "https://github.com/wrapp/floatlabelededittext", "Simple implementation of a Float Labeled EditText: An Android ViewGroup which uses a child EditText and puts the hint on top of the EditText when it is populated with text."));
        libraries.add(new Open_Source_Library("Android Form EditText", "https://github.com/vekexasia/android-edittext-validator", "Android form edit text is an extension of EditText that brings data validation facilities to the edittext."));


        final View listViewFooterAd = View.inflate(this, R.layout.open_source_libs_list_adview_footer, null);
        AdView mAdView = (AdView) listViewFooterAd.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        librariesList.addFooterView(listViewFooterAd);
        librariesList.setAdapter(new Libraries_Adapter(this, libraries));
    }

    public class Libraries_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private ArrayList<Open_Source_Library> libraries;
        private LibrariesViewHolder holder;

        public Libraries_Adapter(Context context, ArrayList<Open_Source_Library> libraries) {
            super();

            this.inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.libraries = libraries;
        }

        @Override
        public int getCount() {
            return libraries.size();
        }

        @Override
        public Object getItem(int i) {
            return libraries.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.open_source_libs_list_item, parent, false);

                holder = new LibrariesViewHolder();
                holder.libraryNameTV = (TextView) convertView.findViewById(R.id.libraryNameTV);
                holder.librarySiteLinkTV = (TextView) convertView.findViewById(R.id.librarySiteLinkTV);
                holder.libraryDescriptionTV = (TextView) convertView.findViewById(R.id.libraryDescriptionTV);

                convertView.setTag(holder);
            } else {
                holder = (LibrariesViewHolder) convertView.getTag();
            }

            holder.libraryNameTV.setText(libraries.get(position).getName());

            holder.librarySiteLinkTV.setClickable(true);
            holder.librarySiteLinkTV.setMovementMethod(LinkMovementMethod.getInstance());
            String text = "<a href='" + libraries.get(position).getWebsite() + "'> " + libraries.get(position).getWebsite() + "</a>";
            holder.librarySiteLinkTV.setText(Html.fromHtml(text));

            holder.libraryDescriptionTV.setText(libraries.get(position).getDescription());

            return convertView;
        }
    }

    public static class LibrariesViewHolder {
        TextView libraryNameTV;
        TextView librarySiteLinkTV;
        TextView libraryDescriptionTV;
    }
}
