package com.om.atomic.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
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

import com.crashlytics.android.Crashlytics;
import com.melnykov.fab.FloatingActionButton;
import com.om.atomic.R;
import com.om.atomic.classes.Book;
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
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import icepick.Icicle;
import io.fabric.sdk.android.Fabric;
import me.grantland.widget.AutofitTextView;

public class Books_Activity extends Base_Activity {

    @Icicle
    ArrayList<Book> books;

    @InjectView(R.id.booksList)
    DragSortListView listView;
    @InjectView(R.id.emptyListLayout)
    RelativeLayout emptyListLayout;
    @InjectView(R.id.createNewBookBTN)
    FloatingActionButton createNewBookBTN;
//    @InjectView(R.id.navDrawer)
//    DrawerFrameLayout navDrawer;

    private final static int SHOW_CREATE_BOOK_SHOWCASE = 1;
    private static Handler UIHandler = new Handler();

    private Helper_Methods helperMethods;
    private Books_Adapter booksAdapter;
    private DatabaseHelper dbHelper;
    private ShowcaseView createBookShowcase;
    private int currentapiVersion = android.os.Build.VERSION.SDK_INT;

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
        setContentView(R.layout.activity_books);

        if (Constants.APPLICATION_CODE_STATE.equals("PRODUCTION"))
            Fabric.with(this, new Crashlytics());

        EventBus_Singleton.getInstance().register(this);

        ButterKnife.inject(this);

        UIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SHOW_CREATE_BOOK_SHOWCASE:
                        showCreateBookShowcase();
                        break;
                }
                super.handleMessage(msg);
            }
        };

        dbHelper = new DatabaseHelper(this);
        books = dbHelper.getAllBooks(null);
        helperMethods = new Helper_Methods(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        /**
         * Navigation Drawer code
         */
//        navDrawer.setProfile(
//                new DrawerProfile()
//                        .setBackground(getResources().getDrawable(R.drawable.navdrawer_background))
//                        .setName("Oday Maleh")
//                        .setDescription("Motherfucker")
//                        .setOnProfileClickListener(new DrawerProfile.OnProfileClickListener() {
//                            @Override
//                            public void onClick(DrawerProfile drawerProfile) {
//                                Toast.makeText(Books_Activity.this, "Clicked profile", Toast.LENGTH_SHORT).show();
//                            }
//                        })
//        );
//
//        navDrawer.addItem(
//                new DrawerItem()
//                        .setImage(getResources().getDrawable(R.drawable.notfound_1))
//                        .setTextPrimary("Title 1")
//                        .setTextSecondary("Description 1")
//                        .setOnItemClickListener(new DrawerItem.OnItemClickListener() {
//                            @Override
//                            public void onClick(DrawerItem drawerItem, int id, int position) {
//                                Toast.makeText(Books_Activity.this, "Clicked 1st item", Toast.LENGTH_SHORT).show();
//                            }
//                        })
//        );
//        navDrawer.addDivider();
//        navDrawer.addItem(
//                new DrawerItem()
//                        .setImage(getResources().getDrawable(R.drawable.notfound_1))
//                        .setTextPrimary("Title 2")
//                        .setTextSecondary("Description 2")
//                        .setOnItemClickListener(new DrawerItem.OnItemClickListener() {
//                            @Override
//                            public void onClick(DrawerItem drawerItem, int id, int position) {
//                                Toast.makeText(Books_Activity.this, "Clicked 2nd item", Toast.LENGTH_SHORT).show();
//                            }
//                        })
//        );

//        String USER_FULL_NAME = getIntent().getExtras().getString(Constants.USER_FULL_NAME);
//        String USER_EMAIL_ADDRESS = getIntent().getExtras().getString(Constants.USER_EMAIL_ADDRESS);
//        String USER_PHOTO_URL = getIntent().getExtras().getString(Constants.USER_PHOTO_URL);
//
//        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, navDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_closed) {
//
//            @Override
//            public void onDrawerOpened(View drawerView) {
//                super.onDrawerOpened(drawerView);
//            }
//
//            @Override
//            public void onDrawerClosed(View drawerView) {
//                super.onDrawerClosed(drawerView);
//            }
//        };
//
//        navDrawer.setDrawerListener(mDrawerToggle);
//        mDrawerToggle.syncState();
        /**
         * END OF NAVIGATION DRAWER CODE
         */

        handleEmptyOrPopulatedScreen(books);

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

        createNewBookBTN.attachToListView(listView);

//        populateWithSampleData();
    }

//    public void uploadBookDataToParse(List<Book> books) {
//        for (Book book : books) {
//            ParseObject bookObject = new ParseObject("Bookmark");
//            bookObject.put("title", book.getTitle());
//            bookObject.put("author", book.getAuthor());
//            bookObject.saveInBackground();
//        }
//    }
//
//    public void uploadBookmarkDataToParse() {
//        List<Book> books = dbHelper.getAllBooks(null);
//        List<Bookmark> bookmarks;
//
//        for (Book book : books) {
//            bookmarks = dbHelper.getAllBookmarks(book.getId(), null);
//            for (Bookmark bookmark : bookmarks) {
//                ParseObject bookmarkObject = new ParseObject("Bookmark");
//                bookmarkObject.put("book_id", bookmark.getBookId());
//                bookmarkObject.put("title", bookmark.getName());
//                bookmarkObject.put("page_number", bookmark.getPage_number());
//                bookmarkObject.saveInBackground();
//            }
//        }
//    }

    public void populateWithSampleData() {
        final List<Book> booksToInsert = new ArrayList<Book>();
        final List<Bookmark> bookmarksToInsert = new ArrayList<Bookmark>();

        Date date = new Date();
        final String day = (String) android.text.format.DateFormat.format("dd", date);
        final String month = (String) android.text.format.DateFormat.format("MMM", date);
        final String year = (String) android.text.format.DateFormat.format("yyyy", date);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.findInBackground(new FindCallback<ParseObject>() {
                                   public void done(final List<ParseObject> parseObjects, ParseException e) {
                                       if (e == null) {
                                           // object will be your game score
                                           for (ParseObject parseObject : parseObjects) {
                                               Random rand = new Random();

                                               Book book = new Book();
                                               book.setId(Integer.parseInt(parseObject.get("book_id").toString()));
                                               book.setTitle(parseObject.get("title").toString());
                                               book.setAuthor(parseObject.get("author").toString());
                                               book.setImagePath(parseObject.get("thumb").toString());
                                               book.setDate_added(month + " " + day + " " + year);
                                               book.setColorCode(rand.nextInt(7 - 1));

                                               booksToInsert.add(book);
                                           }

                                           ParseQuery<ParseObject> query = ParseQuery.getQuery("Bookmark");
                                           query.findInBackground(new FindCallback<ParseObject>() {
                                               @Override
                                               public void done(List<ParseObject> parseObjects, ParseException e) {
                                                   if (e == null) {
                                                       for (ParseObject parseObject : parseObjects) {
                                                           Bookmark bookmark = new Bookmark();
                                                           bookmark.setBookId(Integer.parseInt(parseObject.get("book_id").toString()));
                                                           bookmark.setName(parseObject.get("title").toString());
                                                           bookmark.setPage_number(Integer.parseInt(parseObject.get("page_number").toString()));
                                                           bookmark.setImage_path(parseObject.get("image").toString());

                                                           bookmark.setDate_added(month + " " + day + ", " + year);

                                                           bookmarksToInsert.add(bookmark);
                                                       }

                                                       for (int i = 0; i < booksToInsert.size(); i++) {
                                                           dbHelper.createSampleBook(booksToInsert.get(i));
                                                       }

                                                       for (int j = 0; j < bookmarksToInsert.size(); j++) {
                                                           switch (bookmarksToInsert.get(j).getBookId()) {
                                                               case 3:
                                                                   dbHelper.createBookmark(bookmarksToInsert.get(j), 3);
                                                                   break;
                                                               case 4:
                                                                   dbHelper.createBookmark(bookmarksToInsert.get(j), 4);
                                                                   break;
                                                               case 5:
                                                                   dbHelper.createBookmark(bookmarksToInsert.get(j), 5);
                                                                   break;
                                                           }
                                                       }
                                                       EventBus_Singleton.getInstance().post(new EventBus_Poster("book_added"));
                                                   }
                                               }
                                           });
                                       } else {
                                           // something went wrong
                                       }
                                   }
                               }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.books_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent openSettingsIntent = new Intent(this, Settings_Activity.class);
        startActivity(openSettingsIntent);

        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        prepareForNotifyDataChanged();
        booksAdapter.notifyDataSetChanged();
    }

    @DebugLog
    public void prepareForNotifyDataChanged() {
        books = dbHelper.getAllBooks(null);

        handleEmptyUI(books);
    }

    @DebugLog
    public void handleEmptyOrPopulatedScreen(List<Book> books) {
        handleEmptyUI(books);

        booksAdapter = new Books_Adapter(this);
        DragSortListView thisDragSortListView = listView;
        thisDragSortListView.setDropListener(onDrop);
        thisDragSortListView.setDragListener(onDrag);
        thisDragSortListView.setAdapter(booksAdapter);
    }

    @DebugLog
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

                    handleEmptyUI(books);
                }
            });
            createBookShowcase.show();
        }
    }

    public void handleEmptyUI(List<Book> books) {
        //Books are empty and the coachmark has been dismissed
        if (books.isEmpty() && dbHelper.getSeensParam(null, 2)) {
            emptyListLayout.setVisibility(View.VISIBLE);
        } else if (books.isEmpty()) {
            emptyListLayout.setVisibility(View.GONE);
            UIHandler.sendEmptyMessageDelayed(SHOW_CREATE_BOOK_SHOWCASE, 200);
        } else {
            emptyListLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus_Singleton.getInstance().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                holder.bookTitleTV = (AutofitTextView) convertView.findViewById(R.id.bookTitleTV);
                holder.bookAuthorTV = (AutofitTextView) convertView.findViewById(R.id.bookAuthorTV);
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

//            if (books.get(position).getImagePath() != null) {
            Picasso.with(Books_Activity.this).load(books.get(position).getImagePath()).error(getResources().getDrawable(R.drawable.notfound_1)).into(holder.bookThumbIMG);
//            }

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
                                    editBookIntent.putExtra(Constants.EXTRAS_BOOK_COLOR, books.get(position).getColorCode());
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

        @DebugLog
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
    }

    public static class BooksViewHolder {
        RelativeLayout list_item_book;
        TextView bookDateAddedTV;
        AutofitTextView bookTitleTV;
        AutofitTextView bookAuthorTV;
        ImageView bookThumbIMG;
        TextView bookmarksNumberTV;
        Button bookAction;
    }
}
