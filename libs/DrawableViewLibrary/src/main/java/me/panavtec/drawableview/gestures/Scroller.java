package me.panavtec.drawableview.gestures;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class Scroller extends ScrollerGestureListener {

  private final ScrollerListener listener;
  private final boolean multiTouch;
  private final GestureDetector gestureDetector;

  private float canvasWidth;
  private float canvasHeight;

  private RectF currentViewport = new RectF();
  private RectF canvasRect = new RectF();

  public Scroller(Context context, final ScrollerListener listener, boolean multiTouch) {
    this.listener = listener;
    this.multiTouch = multiTouch;
    this.gestureDetector = new GestureDetector(context, this);
  }

  public boolean onTouchEvent(MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  public void onDraw(Canvas canvas) {
    canvas.translate(-currentViewport.left, -currentViewport.top);
  }

  @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
      float distanceY) {
    if (!Scroller.this.multiTouch || MotionEventCompat.getPointerCount(e2) == 2) {
      float viewportOffsetX = distanceX * currentViewport.width() / canvasRect.width();
      float viewportOffsetY = distanceY * currentViewport.height() / canvasRect.height();

      // Updates the viewport, refreshes the display.
      setViewportBottomLeft(currentViewport.left + viewportOffsetX,
          currentViewport.bottom + viewportOffsetY);
    }
    return true;
  }

  private void setViewportBottomLeft(float x, float y) {
    float curWidth = currentViewport.width();
    float curHeight = currentViewport.height();
    x = Math.max(0, Math.min(x, canvasRect.right - curWidth));
    y = Math.max(0 + curHeight, Math.min(y, canvasRect.bottom));

    currentViewport.set(x, y - curHeight, x + curWidth, y);
    listener.onViewPortChange(currentViewport);
  }

  public void setViewBounds(int viewWidth, int viewHeight) {
    currentViewport.right = viewWidth;
    currentViewport.bottom = viewHeight;
    listener.onViewPortChange(currentViewport);
  }

  public void onScaleChange(float scaleFactor) {
    canvasRect.right = canvasWidth * scaleFactor;
    canvasRect.bottom = canvasHeight * scaleFactor;
  }

  public void setCanvasBounds(int canvasWidth, int canvasHeight) {
    this.canvasWidth = canvasWidth;
    canvasRect.right = canvasWidth;
    this.canvasHeight = canvasHeight;
    canvasRect.bottom = canvasHeight;
  }
}
