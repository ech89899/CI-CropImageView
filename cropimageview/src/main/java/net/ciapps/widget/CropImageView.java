package net.ciapps.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Evgeny Cherkasov on 07.04.2016.
 */
public class CropImageView extends AppCompatImageView {

    @IntDef({CROP_MODE_NONE, CROP_MODE_RECTANGLE, CROP_MODE_CIRCLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CropMode {
    }

    public final static int CROP_MODE_NONE = 0;
    public final static int CROP_MODE_RECTANGLE = 1;
    public final static int CROP_MODE_CIRCLE = 2;
    public final static int MAX_CROP_WIDTH = 512;
    public final static int MAX_CROP_HEIGHT = 512;

    private RectF cropRect;

    private int colorBorder;
    private int colorShadow;
    private int cropMode;
    private boolean fixAspectRatio;
    private int maxCropWidth;
    private int maxCropHeight;
    private boolean showTouchTarget;

    private final static int DRAGGING_MODE_CENTER = 1;
    private final static int DRAGGING_MODE_LEFT_TOP = 2;
    private final static int DRAGGING_MODE_RIGHT_TOP = 3;
    private final static int DRAGGING_MODE_LEFT_BOTTOM = 4;
    private final static int DRAGGING_MODE_RIGHT_BOTTOM = 5;
    private boolean isDragging;
    private int draggingMode;
    private float draggingDeltaX;
    private float draggingDeltaY;
    private float touchTargetSize = 36f;

    @NonNull
    private RectF bitmapRect = new RectF();

    private Paint paintBorder;
    private Paint paintShadow;
    private Paint paintTouchTarget;

    public CropImageView(Context context) {
        super(context);
        if (!isInEditMode()) {
            init(context, null);
        }
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            init(context, attrs);
        }
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            init(context, attrs);
        }
    }

    private void init(Context context, AttributeSet attrs) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0);

        colorBorder = typedArray.getColor(R.styleable.CropImageView_colorBorder, ContextCompat.getColor(context, R.color.border));
        colorShadow = typedArray.getColor(R.styleable.CropImageView_colorShadow, ContextCompat.getColor(context, R.color.shadow));
        cropMode = typedArray.getInteger(R.styleable.CropImageView_cropMode, CROP_MODE_RECTANGLE);
        fixAspectRatio = typedArray.getBoolean(R.styleable.CropImageView_fixAspectRatio, true);
        maxCropWidth = typedArray.getInteger(R.styleable.CropImageView_maxCropWidth, MAX_CROP_WIDTH);
        maxCropHeight = typedArray.getInteger(R.styleable.CropImageView_maxCropHeight, MAX_CROP_HEIGHT);
        showTouchTarget = typedArray.getBoolean(R.styleable.CropImageView_showTouchTarget, true);
        typedArray.recycle();

        paintBorder = initPaintBorder();
        paintShadow = initPaintShadow();
        paintTouchTarget = initPaintTouchTarget();
    }

    private void initCrop() {
        float cropWidth = Math.min(bitmapRect.width(), maxCropWidth);
        float cropHeight = Math.min(bitmapRect.height(), maxCropHeight);
        if (fixAspectRatio) {
            if (cropWidth < cropHeight) {
                cropHeight = cropWidth;
            }
            else {
                cropWidth = cropHeight;
            }
        }
        float left = bitmapRect.left + Math.min(bitmapRect.width(), (bitmapRect.width() - cropWidth) / 2);
        float top = bitmapRect.top + Math.min(bitmapRect.height(), (bitmapRect.height() - cropHeight) / 2);
        cropRect = new RectF(left, top, left + cropWidth, top + cropHeight);
    }

    private Paint initPaintBorder() {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(colorBorder);
        return paint;
    }

    private Paint initPaintShadow() {
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorShadow);
        return paint;
    }

    private Paint initPaintTouchTarget() {
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(colorBorder);
        return paint;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        bitmapRect = getBitmapRect();
        initCrop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode()) {

        } else {
            drawShadow(canvas);
            drawBorder(canvas);
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchActionDown(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                onTouchActionUp();
                return true;
            case MotionEvent.ACTION_MOVE:
                onTouchActionMove(event.getX(), event.getY());
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            default:
                return false;
        }
    }

    // Touch handling
    //
    private void onTouchActionDown(float x, float y) {
//        if (x >= cropRect.left && x <= cropRect.right && y >= cropRect.top && y <= cropRect.bottom) {
        if (cropRect.contains(x, y)) {
            isDragging = true;
            draggingMode = detectDraggingMode(x, y);
            draggingDeltaX = x - cropRect.left;
            draggingDeltaY = y - cropRect.top;
        }
    }

    private int detectDraggingMode(float x, float y) {
        RectF rectCorner;
        rectCorner = new RectF(cropRect.left, cropRect.top, cropRect.left + touchTargetSize, cropRect.top + touchTargetSize);
        if (rectCorner.contains(x, y)) {
            return DRAGGING_MODE_LEFT_TOP;
        }
        rectCorner = new RectF(cropRect.right - touchTargetSize, cropRect.top, cropRect.right, cropRect.top + touchTargetSize);
        if (rectCorner.contains(x, y)) {
            return DRAGGING_MODE_RIGHT_TOP;
        }
        rectCorner = new RectF(cropRect.left, cropRect.bottom - touchTargetSize, cropRect.left + touchTargetSize, cropRect.bottom);
        if (rectCorner.contains(x, y)) {
            return DRAGGING_MODE_LEFT_BOTTOM;
        }
        rectCorner = new RectF(cropRect.right - touchTargetSize, cropRect.bottom - touchTargetSize, cropRect.right, cropRect.bottom);
        if (rectCorner.contains(x, y)) {
            return DRAGGING_MODE_RIGHT_BOTTOM;
        }
        return DRAGGING_MODE_CENTER;
    }

    private void onTouchActionUp() {
        isDragging = false;
    }

    private void onTouchActionMove(float x, float y) {
        if (isDragging) {
            switch (draggingMode) {
                case DRAGGING_MODE_CENTER: {
                    float left = x + Math.min(0, bitmapRect.right - (x + cropRect.width() - draggingDeltaX));
                    float top = y + Math.min(0, bitmapRect.bottom - (y + cropRect.height() - draggingDeltaY));
                    cropRect.offsetTo(Math.max(bitmapRect.left, left - draggingDeltaX), Math.max(bitmapRect.top, top - draggingDeltaY));
                    break;
                }
                case DRAGGING_MODE_LEFT_TOP: {
                    float maxDeltaX = maxCropWidth - cropRect.width();
                    float maxDeltaY = maxCropHeight - cropRect.height();
                    float left = Math.max(cropRect.left - maxDeltaX, Math.max(bitmapRect.left, Math.min(x, cropRect.right - 2 * touchTargetSize)));
                    float top = Math.max(cropRect.top - maxDeltaY, Math.max(bitmapRect.top, Math.min(y, cropRect.bottom - 2 * touchTargetSize)));
                    cropRect.set(left, top, cropRect.right, cropRect.bottom);
                    if (fixAspectRatio) {
                        if (cropRect.width() != cropRect.height()) {
                            if (cropRect.width() < cropRect.height()) {
                                cropRect.set(left, cropRect.bottom - cropRect.width(), cropRect.right, cropRect.bottom);
                            }
                            else {
                                cropRect.set(cropRect.right - cropRect.height(), cropRect.top, cropRect.right, cropRect.bottom);
                            }
                        }
                    }
                    break;
                }
                case DRAGGING_MODE_RIGHT_TOP: {
                    float maxDeltaX = maxCropWidth - cropRect.width();
                    float maxDeltaY = maxCropHeight - cropRect.height();
                    float right = Math.min(cropRect.right + maxDeltaX, Math.min(bitmapRect.right, Math.max(x, cropRect.left + 2 * touchTargetSize)));
                    float top = Math.max(cropRect.top - maxDeltaY, Math.max(bitmapRect.top, Math.min(y, cropRect.bottom - 2 * touchTargetSize)));
                    cropRect.set(cropRect.left, top, right, cropRect.bottom);
                    if (fixAspectRatio) {
                        if (cropRect.width() != cropRect.height()) {
                            if (cropRect.width() < cropRect.height()) {
                                cropRect.set(cropRect.left, cropRect.top, cropRect.right, cropRect.top + cropRect.width());
                            }
                            else {
                                cropRect.set(cropRect.left, cropRect.top, cropRect.left + cropRect.height(), cropRect.bottom);
                            }
                        }
                    }
                    break;
                }
                case DRAGGING_MODE_LEFT_BOTTOM: {
                    float maxDeltaX = maxCropWidth - cropRect.width();
                    float maxDeltaY = maxCropHeight - cropRect.height();
                    float left = Math.max(cropRect.left - maxDeltaX, Math.max(bitmapRect.left, Math.min(x, cropRect.right - 2 * touchTargetSize)));
                    float bottom = Math.min(cropRect.bottom + maxDeltaY, Math.min(bitmapRect.bottom, Math.max(y, cropRect.top + 2 * touchTargetSize)));
                    cropRect.set(left, cropRect.top, cropRect.right, bottom);
                    if (fixAspectRatio) {
                        if (cropRect.width() != cropRect.height()) {
                            if (cropRect.width() < cropRect.height()) {
                                cropRect.set(cropRect.left, cropRect.top, cropRect.right, cropRect.top + cropRect.width());
                            }
                            else {
                                cropRect.set(cropRect.right - cropRect.height(), cropRect.top, cropRect.right, cropRect.bottom);
                            }
                        }
                    }
                    break;
                }
                case DRAGGING_MODE_RIGHT_BOTTOM: {
                    float maxDeltaX = maxCropWidth - cropRect.width();
                    float maxDeltaY = maxCropHeight - cropRect.height();
                    float right = Math.min(cropRect.right + maxDeltaX, Math.min(bitmapRect.right, Math.max(x, cropRect.left + 2 * touchTargetSize)));
                    float bottom = Math.min(cropRect.bottom + maxDeltaY, Math.min(bitmapRect.bottom, Math.max(y, cropRect.top + 2 * touchTargetSize)));
                    cropRect.set(cropRect.left, cropRect.top, right, bottom);
                    if (fixAspectRatio) {
                        if (cropRect.width() != cropRect.height()) {
                            if (cropRect.width() < cropRect.height()) {
                                cropRect.set(cropRect.left, cropRect.top, cropRect.right, cropRect.top + cropRect.width());
                            }
                            else {
                                cropRect.set(cropRect.left, cropRect.top, cropRect.left + cropRect.height(), cropRect.bottom);
                            }
                        }
                    }
                    break;
                }
            }
            invalidate();
        }
    }

    // Attributes
    //
    public void setCropMode(@CropMode int cropMode) {
        this.cropMode = cropMode;
    }

    public void setFixAspectRatio(boolean fixAspectRatio) {
        this.fixAspectRatio = fixAspectRatio;
    }

    public void setMaxCropWidth(int maxCropWidth) {
        this.maxCropWidth = maxCropWidth;
    }

    public void setMaxCropHeight(int maxCropHeight) {
        this.maxCropHeight = maxCropHeight;
    }

    public void setShowTouchTarget(boolean showTouchTarget) {
        this.showTouchTarget = showTouchTarget;
    }

    // Draw
    //
    private RectF getBitmapRect() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return new RectF();
        }

        final float[] matrixValues = new float[9];
        getImageMatrix().getValues(matrixValues);
        final float scaleX = matrixValues[Matrix.MSCALE_X];
        final float scaleY = matrixValues[Matrix.MSCALE_Y];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        // Get the width and height of the original bitmap.
        final int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
        final int drawableIntrinsicHeight = drawable.getIntrinsicHeight();

        // Calculate the dimensions as seen on screen.
        final int drawableDisplayWidth = (scaleX == 0) ? drawableIntrinsicWidth : Math.round(drawableIntrinsicWidth * scaleX);
        final int drawableDisplayHeight = (scaleY == 0) ? drawableIntrinsicHeight : Math.round(drawableIntrinsicHeight * scaleY);

        // Get the Rect of the displayed image within the ImageView.
        final float left = Math.max(transX, 0);
        final float top = Math.max(transY, 0);
        final float right = Math.min(left + drawableDisplayWidth, getWidth());
        final float bottom = Math.min(top + drawableDisplayHeight, getHeight());

        return new RectF(left, top, right, bottom);
    }

    private void drawShadow(@NonNull Canvas canvas) {
        if (cropRect != null) {
            switch (cropMode) {
                case CROP_MODE_RECTANGLE:
                    canvas.clipRect(bitmapRect);
                    canvas.clipRect(cropRect, Region.Op.DIFFERENCE);
                    canvas.drawRect(bitmapRect, paintShadow);
                    break;
                case CROP_MODE_CIRCLE:
                    Path path = new Path();
                    path.addCircle(cropRect.left + cropRect.width() / 2, cropRect.top + cropRect.height() / 2, cropRect.width() / 2, Path.Direction.CW);
                    canvas.clipRect(bitmapRect);
                    canvas.clipPath(path, Region.Op.DIFFERENCE);
                    canvas.drawRect(bitmapRect, paintShadow);
                    break;
            }
        }
    }

    private void drawBorder(@NonNull Canvas canvas) {
        if (cropRect != null) {
            switch (cropMode) {
                case CROP_MODE_RECTANGLE:
                    canvas.clipRect(bitmapRect, Region.Op.REPLACE);
                    canvas.drawRect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom, paintBorder);
                    if (showTouchTarget) {
                        canvas.drawRect(cropRect.left, cropRect.top, cropRect.left + touchTargetSize, cropRect.top + touchTargetSize, paintTouchTarget);
                        canvas.drawRect(cropRect.right - touchTargetSize, cropRect.top, cropRect.right, cropRect.top + touchTargetSize, paintTouchTarget);
                        canvas.drawRect(cropRect.left, cropRect.bottom - touchTargetSize, cropRect.left + touchTargetSize, cropRect.bottom, paintTouchTarget);
                        canvas.drawRect(cropRect.right - touchTargetSize, cropRect.bottom - touchTargetSize, cropRect.right, cropRect.bottom, paintTouchTarget);
                    }
                    break;
                case CROP_MODE_CIRCLE:
                    canvas.clipRect(bitmapRect, Region.Op.REPLACE);
                    canvas.drawRoundRect(cropRect, cropRect.width() / 2, cropRect.height() / 2, paintBorder);
                    if (showTouchTarget) {
                        canvas.drawRect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom, paintTouchTarget);
                    }
                    break;
            }
        }
    }

    //
    //
    public Bitmap getCroppedBitmap() {
        final Drawable drawable = getDrawable();
        if (drawable == null || !(drawable instanceof BitmapDrawable)) {
            return null;
        }

        // Get image matrix values.
        final float[] matrixValues = new float[9];
        getImageMatrix().getValues(matrixValues);
        final float scaleX = (matrixValues[Matrix.MSCALE_X] == 0) ? 1 : matrixValues[Matrix.MSCALE_X];
        final float scaleY = (matrixValues[Matrix.MSCALE_Y] == 0) ? 1 : matrixValues[Matrix.MSCALE_Y];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        final float bitmapLeft = (transX < 0) ? 0 : -transX;
        final float bitmapTop = (transY < 0) ? 0 : -transY;

        final Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();

        // Calculate the top-left and width-height of the crop window relative to the original bitmap size.
        final float cropX = (bitmapLeft + cropRect.left) / scaleX;
        final float cropY = (bitmapTop + cropRect.top) / scaleY;
        final float cropWidth = Math.min(cropRect.width() / scaleX, originalBitmap.getWidth() - cropX);
        final float cropHeight = Math.min(cropRect.height() / scaleY, originalBitmap.getHeight() - cropY);

        return Bitmap.createBitmap(originalBitmap, (int) cropX, (int) cropY, (int) cropWidth, (int) cropHeight);
    }

}
