package com.ttco.bookmarker.activities;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import com.ttco.bookmarker.R;
import com.ttco.bookmarker.classes.Bookmark;
import com.ttco.bookmarker.classes.DatabaseHelper;

import java.util.ArrayList;

public class SearchResults_Activity extends ListActivity {
    private ArrayList<Bookmark> bookmarks;
    private DatabaseHelper dbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
    }

//    private class SearchResults_Adapter extends BaseAdapter {
//
//        private LayoutInflater inflater;
//        private Context context;
//        private BookmarksViewHolder holder;
//        private DatabaseHelper dbHelper;
//
//        private String bookmarkDeletedIntent_String = "com.ttco.bookmarker.bookmarkDeleted";
//
//        public Bookmarks_Adapter(Context context) {
//            super();
//            this.context = context;
//
//            dbHelper = new DatabaseHelper(context);
//        }
//
//        @Override
//        public int getCount() {
//            return bookmarks.size();
//        }
//
//        @Override
//        public Object getItem(int i) {
//            return bookmarks.get(i);
//        }
//
//        @Override
//        public long getItemId(int i) {
//            return 0;
//        }
//
//        @Override
//        public View getView(final int position, View convertView, final ViewGroup parent) {
//            if (convertView == null) {
//                inflater = (LayoutInflater) context
//                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                convertView = inflater.inflate(R.layout.list_item_bookmark, parent, false);
//
//                holder = new BookmarksViewHolder();
//
//                holder.bookmarkName = (TextView) convertView.findViewById(R.id.bookmarkNameTV);
//                holder.bookmarkPageNumber = (TextView) convertView.findViewById(R.id.bookmarkPageNumberTV);
//                holder.bookmarkAction = (Button) convertView.findViewById(R.id.bookmarkAction);
//                holder.bookmarkIMG = (ImageView) convertView.findViewById(R.id.bookmarkIMG);
//                holder.bookmarkViews = (TextView) convertView.findViewById(R.id.bookmarkViewsTV);
//
//                convertView.setTag(holder);
//            } else {
//                holder = (BookmarksViewHolder) convertView.getTag();
//            }
//
//            holder.bookmarkName.setText(bookmarks.get(position).getName());
//
//            String pageNumber = bookmarks.get(position).getPage_number() + "";
//            short pageNumberShort = Short.parseShort(pageNumber);
//            DecimalFormat formatter = new DecimalFormat("#,###");
//            holder.bookmarkPageNumber.setText(formatter.format(pageNumberShort));
//
//            holder.bookmarkViews.setText("Views: " + bookmarks.get(position).getViews());
//
////            switch (book_color_code) {
////                case 0:
////                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.pink));
////                    break;
////                case 1:
////                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.red));
////                    break;
////                case 2:
////                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.purple));
////                    break;
////                case 3:
////                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.yellow));
////                    break;
////                case 4:
////                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.blue));
////                    break;
////                case 5:
////                    holder.bookmarkPageNumber.setTextColor(context.getResources().getColor(R.color.brown));
////                    break;
////            }
//
//            Picasso.with(SearchResults_Activity.this).load(new File(bookmarks.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_width), context.getResources().getDimensionPixelSize(R.dimen.bookmark_thumb_height)).centerCrop().error(getResources().getDrawable(R.drawable.sad_image_not_found)).into(holder.bookmarkIMG);
//
//            holder.bookmarkAction.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    final View overflowButton = view;
//                    overflowButton.setBackground(getDrawable(R.drawable.menu_overflow_focus));
//
//                    PopupMenu popup = new PopupMenu(context, view);
//                    popup.getMenuInflater().inflate(R.menu.bookmark_edit_delete,
//                            popup.getMenu());
//                    for (int i = 0; i < popup.getMenu().size(); i++) {
//                        MenuItem item = popup.getMenu().getItem(i);
//                        SpannableString spanString = new SpannableString(item.getTitle().toString());
//                        spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
//                        item.setTitle(spanString);
//                    }
//                    popup.show();
//                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//                        @Override
//                        public boolean onMenuItemClick(MenuItem item) {
//
//                            switch (item.getItemId()) {
//                                case R.id.edit:
//                                    Intent editBookmarkIntent = new Intent(SearchResults_Activity.this, Create_Bookmark_Activity.class);
//                                    editBookmarkIntent.putExtra(Constants.EDIT_BOOKMARK_PURPOSE_STRING, Constants.EDIT_BOOKMARK_PURPOSE_VALUE);
//                                    editBookmarkIntent.putExtra("bookmark", bookmarks.get(position));
//                                    startActivity(editBookmarkIntent);
//                                    break;
//                                case R.id.delete:
//                                    Intent bookmarkDeletedIntent = new Intent();
//                                    bookmarkDeletedIntent.setAction(bookmarkDeletedIntent_String);
//                                    context.sendBroadcast(bookmarkDeletedIntent);
//                                    dbHelper.deleteBookmark(bookmarks.get(position).getId());
//                                    bookmarks = dbHelper.getAllBookmarks(bookmarks.get(position).getBookId(), null);
//                                    notifyDataSetChanged();
//                                    break;
//                            }
//
//                            return true;
//                        }
//                    });
//                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
//                        @Override
//                        public void onDismiss(PopupMenu popupMenu) {
//                            overflowButton.setBackground(getDrawable(R.drawable.menu_overflow_fade));
//                        }
//                    });
//                }
//            });
//            return convertView;
//        }
//
//        private class BookmarksViewHolder {
//            TextView bookmarkName;
//            TextView bookmarkPageNumber;
//            ImageView bookmarkIMG;
//            Button bookmarkAction;
//            TextView bookmarkViews;
//        }
//    }
}
