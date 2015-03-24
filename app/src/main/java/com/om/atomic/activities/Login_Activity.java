//package com.om.atomic.activities;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.content.IntentSender;
//import android.os.Bundle;
//import android.view.View;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.plus.Plus;
//import com.google.android.gms.plus.model.people.Person;
//import com.om.atomic.R;
//import com.om.atomic.classes.Constants;
//
//import de.keyboardsurfer.android.widget.crouton.Crouton;
//import de.keyboardsurfer.android.widget.crouton.Style;
//
//public class Login_Activity extends Activity implements
//        GoogleApiClient.ConnectionCallbacks,
//        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
//
//    /* Request code used to invoke sign in user interactions. */
//    private static final int RC_SIGN_IN = 0;
//
//    /* Client used to interact with Google APIs. */
//    private GoogleApiClient mGoogleApiClient;
//
//    /* A flag indicating that a PendingIntent is in progress and prevents
//     * us from starting further intents.
//     */
//    private boolean mIntentInProgress;
//
//    /* Track whether the sign-in button has been clicked so that we know to resolve
// * all issues preventing sign-in without waiting.
// */
//    private boolean mSignInClicked;
//
//    /* Store the connection result from onConnectionFailed callbacks so that we can
//     * resolve them when the user clicks sign-in.
//     */
//    private ConnectionResult mConnectionResult;
//
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(Plus.API)
//                .addScope(Plus.SCOPE_PLUS_PROFILE)
//                .build();
//
//        setContentView(R.layout.activity_login);
//
//        findViewById(R.id.sign_in_button).setOnClickListener(this);
//        findViewById(R.id.sign_out_button).setOnClickListener(this);
//    }
//
//    protected void onStart() {
//        super.onStart();
//        mGoogleApiClient.connect();
//    }
//
//    protected void onStop() {
//        super.onStop();
//
//        if (mGoogleApiClient.isConnected()) {
//            mGoogleApiClient.disconnect();
//        }
//    }
//
//    @Override
//    public void onClick(View view) {
//        if (view.getId() == R.id.sign_in_button
//                && !mGoogleApiClient.isConnecting()) {
//            mSignInClicked = true;
//            resolveSignInError();
//        } else if (view.getId() == R.id.sign_out_button) {
//            if (mGoogleApiClient.isConnected()) {
//                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
//                mGoogleApiClient.disconnect();
//                mGoogleApiClient.connect();
//            }
//        }
//    }
//
//    @Override
//    public void onConnected(Bundle bundle) {
//        mSignInClicked = false;
//
//        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
//            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
//
//            Intent open_MainActivity = new Intent(Login_Activity.this, Books_Activity.class);
//            open_MainActivity.putExtra(Constants.USER_EMAIL_ADDRESS, Plus.AccountApi.getAccountName(mGoogleApiClient));
//            open_MainActivity.putExtra(Constants.USER_FULL_NAME, currentPerson.getDisplayName());
//            open_MainActivity.putExtra(Constants.USER_PHOTO_URL, currentPerson.getImage().getUrl());
//            startActivity(open_MainActivity);
//            finish();
//        }
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult result) {
//        if (!mIntentInProgress) {
//            // Store the ConnectionResult so that we can use it later when the user clicks
//            // 'sign-in'.
//            mConnectionResult = result;
//
//            if (mSignInClicked) {
//                // The user has already clicked 'sign-in' so we attempt to resolve all
//                // errors until the user is signed in, or they cancel.
//                resolveSignInError();
//            }
//        }
//    }
//
//    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
//        if (requestCode == RC_SIGN_IN) {
//            if (responseCode != RESULT_OK) {
//                mSignInClicked = false;
//            }
//
//            mIntentInProgress = false;
//
//            if (!mGoogleApiClient.isConnecting()) {
//                mGoogleApiClient.connect();
//            }
//        }
//    }
//
//    /* A helper method to resolve the current ConnectionResult error. */
//    private void resolveSignInError() {
//        if (mConnectionResult == null) {
//            Crouton.makeText(this, "User has already been signed in!", Style.INFO).show();
//        } else {
//            if (mConnectionResult.hasResolution()) {
//                try {
//                    mIntentInProgress = true;
//                    startIntentSenderForResult(mConnectionResult.getResolution().getIntentSender(),
//                            RC_SIGN_IN, null, 0, 0, 0);
//                } catch (IntentSender.SendIntentException e) {
//                    // The intent was canceled before it was sent.  Return to the default
//                    // state and attempt to connect to get an updated ConnectionResult.
//                    mIntentInProgress = false;
//                    mGoogleApiClient.connect();
//                }
//            }
//        }
//    }
//}