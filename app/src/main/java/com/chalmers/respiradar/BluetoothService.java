package com.chalmers.respiradar;

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
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import java.util.Set;
//import androidx.annotation.Nullable;

import static com.chalmers.respiradar.MainActivity.bluetoothMenuItem;
import static com.chalmers.respiradar.MainActivity.measurementRunning;
import static com.chalmers.respiradar.Settings.BluetoothSettings.bluetoothAutoConnect;
import static com.chalmers.respiradar.Settings.BluetoothSettings.bluetoothConnect;
import static com.chalmers.respiradar.Settings.BluetoothSettings.bluetoothList;
import static com.chalmers.respiradar.Settings.BluetoothSettings.bluetoothOn;
import static com.chalmers.respiradar.Settings.BluetoothSettings.bluetoothSearch;
import static com.chalmers.respiradar.Settings.BluetoothSettings.bs;
import static com.chalmers.respiradar.Settings.s;

/**
 * Service to handle all Bluetooth stuff
 * Starts bluetooth on app startup
 * Auto connects to the Raspberry Pi on startup
 * Will search and try to connect up to two times if needed when auto connecting
 */
public class BluetoothService extends Service {

    static BluetoothService b; // for static service
    private final int REQUEST_FINE_LOCATION = 2;
    ConnectThread connectThread; // runs when try to connect with Bluetooth
    ConnectedThread connectedThread; // runs when connected to read serial data
    Handler handler = new Handler();
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice activeDevice; // the Bluetooth device to connect to (Raspberry Pi)
    Set<BluetoothDevice> pairedDevices; // a list of paired devices on the phone
    int chosenDeviceIndex; // the chosen device in the list
    String raspberryPiName = "raspberrypi"; // the Bluetooth name of Raspberry Pi, default: raspberrypi
    boolean autoConnect = false; // true if the app tries to auto connect to Raspberry Pi
    boolean connected = false; // to show connection state, true if connected
    boolean foundRaspberryPi = false; // if the app found Raspberry Pi
    int connectAttempts = 1; // number of attempts to connect. The app will try to connect two times if it failed the first time
    int searchAttempts = 1; // number of attempts to search. The app will search two times if it did't found Raspberry Pi the first time
    boolean startBluetooth = false; // used to start or stop Bluetooth

    // Boolean indicators for BluetoothSettings. Used when BluetoothSettings starts to set the
    // switches to the currently values.
    boolean bluetoothSettingsActive = false;
    boolean bluetoothOnChecked = false;
    boolean bluetoothListEnable = false;
    boolean commandBluetoothList = false; // when a bluetooth list is active
    boolean bluetoothSearchEnable = false;
    boolean bluetoothConnectEnable = false;
    boolean bluetoothSearchChecked = false;
    boolean bluetoothConnectChecked = false;
    boolean bluetoothAutoConnectChecked = false;
    boolean commandSimulate = false; // when simulation is active instead of real data

    // Constants for Broadcast to MainActivity
    public static final String REQUEST_PERMISSION = "REQUEST_PERMISSION";
    public static final String SET_BLUETOOTH_ICON = "SET_BLUETOOTH_ICON";
    static public final String TOAST = "TOAST";
    public final String ICON = "ICON";
    public final String TEXT = "TEXT";
    static public final String START_MEAS_BUTTON_ENABLE = "START_MEAS_BUTTON_ENABLE";
    public final String ENABLE_BUTTON = "ENABLE_BUTTON";

    // ########## ########## onCreate ########## ##########

    @Override
    public void onCreate() {
        b = BluetoothService.this;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Gets the device's BluetoothService adapter
        // Receivers for Bluetooth
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
        startBluetooth(true); // Autostart bluetooth on start up

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(BluetoothBroadcastReceiverState);
        unregisterReceiver(BluetoothBroadcastReceiverAction);
        unregisterReceiver(BluetoothBroadcastReceiverSearch);
        unregisterReceiver(BluetoothBroadcastReceiverSearchFinished);
        unregisterReceiver(BluetoothBroadcastReceiverBondChange);
        startBluetooth(false); // power off Bluetooth
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ########## ########## Methods ########## ##########

    /**
     * Starts or disables BluetoothService
     * Starts at app startup
     * @param start start or stop Bluetooth
     */
    boolean startBluetooth(boolean start) {
        if (start) {
            startBluetooth = true; // for ending threads on destroy
            if (bluetoothAdapter == null) { // Device doesn't support BluetoothService
                Toast.makeText(getApplicationContext(),
                        getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show();
            } else {
                if (!bluetoothAdapter.isEnabled()) { // Enable BluetoothService
                    bluetoothAdapter.enable();
                } else { // bluetooth already on
                    bluetoothOnChecked = true;
                    bluetoothOn();
                    if (connectThread != null) {
                        if (connectThread.isAlive()) {
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
            if (commandBluetoothList) {
                bluetoothList.setEnabled(true);
            }
        }
        autoConnect();
    }

    /**
     * Auto connects to Raspberry Pi
     * Search and bonds to Raspberry Pi if needed
     * Attempts to search and connect two times if needed
     */
    void autoConnect() {
        autoConnect = true;
        connectAttempts = 1; // first attempt
        searchAttempts = 1;
        bluetoothAutoConnectChecked = true;
        if (bluetoothSettingsActive) {
            bluetoothAutoConnect.setChecked(true);
        }
        boolean isRaspberryPi = isRaspberryPi(activeDevice);
        if (!isRaspberryPi) {
            updatePairedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (isRaspberryPi(device)) {
                    activeDevice = device;
                    isRaspberryPi = true;
                    //bluetoothListIndicator
                    if (bluetoothSettingsActive && commandBluetoothList) {
                        bluetoothList.setValue(activeDevice.getAddress());
                        bluetoothList.setSummary(bluetoothList.getEntry());
                    }
                    break;
                }
            }
            if (!isRaspberryPi) {
                startDiscovery();
            } else {
                connectBluetooth(true);
            }
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
     * Starts a new thread when connecting
     * @param connect used to separate auto connect to regular connect with boolean autoConnect
     */
    void connectBluetooth(boolean connect) {
        if (connect) {
            if (!(activeDevice == null)) {
                if (connectThread != null) {
                    if (!connectThread.hasSocket()) { // do nothing if already has socket
                        connectThread = new ConnectThread(activeDevice);
                    }
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectThread = new ConnectThread(activeDevice);
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
                            } catch (IllegalThreadStateException e) {
                                //Log.e(TAG, "Thread already started",e);
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
            switch (connectAttempts) {
                case 1:
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectAttempts++;
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
                                connectAttempts++;
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
                                connectAttempts++;
                                connectBluetooth(true);
                            }
                        }
                    }, 100);
                    break;
                default :
                    Intent intent = new Intent(BluetoothService.TOAST);
                    intent.putExtra(TEXT,getString(R.string.could_not_connect_to_rpi));
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
     * Changes icons and variables
     */
    synchronized void bluetoothConnected() {
        b.bluetoothConnectChecked = true;
        b.bluetoothAutoConnectChecked = true;
        b.connected = true;
        b.bluetoothSearchEnable = false;
        Intent intent = new Intent(BluetoothService.SET_BLUETOOTH_ICON);
        intent.putExtra(ICON,"ic_bluetooth_connected_white_24dp");
        sendBroadcast(intent);
        if (!commandSimulate) {
            Intent StartMeasButtonIntent = new Intent(START_MEAS_BUTTON_ENABLE);
            StartMeasButtonIntent.putExtra(ENABLE_BUTTON, true);
            sendBroadcast(StartMeasButtonIntent);
            if (measurementRunning) {
                String command = "startMeasure";
                byte[] sendCommand = command.getBytes();
                b.connectedThread.write(sendCommand);
            }
        }
        if (b.bluetoothSettingsActive) {
            s.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bluetoothConnect.setChecked(true);
                    bluetoothAutoConnect.setChecked(true);
                    bluetoothSearch.setEnabled(false);
                }
            });
        }
    }

    /**
     * Method to be called when disconnected
     * Changes icons and variables
     * @param uiThread Runs on the uiThread i true, needed for update ui components from another thread
     */
    synchronized void bluetoothDisconnected(boolean uiThread) {
        b.connected = false;
        b.bluetoothConnectChecked = false;
        b.bluetoothAutoConnectChecked = false;
        b.bluetoothSearchEnable = true;
        Intent intent = new Intent(BluetoothService.SET_BLUETOOTH_ICON);
        intent.putExtra(ICON,"ic_bluetooth_white_24dp");
        sendBroadcast(intent);
        if (!commandSimulate) {
            Intent StartMeasButtonIntent = new Intent(START_MEAS_BUTTON_ENABLE);
            StartMeasButtonIntent.putExtra(ENABLE_BUTTON, false);
            sendBroadcast(StartMeasButtonIntent);
        }
        if (b.bluetoothSettingsActive && !uiThread) {
            bluetoothConnect.setChecked(false);
            bluetoothAutoConnect.setChecked(false);
            bluetoothSearch.setEnabled(true);
        } else if (b.bluetoothSettingsActive && uiThread) {
            s.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bluetoothConnect.setChecked(false);
                    bluetoothAutoConnect.setChecked(false);
                    bluetoothSearch.setEnabled(true);
                }
            });
        }
    }

    /**
     * Start searching for Raspberry Pi with bluetooth
     * Needs Fine Location, will requests it if needed
     * When auto connecting, has two attempts to search with searchManager
     * Bonds when found and connects if autoConnect
     */
    void startDiscovery() {
        foundRaspberryPi = false;
        if (hasLocationPermissions()) {
            bluetoothAdapter.startDiscovery();
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
            if (searchAttempts < 2) { // will search max 2 times if RPI not found
                searchAttempts++;
                startDiscovery();
            } else {
                bluetoothAutoConnectChecked = false;
                if (bluetoothSettingsActive) {
                    bluetoothAutoConnect.setChecked(false);
                }
                Toast.makeText(getApplicationContext(), getString(R.string.did_not_find_rpi), Toast.LENGTH_LONG).show();
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
            Intent intent = new Intent(BluetoothService.REQUEST_PERMISSION);
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
            if (bluetoothSettingsActive && commandBluetoothList) {
                bluetoothList.setValue(activeDevice.getAddress());
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
            if (device.getName() != null) {
                return (device.getName().equals(raspberryPiName) ||
                        device.getAddress().equals(raspberryPiName));
            } else {
                return (device.getAddress().equals(raspberryPiName));
            }
        } else {
            return false;
        }
    }

    /**
     * Updates the Bluetooth list
     */
    void updateBluetoothList() {
        if (bluetoothSettingsActive) {
            updatePairedDevices();
            if (commandBluetoothList) {
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
                    bluetoothList.setSummary(getString(R.string.no_device_available));
                    bluetoothList.setEnabled(false);
                }
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
                    Intent intent = new Intent(BluetoothService.SET_BLUETOOTH_ICON);
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
                    Intent intent = new Intent(BluetoothService.SET_BLUETOOTH_ICON);
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
     * Receiving Bluetooth states as on, off and turning on and off
     */
    public BroadcastReceiver BluetoothBroadcastReceiverState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (bluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
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
                            bluetoothOn.setTitle(getString(R.string.bluetooth_off));
                            if (commandBluetoothList) {
                                bluetoothList.setEnabled(false);
                            }
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
                        if (bluetoothSettingsActive) {
                            bluetoothOn.setChecked(true);
                            bluetoothOn.setTitle(getString(R.string.bluetooth_on));
                        }
                        try {
                            bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
                        } catch (NullPointerException e) {
                            Intent intentIcon = new Intent(BluetoothService.SET_BLUETOOTH_ICON);
                            intentIcon.putExtra(ICON,"ic_bluetooth_white_24dp");
                            sendBroadcast(intentIcon);
                        }
                        break;
                }
            }
        }
    };

    // ########## ACTION_FOUND ##########
    /**
     * Receiving information when a device is discovered
     */
    public BroadcastReceiver BluetoothBroadcastReceiverAction = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (isRaspberryPi(device)) { // Raspberry Pi detected
                    Toast.makeText(context, "Found " + device.getName(), Toast.LENGTH_SHORT).show();
                    activeDevice = device;
                    foundRaspberryPi = true;
                    if (bluetoothSettingsActive && commandBluetoothList) {
                        bluetoothList.setValue(activeDevice.getAddress());
                        bluetoothList.setSummary(bluetoothList.getEntry());
                    }
                    bluetoothAdapter.cancelDiscovery();
                    if (activeDevice.getBondState()!= 12) { // Not bonded: 10, Bonding: 11, Bonded: 12
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                activeDevice.createBond();
                            }
                        }, 1); // delay needed
                    } else {
                        connectBluetooth(autoConnect);
                    }
                }
            }
        }
    };

    // ########## ACTION_DISCOVERY ##########
    /**
     * Discovery started
     */
    public BroadcastReceiver BluetoothBroadcastReceiverSearch = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                bluetoothSearchChecked = true;
                bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_searching_white_24dp);
                if (bluetoothSettingsActive) {
                    bluetoothSearch.setChecked(true);
                }
            }
        }
    };

    /**
     * Discovery finished
     */
    public BroadcastReceiver BluetoothBroadcastReceiverSearchFinished = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothSearchChecked = false;
                bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
                if (bluetoothSettingsActive) {
                    bluetoothSearch.setChecked(false);
                }
                if (!foundRaspberryPi) {
                    searchManager();
                    if (!autoConnect) {
                        Toast.makeText(context, getString(R.string.did_not_find_rpi), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

    // ########## ACTION_BOND_STATE_CHANGED ##########
    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     * Will try to connect to the bonded device when auto connecting
     */
    public BroadcastReceiver BluetoothBroadcastReceiverBondChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    updateBluetoothList();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!connected) {
                                connectBluetooth(autoConnect);
                            }
                        }
                    }, 1); // delay needed
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    updateBluetoothList();
                }
            }
        }
    };
}
