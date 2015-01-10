package com.ttco.bookmarker.showcaseview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

interface ShowcaseDrawer {

    void setShowcaseColour(int color);

    void drawShowcase(Bitmap buffer, float x, float y, float scaleMultiplier);

    int getShowcaseWidth();

    int getShowcaseHeight();

    float getBlockedRadius();

    void setBackgroundColour(int backgroundColor);

    void erase(Bitmap bitmapBuffer);

    void drawToCanvas(Canvas canvas, Bitmap bitmapBuffer);
}
