package com.om.snipit.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.andreabaccega.widget.FormEditText;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.om.snipit.R;
import com.om.snipit.classes.CircleTransform;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.GMailSender;
import com.om.snipit.classes.Helpers;
import com.om.snipit.dragsort_listview.DragSortListView;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
import com.om.snipit.repositories.impl.DatabaseBooksRepository;
import com.om.snipit.rest.ApiCallsHandler;
import com.om.snipit.rest.DefaultGetResponse;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.grantland.widget.AutofitTextView;
import net.frakbot.jumpingbeans.JumpingBeans;
import retrofit2.Call;
import retrofit2.Callback;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.om.snipit.classes.Constants.DEBUG_TAG;

  public class BooksActivity extends BaseActivity implements BooksActivityView {

  private static final String TAG = BooksActivity.class.getSimpleName();

  @Bind(R.id.booksList) DragSortListView listView;
  @Bind(R.id.emptyListLayout) RelativeLayout emptyListLayout;
  @Bind(R.id.createNewBookBTN) FloatingActionButton createNewBookBTN;
  @Bind(R.id.drawerLayout) DrawerLayout drawerLayout;
  @Bind(R.id.navDrawer) NavigationView navDrawer;
  View navDrawerheaderLayout;
  ImageView navdrawer_header_user_profile_image;
  TextView navdrawer_header_user_full_name;
  TextView navdrawer_header_user_email;
  @Bind(R.id.toolbar) Toolbar toolbar;

  private ProgressDialog sendEmailFeedbackDialog;

  private ActionBarDrawerToggle drawerToggle;

  private Books_Adapter booksAdapter;
  private List<Book> books;
  private Book tempBook;

  private QueryBuilder<Snippet, Integer> snippetQueryBuilder;
  private PreparedQuery<Snippet> pq;
  private PreparedQuery<Book> pqBook;

  private Snackbar undoDeleteBookSB;
  private boolean itemPendingDeleteDecision = false;

  private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
    @Override public void drop(int from, int to) {
      booksAdapter.notifyDataSetChanged();
    }
  };

  private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() {
    @Override public void drag(int from, int to) {
      booksAdapter.swap(from, to);
    }
  };
  private BooksActivityPresenter presenter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_books);

    ButterKnife.bind(this);

    setupToolbar(toolbar, null, false, Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS);

    navDrawerheaderLayout = navDrawer.inflateHeaderView(R.layout.navigation_drawer_header);

    navdrawer_header_user_email =
        (TextView) navDrawerheaderLayout.findViewById(R.id.navdrawer_header_user_email);
    navdrawer_header_user_profile_image =
        (ImageView) navDrawerheaderLayout.findViewById(R.id.navdrawer_header_user_profile_image);
    navdrawer_header_user_full_name =
        (TextView) navDrawerheaderLayout.findViewById(R.id.navdrawer_header_user_full_name);

    EventBus_Singleton.getInstance().register(this);

    presenter = new BooksActivityPresenter(this, new DatabaseBooksRepository(getApplication()));
    presenter.loadBooks();

    snippetQueryBuilder = snippetDAO.queryBuilder();

    drawerToggle =
        new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_closed) {
          public void onDrawerClosed(View view) {
            invalidateOptionsMenu();
          }

          public void onDrawerOpened(View drawerView) {
            invalidateOptionsMenu();
          }
        };

    drawerLayout.setDrawerListener(drawerToggle);
    drawerLayout.closeDrawer(GravityCompat.START);

    if (prefs.getBoolean(Constants.EXTRAS_USER_LOGGED_IN, false)) {
      String userPhoto = prefs.getString(Constants.EXTRAS_USER_DISPLAY_PHOTO, null);

      if (userPhoto == null) {
        Picasso.with(this)
            .load(R.drawable.ic_launcher)
            .fit()
            .into(navdrawer_header_user_profile_image);
      } else {
        Picasso.with(this)
            .load(userPhoto)
            .fit()
            .transform(new CircleTransform())
            .into(navdrawer_header_user_profile_image);
      }

      navdrawer_header_user_full_name.setText(prefs.getString(Constants.EXTRAS_USER_FULL_NAME, ""));
      navdrawer_header_user_email.setText(prefs.getString(Constants.EXTRAS_USER_EMAIL, ""));
    } else {
      Picasso.with(this)
          .load(R.drawable.ic_launcher)
          .fit()
          .into(navdrawer_header_user_profile_image);
      navdrawer_header_user_full_name.setText(R.string.app_name);
      navdrawer_header_user_email.setText(R.string.app_tagline);
    }

    navDrawer.setNavigationItemSelectedListener(
        new NavigationView.OnNavigationItemSelectedListener() {
          @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
              case R.id.navigation_drawer_item_settings:
                Intent openSettingsIntent = new Intent(BooksActivity.this, SettingsActivity.class);
                startActivity(openSettingsIntent);
                break;
              case R.id.navigation_drawer_item_send_feedback:
                AlertDialog.Builder alert = new AlertDialog.Builder(BooksActivity.this);

                LayoutInflater inflater = (LayoutInflater) BooksActivity.this.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
                View alertComposeFeedback =
                    inflater.inflate(R.layout.alert_send_feedback, null, false);

                final FormEditText inputFeedbackET =
                    (FormEditText) alertComposeFeedback.findViewById(R.id.feedbackET);
                inputFeedbackET.setHintTextColor(
                    BooksActivity.this.getResources().getColor(R.color.edittext_hint_color));
                inputFeedbackET.setSelection(inputFeedbackET.getText().length());

                alert.setPositiveButton(BooksActivity.this.getResources()
                        .getString(R.string.navdrawer_sendfeedback_button_title),
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                        if (Helpers.isInternetAvailable(BooksActivity.this)) {
                          if (inputFeedbackET.testValidity()) {
                            try {
                              new SendFeedbackEmail().execute(inputFeedbackET.getText().toString());
                            } catch (Exception e) {
                              Log.e("SendMail", e.getMessage(), e);
                            }
                          }
                        } else {
                          Crouton.makeText(BooksActivity.this,
                              getString(R.string.action_needs_internet), Style.ALERT).show();
                        }
                      }
                    });

                alert.setNegativeButton(
                    BooksActivity.this.getResources().getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                      }
                    });

                inputFeedbackET.postDelayed(new Runnable() {
                  @Override public void run() {
                    InputMethodManager keyboard =
                        (InputMethodManager) BooksActivity.this.getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(inputFeedbackET, 0);
                  }
                }, 0);

                TextView feedbackSendingSummaryTV =
                    (TextView) alertComposeFeedback.findViewById(R.id.feedbackSendingSummaryTV);
                feedbackSendingSummaryTV.append(" ");
                feedbackSendingSummaryTV.append(Html.fromHtml(
                    "<a href=\"mailto:snipit.me@gmail.com\">snipit.me@gmail.com</a>"));
                feedbackSendingSummaryTV.setMovementMethod(LinkMovementMethod.getInstance());

                alert.setTitle(BooksActivity.this.getResources()
                    .getString(R.string.navdrawer_sendfeedback_alert_title));
                alert.setView(alertComposeFeedback);
                alert.show();

                break;
              default:
                return true;
            }

            return true;
          }
        });

    createNewBookBTN.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Intent openCreateBookActivity = new Intent(BooksActivity.this, CreateBookActivity.class);
        startActivity(openCreateBookActivity);
      }
    });

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Intent openSnippetsForBook = new Intent(BooksActivity.this, SnippetsActivity.class);
        Book book = (Book) listView.getItemAtPosition(position);

        //Clicking on an adview when there's no Internet connection will cause this condition to be
        // satisfied because no Book will be found at the index of that adview
        if (book != null) {
          openSnippetsForBook.putExtra(Constants.EXTRAS_BOOK, book);
          startActivity(openSnippetsForBook);
        }
      }
    });
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    drawerToggle.syncState();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
        drawerLayout.closeDrawer(GravityCompat.START);
      } else {
        super.onBackPressed();
      }

      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Subscribe public void handle_BusEvents(EventBus_Poster ebp) {
    String ebpMessage = ebp.getMessage();

    switch (ebpMessage) {
      case "logged_out":
        finish();
        break;
      case "book_added":
        handleBusEvents_ListRefresher();
        Log.d("EVENTS", "book_added - Books_Activity");
        break;
      case "snippet_added_books_activity":
        handleBusEvents_ListRefresher();
        Log.d("EVENTS", "snippet_added_books_activity - Books_Activity");
        break;
      case "snippet_deleted_books_activity":
        handleBusEvents_ListRefresher();
        Log.d("EVENTS", "snippet_deleted_books_activity - Books_Activity");
        break;
    }
  }

  public void handleBusEvents_ListRefresher() {
    prepareForNotifyDataChanged();
    booksAdapter.notifyDataSetChanged();
  }

  public void prepareForNotifyDataChanged() {
    books = bookDAO.query(pqBook);
    books = bookDAO.query(pqBook);
    handleEmptyUI();
  }

  public void handleEmptyUI() {
    if (bookDAO.queryForAll().isEmpty()) {
      emptyListLayout.setVisibility(View.VISIBLE);
      JumpingBeans.with((TextView) emptyListLayout.findViewById(R.id.emptyLayoutMessageTV))
          .appendJumpingDots()
          .build();
    } else {
      emptyListLayout.setVisibility(View.INVISIBLE);
    }
  }

  public void showUndeleteDialog(final Book tempBookToDelete) {
    itemPendingDeleteDecision = true;

    tempBook = tempBookToDelete;

    Spannable sentenceToSpan = new SpannableString(
        getResources().getString(R.string.delete_book_confirmation_message) + " " + tempBookToDelete
            .getTitle());

    sentenceToSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 7,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
              @Override public void onShow(Snackbar snackbar) {
              }

              @Override public void onShowByReplace(Snackbar snackbar) {
              }

              @Override public void onShown(Snackbar snackbar) {
              }

              @Override public void onDismiss(Snackbar snackbar) {
                if (itemPendingDeleteDecision) {
                  finalizeBookDeletion(tempBookToDelete);
                }
              }

              @Override public void onDismissByReplace(Snackbar snackbar) {
              }

              @Override public void onDismissed(Snackbar snackbar) {
              }
            })
            .actionListener(new ActionClickListener() {
              @Override public void onActionClicked(Snackbar snackbar) {
                prepareForNotifyDataChanged();
                booksAdapter.notifyDataSetChanged();

                itemPendingDeleteDecision = false;
                undoDeleteBookSB.dismiss();
              }
            });

    undoDeleteBookSB.show(BooksActivity.this);
  }

  public void finalizeBookDeletion(Book tempBook) {
    snippetDAO.delete(snippetDAO.queryForEq("book_id", tempBook.getId()));
    bookDAO.delete(tempBook);
    prepareForNotifyDataChanged();
    booksAdapter.notifyDataSetChanged();
    itemPendingDeleteDecision = false;
  }

  @Override protected void onPause() {
    super.onPause();

    if (itemPendingDeleteDecision) {

      finalizeBookDeletion(tempBook);

      if (undoDeleteBookSB.isShowing()) {
        undoDeleteBookSB.dismiss();
      }
    }
  }

    @Override
    protected void onStop() {
      super.onStop();
      presenter.unsubscribe();
    }

    @Override protected void onDestroy() {
    super.onDestroy();
    EventBus_Singleton.getInstance().unregister(this);
  }

  @Override public void displayBooks(List<Book> bookList) {
    Log.d(TAG, "displayBooks: found some books");
    this.books = bookList;
    emptyListLayout.setVisibility(View.INVISIBLE);
    booksAdapter = new Books_Adapter(this);

    listView.setDropListener(onDrop);
    listView.setDragListener(onDrag);
    listView.setAdapter(booksAdapter);
  }

  @Override public void displayNoBooks() {
    Log.d(TAG, "displayBooks: found NO books");
    emptyListLayout.setVisibility(View.VISIBLE);
    JumpingBeans.with((TextView) emptyListLayout.findViewById(R.id.emptyLayoutMessageTV))
            .appendJumpingDots()
            .build();
  }

    @Override
    public void displayError() {
      Toast.makeText(this, "Error accessing data", Toast.LENGTH_SHORT).show();
    }

    public static class BooksViewHolder {
    RelativeLayout list_item_book;
    TextView bookDateAddedTV;
    AutofitTextView bookTitleTV;
    AutofitTextView bookAuthorTV;
    ImageView bookThumbIMG;
    TextView snippetsNumberTV;
    LinearLayout bookActionLayout;
    boolean needInflate;
  }

  private class SendFeedbackEmail extends AsyncTask<String, Void, Void> {
    @Override protected void onPreExecute() {
      super.onPreExecute();
      sendEmailFeedbackDialog = ProgressDialog.show(BooksActivity.this,
          getResources().getString(R.string.loading_book_info_title),
          getResources().getString(R.string.loading_sending_feedback), true);
    }

    @Override protected Void doInBackground(String... feedbackContent) {
      try {
        GMailSender sender = new GMailSender(Constants.FEEDBACK_EMAIL_FROM_ADDRESS,
            Constants.FEEDBACK_EMAIL_FROM_ADDRESS_PASSWORD);
        sender.sendMail(Constants.FEEDBACK_EMAIL_SUBJECT, feedbackContent[0],
            Constants.FEEDBACK_EMAIL_APPEARS_AS, Constants.FEEDBACK_EMAIL_TO);
      } catch (Exception e) {
        e.printStackTrace();
      }

      return null;
    }

    @Override protected void onPostExecute(Void nothing) {
      sendEmailFeedbackDialog.hide();
      Crouton.makeText(BooksActivity.this, getResources().getString(R.string.feedback_sent_alert),
          Style.CONFIRM).show();
    }
  }

  public class Books_Adapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Context context;
    private BooksViewHolder holder;
    private List<Snippet> snippets;
    private ProgressDialog uploadingSnippets_AWS;

    public Books_Adapter(Context context) {
      super();
      this.context = context;

      this.inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
    }

    @Override public int getCount() {
      return books.size();
    }

    @Override public Object getItem(int i) {
      return books.get(i);
    }

    @Override public long getItemId(int i) {
      return 0;
    }

    @Override public View getView(final int position, View convertView, final ViewGroup parent) {
      final View parentView;

      if (convertView == null || ((BooksViewHolder) convertView.getTag()).needInflate) {
        parentView = inflater.inflate(R.layout.list_item_book, parent, false);

        holder = new BooksViewHolder();

        holder.list_item_book = (RelativeLayout) parentView.findViewById(R.id.list_item_book);
        holder.bookDateAddedTV = (TextView) parentView.findViewById(R.id.bookDateAddedTV);
        holder.bookTitleTV = (AutofitTextView) parentView.findViewById(R.id.bookTitleTV);
        holder.bookAuthorTV = (AutofitTextView) parentView.findViewById(R.id.bookAuthorTV);
        holder.bookThumbIMG = (ImageView) parentView.findViewById(R.id.bookThumbIMG);
        holder.snippetsNumberTV = (TextView) parentView.findViewById(R.id.snippetsNumberTV);
        holder.bookActionLayout = (LinearLayout) parentView.findViewById(R.id.bookActionLayout);
        holder.needInflate = false;

        parentView.setTag(holder);
      } else {
        parentView = convertView;
      }

      holder = (BooksViewHolder) parentView.getTag();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        holder.snippetsNumberTV.setElevation(5f);
      }

      holder.bookTitleTV.setText(books.get(position).getTitle());
      holder.bookAuthorTV.setText(books.get(position).getAuthor());

      //            Picasso.with(Books_Activity.this).load(books.get(position).getImagePath()).error(getResources().getDrawable(R.drawable.notfound_1)).into(holder.bookThumbIMG);
      Picasso.with(BooksActivity.this)
          .load(books.get(position).getImagePath())
          .into(holder.bookThumbIMG);

      String[] bookDateAdded = books.get(position).getDate_added().split(" ");
      holder.bookDateAddedTV.setText(
          bookDateAdded[0] + " " + bookDateAdded[1] + ", " + bookDateAdded[2]);

      switch (books.get(position).getColorCode()) {
        case 0:
          holder.snippetsNumberTV.setBackgroundDrawable(
              context.getResources().getDrawable(R.drawable.snippet_pink));
          break;
        case 1:
          holder.snippetsNumberTV.setBackgroundDrawable(
              context.getResources().getDrawable(R.drawable.snippet_red));
          break;
        case 2:
          holder.snippetsNumberTV.setBackgroundDrawable(
              context.getResources().getDrawable(R.drawable.snippet_purple));
          break;
        case 3:
          holder.snippetsNumberTV.setBackgroundDrawable(
              context.getResources().getDrawable(R.drawable.snippet_yellow));
          break;
        case 4:
          holder.snippetsNumberTV.setBackgroundDrawable(
              context.getResources().getDrawable(R.drawable.snippet_blue));
          break;
        case 5:
          holder.snippetsNumberTV.setBackgroundDrawable(
              context.getResources().getDrawable(R.drawable.snippet_brown));
          break;
      }

      snippets = snippetDAO.queryForEq("book_id", books.get(position).getId());
      holder.snippetsNumberTV.setText(snippets.size() + "");

      holder.bookActionLayout.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          final View overflowButton = view.findViewById(R.id.bookAction);
          overflowButton.findViewById(R.id.bookAction)
              .setBackgroundDrawable(
                  context.getResources().getDrawable(R.drawable.menu_overflow_focus));

          PopupMenu popup = new PopupMenu(context, view);
          popup.getMenuInflater().inflate(R.menu.book_list_item, popup.getMenu());
          for (int i = 0; i < popup.getMenu().size(); i++) {
            MenuItem item = popup.getMenu().getItem(i);
            SpannableString spanString = new SpannableString(item.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
            item.setTitle(spanString);
          }
          popup.show();
          popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {

              switch (item.getItemId()) {
                case R.id.share:
                  uploadSnippetsToAWS(snippetDAO.queryForEq("book_id", books.get(position).getId()),
                      position);
                  break;
                case R.id.edit:
                  Intent editBookIntent = new Intent(BooksActivity.this, CreateBookActivity.class);
                  editBookIntent.putExtra(Constants.EXTRAS_BOOK, books.get(position));
                  editBookIntent.putExtra(Constants.EDIT_BOOK_PURPOSE_STRING,
                      Constants.EDIT_BOOK_PURPOSE_VALUE);
                  startActivity(editBookIntent);
                  break;
                case R.id.delete:
                  //Dissmiss the UNDO Snackbar and handle the deletion of the previously awaiting item yourself
                  if (undoDeleteBookSB != null && undoDeleteBookSB.isShowing()) {
                    //Careful about position that is passed from the adapter! This has to be accounted for again by using getItemAtPosition because there's an adview among the views
                    //I am able to use tempBook here because I am certain that it would have now been initialized inside deleteCell(), no way to reach this point without having been through deleteCell() first

                    try {
                      snippetQueryBuilder.where().eq("book_id", tempBook.getId());
                      pq = snippetQueryBuilder.prepare();
                      snippetDAO.delete(snippetDAO.query(pq));
                    } catch (SQLException e) {
                      e.printStackTrace();
                    }

                    bookDAO.delete(tempBook);
                    itemPendingDeleteDecision = false;
                    undoDeleteBookSB.dismiss();
                  }

                  try {
                    showUndeleteDialog(books.get(position));
                  } catch (IndexOutOfBoundsException e) {
                    Log.d(DEBUG_TAG, "Error: " + e.getMessage());
                  }

                  break;
              }

              return true;
            }
          });
          popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override public void onDismiss(PopupMenu popupMenu) {
              overflowButton.findViewById(R.id.bookAction)
                  .setBackgroundDrawable(
                      context.getResources().getDrawable(R.drawable.menu_overflow_fade));
            }
          });
        }
      });

      return parentView;
    }

    public void uploadSnippetsToAWS(final List<Snippet> snippetsForBook, final int position) {
      final List<String> snippetImagePathsAWS = new ArrayList<>();

      double totalSnippetsFileSize = 0;

      for (Snippet snip : snippetsForBook) {
        File file = new File(snip.getImage_path());
        totalSnippetsFileSize += file.length();
      }

      uploadingSnippets_AWS = new ProgressDialog(BooksActivity.this);
      uploadingSnippets_AWS.setMessage("Uploading Book images to AWS");
      uploadingSnippets_AWS.setMax((int) Math.ceil(totalSnippetsFileSize / 1000));
      uploadingSnippets_AWS.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      uploadingSnippets_AWS.setCancelable(false);
      uploadingSnippets_AWS.show();

      final AmazonS3Client s3Client = new AmazonS3Client(
          new BasicAWSCredentials(Constants.AMAZON_ACCESS_KEY, Constants.AMAZON_SECRET_ACCESS_KEY));

      Thread t = new Thread(new Runnable() {
        @Override public void run() {

          for (Snippet snip : snippetsForBook) {

            final String snippetTempFilename = new BigInteger(130, new SecureRandom()).toString(32);

            PutObjectRequest por = new PutObjectRequest("snippet-images", snippetTempFilename,
                new File(snip.getImage_path()));

            por.setGeneralProgressListener(new ProgressListener() {
              @Override
              public void progressChanged(com.amazonaws.event.ProgressEvent progressEvent) {

                uploadingSnippets_AWS.setProgress((uploadingSnippets_AWS.getProgress()
                    + (int) (progressEvent.getBytesTransferred() / 1000)));

                if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                  uploadingSnippets_AWS.setProgress(uploadingSnippets_AWS.getProgress() + 1);

                  snippetImagePathsAWS.add(Constants.AMAZON_IMAGE_BUCKET_URL + snippetTempFilename);

                  Log.d("AWS_IMAGES",
                      "DONE : " + (Constants.AMAZON_IMAGE_BUCKET_URL + snippetTempFilename));
                }
              }
            });

            try {
              s3Client.putObject(por);

              uploadingSnippets_AWS.dismiss();

              prepareBooksForUpload(position, snippetsForBook, snippetImagePathsAWS);
            } catch (AmazonClientException e) {
              runOnUiThread(new Runnable() {
                @Override public void run() {
                  uploadingSnippets_AWS.dismiss();
                }
              });
            }
          }
        }
      });

      t.start();
    }

    public void prepareBooksForUpload(int position, List<Snippet> snippetsForBook,
        List<String> snippetImagePathsAWS) {

      String userEmail = prefs.getString(Constants.EXTRAS_USER_EMAIL, null);
      String userFullName = prefs.getString(Constants.EXTRAS_USER_FULL_NAME, null);

      ApiCallsHandler.UserObject userObj = new ApiCallsHandler.UserObject(userEmail, userFullName);

      String book_title = books.get(position).getTitle();

      List<ApiCallsHandler.SnippetObject> snippetObjects = new ArrayList<>();

      for (int i = 0; i < snippetImagePathsAWS.size(); i++) {
        snippetObjects.add(new ApiCallsHandler.SnippetObject(snippetsForBook.get(i).getName(),
            snippetsForBook.get(i).getPage_number(), snippetsForBook.get(i).getNote(),
            snippetsForBook.get(i).getOcr_content(), snippetImagePathsAWS.get(i)));
      }

      uploadBook(userObj, book_title, snippetObjects);
    }

    private void uploadBook(ApiCallsHandler.UserObject userObj, String book_title,
        List<ApiCallsHandler.SnippetObject> snippetObjs) {
      ApiCallsHandler.uploadBook(userObj, book_title, snippetObjs,
          new Callback<DefaultGetResponse>() {
            @Override public void onResponse(Call<DefaultGetResponse> call,
                retrofit2.Response<DefaultGetResponse> response) {

              if (response.isSuccessful()) Log.d(DEBUG_TAG, "onResponse: I guess it succeeded!");
            }

            @Override public void onFailure(Call<DefaultGetResponse> call, Throwable t) {
              Log.d(DEBUG_TAG, "onFailure: it fucking failed!");
            }
          });
    }

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
}
