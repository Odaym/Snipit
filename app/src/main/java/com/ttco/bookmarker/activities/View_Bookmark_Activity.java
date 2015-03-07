package com.ttco.bookmarker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.EventBus_Poster;
import com.ttco.bookmarker.classes.EventBus_Singleton;
import com.ttco.bookmarker.classes.HackyViewPager;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoViewAttacher;


public class View_Bookmark_Activity extends ActionBarActivity {

    private static int currentapiVersion = android.os.Build.VERSION.SDK_INT;
    private ArrayList<Bookmark> bookmarks;
    private int NUM_PAGES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bookmarks);

        String book_title = getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
        int current_bookmark_position = getIntent().getExtras().getInt(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION);
        bookmarks = getIntent().getExtras().getParcelableArrayList("bookmarks");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(book_title);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

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
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment used to represent each image in the scrolling gallery
     */
    public static class View_Bookmark_Fragment extends Fragment {
        @InjectView(R.id.bookmarkIMG)
        ImageView bookmarkIMG;
        @InjectView(R.id.bookmarkPageNumberLabelTV)
        TextView bookmarkPageNumberLabelTV;
        @InjectView(R.id.bookmarkPageNumberTV)
        TextView bookmarkPageNumberTV;
        @InjectView(R.id.bookmarkDateAddedTV)
        TextView bookmarkDateAddedTV;
        @InjectView(R.id.bookmarkNameTV)
        TextView bookmarkNameTV;
        @InjectView(R.id.bookmarkDetailsView)
        RelativeLayout bookmarkDetailsView;
        @InjectView(R.id.createNewNoteBTN)
        ImageButton createNewNoteBTN;

        private DatabaseHelper dbHelper;
        private Context context;
        private int rotation = 0;
        private String bookmark_imagepath, bookmark_name, bookmark_dateAdded;
        private int bookmark_pagenumber, bookmark_order, bookmark_bookId, bookmark_id, bookmark_views, bookmark_isNoteShowing;

        private boolean clutterHidden = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            dbHelper = new DatabaseHelper(context);
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
                case R.id.share_picture:
                    String book_title = getActivity().getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
                    Uri imageURI = Uri.parse("file://" + bookmark_imagepath);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, imageURI);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Bookmark name: \"" + bookmark_name + "\"\nfrom book: \"" + book_title + "\"\non page: " + bookmark_pagenumber);
                    sendIntent.setType("*/*");
                    startActivity(Intent.createChooser(sendIntent, "Share using:"));
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            context = activity;

            bookmark_id = getArguments().getInt(Constants.EXTRAS_BOOKMARK_ID);
            bookmark_imagepath = getArguments().getString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH);
            bookmark_name = getArguments().getString(Constants.EXTRAS_BOOKMARK_NAME);
            bookmark_pagenumber = getArguments().getInt(Constants.EXTRAS_BOOKMARK_PAGENUMBER);
            bookmark_dateAdded = getArguments().getString(Constants.EXTRAS_BOOKMARK_DATE_ADDED);
            bookmark_bookId = getArguments().getInt(Constants.EXTRAS_FOREIGN_BOOK_ID);
            bookmark_order = getArguments().getInt(Constants.EXTRAS_BOOKMARK_ORDER);
            bookmark_views = getArguments().getInt(Constants.EXTRAS_BOOKMARK_VIEWS);
            bookmark_isNoteShowing = getArguments().getInt(Constants.EXTRAS_BOOKMARK_ISNOTESHOWING);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_bookmarks, container, false);

            ButterKnife.inject(this, rootView);

            bookmarkNameTV.setText(bookmark_name);
            bookmarkPageNumberLabelTV.setText(getString(R.string.page));
            bookmarkPageNumberTV.setText(" " + String.valueOf(bookmark_pagenumber));
            bookmarkDateAddedTV.setText(bookmark_dateAdded);

            if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
                createNewNoteBTN.setElevation(15f);
            }

            Picasso.with(context).load(new File(bookmark_imagepath)).error(context.getResources().getDrawable(R.drawable.sad_image_not_found)).resize(1500, 1500).centerInside().into(bookmarkIMG);

            PhotoViewAttacher mAttacher = new PhotoViewAttacher(bookmarkIMG);
            mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float v, float v2) {
                    if (!clutterHidden) {
                        dealWithClutter(clutterHidden);
                    } else {
                        dealWithClutter(clutterHidden);
                    }

                    clutterHidden = !clutterHidden;
                }
            });

            createNewNoteBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    alert.setTitle(context.getResources().getString(R.string.takeNote));
                    alert.setMessage("");

                    final EditText input = new EditText(context);
                    input.setHint(context.getResources().getString(R.string.writeSomething));
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(30, 0, 30, 0);
                    alert.setView(input);

                    input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    input.setText(dbHelper.getBookmarkNote(bookmark_id));
                    input.setSelection(input.getText().length());

                    alert.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //Prepare the object with all of its previous and currently-updated properties before the object gets updated in the DB, otherwise all unset properties will null or zero.
                            Bookmark bookmark = new Bookmark();
                            bookmark.setId(bookmark_id);
                            bookmark.setName(bookmark_name);
                            bookmark.setImage_path(bookmark_imagepath);
                            bookmark.setBookId(bookmark_bookId);
                            bookmark.setDate_added(bookmark_dateAdded);
                            bookmark.setOrder(bookmark_order);
                            bookmark.setNote(input.getText().toString());
                            bookmark.setPage_number(bookmark_pagenumber);
                            bookmark.setViews(bookmark_views);

                            dbHelper.updateBookmark(bookmark);

                            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_note_changed"));
                        }
                    });

                    alert.setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });

                    alert.show();
                    input.setLayoutParams(layoutParams);

                    input.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager keyboard = (InputMethodManager)
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            keyboard.showSoftInput(input, 0);
                        }
                    }, 0);
                }
            });

            return rootView;
        }

        public void dealWithClutter(final boolean show) {
            ArrayList<ObjectAnimator> arrayListObjectAnimators = new ArrayList<ObjectAnimator>();

            if (show) {
                ((View_Bookmark_Activity) context).getSupportActionBar().show();
                ObjectAnimator bookmarkDetailsAnimator = ObjectAnimator.ofFloat(bookmarkDetailsView, "Alpha", 0, 1);
                bookmarkDetailsAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        bookmarkDetailsView.setVisibility(View.VISIBLE);
                    }

                });
                arrayListObjectAnimators.add(bookmarkDetailsAnimator);

                ObjectAnimator createNewNoteAnimator = ObjectAnimator.ofFloat(createNewNoteBTN, "Alpha", 0, 1);
                createNewNoteAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        createNewNoteBTN.setVisibility(View.VISIBLE);
                    }
                });
                arrayListObjectAnimators.add(createNewNoteAnimator);

                ObjectAnimator[] objectAnimators = arrayListObjectAnimators
                        .toArray(new ObjectAnimator[arrayListObjectAnimators
                                .size()]);

                AnimatorSet hideClutterSet = new AnimatorSet();
                hideClutterSet.playTogether(objectAnimators);
                hideClutterSet.setDuration(300);
                hideClutterSet.start();
                ((View_Bookmark_Activity) context).getSupportActionBar().show();
                ((View_Bookmark_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                ObjectAnimator bookmarkDetailsAnimator = ObjectAnimator.ofFloat(bookmarkDetailsView, "Alpha", 1 - 0, 0);
                bookmarkDetailsAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        bookmarkDetailsView.setVisibility(View.INVISIBLE);
                    }

                });
                arrayListObjectAnimators.add(bookmarkDetailsAnimator);

                ObjectAnimator createNewNoteAnimator = ObjectAnimator.ofFloat(createNewNoteBTN, "Alpha", 1 - 0, 0);
                createNewNoteAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        createNewNoteBTN.setVisibility(View.INVISIBLE);
                    }
                });
                arrayListObjectAnimators.add(createNewNoteAnimator);

                Animator[] objectAnimators = arrayListObjectAnimators
                        .toArray(new Animator[arrayListObjectAnimators
                                .size()]);

                AnimatorSet hideClutterSet = new AnimatorSet();
                hideClutterSet.playTogether(objectAnimators);
                hideClutterSet.setDuration(300);
                hideClutterSet.start();
                ((View_Bookmark_Activity) context).getSupportActionBar().hide();
                ((View_Bookmark_Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
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
            bundle.putInt(Constants.EXTRAS_BOOK_ID, bookmarks.get(position).getBookId());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_ID, bookmarks.get(position).getId());
            bundle.putString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, bookmarks.get(position).getImage_path());
            bundle.putString(Constants.EXTRAS_BOOKMARK_NAME, bookmarks.get(position).getName());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_PAGENUMBER, bookmarks.get(position).getPage_number());
            bundle.putString(Constants.EXTRAS_BOOKMARK_DATE_ADDED, bookmarks.get(position).getDate_added());
            bundle.putString(Constants.EXTRAS_BOOKMARK_NOTE, bookmarks.get(position).getNote());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_VIEWS, bookmarks.get(position).getViews());
            imageFragment.setArguments(bundle);

            return imageFragment;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}

