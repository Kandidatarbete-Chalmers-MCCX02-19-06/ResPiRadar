package com.example.RadarHealthMonitoring;

import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import android.annotation.TargetApi;

/**
 * Settings är en aktivitet som skapar en panel med inställningar. Innehåller genvägar till flera
 * olika paneler med olika kategorier av inställningar. Alla paneler finns under R.xml och
 * dessa styrs av aktiviteten.
 */
public class Settings extends AppCompatPreferenceActivity {

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
    }

    /**
     * Viktigt att få med alla fragment som länkas i panelerna, annars krasar appen. Är en
     * säkerhetsåtgärd för att förhindra att malware får åtkommst till fragmenten.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.BluetoothFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.WifiFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.USBFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.GeneralFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.DeveloperFragment.class.getName().equals(fragmentName);
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

    /**
     * Huvudpanelen för Settings. Innehåller både en lista där anslutning väljs.
     * Innehåller även länkar till de nestade framgenten/panelerna.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);  // hämtar preferenserna
            setHasOptionsMenu(true);  // ger menyraden
            bindPreferenceSummaryToValue(findPreference(key_pref_connection_list));  // delar värdet
            // från listan med anslutningar
        }

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
            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.pref_bluetooth);
                setHasOptionsMenu(true);
                getActivity().setTitle("Bluetooth Settings");  // ändrar titeln i menyraden
            }
        }

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
    }

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
}


