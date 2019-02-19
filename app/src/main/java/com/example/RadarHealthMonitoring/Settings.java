package com.example.RadarHealthMonitoring;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

/**
 * Settings är en aktivitet som skapar en panel med inställningar. Innehåller genvägar till flera
 * olika paneler med olika kategorier av inställningar. Alla paneler finns under R.xml och
 * dessa styrs av aktiviteten.
 */
public class Settings extends AppCompatPreferenceActivity {

    private static final String Settingsmsg = "Settings";
    //boolean static
    //private static UUID deviceUUID;
    /* keys för olika värden från inställningarna */
    public static final String key_pref_connection_list = "connection_list";
    public static final String key_pref_usb_port = "usb_port";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        IntentFilter BTIntentScanChange = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverScan, BTIntentScanChange);
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverBondChange, BTIntentBondChange);
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

        static BluetoothAdapter bluetoothAdapter;
        static SwitchPreference bluetoothOn;
        static ListPreference bluetoothList;
        static SwitchPreference bluetoothSearch;
        static SwitchPreference bluetoothConnect;
        static ConnectThread connectThread;

        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle("Bluetooth Settings");  // ändrar titeln i menyraden
            //bindPreferenceSummaryToValue(findPreference("bluetooth_list"));
            /* Start Bluetooth */
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Får enhetens egna Bluetooth adapter TODO cleanup?
            //final BluetoothManager bluetoothManager =
            //        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            //bluetoothAdapter = bluetoothManager.getAdapter();

            bluetoothOn = (SwitchPreference) findPreference("bluetooth_switch");
            bluetoothConnect = (SwitchPreference) findPreference("bluetooth_connect");
            bluetoothSearch = (SwitchPreference) findPreference("search_bluetooth_device");
            bluetoothList = (ListPreference) findPreference("bluetooth_list");
            if (bluetoothAdapter.isEnabled()) { // kollar om Bluetooth redan är på
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle("Bluetooth On");
                enableBluetoothList();
                bluetoothConnect.setEnabled(true);
                bluetoothSearch.setEnabled(true);
                if (bluetoothAdapter.getBondedDevices().size()>0) {
                    connectBluetooth();  // TODO Error
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
                    final int index = listPreference.findIndexOfValue(stringValue);
                    listPreference.setSummary(  // ändrar summary till värdet
                            listPreference.getEntries()[index]);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (connectThread.isRunning()) {
                                connectThread.cancel();
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Connection canceled", Toast.LENGTH_LONG).show();
                            }
                            connectBluetooth();
                        }
                    }, 1);
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
                    //Log.d(Settingsmsg,newValue.toString());
                    if ((boolean)newValue) {
                        bluetoothAdapter.startDiscovery();
                        Log.d(Settingsmsg,"Enable search");
                    } else {
                        bluetoothAdapter.cancelDiscovery();
                        Log.d(Settingsmsg,"Disable search");
                    }
                    return false;
                }
            });
            // Anslut till enheten Switch Listener
            bluetoothConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        //BluetoothDevice.createRfcommSocketToServiceRecord();
                        Log.d(Settingsmsg,"run thread");
                        connectThread.run();
                        //boolean outcome = connectThread.getDevice().createBond();
                        //Log.d(Settingsmsg, "Bounding outcome : " + outcome);
                    }
                    else {
                        Log.d(Settingsmsg,"cancel Thread");
                        connectThread.cancel();
                    }
                    return false;
                }
            });
        }

        // ########## ########## Program ########## ##########

        public static void connectBluetooth() {
            int index = bluetoothList.findIndexOfValue(bluetoothList.getValue());
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            BluetoothDevice device = (BluetoothDevice)pairedDevices.toArray()[index];
            connectThread = new ConnectThread(device);
            Log.d(Settingsmsg,"Connect Device " + device.getName());
        }

        /**
         * Startar och stänger av Bluetooth
         */
        public boolean startBluetooth() {
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                Toast.makeText(getActivity().getApplicationContext(),
                        "Bluetooth Not Supported", Toast.LENGTH_LONG).show();
                return false;
            } else {
                int REQUEST_ENABLE_BT = 1;  // okänd, måste vara större än noll
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable(); // Startar Bluetooth
                } else {
                    if (connectThread.isRunning()) {
                        connectThread.cancel();
                    }
                    bluetoothAdapter.disable();
                }
                //Toast.makeText(getActivity().getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        /**
         * Fixar listan med Bluetoothenheter
         */
        private static void enableBluetoothList() {
            bluetoothList.setEnabled(true);
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                CharSequence[] deviceName = new CharSequence[pairedDevices.size()];
                CharSequence[] deviceHardwareAddress = new CharSequence[pairedDevices.size()];
                //UUID deviceUUID;
                int i = 0;
                for (BluetoothDevice device : pairedDevices) {
                    deviceName[i] = device.getName();
                    deviceHardwareAddress[i] = device.getAddress(); // MAC address
                    //deviceUUID = device.getUuids()[0].getUuid();
                    i++;
                }
                bluetoothList.setEntries(deviceName);
                bluetoothList.setEntryValues(deviceHardwareAddress);
                bluetoothList.setSummary(bluetoothList.getEntry());
            }
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
                            enableBluetoothList();
                            bluetoothSearch.setEnabled(true);
                            bluetoothConnect.setEnabled(true);
                            if (bluetoothAdapter.getBondedDevices().size()>0) {
                                connectBluetooth();
                            }
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
                    Log.d(Settingsmsg, "Found");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(Settingsmsg,deviceName);
                    //enableBluetoothList(); TODO fixa så att listan uppdateras
                    Toast.makeText(context, "Found: " + deviceName, Toast.LENGTH_SHORT).show();
                }
            }
        };

        // ########## ACTION_DISCOVERY_FINISHED ##########

        public static BroadcastReceiver BluetoothBroadcastReceiverSearch = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //Log.d(Settingsmsg, "Started?");
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
                    Toast.makeText(context, "Search Finished", Toast.LENGTH_SHORT).show();
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
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_NONE.");
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
    // End of Settings Fragment. Start of shared preferences and listeners
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
} // end of Settings.class
