package com.viseeointernational.stop.view.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.viseeointernational.stop.R;

public class BatteryView extends View {

    private int power;
    private int width;
    private int height;
    private Paint paint = new Paint();

    public BatteryView(Context context) {
        super(context);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int headWidth = width / 8;
        int headHeight = height / 3;
        int insideMargin = height / 9;
        int strokeWidth = height / 7;

        paint.setColor(getResources().getColor(R.color.themeDarkDark));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        float left = headWidth + strokeWidth / 2;
        float top = strokeWidth / 2;
        float right = width - strokeWidth / 2;
        float bottom = height - strokeWidth / 2;
        canvas.drawRect(left, top, right, bottom, paint);

        float powerPercent = power / 100.0f;
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        if (powerPercent != 0) {
            float powerLeft = right - strokeWidth / 2 - insideMargin - (width - headWidth - strokeWidth * 3 / 2 - insideMargin * 2) * powerPercent;
            float powerTop = top + strokeWidth / 2 + insideMargin;
            float powerRight = right - strokeWidth / 2 - insideMargin;
            float powerBottom = bottom - strokeWidth / 2 - insideMargin;
            canvas.drawRect(powerLeft, powerTop, powerRight, powerBottom, paint);
        }

        int headLeft = 0;
        int headTop = (height - headHeight) / 2;
        int headRight = headLeft + headWidth;
        int headBottom = headTop + headHeight;
        canvas.drawRect(headLeft, headTop, headRight, headBottom, paint);
    }

    public void setPower(int power) {
        if (power < 0)
            power = 0;
        if (power > 100)
            power = 100;
        this.power = power;
        invalidate();
    }
}