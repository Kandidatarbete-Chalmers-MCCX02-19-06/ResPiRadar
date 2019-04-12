package com.example.RadarHealthMonitoring;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.example.RadarHealthMonitoring.BluetoothService.b;
import static com.example.RadarHealthMonitoring.ConnectedThread.READ_VALUE;
import static com.example.RadarHealthMonitoring.RealTimeBreathActivity.isActive;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothAutoConnect;


/**
 * Main Activity for the Application
 * Visualize respiration rate and heart rate in two graphs
 * Starts a Bluetooth Service to automatically connect to a Raspberry Pi with a radar and
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String msg = "MainActivity";
    static final String REAL_TIME_BREATHING = "REAL_TIME_BREATHING";
    static final String BREATHING_VALUE = "BREATHING_VALUE";
    static boolean measurementRunning = false;
    static MenuItem bluetoothMenuItem;
    MenuItem realTimeBreathingMenuItem;
    int maxDataPoints = 1000;
    Button startStopMeasureButton;
    TextView heartRateValueView;
    TextView respirationValueView;
    Intent intentBluetooth;
    static long startTime;
    boolean firstStartMeasurement = true;
    static int screenWidth;

    boolean waitSetScreenOrientationRunning = false;
    Handler handler = new Handler();

    static boolean startRealTimeBreathingWithRotate = false;

    // Booleans for taping
    boolean isTapingHeartRate = false;
    boolean tapWaitLoopRunningHeartRate = false;
    boolean newTapHeartRate = false;
    boolean isTapingRespiration = false;
    boolean tapWaitLoopRunningRespiration = false;
    boolean newTapRespiration = false;

    // fix the graph view bug
    boolean firstDataHeartRate = true;
    boolean firstDataRespiration = true;
    boolean resume = false;
    static int scrollToEnd = 100;
    ArrayList<DataPoint> dataPointsHeartRate = new ArrayList<>();
    ArrayList<DataPoint> dataPointsRespiration = new ArrayList<>();

    private final int REQUEST_FINE_LOCATION = 2;

    private double dataNumber = 0;
    private Graph graphHeartRate;
    private Graph graphRespiration;
    double yHeartRate;
    double yRespiration;
    DataPoint dataHeartRate;
    DataPoint dataRespiration;

    static Display display;

    /*HandlerThread handlerThread;
    Looper looper;
    static Handler handler;*/

    /**
     * On start up: creates the graphs and starts the BluetoothService service that auto connects to
     * a Raspberry Pi with bluetooth
     * The MainActivity hosts the graphs, a button to start measuring heartRate and respiration rate,
     * an options menu with settings, a reset button and a button/indicator that shows the connection state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false); // så systemet inte sätter default
        startStopMeasureButton = findViewById(R.id.startStopMeasureButton); // Start button
        heartRateValueView = findViewById(R.id.heartRateValueView);
        respirationValueView = findViewById(R.id.respirationValueView);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        screenWidth =  getResources().getDisplayMetrics().widthPixels;

        /* Graphs */
        graphHeartRate = new Graph(findViewById(R.id.graphHeartRate),getApplicationContext(),
                getResources().getColor(R.color.colorGraphHeartRate), true, screenWidth);
        graphRespiration = new Graph(findViewById(R.id.graphRespiration),getApplicationContext(),
                getResources().getColor(R.color.colorGraphRespiration), true, screenWidth);
        graphHeartRate.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                isTapingHeartRate = true;
                if (!tapWaitLoopRunningHeartRate) {
                    tapWaitLoopHeartRate();
                } else {
                    newTapHeartRate = true;
                }
            }
        });
        graphRespiration.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                isTapingRespiration = true;
                if (!tapWaitLoopRunningRespiration) {
                    tapWaitLoopRespiration();
                } else {
                    newTapRespiration = true;
                }
            }
        });
        /* BluetoothService */
        intentBluetooth = new Intent(this, BluetoothService.class);
        startService(intentBluetooth);

        IntentFilter intentFilterRequestPermission = new IntentFilter(BluetoothService.REQUEST_PERMISSION);
        registerReceiver(PermissionBroadcastReceiver, intentFilterRequestPermission);
        IntentFilter intentFilterBluetoothIcon = new IntentFilter(BluetoothService.SET_BLUETOOTH_ICON);
        registerReceiver(BluetoothIconBroadcastReceiver, intentFilterBluetoothIcon);
        IntentFilter intentFilterReadData = new IntentFilter(ConnectedThread.READ);
        registerReceiver(ReadDataBroadcastReceiver, intentFilterReadData);
        IntentFilter intentFilterToast = new IntentFilter(BluetoothService.TOAST);
        registerReceiver(ToastBroadcastReceiver, intentFilterToast);
        IntentFilter intentFilterResetGraph = new IntentFilter(Settings.BluetoothSettings.RESET_GRAPH);
        registerReceiver(ResetGraphBroadcastReceiver, intentFilterResetGraph);
        IntentFilter intentFilterStartMeasButton = new IntentFilter(BluetoothService.START_MEAS_BUTTON_ENABLE);
        registerReceiver(StartMeasButtonBroadcastReceiver, intentFilterStartMeasButton);

        display = ((WindowManager)
                getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    @Override
    public void onResume() {
        Log.d(msg,"onResume");
        if(isActive) {
            fixGraphOnReturn();
            resume = true;
        }
        if (measurementRunning) {
            if (display.getRotation() == Surface.ROTATION_0) {
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else {
                waitSetScreenOrientationRunning = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (measurementRunning) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        }
                        waitSetScreenOrientationRunning = false;
                    }
                }, 3000);
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        Log.d(msg,"onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(msg,"onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(msg,"onDestroy");
        super.onDestroy();
        stopService(intentBluetooth);
        measurementRunning = false;
        unregisterReceiver(PermissionBroadcastReceiver);
        unregisterReceiver(BluetoothIconBroadcastReceiver);
        unregisterReceiver(ReadDataBroadcastReceiver);
        unregisterReceiver(ToastBroadcastReceiver);
        unregisterReceiver(ResetGraphBroadcastReceiver);
        unregisterReceiver(StartMeasButtonBroadcastReceiver);
    }

    /**
     * Creates a options menu with settings, a reset button and a button/indicator that shows the connection state
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        bluetoothMenuItem = menu.findItem(R.id.bluetooth);
        realTimeBreathingMenuItem = menu.findItem(R.id.real_time);
        realTimeBreathingMenuItem.setEnabled(false);
        if(b.bluetoothOnChecked) {
            bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
        }
        return true;
    }

    /**
     * When a menu item is selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intentSettings = new Intent(this, Settings.class);
                this.startActivity(intentSettings);
                return true;
            case R.id.bluetooth:
                if (!b.bluetoothOnChecked) {
                    b.startBluetooth(true);
                } else {
                    if (!b.bluetoothAutoConnectChecked) {
                        b.autoConnect = true;
                        b.autoConnect();
                    } else {
                        b.autoConnect = false;
                        b.bluetoothAdapter.cancelDiscovery();
                        if (b.connectThread != null) {
                            b.connectThread.cancel();
                        } else {
                            b.bluetoothAutoConnectChecked = false;
                            if (b.bluetoothSettingsActive) {
                                bluetoothAutoConnect.setChecked(false);
                            }
                        }
                    }
                }
                return true;
            case R.id.reset_graphs:
                resetGraph();
                return true;
            case R.id.help_main:
                DialogFragment newFragment = new InformationMainFragment();
                newFragment.show(getSupportFragmentManager(), "help_main");
                return true;
            case R.id.real_time:
                if (measurementRunning) {
                    startRealTimeBreathingWithRotate = false;
                    Intent intentRealTimeBreath = new Intent(this, RealTimeBreathActivity.class);
                    this.startActivity(intentRealTimeBreath);
                }
                return true;
            case R.id.scroll_to_end:
                scrollToEnd = 0;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (measurementRunning) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                startRealTimeBreathingWithRotate = true;
                Intent intentRealTimeBreath = new Intent(this, RealTimeBreathActivity.class);
                this.startActivity(intentRealTimeBreath);
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (!waitSetScreenOrientationRunning) {
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                }
            }
        }
    }

    // ########## ########## Methods ########## ##########

    /**
     * Start/stop the measurement
     * Activates from the startStopMeasureButton
     * @param view from the button
     */
    public void measureOnClick(View view) {
        if (!measurementRunning) {
            startStopMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOff));
            startStopMeasureButton.setText("Stop Measure");
            if (firstStartMeasurement) {
                startTime = System.currentTimeMillis();
            }
            firstStartMeasurement = false;
            if (b.commandSimulate) {
                loopAddData();
            }
            scrollToEnd = 0;
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            realTimeBreathingMenuItem.setEnabled(true);
            if (!b.commandSimulate) {
                String command = "startMeasure";
                byte[] sendCommand = command.getBytes();
                b.connectedThread.write(sendCommand);
            }
        } else {
            startStopMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOn));
            startStopMeasureButton.setText("Start Measure");
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            realTimeBreathingMenuItem.setEnabled(false);
            if (!b.commandSimulate) {
                String command = "stopMeasure";
                byte[] sendCommand = command.getBytes();
                b.connectedThread.write(sendCommand);
            }
        }
        measurementRunning = !measurementRunning;
    }

    void tapWaitLoopHeartRate() {
        Thread tapWaitLoopThread = new Thread() {
            @Override
            public void run() {
                do {
                    newTapHeartRate = false;
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (newTapHeartRate);
                tapWaitLoopRunningHeartRate = false;
                isTapingHeartRate = false;
            }
        };
        tapWaitLoopRunningHeartRate = true;
        tapWaitLoopThread.start();
    }

    void tapWaitLoopRespiration() {
        Thread tapWaitLoopThread = new Thread() {
            @Override
            public void run() {
                do {
                    newTapRespiration = false;
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (newTapRespiration);
                tapWaitLoopRunningRespiration = false;
                isTapingRespiration = false;
            }
        };
        tapWaitLoopRunningRespiration = true;
        tapWaitLoopThread.start();
    }

    boolean setGraphViewBounds(double value, Graph graph, boolean isTaping) {
        if (!isTaping) {
            double diff = graph.getViewport().getMaxX(false) -
                    graph.getViewport().getMinX(false);
            if (scrollToEnd < 20) {
                if (value > graph.getViewport().getMinX(false) + diff) {
                    scrollToEnd ++;
                    return true;
                }
            }
            if (value > graph.getViewport().getMinX(false) + diff && value <
                    graph.getViewport().getMinX(false) + diff*1.1) {
                return true;
            } else if (graph.getViewport().getMinX(true) > graph.getViewport().getMaxX(false) - 0.2*diff) {
                graph.getViewport().setMaxX(graph.getViewport().getMaxX(false) + diff * 0.9);
                graph.getViewport().setMinX(graph.getViewport().getMinX(false) + diff * 0.9);
            }
        }
        return false;
    }

    /**
     * Creates a new thread to create simulated data to the graphs every 500 ms
     */
    void loopAddData() {
        Thread loopAddDataThread = new Thread() {
            @Override
            public void run() {
                do {
                    addData();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (measurementRunning);
                Log.d("main", "Loop Finished");
            }
        };
        loopAddDataThread.start();
    }

    /**
     * Add simulated data to the graps
     */
    public void addData() {
        yHeartRate = Math.sin(dataNumber*3/30)*4+70+Math.random();
        yRespiration = Math.sin(dataNumber/20)*4+22+Math.random();
        dataHeartRate = new DataPoint(dataNumber,yHeartRate);
        dataRespiration = new DataPoint(dataNumber, yRespiration);
        dataNumber += 0.5;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!resume) {
                    dataPointsHeartRate.add(dataHeartRate);
                    dataPointsRespiration.add(dataRespiration);
                    if (dataPointsHeartRate.size() > maxDataPoints) {
                        dataPointsHeartRate.remove(0);
                        dataPointsRespiration.remove(0);
                    }
                    //Log.d(msg,"dataNumber: " + dataNumber + " datapoins: " + dataPointsHeartRate.get(dataPointsHeartRate.size()-1));
                    graphHeartRate.getSeries().appendData(dataHeartRate,
                            setGraphViewBounds(dataNumber, graphHeartRate, isTapingHeartRate) ||
                                    firstDataHeartRate, maxDataPoints, isActive);
                    heartRateValueView.setText("Heart Rate: " + String.format("%.1f", yHeartRate) + " bpm");
                    graphRespiration.getSeries().appendData(dataRespiration,
                            setGraphViewBounds(dataNumber, graphRespiration, isTapingRespiration) ||
                                    firstDataHeartRate, maxDataPoints, isActive);
                    respirationValueView.setText("Respiration rate: " + String.format("%.1f", yRespiration) + " bpm");
                    if (firstDataHeartRate) {
                        graphHeartRate.getViewport().setMinX(0);
                        graphHeartRate.getViewport().setMaxX(60);
                        graphRespiration.getViewport().setMinX(0);
                        graphRespiration.getViewport().setMaxX(60);
                    }
                    firstDataHeartRate = false;
                }
            }
        });
    }

    void fixGraphOnReturn() {
        handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(msg,"resume false");
                    graphHeartRate.fixGraphView(dataPointsHeartRate.toArray());
                    graphRespiration.fixGraphView(dataPointsRespiration.toArray());
                    resume = false;
                    scrollToEnd = 0;
                }
            }, 100);
    }

    public void setHeartRateData(int heartRateData) {
        Log.d(msg,"setHeartRateData " + heartRateData);
        dataHeartRate = new DataPoint(((System.currentTimeMillis() - startTime)/1000.0),heartRateData);
        dataPointsHeartRate.add(dataHeartRate);
        if (dataPointsHeartRate.size() > maxDataPoints) {
            dataPointsHeartRate.remove(0);
        }
        graphHeartRate.getSeries().appendData(dataHeartRate, setGraphViewBounds(
                ((System.currentTimeMillis() - startTime)/1000.0), graphHeartRate, isTapingHeartRate),maxDataPoints,isActive);
        heartRateValueView.setText("Heart Rate: " + heartRateData + " bpm");
    }

    public void setRespirationData(int respirationData) {
        dataRespiration = new DataPoint(((System.currentTimeMillis() - startTime)/1000.0),respirationData);
        dataPointsRespiration.add(dataRespiration);
        if (dataPointsRespiration.size() > maxDataPoints) {
            dataPointsRespiration.remove(0);
        }
        graphRespiration.getSeries().appendData(dataRespiration, setGraphViewBounds(
                ((System.currentTimeMillis() - startTime)/1000.0), graphRespiration, isTapingRespiration),maxDataPoints,false); // TODO try
        respirationValueView.setText("Respiration rate: " + respirationData + " bpm");
    }

    void resetGraph() {
        Log.d(msg,"Reset");
        dataNumber = 0;
        graphHeartRate.resetSeries();
        graphRespiration.resetSeries();
        heartRateValueView.setText("Heart Rate:   ");
        respirationValueView.setText("Respiration rate:   ");
        dataPointsHeartRate = new ArrayList<>();
        dataPointsRespiration = new ArrayList<>();
        if (measurementRunning) {
            startTime = System.currentTimeMillis();
        } else {
            firstStartMeasurement = true;
        }
        firstDataHeartRate = true;
        firstDataRespiration = true;
    }

    // ########## ########## Request Permission ########## ##########
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        if (requestCode == 2) {
            b.startDiscovery();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        if (requestCode == 2) {
            b.bluetoothAutoConnectChecked = false;
            Toast.makeText(getApplicationContext(), "Location Permissions denied", Toast.LENGTH_LONG).show();
        }
    }

    // ########## ########## Broadcast Receivers ########## ##########

    public BroadcastReceiver PermissionBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothService.REQUEST_PERMISSION.equals(action)) {
                Log.d(msg, "got intent request permission");
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            }
        }
    };

    public BroadcastReceiver BluetoothIconBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothService.SET_BLUETOOTH_ICON.equals(action)) {
                String icon = intent.getStringExtra(b.ICON);
                //Log.d(msg,"value: " + icon);
                switch (icon) {
                    case "ic_bluetooth_white_24dp":
                        bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
                        break;
                    case "ic_bluetooth_blue_24dp":
                        bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_blue_24dp);
                        break;
                    case "ic_bluetooth_connected_white_24dp":
                        bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_connected_white_24dp);
                        break;
                }
            }
        }
    };

    public BroadcastReceiver ToastBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothService.TOAST.equals(action)) {
                //String text = intent.getStringExtra(b.TEXT);
                Toast.makeText(getApplicationContext(), intent.getStringExtra(b.TEXT), Toast.LENGTH_LONG).show();
            }
        }
    };

    public BroadcastReceiver ReadDataBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectedThread.READ.equals(action)) {
                if (measurementRunning && !b.commandSimulate) {
                    String value = intent.getStringExtra(READ_VALUE);
                    //Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
                    String split[] = value.split(" ");
                    switch (split[0]) {
                        case "HR":
                            try {
                                setHeartRateData(Integer.parseInt(split[1]));
                            } catch (NumberFormatException e) {
                                Toast.makeText(getApplicationContext(), "Couldn't parse heart rate", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case "RR":
                            try {
                                setRespirationData(Integer.parseInt(split[1]));
                            } catch (NumberFormatException e) {
                                Toast.makeText(getApplicationContext(), "Couldn't parse respiration rate", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case "RTB":
                            if (isActive) {
                                try {
                                    Intent valueIntent = new Intent(REAL_TIME_BREATHING);
                                    valueIntent.putExtra(BREATHING_VALUE, Double.parseDouble(split[1]));
                                    sendBroadcast(valueIntent);
                                } catch (NumberFormatException e) {
                                    Toast.makeText(getApplicationContext(), "Couldn't parse real time breathing", Toast.LENGTH_SHORT).show();
                                }
                            }
                            break;
                        default:
                            Log.d(msg, "Could't extract data" + split);
                    }
                }
            }
        }
    };

    public BroadcastReceiver ResetGraphBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Settings.BluetoothSettings.RESET_GRAPH.equals(action)) {
                resetGraph();
            }
        }
    };

    public BroadcastReceiver StartMeasButtonBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothService.START_MEAS_BUTTON_ENABLE.equals(action)) {
                boolean value = intent.getBooleanExtra(b.ENABLE_BUTTON,true);
                startStopMeasureButton.setEnabled(value);
                startStopMeasureButton.setText("Start Measure");
                realTimeBreathingMenuItem.setEnabled(value);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                measurementRunning = false;
                if (value) {
                    startStopMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOn));
                    startStopMeasureButton.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    startStopMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonDisabled));
                    startStopMeasureButton.setTextColor(getResources().getColor(R.color.colorMeasureButtonDisabledText));
                }
            }
        }
    };
}
