package com.example.RadarHealthMonitoring;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

import androidx.annotation.Nullable;

import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothAdapter;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothList;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothOn;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothSearch;
import static com.example.RadarHealthMonitoring.Settings.s;

public class Bluetooth extends Service {

    static Bluetooth b; // for static service
    private final String msg = "Bluetooth";
    private final int REQUEST_FINE_LOCATION = 2;

    ConnectThread connectThread;
    Handler handler = new Handler();

    BluetoothDevice activeDevice;
    Set<BluetoothDevice> pairedDevices;
    int chosenDeviceIndex;
    String raspberryPiName = "raspberrypi";
    final String raspberryPiMAC = "B8:27:EB:FC:22:65";
    boolean autoConnect = false;

    // ########## ########## onCreate ########## ##########

    @Override
    public void onCreate() {
        Log.d(msg, "onCreate");
        b = Bluetooth.this;
        // Receivers
        IntentFilter BTIntentChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(BluetoothBroadcastReceiverState, BTIntentChange);
        IntentFilter BTIntentFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(BluetoothBroadcastReceiverAction, BTIntentFound);
        IntentFilter BTIntentSearch = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(BluetoothBroadcastReceiverSearch, BTIntentSearch);
        IntentFilter BTIntentSearchFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothBroadcastReceiverSearchFinished, BTIntentSearchFinished);
        IntentFilter BTIntentScanChange = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(BluetoothBroadcastReceiverScan, BTIntentScanChange);
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BluetoothBroadcastReceiverBondChange, BTIntentBondChange);
    } // end off onCreate

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(msg, "onStartCommand");
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        Log.d(msg, "onDestroy");
        unregisterReceiver(BluetoothBroadcastReceiverState);
        unregisterReceiver(BluetoothBroadcastReceiverAction);
        unregisterReceiver(BluetoothBroadcastReceiverSearch);
        unregisterReceiver(BluetoothBroadcastReceiverSearchFinished);
        unregisterReceiver(BluetoothBroadcastReceiverScan);
        unregisterReceiver(BluetoothBroadcastReceiverBondChange);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(msg, "onBind");
        return null;
    }

    // ########## ########## Program ########## ##########

    /**
     * Starts or disables Bluetooth
     */
    boolean startBluetooth() {
        if (bluetoothAdapter == null) { // Device doesn't support Bluetooth
            Toast.makeText(getApplicationContext(),
                    "Bluetooth Not Supported", Toast.LENGTH_LONG).show();
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) { // Enable Bluetooth
                bluetoothAdapter.enable();
            } else { // Disable Bluetooth
                if (connectThread != null) {
                    if (connectThread.isRunning()) {
                        connectThread.cancel();
                    }
                }
                bluetoothAdapter.disable();
            }
            return false;
        }
    }

    void connectBluetooth(boolean connect) { // TODO Callback Manager
        if (connect) {
            if (!(activeDevice == null)) {
                if (connectThread != null) {
                    if (!connectThread.hasSocket()) { // do nothing if already has socket
                        connectThread = new ConnectThread(activeDevice);
                        Log.d(msg, "Create Refcomm Socket: " + activeDevice.getName());
                    }
                } else {
                    connectThread = new ConnectThread(activeDevice);
                    Log.d(msg, "Create Refcomm Socket: " + activeDevice.getName());
                }
                connectThread.run();
                Log.d(msg, "Run thread");
            }
        }
    }

    void autoConnect() {
        autoConnect = true;
        Log.d(msg, "activeDevice" + activeDevice);
        boolean isRaspberryPi = isRaspberryPi(activeDevice);
        Log.d(msg, "start isRaspberryPi: " + isRaspberryPi);
        if (!isRaspberryPi) {
            updatePairedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (isRaspberryPi(device)) {
                    activeDevice = device;
                    isRaspberryPi = true;
                    bluetoothList.setValue(raspberryPiMAC);
                    bluetoothList.setSummary(bluetoothList.getEntry());
                    break;
                }
            }
            Log.d(msg, "update device isRaspberryPi: " + isRaspberryPi);
            if (!isRaspberryPi) {
                startDiscovery();
            } else {
                Log.d(msg, "bonded: " + activeDevice.getBondState());
                connectBluetooth(true);
            }
            Log.d(msg, "start discovery isRaspberryPi: " + isRaspberryPi);
        } else {
            if (activeDevice.getBondState() != 12) {
                startDiscovery();
            } else {
                connectBluetooth(true);
            }
        }
    }

    void startDiscovery() {
        if (hasLocationPermissions()) {
            bluetoothAdapter.startDiscovery();
            Log.d(msg, "Enable search");
        } else {
            requestLocationPermission();
        }
    }

    void updatePairedDevices() {
        pairedDevices = bluetoothAdapter.getBondedDevices();
    }

    void setActiveDevice() {
        if (!(chosenDeviceIndex==-1)) {
            activeDevice = (BluetoothDevice)pairedDevices.toArray()[chosenDeviceIndex];
            bluetoothList.setValue(raspberryPiMAC);
            bluetoothList.setSummary(bluetoothList.getEntry());
        }
    }

    boolean isRaspberryPi(BluetoothDevice device) {
        if (device != null) {
            if (device.getName() == null) {
                return device.getAddress().equals(raspberryPiMAC);
            } else {
                return (device.getName().equals(raspberryPiName) ||
                        device.getAddress().equals(raspberryPiName) || device.getAddress().equals(raspberryPiMAC));
            }
        } else {
            return false;
        }
    }

    /**
     * Fixar listan med Bluetoothenheter
     */
    void updateBluetoothList() {
        updatePairedDevices();
        if (pairedDevices.size() > 0) {
            CharSequence[] deviceName = new CharSequence[pairedDevices.size()];
            CharSequence[] deviceHardwareAddress = new CharSequence[pairedDevices.size()];
            int i = 0;
            for (BluetoothDevice device : pairedDevices) {
                deviceName[i] = device.getName();
                deviceHardwareAddress[i] = device.getAddress(); // MAC address
                i++;
            }
            bluetoothList.setEntries(deviceName);
            bluetoothList.setEntryValues(deviceHardwareAddress);
            bluetoothList.setSummary(bluetoothList.getEntry());
            if (!bluetoothList.isEnabled()) {
                bluetoothList.setEnabled(true);
            }
        } else {
            bluetoothList.setSummary("No device avalible");
            bluetoothList.setEnabled(false);
        }
    }

    boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void requestLocationPermission() { // TODO Se till att vid automatisk anlutning loopa så den ansluts
        s.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        //log("Requested user enable Location. Try starting the scan again.");
    }

    // ########## ########## BroadcastReceivers ########## ##########

    // ########## ACTION_STATE_CHANGED ##########
    /**
     * Create a BroadcastReceiver for ACTION_STATE_CHANGED.
     */
    public BroadcastReceiver BluetoothBroadcastReceiverState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        bluetoothOn.setChecked(false);
                        bluetoothOn.setTitle("Bluetooth Off");
                        bluetoothList.setEnabled(false);
                        bluetoothSearch.setEnabled(false);
                        bluetoothConnect.setEnabled(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        updateBluetoothList();
                        bluetoothSearch.setEnabled(true);
                        bluetoothConnect.setEnabled(true);
                        autoConnect();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        bluetoothOn.setChecked(true);
                        bluetoothOn.setTitle("Bluetooth On");
                        break;
                }
            }
        }
    };

    // ########## ACTION_FOUND ##########
    public BroadcastReceiver BluetoothBroadcastReceiverAction = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(msg, "Found: " + device.getName() + " " + device.getAddress() + " " + device.getBondState());
                if (isRaspberryPi(device)) { // Raspberry Pi detected
                    Toast.makeText(context, "Found " + device.getName(), Toast.LENGTH_SHORT).show();
                    activeDevice = device;
                    bluetoothList.setValue(raspberryPiMAC);
                    bluetoothList.setSummary(bluetoothList.getEntry());
                    bluetoothAdapter.cancelDiscovery();
                    if (activeDevice.getBondState()!= 12) { // Not bonded: 10, Bonding: 11, Bonded: 12
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(msg, "Trying to bond");
                                activeDevice.createBond();
                            }
                        }, 1); // delay needed
                    } else {
                        Log.d(msg, "search finnished, foudn RPI, bonded, autoconnect: " + autoConnect);
                        connectBluetooth(autoConnect);
                    }
                }
            }
        }
    };

    // ########## ACTION_DISCOVERY ##########
    public BroadcastReceiver BluetoothBroadcastReceiverSearch = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(msg, "Search Started");
                bluetoothSearch.setChecked(true);
            }
        }
    };

    public BroadcastReceiver BluetoothBroadcastReceiverSearchFinished = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(msg,"Finished");
                bluetoothSearch.setChecked(false);
                if (!isRaspberryPi(activeDevice)) {
                    Toast.makeText(context, "Did not find Raspberry Pi", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    // ########## ACTION_SCAN_MODE_CHANGED ##########
    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    public BroadcastReceiver BluetoothBroadcastReceiverScan = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(msg, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(msg, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(msg, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(msg, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED: // TODO egen receiver
                        Log.d(msg, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    // ########## ACTION_BOND_STATE_CHANGED ##########
    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    public BroadcastReceiver BluetoothBroadcastReceiverBondChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(msg, "BroadcastReceiver: BOND_BONDED.");
                    updateBluetoothList();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(msg, "search finnished, foudn RPI, bonded, autoconnect: " + autoConnect);
                            connectBluetooth(autoConnect);
                        }
                    }, 1); // delay needed
                    if (isRaspberryPi(activeDevice)) {
                    }
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(msg, "BroadcastReceiver: BOND_BONDING.");
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(msg, "BroadcastReceiver: BOND_NONE.");
                    updateBluetoothList();
                }
            }
        }
    };
}
