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
    Button startStoppMeasureButton;
    TextView pulseValueView;
    TextView breathValueView;
    Intent intentBluetooth;
    static long startTime;
    boolean firstStartMeasurement = true;
    static int screenWidth;

    boolean waitSetScreenOrientationRunning = false;
    Handler handler = new Handler();

    static boolean startRealTimeBreathingWithRotate = false;

    // Booleans for taping
    boolean isTapingPulse = false;
    boolean tapWaitLoopRunningPulse = false;
    boolean newTapPulse = false;
    boolean isTapingBreath = false;
    boolean tapWaitLoopRunningBreath = false;
    boolean newTapBreath = false;

    // fix the graph view bug
    boolean firstDataPulse = true;
    boolean firstDataBreath = true;
    boolean resume = false;
    static int scrollToEnd = 100;
    ArrayList<DataPoint> dataPointsPulse = new ArrayList<>();
    ArrayList<DataPoint> dataPointsBreath = new ArrayList<>();

    private final int REQUEST_FINE_LOCATION = 2;

    private double dataNumber = 0;
    private Graph graphPulse;
    private Graph graphBreathe;
    double yPulse;
    double yBreathe;
    DataPoint dataPulse;
    DataPoint dataBreathe;

    static Display display;

    /*HandlerThread handlerThread;
    Looper looper;
    static Handler handler;*/

    /**
     * On start up: creates the graphs and starts the BluetoothService service that auto connects to
     * a Raspberry Pi with bluetooth
     * The MainActivity hosts the graphs, a button to start measuring pulse and breath rate,
     * an options menu with settings, a reset button and a button/indicator that shows the connection state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false); // så systemet inte sätter default
        startStoppMeasureButton = findViewById(R.id.startStoppMeasureButton); // Start button
        pulseValueView = findViewById(R.id.pulseValueView);
        breathValueView = findViewById(R.id.breathValueView);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        screenWidth =  getResources().getDisplayMetrics().widthPixels;

        /* Graphs */
        graphPulse = new Graph(findViewById(R.id.graphPulse),getApplicationContext(),
                getResources().getColor(R.color.colorGraphPulse), true, screenWidth);
        graphBreathe = new Graph(findViewById(R.id.graphBreathe),getApplicationContext(),
                getResources().getColor(R.color.colorGraphBreath), true, screenWidth);
        graphPulse.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                isTapingPulse = true;
                if (!tapWaitLoopRunningPulse) {
                    tapWaitLoopPulse();
                } else {
                    newTapPulse = true;
                }
            }
        });
        graphBreathe.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Viewport.OnXAxisBoundsChangedListener.Reason reason) {
                isTapingBreath = true;
                if (!tapWaitLoopRunningBreath) {
                    tapWaitLoopBreath();
                } else {
                    newTapBreath = true;
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

                // Use TaskStackBuilder to build the back stack and get the PendingIntent
                /*PendingIntent pendingIntent =
                        TaskStackBuilder.create(this)
                                // add all of DetailsActivity's parents to the stack,
                                // followed by DetailsActivity itself
                                .addNextIntentWithParentStack(intentSettings)
                                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                builder.setContentIntent(pendingIntent);
                Intent[] intents = new Intent[1];
                intents[0] = intentSettings;
                startActivities(intents);*/

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
     * Activates from the startStoppMeasureButton
     * @param view from the button
     */
    public void measureOnClick(View view) {
        if (!measurementRunning) {
            startStoppMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOff));
            startStoppMeasureButton.setText("Stop Measure");
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
            startStoppMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOn));
            startStoppMeasureButton.setText("Start Measure");
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

    void tapWaitLoopPulse() {
        Thread tapWaitLoopThread = new Thread() {
            @Override
            public void run() {
                do {
                    newTapPulse = false;
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (newTapPulse);
                tapWaitLoopRunningPulse = false;
                isTapingPulse = false;
            }
        };
        tapWaitLoopRunningPulse = true;
        tapWaitLoopThread.start();
    }

    void tapWaitLoopBreath() {
        Thread tapWaitLoopThread = new Thread() {
            @Override
            public void run() {
                do {
                    newTapBreath = false;
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (newTapBreath);
                tapWaitLoopRunningBreath = false;
                isTapingBreath = false;
            }
        };
        tapWaitLoopRunningBreath = true;
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
        yPulse = Math.sin(dataNumber*3/30)/2+70+Math.random()/2;
        yBreathe = Math.sin(dataNumber/20)/2+22+Math.random()/3;
        dataPulse = new DataPoint(dataNumber,yPulse);
        dataBreathe = new DataPoint(dataNumber,yBreathe);
        dataNumber += 0.5;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!resume) {
                    dataPointsPulse.add(dataPulse);
                    dataPointsBreath.add(dataBreathe);
                    if (dataPointsPulse.size() > maxDataPoints) {
                        dataPointsPulse.remove(0);
                        dataPointsBreath.remove(0);
                    }
                    //Log.d(msg,"dataNumber: " + dataNumber + " datapoins: " + dataPointsPulse.get(dataPointsPulse.size()-1));
                    graphPulse.getSeries().appendData(dataPulse,
                            setGraphViewBounds(dataNumber, graphPulse, isTapingPulse) ||
                                    firstDataPulse, maxDataPoints, isActive); // seriesPulse
                    pulseValueView.setText("Pulse: " + String.format("%.1f", yPulse) + " bpm");
                    graphBreathe.getSeries().appendData(dataBreathe,
                            setGraphViewBounds(dataNumber, graphBreathe, isTapingBreath) ||
                                    firstDataPulse, maxDataPoints, isActive); // seriesBreathe
                    breathValueView.setText("Breath rate: " + String.format("%.1f", yBreathe) + " bpm");
                    if (firstDataPulse) {
                        graphPulse.getViewport().setMinX(0);
                        graphPulse.getViewport().setMaxX(60);
                        graphBreathe.getViewport().setMinX(0);
                        graphBreathe.getViewport().setMaxX(60);
                    }
                    firstDataPulse = false;
                }
            }
        });
    }

    void fixGraphOnReturn() {
        handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(msg,"resume false");
                    graphPulse.fixGraphView(dataPointsPulse.toArray());
                    graphBreathe.fixGraphView(dataPointsBreath.toArray());
                    resume = false;
                    scrollToEnd = 0;
                }
            }, 100);
    }

    public void setPulseData(int pulseData) {
        Log.d(msg,"setPulseData " + pulseData);
        dataPulse = new DataPoint(((System.currentTimeMillis() - startTime)/1000.0),pulseData);
        dataPointsPulse.add(dataPulse);
        if (dataPointsPulse.size() > maxDataPoints) {
            dataPointsPulse.remove(0);
        }
        graphPulse.getSeries().appendData(dataPulse, setGraphViewBounds(
                ((System.currentTimeMillis() - startTime)/1000.0), graphPulse, isTapingPulse),maxDataPoints,isActive);
        pulseValueView.setText("Pulse: " + pulseData + " bpm");
    }

    public void setBreathData(int breathData) {
        dataBreathe = new DataPoint(((System.currentTimeMillis() - startTime)/1000.0),breathData);
        dataPointsBreath.add(dataBreathe);
        if (dataPointsBreath.size() > maxDataPoints) {
            dataPointsBreath.remove(0);
        }
        graphBreathe.getSeries().appendData(dataBreathe, setGraphViewBounds(
                ((System.currentTimeMillis() - startTime)/1000.0), graphBreathe, isTapingBreath),maxDataPoints,isActive);
        breathValueView.setText("Breath rate: " + breathData + " bpm");
    }

    void resetGraph() {
        Log.d(msg,"Reset");
        dataNumber = 0;
        graphPulse.resetSeries();
        graphBreathe.resetSeries();
        pulseValueView.setText("Pulse:   ");
        breathValueView.setText("Breath rate:   ");
        dataPointsPulse = new ArrayList<>();
        dataPointsBreath = new ArrayList<>();
        if (measurementRunning) {
            startTime = System.currentTimeMillis();
        } else {
            firstStartMeasurement = true;
        }
        firstDataPulse = true;
        firstDataBreath = true;
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
                            setPulseData(Integer.parseInt(split[1]));
                            break;
                        case "BR":
                            setBreathData(Integer.parseInt(split[1]));
                            break;
                        case "RTB":
                            if (isActive) {
                                Intent valueIntent = new Intent(REAL_TIME_BREATHING);
                                valueIntent.putExtra(BREATHING_VALUE,Integer.parseInt(split[1]));
                                sendBroadcast(valueIntent);
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
                startStoppMeasureButton.setEnabled(value);
                startStoppMeasureButton.setText("Start Measure");
                measurementRunning = false;
                if (value) {
                    startStoppMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOn));
                    startStoppMeasureButton.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    startStoppMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonDisabled));
                    startStoppMeasureButton.setTextColor(getResources().getColor(R.color.colorMeasureButtonDisabledText));
                }
            }
        }
    };
}
