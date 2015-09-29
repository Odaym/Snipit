package com.om.snipit.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.atomic.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.RoundedTransform;
import com.om.snipit.classes.Snippet;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.grantland.widget.AutofitTextView;

public class Snippets_Gallery_Activity extends Base_Activity {
    @InjectView(R.id.snippetsGridView)
    GridView snippetsGridView;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    private RuntimeExceptionDao<Snippet, Integer> bookmarkDAO;
    private List<Snippet> snippets;
    private Snippets_Adapter snippetsAdapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippets_gallery);

        ButterKnife.inject(this);

        Helper_Methods helperMethods = new Helper_Methods(this);

        EventBus_Singleton.getInstance().register(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.all_snippets_activity_title));
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.purple));

        if (helperMethods.getCurrentapiVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.darker_purple));
            toolbar.setElevation(25f);
        }

        bookmarkDAO = getHelper().getSnipitDAO();

        snippets = bookmarkDAO.queryForAll();
        snippetsAdapter = new Snippets_Adapter(this);
        snippetsGridView.setAdapter(snippetsAdapter);

        snippetsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent openViewSnippetActivity_Intent = new Intent(Snippets_Gallery_Activity.this, View_Snippet_Activity.class);
                openViewSnippetActivity_Intent.putExtra(Constants.EXTRAS_CURRENT_BOOKMARK_POSITION, position);
                openViewSnippetActivity_Intent.putExtra(Constants.EXTRAS_VIEWING_SNIPPETS_GALLERY, true);
                startActivity(openViewSnippetActivity_Intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus_Singleton.getInstance().unregister(this);
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }

    @Subscribe
    public void handle_BusEvents(EventBus_Poster ebp) {
        String ebpMessage = ebp.getMessage();

        switch (ebpMessage) {
            case "snippet_image_updated":
                Helper_Methods.delete_image_from_disk(ebp.getExtra());
                snippets = bookmarkDAO.queryForAll();
                snippetsAdapter.notifyDataSetChanged();
                break;
        }
    }

    public class Snippets_Adapter extends BaseAdapter {

        private LayoutInflater inflater;
        private SnippetsViewHolder holder;
        private Context context;
        private RoundedTransform roundedTransform;

        public Snippets_Adapter(Context context) {
            super();
            this.context = context;
            this.inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.roundedTransform = new RoundedTransform(context.getResources().getDimensionPixelSize(R.dimen.bookmark_image_shape_corners_radius), context.getResources().getDimensionPixelSize(R.dimen.bookmark_image_shape_corners_padding_bottom));
        }

        @Override
        public int getCount() {
            return snippets.size();
        }

        @Override
        public Object getItem(int i) {
            return snippets.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_gallery_snippet, parent, false);

                holder = new SnippetsViewHolder();

                holder.bookmarkName = (AutofitTextView) convertView.findViewById(R.id.bookmarkNameTV);
//                holder.bookmarkActionLayout = (RelativeLayout) parentView.findViewById(R.id.bookmarkActionLayout);
                holder.snippetIMG = (ImageView) convertView.findViewById(R.id.bookmarkIMG);
                holder.bookmarkPageNumber = (TextView) convertView.findViewById(R.id.bookmarkPageNumberTV);
//                holder.bookmarkNoteBTN = (Button) convertView.findViewById(R.id.bookmarkNoteBTN);
//                holder.bookmarkNoteTV = (AutofitTextView) convertView.findViewById(R.id.bookmarkNoteTV);
//                holder.motherView = (RelativeLayout) convertView.findViewById(R.id.list_item_snippet);
                holder.imageProgressLoader = (ProgressBar) convertView.findViewById(R.id.imageProgressLoader);

                convertView.setTag(holder);
            } else {
                holder = (SnippetsViewHolder) convertView.getTag();
            }

            holder.bookmarkName.setText(snippets.get(position).getName());

            Picasso.with(Snippets_Gallery_Activity.this).load(new File(snippets.get(position).getImage_path())).resize(context.getResources().getDimensionPixelSize(R.dimen.snippet_grid_item_picture_width), context.getResources().getDimensionPixelSize(R.dimen.snippet_grid_item_picture_height)).centerCrop().transform(roundedTransform).error(context.getResources().getDrawable(R.drawable.bookmark_not_found)).into(holder.snippetIMG);

            return convertView;
        }
    }

    public static class SnippetsViewHolder {
        RelativeLayout motherView;
        AutofitTextView bookmarkName;
        ImageView snippetIMG;
        RelativeLayout bookmarkActionLayout;
        TextView bookmarkPageNumber;
        Button bookmarkNoteBTN;
        AutofitTextView bookmarkNoteTV;
        ProgressBar imageProgressLoader;
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
}
