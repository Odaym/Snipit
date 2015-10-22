package com.om.snipit.activities;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.Open_Source_Library;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class Open_Source_Libs_Activity extends Base_Activity {
    @InjectView(R.id.librariesList)
    ListView librariesList;
    @InjectView(R.id.introductionTextTV)
    TextView introductionTextTV;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_libs);

        ButterKnife.inject(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        toolbar.setTitle(getResources().getString(R.string.about_open_source_libraries_activity_title));
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        introductionTextTV.setMovementMethod(LinkMovementMethod.getInstance());

        helperMethods.setUpActionbarColors(this, Constants.OPEN_SOURCE_LIBS_ACTIVITY_TOOLBAR_COLORS);

        ArrayList<Open_Source_Library> libraries = new ArrayList<>();

        libraries.add(new Open_Source_Library("Picasso", "https://github.com/square/picasso", "A powerful image downloading and caching library for Android."));
        libraries.add(new Open_Source_Library("Otto", "http://square.github.io/otto/", "An enhanced event bus with emphasis on Android support."));
        libraries.add(new Open_Source_Library("ButterKnife", "https://github.com/JakeWharton/butterknife/", "Field and method binding for Android views which uses annotation processing to generate boilerplate code for you."));
        libraries.add(new Open_Source_Library("PhotoView", "https://github.com/chrisbanes/PhotoView", "PhotoView aims to help produce an easily usable implementation of a zooming Android ImageView. It is currently being used in photup."));
        libraries.add(new Open_Source_Library("SimpleCropView", "https://github.com/IsseiAoki/SimpleCropView", "SimpleCropView is an image cropping library for Android."));
        libraries.add(new Open_Source_Library("Crouton", "https://github.com/keyboardsurfer/Crouton", "Context sensitive notifications for Android."));
        libraries.add(new Open_Source_Library("Autofit TextView", "https://github.com/grantland/android-autofittextview", "A TextView that automatically resizes text to fit perfectly within its bounds."));
        libraries.add(new Open_Source_Library("Android Form EditText", "https://github.com/vekexasia/android-edittext-validator", "Android form edit text is an extension of EditText that brings data validation facilities to the edittext."));

//        final View listViewFooterAd = View.inflate(this, R.layout.open_source_libs_list_adview_footer, null);
//        AdView mAdView = (AdView) listViewFooterAd.findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

//        librariesList.addFooterView(listViewFooterAd);
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
