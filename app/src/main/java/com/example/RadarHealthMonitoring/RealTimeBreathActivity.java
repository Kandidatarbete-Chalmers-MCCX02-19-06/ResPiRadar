package com.example.RadarHealthMonitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;

import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;

import static com.example.RadarHealthMonitoring.Bluetooth.b;
import static com.example.RadarHealthMonitoring.MainActivity.BREATHING_VALUE;
import static com.example.RadarHealthMonitoring.MainActivity.REAL_TIME_BREATHING;
import static com.example.RadarHealthMonitoring.MainActivity.measurementRunning;

public class RealTimeBreathActivity extends AppCompatActivity {

    boolean waitLoopRunning;
    static boolean isActive = false;
    boolean isTaping = false;
    boolean tapWaitLoopRunning = false;
    boolean newTap = false;

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
        // Receiver
        IntentFilter intentFilterBreathing = new IntentFilter(REAL_TIME_BREATHING);
        registerReceiver(BreathingBroadcastReceiver, intentFilterBreathing);
        // Graph
        graphRealTimeBreathe = new Graph(findViewById(R.id.graphRealTimeBreathe),getApplicationContext(),
                ContextCompat.getColor(getBaseContext(), R.color.colorGraphBreath), false);
        graphRealTimeBreathe.getViewport().setMinX(0);
        graphRealTimeBreathe.getViewport().setMaxX(10);
        graphRealTimeBreathe.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                Log.d("RTB", "Taped");
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
                waitForOrientation();
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
            loopAddData();
        }
    }

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
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            finish();
        }
    }

    void waitForOrientation() {
        Thread waitLoop = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                waitLoopRunning = false;
            }
        };
        waitLoopRunning = true;
        waitLoop.start();
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
            if (xValue > graphRealTimeBreathe.getViewport().getMinX(false) + diff*0.95 && xValue < graphRealTimeBreathe.getViewport().getMinX(false) + diff*1.05) {
                graphRealTimeBreathe.getViewport().setMaxX(graphRealTimeBreathe.getViewport().getMaxX(false) + diff * 0.9);
                graphRealTimeBreathe.getViewport().setMinX(graphRealTimeBreathe.getViewport().getMinX(false) + diff * 0.9);
            } else if (graphRealTimeBreathe.getViewport().getMinX(true) > graphRealTimeBreathe.getViewport().getMaxX(false) - 0.2*diff) {
                graphRealTimeBreathe.getViewport().setMaxX(graphRealTimeBreathe.getViewport().getMaxX(false) + diff * 0.9);
                graphRealTimeBreathe.getViewport().setMinX(graphRealTimeBreathe.getViewport().getMinX(false) + diff * 0.9);
            }
        }
    }

    void setBreathData(int breathData) {
        setGraphViewBounds(System.currentTimeMillis() - MainActivity.startTime);
        dataRealTimeBreathe = new DataPoint(Math.round((System.currentTimeMillis() - MainActivity.startTime)/1000.0),breathData);
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
