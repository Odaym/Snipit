package com.om.snipit.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.heinrichreimersoftware.materialdrawer.DrawerView;
import com.heinrichreimersoftware.materialdrawer.structure.DrawerItem;
import com.heinrichreimersoftware.materialdrawer.structure.DrawerProfile;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.om.atomic.R;
import com.om.snipit.classes.Book;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.Param;
import com.om.snipit.classes.Snippet;
import com.om.snipit.dragsort_listview.DragSortListView;
import com.om.snipit.showcaseview.ShowcaseView;
import com.om.snipit.showcaseview.ViewTarget;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import net.frakbot.jumpingbeans.JumpingBeans;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import me.grantland.widget.AutofitTextView;

public class Books_Activity extends Base_Activity {

    @InjectView(R.id.booksList)
    DragSortListView listView;
    @InjectView(R.id.emptyListLayout)
    RelativeLayout emptyListLayout;
    @InjectView(R.id.createNewBookBTN)
    FloatingActionButton createNewBookBTN;
    @InjectView(R.id.drawerLayout)
    DrawerLayout drawerLayout;
    @InjectView(R.id.navDrawer)
    DrawerView navDrawer;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    private ActionBarDrawerToggle drawerToggle;

    private LayoutAnimationController controller;

    private final static int SHOW_CREATE_BOOK_SHOWCASE = 1;
    private static Handler UIHandler = new Handler();

    private Books_Adapter booksAdapter;
    private List<Book> books;
    private Book tempBook;

    private DatabaseHelper databaseHelper = null;
    private RuntimeExceptionDao<Book, Integer> bookDAO;
    private RuntimeExceptionDao<Snippet, Integer> snipitDAO;
    private RuntimeExceptionDao<Param, Integer> paramDAO;

    private QueryBuilder<Book, Integer> bookQueryBuilder;
    private QueryBuilder<Snippet, Integer> bookmarkQueryBuilder;
    private PreparedQuery<Snippet> pq;
    private PreparedQuery<Book> pqBook;

    private ShowcaseView createBookShowcase;
    private int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    private Snackbar undoDeleteBookSB;
    private boolean itemPendingDeleteDecision = false;

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

        EventBus_Singleton.getInstance().register(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        bookDAO = getHelper().getBookDAO();
        snipitDAO = getHelper().getSnipitDAO();
        paramDAO = getHelper().getParamDAO();

        bookQueryBuilder = bookDAO.queryBuilder();
        bookmarkQueryBuilder = snipitDAO.queryBuilder();

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

        prepareQueryBuilder();

        books = bookDAO.query(pqBook);

        handleEmptyOrPopulatedScreen();

        setSupportActionBar(toolbar);
        helperMethods.setUpActionbarColors(this, Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS);

        if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        } else {
            listView.setDrawSelectorOnTop(true);
            listView.setSelector(R.drawable.abc_list_selector_holo_dark);
        }

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_closed) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.closeDrawer(GravityCompat.START);

        navDrawer.setProfile(
                new DrawerProfile()
                        .setBackground(getResources().getDrawable(R.drawable.navdrawer_background))
                        .setAvatar(getResources().getDrawable(R.drawable.ic_launcher))
                        .setName(getResources().getString(R.string.app_name))
                        .setDescription(getResources().getString(R.string.app_tagline))
        );

        /**
         * FAVORITE BOOKMARKS
         */
        navDrawer.addItem(
                new DrawerItem()
                        .setImage(getResources().getDrawable(R.drawable.favorites), DrawerItem.SMALL_AVATAR)
                        .setTextPrimary(getResources().getString(R.string.navdrawer_favorite_bookmarks_item_primary))
                        .setTextSecondary(getResources().getString(R.string.navdrawer_favorite_bookmarks_item_secondary))
        );

        /**
         * Snippets Gallery
         */
        navDrawer.addItem(
                new DrawerItem()
                        .setImage(getResources().getDrawable(R.drawable.trash), DrawerItem.SMALL_AVATAR)
                        .setTextPrimary(getResources().getString(R.string.navdrawer_snippets_gallery_primary))
                        .setTextSecondary(getResources().getString(R.string.navdrawer_snippets_gallery_secondary))
        );

        /**
         * UPGRADE TO PREMIUM
         */
        navDrawer.addFixedItem(
                new DrawerItem()
                        .setImage(getResources().getDrawable(R.drawable.premium), DrawerItem.SMALL_AVATAR)
                        .setTextPrimary(getResources().getString(R.string.navdrawer_upgrade_premium_item))
        );

        /**
         * SETTINGS
         */
        navDrawer.addFixedItem(
                new DrawerItem()
                        .setImage(getResources().getDrawable(R.drawable.settings), DrawerItem.SMALL_AVATAR)
                        .setTextPrimary(getResources().getString(R.string.settings))
        );

        navDrawer.setOnItemClickListener(new DrawerItem.OnItemClickListener() {
            @Override
            public void onClick(DrawerItem drawerItem, int id, int position) {
                navDrawer.selectItem(position);

                switch (position) {
                    case 0:
                        Toast.makeText(Books_Activity.this, "Your favorite bookmarks will appear here", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Intent openAllSnippets_Intent = new Intent(Books_Activity.this, Snippets_Gallery_Activity.class);
                        startActivity(openAllSnippets_Intent);
                        break;
                }
            }
        });

        navDrawer.setOnFixedItemClickListener(new DrawerItem.OnItemClickListener() {
            @Override
            public void onClick(DrawerItem drawerItem, int id, int position) {
                switch (position) {
                    case 0:
                        Toast.makeText(Books_Activity.this, "Upgrade to premium!", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Intent openSettingsIntent = new Intent(Books_Activity.this, Settings_Activity.class);
                        startActivity(openSettingsIntent);
                        break;
                }
            }
        });


        navDrawer.setBackground(getResources().getDrawable(R.drawable.navdrawer_background_repeat));

        createNewBookBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openCreateBookActivity = new Intent(Books_Activity.this, Create_Book_Activity.class);
                startActivity(openCreateBookActivity);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent openBookmarksForBook = new Intent(Books_Activity.this, Snippets_Activity.class);
                Book book = (Book) listView.getItemAtPosition(position);

                //Clicking on an adview when there's no Internet connection will cause this condition to be satisfied because no Book will be found at the index of that adview
                if (book != null) {
                    openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_TITLE, book.getTitle());
                    openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_ID, book.getId());
                    openBookmarksForBook.putExtra(Constants.EXTRAS_BOOK_COLOR, book.getColorCode());
                    openBookmarksForBook.putExtra(Constants.EXTRAS_IS_FAVORITE_BOOKMARKS_FRAGMENT, false);
                    startActivity(openBookmarksForBook);
                }
            }
        });
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }

    public void prepareQueryBuilder() {
        try {
            bookQueryBuilder.where().not().eq("title", "null");
            bookQueryBuilder.orderBy("order", true);
            pqBook = bookQueryBuilder.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START);
            else
                super.onBackPressed();

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        String ebpMessage = ebp.getMessage();

        switch (ebpMessage) {
            case "book_added":
                if (createBookShowcase != null)
                    createBookShowcase.hide();

                handleBusEvents_ListRefresher();
                Log.d("EVENTS", "book_added - Books_Activity");
                break;
            case "bookmark_added_books_activity":
                handleBusEvents_ListRefresher();
                Log.d("EVENTS", "bookmark_added_books_activity - Books_Activity");
                break;
            case "bookmark_deleted_books_activity":
                handleBusEvents_ListRefresher();
                Log.d("EVENTS", "bookmark_deleted_books_activity - Books_Activity");
                break;
        }
    }

    public void handleBusEvents_ListRefresher() {
        prepareForNotifyDataChanged();

        //If animations are disabled
        Param animationsParam = paramDAO.queryForId(Constants.ANIMATIONS_DATABASE_VALUE);
        if (animationsParam.isEnabled()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    listView.setLayoutAnimation(controller);
                    booksAdapter.notifyDataSetChanged();
                }
            }, 100);
        } else {
            booksAdapter.notifyDataSetChanged();
        }
    }

    @DebugLog
    public void prepareForNotifyDataChanged() {
        books = bookDAO.query(pqBook);
        handleEmptyUI();
    }

    @DebugLog
    public void handleEmptyOrPopulatedScreen() {
        handleEmptyUI();

        booksAdapter = new Books_Adapter(this);

        listView.setDropListener(onDrop);
        listView.setDragListener(onDrag);

//        final View listViewHeaderAd = View.inflate(this, R.layout.books_list_adview_footer, null);
//        AdView mAdView = (AdView) listViewHeaderAd.findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

        controller
                = AnimationUtils.loadLayoutAnimation(
                this, R.anim.books_list_layout_controller);

        //If animations are enabled
        Param animationsParam = paramDAO.queryForId(Constants.ANIMATIONS_DATABASE_VALUE);
        if (animationsParam.isEnabled()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    listView.addFooterView(listViewHeaderAd);
                    listView.setAdapter(booksAdapter);
                    listView.setLayoutAnimation(controller);
                }
            }, 100);
        } else {
//            listView.addFooterView(listViewHeaderAd);
            listView.setAdapter(booksAdapter);
        }
    }

    @DebugLog
    public void showCreateBookShowcase() {
        Param tutorialMode = paramDAO.queryForId(Constants.TUTORIAL_MODE_DATABASE_VALUE);

        if (tutorialMode.isEnabled()) {
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

                    Param bookTutorialParam = paramDAO.queryForId(Constants.BOOK_TUTORIAL_DATABASE_VALUE_ENABLED);
                    bookTutorialParam.setEnabled(false);
                    paramDAO.update(bookTutorialParam);

                    handleEmptyUI();
                }
            });
            createBookShowcase.show();
        }
    }

    public void handleEmptyUI() {
        //Books are empty and the coachmark has been dismissed

        Param bookTutorialParam = paramDAO.queryForId(Constants.BOOK_TUTORIAL_DATABASE_VALUE_ENABLED);

        if (bookDAO.queryForAll().isEmpty() && !bookTutorialParam.isEnabled()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            JumpingBeans.with((TextView) emptyListLayout.findViewById(R.id.emptyLayoutMessageTV)).appendJumpingDots().build();
        } else if (bookDAO.queryForAll().isEmpty()) {
            emptyListLayout.setVisibility(View.GONE);
            UIHandler.sendEmptyMessageDelayed(SHOW_CREATE_BOOK_SHOWCASE, 200);
        } else {
            emptyListLayout.setVisibility(View.INVISIBLE);
        }
    }

    private void deleteCell(final View bookView, final int index) {
        BooksViewHolder vh = (BooksViewHolder) bookView.getTag();
        vh.needInflate = true;

        tempBook = books.get(index);

        itemPendingDeleteDecision = true;

        Animation.AnimationListener collapseAL = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                showUndeleteDialog(tempBook);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };

        collapse(bookView, collapseAL);
    }

    private void collapse(final View v, AnimationListener al) {
        final int initialHeight = v.getMeasuredHeight();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (al != null) {
            anim.setAnimationListener(al);
        }

        anim.setDuration(Constants.DELETE_BOOK_BOOKMARK_ANIMATION_DURATION);
        v.startAnimation(anim);
    }

    public void showUndeleteDialog(final Book tempBookToDelete) {
        itemPendingDeleteDecision = true;

        tempBook = tempBookToDelete;

        Spannable sentenceToSpan = new SpannableString(getResources().getString(R.string.delete_book_confirmation_message) + " " + tempBookToDelete.getTitle());

        sentenceToSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        books.remove(tempBookToDelete);
        handleEmptyUI();
        booksAdapter.notifyDataSetChanged();

        undoDeleteBookSB =
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
                                    finalizeBookDeletion(tempBookToDelete);
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
                        prepareForNotifyDataChanged();
                        booksAdapter.notifyDataSetChanged();

                        itemPendingDeleteDecision = false;
                        undoDeleteBookSB.dismiss();
                    }
                });

        undoDeleteBookSB.show(Books_Activity.this);
    }

    public void finalizeBookDeletion(Book tempBook) {
        snipitDAO.delete(snipitDAO.queryForEq("book_id", tempBook.getId()));
        bookDAO.delete(tempBook);
        prepareForNotifyDataChanged();
        booksAdapter.notifyDataSetChanged();
        itemPendingDeleteDecision = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (itemPendingDeleteDecision) {

            finalizeBookDeletion(tempBook);

            if (undoDeleteBookSB.isShowing()) {
                undoDeleteBookSB.dismiss();
            }
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
        private List<Snippet> snippets;

        public Books_Adapter(Context context) {
            super();
            this.context = context;

            this.inflater = (LayoutInflater) context
                    .getSystemService(LAYOUT_INFLATER_SERVICE);
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
            final View parentView;

            if (convertView == null || ((BooksViewHolder) convertView.getTag()).needInflate) {
                parentView = inflater.inflate(R.layout.list_item_book, parent, false);

                holder = new BooksViewHolder();

                holder.list_item_book = (RelativeLayout) parentView.findViewById(R.id.list_item_book);
                holder.bookDateAddedTV = (TextView) parentView.findViewById(R.id.bookDateAddedTV);
                holder.bookTitleTV = (AutofitTextView) parentView.findViewById(R.id.bookTitleTV);
                holder.bookAuthorTV = (AutofitTextView) parentView.findViewById(R.id.bookAuthorTV);
                holder.bookThumbIMG = (ImageView) parentView.findViewById(R.id.bookThumbIMG);
                holder.bookmarksNumberTV = (TextView) parentView.findViewById(R.id.bookmarksNumberTV);
                holder.bookActionLayout = (LinearLayout) parentView.findViewById(R.id.bookActionLayout);
                holder.needInflate = false;

                parentView.setTag(holder);
            } else {
                parentView = convertView;
            }

            holder = (BooksViewHolder) parentView.getTag();

            if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
                holder.bookmarksNumberTV.setElevation(5f);
            }

            holder.bookTitleTV.setText(books.get(position).getTitle());
            holder.bookAuthorTV.setText(books.get(position).getAuthor());

            Picasso.with(Books_Activity.this).load(books.get(position).getImagePath()).error(getResources().getDrawable(R.drawable.notfound_1)).into(holder.bookThumbIMG);

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

            holder.bookActionLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View overflowButton = view.findViewById(R.id.bookAction);
                    overflowButton.findViewById(R.id.bookAction).setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_focus));

                    PopupMenu popup = new PopupMenu(context, view);
                    popup.getMenuInflater().inflate(R.menu.book_list_item,
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
                                    editBookIntent.putExtra(Constants.EXTRAS_BOOK_ID, books.get(position).getId());
//                                    editBookIntent.putExtra(Constants.EXTRAS_BOOK_COLOR, books.get(position).getColorCode());
//                                    editBookIntent.putExtra("book", Parcels.wrap(books.get(position)));
                                    startActivity(editBookIntent);
                                    break;
                                case R.id.delete:
                                    //Dissmiss the UNDO Snackbar and handle the deletion of the previously awaiting item yourself
                                    if (undoDeleteBookSB != null && undoDeleteBookSB.isShowing()) {
                                        //Careful about position that is passed from the adapter! This has to be accounted for again by using getItemAtPosition because there's an adview among the views
                                        //I am able to use tempBook h=ere because I am certain that it would have now been initialized inside deleteCell(), no way to reach this point without having been through deleteCell() first

                                        try {
                                            bookmarkQueryBuilder.where().eq("book_id", tempBook.getId());
                                            pq = bookmarkQueryBuilder.prepare();
                                            snipitDAO.delete(snipitDAO.query(pq));
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        bookDAO.delete(tempBook);
                                        itemPendingDeleteDecision = false;
                                        undoDeleteBookSB.dismiss();
                                    }

                                    Param animationsParam = paramDAO.queryForId(Constants.ANIMATIONS_DATABASE_VALUE);

                                    if (animationsParam.isEnabled()) {
                                        deleteCell(parentView, position);
                                    } else {
                                        showUndeleteDialog(books.get(position));
                                    }

                                    break;
                            }

                            return true;
                        }
                    });
                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu popupMenu) {
                            overflowButton.findViewById(R.id.bookAction).setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_fade));
                        }
                    });
                }
            });

            snippets = snipitDAO.queryForEq("book_id", books.get(position).getId());
            holder.bookmarksNumberTV.setText(snippets.size() + "");

            return parentView;
        }

        @DebugLog
        public void swap(int from, int to) {
            if (to < books.size() && from < books.size()) {
                Collections.swap(books, from, to);
                int tempNumber = books.get(from).getOrder();
                books.get(from).setOrder(books.get(to).getOrder());
                books.get(to).setOrder(tempNumber);
                bookDAO.update(books.get(from));
                bookDAO.update(books.get(to));
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
        LinearLayout bookActionLayout;
        boolean needInflate;
    }
}
