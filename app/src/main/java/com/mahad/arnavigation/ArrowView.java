package com.mahad.arnavigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ArrowView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();

    public ArrowView(Context context) {
        super(context);
        init();
    }

    public ArrowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArrowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        fillPaint.setColor(Color.parseColor("#FF3D00"));
        fillPaint.setStyle(Paint.Style.FILL);

        outlinePaint.setColor(Color.WHITE);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(6f);
        outlinePaint.setAlpha(220);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;

        arrowPath.reset();
        arrowPath.moveTo(cx, h * 0.08f);
        arrowPath.lineTo(w * 0.80f, h * 0.52f);
        arrowPath.lineTo(w * 0.60f, h * 0.52f);
        arrowPath.lineTo(w * 0.60f, h * 0.92f);
        arrowPath.lineTo(w * 0.40f, h * 0.92f);
        arrowPath.lineTo(w * 0.40f, h * 0.52f);
        arrowPath.lineTo(w * 0.20f, h * 0.52f);
        arrowPath.close();

        canvas.drawPath(arrowPath, fillPaint);
        canvas.drawPath(arrowPath, outlinePaint);
    }
}
