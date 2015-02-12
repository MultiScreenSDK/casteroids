package com.hitwicketapps.accelerometer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.view.*;
import android.util.AttributeSet;

public class CompassView extends View {

    float pitch;
    boolean showNumber = false;
    private Paint markerPaint;
    private Paint textPaint;
    private Paint circlePaint;


    private int textHeight;

    /**
     * Get the current pitch
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Set the current pitch
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * Set whether the pitch value is shown or not.
     *
     * @param show
     */
    public void setShowNumber(boolean show){
        this.showNumber = show;
    }



    public CompassView(Context context) {
        super(context);
        initCompassView();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCompassView();
    }

    public CompassView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initCompassView();
    }

    protected void initCompassView() {
        setFocusable(true);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(getResources().getColor(R.color.blue_grey_700));
        circlePaint.setStrokeWidth(1);
        circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        Resources r = this.getResources();

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(r.getColor(R.color.grey_100));
        textPaint.setTextSize(45);

        textHeight = (int) textPaint.measureText("yY");

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(r.getColor(R.color.green_800));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The compass is a circle that fills as much space as possible.
        // Set the measured dimensions by figuring out the shortest boundary,
        // height or width.
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);

        int d = Math.min(measuredWidth, measuredHeight);

        setMeasuredDimension(d, d);
    }

    private int measure(int measureSpec) {
        int result = 0;

        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight() / 2;
        int radius = Math.min(centerX, centerY);

        // Draw the background
        canvas.drawCircle(centerX, centerY, radius, circlePaint);
        // Rotate our perspective so that the 'top' is
        // facing the current bearing.
        canvas.save();
        RectF pitchOval = new RectF((getMeasuredWidth() / 2) - getMeasuredWidth() / 2,
                (getMeasuredHeight() / 2) - getMeasuredWidth() / 2,
                (getMeasuredWidth() / 2) + getMeasuredWidth() / 2,
                (getMeasuredHeight() / 2) + getMeasuredWidth() / 2);

        RectF pitchOval2 = new RectF();
        markerPaint.setStyle(Paint.Style.STROKE);
        canvas.drawOval(pitchOval, markerPaint);
        markerPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.rotate(-pitch, getMeasuredWidth() / 2, getMeasuredHeight() / 2);
        canvas.drawArc(pitchOval, 0, 180, false, markerPaint);
        markerPaint.setStyle(Paint.Style.STROKE);
        canvas.restore();

        if(showNumber) {
            canvas.drawText(String.format("%.2f", pitch), centerX, centerY, textPaint);
        }

    }
}