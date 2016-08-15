package com.stream11.puffer;
/*
 *       Copyright 2016 Gene Leybzon
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by gleybzon on 5/3/16.
 */

public class Fire extends ImageView {
    private int mW, mH;
    private boolean mOn = false;

    private Paint mPaintOff;
    private Paint mPaintOn;

    public Fire(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        this.setWillNotDraw(false);
    }

    public Fire(Context context) {
        this(context, null);
        init();
        this.setWillNotDraw(false);
    }

    private void init() {
        mPaintOn = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintOn.setStyle(Paint.Style.FILL);
        mPaintOn.setColor(Color.RED);

        mPaintOff = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintOff.setStyle(Paint.Style.STROKE);
        mPaintOff.setColor(Color.GREEN);

        this.setMinimumHeight(10);
        this.setMinimumWidth(10);

        setOnClickListener(mClickListener);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mW = w;
        mH = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
/*
        int x = getWidth();
        int y = getHeight();
        int radius = Math.min(x, y) / 2;

        if (isOn()) {
            canvas.drawCircle(x / 2, y / 2, radius, mPaintOn);
        } else {
            canvas.drawCircle(x / 2, y / 2, radius, mPaintOff);
        }
        */
    }

    public boolean isOn() {
        return mOn;
    }

    public void setOn(boolean on) {
        this.mOn = on;
        if (on) {
            setImageResource(R.mipmap.ic_fire);
        } else {
            setImageResource(R.mipmap.ic_launcher);
        }

        invalidate();
    }

    public void reverse() {
        setOn(!isOn());
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            reverse();
        }
    };


}
