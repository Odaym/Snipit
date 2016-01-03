package com.om.snipit.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.RoundedTransform;
import com.om.snipit.dragsort_listview.DragSortListView;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import net.frakbot.jumpingbeans.JumpingBeans;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import me.grantland.widget.AutofitTextView;

public class Snippets_Activity extends Base_Activity implements SearchView.OnQueryTextListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PICK_IMAGE_GALLERY = 2;

    @Bind(R.id.createNewSnippetBTN)
    FloatingActionsMenu createNewSnippetBTN;
    @Bind(R.id.emptyListLayout)
    RelativeLayout emptyListLayout;
    @Bind(R.id.snippetsList)
    DragSortListView listView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private DatabaseHelper databaseHelper;
    private QueryBuilder<Snippet, Integer> snippetQueryBuilder;
    private PreparedQuery<Snippet> pq;
    private RuntimeExceptionDao<Snippet, Integer> snippetDAO;

    private boolean inSearchMode = false;
    private String searchQueryForGlobalUse = Constants.EXTRAS_NO_SEARCH_TERM;

    private Snippet tempSnippet;

    private SearchView searchView;

    private Snippets_Adapter snippetsAdapter;

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    snippetsAdapter.notifyDataSetChanged();
                }
            };

    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() {
        @Override
        public void drag(int from, int to) {
            snippetsAdapter.swap(from, to);
        }
    };

    private Snackbar undeleteSnippetSB;
    private boolean itemPendingDeleteDecision = false;

    private final static int WAIT_DURATION_BFEORE_HIDE_CLUTTER_ANIMATION = 200;

    private Book book;

    private List<Snippet> snippets;
    private File photoFile;
    private Uri photoFileUri;
    private Helper_Methods helperMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippets);

        ButterKnife.bind(this);

        EventBus_Singleton.getInstance().register(this);

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            listView.setDrawSelectorOnTop(true);
            listView.setSelector(R.drawable.abc_list_selector_holo_dark);
        }

        helperMethods = new Helper_Methods(this);

        snippetDAO = getHelper().getSnippetDAO();

        snippetQueryBuilder = snippetDAO.queryBuilder();

        Helper_Methods helperMethods = new Helper_Methods(this);

        book = getIntent().getParcelableExtra(Constants.EXTRAS_BOOK);

        toolbar.setTitle(book.getTitle());
        setSupportActionBar(toolbar);
        helperMethods.setUpActionbarColors(this, book.getColorCode());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        prepareQueryBuilder(book.getId());

        snippets = snippetDAO.query(pq);

        handleEmptyOrPopulatedScreen(snippets);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Snippet snippet = ((Snippet) listView.getItemAtPosition(position));

                //Clicking on an adview when there's no Internet connection will cause this condition to be satisfied because no Book will be found at the index of that adview
                if (snippet != null) {
                    if (snippet.getIsNoteShowing() == 0) {
                        /**
                         * UPDATING SNIPPET VIEWS HERE
                         */
                        int snippetViews = snippetDAO.queryForId(snippet.getId()).getViews();
                        snippet.setViews(snippetViews + 1);
                        snippetDAO.update(snippet);

                        Intent intent = new Intent(Snippets_Activity.this, View_Snippet_Activity.class);
                        intent.putExtra(Constants.EXTRAS_BOOK, book);
                        intent.putExtra(Constants.EXTRAS_VIEWING_SNIPPETS_GALLERY, false);
                        intent.putExtra(Constants.EXTRAS_SEARCH_TERM, searchQueryForGlobalUse);
                        intent.putExtra(Constants.EXTRAS_CURRENT_SNIPPET_POSITION, position);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    public void onAddSnippetMenuBtnClicked(View view) {
        createNewSnippetBTN.collapse();

        switch (view.getId()) {
            case R.id.addSnippetCameraBTN:
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    photoFile = null;
                    try {
                        photoFile = createImageFile(constructImageFilename());
                        photoFileUri = Uri.fromFile(photoFile);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                photoFileUri);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                break;
//            case R.id.addSnippetVoiceBTN:
//
//                break;
            case R.id.addSnippetFromGalleryBTN:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_PICK_IMAGE_GALLERY);
                break;
        }
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (photoFile != null)
            outState.putString("photoFilePath", photoFile.getAbsolutePath());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        try {
            photoFile = new File(savedInstanceState.getString("photoFilePath"));
        } catch (NullPointerException e) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        if (!snippets.isEmpty()) {
            inflater.inflate(R.menu.snippets_activity, menu);

            MenuItem searchItem = menu.findItem(R.id.search);
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    inSearchMode = true;
                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    inSearchMode = false;
                    searchQueryForGlobalUse = Constants.EXTRAS_NO_SEARCH_TERM;
                    return false;
                }
            });
            searchView.setOnQueryTextListener(this);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                if (getCurrentFocus() != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                break;
            case R.id.search:
                searchView.setIconified(false);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK)
            return;

        Intent openCropSnippetIntent = new Intent(Snippets_Activity.this, Crop_Image_Activity.class);
        openCropSnippetIntent.putExtra(Constants.EXTRAS_BOOK, book);

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                openCropSnippetIntent.putExtra(Constants.EXTRAS_SNIPPET_TEMP_IMAGE_PATH, photoFile.getAbsolutePath());
                break;
            case REQUEST_PICK_IMAGE_GALLERY:
                Uri selectedImageUri = intent.getData();
                String selectedImagePath = getPhotoPathFromGallery(selectedImageUri);
                openCropSnippetIntent.putExtra(Constants.EXTRAS_SNIPPET_TEMP_IMAGE_PATH, selectedImagePath);
                break;
        }

        startActivity(openCropSnippetIntent);
    }

    public String getPhotoPathFromGallery(Uri uri) {
        if (uri == null) {
            // TODO perform some logging or show user feedback
            return null;
        }

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }

        return uri.getPath();
    }

    @Subscribe
    public void onBusEvents(EventBus_Poster ebp) {
        String ebpMessage = ebp.getMessage();

        switch (ebpMessage) {
            case "snippet_added_snippets_activity":
                prepareForNotifyDataChanged(book.getId());
                snippetsAdapter.notifyDataSetChanged();
                listView.smoothScrollToPosition(snippets.size());
//                Log.d("EVENTS", "snippet_added_snippets_activity - Snippets_Activity");
                break;
            case "snippet_deleted_snippets_activity":
                prepareForNotifyDataChanged(book.getId());
                snippetsAdapter.notifyDataSetChanged();
//                Log.d("EVENTS", "snippet_deleted_snippets_activity - Snippets_Activity");
                break;
            case "snippet_image_updated":
                Helper_Methods.delete_image_from_disk(ebp.getExtra());
                prepareForNotifyDataChanged(book.getId());
                snippetsAdapter.notifyDataSetChanged();
//                Log.d("EVENTS", "snippet_image_updated - Snippets_Activity");
                break;
            case "snippet_name_page_edited":
                prepareForNotifyDataChanged(book.getId());
                snippetsAdapter.notifyDataSetChanged();
//                Log.d("EVENTS", "snippet_name_page_edited - Snippets_Activity");
                break;
            case "snippet_note_changed":
                prepareForNotifyDataChanged(book.getId());
                snippetsAdapter.notifyDataSetChanged();
//                Log.d("EVENTS", "snippet_note_changed - Snippets_Activity");
                break;
            case "snippet_ocr_content_changed":
                prepareForNotifyDataChanged(book.getId());
                snippetsAdapter.notifyDataSetChanged();
//                Log.d("EVENTS", "snippet_note_changed - Snippets_Activity");
                break;
        }
    }

    @DebugLog
    private String constructImageFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp;

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Snipit");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(Constants.DEBUG_TAG, "failed to create directory");
                return null;
            }
        }

        return mediaStorageDir.getPath() + File.separator + imageFileName;
    }

    @DebugLog
    private File createImageFile(String imagePath) throws IOException {
        return new File(imagePath);
    }

    @DebugLog
    public void prepareForNotifyDataChanged(int book_id) {
        /**
         * If a specific sorting order exists, follow that order when getting the snippets
         */
        if (!inSearchMode) {
            prepareQueryBuilder(book_id);

            snippets = snippetDAO.query(pq);

            handleEmptyUI(snippets);
        } else {
            //Handle empty search results here, because this is the method that will be called when the snippet is ACTUALLY deleted, the other method that just fakes the disappearance is handleEmptyUI
            prepareSearchQueryBuilder(searchQueryForGlobalUse);

            snippets = snippetDAO.query(pq);

            if (snippets.isEmpty()) {
                inSearchMode = false;
                prepareForNotifyDataChanged(book_id);
            }
        }
    }

    @DebugLog
    public void handleEmptyOrPopulatedScreen(List<Snippet> snippets) {
        handleEmptyUI(snippets);

        snippetsAdapter = new Snippets_Adapter(this);

        listView.setDropListener(onDrop);
        listView.setDragListener(onDrag);

//        final View listViewHeaderAd = View.inflate(this, R.layout.adview_snippets_list_header, null);
//        AdView mAdView = (AdView) listViewHeaderAd.findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
//
//        listView.addHeaderView(listViewHeaderAd);
        listView.setAdapter(snippetsAdapter);
    }

    public void handleEmptyUI(List<Snippet> snippets) {
        if (!inSearchMode) {

            prepareQueryBuilder(book.getId());

            if (snippetDAO.query(pq).isEmpty()) {
                emptyListLayout.setVisibility(View.VISIBLE);
                JumpingBeans.with((TextView) emptyListLayout.findViewById(R.id.emptyLayoutMessageTV)).appendJumpingDots().build();
            } else if (snippetDAO.query(pq).isEmpty()) {
                emptyListLayout.setVisibility(View.GONE);
            } else {
                emptyListLayout.setVisibility(View.INVISIBLE);
            }

            invalidateOptionsMenu();
        } else {
            if (snippets.isEmpty()) {
                //Snippet was actually deleted, not just waiting for dismiss of Snackbar or leaving activity
                searchView.clearFocus();
                invalidateOptionsMenu();
            }
        }
    }

    public void showUndeleteDialog(final Snippet tempSnippetToDelete) {
        EventBus_Singleton.getInstance().post(new EventBus_Poster("snippet_deleted_books_activity"));

        tempSnippet = tempSnippetToDelete;

        itemPendingDeleteDecision = true;

        Spannable sentenceToSpan = new SpannableString(getResources().getString(R.string.delete_book_confirmation_message) + " " + tempSnippetToDelete.getName());

        sentenceToSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        snippets.remove(tempSnippetToDelete);
        handleEmptyUI(snippets);
        snippetsAdapter.notifyDataSetChanged();

        undeleteSnippetSB =
                Snackbar.with(getApplicationContext())
                        .actionLabel(R.string.undo_deletion_title)
                                //So that we differentiate between explicitly dismissing the snackbar and having it go away due to pressing UNDO
                        .dismissOnActionClicked(false)
                        .duration(8000)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .text(sentenceToSpan)
                        .eventListener(new EventListener() {
                            @Override
                            public void onShow(Snackbar snackbar) {
                            }

                            @Override
                            public void onShowByReplace(Snackbar snackbar) {
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                            }

                            @Override
                            public void onDismiss(Snackbar snackbar) {
                                if (itemPendingDeleteDecision) {
                                    finalizeSnippetDeletion(tempSnippetToDelete);
                                }
                            }

                            @Override
                            public void onDismissByReplace(Snackbar snackbar) {
                            }

                            @Override
                            public void onDismissed(Snackbar snackbar) {
                            }
                        }).actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        prepareForNotifyDataChanged(book.getId());
                        snippetsAdapter.notifyDataSetChanged();

                        itemPendingDeleteDecision = false;
                        undeleteSnippetSB.dismiss();
                    }
                });

        undeleteSnippetSB.show(Snippets_Activity.this);
    }

    public void finalizeSnippetDeletion(Snippet tempSnippet) {
        Helper_Methods.delete_image_from_disk(tempSnippet.getImage_path());
        snippetDAO.delete(tempSnippet);
        prepareForNotifyDataChanged(tempSnippet.getBook().getId());
        snippetsAdapter.notifyDataSetChanged();
        itemPendingDeleteDecision = false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String searchQuery) {
        if (inSearchMode) {
            if (!searchQuery.isEmpty()) {
                searchQueryForGlobalUse = searchQuery;
                prepareSearchQueryBuilder(searchQuery);
                snippets = snippetDAO.query(pq);
            } else {
                prepareQueryBuilder(book.getId());
                snippets = snippetDAO.query(pq);
            }

            snippetsAdapter.notifyDataSetChanged();
        }

        return true;
    }

    public void prepareQueryBuilder(int book_id) {
        try {
            snippetQueryBuilder.where().eq("book_id", book_id);
            snippetQueryBuilder.orderBy("order", true);
            pq = snippetQueryBuilder.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void prepareSearchQueryBuilder(String searchQuery) {
        try {
            SelectArg nameSelectArg = new SelectArg("%" + searchQuery + "%");
            SelectArg noteSelectArg = new SelectArg("%" + searchQuery + "%");
            SelectArg ocrSelectArg = new SelectArg("%" + searchQuery + "%");

            snippetQueryBuilder.where().eq("book_id", book.getId()).and().like("name", nameSelectArg).or().like("note", noteSelectArg).or().like("ocr_content", ocrSelectArg);
            pq = snippetQueryBuilder.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (itemPendingDeleteDecision) {
            finalizeSnippetDeletion(tempSnippet);

            if (undeleteSnippetSB.isShowing()) {
                undeleteSnippetSB.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus_Singleton.getInstance().unregister(this);
    }

    public class Snippets_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private Context context;
        private SnippetsViewHolder holder;
        private PicassoImageLoadedCallback picassoImageLoadedCallback;
        private RoundedTransform roundedTransform;

        public Snippets_Adapter(Context context) {
            super();
            this.context = context;
            this.inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            this.roundedTransform = new RoundedTransform(context.getResources().getDimensionPixelSize(R.dimen.snippet_image_shape_corners_radius), context.getResources().getDimensionPixelSize(R.dimen.snippet_image_shape_corners_padding_bottom));
        }

        @Override
        public int getCount() {
            return snippets.size();
        }

        @Override
        public Object getItem(int i) {
            return snippets.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            final View parentView;

            if (convertView == null || ((SnippetsViewHolder) convertView.getTag()).needInflate) {
                parentView = inflater.inflate(R.layout.list_item_snippet, parent, false);

                holder = new SnippetsViewHolder();

                holder.snippetName = (AutofitTextView) parentView.findViewById(R.id.snippetNameTV);
                holder.snippetActionLayout = (RelativeLayout) parentView.findViewById(R.id.snippetActionLayout);
                holder.snippetIMG = (ImageView) parentView.findViewById(R.id.snippetIMG);
                holder.snippetPageNumber = (TextView) parentView.findViewById(R.id.snippetPageNumberTV);
                holder.snippetNoteBTN = (Button) parentView.findViewById(R.id.snippetNoteBTN);
                holder.snippetNoteTV = (AutofitTextView) parentView.findViewById(R.id.snippetNoteTV);
                holder.motherView = (RelativeLayout) parentView.findViewById(R.id.list_item_snippet);
                holder.imageProgressLoader = (ProgressBar) parentView.findViewById(R.id.imageProgressLoader);
                holder.needInflate = false;

                this.picassoImageLoadedCallback = new PicassoImageLoadedCallback(holder.imageProgressLoader) {
                    @Override
                    public void onSuccess() {
                        if (holder.imageProgressLoader != null) {
                            holder.imageProgressLoader.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError() {
                        if (holder.imageProgressLoader != null) {
                            holder.imageProgressLoader.setVisibility(View.GONE);
                        }
                    }
                };

                parentView.setTag(holder);
            } else {
                parentView = convertView;
            }

            holder = (SnippetsViewHolder) parentView.getTag();

            //If the snippet doesn't have a note
            if (TextUtils.isEmpty(snippets.get(position).getNote())) {
                holder.snippetNoteBTN.setVisibility(View.INVISIBLE);
            } else {
                holder.snippetNoteBTN.setVisibility(View.VISIBLE);
            }

            GradientDrawable gradient;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LayerDrawable drawable = (LayerDrawable) ((LayerDrawable) holder.motherView
                        .getBackground()).findDrawableByLayerId(R.id.content);
                gradient = (GradientDrawable) drawable.findDrawableByLayerId(R.id.innerView);
            } else {
                gradient = ((GradientDrawable) ((LayerDrawable) holder.motherView.getBackground()).findDrawableByLayerId(R.id.innerView));
            }

            if (snippets.get(position).getIsNoteShowing() == 0) {
                gradient.setColor(context.getResources().getColor(R.color.white));

                holder.snippetActionLayout.setAlpha(1f);
                holder.snippetActionLayout.setVisibility(View.VISIBLE);
                holder.snippetIMG.setAlpha(1f);
                holder.snippetIMG.setVisibility(View.VISIBLE);
                holder.snippetPageNumber.setAlpha(1f);
                holder.snippetPageNumber.setVisibility(View.VISIBLE);
                holder.snippetName.setVisibility(View.VISIBLE);
                holder.snippetName.setAlpha(1f);
                holder.imageProgressLoader.setAlpha(1f);
                holder.snippetNoteBTN.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.gray_bookmark));
            } else {
                gradient.setColor(context.getResources().getColor(helperMethods.determineNoteViewBackground(book.getColorCode())));

                holder.snippetNoteTV.setText(snippets.get(position).getNote());
                holder.snippetActionLayout.setVisibility(View.INVISIBLE);
                holder.snippetIMG.setVisibility(View.INVISIBLE);
                holder.snippetPageNumber.setVisibility(View.INVISIBLE);
                holder.snippetName.setVisibility(View.INVISIBLE);
                holder.snippetNoteTV.setVisibility(View.VISIBLE);
                holder.imageProgressLoader.setVisibility(View.VISIBLE);
                holder.snippetNoteBTN.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.white_bookmark));
            }

            holder.snippetName.setText(snippets.get(position).getName());

            if (snippets.get(position).getPage_number() == Constants.NO_SNIPPET_PAGE_NUMBER)
                holder.snippetPageNumber.setVisibility(View.INVISIBLE);
            else
                holder.snippetPageNumber.setText(context.getResources().getText(R.string.snippet_page_number_label) + " " + snippets.get(position).getPage_number());

            Picasso.with(Snippets_Activity.this).load(new File(snippets.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.snippet_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.snippet_thumb_height)).centerCrop().transform(roundedTransform).error(context.getResources().getDrawable(R.drawable.snippet_not_found)).into(holder.snippetIMG, picassoImageLoadedCallback);

            holder.snippetActionLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View overflowButton = view.findViewById(R.id.snippetAction);
                    overflowButton.findViewById(R.id.snippetAction).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.menu_overflow_focus));

                    PopupMenu popup = new PopupMenu(context, view);
                    popup.getMenuInflater().inflate(R.menu.snippet_list_item,
                            popup.getMenu());
                    for (int i = 0; i < popup.getMenu().size(); i++) {
                        MenuItem item = popup.getMenu().getItem(i);
                        SpannableString spanString = new SpannableString(item.getTitle().toString());
                        spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
                        item.setTitle(spanString);
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.edit:
                                    Intent editSnippetIntent = new Intent(Snippets_Activity.this, Create_Snippet_Activity.class);
                                    editSnippetIntent.putExtra(Constants.EDIT_SNIPPET_PURPOSE_STRING, Constants.EDIT_SNIPPET_PURPOSE_VALUE);
                                    editSnippetIntent.putExtra(Constants.EXTRAS_BOOK, book);
                                    editSnippetIntent.putExtra(Constants.EXTRAS_SNIPPET, snippets.get(position));
                                    startActivity(editSnippetIntent);
                                    break;
                                case R.id.delete:
                                    //Dissmiss the UNDO Snackbar and handle the deletion of the previously awaiting item yourself
                                    if (undeleteSnippetSB != null && undeleteSnippetSB.isShowing()) {
                                        //Careful about position that is passed from the adapter! This has to be accounted for again by using getItemAtPosition because there's an adview among the views
                                        snippetDAO.delete(tempSnippet);
                                        itemPendingDeleteDecision = false;
                                        undeleteSnippetSB.dismiss();
                                    }

                                    showUndeleteDialog(snippets.get(position));

                                    break;
                            }

                            return true;
                        }
                    });
                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu popupMenu) {
                            overflowButton.findViewById(R.id.snippetAction).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.menu_overflow_fade));
                        }
                    });
                }
            });

            holder.snippetNoteBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    ArrayList<ObjectAnimator> arrayListObjectAnimators = new ArrayList<ObjectAnimator>();
                    Animator[] objectAnimators;

                    RelativeLayout motherView = (RelativeLayout) view.getParent();
                    TextView snippetNoteTV = (TextView) motherView.getChildAt(0);
                    ProgressBar imageProgressLoader = (ProgressBar) ((RelativeLayout) motherView.getChildAt(1)).getChildAt(0);
                    ImageView snippetIMG = (ImageView) ((RelativeLayout) motherView.getChildAt(1)).getChildAt(1);
                    TextView snippetName = (TextView) motherView.getChildAt(2);
                    RelativeLayout snippetActionLayout = (RelativeLayout) motherView.getChildAt(3);
                    TextView snippetPageNumber = (TextView) motherView.getChildAt(6);

                    int isNoteShowing = snippets.get(position).getIsNoteShowing();

                    GradientDrawable gradient;

                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        LayerDrawable drawable = (LayerDrawable) ((LayerDrawable) motherView
                                .getBackground()).findDrawableByLayerId(R.id.content);
                        gradient = (GradientDrawable) drawable.findDrawableByLayerId(R.id.innerView);
                    } else {
                        gradient = ((GradientDrawable) ((LayerDrawable) motherView.getBackground()).findDrawableByLayerId(R.id.innerView));
                    }

                    //Note was showing, hide
                    if (isNoteShowing == 1) {
                        view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.gray_bookmark));

                        gradient.setColor(context.getResources().getColor(R.color.white));

                        arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetNoteTV));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(snippetActionLayout));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(snippetIMG));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(snippetPageNumber));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(snippetName));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(imageProgressLoader));

                        snippets.get(position).setIsNoteShowing(0);
                    } else {
                        view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.white_bookmark));

                        gradient.setColor(context.getResources().getColor(helperMethods.determineNoteViewBackground(book.getColorCode())));

                        snippetNoteTV.setText(snippets.get(position).getNote());

                        arrayListObjectAnimators.add(helperMethods.showViewElement(snippetNoteTV));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetActionLayout));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetIMG));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetPageNumber));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(snippetName));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(imageProgressLoader));

                        snippets.get(position).setIsNoteShowing(1);
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
                    hideClutterSet.setDuration(WAIT_DURATION_BFEORE_HIDE_CLUTTER_ANIMATION);
                    hideClutterSet.start();
                }
            });

            return parentView;
        }

        @DebugLog
        public void swap(int from, int to) {
            if (to < snippets.size() && from < snippets.size()) {
                Collections.swap(snippets, from, to);
                int tempNumber = snippets.get(from).getOrder();
                snippets.get(from).setOrder(snippets.get(to).getOrder());
                snippets.get(to).setOrder(tempNumber);
                snippetDAO.update(snippets.get(from));
                snippetDAO.update(snippets.get(to));
            }
        }
    }

    public static class SnippetsViewHolder {
        RelativeLayout motherView;
        AutofitTextView snippetName;
        ImageView snippetIMG;
        RelativeLayout snippetActionLayout;
        TextView snippetPageNumber;
        Button snippetNoteBTN;
        AutofitTextView snippetNoteTV;
        ProgressBar imageProgressLoader;
        boolean needInflate;
    }

    private class PicassoImageLoadedCallback implements Callback {
        ProgressBar progressBar;

        public PicassoImageLoadedCallback(ProgressBar progBar) {
            progressBar = progBar;
        }

        @Override
        public void onSuccess() {

        }

        @Override
        public void onError() {

        }
    }
}
