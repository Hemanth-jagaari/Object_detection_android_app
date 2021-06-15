package com.example.first_version;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;




public class Draw extends View {

    private final Paint paint;
    private final RectF rect;
    private String text;
    private final Paint textpaint;


    public Draw(Context context, RectF rect, String label) {
        super((Context) context);
        paint=new Paint();
        this.rect=rect;
        this.text=label;
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        textpaint=new Paint();
        textpaint.setColor(Color.GREEN);
        textpaint.setStyle(Paint.Style.FILL);
        textpaint.setTextSize(80f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(text,rect.centerX(),rect.centerY(),textpaint);
        canvas.drawRect(rect.left,rect.top,rect.right,rect.bottom,paint);

    }
}
