package com.om.snipit.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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
import com.om.snipit.R;
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
import hugo.weaving.DebugLog;
import uk.co.senab.photoview.PhotoViewAttacher;

public class View_Snippet_Activity extends Base_Activity {

    private String extras_search_term;
    private boolean extras_viewing_snippets_gallery = false;
    private int book_id;
    private String book_title;

    private List<Snippet> snippets;
    private int NUM_PAGES;
    private int current_snippet_position;
    private ScreenSlidePagerAdapter mPagerAdapter;

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Snippet, Integer> snippetDAO;
    private QueryBuilder<Snippet, Integer> snippetQueryBuilder;
    private PreparedQuery<Snippet> pq;

    @InjectView(R.id.pager)
    HackyViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_snippets);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EventBus_Singleton.getInstance().register(this);

        ButterKnife.inject(this);

        snippetDAO = getHelper().getSnipitDAO();
        snippetQueryBuilder = snippetDAO.queryBuilder();

        //These two fields are common amongst both Intents that lead up to this Activity. Whether we are in Snippets Gallery or not, and what the position of the current Snippet is within the list of all grabbed snippets, perfect.
        extras_viewing_snippets_gallery = getIntent().getExtras().getBoolean(Constants.EXTRAS_VIEWING_SNIPPETS_GALLERY);
        current_snippet_position = getIntent().getExtras().getInt(Constants.EXTRAS_CURRENT_SNIPPET_POSITION);

        if (extras_viewing_snippets_gallery) {
            //If viewing snippets from Snippets Gallery
            getSupportActionBar().setTitle(getResources().getString(R.string.all_snippets_activity_title));
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
        mPager.setCurrentItem(current_snippet_position);
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        if (ebp.getMessage().equals("snippet_image_needs_reload")) {

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
                snippets = snippetDAO.queryForAll();
            } else if (extras_search_term.equals(Constants.EXTRAS_NO_SEARCH_TERM)) {
                snippetQueryBuilder.where().eq("book_id", book_id);
                snippetQueryBuilder.orderBy("order", false);

                pq = snippetQueryBuilder.prepare();
                snippets = snippetDAO.query(pq);
            } else {
                SelectArg nameSelectArg = new SelectArg("%" + extras_search_term + "%");
                SelectArg noteSelectArg = new SelectArg("%" + extras_search_term + "%");

                snippetQueryBuilder.where().eq("book_id", book_id).and().like("name", nameSelectArg).or().like("note", noteSelectArg);
                snippetQueryBuilder.orderBy("order", false);

                pq = snippetQueryBuilder.prepare();
                snippets = snippetDAO.query(pq);
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
        @InjectView(R.id.snippetIMG)
        ImageView snippetIMG;
        @InjectView(R.id.snippetPageNumberLabelTV)
        TextView snippetPageNumberLabelTV;
        @InjectView(R.id.snippetPageNumberTV)
        TextView snippetPageNumberTV;
        @InjectView(R.id.snippetDateAddedTV)
        TextView snippetDateAddedTV;
        @InjectView(R.id.snippetNameTV)
        TextView snippetNameTV;
        @InjectView(R.id.snippetDetailsView)
        RelativeLayout snippetDetailsView;
        @InjectView(R.id.createNewNoteBTN)
        FloatingActionButton createNewNoteBTN;
        @InjectView(R.id.paintBookmarkBTN)
        FloatingActionButton paintBookmarkBTN;
        @InjectView(R.id.imageProgressBar)
        ProgressBar imageProgressBar;

        private Callback picassoCallback;

        private Helper_Methods helperMethods;
        private Context context;

        private String snippet_imagepath, snippet_name, snippet_dateAdded;
        private int snippet_pagenumber, snippet_id;

        private boolean clutterHidden = false;

        private DatabaseHelper databaseHelper;
        private RuntimeExceptionDao<Snippet, Integer> snippetDAO;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            helperMethods = new Helper_Methods(context);
            snippetDAO = getHelper().getSnipitDAO();

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

                            final EditText inputNoteET = (EditText) alertCreateNoteView.findViewById(R.id.snippetNoteET);
                            inputNoteET.setHintTextColor(getActivity().getResources().getColor(R.color.edittext_hint_color));
                            inputNoteET.setText(snippetDAO.queryForId(snippet_id).getNote());
                            inputNoteET.setSelection(inputNoteET.getText().length());

                            alert.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Snippet snippetToUpdate = snippetDAO.queryForId(snippet_id);
                                    snippetToUpdate.setNote(inputNoteET.getText().toString());
                                    snippetDAO.update(snippetToUpdate);

                                    EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_note_changed"));
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
                            openPaintActivity.putExtra(Constants.EXTRAS_SNIPPET_IMAGE_PATH, snippet_imagepath);
//                            openPaintActivity.putExtra(Constants.EXTRAS_CURRENT_SNIPPET_POSITION, mPager.getCurrentItem())
                            //Send the ID of the snippet to be used in case the snippet image needs to be updated
                            openPaintActivity.putExtra(Constants.EXTRAS_SNIPPET_ID, snippet_id);
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
            inflater.inflate(R.menu.view_snippet, menu);

            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
//                case R.id.run_ocr:
//                    if (Helper_Methods.isInternetAvailable(getActivity())) {
//                        new AsyncTask_ProcessOCR((View_Snippet_Activity) getActivity()).execute(snippet_imagepath, "results.txt");
//                    } else {
//                        Crouton.makeText(getActivity(), getString(R.string.action_needs_internet), Style.ALERT).show();
//                    }
//                    break;
                case R.id.share_snippet:
                    String book_title = getActivity().getIntent().getExtras().getString(Constants.EXTRAS_BOOK_TITLE);
                    Uri imageURI = Uri.parse(snippet_imagepath);

                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, imageURI);

                    //Include the page number in the sharing message if a page number exists, otherwise don't
                    if (snippet_pagenumber == Constants.NO_SNIPPET_PAGE_NUMBER) {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.sharing_message) + "\nTitle: \"" + snippet_name + "\"\nFrom: \"" + book_title + "\"");
                    } else {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.sharing_message) + "\nTitle: \"" + snippet_name + "\"\nFrom: \"" + book_title + "\"\nPage: " + snippet_pagenumber);
                    }

                    sendIntent.setType("image/*");
                    startActivity(Intent.createChooser(sendIntent, "Share using:"));
                    break;
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            context = activity;

            snippet_id = getArguments().getInt(Constants.EXTRAS_SNIPPET_ID);
            snippet_imagepath = getArguments().getString(Constants.EXTRAS_SNIPPET_IMAGE_PATH);
            snippet_name = getArguments().getString(Constants.EXTRAS_SNIPPET_NAME);
            snippet_pagenumber = getArguments().getInt(Constants.EXTRAS_SNIPPET_PAGENUMBER);
            snippet_dateAdded = getArguments().getString(Constants.EXTRAS_SNIPPET_DATE_ADDED);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {

            final ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_snippets, container, false);

            ButterKnife.inject(this, rootView);

            ((View_Snippet_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ((View_Snippet_Activity) context).getSupportActionBar().show();

            snippetNameTV.setText(snippet_name);

            if (snippet_pagenumber == Constants.NO_SNIPPET_PAGE_NUMBER) {
                snippetPageNumberLabelTV.setVisibility(View.INVISIBLE);
                snippetPageNumberTV.setVisibility(View.INVISIBLE);
            } else {
                snippetPageNumberLabelTV.setText(getString(R.string.page));
                snippetPageNumberTV.setText(" " + String.valueOf(snippet_pagenumber));
            }

            snippetDateAddedTV.setText(snippet_dateAdded);

            if (helperMethods.isBookmarkOnDisk(snippet_imagepath)) {
                paintBookmarkBTN.setVisibility(View.VISIBLE);
            } else {
                paintBookmarkBTN.setVisibility(View.GONE);
            }

            Picasso.with(context).load(new File(snippet_imagepath)).error(getResources().getDrawable(R.drawable.snippet_not_found)).resize(1500, 1500).centerInside().into(snippetIMG, picassoCallback);

            PhotoViewAttacher mAttacher = new PhotoViewAttacher(snippetIMG);
            mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float v, float v2) {
                    if (!clutterHidden) {
                        dealWithClutter(clutterHidden, view, snippet_imagepath);
                    } else {
                        dealWithClutter(clutterHidden, view, snippet_imagepath);
                    }

                    clutterHidden = !clutterHidden;
                }
            });

            return rootView;
        }

        @DebugLog
        public void dealWithClutter(final boolean wasHidden, final View view, String snippet_imagepath) {
            ArrayList<ObjectAnimator> arrayListObjectAnimators = new ArrayList<ObjectAnimator>();
            Animator[] objectAnimators;

            if (wasHidden) {
                ((View_Snippet_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Snippet_Activity) context).getSupportActionBar().show();

                arrayListObjectAnimators.add(helperMethods.showViewElement(snippetDetailsView));
                arrayListObjectAnimators.add(helperMethods.showViewElement(createNewNoteBTN));
                arrayListObjectAnimators.add(helperMethods.showViewElement(paintBookmarkBTN));

            } else {
                ((View_Snippet_Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Snippet_Activity) context).getSupportActionBar().hide();

                arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetDetailsView));
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
            bundle.putInt(Constants.EXTRAS_SNIPPET_ID, snippets.get(position).getId());
            bundle.putString(Constants.EXTRAS_SNIPPET_IMAGE_PATH, snippets.get(position).getImage_path());
            bundle.putString(Constants.EXTRAS_SNIPPET_NAME, snippets.get(position).getName());
            bundle.putInt(Constants.EXTRAS_SNIPPET_PAGENUMBER, snippets.get(position).getPage_number());
            bundle.putString(Constants.EXTRAS_SNIPPET_DATE_ADDED, snippets.get(position).getDate_added());
            bundle.putString(Constants.EXTRAS_SNIPPET_NOTE, snippets.get(position).getNote());
            bundle.putInt(Constants.EXTRAS_SNIPPET_VIEWS, snippets.get(position).getViews());
            bundle.putInt(Constants.EXTRAS_SNIPPET_ISNOTESHOWING, snippets.get(position).getIsNoteShowing());
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
