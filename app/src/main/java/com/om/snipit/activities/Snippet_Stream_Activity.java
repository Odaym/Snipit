package com.om.snipit.activities;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.om.snipit.R;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.Bind;
import me.grantland.widget.AutofitTextView;

public class Snippet_Stream_Activity extends Base_Activity {

    @Bind(R.id.snippetsStreamList)
    RecyclerView snippetsStreamList;
    @Bind(R.id.noConnectionlayout)
    RelativeLayout noConnectionLayout;
    @Bind(R.id.contentViewSwipeRefresh)
    SwipeRefreshLayout contentViewSwipeRefresh;
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private LinearLayoutManager mLayoutManager;
    private Snippets_Adapter snippetsAdapter;
    protected Handler handler;

    private Date lastRefreshDate;

    private int pagesLoadedSoFar = 1;
    private int pageItemsLimit = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippets_stream);

        ButterKnife.bind(this);

        EventBus_Singleton.getInstance().register(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        toolbar.setTitle(R.string.snippets_stream_activity_title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(25f);
        }

        setSupportActionBar(toolbar);
        helperMethods.setUpActionbarColors(this, -1);

        contentViewSwipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                contentViewSwipeRefresh.setRefreshing(true);
            }
        });

        contentViewSwipeRefresh.setColorSchemeColors(getResources().getColor(R.color.red), getResources().getColor(R.color.green), getResources().getColor(R.color.blue), getResources().getColor(R.color.yellow));

        handler = new Handler();

        initializeRecyclerViewLayoutManager();

        if (Helper_Methods.isInternetAvailable(this)) {
            fetchSnippetsFromParse();
        } else {
            fetchSnippetsFromLocalDataStore();
        }
    }

    public void initializeRecyclerViewLayoutManager() {
        mLayoutManager = new LinearLayoutManager(this);
        snippetsStreamList.setLayoutManager(mLayoutManager);
        snippetsStreamList.setHasFixedSize(true);
    }

    public void fetchSnippetsFromParse() {
        ParseQuery<ParseObject> querySharedSnippets = new ParseQuery<>("Shared_Snippet");
        querySharedSnippets.setLimit(pageItemsLimit);
        querySharedSnippets.addDescendingOrder("createdAt");
        querySharedSnippets.findInBackground(new FindCallback<ParseObject>() {
            public void done(final List<ParseObject> foundSnippets, ParseException e) {
                if (e == null) {
                    ParseObject.pinAllInBackground(foundSnippets);

                    lastRefreshDate = new Date(new SimpleDateFormat("MMM dd, yyyy, HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime()));

                    contentViewSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            refreshSnippetsFromParse(foundSnippets);
                        }
                    });

                    contentViewSwipeRefresh.setRefreshing(false);

                    snippetsAdapter = new Snippets_Adapter(Snippet_Stream_Activity.this, foundSnippets, snippetsStreamList);
                    snippetsStreamList.setAdapter(snippetsAdapter);
                    snippetsAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                        @Override
                        public void onLoadMore() {
                            //add null , so the adapter will check view_type and show progress bar at bottom
                            foundSnippets.add(null);
                            snippetsAdapter.notifyItemInserted(foundSnippets.size() - 1);

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    foundSnippets.remove(foundSnippets.size() - 1);
                                    snippetsAdapter.notifyItemRemoved(foundSnippets.size());

                                    loadMoreSnippetsFromParse(foundSnippets);
                                }
                            }, 1500);
                        }
                    });
                } else {
                    // handle Parse Exception here
                }
            }
        });
    }

    public void refreshSnippetsFromParse(final List<ParseObject> currentSnippets) {
        ParseQuery<ParseObject> querySharedSnippets = new ParseQuery<>("Shared_Snippet");
        querySharedSnippets.whereGreaterThan("createdAt", lastRefreshDate).addDescendingOrder("createdAt");
        querySharedSnippets.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> foundSnippets, ParseException e) {
                if (e == null) {
                    lastRefreshDate = new Date(new SimpleDateFormat("MMM dd, yyyy, HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime()));

                    for (ParseObject parseObj : foundSnippets) {
                        currentSnippets.add(0, parseObj);
                        snippetsAdapter.notifyItemInserted(0);
                        snippetsStreamList.scrollToPosition(0);
                    }
                    contentViewSwipeRefresh.setRefreshing(false);
                } else {
                    // handle Parse Exception here
                }
            }
        });
    }

    public void loadMoreSnippetsFromParse(final List<ParseObject> currentSnippets) {
        ParseQuery<ParseObject> querySharedSnippets = new ParseQuery<>("Shared_Snippet");
        querySharedSnippets.setLimit(pageItemsLimit);
        querySharedSnippets.setSkip(pagesLoadedSoFar * pageItemsLimit);
        querySharedSnippets.addDescendingOrder("createdAt");
        querySharedSnippets.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> foundSnippets, ParseException e) {
                if (e == null) {
                    if (!foundSnippets.isEmpty()) {
                        currentSnippets.addAll(foundSnippets);
                        snippetsAdapter.notifyDataSetChanged();
                        snippetsAdapter.setLoaded();
                        snippetsStreamList.smoothScrollToPosition(currentSnippets.size() - 3);
                    }
                } else {
                    // handle Parse Exception here
                }
            }
        });

        pagesLoadedSoFar++;
    }

    public void fetchSnippetsFromLocalDataStore() {
        ParseQuery<ParseObject> querySharedSnippets = new ParseQuery<>("Shared_Snippet");
        querySharedSnippets.fromLocalDatastore();
        querySharedSnippets.setLimit(pageItemsLimit);
        querySharedSnippets.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> foundSnippets, ParseException e) {
                if (e == null) {
                    if (foundSnippets.isEmpty()) {
                        noConnectionLayout.setVisibility(View.VISIBLE);
                    } else {
                        snippetsAdapter = new Snippets_Adapter(Snippet_Stream_Activity.this, foundSnippets, snippetsStreamList);
                        snippetsStreamList.setAdapter(snippetsAdapter);

                    }
                } else {
                    // handle Parse Exception here
                }
            }
        });
    }

    public class Snippets_Adapter extends RecyclerView.Adapter {

        private Context context;
        private PicassoImageLoadedCallback picassoImageLoadedCallback;
        private List<ParseObject> snippets;
        private int lastPosition = -1;

        private OnLoadMoreListener onLoadMoreListener;
        private int visibleThreshold = 5;
        private int lastVisibleItem, totalItemCount;
        private boolean loading;

        private final int VIEW_ITEM = 1;
        private final int VIEW_PROG = 0;

        private int[] loadMoreSnippetsMessages = new int[]{R.string.load_more_snippets_message_1, R.string.load_more_snippets_message_2, R.string.load_more_snippets_message_3, R.string.load_more_snippets_message_4, R.string.load_more_snippets_message_5, R.string.load_more_snippets_message_6, R.string.load_more_snippets_message_7, R.string.load_more_snippets_message_8, R.string.load_more_snippets_message_9, R.string.load_more_snippets_message_10};

        Random randomNumber = new Random();

        public class SnippetsViewHolder extends RecyclerView.ViewHolder {
            public CardView list_item_snippet;
            public AutofitTextView snippetName;
            public AutofitTextView bookTitleTV;
            public AutofitTextView bookAuthorTV;
            public ImageView snippetIMG;
            public TextView snippetByScreenNameTV;
            public ProgressBar imageProgressLoader;

            public SnippetsViewHolder(View itemView) {
                super(itemView);

                list_item_snippet = (CardView) itemView.findViewById(R.id.list_item_snippet);
                snippetName = (AutofitTextView) itemView.findViewById(R.id.snippetNameTV);
                bookTitleTV = (AutofitTextView) itemView.findViewById(R.id.bookTitleTV);
                bookAuthorTV = (AutofitTextView) itemView.findViewById(R.id.bookAuthorTV);
                snippetIMG = (ImageView) itemView.findViewById(R.id.snippetIMG);
                snippetByScreenNameTV = (TextView) itemView.findViewById(R.id.snippetByScreenNameTV);

                imageProgressLoader = (ProgressBar) itemView.findViewById(R.id.imageProgressLoader);
            }
        }

        public class ProgressViewHolder extends RecyclerView.ViewHolder {
            public ProgressBar progressBar;
            public AutofitTextView snippetsLoadMoreTV;

            public ProgressViewHolder(View itemView) {
                super(itemView);
                progressBar = (ProgressBar) itemView.findViewById(R.id.snippetsLoadMoreBar);
                snippetsLoadMoreTV = (AutofitTextView) itemView.findViewById(R.id.snippetsLoadMoreTV);
            }
        }

        public Snippets_Adapter(Context context, List<ParseObject> snippets, RecyclerView snippetsStream) {
            this.context = context;
            this.snippets = snippets;

            snippetsStream.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView,
                                       int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = mLayoutManager.getItemCount();
                    lastVisibleItem = mLayoutManager
                            .findLastVisibleItemPosition();
                    if (!loading
                            && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (onLoadMoreListener != null) {
                            onLoadMoreListener.onLoadMore();
                        }
                        loading = true;
                    }
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder vh = null;
            View view;

            switch (viewType) {
                case VIEW_ITEM:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_snippets_stream, parent, false);
                    vh = new SnippetsViewHolder(view);
                    break;
                case VIEW_PROG:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_snippets_stream_load_more, parent, false);
                    vh = new ProgressViewHolder(view);
                    break;
            }

            return vh;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SnippetsViewHolder) {
                ((SnippetsViewHolder) holder).snippetName.setText(snippets.get(position).getString("name"));

                SpannableStringBuilder screenNameText = new SpannableStringBuilder(getString(R.string.shared_by_label) + " " + snippets.get(position).getString("screen_name"));
                screenNameText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 11, screenNameText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                ((SnippetsViewHolder) holder).snippetByScreenNameTV.setText(screenNameText);

                SpannableStringBuilder bookTitleText = new SpannableStringBuilder(getString(R.string.book_title_label) + " " + snippets.get(position).getString("book_title"));
                bookTitleText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 6, bookTitleText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                ((SnippetsViewHolder) holder).bookTitleTV.setText(bookTitleText);

                SpannableStringBuilder bookAuthorText = new SpannableStringBuilder(getString(R.string.book_author_label) + " " + snippets.get(position).getString("book_author"));
                bookAuthorText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 7, bookAuthorText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                ((SnippetsViewHolder) holder).bookAuthorTV.setText(bookAuthorText);

                Picasso.with(Snippet_Stream_Activity.this).load(snippets.get(position).getString("aws_image_path")).error(context.getResources().getDrawable(R.drawable.snippet_not_found)).fit().into(((SnippetsViewHolder) holder).snippetIMG, picassoImageLoadedCallback);

                this.picassoImageLoadedCallback = new PicassoImageLoadedCallback(((SnippetsViewHolder) holder).imageProgressLoader) {
                    @Override
                    public void onSuccess() {
                        if (((SnippetsViewHolder) holder).imageProgressLoader != null) {
                            ((SnippetsViewHolder) holder).imageProgressLoader.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError() {
                        if (((SnippetsViewHolder) holder).imageProgressLoader != null) {
                            ((SnippetsViewHolder) holder).imageProgressLoader.setVisibility(View.GONE);
                        }
                    }
                };

                setAnimation(((SnippetsViewHolder) holder).list_item_snippet, position);
            } else {
                ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
                ((ProgressViewHolder) holder).snippetsLoadMoreTV.setText(getString(loadMoreSnippetsMessages[randomNumber.nextInt(10)]));
            }
        }

        private void setAnimation(View viewToAnimate, int position) {
            if (position > lastPosition) {
                Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
                viewToAnimate.startAnimation(animation);
                lastPosition = position;
            }
        }

        @Override
        public int getItemCount() {
            return snippets.size();
        }

        @Override
        public int getItemViewType(int position) {
            return snippets.get(position) != null ? VIEW_ITEM : VIEW_PROG;
        }

        public void setLoaded() {
            loading = false;
        }

        public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
            this.onLoadMoreListener = onLoadMoreListener;
        }
    }

    private class PicassoImageLoadedCallback implements Callback {
        ProgressBar progressBar;

        public PicassoImageLoadedCallback(ProgressBar progBar) {
            progressBar = progBar;
        }

        @Override
        public void onSuccess() {

        }

        @Override
        public void onError() {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus_Singleton.getInstance().unregister(this);
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }
}
