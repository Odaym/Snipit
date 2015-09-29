package com.om.snipit.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;
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

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.atomic.R;
import com.om.snipit.abbyy_ocr.AsyncTask_ProcessOCR;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.HackyViewPager;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.Snippet;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;
import uk.co.senab.photoview.PhotoViewAttacher;

public class View_Snippet_Activity extends Base_Activity {

    private String extras_search_term;
    private boolean extras_viewing_snippets_gallery = false;
    private int book_id;
    private String book_title;

    private List<Snippet> snippets;
    private int NUM_PAGES;
    private int current_bookmark_position;
    private ScreenSlidePagerAdapter mPagerAdapter;

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Snippet, Integer> bookmarkDAO;
    private QueryBuilder<Snippet, Integer> bookmarkQueryBuilder;
    private PreparedQuery<Snippet> pq;

    @InjectView(R.id.pager)
    HackyViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_snippets);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EventBus_Singleton.getInstance().register(this);

        ButterKnife.inject(this);

        bookmarkDAO = getHelper().getSnipitDAO();
        bookmarkQueryBuilder = bookmarkDAO.queryBuilder();

        //These two fields are common amongst both Intents that lead up to this Activity. Whether we are in Snippets Gallery or not, and what the position of the current Snippet is within the list of all grabbed snippets, perfect.
        extras_viewing_snippets_gallery = getIntent().getExtras().getBoolean(Constants.EXTRAS_VIEWING_SNIPPETS_GALLERY);
        current_bookmark_position = getIntent().getExtras().getInt(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION);

        if (extras_viewing_snippets_gallery) {
            //If viewing snippets from Snippets Gallery
            getSupportActionBar().setTitle("Zabri");
        } else {
            //If viewing snippets from a Collection, not from Snippets Gallery
            book_title = getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
            book_id = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID);
            extras_search_term = getIntent().getExtras().getString(Constants.EXTRAS_SEARCH_TERM, Constants.EXTRAS_NO_SEARCH_TERM);
            getSupportActionBar().setTitle(book_title);
        }

        handleWhichBookmarksToLoad();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        NUM_PAGES = snippets.size();

        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(current_bookmark_position);
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        if (ebp.getMessage().equals("bookmark_image_needs_reload")) {

            handleWhichBookmarksToLoad();

            mPagerAdapter.notifyDataSetChanged();
        }
    }

    public void updateResults(Boolean success) {
        if (!success)
            return;
        try {
            StringBuilder contents = new StringBuilder();

            FileInputStream fis = openFileInput("results.txt");
            try {
                Reader reader = new InputStreamReader(fis, "UTF-8");
                BufferedReader bufReader = new BufferedReader(reader);
                String text = null;
                while ((text = bufReader.readLine()) != null) {
                    contents.append(text).append(System.getProperty("line.separator"));
                }
            } finally {
                fis.close();
            }

            displayMessage(contents.toString());
        } catch (Exception e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    public void displayMessage(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OCR Scan Results")
                .setMessage(text)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void handleWhichBookmarksToLoad() {
        try {
            if (extras_viewing_snippets_gallery) {
                snippets = bookmarkDAO.queryForAll();
            } else if (extras_search_term.equals(Constants.EXTRAS_NO_SEARCH_TERM)) {
                bookmarkQueryBuilder.where().eq("book_id", book_id);
                bookmarkQueryBuilder.orderBy("order", false);

                pq = bookmarkQueryBuilder.prepare();
                snippets = bookmarkDAO.query(pq);
            } else {
                SelectArg nameSelectArg = new SelectArg("%" + extras_search_term + "%");
                SelectArg noteSelectArg = new SelectArg("%" + extras_search_term + "%");

                bookmarkQueryBuilder.where().eq("book_id", book_id).and().like("name", nameSelectArg).or().like("note", noteSelectArg);
                bookmarkQueryBuilder.orderBy("order", false);

                pq = bookmarkQueryBuilder.prepare();
                snippets = bookmarkDAO.query(pq);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        EventBus_Singleton.getInstance().unregister(this);
        super.onDestroy();
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

        private MenuItem favoriteBookmark_Item;

        private Callback picassoCallback;

        private Helper_Methods helperMethods;
        private Context context;
        private int rotation = 0;

        private Snippet tempSnippet;

        private String bookmark_imagepath, bookmark_name, bookmark_dateAdded;
        private int bookmark_pagenumber, bookmark_id;

        private boolean clutterHidden = false;

        private DatabaseHelper databaseHelper;
        private RuntimeExceptionDao<Snippet, Integer> bookmarkDAO;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            helperMethods = new Helper_Methods(context);
            bookmarkDAO = getHelper().getSnipitDAO();

            picassoCallback = new Callback() {
                @Override
                public void onSuccess() {
                    //Because they become disabled if an error occurred while loading the image
                    if (!createNewNoteBTN.isEnabled()) {
                        createNewNoteBTN.setEnabled(true);
                        createNewNoteBTN.setAlpha(1f);
                    }
                    if (!paintBookmarkBTN.isEnabled()) {
                        paintBookmarkBTN.setEnabled(true);
                        paintBookmarkBTN.setAlpha(1f);
                    }

                    imageProgressBar.setVisibility(View.INVISIBLE);

                    createNewNoteBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(context);

                            LayoutInflater inflater = (LayoutInflater) context
                                    .getSystemService(LAYOUT_INFLATER_SERVICE);
                            View alertCreateNoteView = inflater.inflate(R.layout.alert_create_snippet_note, null, false);

                            final EditText inputNoteET = (EditText) alertCreateNoteView.findViewById(R.id.bookmarkNoteET);
                            inputNoteET.setHintTextColor(getActivity().getResources().getColor(R.color.edittext_hint_color));
                            inputNoteET.setText(bookmarkDAO.queryForId(bookmark_id).getNote());
                            inputNoteET.setSelection(inputNoteET.getText().length());

                            alert.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Snippet snippetToUpdate = bookmarkDAO.queryForId(bookmark_id);
                                    snippetToUpdate.setNote(inputNoteET.getText().toString());
                                    bookmarkDAO.update(snippetToUpdate);

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
                                            context.getSystemService(INPUT_METHOD_SERVICE);
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
                            Intent openPaintActivity = new Intent(context, Paint_Snippet_Activity.class);
                            openPaintActivity.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, bookmark_imagepath);
//                            openPaintActivity.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, mPager.getCurrentItem())
                            //Send the ID of the bookmark to be used in case the bookmark image needs to be updated
                            openPaintActivity.putExtra(Constants.EXTRAS_BOOKMARK_ID, bookmark_id);
                            startActivity(openPaintActivity);
                        }
                    });
                }

                @Override
                public void onError() {
                    imageProgressBar.setVisibility(View.INVISIBLE);

                    createNewNoteBTN.setEnabled(false);
                    createNewNoteBTN.setAlpha(0.2f);
                    paintBookmarkBTN.setEnabled(false);
                    paintBookmarkBTN.setAlpha(0.2f);
                }
            };

        }

        public DatabaseHelper getHelper() {
            if (databaseHelper == null) {
                databaseHelper =
                        OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
            }

            return databaseHelper;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.view_bookmark, menu);

            MenuItem rotate_left_item = menu.getItem(0);
            MenuItem rotate_right_item = menu.getItem(1);

            favoriteBookmark_Item = menu.getItem(4);

            tempSnippet = bookmarkDAO.queryForId(bookmark_id);

            if (tempSnippet.isFavorite())
                favoriteBookmark_Item.setIcon(getResources().getDrawable(android.R.drawable.star_big_on));
            else
                favoriteBookmark_Item.setIcon(getResources().getDrawable(android.R.drawable.star_big_off));

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
                case R.id.run_ocr:
                    if (Helper_Methods.isInternetAvailable(getActivity())) {
                        new AsyncTask_ProcessOCR((View_Snippet_Activity) getActivity()).execute(bookmark_imagepath, "results.txt");
                    } else {
                        Crouton.makeText(getActivity(), getString(R.string.action_needs_internet), Style.ALERT).show();
                    }
                    break;
                case R.id.rotate_right:
                    rotation += 90;
                    imageProgressBar.setVisibility(View.VISIBLE);
                    Picasso.with(context).load(new File(bookmark_imagepath)).error(getResources().getDrawable(R.drawable.bookmark_not_found)).noPlaceholder().resize(2000, 2000).centerInside().rotate(rotation).into(bookmarkIMG, new Callback() {
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
                    Picasso.with(context).load(new File(bookmark_imagepath)).error(getResources().getDrawable(R.drawable.bookmark_not_found)).noPlaceholder().resize(2000, 2000).centerInside().rotate(rotation).into(bookmarkIMG, new Callback() {
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
                case R.id.share_bookmark:
                    String book_title = getActivity().getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
                    Uri imageURI = Uri.parse(bookmark_imagepath);

                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, imageURI);

                    //Include the page number in the sharing message if a page number exists, otherwise don't
                    if (bookmark_pagenumber == Constants.NO_BOOKMARK_PAGE_NUMBER) {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.sharing_message) + "\nTitle: \"" + bookmark_name + "\"\nFrom: \"" + book_title + "\"");
                    } else {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.sharing_message) + "\nTitle: \"" + bookmark_name + "\"\nFrom: \"" + book_title + "\"\nPage: " + bookmark_pagenumber);
                    }

                    sendIntent.setType("image/*");
                    startActivity(Intent.createChooser(sendIntent, "Share using:"));
                    break;
                case R.id.favorite_bookmark:
                    if (tempSnippet.isFavorite()) {
                        tempSnippet.setFavorite(false);
                        favoriteBookmark_Item.setIcon(getResources().getDrawable(android.R.drawable.star_big_off));
                    } else {
                        tempSnippet.setFavorite(true);
                        favoriteBookmark_Item.setIcon(getResources().getDrawable(android.R.drawable.star_big_on));
                    }

                    bookmarkDAO.update(tempSnippet);

                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_favorited"));
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
                    R.layout.fragment_snippets, container, false);

            ButterKnife.inject(this, rootView);

            ((View_Snippet_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ((View_Snippet_Activity) context).getSupportActionBar().show();

            bookmarkNameTV.setText(bookmark_name);

            if (bookmark_pagenumber == Constants.NO_BOOKMARK_PAGE_NUMBER) {
                bookmarkPageNumberLabelTV.setVisibility(View.INVISIBLE);
                bookmarkPageNumberTV.setVisibility(View.INVISIBLE);
            } else {
                bookmarkPageNumberLabelTV.setText(getString(R.string.page));
                bookmarkPageNumberTV.setText(" " + String.valueOf(bookmark_pagenumber));
            }

            bookmarkDateAddedTV.setText(bookmark_dateAdded);

            if (helperMethods.isBookmarkOnDisk(bookmark_imagepath)) {
                paintBookmarkBTN.setVisibility(View.VISIBLE);
            } else {
                paintBookmarkBTN.setVisibility(View.GONE);
            }

            Picasso.with(context).load(new File(bookmark_imagepath)).error(getResources().getDrawable(R.drawable.bookmark_not_found)).resize(2000, 2000).centerInside().into(bookmarkIMG, picassoCallback);

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
                ((View_Snippet_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Snippet_Activity) context).getSupportActionBar().show();

                arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkDetailsView));
                arrayListObjectAnimators.add(helperMethods.showViewElement(createNewNoteBTN));
                arrayListObjectAnimators.add(helperMethods.showViewElement(paintBookmarkBTN));

            } else {
                ((View_Snippet_Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Snippet_Activity) context).getSupportActionBar().hide();

                arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkDetailsView));
                arrayListObjectAnimators.add(helperMethods.hideViewElement(createNewNoteBTN));
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
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            View_Bookmark_Fragment imageFragment = new View_Bookmark_Fragment();
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.EXTRAS_BOOK_ID, snippets.get(position).getBookId());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_ID, snippets.get(position).getId());
            bundle.putString(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, snippets.get(position).getImage_path());
            bundle.putString(Constants.EXTRAS_BOOKMARK_NAME, snippets.get(position).getName());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_PAGENUMBER, snippets.get(position).getPage_number());
            bundle.putString(Constants.EXTRAS_BOOKMARK_DATE_ADDED, snippets.get(position).getDate_added());
            bundle.putString(Constants.EXTRAS_BOOKMARK_NOTE, snippets.get(position).getNote());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_VIEWS, snippets.get(position).getViews());
            bundle.putInt(Constants.EXTRAS_BOOKMARK_ISNOTESHOWING, snippets.get(position).getIsNoteShowing());
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

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }
}
