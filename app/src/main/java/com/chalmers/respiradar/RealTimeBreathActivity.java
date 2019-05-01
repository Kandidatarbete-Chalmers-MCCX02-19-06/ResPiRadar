package com.chalmers.respiradar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Surface;
import android.widget.Toast;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;

import static com.chalmers.respiradar.BluetoothService.b;
import static com.chalmers.respiradar.MainActivity.BREATHING_VALUE;
import static com.chalmers.respiradar.MainActivity.REAL_TIME_BREATHING;
import static com.chalmers.respiradar.MainActivity.measurementRunning;

/**
 * Displays real time breathing amplitude in a graph.
 * Gets the data from MainActivity.
 * Can simulate a sine wave if needed.
 */
public class RealTimeBreathActivity extends AppCompatActivity {

    boolean waitSetScreenOrientationRunning = false;
    Handler handler = new Handler();
    static boolean isActive = false; // if activity is active
    boolean isTapping = false; // if the graph is tapped
    boolean tapWaitLoopRunning = false;
    boolean newTap = false;
    boolean firstValue = false;
    boolean waitToStart = true; // Wait to plot the values, otherwise a bug in the graph will move the graph
    private double dataNumber = 0; // used in simulations
    private Graph graphRealTimeBreath;
    double yRealTimeBreath;
    DataPoint dataRealTimeBreath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_breath);
        setupActionBar();
        isActive = true;
        firstValue = true;
        // Receiver for real time breathing values from MainActivity
        IntentFilter intentFilterBreathing = new IntentFilter(REAL_TIME_BREATHING);
        registerReceiver(BreathingBroadcastReceiver, intentFilterBreathing);
        // Graph
        graphRealTimeBreath = new Graph(findViewById(R.id.graphRealTimeBreath),getApplicationContext(),
                ContextCompat.getColor(getBaseContext(), R.color.colorGraphRespiration), false, MainActivity.screenWidth);
        graphRealTimeBreath.getViewport().setMinX(0);
        graphRealTimeBreath.getViewport().setMaxX(40);
        graphRealTimeBreath.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                // tap listener, used to manually set the view bounds and pause the automatic set of view bounds
                isTapping = true;
                if (!tapWaitLoopRunning) {
                    tapWaitLoop();
                } else {
                    newTap = true;
                }
            }
        });
        // Screen Orientation
        // The screen orientation is landscape. Finishes the activity when rotated to portrait
        switch (MainActivity.display.getRotation()) {
            case Surface.ROTATION_0:
                waitSetScreenOrientationRunning = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() { // wait 3 sec before detecting screen orientation
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        waitSetScreenOrientationRunning = false;
                    }
                }, 3000);
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_90:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_270:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
        }
        if (b.commandSimulate && measurementRunning) {
            loopAddData(); // Starts the simulation
        } else {
            // wait a bit before plotting the data to avoid some issues with the graph changing size.
            // There is still an issue with the graph changing size, probably because of the number of decimals for the first value
            handler.postDelayed(new Runnable() { // delay needed to avoid som graph problems
                @Override
                public void run() {
                    waitToStart = false;
                }
            }, 100);
        }
    } // end of onCreate

    @Override
    public void onDestroy() {
        super.onDestroy();
        isActive = false;
        unregisterReceiver(BreathingBroadcastReceiver);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // The back-to-home button is only available when the activity was created when rotating the device.
            // The application will crash if finish or onBackPress is called and the activity was started with the option menu button.
            if (MainActivity.startRealTimeBreathingWithRotate) {
                onBackPressed();
            } else {
                Toast.makeText(this, getString(R.string.rotate_device), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            finish();
        }
    }

    /**
     * Loop that runs when the graph has been tapped on.
     * Waits 100 ms before the graph can automatically change the view bounds.
     * Loops as long new tap is made.
     */
    void tapWaitLoop() {
        Thread tapWaitLoopThread = new Thread() {
            @Override
            public void run() {
                do {
                    newTap = false;
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (newTap && isActive);
                tapWaitLoopRunning = false;
                isTapping = false;
            }
        };
        tapWaitLoopRunning = true;
        tapWaitLoopThread.start();
    }

    /**
     * Loop to add simulated data to the graph 50 times per second
     */
    void loopAddData() {
        Thread loopAddDataThread = new Thread() {
            @Override
            public void run() {
                do {
                    addData();
                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (measurementRunning && isActive);
            }
        };
        loopAddDataThread.start();
    }

    /**
     * Add simulated data to the graph as a pure sine wave
     */
    void addData() {
        yRealTimeBreath = Math.sin(dataNumber)+1;
        dataRealTimeBreath = new DataPoint(dataNumber,yRealTimeBreath);
        dataNumber += 0.02;
        setGraphViewBounds(dataNumber);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphRealTimeBreath.getSeries().appendData(dataRealTimeBreath, false,1000);
            }
        });
    }

    /**
     * Sets the graph view bounds to reasonable size values.
     * @param xValue current x-value in the series
     */
    void setGraphViewBounds(double xValue) {
        if (!isTapping) {
            double diff = graphRealTimeBreath.getViewport().getMaxX(false) -
                    graphRealTimeBreath.getViewport().getMinX(false);
            if (firstValue && !b.commandSimulate) { // only for the first value
                graphRealTimeBreath.getViewport().setMaxX((System.currentTimeMillis() - MainActivity.startTime)/1000.0 + diff);
                graphRealTimeBreath.getViewport().setMinX((System.currentTimeMillis() - MainActivity.startTime)/1000.0);
                firstValue = false;
            } else if(xValue > graphRealTimeBreath.getViewport().getMinX(false) + diff &&
                    xValue < graphRealTimeBreath.getViewport().getMinX(false) + diff*1.1) {
                // the series is moving out of sight to the right
                graphRealTimeBreath.getViewport().setMaxX(graphRealTimeBreath.getViewport().getMaxX(false) + diff * 0.9);
                graphRealTimeBreath.getViewport().setMinX(graphRealTimeBreath.getViewport().getMinX(false) + diff * 0.9);
            } else if (graphRealTimeBreath.getViewport().getMinX(true) >
                    graphRealTimeBreath.getViewport().getMaxX(false) - 0.2*diff) {
                // the series is disappearing from the left
                graphRealTimeBreath.getViewport().setMaxX(graphRealTimeBreath.getViewport().getMaxX(false) + diff * 0.9);
                graphRealTimeBreath.getViewport().setMinX(graphRealTimeBreath.getViewport().getMinX(false) + diff * 0.9);
            }
        }
    }

    /**
     * Adds real time breathing values to the graph
     * @param breathData real time breathing amplitude as a single value to add to the graph
     */
    void setBreathData(double breathData) {
        setGraphViewBounds((System.currentTimeMillis() - MainActivity.startTime)/1000.0);
        dataRealTimeBreath = new DataPoint(((System.currentTimeMillis() - MainActivity.startTime)/1000.0),breathData);
        graphRealTimeBreath.getSeries().appendData(dataRealTimeBreath, false,1000);
    }

    /**
     * Receives broadcasts from MainActivity vith the real time breathing values
     */
    public BroadcastReceiver BreathingBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (REAL_TIME_BREATHING.equals(action)) {
                if (!waitToStart) {
                    setBreathData(intent.getDoubleExtra(BREATHING_VALUE, 0));
                }
            }
        }
    };
}
