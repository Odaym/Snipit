package com.ttco.bookmarker.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Book;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.fabric.sdk.android.Fabric;

public class Books_Activity extends ActionBarActivity {

    private Books_Adapter booksAdapter;

    private DatabaseHelper dbHelper;
    private ArrayList<Book> books;
    private ShowcaseView createBookShowcase;
    private int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    @InjectView(R.id.booksList)
    DragSortListView listView;
    @InjectView(R.id.emptyListLayout)
    RelativeLayout emptyListLayout;
    @InjectView(R.id.createNewBookBTN)
    ImageButton createNewBookBTN;

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

        EventBus_Singleton.getInstance().register(this);

        setContentView(R.layout.activity_books);

        ButterKnife.inject(this);

        dbHelper = new DatabaseHelper(this);

        books = dbHelper.getAllBooks(null);

        handleEmptyOrPopulatedScreen(books);

        if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            createNewBookBTN.setElevation(15f);
        }

        createNewBookBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (createBookShowcase != null)
                    createBookShowcase.hide();

                Intent openCreateBookActivity = new Intent(Books_Activity.this, Create_Book_Activity.class);
                startActivity(openCreateBookActivity);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent openBookmarksForBook = new Intent(Books_Activity.this, Bookmarks_Activity.class);
                Book book = (Book) listView.getAdapter().getItem(position);
                openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_TITLE, book.getTitle());
                openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_ID, book.getId());
                openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_COLOR, book.getColorCode());
                startActivity(openBookmarksForBook);
            }
        });
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        prepareForNotifyDataChanged();
        booksAdapter.notifyDataSetChanged();
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
        DragSortListView thisDragSortListView = listView;
        thisDragSortListView.setDropListener(onDrop);
        thisDragSortListView.setDragListener(onDrag);
        thisDragSortListView.setAdapter(booksAdapter);
    }

    public void showCreateBookShowcase() {
        if (!dbHelper.getSeensParam(null, 2)) {
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

                    Param param = new Param();
                    param.setNumber(2);
                    param.setValue("True");
                    dbHelper.updateParam(param);
                }
            });
            createBookShowcase.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus_Singleton.getInstance().unregister(this);
    }

    public class Books_Adapter extends BaseAdapter {

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

                holder.list_item_book = (RelativeLayout) convertView.findViewById(R.id.list_item_book);
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

            if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
                holder.bookmarksNumberTV.setElevation(5f);
            }

            ViewGroup.LayoutParams listItemHeightParam = holder.list_item_book.getLayoutParams();

            //If the item has a bookmark image, increase the hosting row's height
            if (books.get(position).getImagePath() != null) {
                listItemHeightParam.height = context.getResources().getDimensionPixelSize(R.dimen.book_with_image_height);
                holder.list_item_book.setLayoutParams(listItemHeightParam);
            } else {
                listItemHeightParam.height = context.getResources().getDimensionPixelSize(R.dimen.book_without_image_height);
                holder.list_item_book.setLayoutParams(listItemHeightParam);
            }

            holder.bookTitleTV.setText(books.get(position).getTitle());
            holder.bookAuthorTV.setText(books.get(position).getAuthor());

            Picasso.with(Books_Activity.this).load(books.get(position).getImagePath()).error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(holder.bookThumbIMG);

            String[] bookDateAdded = books.get(position).getDate_added().split(" ");
            holder.bookDateAddedTV.setText(bookDateAdded[0] + " " + bookDateAdded[1] + ", " + bookDateAdded[2]);

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
                    overflowButton.setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_focus));

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
                            overflowButton.setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_fade));
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

        public class BooksViewHolder {
            RelativeLayout list_item_book;
            TextView bookDateAddedTV;
            TextView bookTitleTV;
            TextView bookAuthorTV;
            ImageView bookThumbIMG;
            TextView bookmarksNumberTV;
            Button bookAction;
        }
    }
}
