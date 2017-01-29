package com.om.snipit.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class IntroSlideFragment extends Fragment {

  private static final String LAYOUT_RES_ID = "layoutResId";
  private int layoutResId;

  public static IntroSlideFragment newInstance(int layoutResId) {
    IntroSlideFragment frag = new IntroSlideFragment();

    Bundle b = new Bundle();
    b.putInt(LAYOUT_RES_ID, layoutResId);
    frag.setArguments(b);

    return frag;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!getArguments().containsKey(LAYOUT_RES_ID)) {
      throw new RuntimeException("Fragment must contain a layoutResId argument!");
    }

    layoutResId = getArguments().getInt(LAYOUT_RES_ID);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return getActivity().getLayoutInflater().inflate(layoutResId, container, false);
  }
}