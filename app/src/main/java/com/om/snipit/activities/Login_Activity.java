package com.om.snipit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.FrameLayout;

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
import com.om.snipit.models.User;

import butterknife.Bind;
import butterknife.ButterKnife;

public class Login_Activity extends Base_Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private DefaultIndicatorController indicatorController;
    private GoogleApiClient googleApiClient;

    private static final int RC_SIGN_IN = 0;

    @Bind(R.id.viewpager)
    ViewPager viewPager;

    @Bind(R.id.signInGoogleBTN)
    SignInButton signInGoogleBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        viewPager.setAdapter(new IntroAdapter(getSupportFragmentManager()));

        signInGoogleBTN.setSize(SignInButton.SIZE_WIDE);
        signInGoogleBTN.setColorScheme(SignInButton.COLOR_DARK);

        signInGoogleBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInGoogle();
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                indicatorController.selectPosition(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        initController();
    }

    private void signInGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {

            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            if (acct != null) {
                User user = new User();

                user.setFull_name(acct.getDisplayName());
                user.setEmail_address(acct.getEmail());
                assert acct.getPhotoUrl() != null;
                user.setPhoto_url(acct.getPhotoUrl().toString());

                Intent openBooksActivity = new Intent(Login_Activity.this, Books_Activity.class);
                openBooksActivity.putExtra(Constants.EXTRAS_USER, user);
                startActivity(openBooksActivity);

                finish();
            }
        } else {
            // Signed out, show unauthenticated UI.
        }
    }

    private void initController() {
        if (indicatorController == null)
            indicatorController = new DefaultIndicatorController();

        FrameLayout indicatorContainer = (FrameLayout) findViewById(R.id.indicator_container);
        indicatorContainer.addView(indicatorController.newInstance(this));

        indicatorController.initialize(4);
    }

    public class IntroAdapter extends FragmentPagerAdapter {

        public IntroAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_1);
                case 1:
                    return IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_2);
                case 2:
                    return IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_3);
                case 3:
                    return IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_4);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}