package com.ttco.bookmarker.activities;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.EventBus_Poster;
import com.ttco.bookmarker.classes.EventBus_Singleton;
import com.ttco.bookmarker.classes.Helper_Methods;
import com.ttco.bookmarker.classes.Param;
import com.ttco.bookmarker.dragsort_listview.DragSortListView;
import com.ttco.bookmarker.showcaseview.ShowcaseView;
import com.ttco.bookmarker.showcaseview.ViewTarget;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Bookmarks_Activity extends ListActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    bookmarksAdapter.notifyDataSetChanged();
                }
            };
    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() {
        @Override
        public void drag(int from, int to) {
            bookmarksAdapter.swap(from, to);
        }
    };
    private Bookmarks_Adapter bookmarksAdapter;
    private int book_id;
    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            dbHelper.deleteBookmark(dbHelper.getAllBookmarks(book_id, null).get(which).getId());
            handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, null));

            EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_deleted"));
        }
    };
    private String book_title;
    private int book_color_code;
    private ShowcaseView createBookmarkShowcase;
    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;
    private RelativeLayout emptyListLayout;
    private String mCurrentPhotoPath;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        EventBus_Singleton.getInstance().register(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();
        prefsEditor.apply();

        dbHelper = new DatabaseHelper(this);

        book_id = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID);
        book_title = getIntent().getStringExtra(Constants.EXTRAS_BOOK_TITLE);
        book_color_code = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_COLOR);

        emptyListLayout = (RelativeLayout) findViewById(R.id.emptyListLayout);

        String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);
        if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
            bookmarks = dbHelper.getAllBookmarks(book_id, null);
        } else {
            bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
        }

        handleEmptyOrPopulatedScreen(bookmarks);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(book_title);

        registerForContextMenu(getListView());
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

            SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView =
                    (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        String sortByFormattedForSQL;

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
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

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void startActivity(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
            intent.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
            intent.putExtra(Constants.EXTRAS_BOOK_COLOR, book_color_code);
        }

        super.startActivity(intent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent intent = new Intent(Bookmarks_Activity.this, View_Bookmark_Activity.class);
        intent.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
        intent.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
        intent.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, position);
        intent.putParcelableArrayListExtra("bookmarks", bookmarks);
        startActivity(intent);

        int bookmarkViews = dbHelper.getBookmarkViews(bookmarks.get(position));
        bookmarks.get(position).setViews(bookmarkViews + 1);
        dbHelper.updateBookmark(bookmarks.get(position));
        bookmarksAdapter.notifyDataSetChanged();
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
                startActivity(openCreateBookmark);
                break;
        }
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

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        if (ebp.getMessage().equals("bookmark_viewed")) {
            String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);
            if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
                bookmarks = dbHelper.getAllBookmarks(book_id, null);
            } else {
                bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
            }
            bookmarksAdapter.notifyDataSetChanged();
        } else if (ebp.getMessage().equals("bookmark_added")) {
            prepareForNotifyDataChanged(book_id);
            bookmarksAdapter.notifyDataSetChanged();
        }
    }

    public void handleAddBookmark_Pressed(View view) {
        if (createBookmarkShowcase != null)
            createBookmarkShowcase.hide();

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Atomic");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(Constants.DEBUG_TAG, "failed to create directory");
                return null;
            }
        }

        File image = new File(mediaStorageDir.getPath() + File.separator + imageFileName);

        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

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
            showCreateBookShowcase();
            invalidateOptionsMenu();
        } else {
            emptyListLayout.setVisibility(View.GONE);
            invalidateOptionsMenu();
        }
    }

    public void handleEmptyOrPopulatedScreen(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            showCreateBookShowcase();
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        bookmarksAdapter = new Bookmarks_Adapter(this);
        DragSortListView thisDragSortListView = (DragSortListView) getListView();
        thisDragSortListView.setDropListener(onDrop);
        thisDragSortListView.setDragListener(onDrag);
        thisDragSortListView.setRemoveListener(onRemove);
        setListAdapter(bookmarksAdapter);
    }

    public void showCreateBookShowcase() {
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

                holder.bookmarkName = (TextView) convertView.findViewById(R.id.bookmarkNameTV);
                holder.bookmarkAction = (Button) convertView.findViewById(R.id.bookmarkAction);
                holder.bookmarkIMG = (ImageView) convertView.findViewById(R.id.bookmarkIMG);
                holder.bookmarkViews = (TextView) convertView.findViewById(R.id.bookmarkViewsTV);

                convertView.setTag(holder);
            } else {
                holder = (BookmarksViewHolder) convertView.getTag();
            }

            holder.bookmarkName.setText(bookmarks.get(position).getName());

            holder.bookmarkViews.setText("Views: " + bookmarks.get(position).getViews());

            Picasso.with(Bookmarks_Activity.this).load(new File(bookmarks.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(holder.bookmarkIMG);

            holder.bookmarkAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View overflowButton = view;
                    overflowButton.setBackground(getDrawable(R.drawable.menu_overflow_focus));

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
                                    editBookmarkIntent.putExtra("bookmark", bookmarks.get(position));
                                    startActivity(editBookmarkIntent);
                                    break;
                                case R.id.delete:
                                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_deleted"));
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
                            overflowButton.setBackground(getDrawable(R.drawable.menu_overflow_fade));
                        }
                    });
                }
            });
            return convertView;
        }

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

        public class BookmarksViewHolder {
            TextView bookmarkName;
            ImageView bookmarkIMG;
            Button bookmarkAction;
            TextView bookmarkViews;
        }
    }
}
