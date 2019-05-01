package com.chalmers.respiradar;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import java.util.List;
//import androidx.annotation.NonNull;
import pub.devrel.easypermissions.EasyPermissions;

import static com.chalmers.respiradar.BluetoothService.START_MEAS_BUTTON_ENABLE;
import static com.chalmers.respiradar.BluetoothService.b;

/**
 * Activity to show settings for Bluetooth and information about the application.
 * All Bluetooth properties is found in the BluetoothService, the SettingsActivity is just for visual input/output.
 */
public class Settings extends AppCompatActivity implements PreferenceFragment.OnPreferenceStartFragmentCallback {

    static Settings s; // for static activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        s = Settings.this;
        setupActionBar();
        setContentView(R.layout.empty);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.empty, new SettingsFragment())
                .commit();  // Skapar ett nytt fragment i en ny panel
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            setTitle(getString(R.string.title_activity_settings)); // Sets the title when sent back from BluetoothSettingsFragment
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Manages Fragment transactions
     */
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        replaceCurrentFragmentsWith(pref.getFragment(), pref.getExtras(), pref.getTitleRes(),
                pref.getTitle());
        return true;
    }
    private void replaceCurrentFragmentsWith(String fragmentClass, Bundle args, @StringRes int titleRes,
                                             CharSequence titleText) {
        Fragment f = Fragment.instantiate(this, fragmentClass, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.empty, f);
        if (titleRes != 0) {
            transaction.setBreadCrumbTitle(titleRes);
        } else if (titleText != null) {
            transaction.setBreadCrumbTitle(titleText);
        }
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    // ########## ########## Settings Fragment ########## ##########

    /**
     * Fragment for Settigns.
     * Shows info about the application and has a link to the nested BluetoothSettingsFragment.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {

        private static final String key_pref_information = "information";
        static Preference informationPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);  // hämtar preferenserna
            setHasOptionsMenu(true);  // ger menyraden
            informationPreference = findPreference(key_pref_information);
            informationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    DialogFragment newFragment = new InformationSettingsFragment();
                    newFragment.show(getFragmentManager(), "info_settings");
                    return false;
                }
            });
        }
    }

    // ########## ########## ########## BluetoothSettings ########## ########## ##########

    /**
     *  Settings for BluetoothService and the Bluetooth connection
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothSettings extends PreferenceFragment implements EasyPermissions.PermissionCallbacks {

        static SwitchPreference bluetoothOn; // to turn Bluetooth on or off
        static SwitchPreference bluetoothAutoConnect; // to start automated connection
        static ListPreference bluetoothList; // list of bonded Bluetooth devices
        static SwitchPreference bluetoothSearch; // start searching after Raspberry Pi
        static SwitchPreference bluetoothConnect; // connect to Raspberry Pi
        static EditTextPreference bluetoothRaspberryPiName; // change the Raspberry Pi name or MAC-address
        static EditTextPreference commandTerminal; // send commands in a "terminal"

        // Preference keys
        private static final String key_pref_bluetooth_switch = "bluetooth_switch";
        private static final String key_pref_bluetooth_auto_connect = "bluetooth_auto_connect";
        private static final String key_pref_bluetooth_connect = "bluetooth_connect";
        private static final String key_pref_bluetooth_search = "search_bluetooth_device";
        private static final String key_pref_bluetooth_list = "bluetooth_list";
        private static final String key_pref_raspberry_pi_name = "bluetooth_raspberrypi_name";
        private static final String key_pref_command_terminal = "command_terminal";
        static final String RESET_GRAPH = "RESET_GRAPH";

        static BluetoothSettings bs; // for static fragment

        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            bs = BluetoothSettings.this;
            b.bluetoothSettingsActive = true;
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle(getString(R.string.title_bluetooth_settings));
            // BluetoothService preferences
            bluetoothOn = (SwitchPreference) findPreference(key_pref_bluetooth_switch);
            bluetoothAutoConnect = (SwitchPreference) findPreference(key_pref_bluetooth_auto_connect);
            bluetoothConnect = (SwitchPreference) findPreference(key_pref_bluetooth_connect);
            bluetoothSearch = (SwitchPreference) findPreference(key_pref_bluetooth_search);
            bluetoothList = (ListPreference) findPreference(key_pref_bluetooth_list);
            bluetoothRaspberryPiName = (EditTextPreference) findPreference(key_pref_raspberry_pi_name);
            commandTerminal = (EditTextPreference) findPreference(key_pref_command_terminal);
            // On return to bluetooth settings, manually update everything
            if (b.bluetoothOnChecked) {
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle(getString(R.string.bluetooth_on));
                b.updateBluetoothList();
            } else {
                bluetoothOn.setChecked(false);
                bluetoothOn.setTitle(getString(R.string.bluetooth_off));
            }
            bluetoothConnect.setEnabled(b.bluetoothConnectEnable);
            bluetoothConnect.setChecked(b.bluetoothConnectChecked);
            bluetoothAutoConnect.setEnabled(b.bluetoothConnectEnable);
            bluetoothAutoConnect.setChecked(b.bluetoothAutoConnectChecked);
            bluetoothSearch.setEnabled(b.bluetoothSearchEnable);
            bluetoothSearch.setChecked(b.bluetoothSearchChecked);
            if (!b.commandBluetoothList) { // the Bluetooth list is only available if activated in the command terminal
                getPreferenceScreen().removePreference(bluetoothList);
            }
            bluetoothList.setEnabled(b.bluetoothListEnable && b.commandBluetoothList);

            // ########## Preference Listeners ##########
            // Start BluetoothService Swithc Listener
            bluetoothOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return b.startBluetooth((boolean)newValue);
                }
            });
            // Auto connect switch listener
            bluetoothAutoConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        b.autoConnect = true;
                        b.autoConnect();
                        return false;
                    } else {
                        b.autoConnect = false;
                        b.bluetoothAdapter.cancelDiscovery();
                        if (b.connectThread != null) {
                            b.connectThread.cancel();
                        } else {
                            b.bluetoothAutoConnectChecked = false;
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
            // Change of devices in Bluetooth list Listener
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
                                            getString(R.string.connection_canceled), Toast.LENGTH_LONG).show();
                                }
                            }
                            b.setActiveDevice();
                        }
                    }, 1); // delay needed, otherwise it won't change
                    return true;
                }
            });
            // Search after Bluetooth devices Switch Listener
            bluetoothSearch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        b.searchAttempts = 1;
                        b.autoConnect = false;
                        b.startDiscovery();
                    } else {
                        b.bluetoothAdapter.cancelDiscovery();
                        b.autoConnect = false;
                        if (!b.connected) {
                            b.bluetoothAutoConnectChecked = false;
                            bluetoothAutoConnect.setChecked(false);
                        }
                        return true;
                    }
                    return false;
                }
            });
            // Connect to device Switch Listener
            bluetoothConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        if (b.activeDevice != null) {
                            b.autoConnect = false;
                            b.connectAttempts = 4;
                            b.connectBluetooth(true);
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getString(R.string.no_device_selected), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (b.connectThread != null) {
                            b.connectThread.cancel();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });
            // Write commands text preference
            commandTerminal.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String command = ((String)newValue).toLowerCase();
                    switch (command) {
                        case "poweroff": // write to Raspberry Pi to power off
                            if (b.connected) {
                                byte[] sendCommand = command.getBytes();
                                b.connectedThread.write(sendCommand);
                                preference.setSummary((String)newValue);
                                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.power_off_rpi), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.not_connected_to_rpi), Toast.LENGTH_LONG).show();
                            }
                            break;
                        case "list": // enable/disable the Bluetooth list of bonded devices
                            if (b.commandBluetoothList) {
                                b.commandBluetoothList = false;
                                b.bluetoothListEnable = false;
                                bluetoothList.setEnabled(false);
                                getPreferenceScreen().removePreference(bluetoothList);
                            } else {
                                b.commandBluetoothList = true;
                                getPreferenceScreen().addPreference(bluetoothList);
                                b.updateBluetoothList();
                                if (b.bluetoothListEnable) {
                                    bluetoothList.setEnabled(true);
                                }
                            }
                            break;
                        case "simulate": // activate/deactivate simulation of values
                            b.commandSimulate = !b.commandSimulate;
                            Intent StartMeasButtonIntent = new Intent(START_MEAS_BUTTON_ENABLE);
                            StartMeasButtonIntent.putExtra(b.ENABLE_BUTTON,b.commandSimulate || b.connected);
                            s.sendBroadcast(StartMeasButtonIntent);
                            Intent readIntent = new Intent(BluetoothSettings.RESET_GRAPH);
                            s.sendBroadcast(readIntent);
                            break;
                        default:
                            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.not_a_command), Toast.LENGTH_LONG).show();
                            break;
                    }
                    return true;
                }
            });
        } // end of onCreate

        @Override
        public void onDestroy() {
            super.onDestroy();
            b.bluetoothSettingsActive = false;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.bluetooth_settings_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.bluetooth_info:
                    DialogFragment newFragment = new InformationBluetoothSettingsFragment();
                    newFragment.show(getFragmentManager(), "help_bluetooth");
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        // ########## ########## Request Permission ########## ##########
        // To search after Bluetooth Devices, location permission is needed

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }

        @Override
        public void onPermissionsGranted(int requestCode, @NonNull List<String> list) {
            // Some permissions have been granted
            if (requestCode == 2) {
                b.startDiscovery();
            }
        }

        @Override
        public void onPermissionsDenied(int requestCode, @NonNull List<String> list) {
            // Some permissions have been denied
            if (requestCode == 2) {
                b.bluetoothAutoConnectChecked = false;
                Settings.BluetoothSettings.bluetoothAutoConnect.setChecked(false);
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.location_permissions_denied), Toast.LENGTH_LONG).show();
            }
        }

    } // end of BluetoothSettings
} // end of Settings class
