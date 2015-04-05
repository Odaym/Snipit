package me.panavtec.drawableview.gestures;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class CanvasDrawer {

  private Paint paint =
      new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
  private int canvasWidth;
  private int canvasHeight;
  private float scaleFactor = 1.0f;
  private RectF canvasPort = new RectF();
  private RectF canvasPortNotScaled = new RectF();
  private RectF currentViewport = new RectF();
  private CanvasDrawerListener canvasDrawerListener;

  public CanvasDrawer(CanvasDrawerListener canvasDrawerListener) {
    this.canvasDrawerListener = canvasDrawerListener;
    paint.setStrokeWidth(2.0f);
    paint.setStyle(Paint.Style.STROKE);
  }

  public void onDraw(Canvas canvas) {
    //if (BuildConfig.DEBUG) {
    //  paint.setTextSize(30.0f);
    //  canvas.drawText(canvasPort.toShortString(), 0, 30, paint);
    //}
    canvas.drawRect(canvasPort, paint);
  }

  public void onScaleChange(float scaleFactor) {
    this.scaleFactor = scaleFactor;
    recalculateCanvas();
  }

  public void changedViewPort(RectF currentViewport) {
    this.currentViewport = currentViewport;
    recalculateCanvas();
  }

  public void setCanvasBounds(int canvasWidth, int canvasHeight) {
    this.canvasWidth = canvasWidth;
    this.canvasHeight = canvasHeight;
  }

  private void recalculateCanvas() {
    float left = -currentViewport.left;
    float top = -currentViewport.top;
    float right = canvasWidth * scaleFactor - currentViewport.left;
    float bottom = canvasHeight * scaleFactor - currentViewport.top;
    canvasPort.set(left, top, right, bottom);
    canvasPortNotScaled.set(left, top, canvasWidth + currentViewport.left, canvasHeight + currentViewport.top);
    canvasDrawerListener.onCanvasPortChanged(canvasPortNotScaled);
  }
}
