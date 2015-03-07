package ua.com.elx.usbtest;

/**
 * Created by michaelbilenko on 13.10.14.
 */
public class DisplayBuffer {
    public float[] dataMin = new float[SignalChart.CHART_POINT];
    public float[] dataMax = new float[SignalChart.CHART_POINT];

    public int block;

    public void copyTo(DisplayBuffer buf) {
        buf.dataMin = dataMin.clone();
        buf.dataMax = dataMax.clone();
        buf.block = block;
    }

}
