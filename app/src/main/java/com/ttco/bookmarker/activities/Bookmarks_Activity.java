package com.ttco.bookmarker.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.dragsort_listview.DragSortListView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Bookmarks_Activity extends ListActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Bookmarks_Adapter bookmarksAdapter;

    private int book_id;
    private String book_title;
    private DatabaseHelper dbHelper;
    private List<Bookmark> bookmarks;
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
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    bookmarksAdapter.notifyData();
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

        dbHelper = new DatabaseHelper(this);

        book_id = getIntent().getExtras().getInt(Constants.EXTRAS_BOOK_ID);
        book_title = getIntent().getStringExtra(Constants.EXTRAS_BOOK_TITLE);

        emptyListLayout = (RelativeLayout) findViewById(R.id.emptyListLayout);

        bookmarks = dbHelper.getAllBookmarks(book_id, null);

        handleEmptyOrPopulatedScreen(bookmarks);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(book_title);

        bookmarkAddedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(bookmarkAddedIntent_String)) {
                    handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, null));
                }
            }
        };

        bookmarkDeletedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(bookmarkDeletedIntent_String)) {
                    handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, null));
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
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getListView().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
                getListView().requestDisallowInterceptTouchEvent(true);
                break;
        }
        return super.onTouchEvent(event);
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
                handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, sortByFormattedForSQL));
                break;
            case R.id.sort_by_name:
                sortByFormattedForSQL = "name";
                handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, sortByFormattedForSQL));
                break;
            case R.id.sort_by_date:
                sortByFormattedForSQL = "date_added";
                handleEmptyOrPopulatedScreen(dbHelper.getAllBookmarks(book_id, sortByFormattedForSQL));
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
        startActivity(intent);

        bookmarks.get(position).setViews(bookmarks.get(position).getViews() + 1);
        dbHelper.updateBookmark(bookmarks.get(position));
        bookmarksAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    public void handleEmptyOrPopulatedScreen(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        bookmarksAdapter = new Bookmarks_Adapter(this, bookmarks);
        DragSortListView thisDragSortListView = (DragSortListView) getListView();
        thisDragSortListView.setDropListener(onDrop);
        thisDragSortListView.setDragListener(onDrag);
        thisDragSortListView.setRemoveListener(onRemove);
        setListAdapter(bookmarksAdapter);
    }

    private class Bookmarks_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private Context context;
        private BookmarksViewHolder holder;
        private List<Bookmark> bookmarks;
        private DatabaseHelper dbHelper;

        private String bookmarkDeletedIntent_String = "com.ttco.bookmarker.bookmarkDeleted";

        public Bookmarks_Adapter(Context context, List<Bookmark> bookmarks) {
            super();
            this.context = context;
            this.bookmarks = bookmarks;

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
                holder.bookmarkViews = (TextView) convertView.findViewById(R.id.bookmarkViewsTV);

                convertView.setTag(holder);
            } else {
                holder = (BookmarksViewHolder) convertView.getTag();
            }

            holder.bookmarkName.setText(bookmarks.get(position).getName());
            holder.bookmarkPageNumber.setText("page : " + bookmarks.get(position).getPage_number());
            holder.bookmarkViews.setText("Views: " + bookmarks.get(position).getViews());

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

        public void notifyData() {
            notifyDataSetChanged();
        }

        private class BookmarksViewHolder {
            TextView bookmarkName;
            TextView bookmarkPageNumber;
            Button bookmarkAction;
            TextView bookmarkViews;
        }
    }
}
