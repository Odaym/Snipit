package com.ttco.bookmarker.activities;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.Constants;
import com.ttco.bookmarker.classes.DatabaseHelper;
import com.ttco.bookmarker.classes.EventBus_Poster;
import com.ttco.bookmarker.classes.EventBus_Singleton;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SearchResults_Activity extends ListActivity {
    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;
    private SearchResults_Adapter searchResultsAdapter;

    private String query;
    private int book_color_code;
    private int book_id;
    private String book_title;
    private RelativeLayout emptyListLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        EventBus_Singleton.getInstance().register(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getString(R.string.search_results_activity_title));

        dbHelper = new DatabaseHelper(this);

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
            book_id = getIntent().getIntExtra(Constants.EXTRAS_BOOK_ID, -1);
            book_color_code = getIntent().getIntExtra(Constants.EXTRAS_BOOK_COLOR, -1);
            book_title = getIntent().getStringExtra(Constants.EXTRAS_BOOK_TITLE);
        }

        emptyListLayout = (RelativeLayout) findViewById(R.id.emptyListLayout);


        bookmarks = dbHelper.searchAllBookmarks(book_id, query);

        handleEmptyOrPopulatedScreen(bookmarks);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        if (ebp.getMessage().equals("bookmark_added")) {
            bookmarks = dbHelper.searchAllBookmarks(book_id, query);
            handleEmptyOrPopulatedScreen(bookmarks);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent intent = new Intent(SearchResults_Activity.this, View_Bookmark_Activity.class);
        intent.putExtra(Constants.EXTRAS_BOOK_ID, book_id);
        intent.putExtra(Constants.EXTRAS_BOOK_TITLE, book_title);
        intent.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, position);
        intent.putParcelableArrayListExtra("bookmarks", bookmarks);
        startActivity(intent);

        int bookmarkViews = dbHelper.getBookmarkViews(bookmarks.get(position));
        bookmarks.get(position).setViews(bookmarkViews + 1);
        dbHelper.updateBookmark(bookmarks.get(position));
        searchResultsAdapter.notifyDataSetChanged();

        EventBus_Singleton.getInstance().post(new EventBus_Poster("bookmark_viewed"));
    }

    public void handleEmptyOrPopulatedScreen(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            emptyListLayout.setVisibility(View.VISIBLE);
        } else {
            emptyListLayout.setVisibility(View.GONE);
        }

        searchResultsAdapter = new SearchResults_Adapter(this);
        setListAdapter(searchResultsAdapter);
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

            Picasso.with(SearchResults_Activity.this).load(new File(bookmarks.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(holder.bookmarkIMG);

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
                                    Intent editBookmarkIntent = new Intent(SearchResults_Activity.this, Create_Bookmark_Activity.class);
                                    editBookmarkIntent.putExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, Constants.EDIT_BOOKMARK_PURPOSE_VALUE);
                                    editBookmarkIntent.putExtra("bookmark", bookmarks.get(position));
                                    startActivity(editBookmarkIntent);
                                    break;
                                case R.id.delete:
                                    EventBus_Singleton.getInstance().post("bookmark_deleted");
                                    dbHelper.deleteBookmark(bookmarks.get(position).getId());
                                    bookmarks = dbHelper.searchAllBookmarks(book_id, query);
                                    handleEmptyOrPopulatedScreen(bookmarks);
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

        private class BookmarksViewHolder {
            TextView bookmarkName;
            TextView bookmarkPageNumber;
            ImageView bookmarkIMG;
            Button bookmarkAction;
            TextView bookmarkViews;
        }
    }
}
