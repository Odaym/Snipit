package com.ttco.bookmarker.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.Helper_Methods;
import com.ttco.bookmarker.classes.Param;
import com.ttco.bookmarker.dragsort_listview.DragSortListView;
import com.ttco.bookmarker.showcaseview.ShowcaseView;
import com.ttco.bookmarker.showcaseview.ViewTarget;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Bookmarks_Activity extends ListActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Bookmarks_Adapter bookmarksAdapter;

    private int book_id;
    private String book_title;
    private int book_color_code;
    private ShowcaseView createBookmarkShowcase;

    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;
    private RelativeLayout emptyListLayout;
    private BroadcastReceiver bookmarkAddedBR, bookmarkDeletedBR;
    private String mCurrentPhotoPath;
    private String bookmarkAddedIntent_String = "com.ttco.bookmarker.newBookmarkAdded";
    private String bookmarkDeletedIntent_String = "com.ttco.bookmarker.bookmarkDeleted";
    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            dbHelper.deleteBookmark(dbHelper.getAllBookmarks(book_id, null).get(which).getId());
            handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, null));

            Intent bookmarkDeletedIntent = new Intent();
            bookmarkDeletedIntent.setAction(bookmarkDeletedIntent_String);
            sendBroadcast(bookmarkDeletedIntent);
        }
    };
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

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

        /**
         * If a specific sorting order exists, follow that order when getting the bookmarks
         */
        bookmarkAddedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(bookmarkAddedIntent_String)) {
                    prepareForNotifyDataChanged(book_id);
                    bookmarksAdapter.notifyDataSetChanged();
                }
            }
        };

        bookmarkDeletedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(bookmarkDeletedIntent_String)) {
                    prepareForNotifyDataChanged(book_id);
                    bookmarksAdapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter bookmarkAddedFilter = new IntentFilter();
        bookmarkAddedFilter.addAction(bookmarkAddedIntent_String);
        registerReceiver(bookmarkAddedBR, bookmarkAddedFilter);

        IntentFilter bookmarkDeletedFilter = new IntentFilter();
        bookmarkDeletedFilter.addAction(bookmarkDeletedIntent_String);
        registerReceiver(bookmarkDeletedBR, bookmarkDeletedFilter);

        registerForContextMenu(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bookmarks_sort_by_actions, menu);

        MenuItem byNumber = menu.getItem(0);
        SpannableString numberString = new SpannableString(byNumber.getTitle().toString());
        numberString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, numberString.length(), 0);
        byNumber.setTitle(numberString);

        MenuItem byName = menu.getItem(1);
        SpannableString nameString = new SpannableString(byName.getTitle().toString());
        nameString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, nameString.length(), 0);
        byName.setTitle(nameString);

        MenuItem byDate = menu.getItem(2);
        SpannableString dateString = new SpannableString(byDate.getTitle().toString());
        dateString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, dateString.length(), 0);
        byDate.setTitle(dateString);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        String sortByFormattedForSQL = "";

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
                Intent openCreateBookmark = new Intent(Bookmarks_Activity.this, Create_Bookmark_Activity.class);
                openCreateBookmark.putExtra(Constants.EXTRAS_BOOK_ID, getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID));
                openCreateBookmark.putExtra(Constants.EXTRAS_BOOKMARK_IMAGE_PATH, mCurrentPhotoPath);
                startActivity(openCreateBookmark);
                break;
        }
//        deleteFile("result.txt");
//
//        Intent results = new Intent(this, ResultsActivity.class);
//        results.putExtra("IMAGE_PATH", mCurrentPhotoPath);
//        results.putExtra("RESULT_PATH", "result.txt");
//        startActivity(results);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bookmarkAddedBR);
        unregisterReceiver(bookmarkDeletedBR);
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
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void prepareForNotifyDataChanged(int book_id) {
        String sorting_type_pref = prefs.getString(Constants.SORTING_TYPE_PREF, Constants.SORTING_TYPE_NOSORT);
        if (sorting_type_pref.equals(Constants.SORTING_TYPE_NOSORT)) {
            bookmarks = dbHelper.getAllBookmarks(book_id, null);
        } else {
            bookmarks = dbHelper.getAllBookmarks(book_id, sorting_type_pref);
        }

        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            showCreateBookShowcase();
        } else {
            emptyListLayout.setVisibility(View.GONE);
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

    private class Bookmarks_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private Context context;
        private BookmarksViewHolder holder;
        private DatabaseHelper dbHelper;

        private String bookmarkDeletedIntent_String = "com.ttco.bookmarker.bookmarkDeleted";

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
                holder.bookmarkPageNumber = (TextView) convertView.findViewById(R.id.bookmarkPageNumberTV);
                holder.bookmarkAction = (Button) convertView.findViewById(R.id.bookmarkAction);
                holder.bookmarkIMG = (ImageView) convertView.findViewById(R.id.bookmarkIMG);
                holder.bookmarkViews = (TextView) convertView.findViewById(R.id.bookmarkViewsTV);

                convertView.setTag(holder);
            } else {
                holder = (BookmarksViewHolder) convertView.getTag();
            }

            holder.bookmarkName.setText(bookmarks.get(position).getName());

            String pageNumber = bookmarks.get(position).getPage_number() + "";
            short pageNumberShort = Short.parseShort(pageNumber);
            DecimalFormat formatter = new DecimalFormat("#,###");
            holder.bookmarkPageNumber.setText(formatter.format(pageNumberShort));

            holder.bookmarkViews.setText("Views: " + bookmarks.get(position).getViews());

            switch (book_color_code) {
                case 0:
                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.pink));
                    break;
                case 1:
                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.red));
                    break;
                case 2:
                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.purple));
                    break;
                case 3:
                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.yellow));
                    break;
                case 4:
                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.blue));
                    break;
                case 5:
                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.brown));
                    break;
            }

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
                                    Intent bookmarkDeletedIntent = new Intent();
                                    bookmarkDeletedIntent.setAction(bookmarkDeletedIntent_String);
                                    context.sendBroadcast(bookmarkDeletedIntent);
                                    dbHelper.deleteBookmark(bookmarks.get(position).getId());
                                    bookmarks = dbHelper.getAllBookmarks(bookmarks.get(position).getBookId(), null);
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

        private class BookmarksViewHolder {
            TextView bookmarkName;
            TextView bookmarkPageNumber;
            ImageView bookmarkIMG;
            Button bookmarkAction;
            TextView bookmarkViews;
        }
    }
}
