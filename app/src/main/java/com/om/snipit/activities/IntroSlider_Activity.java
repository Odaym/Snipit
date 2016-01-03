package com.om.snipit.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.github.paolorotolo.appintro.AppIntro;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;

public class IntroSlider_Activity extends AppIntro {
    private SharedPreferences prefs;

    @Override
    public void init(Bundle savedInstanceState) {
        prefs = getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);

        addSlide(IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_1));
        addSlide(IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_2));
        addSlide(IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_3));
        addSlide(IntroSlide_Fragment.newInstance(R.layout.fragment_intro_slide_4));
    }

    private void loadMainActivity() {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(Constants.SEEN_APP_INTRO_PREF, true);
        prefsEditor.apply();

        Intent intent = new Intent(this, Books_Activity.class);
        startActivity(intent);
    }

    @Override
    public void onNextPressed() {
    }

    @Override
    public void onSkipPressed() {
        loadMainActivity();
    }

    @Override
    public void onDonePressed() {
        loadMainActivity();
    }

    @Override
    public void onSlideChanged() {
    }

    public void getStarted(View v) {
        loadMainActivity();
    }
}