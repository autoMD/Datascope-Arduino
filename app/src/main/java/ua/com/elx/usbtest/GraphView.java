package ua.com.elx.usbtest;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class GraphView extends GLSurfaceView{

    private static final int INVALID_POINTER_ID = -1;
    public GLGraphRender chartRenderer;
    //private float[] dataMin = new float[SignalChart.CHART_POINT];
    //private float[] dataMax = new float[SignalChart.CHART_POINT];
    DisplayBuffer main = new DisplayBuffer();
    DisplayBuffer back = new DisplayBuffer();
    private float gMaxValue = 0f;
    private float gMinValue = 0f;
    private MyActivity parent;
    float mScaleFactor;

    int width;
    int height;

    boolean isUpdating = false;
    int i = 1;

    private ScaleGestureDetector mScaleDetector;

    public GraphView(Context context, MyActivity act) {
        super(context);

        parent = act;
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.setZOrderOnTop(true); //necessary
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // Set the Renderer for drawing on the GLSurfaceView
        chartRenderer = new GLGraphRender(context);
        setRenderer(chartRenderer);
        for (int i = 0; i < main.dataMin.length; i++){
            main.dataMin[i] = 0.0f;
            main.dataMax[i] = 0.0f;
            back.dataMin[i] = 0.0f;
            back.dataMax[i] = 0.0f;
        }
        setChartData(main,back);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        new Thread(new Task()).start();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setChartData(DisplayBuffer main,DisplayBuffer back) {
        isUpdating = true;
        if (main.dataMin.length > 0){

            main.copyTo(this.main);

            gMaxValue = 280.0f;

            //this.datapoints[0] = (((0.0f - gMinValue) * (1.0f - (-1.0f))/(gMaxValue - gMinValue)) + (-1));
            for (int i = 0; i < this.main.dataMin.length; i++){
                this.main.dataMin[i] = (((main.dataMin[i] - gMinValue) * (1.0f - (-1.0f))/(gMaxValue - gMinValue)) + (-1));
                this.main.dataMax[i] = (((main.dataMax[i] - gMinValue) * (1.0f - (-1.0f))/(gMaxValue - gMinValue)) + (-1));
                //Log.d("DD", "Data Chart" + this.datapoints[i]);
            }
            //this.datapoints[this.datapoints.length - 1] = (((0.0f - gMinValue) * (1.0f - (-1.0f))/(gMaxValue - gMinValue)) + (-1));

        }

        if (back.dataMin.length > 0){

            back.copyTo(this.back);

            gMaxValue = 280.0f;
            gMinValue =-1.0f;


            for (int i = 0; i < this.back.dataMin.length; i++){
                this.back.dataMin[i] = (((back.dataMin[i] - gMinValue) * (1.0f - (-1.0f))/(gMaxValue - gMinValue)) + (-1));
                this.back.dataMax[i] = (((back.dataMax[i] - gMinValue) * (1.0f - (-1.0f))/(gMaxValue - gMinValue)) + (-1));

            }

        }
        isUpdating = false;

    }

    class Task implements Runnable {
        @Override
        public void run() {
            Thread thread = Thread.currentThread();
            System.out.println("RealtimeCharSurfaceView is being run by " + thread.getName() + " (" + thread.getId() + ")");
            while (true){
                if (!isUpdating){
                    parent.offset = chartRenderer.offset_filtered;
                    //chartRenderer.chartDataMin = dataMin;
                    //chartRenderer.chartDataMax = dataMax;
                    main.copyTo(chartRenderer.main);
                    back.copyTo(chartRenderer.back);
                    requestRender();
                }

                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private float mPreviousX=0,mPreviousY=0;
    private long oldTime=0;
    private boolean pressed = false, moved = false;

    private int mActivePointerId;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Process touch events from surface
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        mScaleDetector.onTouchEvent(ev);

       /* float x = e.getX();
        float y = e.getY();
        long time = System.currentTimeMillis();

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;moved = false;

                break;
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                float dt = (float) (time - oldTime)/8;
                chartRenderer.momentum += 0.1*dx/(dt*dt);

                if (Math.abs(dx) > 5.0f) {
                    chartRenderer.f.setFriction(Filter.NOMINAL_FRICTION);
                } else {
                    chartRenderer.f.setFriction(0.8);
                }
                break;
            case MotionEvent.ACTION_UP:
               // chartRenderer.f.setFriction(Filter.NOMINAL_FRICTION);
                moved=pressed=false;
                break;


        }*/

        final int action = ev.getAction();
        long time = System.currentTimeMillis();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mPreviousX = x;
                mPreviousY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }
        case MotionEvent.ACTION_MOVE: {
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        final float x = ev.getX(pointerIndex);
        final float y = ev.getY(pointerIndex);

        // Only move if the ScaleGestureDetector isn't processing a gesture.
        if (!mScaleDetector.isInProgress()) {
            final float dx = x - mPreviousX;
            final float dy = y - mPreviousY;

            float dt = (float) (time - oldTime)/8;
            chartRenderer.momentum += 0.1*dx/(dt*dt);

            if (Math.abs(dx) > 5.0f) {
                chartRenderer.f.setFriction(Filter.NOMINAL_FRICTION);
            } else {
                chartRenderer.f.setFriction(0.8);
            }
            //mPosY += dy;

            invalidate();
        }

        mPreviousX = x;
        mPreviousY = y;

        break;
    }

    case MotionEvent.ACTION_UP: {
        mActivePointerId = INVALID_POINTER_ID;
        break;
    }

    case MotionEvent.ACTION_CANCEL: {
        mActivePointerId = INVALID_POINTER_ID;
        break;
    }

    case MotionEvent.ACTION_POINTER_UP: {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mPreviousX = ev.getX(newPointerIndex);
            mPreviousY = ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
        break;
    }
}


       // mPreviousX = x;
       // mPreviousY = y;
        oldTime = time;
        return true;
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            float focus = detector.getFocusX();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            parent.setDivider(mScaleFactor);

            invalidate();
            return true;
        }
    }
}