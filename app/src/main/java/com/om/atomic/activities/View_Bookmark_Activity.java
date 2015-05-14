package com.om.atomic.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.melnykov.fab.FloatingActionButton;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.atomic.R;
import com.om.atomic.classes.Atomic_Application;
import com.om.atomic.classes.Bookmark;
import com.om.atomic.classes.Constants;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.om.atomic.classes.HackyViewPager;
import com.om.atomic.classes.Helper_Methods;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;
import uk.co.senab.photoview.PhotoViewAttacher;


public class View_Bookmark_Activity extends Base_Activity {

    private Tracker tracker;
    private ArrayList<Bookmark> bookmarks;
    private int NUM_PAGES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bookmarks);

        tracker = ((Atomic_Application) getApplication()).getTracker(Atomic_Application.TrackerName.APP_TRACKER);
        tracker.setScreenName("View_Bookmark");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        String book_title = getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
        int current_bookmark_position = getIntent().getExtras().getInt(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION);
        bookmarks = getIntent().getExtras().getParcelableArrayList("bookmarks");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(book_title);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        NUM_PAGES = bookmarks.size();

        final HackyViewPager mPager = (HackyViewPager) findViewById(R.id.pager);
        final ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(current_bookmark_position);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
            overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        FloatingActionButton createNewNoteBTN;
        @InjectView(R.id.paintBookmarkBTN)
        FloatingActionButton paintBookmarkBTN;
        @InjectView(R.id.imageProgressBar)
        ProgressBar imageProgressBar;

        private DatabaseHelper dbHelper;
        private Helper_Methods helperMethods;
        private Context context;
        private int rotation = 0;
        private String bookmark_imagepath, bookmark_name, bookmark_dateAdded;
        private int bookmark_pagenumber, bookmark_id;
        private Tracker tracker;

        private boolean clutterHidden = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            dbHelper = new DatabaseHelper(context);
            helperMethods = new Helper_Methods(context);
            tracker = ((Atomic_Application) getActivity().getApplication()).getTracker(Atomic_Application.TrackerName.APP_TRACKER);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.view_bookmark, menu);

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
                    imageProgressBar.setVisibility(View.VISIBLE);
                    Picasso.with(context).load(new File(bookmark_imagepath)).error(getResources().getDrawable(R.drawable.notfound_1)).resize(1500, 1500).centerInside().rotate(rotation).into(bookmarkIMG, new Callback() {
                        @Override
                        public void onSuccess() {
                            imageProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError() {
                            imageProgressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                case R.id.rotate_left:
                    imageProgressBar.setVisibility(View.VISIBLE);
                    rotation -= 90;
                    Picasso.with(context).load(new File(bookmark_imagepath)).error(getResources().getDrawable(R.drawable.notfound_1)).resize(1500, 1500).centerInside().rotate(rotation).into(bookmarkIMG, new Callback() {
                        @Override
                        public void onSuccess() {
                            imageProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError() {
                            imageProgressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                case R.id.share_picture:
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Core")
                            .setAction("Share Bookmark")
                            .build());

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
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {

            final ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_bookmarks, container, false);

            ButterKnife.inject(this, rootView);

            ((View_Bookmark_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ((View_Bookmark_Activity) context).getSupportActionBar().show();

            bookmarkNameTV.setText(bookmark_name);
            bookmarkPageNumberLabelTV.setText(getString(R.string.page));
            bookmarkPageNumberTV.setText(" " + String.valueOf(bookmark_pagenumber));
            bookmarkDateAddedTV.setText(bookmark_dateAdded);

            if (helperMethods.isBookmarkOnDisk(bookmark_imagepath)) {
                paintBookmarkBTN.setVisibility(View.VISIBLE);
            } else {
                paintBookmarkBTN.setVisibility(View.GONE);
            }

            Callback picassoCallback = new Callback() {
                @Override
                public void onSuccess() {
                    imageProgressBar.setVisibility(View.INVISIBLE);

                    createNewNoteBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(context);

                            LayoutInflater inflater = (LayoutInflater) context
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View alertCreateNoteView = inflater.inflate(R.layout.alert_create_bookmark_note, rootView, false);

                            final EditText inputNoteET = (EditText) alertCreateNoteView.findViewById(R.id.bookmarkNoteET);
                            inputNoteET.setText(dbHelper.getBookmarkNote(bookmark_id));
                            inputNoteET.setSelection(inputNoteET.getText().length());

                            alert.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dbHelper.update_BookmarkNote(bookmark_id, inputNoteET.getText().toString());

                                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_note_changed"));
                                }
                            });

                            alert.setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            });

                            inputNoteET.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    InputMethodManager keyboard = (InputMethodManager)
                                            context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    keyboard.showSoftInput(inputNoteET, 0);
                                }
                            }, 0);

                            alert.setTitle(context.getResources().getString(R.string.takeNote));
                            alert.setView(alertCreateNoteView);
                            alert.show();
                        }
                    });

                    paintBookmarkBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent openPaintActivity = new Intent(context, Paint_Bookmark_Activity.class);
                            openPaintActivity.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, bookmark_imagepath);
                            //Send the ID of the bookmark to be used in case the bookmark image needs to be updated
                            openPaintActivity.putExtra(Constants.EXTRAS_BOOKMARK_ID, bookmark_id);
                            startActivity(openPaintActivity);
                        }
                    });
                }

                @Override
                public void onError() {
                    imageProgressBar.setVisibility(View.INVISIBLE);

                    createNewNoteBTN.setAlpha(0.2f);
                    paintBookmarkBTN.setAlpha(0.2f);

                    createNewNoteBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Crouton.makeText(getActivity(), getResources().getString(R.string.image_not_found), Style.ALERT).show();
                        }
                    });

                    paintBookmarkBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Crouton.makeText(getActivity(), getResources().getString(R.string.image_not_found), Style.ALERT).show();
                        }
                    });
                }
            };

            if (!helperMethods.isBookmarkOnDisk(bookmark_imagepath)) {
                Picasso.with(context).load(bookmark_imagepath).error(getResources().getDrawable(R.drawable.notfound_1)).resize(1500, 1500).centerInside().into(bookmarkIMG, picassoCallback);
            } else {
                Picasso.with(context).load(new File(bookmark_imagepath)).error(getResources().getDrawable(R.drawable.notfound_1)).resize(1500, 1500).centerInside().into(bookmarkIMG, picassoCallback);
            }

            PhotoViewAttacher mAttacher = new PhotoViewAttacher(bookmarkIMG);
            mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float v, float v2) {
                    if (!clutterHidden) {
                        dealWithClutter(clutterHidden, view, bookmark_imagepath);
                    } else {
                        dealWithClutter(clutterHidden, view, bookmark_imagepath);
                    }

                    clutterHidden = !clutterHidden;
                }
            });

            return rootView;
        }

        @DebugLog
        public void dealWithClutter(final boolean wasHidden, final View view, String bookmark_imagepath) {
            ArrayList<ObjectAnimator> arrayListObjectAnimators = new ArrayList<ObjectAnimator>();
            Animator[] objectAnimators;

            if (wasHidden) {
                ((View_Bookmark_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Bookmark_Activity) context).getSupportActionBar().show();

                arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkDetailsView));
                arrayListObjectAnimators.add(helperMethods.showViewElement(createNewNoteBTN));

                if (helperMethods.isBookmarkOnDisk(bookmark_imagepath))
                    arrayListObjectAnimators.add(helperMethods.showViewElement(paintBookmarkBTN));

            } else {
                ((View_Bookmark_Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Bookmark_Activity) context).getSupportActionBar().hide();

                arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkDetailsView));
                arrayListObjectAnimators.add(helperMethods.hideViewElement(createNewNoteBTN));

                if (helperMethods.isBookmarkOnDisk(bookmark_imagepath))
                    arrayListObjectAnimators.add(helperMethods.hideViewElement(paintBookmarkBTN));
            }

            objectAnimators = arrayListObjectAnimators
                    .toArray(new ObjectAnimator[arrayListObjectAnimators
                            .size()]);

            AnimatorSet hideClutterSet = new AnimatorSet();

            hideClutterSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    view.setEnabled(false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setEnabled(true);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            hideClutterSet.playTogether(objectAnimators);
            hideClutterSet.setDuration(300);
            hideClutterSet.start();
        }
    }

    public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

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
            bundle.putInt(Constants.EXTRAS_BOOKMARK_ISNOTESHOWING, bookmarks.get(position).getIsNoteShowing());
            imageFragment.setArguments(bundle);

            return imageFragment;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }
}

