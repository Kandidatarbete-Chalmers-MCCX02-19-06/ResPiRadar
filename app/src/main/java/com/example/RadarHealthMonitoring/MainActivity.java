package com.example.RadarHealthMonitoring;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.example.RadarHealthMonitoring.Bluetooth.b;
import static com.example.RadarHealthMonitoring.ConnectedThread.READ_VALUE;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothAutoConnect;


/**
 * Huvudaktiviteten för appen
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    //static MainActivity m; // for static activity
    private static final String msg = "MainActivity";
    boolean measurementRunning = false;
    static MenuItem bluetoothMenuItem;
    Button startStoppMeasureButton;
    TextView pulseValueView;
    TextView breathValueView;
    Intent intentBluetooth;

    private final int REQUEST_FINE_LOCATION = 2;

    private double dataNumber = 0;
    private Graph graphPulse;
    private Graph graphBreathe;
    double yPulse;
    double yBreathe;
    DataPoint dataPulse;
    DataPoint dataBreathe;

    HandlerThread handlerThread;
    Looper looper;
    static Handler handler;

    /**
     * On start up: creates the graphs and starts the Bluetooth service that auto connects to
     * a Raspberry Pi with bluetooth
     * The MainActivity hosts the graphs, a button to start measuring pulse and breath rate,
     * an options menu with settings, a reset button and a button/indicator that shows the connection state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //m = MainActivity.this;
        PreferenceManager.setDefaultValues(this, R.xml.settings, false); // så systemet inte sätter default
        startStoppMeasureButton = findViewById(R.id.startStoppMeasureButton); // Start button
        pulseValueView = findViewById(R.id.pulseValueView);
        breathValueView = findViewById(R.id.breathValueView);

        /* Graphs */
        graphPulse = new Graph(findViewById(R.id.graphPulse),getApplicationContext(),getResources().getColor(R.color.colorGraphPulse));
        graphBreathe = new Graph(findViewById(R.id.graphBreathe),getApplicationContext(),getResources().getColor(R.color.colorGraphBreath));
        /* Bluetooth */
        intentBluetooth = new Intent(this, Bluetooth.class);
        startService(intentBluetooth);

        /*handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        looper = handlerThread.getLooper();
        handler = new Handler(looper);*/

        IntentFilter intentFilterRequestPermission = new IntentFilter(Bluetooth.REQUEST_PERMISSION);
        registerReceiver(PermissionBroadcastReceiver, intentFilterRequestPermission);
        IntentFilter intentFilterBluetoothIcon = new IntentFilter(Bluetooth.SET_BLUETOOTH_ICON);
        registerReceiver(BluetoothIconBroadcastReceiver, intentFilterBluetoothIcon);
        IntentFilter intentFilterReadData = new IntentFilter(ConnectedThread.READ);
        registerReceiver(ReadDataBroadcastReceiver, intentFilterReadData);
        IntentFilter intentFilterToast = new IntentFilter(Bluetooth.TOAST);
        registerReceiver(ToastBroadcastReceiver, intentFilterToast);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(intentBluetooth);
        measurementRunning = false;
        unregisterReceiver(PermissionBroadcastReceiver);
        unregisterReceiver(BluetoothIconBroadcastReceiver);
        unregisterReceiver(ReadDataBroadcastReceiver);
        unregisterReceiver(ToastBroadcastReceiver);

        //Log.d(msg,"connectedThread " + b.connectedThread.isAlive());
        /*try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        try {
            Log.d(msg, "connectedThread " + b.connectedThread.isAlive() + " interuppted " + b.connectedThread.isInterrupted() + " deamon " + b.connectedThread.isDaemon());
            Log.d(msg, "connectThread " + b.connectThread.isAlive());

            try {
                b.connectedThread.join();
            } catch (InterruptedException e) { // TODO CHECK
                e.printStackTrace();
            }
        } catch (NullPointerException e) {
        }
        try {
            Log.d(msg, "connectedThread " + b.connectedThread.isAlive() + " interuppted " + b.connectedThread.isInterrupted() + " deamon " + b.connectedThread.isDaemon());
            Log.d(msg, "connectThread " + b.connectThread.isAlive());
        } catch (NullPointerException e) {

        }
    }

    /**
     * Creates a options menu with settings, a reset button and a button/indicator that shows the connection state
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        bluetoothMenuItem = menu.findItem(R.id.bluetooth);
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
                            //b.bluetoothDisconnected(false); // TODO undersök
                            b.bluetoothAutoConnectChecked = false;
                            if (b.bluetoothSettingsActive) {
                                bluetoothAutoConnect.setChecked(false);
                            }
                        }
                    }
                }
                return true;
            case R.id.reset_graphs:
                graphPulse.resetSeries();
                graphBreathe.resetSeries();
                dataNumber = 0;
            default:
                return super.onOptionsItemSelected(item);
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
            loopAddData();
        } else {
            startStoppMeasureButton.setBackgroundColor(getResources().getColor(R.color.colorMeasureButtonOn));
            startStoppMeasureButton.setText("Start Measure");
        }
        measurementRunning = !measurementRunning;
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
    public void addData() {        //knapp för att lägga till ett värde till serien
        yPulse = Math.sin(dataNumber*3/30)/2+70+Math.random()/2;
        yBreathe = Math.sin(dataNumber/20)/2+22+Math.random()/3;
        dataPulse = new DataPoint(dataNumber,yPulse);
        dataBreathe = new DataPoint(dataNumber,yBreathe);
        dataNumber += 0.5;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphPulse.getSeries().appendData(dataPulse, true,1000); // seriesPulse
                pulseValueView.setText("Pulse: " + String.format("%.1f",yPulse));
                graphBreathe.getSeries().appendData(dataBreathe, true,1000); // seriesBreathe
                breathValueView.setText("Breath rate: " + String.format("%.1f",yBreathe));
            }
        });
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
            if (Bluetooth.REQUEST_PERMISSION.equals(action)) {
                Log.d(msg, "got intent request permission");
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            }
        }
    };

    public BroadcastReceiver BluetoothIconBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Bluetooth.SET_BLUETOOTH_ICON.equals(action)) {
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
            if (Bluetooth.TOAST.equals(action)) {
                //String text = intent.getStringExtra(b.TEXT);
                Toast.makeText(getApplicationContext(), intent.getStringExtra(b.TEXT), Toast.LENGTH_LONG).show();
            }
        }
    };

    public BroadcastReceiver ReadDataBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectedThread.READ.equals(action)) {
                String value = intent.getStringExtra(READ_VALUE);
                //Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
