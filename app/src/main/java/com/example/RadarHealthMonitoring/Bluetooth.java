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
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

import androidx.annotation.Nullable;

import static com.example.RadarHealthMonitoring.MainActivity.bluetoothMenuItem;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothAutoConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothList;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothOn;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothSearch;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothWrite;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bs;
import static com.example.RadarHealthMonitoring.Settings.s;

public class Bluetooth extends Service {

    static Bluetooth b; // for static service
    private final String TAG = "Bluetooth";
    private final int REQUEST_FINE_LOCATION = 2;
    ConnectThread connectThread;
    ConnectedThread connectedThread;
    Handler handler = new Handler();
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice activeDevice;
    Set<BluetoothDevice> pairedDevices;
    int chosenDeviceIndex;
    String raspberryPiName = "raspberrypi";
    final String raspberryPiMAC = "B8:27:EB:FC:22:65";
    boolean autoConnect = false;
    boolean connected = false;
    boolean foundRaspberryPi = false;
    int connectAttempt = 1;
    int searchAttempts = 1;
    int delay;
    boolean startBluetooth = false;

    MainActivity mainActivity;

    private static Handler uiHandler;// = new Handler(Looper.getMainLooper()); // static handler
    static
    {
        uiHandler = new Handler(Looper.getMainLooper());
    }
    public static void runOnUI(Runnable runnable) {
        uiHandler.post(runnable);
    }

    // Boolean indicators for BluetoothSettings
    boolean bluetoothSettingsActive = false;
    boolean bluetoothOnChecked = false;
    boolean bluetoothListEnable = false;
    boolean bluetoothSearchEnable = false;
    boolean bluetoothConnectEnable = false;
    boolean bluetoothWriteEnable = false;
    boolean bluetoothSearchChecked = false;
    boolean bluetoothConnectChecked = false;
    boolean bluetoothAutoConnectChecked = false;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // Constants for Broadcast
    public static final String REQUEST_PERMISSION = "REQUEST_PERMISSION";
    public static final String SET_BLUETOOTH_ICON = "SET_BLUETOOTH_ICON";
    static public final String TOAST = "TOAST";
    public final String ICON = "ICON";
    public final String TEXT = "TEXT";

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
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BluetoothBroadcastReceiverBondChange, BTIntentBondChange);
        startBluetooth(true); // Autostart bluetoth on start up

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
        unregisterReceiver(BluetoothBroadcastReceiverBondChange);
        startBluetooth(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    // ########## ########## Methods ########## ##########

    /**
     * Starts or disables Bluetooth
     * Starts at app start up
     */
    boolean startBluetooth(boolean start) {
        if (start) {
            startBluetooth = true; // for ending threads on destroy
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
                        if (connectThread.isAlive()) {
                            // connectedThread.cancel(); // redudant?
                            connectThread.cancel();
                        }
                    }
                }
            }
        } else {
            startBluetooth = false;
            if (bluetoothAdapter != null) {
                if (connectThread != null) {
                    if (connectThread.isAlive()) {
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

    /**
     * Method to be called when bluetooth is on
     * Auto connects to Raspberry Pi
     */
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

    /**
     * Auto connects to Raspberry Pi
     * Search and bonds to Raspberry Pi if needed
     * Attempts to search and connect two times
     */
    void autoConnect() {
        autoConnect = true; // eventually remove
        connectAttempt = 1;
        searchAttempts = 1;
        bluetoothAutoConnectChecked = true;
        if (bluetoothSettingsActive) {
            bluetoothAutoConnect.setChecked(true);
        }
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
            }
            Log.d(TAG, "start discovery isRaspberryPi: " + isRaspberryPi);
        } else {
            if (activeDevice.getBondState() != 12) {
                startDiscovery();
            } else {
                connectBluetooth(true);
            }
        }
    }

    /**
     * Connects to Raspberry Pi with bluetooth rfcomm serial communication
     * @param connect used to differ auto connect to regular connect with boolean autoConnect
     */
    void connectBluetooth(boolean connect) { // TODO Callback Manager
        if (connect) {
            if (!(activeDevice == null)) {
                if (connectThread != null) {
                    if (!connectThread.hasSocket()) { // do nothing if already has socket
                        connectThread = new ConnectThread(activeDevice);
                        Log.d(TAG, "Create Refcomm Socket: " + activeDevice.getName());
                    }
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectThread = new ConnectThread(activeDevice);
                                Log.d(TAG, "Create Refcomm Socket: " + activeDevice.getName());
                            }
                        }
                    }, 1);

                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!connected) {
                            try {
                                connectThread.start();
                                Log.d(TAG, "Run thread");
                            } catch (IllegalThreadStateException e) {
                                Log.e(TAG, "Thread already started",e);
                            }
                        }
                    }
                }, 2);
            }
        }
    }

    /**
     * Method for handle not succeeded connections at connectThread
     * Only used with auto connect
     * Tries to connect a second time, then it searches up to two times for Raspberry Pi
     * If Raspberry Pi found, it bonds and connects with two attempts
     */
    void connectManager() { // If device did not connect
        if (autoConnect && startBluetooth) {
            Log.d(TAG, "connectManager: " + connectAttempt);
            switch (connectAttempt) {
                case 1:
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectAttempt ++;
                                connectBluetooth(true);
                            }
                        }
                    }, 100);
                    break;
                case 2:
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectAttempt ++;
                                startDiscovery();
                            }
                        }
                    }, 100);
                    break;
                case 3:
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectAttempt ++;
                                connectBluetooth(true);
                            }
                        }
                    }, 100);
                    break;
                default :
                    Intent intent = new Intent(Bluetooth.TOAST);
                    intent.putExtra(TEXT,"Error: Couldn't connect to Raspberry Pi");
                    sendBroadcast(intent);
                    bluetoothAutoConnectChecked = false;
                    if (bluetoothSettingsActive) {
                        s.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                bluetoothAutoConnect.setChecked(false);
                            }
                        });
                    }
            }
        }
    }

    /**
     * Method to be called when connected
     */
    synchronized void bluetoothConnected() {
        b.bluetoothConnectChecked = true;
        b.bluetoothAutoConnectChecked = true;
        b.bluetoothWriteEnable = true;
        b.connected = true;
        b.bluetoothSearchEnable = false;
        Intent intent = new Intent(Bluetooth.SET_BLUETOOTH_ICON);
        intent.putExtra(ICON,"ic_bluetooth_connected_white_24dp");
        sendBroadcast(intent);
        if (b.bluetoothSettingsActive) {
            s.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bluetoothConnect.setChecked(true);
                    bluetoothAutoConnect.setChecked(true);
                    bluetoothSearch.setEnabled(false);
                    bluetoothWrite.setEnabled(true);
                }
            });
        }
    }

    /**
     * Method to be called when disconnected
     * @param uiThread Runs on the uiThread i true, needed for update ui components from another thread
     */
    synchronized void bluetoothDisconnected(boolean uiThread) {
        b.connected = false;
        b.bluetoothConnectChecked = false;
        b.bluetoothAutoConnectChecked = false;
        b.bluetoothSearchEnable = true;
        b.bluetoothWriteEnable = false;
        Intent intent = new Intent(Bluetooth.SET_BLUETOOTH_ICON);
        intent.putExtra(ICON,"ic_bluetooth_white_24dp");
        sendBroadcast(intent);
        if (b.bluetoothSettingsActive && !uiThread) {
            bluetoothConnect.setChecked(false);
            bluetoothAutoConnect.setChecked(false);
            bluetoothSearch.setEnabled(true);
            bluetoothWrite.setEnabled(false);
        } else if (b.bluetoothSettingsActive && uiThread) { // TODO Undersök
            //bs.connectedThreadDisconnect();
            s.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bluetoothConnect.setChecked(false);
                    bluetoothAutoConnect.setChecked(false);
                    bluetoothSearch.setEnabled(true);
                    bluetoothWrite.setEnabled(false);
                }
            });
        }
    }

    /**
     * Start searching for Raspberry Pi with bluetooth
     * Needs Fine Location, will requests it if needed
     * When auto connect, has two attempts to search with searchManager
     * Bonds when found and connects if autoConnect
     */
    void startDiscovery() {
        foundRaspberryPi = false;
        if (hasLocationPermissions()) {
            bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Enable search");
        } else {
            requestLocationPermission();
        }
    }

    /**
     * Method for handle not succeeded discoveries/searches
     * Tries to search after Raspberry Pi another time of autoConnect
     */
    void searchManager() {
        if (autoConnect && startBluetooth) {
            Log.d(TAG, "SearchManager: " + searchAttempts);
            if (searchAttempts < 2) { // will search max 2 times if RPI not found
                searchAttempts++;
                startDiscovery();
            } else {
                bluetoothAutoConnectChecked = false;
                if (bluetoothSettingsActive) {
                    bluetoothAutoConnect.setChecked(false);
                }
                Toast.makeText(getApplicationContext(), "Did not find Raspberry Pi", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Checks the location permissions
     * @return true if already has permission
     */
    boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests for Fine Location permission
     * The results are handled either at MainActivity if active or BluetoothSettings if active
     * A broadcast is sent to MainActivity if handled there
     */
    void requestLocationPermission() {
        if (bluetoothSettingsActive) {
            bs.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        } else {
            Intent intent = new Intent(Bluetooth.REQUEST_PERMISSION);
            sendBroadcast(intent);
        }
    }

    /**
     * Updates the paired devices the system already has
     */
    void updatePairedDevices() {
        pairedDevices = bluetoothAdapter.getBondedDevices();
    }

    /**
     * To set the activeDevice (the device to be connected to) when changed at the bluetoothList
     */
    void setActiveDevice() {
        if (!(chosenDeviceIndex==-1)) {
            activeDevice = (BluetoothDevice)pairedDevices.toArray()[chosenDeviceIndex];
            if (bluetoothSettingsActive) {
                bluetoothList.setValue(raspberryPiMAC);
                bluetoothList.setSummary(bluetoothList.getEntry());
            }
        }
    }

    /**
     * Checks if the device is Raspberry Pi
     * @param device the device to compare with
     * @return true if the devices name or hard ware address equals the Raspberry Pi's
     */
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

    /**
     * Starts a new thread to get a flashing bluetooth symbol at the options menu when connecting
     * Updates the symbol every 500 ms
     * Sends broadcast to MainActivity to update the icons
     */
    synchronized void uiBluetoothConnecting() {
        Thread uiBluetoothConnectingThread = new Thread() {
            @Override
            public void run() {
                boolean blueIC = false;
                while (connectThread.isRunning() && connectThread.isAlive()) {
                    Log.d(TAG, "connectThread is alive " + blueIC);
                    Intent intent = new Intent(Bluetooth.SET_BLUETOOTH_ICON);
                    if (blueIC) {
                        intent.putExtra(ICON,"ic_bluetooth_white_24dp");
                    } else {
                        intent.putExtra(ICON,"ic_bluetooth_blue_24dp");
                    }
                    sendBroadcast(intent);
                    blueIC = !blueIC;
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (!connected && blueIC && !bluetoothSearchChecked) {
                    Intent intent = new Intent(Bluetooth.SET_BLUETOOTH_ICON);
                    intent.putExtra(ICON,"ic_bluetooth_white_24dp");
                    sendBroadcast(intent);
                }
            }
        };
        uiBluetoothConnectingThread.start();
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
                        bluetoothConnectChecked = false;
                        bluetoothAutoConnectChecked = false;
                        bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_disabled_gray_24dp);
                        if (bluetoothSettingsActive) {
                            bluetoothOn.setChecked(false);
                            bluetoothOn.setTitle("Bluetooth Off");
                            bluetoothList.setEnabled(false);
                            bluetoothSearch.setEnabled(false);
                            bluetoothConnect.setEnabled(false);
                            bluetoothAutoConnect.setEnabled(false);
                            bluetoothConnect.setChecked(false);
                            bluetoothAutoConnect.setChecked(false);
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothOn();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        bluetoothOnChecked = true;
                        bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
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
                    foundRaspberryPi = true;
                    if (bluetoothSettingsActive) {
                        bluetoothList.setValue(raspberryPiMAC);
                        bluetoothList.setSummary(bluetoothList.getEntry());
                    }
                    bluetoothAdapter.cancelDiscovery();
                    if (activeDevice.getBondState()!= 12) { // Not bonded: 10, Bonding: 11, Bonded: 12
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
                bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_searching_white_24dp);
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
                bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
                if (bluetoothSettingsActive) {
                    bluetoothSearch.setChecked(false);
                }
                if (!foundRaspberryPi) {
                    searchManager();
                    if (!autoConnect) {
                        Toast.makeText(context, "Did not find Raspberry Pi", Toast.LENGTH_LONG).show();
                    }
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
                            if (!connected) {
                                connectBluetooth(autoConnect);
                            }
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
