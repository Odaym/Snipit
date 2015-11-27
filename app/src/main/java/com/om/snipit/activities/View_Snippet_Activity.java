package com.om.snipit.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.snipit.R;
import com.om.snipit.abbyy_ocr.AsyncTask_ProcessOCR;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.HackyViewPager;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
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
import butterknife.Bind;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;
import uk.co.senab.photoview.PhotoViewAttacher;

public class View_Snippet_Activity extends Base_Activity {

    private String extras_search_term;
    private boolean extras_viewing_snippets_gallery = false;
    private Book book;

    private List<Snippet> snippets;
    private int NUM_PAGES;
    private int current_snippet_position;
    private ScreenSlidePagerAdapter mPagerAdapter;

    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Snippet, Integer> snippetDAO;
    private QueryBuilder<Snippet, Integer> snippetQueryBuilder;
    private PreparedQuery<Snippet> pq;

    @Bind(R.id.pager)
    HackyViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_snippets);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EventBus_Singleton.getInstance().register(this);

        ButterKnife.bind(this);

        snippetDAO = getHelper().getSnippetDAO();
        snippetQueryBuilder = snippetDAO.queryBuilder();

        //These two fields are common amongst both Intents that lead up to this Activity. Whether we are in Snippets Gallery or not, and what the position of the current Snippet is within the list of all grabbed snippets, perfect.
        extras_viewing_snippets_gallery = getIntent().getExtras().getBoolean(Constants.EXTRAS_VIEWING_SNIPPETS_GALLERY);
        current_snippet_position = getIntent().getExtras().getInt(Constants.EXTRAS_CURRENT_SNIPPET_POSITION);
        book = getIntent().getParcelableExtra(Constants.EXTRAS_BOOK);

        if (book != null) {
            getSupportActionBar().setTitle(book.getTitle());
        } else {
            finish();
        }

        //If viewing snippets from a Collection, not from Snippets Gallery
        if (!extras_viewing_snippets_gallery)
            extras_search_term = getIntent().getExtras().getString(Constants.EXTRAS_SEARCH_TERM, Constants.EXTRAS_NO_SEARCH_TERM);

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
        switch (ebp.getMessage()) {
            case "snippet_image_needs_reload":
                handleWhichBookmarksToLoad();

                mPagerAdapter.notifyDataSetChanged();
                break;
            case "snippet_ocr_content_changed":
                handleWhichBookmarksToLoad();

                mPagerAdapter.notifyDataSetChanged();
                break;
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
                String text;
                while ((text = bufReader.readLine()) != null) {
                    contents.append(text).append(System.getProperty("line.separator"));
                }
            } finally {
                fis.close();
            }

            if (contents.toString().length() == 0 || contents.toString().length() == 2)
                displayMessage(Constants.OCR_SCAN_SUCCESS_EMPTY, getString(R.string.ocr_scan_empty));
            else
                displayMessage(Constants.OCR_SCAN_SUCCESS, contents.toString());
        } catch (Exception e) {
            displayMessage(Constants.OCR_SCAN_ERROR, getString(R.string.ocr_scan_error));
        }
    }

    public void displayMessage(int ocrResultCode, final String ocrResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alert;
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(getResources().getDimensionPixelSize(R.dimen.ocr_scan_result_layout_padding_sides), 0, getResources().getDimensionPixelSize(R.dimen.ocr_scan_result_layout_padding_sides), 0);

        switch (ocrResultCode) {
            case Constants.OCR_SCAN_SUCCESS:
                final EditText ocrResultET = new EditText(this);

                ocrResultET.setText(ocrResult);

                lay.addView(ocrResultET);

                builder.setTitle(R.string.ocr_scan_result);
                builder.setView(lay);
                builder.setPositiveButton(R.string.SAVE, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        snippets.get(mPager.getCurrentItem()).setOcr_content(ocrResultET.getText().toString());
                        snippetDAO.update(snippets.get(mPager.getCurrentItem()));

                        EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_ocr_content_changed"));
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                break;
            case Constants.OCR_SCAN_SUCCESS_EMPTY:
            case Constants.OCR_SCAN_ERROR:
                TextView ocrErrorResultTV = new TextView(this);

                ocrErrorResultTV.setText(ocrResult);

                builder.setTitle(R.string.ocr_scan_result);
                builder.setMessage(ocrResult);
                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                break;
        }

        alert = builder.create();
        alert.show();
    }

    public void handleWhichBookmarksToLoad() {
        try {
            if (extras_viewing_snippets_gallery) {
                snippets = snippetDAO.queryForAll();
            } else if (extras_search_term.equals(Constants.EXTRAS_NO_SEARCH_TERM)) {
                snippetQueryBuilder.where().eq("book_id", book.getId());
                snippetQueryBuilder.orderBy("order", true);

                pq = snippetQueryBuilder.prepare();
                snippets = snippetDAO.query(pq);
            } else {
                SelectArg nameSelectArg = new SelectArg("%" + extras_search_term + "%");
                SelectArg noteSelectArg = new SelectArg("%" + extras_search_term + "%");
                SelectArg ocrSelectArg = new SelectArg("%" + extras_search_term + "%");

                snippetQueryBuilder.where().eq("book_id", book.getId()).and().like("name", nameSelectArg).or().like("note", noteSelectArg).or().like("ocr_content", ocrSelectArg);
                snippetQueryBuilder.orderBy("order", true);

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
        @Bind(R.id.snippetIMG)
        ImageView snippetIMG;
        @Bind(R.id.snippetPageNumberLabelTV)
        TextView snippetPageNumberLabelTV;
        @Bind(R.id.snippetPageNumberTV)
        TextView snippetPageNumberTV;
        @Bind(R.id.snippetDateAddedTV)
        TextView snippetDateAddedTV;
        @Bind(R.id.snippetNameTV)
        TextView snippetNameTV;
        @Bind(R.id.snippetDetailsView)
        RelativeLayout snippetDetailsView;
        @Bind(R.id.snippet_actions_fab)
        FloatingActionsMenu snippetActionsBTN;
        @Bind(R.id.createNewNoteBTN)
        FloatingActionButton createNewNoteBTN;
        @Bind(R.id.paintSnippetBTN)
        FloatingActionButton paintSnippetBTN;
        @Bind(R.id.ocrSnippetBTN)
        FloatingActionButton ocrSnippetBTN;
        @Bind(R.id.imageProgressBar)
        ProgressBar imageProgressBar;

        private Callback picassoCallback;

        private Helper_Methods helperMethods;
        private Context context;

        private Snippet snippet;
        private Book book;

        private boolean clutterHidden = false;

        private DatabaseHelper databaseHelper;
        private RuntimeExceptionDao<Snippet, Integer> snippetDAO;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            helperMethods = new Helper_Methods(context);
            snippetDAO = getHelper().getSnippetDAO();

            picassoCallback = new Callback() {
                @Override
                public void onSuccess() {
                    //Because they become disabled if an error occurred while loading the image
                    if (!snippetActionsBTN.isEnabled()) {
                        snippetActionsBTN.setEnabled(true);
                        snippetActionsBTN.setAlpha(1f);
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
                            inputNoteET.setText(snippetDAO.queryForId(snippet.getId()).getNote());
                            inputNoteET.setSelection(inputNoteET.getText().length());

                            alert.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    snippet.setNote(inputNoteET.getText().toString());
                                    snippetDAO.update(snippet);

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

                    paintSnippetBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent openPaintActivity = new Intent(context, Paint_Snippet_Activity.class);
                            openPaintActivity.putExtra(Constants.EXTRAS_SNIPPET, snippet);
                            startActivity(openPaintActivity);
                        }
                    });

                    ocrSnippetBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if ((snippet.getOcr_content() != null && snippet.getOcr_content().isEmpty()) || snippet.getOcr_content() == null) {
                                if (Helper_Methods.isInternetAvailable(getActivity())) {
                                    AlertDialog.Builder ocrLanguagePickerDialog = new AlertDialog.Builder(getActivity());
                                    ocrLanguagePickerDialog.setTitle(R.string.ocr_recognition_languages_alert_title);
                                    final String[] types = {"English",
                                            "Arabic",
                                            "Albanian",
                                            "Armenian (Eastern)",
                                            "Armenian (Grabar)",
                                            "Armenian (Western)",
                                            "Azerbaijani (Cyrillic)",
                                            "Azerbaijani (Latin)",
                                            "Belarussian",
                                            "Bulgarian",
                                            "Catalan",
                                            "Chechen",
                                            "Chinese Simplified",
                                            "Chinese Traditional",
                                            "Corsican",
                                            "Croatian",
                                            "Czech",
                                            "Danish",
                                            "Dutch (Netherlands)",
                                            "Dutch (Belgium)",
                                            "Eskimo (Cyrillic)",
                                            "Eskimo (Latin)",
                                            "Estonian",
                                            "Finnish",
                                            "French",
                                            "Ganda",
                                            "German",
                                            "German (New spelling)",
                                            "German (Luxembourg)",
                                            "Greek",
                                            "Hawaiian",
                                            "Hebrew",
                                            "Hungarian",
                                            "Icelandic",
                                            "Indonesian",
                                            "Irish",
                                            "Italian",
                                            "Japanese",
                                            "Kazakh",
                                            "Kongo",
                                            "Korean",
                                            "Korean (Hangul)",
                                            "Koryak",
                                            "Kurdish",
                                            "Lak",
                                            "Latin",
                                            "Latvian",
                                            "Lithuanian",
                                            "Macedonian",
                                            "Malay (Malaysian)",
                                            "Mongol",
                                            "Nahuatl",
                                            "Norwegian (Bokmal)",
                                            "Norwegian (Nynorsk)",
                                            "Old English",
                                            "Old French",
                                            "Old German",
                                            "Old Italian",
                                            "Old Slavonic",
                                            "Old Spanish",
                                            "Polish",
                                            "Portuguese (Brazil)",
                                            "Portuguese (Portugal)",
                                            "Romanian",
                                            "Romanian (Moldavia)",
                                            "Russian (Old Spelling)",
                                            "Russian",
                                            "Samoan",
                                            "Serbian (Cyrillic)",
                                            "Serbian (Latin)",
                                            "Slovenian",
                                            "Somali",
                                            "Spanish",
                                            "Swedish",
                                            "Thai",
                                            "Turkish",
                                            "Ukrainian",
                                            "Uzbek (Cyrillic)",
                                            "Uzbek (Latin)",
                                            "Vietnamese",
                                            "Yiddish"};
                                    ocrLanguagePickerDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    });
                                    ocrLanguagePickerDialog.setItems(types, new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            new AsyncTask_ProcessOCR((View_Snippet_Activity) getActivity()).execute(snippet.getImage_path(), "results.txt", types[which]);

                                            Helper_Methods.logEvent("OCR Scan Run", new String[]{types[which]});
                                        }

                                    });

                                    ocrLanguagePickerDialog.show();
                                } else {
                                    Crouton.makeText(getActivity(), getString(R.string.action_needs_internet), Style.ALERT).show();
                                }
                            } else {
                                Helper_Methods.logEvent("OCR Scan Viewed", null);

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                LinearLayout lay = new LinearLayout(getActivity());
                                final EditText ocrResultET = new EditText(getActivity());

                                ocrResultET.setText(snippet.getOcr_content());

                                lay.setOrientation(LinearLayout.VERTICAL);
                                lay.addView(ocrResultET);

                                builder.setTitle(R.string.ocr_scan_result);
                                builder.setPositiveButton(R.string.SAVE, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        snippet.setOcr_content(ocrResultET.getText().toString());
                                        snippetDAO.update(snippet);

                                        EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_ocr_content_changed"));
                                    }
                                });
                                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                                builder.setView(lay);

                                AlertDialog alert = builder.create();
                                alert.show();
                            }
                        }
                    });
                }

                @Override
                public void onError() {
                    imageProgressBar.setVisibility(View.INVISIBLE);

                    snippetActionsBTN.setEnabled(false);
                    snippetActionsBTN.setAlpha(0.2f);
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
                case R.id.share_snippet:
                    Intent shareSnippetIntent = new Intent(getActivity(), Share_Snippet_Activity.class);
                    shareSnippetIntent.putExtra(Constants.EXTRAS_SNIPPET, snippet);
                    shareSnippetIntent.putExtra(Constants.EXTRAS_BOOK, book);
                    startActivity(shareSnippetIntent);
                    break;
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            context = activity;

            snippet = getArguments().getParcelable(Constants.EXTRAS_SNIPPET);
            book = getArguments().getParcelable(Constants.EXTRAS_BOOK);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {

            final ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_snippets, container, false);

            ButterKnife.bind(this, rootView);

            ((View_Snippet_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ((View_Snippet_Activity) context).getSupportActionBar().show();

            snippetNameTV.setText(snippet.getName());

            if (snippet.getPage_number() == Constants.NO_SNIPPET_PAGE_NUMBER) {
                snippetPageNumberLabelTV.setVisibility(View.INVISIBLE);
                snippetPageNumberTV.setVisibility(View.INVISIBLE);
            } else {
                snippetPageNumberLabelTV.setText(getString(R.string.page));
                snippetPageNumberTV.setText(" " + String.valueOf(snippet.getPage_number()));
            }

            snippetDateAddedTV.setText(snippet.getDate_added());

            Picasso.with(context).load(new File(snippet.getImage_path())).error(getResources().getDrawable(R.drawable.snippet_not_found)).resize(1500, 1500).centerInside().into(snippetIMG, picassoCallback);

            PhotoViewAttacher mAttacher = new PhotoViewAttacher(snippetIMG);
            mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float v, float v2) {
                    if (!clutterHidden) {
                        dealWithClutter(clutterHidden, view);
                    } else {
                        dealWithClutter(clutterHidden, view);
                    }

                    clutterHidden = !clutterHidden;
                }
            });

            getActivity().invalidateOptionsMenu();

            return rootView;
        }

        @DebugLog
        public void dealWithClutter(final boolean wasHidden, final View view) {
            ArrayList<ObjectAnimator> arrayListObjectAnimators = new ArrayList<ObjectAnimator>();
            Animator[] objectAnimators;

            if (wasHidden) {
                ((View_Snippet_Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Snippet_Activity) context).getSupportActionBar().show();

                arrayListObjectAnimators.add(helperMethods.showViewElement(snippetDetailsView));
                arrayListObjectAnimators.add(helperMethods.showViewElement(snippetActionsBTN));

            } else {
                ((View_Snippet_Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((View_Snippet_Activity) context).getSupportActionBar().hide();

                arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetDetailsView));
                arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetActionsBTN));
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
            bundle.putParcelable(Constants.EXTRAS_BOOK, snippets.get(position).getBook());
            bundle.putParcelable(Constants.EXTRAS_SNIPPET, snippets.get(position));
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
