package me.panavtec.drawableview.gestures;

import android.view.GestureDetector;
import android.view.MotionEvent;

public abstract class ScrollerGestureListener implements GestureDetector.OnGestureListener {
  
  @Override public boolean onDown(MotionEvent e) {
    return true;
  }

  @Override public void onShowPress(MotionEvent e) {
  }

  @Override public boolean onSingleTapUp(MotionEvent e) {
    return false;
  }

  @Override public void onLongPress(MotionEvent e) {
  }

  @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
      float velocityY) {
    return false;
  }
  
}
