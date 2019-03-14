package com.example.RadarHealthMonitoring;

import android.annotation.TargetApi;
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

import static com.example.RadarHealthMonitoring.Bluetooth.b;

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

    //private static final int REQUEST_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settingsmsg, "onCreate for Settings");
        s = Settings.this;
        setupActionBar();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();  // Skapar ett nytt fragment i en ny panel
    }

    @Override
        public void onDestroy() {
            Log.d(Settingsmsg, "onDestroy: called for Settings.");
            super.onDestroy();
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

        static SwitchPreference bluetoothOn;
        static SwitchPreference bluetoothAutoConnect;
        static ListPreference bluetoothList;
        static SwitchPreference bluetoothSearch;
        static SwitchPreference bluetoothConnect;
        static EditTextPreference bluetoothRaspberryPiName;
        static EditTextPreference bluetoothWrite;

        /*static ConnectThread connectThread;
        static Handler handler = new Handler();

        static BluetoothDevice activeDevice;
        static Set<BluetoothDevice> pairedDevices;
        static int chosenDeviceIndex;
        static String raspberryPiName = "raspberrypi";
        static final String raspberryPiMAC = "B8:27:EB:FC:22:65";
        static boolean autoConnect = false;*/

        private static final String key_pref_bluetooth_switch = "bluetooth_switch";
        private static final String key_pref_bluetooth_auto_connect = "bluetooth_auto_connect";
        private static final String key_pref_bluetooth_connect = "bluetooth_connect";
        private static final String key_pref_bluetooth_search = "search_bluetooth_device";
        private static final String key_pref_bluetooth_list = "bluetooth_list";
        private static final String key_pref_raspberry_pi_name = "bluetooth_raspberrypi_name";
        private static final String key_pref_bluetooth_write = "bluetooth_write";

        private static Handler uiHandler = new Handler(Looper.getMainLooper());

        static BluetoothSettings bs; // for static service

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(Settingsmsg, "BluetoothSettings onDestroy");
            b.bluetoothSettingsActive = false;
        }

        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(Settingsmsg, "BluetoothSettings onCreate");
            bs = BluetoothSettings.this;
            b.bluetoothSettingsActive = true;
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle("Bluetooth Settings");  // Change title
            // Start Bluetooth
            bluetoothOn = (SwitchPreference) findPreference(key_pref_bluetooth_switch);
            bluetoothAutoConnect = (SwitchPreference) findPreference(key_pref_bluetooth_auto_connect);
            bluetoothConnect = (SwitchPreference) findPreference(key_pref_bluetooth_connect);
            bluetoothSearch = (SwitchPreference) findPreference(key_pref_bluetooth_search);
            bluetoothList = (ListPreference) findPreference(key_pref_bluetooth_list);
            bluetoothRaspberryPiName = (EditTextPreference) findPreference(key_pref_raspberry_pi_name);
            bluetoothWrite = (EditTextPreference) findPreference(key_pref_bluetooth_write);
            // On return to bluetooth settings, manually update everything
            if (b.bluetoothOnChecked) {
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle("Bluetooth On");
                b.updateBluetoothList();
            } else {
                bluetoothOn.setChecked(false);
                bluetoothOn.setTitle("Bluetooth Off");
            }
            bluetoothConnect.setEnabled(b.bluetoothConnectEnable);
            bluetoothConnect.setChecked(b.bluetoothConnectChecked);
            bluetoothAutoConnect.setEnabled(b.bluetoothConnectEnable);
            bluetoothAutoConnect.setChecked(b.bluetoothAutoConnectChecked);
            bluetoothSearch.setEnabled(b.bluetoothSearchEnable);
            bluetoothSearch.setChecked(b.bluetoothSearchChecked);
            bluetoothList.setEnabled(b.bluetoothListEnable);
            bluetoothWrite.setEnabled(b.bluetoothWriteEnable);
            bluetoothList.setEnabled(b.bluetoothListEnable);

            // ########## Preference Listeners ##########
            // Ändring av enhet i Bluetoothlistan Listener
            bluetoothList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringValue = newValue.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    b.chosenDeviceIndex = listPreference.findIndexOfValue(stringValue);
                    listPreference.setSummary(listPreference.getEntries()[b.chosenDeviceIndex]); // ändrar summary till värdet
                    b.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!(b.connectThread == null)) {
                                if (b.connectThread.isRunning()) {
                                    b.connectThread.cancel();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            "Connection canceled", Toast.LENGTH_LONG).show();
                                }
                            }
                            b.setActiveDevice();
                        }
                    }, 1); // delay needed, otherwise it won't change
                    return true;
                }
            });

            // Starta Bluetooth Swithc Listener
            bluetoothOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return b.startBluetooth((boolean)newValue);
                }
            });

            // Leta efter bluetoothenheter Switch Listener
            bluetoothSearch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        b.searchAttempts = 1;
                        b.autoConnect = false;
                        b.startDiscovery();
                    } else {
                        b.bluetoothAdapter.cancelDiscovery();
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
                        if (b.activeDevice != null) {
                            b.autoConnect = false;
                            b.connectAttempt = 4;
                            b.connectBluetooth(true);
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "No device selected", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (b.connectThread != null) {
                            Log.d(Settingsmsg, "cancel Thread");
                            b.connectThread.cancel();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });

            // Change Raspberry Pi bluetooth name or MAC address text preference
            bluetoothRaspberryPiName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    b.raspberryPiName = (String) newValue;
                    preference.setSummary((String)newValue);
                    return true;
                }
            });

            // Auto connect switch listener
            bluetoothAutoConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        b.autoConnect = true;
                        b.autoConnect();
                        return true;
                    } else {
                        if (b.connectThread != null) {
                            Log.d(Settingsmsg, "cancel Thread");
                            b.connectThread.cancel();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });

            // Write to  Raspberry Pi text preference
            bluetoothWrite.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    byte[] write = ((String)newValue).getBytes();
                    Log.d(Settingsmsg, "Message: " + newValue + write);
                    b.connectedThread.write(write);
                    //b.raspberryPiName = (String) newValue;
                    preference.setSummary((String)newValue);
                    return true;
                }
            });

        }

        // ########## ########## Methods ########## ##########
        void connectedThreadDisconnect() {
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

    /*boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void requestLocationPermission() { // TODO Se till att vid automatisk anlutning loopa så den ansluts
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        //log("Requested user enable Location. Try starting the scan again.");
    }*/
} // end of Settings.class
