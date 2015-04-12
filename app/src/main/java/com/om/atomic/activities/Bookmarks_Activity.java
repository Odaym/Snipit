package com.om.atomic.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.melnykov.fab.FloatingActionButton;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.om.atomic.R;
import com.om.atomic.classes.Bookmark;
import com.om.atomic.classes.Constants;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.om.atomic.classes.Helper_Methods;
import com.om.atomic.classes.Param;
import com.om.atomic.dragsort_listview.DragSortListView;
import com.om.atomic.showcaseview.ShowcaseView;
import com.om.atomic.showcaseview.ViewTarget;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;
import me.grantland.widget.AutofitTextView;

public class Bookmarks_Activity extends BaseActivity implements SearchView.OnQueryTextListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @InjectView(R.id.createNewBookmarkBTN)
    FloatingActionButton createNewBookmarkBTN;
    @InjectView(R.id.emptyListLayout)
    RelativeLayout emptyListLayout;
    @InjectView(R.id.bookmarksList)
    DragSortListView listView;

    private SearchView searchView;

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    bookmarksAdapter.notifyDataSetChanged();
                }
            };
    private Bookmarks_Adapter bookmarksAdapter;
    private int book_id;
    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            ArrayList<Bookmark> tempBookmarks = dbHelper.getAllBookmarks(book_id, null);
            dbHelper.deleteBookmark(tempBookmarks.get(which).getId());
            handleEmptyOrPopulatedScreen(tempBookmarks);

            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_changed"));
        }
    };
    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() {
        @Override
        public void drag(int from, int to) {
            bookmarksAdapter.swap(from, to);
            String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);

            if (!sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
                prefsEditor.putString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);
                prefsEditor.commit();
                prepareForNotifyDataChanged(book_id);

                Crouton.makeText(Bookmarks_Activity.this, R.string.sort_order_override, Style.ALERT).show();
            }
        }
    };

    private final static int SHOW_CREATE_BOOKMARK_SHOWCASE = 1;
    private String book_title;
    private int book_color_code;
    private ShowcaseView createBookmarkShowcase;
    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;
    private String mCurrentPhotoPath;
    private Uri photoFileUri;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    private Helper_Methods helperMethods;
    private boolean activityVisible = true;
    private Handler UIHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        ButterKnife.inject(this);

        UIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SHOW_CREATE_BOOKMARK_SHOWCASE:
                        showCreateBookmarkShowcase();
                        break;
                }
                super.handleMessage(msg);
            }
        };

        EventBus_Singleton.getInstance().register(this);

        helperMethods = new Helper_Methods(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();
        prefsEditor.apply();

        dbHelper = new DatabaseHelper(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        mCurrentPhotoPath = constructImageFilename();

        book_id = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID);
        book_title = getIntent().getStringExtra(Constants.EXTRAS_BOOK_TITLE);
        book_color_code = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR);

        String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);
        if (sorting_type_pref != null) {
            if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
                bookmarks = dbHelper.getAllBookmarks(book_id, null);
            } else {
                bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
            }
        }

        handleEmptyOrPopulatedScreen(bookmarks);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(book_title);
        helperMethods.setUpActionbarColors(this, book_color_code);

        createNewBookmarkBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (createBookmarkShowcase != null)
                    createBookmarkShowcase.hide();

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile(mCurrentPhotoPath);
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
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (bookmarks.get(position).getIsNoteShowing() == 0) {
                    int bookmarkViews = dbHelper.getBookmarkViews(bookmarks.get(position).getId());
                    bookmarks.get(position).setViews(bookmarkViews + 1);
                    dbHelper.updateBookmark(bookmarks.get(position));
                    bookmarksAdapter.notifyDataSetChanged();

                    Intent intent = new Intent(Bookmarks_Activity.this, View_Bookmark_Activity.class);
                    intent.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
                    intent.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
                    intent.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, position);
                    intent.putParcelableArrayListExtra("bookmarks", bookmarks);
                    startActivity(intent);
                }
            }
        });

        createNewBookmarkBTN.attachToListView(listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (!bookmarks.isEmpty()) {
            inflater.inflate(R.menu.bookmarks_sort_by_actions, menu);

            MenuItem byNumber = menu.getItem(1);
            SpannableString numberString = new SpannableString(byNumber.getTitle().toString());
            numberString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, numberString.length(), 0);
            byNumber.setTitle(numberString);

            MenuItem byName = menu.getItem(2);
            SpannableString nameString = new SpannableString(byName.getTitle().toString());
            nameString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, nameString.length(), 0);
            byName.setTitle(nameString);

            MenuItem byViews = menu.getItem(3);
            SpannableString viewsString = new SpannableString(byViews.getTitle().toString());
            viewsString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, viewsString.length(), 0);
            byViews.setTitle(viewsString);

            MenuItem searchItem = menu.findItem(R.id.search);
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnQueryTextListener(this);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String sortByFormattedForSQL;

        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
                break;
            case R.id.search:
                searchView.setIconified(false);
                return true;
            case R.id.sort_page_number:
                sortByFormattedForSQL = "page_number";
                prefsEditor.putString(Constants.SORTING_TYPE_PREF, sortByFormattedForSQL);
                prefsEditor.commit();
                bookmarks = dbHelper.getAllBookmarks(book_id, sortByFormattedForSQL);
                bookmarksAdapter.notifyDataSetChanged();
                break;
            case R.id.sort_by_name:
                sortByFormattedForSQL = "name";
                prefsEditor.putString(Constants.SORTING_TYPE_PREF, sortByFormattedForSQL);
                prefsEditor.commit();
                bookmarks = dbHelper.getAllBookmarks(book_id, sortByFormattedForSQL);
                bookmarksAdapter.notifyDataSetChanged();
                break;
            case R.id.sort_by_views:
                sortByFormattedForSQL = "views";
                prefsEditor.putString(Constants.SORTING_TYPE_PREF, sortByFormattedForSQL);
                prefsEditor.commit();
                bookmarks = dbHelper.getAllBookmarks(book_id, sortByFormattedForSQL);
                bookmarksAdapter.notifyDataSetChanged();
                break;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                Intent openCreateBookmark = new Intent(Bookmarks_Activity.this, Crop_Image_Activity.class);
                openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
                openCreateBookmark.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, mCurrentPhotoPath);
                openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_COLOR, book_color_code);
                startActivity(openCreateBookmark);
                break;
        }
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        switch (ebp.getMessage()) {
            case "bookmark_viewed":
                String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);
                if (sorting_type_pref != null) {
                    if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
                        bookmarks = dbHelper.getAllBookmarks(book_id, null);
                    } else {
                        bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
                    }
                }
                bookmarksAdapter.notifyDataSetChanged();
                break;
            case "bookmark_changed":
            case "bookmark_note_changed":
                prepareForNotifyDataChanged(book_id);
                bookmarksAdapter.notifyDataSetChanged();
//            if (ebp.getExtra() != null) {
//                if (ebp.getExtra().equals("new_bookmark")) {
//                    listView.smoothScrollToPosition(bookmarksAdapter.getCount() + 1, 0, 500);
//                    YoYo.with(Techniques.Tada)
//                            .duration(1500)
//                            .playOn(bookmarksAdapter
//                                    .getView(bookmarksAdapter.getCount() - 1, null, null));
//                }
//            }
                break;
            case "reset_bookmark_notes_showing":
//                for (Bookmark bookmark : bookmarks) {
//                    bookmark.setIsNoteShowing(0);
//                }
//                bookmarksAdapter.notifyDataSetChanged();
                break;
        }
    }

    @DebugLog
    private String constructImageFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp;

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Atomic");

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
         * If a specific sorting order exists, follow that order when getting the bookmarks
         */
        String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);

        if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
            bookmarks = dbHelper.getAllBookmarks(book_id, null);
        } else {
            bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
        }

        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            showCreateBookmarkShowcase();
            invalidateOptionsMenu();
        } else {
            emptyListLayout.setVisibility(View.GONE);
            invalidateOptionsMenu();
        }
    }

    @DebugLog
    public void handleEmptyOrPopulatedScreen(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            UIHandler.sendEmptyMessageDelayed(SHOW_CREATE_BOOKMARK_SHOWCASE, 500);
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        bookmarksAdapter = new Bookmarks_Adapter(this);
        DragSortListView thisDragSortListView = listView;
        thisDragSortListView.setDropListener(onDrop);
        thisDragSortListView.setDragListener(onDrag);
        thisDragSortListView.setRemoveListener(onRemove);
        listView.setAdapter(bookmarksAdapter);
    }

    @DebugLog
    public void showCreateBookmarkShowcase() {
        //When a bookmark is deleted from inside Search Results Activity, leading up to this Activity having zero bookmarks and causing the coachmark to appear when the activity is not in focus. So make sure it is in focus first
        if (!dbHelper.getSeensParam(null, 1)) {

            RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lps.setMargins(getResources().getDimensionPixelOffset(R.dimen.button_margin_left), 0, 0, getResources().getDimensionPixelOffset(R.dimen.button_margin_bottom));

            ViewTarget target = new ViewTarget(R.id.createNewBookmarkBTN, Bookmarks_Activity.this);

            String showcaseTitle = getString(R.string.create_bookmark_showcase_title);
            String showcaseDescription = getString(R.string.create_bookmark_showcase_description);

            createBookmarkShowcase = new ShowcaseView.Builder(Bookmarks_Activity.this, getResources().getDimensionPixelSize(R.dimen.create_bookmark_showcase_inner_rad), getResources().getDimensionPixelSize(R.dimen.create_bookmark_showcase_outer_rad))
                    .setTarget(target)
                    .setContentTitle(Helper_Methods.fontifyString(showcaseTitle))
                    .setContentText(Helper_Methods.fontifyString(showcaseDescription))
                    .setStyle(R.style.CustomShowcaseTheme)
                    .hasManualPosition(true)
                    .xPostion(getResources().getDimensionPixelSize(R.dimen.create_bookmark_text_x))
                    .yPostion(getResources().getDimensionPixelSize(R.dimen.create_bookmark_text_y))
                    .build();
            createBookmarkShowcase.setButtonPosition(lps);
            createBookmarkShowcase.findViewById(R.id.showcase_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createBookmarkShowcase.hide();

                    Param param = new Param();
                    param.setNumber(1);
                    param.setValue("True");
                    dbHelper.updateParam(param);
                }
            });
            createBookmarkShowcase.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus_Singleton.getInstance().unregister(this);
    }

    @Override
    public boolean onQueryTextSubmit(String searchTerm) {
        activityVisible = false;
        Intent openSearchActivity = new Intent(Bookmarks_Activity.this, SearchResults_Activity.class);
        openSearchActivity.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
        openSearchActivity.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
        openSearchActivity.putExtra(Constants.EXTRAS_BOOK_COLOR, book_color_code);
        openSearchActivity.putExtra(Constants.EXTRAS_SEARCH_TERM, searchTerm);
        startActivity(openSearchActivity);

//        EventBus_Singleton.getInstance().post(new EventBus_Poster("reset_bookmark_notes_showing"));

        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    private class Bookmarks_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private Context context;
        private BookmarksViewHolder holder;
        private DatabaseHelper dbHelper;

        public Bookmarks_Adapter(Context context) {
            super();
            this.context = context;

            dbHelper = new DatabaseHelper(context);
        }

        @Override
        public int getCount() {
            return bookmarks.size();
        }

        @Override
        public Object getItem(int i) {
            return bookmarks.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (convertView == null) {
                inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item_bookmark, parent, false);

                holder = new BookmarksViewHolder();

                holder.bookmarkName = (AutofitTextView) convertView.findViewById(R.id.bookmarkNameTV);
                holder.bookmarkAction = (Button) convertView.findViewById(R.id.bookmarkAction);
                holder.bookmarkIMG = (ImageView) convertView.findViewById(R.id.bookmarkIMG);
                holder.bookmarkViews = (TextView) convertView.findViewById(R.id.bookmarkViewsTV);
                holder.bookmarkNoteBTN = (Button) convertView.findViewById(R.id.bookmarkNoteBTN);
                holder.bookmarkNoteTV = (AutofitTextView) convertView.findViewById(R.id.bookmarkNoteTV);
                holder.motherView = (RelativeLayout) convertView.findViewById(R.id.list_item_bookmark);

                convertView.setTag(holder);
            } else {
                holder = (BookmarksViewHolder) convertView.getTag();
            }

            //If the bookmark doesn't have a note
            if (TextUtils.isEmpty(bookmarks.get(position).getNote())) {
                holder.bookmarkNoteBTN.setVisibility(View.INVISIBLE);
            } else {
                holder.bookmarkNoteBTN.setVisibility(View.VISIBLE);
            }

//            if (holder.bookmarkIMG.getAlpha() == 0) {
            if (bookmarks.get(position).getIsNoteShowing() == 0) {
                holder.motherView.setBackground(context.getResources().getDrawable(R.drawable.listview_items_shape));
                holder.bookmarkAction.setAlpha(1f);
                holder.bookmarkAction.setVisibility(View.VISIBLE);
                holder.bookmarkIMG.setAlpha(1f);
                holder.bookmarkIMG.setVisibility(View.VISIBLE);
                holder.bookmarkViews.setAlpha(1f);
                holder.bookmarkViews.setVisibility(View.VISIBLE);
                holder.bookmarkName.setVisibility(View.VISIBLE);
                holder.bookmarkName.setAlpha(1f);
                holder.bookmarkNoteBTN.setBackground(context.getResources().getDrawable(R.drawable.gray_bookmark));
            } else {
                holder.motherView.setBackgroundColor(context.getResources().getColor(helperMethods.determineNoteViewBackground(book_color_code)));
                holder.bookmarkNoteTV.setText(bookmarks.get(position).getNote());

//                holder.bookmarkAction.setAlpha(1f);
                holder.bookmarkAction.setVisibility(View.INVISIBLE);
//                holder.bookmarkIMG.setAlpha(1f);
                holder.bookmarkIMG.setVisibility(View.INVISIBLE);
//                holder.bookmarkViews.setAlpha(1f);
                holder.bookmarkViews.setVisibility(View.INVISIBLE);
                holder.bookmarkName.setVisibility(View.INVISIBLE);
                holder.bookmarkNoteTV.setVisibility(View.VISIBLE);
//                holder.bookmarkName.setAlpha(1f);
                holder.bookmarkNoteBTN.setBackground(context.getResources().getDrawable(R.drawable.white_bookmark));
            }

            holder.bookmarkName.setText(bookmarks.get(position).getName());
            holder.bookmarkViews.setText("Views: " + bookmarks.get(position).getViews());

            Glide.with(Bookmarks_Activity.this).load(new File(bookmarks.get(position).getImage_path())).centerCrop().error(getDrawable(R.drawable.notfound_1)).into(holder.bookmarkIMG);

//            Picasso.with(Bookmarks_Activity.this).load(new File(bookmarks.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().error(helperMethods.getNotFoundImage(context)).into(holder.bookmarkIMG);

            holder.bookmarkAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View overflowButton = view;
                    overflowButton.setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_focus));

                    PopupMenu popup = new PopupMenu(context, view);
                    popup.getMenuInflater().inflate(R.menu.bookmark_edit_delete,
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
                                    Intent editBookmarkIntent = new Intent(Bookmarks_Activity.this, Create_Bookmark_Activity.class);
                                    editBookmarkIntent.putExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, Constants.EDIT_BOOKMARK_PURPOSE_VALUE);
                                    editBookmarkIntent.putExtra(Constants.EXTRAS_BOOK_COLOR, book_color_code);
                                    editBookmarkIntent.putExtra("bookmark", bookmarks.get(position));
                                    startActivity(editBookmarkIntent);
                                    break;
                                case R.id.delete:
                                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_changed"));
                                    dbHelper.deleteBookmark(bookmarks.get(position).getId());
                                    prepareForNotifyDataChanged(book_id);
                                    notifyDataSetChanged();
                                    break;
                            }

                            return true;
                        }
                    });
                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu popupMenu) {
                            overflowButton.setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_fade));
                        }
                    });
                }
            });

            holder.bookmarkNoteBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    ArrayList<ObjectAnimator> arrayListObjectAnimators = new ArrayList<ObjectAnimator>();
                    Animator[] objectAnimators;

                    RelativeLayout motherView = (RelativeLayout) view.getParent();
                    TextView bookmarkNoteTV = (TextView) motherView.getChildAt(0);
                    ImageView bookmarkIMG = (ImageView) motherView.getChildAt(1);
                    TextView bookmarkName = (TextView) motherView.getChildAt(2);
                    Button bookmarkAction = (Button) motherView.getChildAt(3);
                    TextView bookmarkViews = (TextView) motherView.getChildAt(5);

                    int isNoteShowing = bookmarks.get(position).getIsNoteShowing();

                    //Note was showing, hide
                    if (isNoteShowing == 1) {
                        view.setBackground(context.getResources().getDrawable(R.drawable.gray_bookmark));

                        motherView.setBackground(context.getResources().getDrawable(R.drawable.listview_items_shape));

                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkNoteTV));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkAction));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkIMG));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkViews));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkName));

                        bookmarks.get(position).setIsNoteShowing(0);
                    } else {
                        view.setBackground(context.getResources().getDrawable(R.drawable.white_bookmark));

                        motherView.setBackgroundColor(context.getResources().getColor(helperMethods.determineNoteViewBackground(book_color_code)));
                        bookmarkNoteTV.setText(bookmarks.get(position).getNote());

                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkNoteTV));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkAction));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkIMG));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkViews));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkName));

                        bookmarks.get(position).setIsNoteShowing(1);
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
                    hideClutterSet.setDuration(200);
                    hideClutterSet.start();
                }
            });

            return convertView;
        }

        @DebugLog
        public void swap(int from, int to) {
            if (to < bookmarks.size() && from < bookmarks.size()) {
                Collections.swap(bookmarks, from, to);
                int tempNumber = bookmarks.get(from).getOrder();
                bookmarks.get(from).setOrder(bookmarks.get(to).getOrder());
                bookmarks.get(to).setOrder(tempNumber);
                dbHelper.updateBookmark(bookmarks.get(from));
                dbHelper.updateBookmark(bookmarks.get(to));
            }
        }
    }

    public static class BookmarksViewHolder {
        RelativeLayout motherView;
        AutofitTextView bookmarkName;
        ImageView bookmarkIMG;
        Button bookmarkAction;
        TextView bookmarkViews;
        Button bookmarkNoteBTN;
        AutofitTextView bookmarkNoteTV;
    }
}
