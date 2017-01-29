package com.om.snipit.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.FrameLayout;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DefaultIndicatorController;
import com.om.snipit.classes.Helpers;
import com.om.snipit.models.User;

public class LoginActivity extends BaseActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private static final int RC_SIGN_IN = 0;
  @Bind(R.id.viewpager) ViewPager viewPager;
  @Bind(R.id.signInGoogleBTN) SignInButton signInGoogleBTN;
  private DefaultIndicatorController indicatorController;
  private GoogleApiClient googleApiClient;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_login);

    ButterKnife.bind(this);

    GoogleSignInOptions gso =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

    googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this)
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
        .build();

    viewPager.setAdapter(new IntroAdapter(getSupportFragmentManager()));

    signInGoogleBTN.setSize(SignInButton.SIZE_WIDE);
    signInGoogleBTN.setColorScheme(SignInButton.COLOR_DARK);

    signInGoogleBTN.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        signInGoogle();
      }
    });

    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override public void onPageSelected(int position) {
        indicatorController.selectPosition(position);
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });

    initController();
  }

  private void signInGoogle() {
    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == RC_SIGN_IN) {
      GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      handleSignInResult(result);
    }
  }

  private void handleSignInResult(GoogleSignInResult result) {
    if (result.isSuccess()) {
      if (Helpers.isInternetAvailable(this)) {
        ProgressDialog loggingInDialog = ProgressDialog.show(LoginActivity.this, "",
            getResources().getString(R.string.logging_in_dialog_message), true);

        GoogleSignInAccount acct = result.getSignInAccount();

        if (acct != null) {
          User user = new User();

          user.setFull_name(acct.getDisplayName());
          user.setEmail_address(acct.getEmail());
          if (acct.getPhotoUrl() == null) {
            user.setPhoto_url(null);
          } else {
            user.setPhoto_url(acct.getPhotoUrl().toString());
          }

          prefsEditor.putBoolean(Constants.EXTRAS_USER_LOGGED_IN, true);
          prefsEditor.putString(Constants.EXTRAS_USER_FULL_NAME, user.getFull_name());
          prefsEditor.putString(Constants.EXTRAS_USER_DISPLAY_PHOTO, user.getPhoto_url());
          prefsEditor.putString(Constants.EXTRAS_USER_EMAIL, user.getEmail_address());
          prefsEditor.apply();

          Intent openBooksActivity = new Intent(LoginActivity.this, BooksActivity.class);
          startActivity(openBooksActivity);

          finish();
        }
      } else {
        new AlertDialog.Builder(LoginActivity.this).setMessage(R.string.no_internet_connection)
            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            })
            .show();
      }
    } else {
      // Signed out, show unauthenticated UI.
    }
  }

  private void initController() {
    if (indicatorController == null) indicatorController = new DefaultIndicatorController();

    FrameLayout indicatorContainer = (FrameLayout) findViewById(R.id.indicator_container);
    indicatorContainer.addView(indicatorController.newInstance(this));

    indicatorController.initialize(4);
  }

  @Override public void onConnected(Bundle bundle) {

  }

  @Override public void onConnectionSuspended(int i) {

  }

  @Override public void onConnectionFailed(ConnectionResult connectionResult) {

  }

  public class IntroAdapter extends FragmentPagerAdapter {

    public IntroAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override public Fragment getItem(int position) {
      switch (position) {
        case 0:
          return IntroSlideFragment.newInstance(R.layout.fragment_intro_slide_1);
        case 1:
          return IntroSlideFragment.newInstance(R.layout.fragment_intro_slide_2);
        case 2:
          return IntroSlideFragment.newInstance(R.layout.fragment_intro_slide_3);
        case 3:
          return IntroSlideFragment.newInstance(R.layout.fragment_intro_slide_4);
        default:
          return null;
      }
    }

    @Override public int getCount() {
      return 4;
    }
  }
}