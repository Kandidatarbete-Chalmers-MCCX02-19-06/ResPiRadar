package com.example.RadarHealthMonitoring;

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
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;
import android.widget.Toast;

import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;

import static com.example.RadarHealthMonitoring.BluetoothService.b;
import static com.example.RadarHealthMonitoring.MainActivity.BREATHING_VALUE;
import static com.example.RadarHealthMonitoring.MainActivity.REAL_TIME_BREATHING;
import static com.example.RadarHealthMonitoring.MainActivity.measurementRunning;

public class RealTimeBreathActivity extends AppCompatActivity {

    boolean waitSetScreenOrientationRunning = false;
    Handler handler = new Handler();

    static boolean isActive = false;
    boolean isTaping = false;
    boolean tapWaitLoopRunning = false;
    boolean newTap = false;
    boolean firstValue = false;

    private double dataNumber = 0;
    private Graph graphRealTimeBreathe;
    double yRealTimeBreathe;
    DataPoint dataRealTimeBreathe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_breath);
        setupActionBar();
        isActive = true;
        firstValue = true;
        // Receiver
        IntentFilter intentFilterBreathing = new IntentFilter(REAL_TIME_BREATHING);
        registerReceiver(BreathingBroadcastReceiver, intentFilterBreathing);
        // Graph
        graphRealTimeBreathe = new Graph(findViewById(R.id.graphRealTimeBreathe),getApplicationContext(),
                ContextCompat.getColor(getBaseContext(), R.color.colorGraphBreath), false, MainActivity.screenWidth);
        graphRealTimeBreathe.getViewport().setMinX(0);
        graphRealTimeBreathe.getViewport().setMaxX(10);
        graphRealTimeBreathe.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                isTaping = true;
                if (!tapWaitLoopRunning) {
                    tapWaitLoop();
                } else {
                    newTap = true;
                }

            }
        });
        // Screen Orientation
        switch (MainActivity.display.getRotation()) {
            case Surface.ROTATION_0:
                waitSetScreenOrientationRunning = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
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
            //loopAddData();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loopAddData();
                }
            }, 100);
        }
    } // end of onCreate

    @Override
    public void onDestroy() {
        super.onDestroy();
        isActive = false;
        unregisterReceiver(BreathingBroadcastReceiver);
        Log.d("RTBActivity", "onDestroy");
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  // Ger en fungerande tillbaka-pil
        if (item.getItemId() == android.R.id.home) {
            if (MainActivity.startRealTimeBreathingWithRotate) {
                onBackPressed();
            } else {
                Toast.makeText(this, "Rotate the device to portrait orientation", Toast.LENGTH_LONG).show();
            }
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
                isTaping = false;
            }
        };
        tapWaitLoopRunning = true;
        tapWaitLoopThread.start();
    }

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
     * Add simulated data to the grap
     */
    void addData() {
        yRealTimeBreathe = Math.sin(dataNumber)+1;
        dataRealTimeBreathe = new DataPoint(dataNumber,yRealTimeBreathe);
        dataNumber += 0.02;
        setGraphViewBounds(dataNumber);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphRealTimeBreathe.getSeries().appendData(dataRealTimeBreathe, false,1000); // seriesBreathe
            }
        });
    }

    void setGraphViewBounds(double xValue) {
        if (!isTaping) {
            double diff = graphRealTimeBreathe.getViewport().getMaxX(false) -
                    graphRealTimeBreathe.getViewport().getMinX(false);
            if (firstValue && !b.commandSimulate) {
                graphRealTimeBreathe.getViewport().setMaxX((System.currentTimeMillis() - MainActivity.startTime)/1000.0 + diff);
                graphRealTimeBreathe.getViewport().setMinX((System.currentTimeMillis() - MainActivity.startTime)/1000.0);
                firstValue = false;
            } else if(xValue > graphRealTimeBreathe.getViewport().getMinX(false) + diff && xValue < graphRealTimeBreathe.getViewport().getMinX(false) + diff*1.1) {
                graphRealTimeBreathe.getViewport().setMaxX(graphRealTimeBreathe.getViewport().getMaxX(false) + diff * 0.9);
                graphRealTimeBreathe.getViewport().setMinX(graphRealTimeBreathe.getViewport().getMinX(false) + diff * 0.9);
            } else if (graphRealTimeBreathe.getViewport().getMinX(true) > graphRealTimeBreathe.getViewport().getMaxX(false) - 0.2*diff) {
                graphRealTimeBreathe.getViewport().setMaxX(graphRealTimeBreathe.getViewport().getMaxX(false) + diff * 0.9);
                graphRealTimeBreathe.getViewport().setMinX(graphRealTimeBreathe.getViewport().getMinX(false) + diff * 0.9);
            }
        }
    }

    void setBreathData(int breathData) {
        Log.d("RTB", "setBData " + breathData);
        setGraphViewBounds((System.currentTimeMillis() - MainActivity.startTime)/1000.0);
        dataRealTimeBreathe = new DataPoint(((System.currentTimeMillis() - MainActivity.startTime)/1000.0),breathData);
        graphRealTimeBreathe.getSeries().appendData(dataRealTimeBreathe, false,1000); // seriesPulse
    }

    public BroadcastReceiver BreathingBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (REAL_TIME_BREATHING.equals(action)) {
                setBreathData(intent.getIntExtra(BREATHING_VALUE,0));
            }
        }
    };
}
