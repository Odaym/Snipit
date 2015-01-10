package com.ttco.bookmarker.showcaseview;

import android.widget.RelativeLayout;

/**
 * A simple interface which makes it easy to keep track of what is in the public
 * ShowcaseView API
 */
public interface ShowcaseViewApi {
    void hide();

    void show();

    void setContentTitle(CharSequence title);

    void setContentText(CharSequence text);

    void setButtonPosition(RelativeLayout.LayoutParams layoutParams);

    void setHideOnTouchOutside(boolean hideOnTouch);

    void setBlocksTouches(boolean blockTouches);

    void setStyle(int theme);

    boolean isShowing();
}
