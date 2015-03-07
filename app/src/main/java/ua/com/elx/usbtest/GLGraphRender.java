package ua.com.elx.usbtest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.util.Log;


public class GLGraphRender implements Renderer {

    public SignalChart sineChart,backChart;
    public Grid grid;
    GL10 glx;

    public DisplayBuffer main = new DisplayBuffer();
    public DisplayBuffer back = new DisplayBuffer();
    int width;
    int height;
    Context context;
    public Filter f = new Filter(6,Filter.NOMINAL_FRICTION);
    public double momentum=0;
    public double offset=0,offset_filtered=0;
    /** Constructor */
    public GLGraphRender(Context context) {
        this.sineChart = new SignalChart();
        this.backChart = new SignalChart();
        this.context = context;
        this.grid = new Grid();
    }




    @Override
    public void onDrawFrame(GL10 gl) {

        // clear Screen and Depth Buffer
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        // Reset the Modelview Matrix
        gl.glLoadIdentity();
        // Drawing
        //Log.d("Chart Ratio1 "," width " +width + " H " + height);
        gl.glTranslatef(0.0f, 0.0f, -3.0f);     // move 5 units INTO the screen
        // is the same as moving the camera 5 units away
        this.sineChart.setResolution(width, height);
        this.backChart.setResolution(width, height);
        this.grid.setResolution(width,height);



        offset_filtered  = (float)f.doFilter(momentum);

        if (Math.abs(momentum)>0.001) momentum = 0;
        float step = 2.0f/grid.cols;
        float offsetRough = (float)offset_filtered / step;
        offsetRough = (float)Math.floor(offsetRough) * step;
        float offsetFine = (float)offset_filtered - offsetRough;

        //gl.glPushMatrix();
        gl.glTranslatef(offsetFine,0.0f,0.0f);
        grid.draw(gl);
        //gl.glPopMatrix();
        //gl.glTranslatef(offsetRough,0.0f,0.0f);
        gl.glPushMatrix();
        gl.glTranslatef(offsetRough,0.0f,0.0f);
        //gl.glTranslatef((float)offset_filtered,0.0f);
        this.sineChart.setChartData(main);
        this.sineChart.regenBuffers();
        sineChart.draw(gl,0,SignalChart.CHART_POINT);

        this.backChart.setChartData(back);
        this.backChart.regenBuffers();
        backChart.draw(gl,0,SignalChart.CHART_POINT);

        gl.glPopMatrix();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;

        if(height == 0) {                       //Prevent A Divide By Zero By
            height = 1;                         //Making Height Equal One
        }
        gl.glViewport(0, 0, width, height);     //Reset The Current Viewport
        gl.glMatrixMode(GL10.GL_PROJECTION);    //Select The Projection Matrix
        gl.glLoadIdentity();                    //Reset The Projection Matrix

        //Calculate The Aspect Ratio Of The Window
        //Log.d("Chart Ratio2 "," width " +width + " H " + height);
        GLU.gluPerspective(gl, 45.0f, (float) height * 2.0f / (float) width, 0.1f, 100.0f);
        gl.glScalef(0.95f, 0.95f, 1);
        gl.glMatrixMode(GL10.GL_MODELVIEW);     //Select The Modelview Matrix
        gl
                .glLoadIdentity();                    //Reset The Modelview Matrix
        gl.glEnable(gl.GL_LINE_SMOOTH);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }
}


class SignalChart {

    public static int CHART_POINT = 640;
    DisplayBuffer dispBuf;

    int width;
    int height;
    private FloatBuffer vertexBuffer;   // buffer holding the vertices
    private float vertices[] = new float[(int) (CHART_POINT * 6)];




    public void drawRealtimeChart (){
        float verticeInc = 2.0f/CHART_POINT;
        // update x vertrices

        int k = 0;

        for(int i = 0; i < CHART_POINT ; i++) {
                float ci = (float)i;
                vertices[k++] = -1 + (ci * verticeInc);
                vertices[k++] = dispBuf.dataMin[i];
                vertices[k++] = 0.0f;

                vertices[k++] = -1 + (ci * verticeInc);;
                vertices[k++] = dispBuf.dataMax[i];
                vertices[k++] = 0.0f;


        }

        // Debug Chart Value
		/*
		for (int i = 0; i < CHART_POINT * 3; i++){
			Log.d("VERTICES", "test :" + vertices[i]);
		}*/
    }

    /**
     * @param chartData the chartData to set
     */
    public void setChartData(DisplayBuffer main) {
      this.dispBuf=main;



    }
    public void regenBuffers() {
        drawRealtimeChart();
        vertexGenerate();
    }

    public SignalChart() {
       // drawRealtimeChart();
       // vertexGenerate();


    }

    public void vertexGenerate(){
        // a float has 4 bytes so we allocate for each coordinate 4 bytes
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);

        vertexByteBuffer.order(ByteOrder.nativeOrder());
        // allocates the memory from the byte buffer
        vertexBuffer = vertexByteBuffer.asFloatBuffer();
        // fill the vertexBuffer with the vertices
        vertexBuffer.put(vertices);
        // set the cursor position to the beginning of the buffer
        vertexBuffer.position(0);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
    }
    public void setResolution(int width, int height){
        this.width = width;
        this.height = height;
    }



    public void draw(GL10 gl,int digitmin,int digitmax) {

        gl.glPushMatrix();
        float block = (float)dispBuf.block;
        gl.glTranslatef(block,0.0f, 0.0f);

        gl.glViewport(0, 0, width, height);
        // bind the previously generated texture
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        // set the color for the triangle
        gl.glColor4f(0.2f, 0.5f, 0.2f, 0.2f);
        // Point to our vertex buffer
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        // Line width
        gl.glLineWidth(3.0f);
        // Draw the vertices as triangle strip
        gl.glDrawArrays(GL10.GL_LINE_STRIP, digitmin*2, digitmax*2);// CHART_POINT*2
        //Disable the client state before leaving
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }
}

class Grid {

    int width,height;
    public int rows = 4;
    public int cols = 11;
    private float vertices[] = new float[(int) ((rows+cols+4) * 6)];

    private FloatBuffer vertexBuffer;   // buffer holding the vertices



    public void drawRealtimeChart (float offset){
        float verticeInc = 2.0f/cols;
        // update x vertrices

        int k = 0;



        offset = offset / verticeInc;
        offset = offset * verticeInc;

        for(int i = 0; i < (cols+3); i++) {

            vertices[k++] = offset -1 - verticeInc + (i * verticeInc);
            vertices[k++] = -1.0f;
            vertices[k++] = 0.0f;

            vertices[k++] = offset-1 -verticeInc + (i * verticeInc);
            vertices[k++] = 1.0f;
            vertices[k++] = 0.0f;


        }

        verticeInc = 2.0f/rows;
        for(int i = 0; i < (rows+1); i++) {

            vertices[k++] = offset -1.0f-verticeInc;
            vertices[k++] = -1 + (i * verticeInc);
            vertices[k++] = 0.0f;

            vertices[k++] = offset + 1.0f+verticeInc;
            vertices[k++] = -1 + (i * verticeInc);
            vertices[k++] = 0.0f;

        }



    }




    public Grid() {
        drawRealtimeChart(0.0f);
        vertexGenerate();
    }

    public void vertexGenerate(){
        // a float has 4 bytes so we allocate for each coordinate 4 bytes
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);

        vertexByteBuffer.order(ByteOrder.nativeOrder());
        // allocates the memory from the byte buffer
        vertexBuffer = vertexByteBuffer.asFloatBuffer();
        // fill the vertexBuffer with the vertices
        vertexBuffer.put(vertices);
        // set the cursor position to the beginning of the buffer
        vertexBuffer.position(0);


    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
    }
    public void setResolution(int width, int height){
        this.width = width;
        this.height = height;
    }


    public void draw(GL10 gl) {

        //Log.d("Chart Ratio3 "," width " +width + " H " + height);
        gl.glViewport(0, 0, width, height);
        // bind the previously generated texture
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        // set the color for the triangle
        gl.glColor4f(0.1f, 0.1f, 0.1f, 0.2f);
        // Point to our vertex buffer
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        // Line width
        gl.glLineWidth(5.0f);
        // Draw the vertices as triangle strip

        gl.glDrawArrays(GL10.GL_LINES, 0, (rows+cols+4)*2);
        //Disable the client state before leaving
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
