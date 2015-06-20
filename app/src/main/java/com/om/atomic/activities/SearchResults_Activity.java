package com.om.atomic.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.om.atomic.R;
import com.om.atomic.classes.Bookmark;
import com.om.atomic.classes.Constants;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.om.atomic.classes.Helper_Methods;
import com.om.atomic.classes.RoundedTransform;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import me.grantland.widget.AutofitTextView;

public class SearchResults_Activity extends Base_Activity {
    static final int DELETE_BOOKMARK_ANIMATION_DURATION = 500;

    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;

    @InjectView(R.id.searchResultsList)
    ListView listView;
    @InjectView(R.id.searchNotFoundTV)
    TextView searchNotFoundTV;

    private Snackbar undoDeleteBookmarkSB;
    private boolean itemPendingDeleteDecision = false;

    private Bookmark tempBookmark;

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

        EventBus_Singleton.getInstance().register(this);

        ButterKnife.inject(this);

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
                Bookmark bookmark = ((Bookmark) listView.getItemAtPosition(position));

                //Clicking on an adview when there's no Internet connection will cause this condition to be satisfied because no Book will be found at the index of that adview
                if (bookmark != null) {
                    if (bookmark.getIsNoteShowing() == 0) {
                        Intent intent = new Intent(SearchResults_Activity.this, View_Bookmark_Activity.class);
                        intent.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
                        intent.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
                        intent.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, position - 1);
                        intent.putExtra(Constants.EXTRAS_SEARCH_TERM, query);
                        intent.putParcelableArrayListExtra("bookmarks", bookmarks);
                        startActivity(intent);

                        int bookmarkViews = dbHelper.getBookmarkViews(bookmark.getId());
                        bookmark.setViews(bookmarkViews + 1);
                        dbHelper.updateBookmark(bookmark);
                        searchResultsAdapter.notifyDataSetChanged();

                        //This tells Bookmarks Activity to update the views counter of all bookmarks
                        EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_viewed"));
                    }
                }
            }
        });
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        switch (ebp.getMessage()) {
            case "bookmark_image_updated":
                Helper_Methods.delete_image_from_disk(ebp.getExtra());
            case "bookmark_changed":
            case "bookmark_note_changed":
                prepareForNotifyDataChanged(book_id, query);
                searchResultsAdapter.notifyDataSetChanged();
                break;
        }
    }

    @DebugLog
    public void handleEmptyOrPopulatedScreen(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
            searchNotFoundTV.setText(searchNotFoundTV.getText() + " \"" + query + "\"");
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        searchResultsAdapter = new SearchResults_Adapter(this);

        final View listViewHeaderAd = View.inflate(this, R.layout.search_results_list_adview_header, null);
        AdView mAdView = (AdView) listViewHeaderAd.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        listView.addHeaderView(listViewHeaderAd);
        listView.setAdapter(searchResultsAdapter);
    }

    @DebugLog
    public void prepareForNotifyDataChanged(int book_id, String searchQuery) {
        bookmarks = dbHelper.searchAllBookmarks(book_id, searchQuery);
        if (bookmarks.isEmpty()) {
            finish();
        }
    }

    private void deleteCell(final View v, final int index) {
        BookmarksViewHolder vh = (BookmarksViewHolder) v.getTag();
        vh.needInflate = true;

        tempBookmark = bookmarks.get(index);

        itemPendingDeleteDecision = true;

        Animation.AnimationListener collapseAL = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {

                Spannable sentenceToSpan = new SpannableString(getResources().getString(R.string.delete_book_confirmation_message) + " " + bookmarks.get(index).getName());

                sentenceToSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                bookmarks.remove(index);
                searchResultsAdapter.notifyDataSetChanged();

                undoDeleteBookmarkSB =
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
                                            finalizeBookmarkDeletion(tempBookmark);
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
                                prepareForNotifyDataChanged(book_id, query);
                                searchResultsAdapter.notifyDataSetChanged();

                                itemPendingDeleteDecision = false;
                                undoDeleteBookmarkSB.dismiss();
                            }
                        });

                undoDeleteBookmarkSB.show(SearchResults_Activity.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };

        collapse(v, collapseAL);
    }

    private void collapse(final View v, Animation.AnimationListener al) {
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

        anim.setDuration(DELETE_BOOKMARK_ANIMATION_DURATION);
        v.startAnimation(anim);
    }

    public void finalizeBookmarkDeletion(Bookmark tempBookmark) {
        Helper_Methods.delete_image_from_disk(tempBookmark.getImage_path());
        dbHelper.deleteBookmark(tempBookmark.getId());
        prepareForNotifyDataChanged(tempBookmark.getBookId(), query);
        searchResultsAdapter.notifyDataSetChanged();
        itemPendingDeleteDecision = false;

        EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_changed"));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (itemPendingDeleteDecision) {

            finalizeBookmarkDeletion(tempBookmark);

            if (undoDeleteBookmarkSB.isShowing()) {
                undoDeleteBookmarkSB.dismiss();
            }
        }
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
            this.inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            final View parentView;

            if (convertView == null || ((BookmarksViewHolder) convertView.getTag()).needInflate) {
                parentView = inflater.inflate(R.layout.list_item_bookmark, parent, false);

                holder = new BookmarksViewHolder();

                holder.bookmarkName = (TextView) parentView.findViewById(R.id.bookmarkNameTV);
                holder.bookmarkAction = (Button) parentView.findViewById(R.id.bookmarkAction);
                holder.bookmarkIMG = (ImageView) parentView.findViewById(R.id.bookmarkIMG);
                holder.bookmarkViews = (TextView) parentView.findViewById(R.id.bookmarkViewsTV);
                holder.bookmarkNoteBTN = (Button) parentView.findViewById(R.id.bookmarkNoteBTN);
                holder.bookmarkNoteTV = (AutofitTextView) parentView.findViewById(R.id.bookmarkNoteTV);
                holder.imageProgressLoader = (ProgressBar) parentView.findViewById(R.id.imageProgressLoader);
                holder.motherView = (RelativeLayout) parentView.findViewById(R.id.list_item_bookmark);
                holder.needInflate = false;

                parentView.setTag(holder);
            } else {
                parentView = convertView;
            }

            holder = (BookmarksViewHolder) parentView.getTag();

            if (TextUtils.isEmpty(bookmarks.get(position).getNote()))
                holder.bookmarkNoteBTN.setVisibility(View.INVISIBLE);
            else
                holder.bookmarkNoteBTN.setVisibility(View.VISIBLE);

            LayerDrawable drawable = (LayerDrawable) ((LayerDrawable) holder.motherView
                    .getBackground()).findDrawableByLayerId(R.id.content);
            GradientDrawable gradient = (GradientDrawable) drawable.findDrawableByLayerId(R.id.innerView);

            if (bookmarks.get(position).getIsNoteShowing() == 0) {
                gradient.setColor(context.getResources().getColor(R.color.white));

                holder.bookmarkAction.setAlpha(1f);
                holder.bookmarkAction.setVisibility(View.VISIBLE);
                holder.bookmarkIMG.setAlpha(1f);
                holder.bookmarkIMG.setVisibility(View.VISIBLE);
                holder.bookmarkViews.setAlpha(1f);
                holder.bookmarkViews.setVisibility(View.VISIBLE);
                holder.bookmarkName.setVisibility(View.VISIBLE);
                holder.bookmarkName.setAlpha(1f);
                holder.imageProgressLoader.setAlpha(1f);
                holder.bookmarkNoteBTN.setBackground(context.getResources().getDrawable(R.drawable.gray_bookmark));
            } else {
                gradient.setColor(context.getResources().getColor(helperMethods.determineNoteViewBackground(book_color_code)));

                holder.bookmarkNoteTV.setText(bookmarks.get(position).getNote());
                holder.bookmarkAction.setVisibility(View.INVISIBLE);
                holder.bookmarkIMG.setVisibility(View.INVISIBLE);
                holder.bookmarkViews.setVisibility(View.INVISIBLE);
                holder.bookmarkName.setVisibility(View.INVISIBLE);
                holder.bookmarkNoteTV.setVisibility(View.VISIBLE);
                holder.imageProgressLoader.setVisibility(View.VISIBLE);
                holder.bookmarkNoteBTN.setBackground(context.getResources().getDrawable(R.drawable.white_bookmark));
            }

            holder.bookmarkName.setText(bookmarks.get(position).getName());
            holder.bookmarkViews.setText(context.getResources().getText(R.string.bookmark_views_label) + " " + bookmarks.get(position).getViews());

//            try {
//                //If the String was a URL then this bookmark is a sample
//                new URL(bookmarks.get(position).getImage_path());
//                Glide.with(SearchResults_Activity.this).load(bookmarks.get(position).getImage_path()).centerCrop().error(context.getResources().getDrawable(R.drawable.notfound_1)).into(holder.bookmarkIMG);
//            } catch (MalformedURLException e) {
//                //Else it's on disk
//                Glide.with(SearchResults_Activity.this).load(new File(bookmarks.get(position).getImage_path())).centerCrop().error(context.getResources().getDrawable(R.drawable.notfound_1)).into(holder.bookmarkIMG);
//            }

            if (bookmarks.get(position).getImage_path().contains("http")) {
                Picasso.with(SearchResults_Activity.this).load(bookmarks.get(position).getImage_path()).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().transform(new RoundedTransform(context.getResources().getDimensionPixelSize(R.dimen.bookmark_image_shape_corners_radius), context.getResources().getDimensionPixelSize(R.dimen.bookmark_image_shape_corners_padding_bottom))).error(context.getResources().getDrawable(R.drawable.bookmark_not_found)).noPlaceholder().into(holder.bookmarkIMG);
            } else
                Picasso.with(SearchResults_Activity.this).load(new File(bookmarks.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().transform(new RoundedTransform(context.getResources().getDimensionPixelSize(R.dimen.bookmark_image_shape_corners_radius), context.getResources().getDimensionPixelSize(R.dimen.bookmark_image_shape_corners_padding_bottom))).error(context.getResources().getDrawable(R.drawable.bookmark_not_found)).noPlaceholder().into(holder.bookmarkIMG);

            holder.bookmarkAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View overflowButton = view;
                    overflowButton.setBackground(context.getResources().getDrawable(R.drawable.menu_overflow_focus));

                    PopupMenu popup = new PopupMenu(context, view);
                    popup.getMenuInflater().inflate(R.menu.bookmark_list_item,
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
                                    //Dissmiss the UNDO Snackbar and handle the deletion of the previously awaiting item yourself
                                    if (undoDeleteBookmarkSB != null && undoDeleteBookmarkSB.isShowing()) {
                                        //Careful about position that is passed from the adapter! This has to be accounted for again by using getItemAtPosition because there's an adview among the views
                                        dbHelper.deleteBookmark(tempBookmark.getId());
                                        itemPendingDeleteDecision = false;
                                        undoDeleteBookmarkSB.dismiss();
                                    }

                                    deleteCell(parentView, position);

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
                    ProgressBar imageProgressLoader = (ProgressBar) ((RelativeLayout) motherView.getChildAt(1)).getChildAt(0);
                    ImageView bookmarkIMG = (ImageView) ((RelativeLayout) motherView.getChildAt(1)).getChildAt(1);
                    TextView bookmarkName = (TextView) motherView.getChildAt(2);
                    Button bookmarkAction = (Button) motherView.getChildAt(3);
                    TextView bookmarkViews = (TextView) motherView.getChildAt(5);

                    int isNoteShowing = bookmarks.get(position).getIsNoteShowing();

                    LayerDrawable drawable = (LayerDrawable) ((LayerDrawable) motherView
                            .getBackground()).findDrawableByLayerId(R.id.content);
                    GradientDrawable gradient = (GradientDrawable) drawable.findDrawableByLayerId(R.id.innerView);

                    //Note was showing, hide
                    if (isNoteShowing == 1) {
                        view.setBackground(context.getResources().getDrawable(R.drawable.gray_bookmark));

                        gradient.setColor(context.getResources().getColor(R.color.white));

                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkNoteTV));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkAction));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkIMG));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkViews));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkName));
                        arrayListObjectAnimators.add(helperMethods.showViewElement(imageProgressLoader));

                        bookmarks.get(position).setIsNoteShowing(0);
                    } else {
                        view.setBackground(context.getResources().getDrawable(R.drawable.white_bookmark));

                        gradient.setColor(context.getResources().getColor(helperMethods.determineNoteViewBackground(book_color_code)));

                        bookmarkNoteTV.setText(bookmarks.get(position).getNote());

                        arrayListObjectAnimators.add(helperMethods.showViewElement(bookmarkNoteTV));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkAction));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkIMG));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkViews));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(bookmarkName));
                        arrayListObjectAnimators.add(helperMethods.hideViewElement(imageProgressLoader));

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

            return parentView;
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
        ProgressBar imageProgressLoader;
        boolean needInflate;
    }
}
