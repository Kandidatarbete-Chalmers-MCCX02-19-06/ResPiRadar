package com.example.RadarHealthMonitoring;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;
import java.util.UUID;

/**
 * Settings är en aktivitet som skapar en panel med inställningar. Innehåller genvägar till flera
 * olika paneler med olika kategorier av inställningar. Alla paneler finns under R.xml och
 * dessa styrs av aktiviteten.
 */
public class Settings extends AppCompatPreferenceActivity {

    static Settings s; // for static activity

    private static final String Settingsmsg = "Settings";

    // keys för olika värden från inställningarna
    public static final String key_pref_connection_list = "connection_list";
    public static final String key_pref_usb_port = "usb_port";

    private static final int REQUEST_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        s = Settings.this;

        super.onCreate(savedInstanceState);
        Log.d(Settingsmsg, "onCreate for Settings");
        setupActionBar();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();  // Skapar ett nytt fragment i en ny panel

        IntentFilter BTIntentChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverState, BTIntentChange);
        IntentFilter BTIntentFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverAction, BTIntentFound);
        IntentFilter BTIntentSearch = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearch, BTIntentSearch);
        IntentFilter BTIntentSearchFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearchFinished, BTIntentSearchFinished);
        IntentFilter BTIntentScanChange = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverScan, BTIntentScanChange);
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverBondChange, BTIntentBondChange);

        /*IntentFilter BTIntentChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReciver, BTIntentChange);
        IntentFilter BTIntentFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReciver, BTIntentFound);
        IntentFilter BTIntentSearch = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReciver, BTIntentSearch);
        IntentFilter BTIntentSearchFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReciver, BTIntentSearchFinished);
        IntentFilter BTIntentScanChange = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReciver, BTIntentScanChange);
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReciver, BTIntentBondChange);*/

        //IntentFilter BluetoothIntent = new IntentFilter();
        //registerReceiver(BluetoothSettings.BluetoothBroadcastReciver,BluetoothIntent);

        // BLE, fungerar inte
        /*final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();*/
    }

    @Override
        public void onDestroy() {
            Log.d(Settingsmsg, "onDestroy: called for Settings.");
            super.onDestroy();
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverState); // viktigt att stänga av
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverAction);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearch);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearchFinished);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverScan);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverBondChange);

            //unregisterReceiver(BluetoothSettings.BluetoothBroadcastReciver);
        }

    /**
     * Viktigt att få med alla fragment som länkas i panelerna, annars krasar appen. Är en
     * säkerhetsåtgärd för att förhindra att malware får åtkommst till fragmenten.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.class.getName().equals(fragmentName)
                || Settings.BluetoothFragment.class.getName().equals(fragmentName)
                || Settings.WifiFragment.class.getName().equals(fragmentName)
                || Settings.USBFragment.class.getName().equals(fragmentName)
                || Settings.GeneralFragment.class.getName().equals(fragmentName)
                || Settings.DeveloperFragment.class.getName().equals(fragmentName);
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

    // ########## ########## ########## ########## ########## ########## ##########
    // Start of Settings Fragments
    // ########## ########## ########## ########## ########## ########## ##########

    /**
     * Huvudpanelen för Settings. Innehåller både en lista där anslutning väljs.
     * Innehåller även länkar till de nestade framgenten/panelerna.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        ListPreference connectionList;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);  // hämtar preferenserna
            setHasOptionsMenu(true);  // ger menyraden
            bindPreferenceSummaryToValue(findPreference(key_pref_connection_list));  // delar värdet
        }
    }

    // ########## ########## ########## Bluetooth ########## ########## ##########

    /**
     * Bluetoothfragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new BluetoothSettings()) // add?
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     *  Inställningar för Bluetooth
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothSettings extends PreferenceFragment {

        private static boolean autoConnect = false;

        static BluetoothAdapter bluetoothAdapter;
        static SwitchPreference bluetoothOn;
        static SwitchPreference bluetoothAutoConnect;
        static ListPreference bluetoothList;
        static SwitchPreference bluetoothSearch;
        static SwitchPreference bluetoothConnect;
        static SwitchPreference bluetoothRead;
        static EditTextPreference bluetoothRaspberryPiName;
        static ConnectThread connectThread;
        private static Handler handler = new Handler();

        private static BluetoothDevice activeDevice;
        private static Set<BluetoothDevice> pairedDevices;
        private static int chosenDeviceIndex;
        private static String raspberryPiName = "raspberrypi";
        private static final String raspberryPiMAC = "B8:27:EB:FC:22:65";

        private static final String key_pref_bluetooth_switch = "bluetooth_switch";
        private static final String key_pref_bluetooth_auto_connect = "bluetooth_auto_connect";
        private static final String key_pref_bluetooth_connect = "bluetooth_connect";
        private static final String key_pref_bluetooth_search = "search_bluetooth_device";
        private static final String key_pref_bluetooth_read = "bluetooth_read";
        private static final String key_pref_bluetooth_list = "bluetooth_list";
        private static final String key_pref_raspberry_pi_name = "bluetooth_raspberrypi_name";

        private static Handler uiHandler = new Handler(Looper.getMainLooper());

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(Settingsmsg, "BluetoothSettings onDestroy");
        }

        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(Settingsmsg, "BluetoothSettings onCreate");
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle("Bluetooth Settings");  // Change title
            // Start Bluetooth
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Gets the device's Bluetooth adapter
            bluetoothOn = (SwitchPreference) findPreference(key_pref_bluetooth_switch);
            bluetoothAutoConnect = (SwitchPreference) findPreference(key_pref_bluetooth_auto_connect);
            bluetoothConnect = (SwitchPreference) findPreference(key_pref_bluetooth_connect);
            bluetoothSearch = (SwitchPreference) findPreference(key_pref_bluetooth_search);
            bluetoothRead = (SwitchPreference) findPreference(key_pref_bluetooth_read);
            bluetoothList = (ListPreference) findPreference(key_pref_bluetooth_list);
            bluetoothRaspberryPiName = (EditTextPreference) findPreference(key_pref_raspberry_pi_name);
            // On create
            if (bluetoothAdapter.isEnabled()) { // Check if Bluetooth already is on
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle("Bluetooth On");
                updateBluetoothList();
                bluetoothConnect.setEnabled(true);
                bluetoothSearch.setEnabled(true);
                if (bluetoothAdapter.getBondedDevices().size() > 0) {
                    setActiveDevice();
                }
            } else {
                bluetoothOn.setChecked(false);
                bluetoothOn.setTitle("Bluetooth Off");
                bluetoothList.setEnabled(false);
                bluetoothConnect.setEnabled(false);
                bluetoothSearch.setEnabled(false);
            }
            // Ändring av enhet i Bluetoothlistan Listener
            bluetoothList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringValue = newValue.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    chosenDeviceIndex = listPreference.findIndexOfValue(stringValue);
                    listPreference.setSummary(listPreference.getEntries()[chosenDeviceIndex]); // ändrar summary till värdet
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!(connectThread == null)) {
                                if (connectThread.isRunning()) {
                                    connectThread.cancel();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            "Connection canceled", Toast.LENGTH_LONG).show();
                                }
                            }
                            setActiveDevice();
                            //connectBluetooth();
                        }
                    }, 1); // delay needed, otherwise it won't change
                    return true;
                }
            });
            // Starta Bluetooth Swithc Listener
            bluetoothOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return startBluetooth();
                }
            });
            // Leta efter bluetoothenheter Switch Listener
            bluetoothSearch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        autoConnect = false;
                        startDiscovery();
                    } else {
                        bluetoothAdapter.cancelDiscovery();
                        Log.d(Settingsmsg, "Disable search");
                        return true;
                    }
                    return false;
                }
            });
            // Anslut till enheten Switch Listener
            bluetoothConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        if (activeDevice != null) {
                            connectBluetooth(true);
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "No device selected", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (connectThread != null) {
                            Log.d(Settingsmsg, "cancel Thread");
                            connectThread.cancel();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });
            bluetoothRaspberryPiName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    raspberryPiName = (String) newValue;
                    preference.setSummary((String)newValue);
                    return true;
                }
            });
            bluetoothAutoConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        autoConnect();
                        return true;
                    } else {
                        if (connectThread != null) {
                            Log.d(Settingsmsg, "cancel Thread");
                            connectThread.cancel();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }


        // ########## ########## Program ########## ##########

        private static void startDiscovery() {
            if (s.hasLocationPermissions()) {
                bluetoothAdapter.startDiscovery();
                Log.d(Settingsmsg, "Enable search");
            } else {
                s.requestLocationPermission();
            }
        }

        private static void autoConnect() {
            autoConnect = true;
            Log.d(Settingsmsg, "activeDevice" + activeDevice);
            boolean isRaspberryPi = isRaspberryPi(activeDevice);
            Log.d(Settingsmsg, "start isRaspberryPi: " + isRaspberryPi);
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
                Log.d(Settingsmsg, "update device isRaspberryPi: " + isRaspberryPi);
                if (!isRaspberryPi || (activeDevice.getBondState() != 12)) {
                    startDiscovery();
                } else {
                    connectBluetooth(true);
                }
                Log.d(Settingsmsg, "start discovery isRaspberryPi: " + isRaspberryPi);
            } else {
                connectBluetooth(true);
            }
        }

        private static void updatePairedDevices() {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        }

        private static void setActiveDevice() {
            if (!(chosenDeviceIndex==-1)) {
                activeDevice = (BluetoothDevice)pairedDevices.toArray()[chosenDeviceIndex];
                bluetoothList.setValue(raspberryPiMAC);
                bluetoothList.setSummary(bluetoothList.getEntry());
            }
        }

        private static boolean isRaspberryPi(BluetoothDevice device) {
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

        public static void connectBluetooth(boolean connect) { // TODO Callback Manager
            if (connect) {
                if (!(activeDevice == null)) {
                    if (connectThread != null) {
                        if (!connectThread.hasSocket()) { // do nothing if already has socket
                            connectThread = new ConnectThread(activeDevice);
                            Log.d(Settingsmsg, "Create Refcomm Socket: " + activeDevice.getName());
                        }
                    } else {
                        connectThread = new ConnectThread(activeDevice);
                        Log.d(Settingsmsg, "Create Refcomm Socket: " + activeDevice.getName());
                    }
                    connectThread.run();
                    Log.d(Settingsmsg, "Run thread");
                }
            }
        }

        /**
         * Starts or disables Bluetooth
         */
        public boolean startBluetooth() {
            if (bluetoothAdapter == null) { // Device doesn't support Bluetooth
                Toast.makeText(getActivity().getApplicationContext(),
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

        /**
         * Fixar listan med Bluetoothenheter
         */
        private static void updateBluetoothList() {
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

        /**
         * Converts a 16 bit UUID to 128 bit
         * @param i 16 bit UUID integer
         * @return UUID
         */
        public static UUID convertFromInteger(int i) {
            final long MSB = 0x0000000000001000L;
            final long LSB = 0x800000805f9b34fbL;
            long value = i & 0xFFFFFFFF;
            return new UUID(MSB | (value << 32), LSB);
        }

        /**
         * Method to run a thread in a static context. Can also be done with useing s.runOnUiThread
         * @param runnable Runnable
         */
        public static void runOnUI(Runnable runnable) {
            uiHandler.post(runnable);
        }

        // ########## ########## BroadcastReceivers ########## ##########

        // ########## ACTION_STATE_CHANGED ##########
        /**
         * Create a BroadcastReceiver for ACTION_STATE_CHANGED.
         */
        public static BroadcastReceiver BluetoothBroadcastReceiverState = new BroadcastReceiver() {
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
        public static BroadcastReceiver BluetoothBroadcastReceiverAction = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(Settingsmsg, "Found: " + device.getName() + " " + device.getAddress());
                    if (isRaspberryPi(device)) { // Raspberry Pi detected
                        Toast.makeText(context, "Found " + device.getName(), Toast.LENGTH_SHORT).show();
                        if (device.getBondState()!= 12) { // Not bonded: 10, Bonding: 11, Bonded: 12
                            device.createBond();
                        } else {
                            Log.d(Settingsmsg, "search finnished, foudn RPI, bonded, autoconnect: " + autoConnect);
                            connectBluetooth(autoConnect);
                        }
                        activeDevice = device;
                        bluetoothList.setValue(raspberryPiMAC);
                        bluetoothList.setSummary(bluetoothList.getEntry());
                        bluetoothAdapter.cancelDiscovery();
                    }
                }
            }
        };

        // ########## ACTION_DISCOVERY ##########
        public static BroadcastReceiver BluetoothBroadcastReceiverSearch = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.d(Settingsmsg, "Search Started");
                    bluetoothSearch.setChecked(true);
                }
            }
        };

        public static BroadcastReceiver BluetoothBroadcastReceiverSearchFinished = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(Settingsmsg,"Finished");
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
        private static BroadcastReceiver BluetoothBroadcastReceiverScan = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                    int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                    switch (mode) {
                        //Device is in Discoverable Mode
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Discoverability Enabled.");
                            break;
                        //Device not in discoverable mode
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Connecting....");
                            break;
                        case BluetoothAdapter.STATE_CONNECTED:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Connected.");
                            break;
                    }
                }
            }
        };

        // ########## ACTION_BOND_STATE_CHANGED ##########
        /**
         * Broadcast Receiver that detects bond state changes (Pairing status changes)
         */
        private static BroadcastReceiver BluetoothBroadcastReceiverBondChange = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //3 cases:
                    //case1: bonded already
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_BONDED.");
                        updateBluetoothList();
                        if (isRaspberryPi(activeDevice)) {
                            Log.d(Settingsmsg, "search finnished, foudn RPI, bonded, autoconnect: " + autoConnect);
                            connectBluetooth(autoConnect);
                        }
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_NONE.");
                        updateBluetoothList();
                    }
                }
            }
        };
    } // end of BluetoothSettings

    // ########## ########## ########## Wifi ########## ########## ##########

    /**
     * Wififragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WifiFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new WifiSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * Wifiinställningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WifiSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wifi);
            setHasOptionsMenu(true);
            getActivity().setTitle("Wifi Settings");
        }
    }

    // ########## ########## ########## USB ########## ########## ##########

    /**
     * USBfragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class USBFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new USBSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * USB-inställningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class USBSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_usb);
            setHasOptionsMenu(true);
            getActivity().setTitle("USB Settings");
            bindPreferenceSummaryToValue(findPreference(key_pref_usb_port));
        }
    }

    // ########## ########## ########## General ########## ########## ##########

    /**
     * Fragment till Generella inställningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new GeneralSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * Generella instälningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            getActivity().setTitle("General Settings");
        }
    }

    // ########## ########## ########## Developer ########## ########## ##########

    /**
     * Developerfragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DeveloperFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new DeveloperSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * Inställningar för utvecklare och debug
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DeveloperSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_developer);
            setHasOptionsMenu(true);
            getActivity().setTitle("Developer Settings");
        }
    }

    // ########## ########## ########## ########## ########## ########## ##########
    // End of Settings Fragments. Start of shared preferences and listeners
    // ########## ########## ########## ########## ########## ########## ##########

    /**
     * Lyssnar efter ändringar. Gör bland annat att alla preferenser som har en key och är bundet med
     * bindPreferenceSummaryToValue kan få sitt värde utskrivet i summary. Dessutom får listan
     * med olika anslutningsmöjligheter sina figurer här.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                if (listPreference.getKey().equals(key_pref_connection_list)) {  // ändrar ikonen
                    switch (stringValue) {
                        case "1":
                            listPreference.setIcon(R.drawable.ic_bluetooth_black_24dp);
                            break;
                        case "0":
                            listPreference.setIcon(R.drawable.ic_wifi_black_24dp);
                            break;
                        case "-1":
                            listPreference.setIcon(R.drawable.ic_usb_black_24dp);
                            break;
                    }
                }
                preference.setSummary(  // ändrar summary till värdet
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        // Trigger the listener immediately with the preference's current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() { // TODO Se till att vid automatisk anlutning loopa så den ansluts
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        //log("Requested user enable Location. Try starting the scan again.");
    }
} // end of Settings.class
