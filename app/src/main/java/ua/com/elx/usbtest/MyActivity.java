package ua.com.elx.usbtest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class MyActivity extends Activity {

    private final String TAG = MyActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;


    private TextView mTextView;
    private ScrollView mScrollView;

    void addLine(String txt) {

        mTextView.append(txt+'\n');
    }

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    MyActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MyActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };


    private void openPort() {
        addLine("Init start...");
        addLine("...");

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            addLine("no drivers available, exit...");
            return;
        }

// Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            addLine("Connection failed");
            return;
        }

// Read some data! Most have just one port (port 0).
        final List<UsbSerialPort> ports = driver.getPorts();
        addLine("Getting Ports...");
        sPort = ports.get(0);





    }

    private Handler customHandler = new Handler();
    private RealtimeChartSurfaceView glChart;
    private LinearLayout glChartContainer;
    Timer t = new Timer();
   // float[] dataMin = new float[SignalChart.CHART_POINT];
    //float[] dataMax = new float[SignalChart.CHART_POINT];
    DisplayBuffer main = new DisplayBuffer();
    DisplayBuffer back = new DisplayBuffer();
    int datapos = 0;
    public boolean stopped=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTextView = (TextView) findViewById(R.id.list);
        mScrollView = (ScrollView) findViewById(R.id.scroll);
        openPort();

        glChartContainer = (LinearLayout) findViewById(R.id.chartContainer);
        glChart = new RealtimeChartSurfaceView(this,this);
        glChartContainer.addView(glChart, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        glChartContainer.setVisibility(View.VISIBLE);
        //Set the schedule function and rate
        updateTimerThread.run();


        Button resetButton = (Button) findViewById(R.id.resetbutton);
        SeekBar xScale = (SeekBar) findViewById(R.id.seekBar1);
        Switch modeChanger = (Switch) findViewById(R.id.osc_reg);
        Button stopButton = (Button) findViewById(R.id.stopButton);

        final TextView mTextValue = (TextView) findViewById(R.id.scale);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                addLine("StartOp ");
                byte packet[] = {'a',0,120,12,0,4,5,6,7,8,9};
                //byte packet[] = {'a',0,120,12,1,0,1,2,3,0};

                try {
                    sPort.write(packet,100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stopped) {
                    stopped = false;
                } else {
                    stopped = true;
                }
            }
        });
        mTextValue.setText("234");
        xScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mTextValue.setText(String.valueOf(i));
                double scale = Math.pow(1.0472,(double)i); // create more usable scale
                divider = (int)scale;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        modeChanger.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                oscMode = b;
                if (b) {
                    addLine("Osc mode");
                } else {
                    addLine("Reg mode");
                }
            }
        });

    }



    int j = 0;
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int frequency = 10;

            generateBuffers();

            glChart.setChartData(main,back);
            customHandler.postDelayed(this, 0);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            addLine("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                addLine("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(1000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                addLine("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            addLine("Serial device: " + sPort.getClass().getSimpleName());
            byte packet[] = {'a',0,120,12,0,4,5,6,7,8,9};
            //byte packet[] = {'a',0,120,12,1,0,1,2,3,0};

            try {
                sPort.write(packet,100);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    //private int average=0;
    //private int avgcount=0;
    private int divider = 1;
    public void setDivider(float d) {
        divider = (int) d*10 + 1;
        if (divider < 0) divider =1;
    }
   // private float min = 1024f;
   // private float max = -1024f;
    private boolean oscMode = false;


    private int rxpos=0,start_page=0;
    private int RX_LEN = 4*1000000;
    private int [] rxbuffer = new int[RX_LEN];
    private void updateReceivedData(byte[] data) {
        //final String message = HexDump.dumpHexString(data);
       // addLine(message);
        //mScrollView.smoothScrollTo(0, mTextView.getBottom());
        if (stopped) return;
        for (int i=0;i<data.length;i++) {

            int d = data[i]&0xFF;

            rxbuffer[rxpos++] = d;

            start_page++;
           // glChart.chartRenderer.momentum=-23/((float)divider);




        }
    }
    public double offset=0.0f;

    private int offset_to_pix(double offset) {
        float fpoints = (float) SignalChart.CHART_POINT;
        return (int)(fpoints *(-offset))/2;
    }


    private void generateBuffer(DisplayBuffer buf,int start) {
        int rx_ptr=0,out_ptr=0;
        rx_ptr=start*divider;
        float min = 1024f;
        float max = -1024f;
        int avgcount=0;
        if (start < 0) return;
        while (rx_ptr < RX_LEN && out_ptr < SignalChart.CHART_POINT) {


            int d = rxbuffer[rx_ptr++];
            if (d > max) max = d;
            if (d < min) min = d;

            if (avgcount++ >= divider) {
                buf.dataMin[out_ptr] = min;
                buf.dataMax[out_ptr] = max;

                min = 1024f;
                max = -1024f;

                avgcount = 0;
                out_ptr++;


            }
        }
    }

    private void generateBuffers() {



        int start_point = offset_to_pix(offset);
        int end_point = start_point + SignalChart.CHART_POINT ;

        int firstBufferStart = (start_point / SignalChart.CHART_POINT)*SignalChart.CHART_POINT;
        int secondBufferStart = (end_point / SignalChart.CHART_POINT)*SignalChart.CHART_POINT;

        //int step = offset_to_pixels(offset);
        generateBuffer(this.main,firstBufferStart);
        generateBuffer(this.back,secondBufferStart);
        main.block = 2*firstBufferStart / SignalChart.CHART_POINT;
        back.block = 2*secondBufferStart / SignalChart.CHART_POINT;
        int i;

        Log.e(TAG,String.format("main.block= %d back.block=%d",main.block,back.block));


    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, MyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

}
