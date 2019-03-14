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
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

import androidx.annotation.Nullable;

import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothAutoConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothList;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothOn;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothSearch;
import static com.example.RadarHealthMonitoring.Settings.s;

public class Bluetooth extends Service {

    static Bluetooth b; // for static service
    private final String TAG = "Bluetooth";
    private final int REQUEST_FINE_LOCATION = 2;

    boolean alwaysBind = true;

    ConnectThread connectThread;
    Handler handler = new Handler();

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice activeDevice;
    Set<BluetoothDevice> pairedDevices;
    int chosenDeviceIndex;
    String raspberryPiName = "raspberrypi";
    final String raspberryPiMAC = "B8:27:EB:FC:22:65";
    boolean autoConnect = false;
    boolean connected = false; // TODO
    int connectAttempt = 1;

    // Boolean indicators for BluetoothSettings
    boolean bluetoothSettingsActive = false;
    boolean bluetoothOnChecked= false;
    boolean bluetoothListEnable = false;
    boolean bluetoothSearchEnable = false;
    boolean bluetoothConnectEnable = false;
    boolean bluetoothWriteEnable = false;
    boolean bluetoothSearchChecked = false;
    boolean bluetoothConnectChecked = false;
    boolean bluetoothAutoConnectChecked = false;

    //BluetoothCommunicationService comService;
    ConnectedThread connectedThread;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // ########## ########## onCreate ########## ##########

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        b = Bluetooth.this;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Gets the device's Bluetooth adapter
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
        //comService = new BluetoothCommunicationService();
        //connectedThread = new ConnectedThread();

        startBluetooth(true);

    } // end off onCreate

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(BluetoothBroadcastReceiverState);
        unregisterReceiver(BluetoothBroadcastReceiverAction);
        unregisterReceiver(BluetoothBroadcastReceiverSearch);
        unregisterReceiver(BluetoothBroadcastReceiverSearchFinished);
        unregisterReceiver(BluetoothBroadcastReceiverScan);
        unregisterReceiver(BluetoothBroadcastReceiverBondChange);
        startBluetooth(false);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    // ########## ########## Program ########## ##########

    /**
     * Starts or disables Bluetooth
     */
    boolean startBluetooth(boolean start) {
        if (start) {
            if (bluetoothAdapter == null) { // Device doesn't support Bluetooth
                Toast.makeText(getApplicationContext(),
                        "Bluetooth Not Supported", Toast.LENGTH_LONG).show();
            } else {
                if (!bluetoothAdapter.isEnabled()) { // Enable Bluetooth
                    bluetoothAdapter.enable();
                } else { // bluetooth already on
                    bluetoothOnChecked = true;
                    bluetoothOn();
                    if (connectThread != null) {
                        if (connectThread.isRunning()) {
                            // connectedThread.cancel(); // redudant
                            connectThread.cancel();
                        }
                    }
                }
            }
        } else {
            if (bluetoothAdapter != null) {
                if (connectThread != null) {
                    if (connectThread.isRunning()) {
                        // connectedThread.cancel(); // redudant
                        connectThread.cancel();
                    }
                }
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                }
            }

        }
        return false;
    }

    void bluetoothOn() { // When bluetooth is turned on, or already on at start
        updateBluetoothList();
        bluetoothSearchEnable = true;
        bluetoothConnectEnable = true;
        bluetoothListEnable = true;
        if (bluetoothSettingsActive) {
            bluetoothSearch.setEnabled(true);
            bluetoothConnect.setEnabled(true);
            bluetoothAutoConnect.setEnabled(true);
            bluetoothList.setEnabled(true);
        }
        autoConnect();
    }

    void connectBluetooth(boolean connect) { // TODO Callback Manager
        if (connect) {
            if (!(activeDevice == null)) {
                if (connectThread != null) {
                    if (!connectThread.hasSocket()) { // do nothing if already has socket
                        connectThread = new ConnectThread(activeDevice);
                        Log.d(TAG, "Create Refcomm Socket: " + activeDevice.getName());
                    }
                } else {
                    connectThread = new ConnectThread(activeDevice);
                    Log.d(TAG, "Create Refcomm Socket: " + activeDevice.getName());
                }
                connectThread.start();
                Log.d(TAG, "Run thread");
            }
        }
    }

    void autoConnect() {
        autoConnect = true; // eventually remove
        connectAttempt = 1;
        Log.d(TAG, "activeDevice" + activeDevice);
        boolean isRaspberryPi = isRaspberryPi(activeDevice);
        Log.d(TAG, "start isRaspberryPi: " + isRaspberryPi);
        if (!isRaspberryPi) {
            updatePairedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (isRaspberryPi(device)) {
                    activeDevice = device;
                    isRaspberryPi = true;
                    //bluetoothListIndicator
                    if (bluetoothSettingsActive) {
                        bluetoothList.setValue(raspberryPiMAC);
                        bluetoothList.setSummary(bluetoothList.getEntry());
                    }
                    break;
                }
            }
            Log.d(TAG, "update device isRaspberryPi: " + isRaspberryPi);
            if (!isRaspberryPi) {
                startDiscovery();
            } else {
                Log.d(TAG, "bonded: " + activeDevice.getBondState());
                connectBluetooth(true);
                autoConnectThread();
            }
            Log.d(TAG, "start discovery isRaspberryPi: " + isRaspberryPi);
        } else {
            if (activeDevice.getBondState() != 12) {
                startDiscovery();
            } else {
                connectBluetooth(true);
                autoConnectThread();
            }
        }
    }

    void autoConnectThread() { // If device bonded on the phone but not on the RPI
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!connected) {
                    startDiscovery();
                }
            }
        }, 3000);
    }

    void connectManager() { // If device did not connect
        if (autoConnect) {
            if (connectAttempt == 1) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!connected) {
                            connectAttempt ++;
                            startDiscovery();
                        }
                    }
                }, 200);
            } else if (connectAttempt < 3) {
                connectAttempt++;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!connected) {
                            connectBluetooth(true);
                        }
                    }
                }, 200);
            } else {
                Toast.makeText(getApplicationContext(), "Error: Couldn't connect to Raspberry Pi", Toast.LENGTH_LONG).show();
            }
        }
    }

    void startDiscovery() {
        if (hasLocationPermissions()) {
            bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Enable search");
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
            if (bluetoothSettingsActive) {
                bluetoothList.setValue(raspberryPiMAC);
                bluetoothList.setSummary(bluetoothList.getEntry());
            }
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
        if (bluetoothSettingsActive) {
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
                        bluetoothOnChecked = false;
                        bluetoothSearchEnable = false;
                        bluetoothConnectEnable = false;
                        bluetoothListEnable = false;
                        if (bluetoothSettingsActive) {
                            bluetoothOn.setChecked(false);
                            bluetoothOn.setTitle("Bluetooth Off");
                            bluetoothList.setEnabled(false);
                            bluetoothSearch.setEnabled(false);
                            bluetoothConnect.setEnabled(false);
                            bluetoothAutoConnect.setEnabled(false);
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothOn();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        bluetoothOnChecked = true;
                        if (bluetoothSettingsActive) {
                            bluetoothOn.setChecked(true);
                            bluetoothOn.setTitle("Bluetooth On");
                        }
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
                Log.d(TAG, "Found: " + device.getName() + " " + device.getAddress() + " " + device.getBondState());
                if (isRaspberryPi(device)) { // Raspberry Pi detected
                    Toast.makeText(context, "Found " + device.getName(), Toast.LENGTH_SHORT).show();
                    activeDevice = device;
                    if (bluetoothSettingsActive) {
                        bluetoothList.setValue(raspberryPiMAC);
                        bluetoothList.setSummary(bluetoothList.getEntry());
                    }
                    bluetoothAdapter.cancelDiscovery();
                    if (activeDevice.getBondState()!= 12 || alwaysBind) { // Not bonded: 10, Bonding: 11, Bonded: 12
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Trying to bond");
                                activeDevice.createBond();
                            }
                        }, 1); // delay needed
                    } else {
                        Log.d(TAG, "search finnished, foudn RPI, bonded, autoconnect: " + autoConnect);
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
                Log.d(TAG, "Search Started");
                bluetoothSearchChecked = true;
                if (bluetoothSettingsActive) {
                    bluetoothSearch.setChecked(true);
                }
            }
        }
    };

    public BroadcastReceiver BluetoothBroadcastReceiverSearchFinished = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"Finished");
                bluetoothSearchChecked = false;
                if (bluetoothSettingsActive) {
                    bluetoothSearch.setChecked(false);
                }
                if (!isRaspberryPi(activeDevice)) {
                    Toast.makeText(context, "Did not find Raspberry Pi", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    // ########## ACTION_SCAN_MODE_CHANGED ##########
    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    public BroadcastReceiver BluetoothBroadcastReceiverScan = new BroadcastReceiver() { // TODO Ta bort
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
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
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    updateBluetoothList();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "search finnished, foudn RPI, bonded, autoconnect: " + autoConnect);
                            connectBluetooth(autoConnect);
                        }
                    }, 1); // delay needed
                    if (isRaspberryPi(activeDevice)) {
                    }
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                    updateBluetoothList();
                }
            }
        }
    };

    // ########## ########## bluetoothHandler ########## ##########

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler bluetoothHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case STATE_CONNECTED:
                            Log.d(TAG, "STATE_CONNECTED");
                            // TODO
                            break;
                        case STATE_CONNECTING:
                            Log.d(TAG, "STATE_CONNECTING");
                            break;
                        case STATE_LISTEN:
                        case STATE_NONE:
                            Log.d(TAG, "STATE_NONE");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    //mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);

                    //Toast.makeText(getApplicationContext(), "Connected to "
                    //        + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    break;
                case Constants.MESSAGE_TOAST:

                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();

                    break;
            }
            return true;
        }
    });

}
