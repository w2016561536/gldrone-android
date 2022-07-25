package com.gldz.gldrone;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;

public class RockerView extends View {

    //固定摇杆背景圆形的X,Y坐标以及半径
    private float mRockerBg_X;
    private float mRockerBg_Y;
    private float mRockerBg_R;
    //摇杆的X,Y坐标以及摇杆的半径
    private float mRockerBtn_X;
    private float mRockerBtn_Y;
    private float mRockerBtn_R;
    private float Differ_R;

    private Bitmap mBmpRockerBg;
    private Bitmap mBmpRockerBtn;

    private PointF mCenterPoint;

    public boolean xLock = false;
    public boolean yLock = false;
    public boolean xTHR = false;
    public boolean yTHR = false;
    public boolean xRETURN = false;
    public boolean yRETURN = false;

    public int Sensitivity = 520;
    public boolean touchEnable = true;

    public void setMode(boolean xLock,boolean xTHR,boolean xRETURN,boolean yLock,boolean yTHR,boolean yRETURN)
    {
        this.xLock = xLock;
        this.yLock = yLock;
        this.xTHR = xTHR;
        this.yTHR = yTHR;
        this.xRETURN = xRETURN;
        this.yRETURN = yRETURN;

        preDraw();
    }

    private void preDraw()
    {
        mCenterPoint = new PointF(getWidth() / 2, getHeight() / 2);
        mRockerBg_X = mCenterPoint.x;
        mRockerBg_Y = mCenterPoint.y;

        mRockerBg_R = getWidth() / 2;
        mRockerBtn_R = 0.3f * getWidth() / 2;

        Differ_R = mRockerBg_R - mRockerBtn_R;

        if(xTHR)
            mRockerBtn_X = mRockerBg_R*2 - mRockerBtn_R;
        else
            mRockerBtn_X = mCenterPoint.x;

        if(yTHR)
            mRockerBtn_Y = mRockerBg_R*2 - mRockerBtn_R;
        else
            mRockerBtn_Y = mCenterPoint.y;
    }

    public RockerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        // 获取bitmap
        mBmpRockerBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.joy_back);
        mBmpRockerBtn = BitmapFactory.decodeResource(context.getResources(), R.drawable.joy_move);

        getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {

            // 调用该方法时可以获取view实际的宽getWidth()和高getHeight()
            @Override
            public boolean onPreDraw() {
                // TODO Auto-generated method stub
                getViewTreeObserver().removeOnPreDrawListener(this);

                //Log.e("RockerView", getWidth() + "/" + getHeight());

                preDraw();

                return true;
            }
        });

        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                while(true){

                    //系统调用onDraw方法刷新画面
                    RockerView.this.postInvalidate();
                    //xyReport();
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        canvas.drawBitmap(mBmpRockerBg, null,
                new Rect((int)(mRockerBg_X - mRockerBg_R),
                        (int)(mRockerBg_Y - mRockerBg_R),
                        (int)(mRockerBg_X + mRockerBg_R),
                        (int)(mRockerBg_Y + mRockerBg_R)),
                null);
        canvas.drawBitmap(mBmpRockerBtn, null,
                new Rect((int)(mRockerBtn_X - mRockerBtn_R),
                        (int)(mRockerBtn_Y - mRockerBtn_R),
                        (int)(mRockerBtn_X + mRockerBtn_R),
                        (int)(mRockerBtn_Y + mRockerBtn_R)),
                null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!touchEnable)
            return true;

        // TODO Auto-generated method stub
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            // 当触屏区域不在活动范围内
            if(Math.abs(mRockerBg_X - (int) event.getX()) > Differ_R && Math.abs(mRockerBg_Y - (int) event.getY()) > Differ_R)
            {

            }
            else if (Math.abs(mRockerBg_X - (int) event.getX()) > Differ_R) {
                if(!yLock)
                    mRockerBtn_Y = (int) event.getY();
            } else if(Math.abs(mRockerBg_Y - (int) event.getY()) > Differ_R)
            {
                if(!xLock)
                    mRockerBtn_X = (int) event.getX();
            } else{//如果小球中心点小于活动区域则随着用户触屏点移动即可
                if(!xLock)
                    mRockerBtn_X = (int) event.getX();
                if(!yLock)
                    mRockerBtn_Y = (int) event.getY();
            }

            xyReport();

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            //当释放按键时摇杆要恢复摇杆的位置为初始位置

            if(!xRETURN)
            {
                if(xTHR)
                    mRockerBtn_X = mRockerBg_R*2 - mRockerBtn_R;
                else
                    mRockerBtn_X = mCenterPoint.x;
            }

            if(!yRETURN) {
                if (yTHR)
                    mRockerBtn_Y = mRockerBg_R * 2 - mRockerBtn_R;
                else
                    mRockerBtn_Y = mCenterPoint.y;
            }

            xyReport();
        }
        return true;
    }

    RockerChangeListener mRockerChangeListener = null;
    public void setRockerChangeListener(RockerChangeListener rockerChangeListener) {
        mRockerChangeListener = rockerChangeListener;
    }
    public interface RockerChangeListener {
        public void report(int x, int y);
    }

    public void xyReport()
    {
        if(mCenterPoint != null)
        {
//            int xValue = (int)(1500 + 500 * (mRockerBtn_X - mCenterPoint.x) / Differ_R);
//            int yValue = (int)(1500 + 500 * (mCenterPoint.y - mRockerBtn_Y) / Differ_R);


            int xValue = (int)(1500 + Sensitivity * (mRockerBtn_X - mCenterPoint.x) / Differ_R);
            int yValue = (int)(1500 + Sensitivity * (mCenterPoint.y - mRockerBtn_Y) / Differ_R);
            if(mRockerChangeListener != null) {
                mRockerChangeListener.report(limit_rc(xValue), limit_rc(yValue));
            }
        }
    }

    public void setXY(int x,int y)
    {
        x = limit_rc(x);
        y = limit_rc(y);

        if(mCenterPoint != null)
        {
            mRockerBtn_X = (float)(x-1500) / Sensitivity * Differ_R + mCenterPoint.x;
            mRockerBtn_Y = mCenterPoint.y - (float)(y-1500) / Sensitivity * Differ_R;

            //Log.i("RIGHT", String.valueOf(mRockerBtn_X) + " " + String.valueOf(mRockerBtn_Y) + " " + String.valueOf(x) + " " + String.valueOf(y)  );
        }

    }

    public static int limit_rc(int input)
    {
        int rc = input;
        if(input > 2000)
            rc = 2000;
        else if(input < 1000)
            rc = 1000;

        return rc;
    }
}
