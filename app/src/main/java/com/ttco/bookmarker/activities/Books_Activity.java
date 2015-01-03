package com.ttco.bookmarker.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
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

import com.crashlytics.android.Crashlytics;
import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Book;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.Helper_Methods;
import com.ttco.bookmarker.dragsort_listview.DragSortListView;
import com.ttco.bookmarker.showcaseview.ShowcaseView;
import com.ttco.bookmarker.showcaseview.ViewTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class Books_Activity extends ListActivity {

    private Books_Adapter booksAdapter;

    private DatabaseHelper dbHelper;
    private BroadcastReceiver bookAddedBR, bookmarkAddedBR, bookmarkDeletedBR;
    private RelativeLayout emptyListLayout;
    private ArrayList<Book> books;
    private ShowcaseView createBookShowcase;

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    private String bookAddedIntent_String = "com.ttco.bookmarker.newBookAdded";
    private String bookmarkAddedIntent_String = "com.ttco.bookmarker.newBookmarkAdded";
    private String bookmarkDeletedIntent_String = "com.ttco.bookmarker.bookmarkDeleted";

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    booksAdapter.notifyDataSetChanged();
                }
            };

    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() {
        @Override
        public void drag(int from, int to) {
            booksAdapter.swap(from, to);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constants.APPLICATION_CODE_STATE.equals("PRODUCTION"))
            Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_books);

        dbHelper = new DatabaseHelper(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();
        prefsEditor.apply();

        emptyListLayout = (RelativeLayout) findViewById(R.id.emptyListLayout);

        books = dbHelper.getAllBooks(null);

        handleEmptyOrPopulatedScreen(books);

        bookAddedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(bookAddedIntent_String)) {
                    prepareForNotifyDataChanged();
                    booksAdapter.notifyDataSetChanged();
                }
            }
        };

        bookmarkAddedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(bookmarkAddedIntent_String)) {
                    prepareForNotifyDataChanged();
                    booksAdapter.notifyDataSetChanged();
                }
            }
        };

        bookmarkDeletedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(bookmarkDeletedIntent_String) == 0) {
                    prepareForNotifyDataChanged();
                    booksAdapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter bookAddedFilter = new IntentFilter();
        bookAddedFilter.addAction(bookAddedIntent_String);
        registerReceiver(bookAddedBR, bookAddedFilter);

        IntentFilter bookmarkAddedFilter = new IntentFilter();
        bookmarkAddedFilter.addAction(bookmarkAddedIntent_String);
        registerReceiver(bookmarkAddedBR, bookmarkAddedFilter);

        IntentFilter bookmarksEmptyFilter = new IntentFilter();
        bookmarksEmptyFilter.addAction(bookmarkDeletedIntent_String);
        registerReceiver(bookmarkDeletedBR, bookmarksEmptyFilter);

    }

    public void handleAddBook_Pressed(View view) {
        if (createBookShowcase != null)
            createBookShowcase.hide();

        Intent openCreateBookActivity = new Intent(Books_Activity.this, Create_Book_Activity.class);
        startActivity(openCreateBookActivity);
    }

    @Override
    protected void onListItemClick(ListView listView, View v, int position, long id) {
        super.onListItemClick(listView, v, position, id);
        Intent openBookmarksForBook = new Intent(Books_Activity.this, Bookmarks_Activity.class);
        Book book = (Book) listView.getAdapter().getItem(position);
        openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_TITLE, book.getTitle());
        openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_ID, book.getId());
        openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_COLOR, book.getColorCode());
        startActivity(openBookmarksForBook);
    }

    public void prepareForNotifyDataChanged() {
        books = dbHelper.getAllBooks(null);

        if (books.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            showCreateBookShowcase();
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }
    }

    public void handleEmptyOrPopulatedScreen(List<Book> books) {
        if (books.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            showCreateBookShowcase();
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        booksAdapter = new Books_Adapter(this);
        DragSortListView thisDragSortListView = (DragSortListView) getListView();
        thisDragSortListView.setDropListener(onDrop);
        thisDragSortListView.setDragListener(onDrag);
        thisDragSortListView.setAdapter(booksAdapter);
    }

    public void showCreateBookShowcase() {
        if (!prefs.getBoolean(Constants.SEEN_BOOKS_SHOWCASE, false)) {
            RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lps.setMargins(getResources().getDimensionPixelOffset(R.dimen.button_margin_left), 0, 0, getResources().getDimensionPixelOffset(R.dimen.button_margin_bottom));

            ViewTarget target = new ViewTarget(R.id.createNewBookBTN, Books_Activity.this);

            String showcaseTitle = getString(R.string.create_book_showcase_title);

            String showcaseDescription = getString(R.string.create_book_showcase_description);

            createBookShowcase = new ShowcaseView.Builder(Books_Activity.this, getResources().getDimensionPixelSize(R.dimen.create_book_showcase_inner_rad), getResources().getDimensionPixelSize(R.dimen.create_book_showcase_outer_rad))
                    .setTarget(target)
                    .setContentTitle(Helper_Methods.fontifyString(showcaseTitle))
                    .setContentText(Helper_Methods.fontifyString(showcaseDescription))
                    .setStyle(R.style.CustomShowcaseTheme)
                    .hasManualPosition(true)
                    .xPostion(getResources().getDimensionPixelSize(R.dimen.create_book_text_x))
                    .yPostion(getResources().getDimensionPixelSize(R.dimen.create_book_text_y))
                    .build();
            createBookShowcase.setButtonPosition(lps);
            createBookShowcase.findViewById(R.id.showcase_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createBookShowcase.hide();
                    prefsEditor.putBoolean(Constants.SEEN_BOOKS_SHOWCASE, true);
                    prefsEditor.commit();
                }
            });
            createBookShowcase.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bookAddedBR);
        unregisterReceiver(bookmarkAddedBR);
        unregisterReceiver(bookmarkDeletedBR);
    }

    private class Books_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private Context context;
        private BooksViewHolder holder;
        private List<Bookmark> bookmarks;
        private DatabaseHelper dbHelper;

        public Books_Adapter(Context context) {
            super();
            this.context = context;

            dbHelper = new DatabaseHelper(context);
        }

        @Override
        public int getCount() {
            return books.size();
        }

        @Override
        public Object getItem(int i) {
            return books.get(i);
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
                convertView = inflater.inflate(R.layout.list_item_book, parent, false);

                holder = new BooksViewHolder();

                holder.bookDateAddedTV = (TextView) convertView.findViewById(R.id.bookDateAddedTV);
                holder.bookTitleTV = (TextView) convertView.findViewById(R.id.bookTitleTV);
                holder.bookAuthorTV = (TextView) convertView.findViewById(R.id.bookAuthorTV);
                holder.bookThumbIMG = (ImageView) convertView.findViewById(R.id.bookThumbIMG);
                holder.bookmarksNumberTV = (TextView) convertView.findViewById(R.id.bookmarksNumberTV);
                holder.bookAction = (Button) convertView.findViewById(R.id.bookAction);

                convertView.setTag(holder);
            } else {
                holder = (BooksViewHolder) convertView.getTag();
            }

            holder.bookTitleTV.setText(books.get(position).getTitle());
            holder.bookAuthorTV.setText(books.get(position).getAuthor());

            Picasso.with(Books_Activity.this).load(books.get(position).getImagePath()).error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(holder.bookThumbIMG);

            String[] bookDateAdded = books.get(position).getDate_added().split(" ");
            holder.bookDateAddedTV.setText(bookDateAdded[0] + " " + bookDateAdded[1]);

            switch (books.get(position).getColorCode()) {
                case 0:
                    holder.bookmarksNumberTV.setBackground(context.getResources().getDrawable(R.drawable.bookmark_pink));
                    break;
                case 1:
                    holder.bookmarksNumberTV.setBackground(context.getResources().getDrawable(R.drawable.bookmark_red));
                    break;
                case 2:
                    holder.bookmarksNumberTV.setBackground(context.getResources().getDrawable(R.drawable.bookmark_purple));
                    break;
                case 3:
                    holder.bookmarksNumberTV.setBackground(context.getResources().getDrawable(R.drawable.bookmark_yellow));
                    break;
                case 4:
                    holder.bookmarksNumberTV.setBackground(context.getResources().getDrawable(R.drawable.bookmark_blue));
                    break;
                case 5:
                    holder.bookmarksNumberTV.setBackground(context.getResources().getDrawable(R.drawable.bookmark_brown));
                    break;
            }

            holder.bookAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View overflowButton = view;
                    overflowButton.setBackground(getDrawable(R.drawable.menu_overflow_focus));

                    PopupMenu popup = new PopupMenu(context, view);
                    popup.getMenuInflater().inflate(R.menu.book_edit_delete,
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
                                    Intent editBookIntent = new Intent(Books_Activity.this, Create_Book_Activity.class);
                                    editBookIntent.putExtra(Constants.EDIT_BOOK_PURPOSE_STRING, Constants.EDIT_BOOK_PURPOSE_VALUE);
                                    editBookIntent.putExtra("book", books.get(position));
                                    startActivity(editBookIntent);
                                    break;
                                case R.id.remove:
                                    dbHelper.deleteBook(books.get(position).getId());
                                    prepareForNotifyDataChanged();
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

            bookmarks = dbHelper.getAllBookmarks(books.get(position).getId(), null);
            holder.bookmarksNumberTV.setText(bookmarks.size() + "");

            return convertView;
        }

        public void swap(int from, int to) {
            if (to < books.size() && from < books.size()) {
                Collections.swap(books, from, to);
                int tempNumber = books.get(from).getOrder();
                books.get(from).setOrder(books.get(to).getOrder());
                books.get(to).setOrder(tempNumber);
                dbHelper.updateBook(books.get(from));
                dbHelper.updateBook(books.get(to));
            }
        }

        private class BooksViewHolder {
            TextView bookDateAddedTV;
            TextView bookTitleTV;
            TextView bookAuthorTV;
            ImageView bookThumbIMG;
            TextView bookmarksNumberTV;
            Button bookAction;
        }
    }
}
