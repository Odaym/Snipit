package com.om.atomic.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
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
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import me.grantland.widget.AutofitTextView;

public class SearchResults_Activity extends BaseActivity {
    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;
    private ListView listView;

    private String query;
    private int book_color_code;
    private int book_id;
    private String book_title;
    private RelativeLayout emptyListLayout;
    private SearchResults_Adapter searchResultsAdapter;
    private Helper_Methods helperMethods;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        EventBus_Singleton.getInstance().register(this);

        helperMethods = new Helper_Methods(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.search_results_activity_title));

        dbHelper = new DatabaseHelper(this);

        query = getIntent().getStringExtra(Constants.EXTRAS_SEARCH_TERM);
        book_id = getIntent().getIntExtra(Constants.EXTRAS_BOOK_ID, -1);
        book_color_code = getIntent().getIntExtra(Constants.EXTRAS_BOOK_COLOR, -1);
        book_title = getIntent().getStringExtra(Constants.EXTRAS_BOOK_TITLE);

        helperMethods.setUpActionbarColors(this, book_color_code);

        emptyListLayout = (RelativeLayout) findViewById(R.id.emptyListLayout);
        listView = (ListView) findViewById(R.id.searchResultsList);

        bookmarks = dbHelper.searchAllBookmarks(book_id, query);

        handleEmptyOrPopulatedScreen(bookmarks);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (bookmarks.get(position).getIsNoteShowing() == 0) {
                    Intent intent = new Intent(SearchResults_Activity.this, View_Bookmark_Activity.class);
                    intent.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
                    intent.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
                    intent.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, position);
                    intent.putParcelableArrayListExtra("bookmarks", bookmarks);
                    startActivity(intent);

                    int bookmarkViews = dbHelper.getBookmarkViews(bookmarks.get(position).getId());
                    bookmarks.get(position).setViews(bookmarkViews + 1);
                    dbHelper.updateBookmark(bookmarks.get(position));
                    searchResultsAdapter.notifyDataSetChanged();

                    //This tells Bookmarks Activity to update the views counter of all bookmarks
                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_viewed"));
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
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

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        if (ebp.getMessage().equals("bookmark_note_changed") || ebp.getMessage().equals("bookmark_changed")) {
            bookmarks = dbHelper.searchAllBookmarks(book_id, query);
            handleEmptyOrPopulatedScreen(bookmarks);
        }
    }

    @DebugLog
    public void handleEmptyOrPopulatedScreen(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            finish();
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        searchResultsAdapter = new SearchResults_Adapter(this);
        listView.setAdapter(searchResultsAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus_Singleton.getInstance().unregister(this);
    }

    private class SearchResults_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private Context context;
        private BookmarksViewHolder holder;

        public SearchResults_Adapter(Context context) {
            super();
            this.context = context;
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
                holder.bookmarkNoteBTN = (Button) convertView.findViewById(R.id.bookmarkNoteBTN);
                holder.bookmarkNoteTV = (AutofitTextView) convertView.findViewById(R.id.bookmarkNoteTV);
                holder.motherView = (RelativeLayout) convertView.findViewById(R.id.list_item_bookmark);

                convertView.setTag(holder);
            } else {
                holder = (BookmarksViewHolder) convertView.getTag();
            }

            if (TextUtils.isEmpty(bookmarks.get(position).getNote()))
                holder.bookmarkNoteBTN.setVisibility(View.INVISIBLE);
            else
                holder.bookmarkNoteBTN.setVisibility(View.VISIBLE);

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
                holder.bookmarkAction.setVisibility(View.INVISIBLE);
                holder.bookmarkIMG.setVisibility(View.INVISIBLE);
                holder.bookmarkViews.setVisibility(View.INVISIBLE);
                holder.bookmarkName.setVisibility(View.INVISIBLE);
                holder.bookmarkNoteTV.setVisibility(View.VISIBLE);
                holder.bookmarkNoteBTN.setBackground(context.getResources().getDrawable(R.drawable.white_bookmark));
            }

            holder.bookmarkName.setText(bookmarks.get(position).getName());
            holder.bookmarkViews.setText("Views: " + bookmarks.get(position).getViews());

            Glide.with(SearchResults_Activity.this).load(new File(bookmarks.get(position).getImage_path())).centerCrop().error(getResources().getDrawable(R.drawable.notfound_1)).into(holder.bookmarkIMG);

//            Picasso.with(SearchResults_Activity.this).load(new File(bookmarks.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().error(helperMethods.getNotFoundImage(context)).into(holder.bookmarkIMG);

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
                                    Intent editBookmarkIntent = new Intent(SearchResults_Activity.this, Create_Bookmark_Activity.class);
                                    editBookmarkIntent.putExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, Constants.EDIT_BOOKMARK_PURPOSE_VALUE);
                                    editBookmarkIntent.putExtra(Constants.EXTRAS_BOOK_COLOR, book_color_code);
                                    editBookmarkIntent.putExtra("bookmark", bookmarks.get(position));
                                    startActivity(editBookmarkIntent);
                                    break;
                                case R.id.delete:
                                    dbHelper.deleteBookmark(bookmarks.get(position).getId());
                                    bookmarks = dbHelper.searchAllBookmarks(book_id, query);
                                    handleEmptyOrPopulatedScreen(bookmarks);
                                    EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_changed"));
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
                    hideClutterSet.setDuration(300);
                    hideClutterSet.start();
                }
            });

            return convertView;
        }
    }

    public static class BookmarksViewHolder {
        RelativeLayout motherView;
        TextView bookmarkName;
        ImageView bookmarkIMG;
        Button bookmarkAction;
        TextView bookmarkViews;
        Button bookmarkNoteBTN;
        AutofitTextView bookmarkNoteTV;
    }
}
