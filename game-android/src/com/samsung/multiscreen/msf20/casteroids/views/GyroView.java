package com.samsung.multiscreen.msf20.casteroids.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.samsung.multiscreen.msf20.casteroids.R;

/**
 * Visual representation of the orientation of the device while held in landscape mode.
 *
 * @author Nik Bhattacharya
 */

public class GyroView extends View {

    float pitch;
    boolean showNumber = false;
    private Paint outerStrokePaint, textPaint, gyroPaint, innerStrokePaint;
    private int gyroColor;
    private RectF pitchOval;
    private float centerX, centerY, radius, innerRadius;

    /**
     * Constructor.
     *
     * @param context
     */
    public GyroView(Context context) {
        super(context);
        initCompassView();
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     */
    public GyroView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCompassView();
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     * @param defaultStyle
     */
    public GyroView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initCompassView();
    }

    protected void initCompassView() {
        setFocusable(true);
        Resources r = this.getResources();
        gyroPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gyroPaint.setStyle(Paint.Style.FILL);

        innerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerStrokePaint.setStyle(Paint.Style.FILL);

        outerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerStrokePaint.setColor(getResources().getColor(R.color.grey_100));
        outerStrokePaint.setStyle(Paint.Style.STROKE);


        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(r.getColor(R.color.grey_100));
        textPaint.setTextSize(45);

        gyroColor = r.getColor(R.color.amber_600);


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The compass is a circle that fills as much space as possible.
        // Set the measured dimensions by figuring out the shortest boundary,
        // height or width.
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);

        setMeasuredDimension(measuredWidth, measuredHeight);
        calculateDimensions();
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
        canvas.save();
        canvas.rotate(-pitch, centerX, centerY);
        canvas.drawArc(pitchOval, 0, 180, true, gyroPaint);
        canvas.restore();


        //draw the inner stroke
        canvas.drawCircle(centerX, centerY, radius, innerStrokePaint);

        //draw the outer stroke
        canvas.drawCircle(centerX, centerY, radius, outerStrokePaint);

        if (showNumber) {
            canvas.drawText(String.format("%.0f", pitch), centerX, centerY, textPaint);
        }
    }

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
    public void setShowNumber(boolean show) {
        this.showNumber = show;
    }

    /**
     * Sets the color of the gyroscope.
     *
     * @param color argb hex color.
     */
    public void setGyroColor(int color) {
        this.gyroColor = color;
        gyroPaint.setColor(gyroColor);
        gyroPaint.setAlpha(212);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    private void calculateDimensions() {
        centerX = getMeasuredWidth()  / 2.0f;
        centerY = getMeasuredHeight()/ 2.0f;
        float originalRadius = Math.min(centerX, centerY);
        radius =  originalRadius* 0.98f;
        innerRadius = (int)(radius - (radius * 0.05));

        pitchOval = new RectF(originalRadius*0.05f,originalRadius*0.05f,(originalRadius-0.05f)*2,(originalRadius-0.05f)*2);

        outerStrokePaint.setStrokeWidth(radius * 0.05f);

        RadialGradient radialGradient=new RadialGradient(centerX,centerY,originalRadius,
                /** 5 colors */
                new int[]{
                    0x00000000,
                    0x00000000,
                    Color.argb(50, Color.red(this.gyroColor), Color.green(this.gyroColor), Color.blue(this.gyroColor)),
                    Color.argb(100, Color.red(this.gyroColor), Color.green(this.gyroColor), Color.blue(this.gyroColor)),
                    this.gyroColor
                },
                /** 5 stops */
                new float[]{
                    0.f,
                    0.85f,
                    0.86f,
                    0.90f,
                    1.f},
                Shader.TileMode.CLAMP);
        innerStrokePaint.setShader(radialGradient);
        innerStrokePaint.setAntiAlias(true);
        innerStrokePaint.setDither(true);

    }
}