package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.HackyViewPager;

import java.io.File;
import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;


public class View_Bookmark_Activity extends FragmentActivity {

    private ArrayList<Bookmark> bookmarks;
    private int NUM_PAGES;
    private String sorting_type_pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bookmarks);

        int book_id = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID);
        String book_title = getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
        int current_bookmark_position = getIntent().getExtras().getInt(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION);
        bookmarks = getIntent().getExtras().getParcelableArrayList("bookmarks");

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(book_title);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

//        DatabaseHelper dbHelper = new DatabaseHelper(this);

//        if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
//            bookmarks = dbHelper.getAllBookmarks(book_id, null);
//        } else {
//            bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
//        }

        NUM_PAGES = bookmarks.size();

        HackyViewPager mPager = (HackyViewPager) findViewById(R.id.pager);
        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(current_bookmark_position);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment used to represent each image in the scrolling gallery
     */

    public static class View_Bookmark_Fragment extends Fragment {
        private Context context;
        private ImageView bookmarkIMG;
        private TextView bookmarkNameTV, bookmarkPageNumberLabelTV, bookmarkPageNumberTV, bookmarkDateAddedTV;
        private int rotation = 0;
        private String bookmark_imagepath, bookmark_name, bookmark_dateAdded;
        private int bookmark_pagenumber;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.view_bookmarks, menu);

            MenuItem rotate_left_item = menu.getItem(0);
            MenuItem rotate_right_item = menu.getItem(1);

            SpannableString rotate_left_string = new SpannableString(rotate_left_item.getTitle().toString());
            rotate_left_string.setSpan(new ForegroundColorSpan(Color.BLACK), 0, rotate_left_string.length(), 0);
            rotate_left_item.setTitle(rotate_left_string);

            SpannableString rotate_right_string = new SpannableString(rotate_right_item.getTitle().toString());
            rotate_right_string.setSpan(new ForegroundColorSpan(Color.BLACK), 0, rotate_right_string.length(), 0);
            rotate_right_item.setTitle(rotate_right_string);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.rotate_right:
                    rotation += 90;
                    Picasso.with(context).load(new File(bookmark_imagepath)).error(context.getResources().getDrawable(R.drawable.sad_image_not_found)).resize(1500, 1500).centerInside().rotate(rotation).into(bookmarkIMG);
                    break;
                case R.id.rotate_left:
                    rotation -= 90;
                    Picasso.with(context).load(new File(bookmark_imagepath)).error(context.getResources().getDrawable(R.drawable.sad_image_not_found)).resize(1500, 1500).centerInside().rotate(rotation).into(bookmarkIMG);
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            context = activity;

            bookmark_imagepath = getArguments().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);
            bookmark_name = getArguments().getString(Constants.EXTRAS_BOOKMARK_NAME);
            bookmark_pagenumber = getArguments().getInt(Constants.EXTRAS_BOOKMARK_PAGENUMBER);
            bookmark_dateAdded = getArguments().getString(Constants.EXTRAS_BOOKMARK_DATE_ADDED);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_bookmarks, container, false);

            bookmarkIMG = (ImageView) rootView.findViewById(R.id.bookmarkIMG);
            bookmarkNameTV = (TextView) rootView.findViewById(R.id.bookmarkNameTV);
            bookmarkPageNumberLabelTV = (TextView) rootView.findViewById(R.id.bookmarkPageNumberLabelTV);
            bookmarkPageNumberTV = (TextView) rootView.findViewById(R.id.bookmarkPageNumberTV);
            bookmarkDateAddedTV = (TextView) rootView.findViewById(R.id.bookmarkDateAddedTV);

            bookmarkNameTV.setText(bookmark_name);
            bookmarkPageNumberLabelTV.setText(getString(R.string.page));
            bookmarkPageNumberTV.setText(" " + String.valueOf(bookmark_pagenumber));
            bookmarkDateAddedTV.setText(bookmark_dateAdded);

            Picasso.with(context).load(new File(bookmark_imagepath)).error(context.getResources().getDrawable(R.drawable.sad_image_not_found)).resize(1500, 1500).centerInside().into(bookmarkIMG);

            PhotoViewAttacher mAttacher = new PhotoViewAttacher(bookmarkIMG);

            return rootView;
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            View_Bookmark_Fragment imageFragment = new View_Bookmark_Fragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, bookmarks.get(position).getImage_path());
            bundle.putString(Constants.EXTRAS_BOOKMARK_NAME, bookmarks.get(position).getName());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_PAGENUMBER, bookmarks.get(position).getPage_number());
            bundle.putString(Constants.EXTRAS_BOOKMARK_DATE_ADDED, bookmarks.get(position).getDate_added());
            imageFragment.setArguments(bundle);
            return imageFragment;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}

