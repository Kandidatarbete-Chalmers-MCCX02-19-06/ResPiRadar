package com.example.RadarHealthMonitoring;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.Set;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

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
    private static final int REQUEST_FINE_LOCATION = 2;
    //private static BluetoothAdapter bluetoothAdapter;

    public static UUID characteristicUuid2;

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
        IntentFilter BTIntentGattConnection = new IntentFilter(BluetoothLeService.ACTION_GATT_CONNECTED); // används? Ta bort?
        registerReceiver(BluetoothSettings.gattUpdateReceiver, BTIntentBondChange);

        characteristicUuid2 = (UUID) getIntent().getSerializableExtra("extra_uuid");

        if (!hasLocationPermissions()) { // ger tillåtelse att scanna med bluetooth
            requestLocationPermission();
        }

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
            unregisterReceiver(BluetoothSettings.gattUpdateReceiver);
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


        // Low Energy Bluetooth enable
        private static boolean ble = false;
        private static boolean RxBle = false;
        private static boolean discoverAll = true;
        private static boolean discoveredRaspberryPi = false;
        private static boolean infoRxbleOn = false;
        private static boolean RxBT = true;

        static BluetoothAdapter bluetoothAdapter;
        static SwitchPreference bluetoothOn;
        static ListPreference bluetoothList;
        static SwitchPreference bluetoothSearch;
        static SwitchPreference bluetoothConnect;
        static SwitchPreference bluetoothRead;
        static ConnectThread connectThread;
        private boolean mScanning;
        private static Handler handler = new Handler();

        BluetoothLeScanner bluetoothLeScanner;  // BLE
        static RxBleClient rxBleClient; // RxBle
        static UUID uuid;
        static BluetoothDevice activeDevice;
        static UUID[] uuidRaspberryPi = new UUID[1];
        static RxBleDevice rxDevice;
        private static String raspberryPiMAC = "B8:27:EB:FC:22:65";

        private static final int SCAN_REQUEST_CODE = 42;
        private PendingIntent callbackIntent;
        private Disposable scanDisposable;
        //private ScanResultsAdapter resultsAdapter;
        Disposable flowDisposable;
        Disposable scanSubscription;
        static Disposable disposable;
        //Disposable nonStaticDisposable;
        private static Observable<RxBleConnection> connectionObservable;
        private RxBleDevice bleDevice;
        private PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
        //private static UUID characteristicUuid = UUID.fromString("82ff5000-abc8-40dd-99fe-ba009c4a2acd");
        //private static UUID characteristicUuid = UUID.fromString("00x2A37-0000-1000-8000-00805F9B34FB");
        //private static UUID characteristicUuid = UUID.fromString("0x2A37");
        //private static UUID characteristicUuid;
        private static UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D);
        private static UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37);
        private static UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39);
        //private static UUID characteristicUuid = convertFromInteger(0x2A3D);
        //private static UUID characteristicUuid = new UUID((byte)0x2A37);
        private static UUID characteristicUuid = HEART_RATE_MEASUREMENT_CHAR_UUID;

        //        .fromString("00x2A37-0000-1000-8000-00805F9B34FB");
        private static final CompositeDisposable compositeDisposable = new CompositeDisposable();
        //UUID.fromString("0x2A37"



        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle("Bluetooth Settings");  // ändrar titeln i menyraden
            /* Start Bluetooth */
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Får enhetens egna Bluetooth adapter

            uuidRaspberryPi[0]= UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb");


            // BLE
            if (ble) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            } else if (RxBle) {
                rxBleClient = RxBleClient.create(getActivity().getApplicationContext());
                callbackIntent = PendingIntent.getBroadcast(getActivity().getApplicationContext(), SCAN_REQUEST_CODE,
                        new Intent(getActivity().getApplicationContext(), ScanReceiver.class), 0);
            }

            bluetoothOn = (SwitchPreference) findPreference("bluetooth_switch");
            bluetoothConnect = (SwitchPreference) findPreference("bluetooth_connect");
            bluetoothSearch = (SwitchPreference) findPreference("search_bluetooth_device");
            bluetoothRead = (SwitchPreference) findPreference("bluetooth_read");
            bluetoothList = (ListPreference) findPreference("bluetooth_list");
            // On create
            if (bluetoothAdapter.isEnabled()) { // kollar om Bluetooth redan är på
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle("Bluetooth On");
                enableBluetoothList();
                bluetoothConnect.setEnabled(true);
                bluetoothSearch.setEnabled(true);
                if (bluetoothAdapter.getBondedDevices().size()>0) {
                    setActiveDevice();
                    //connectBluetooth(); // TODO automaitsk anslutning
                }
            } else {
                bluetoothOn.setChecked(false);
                bluetoothOn.setTitle("Bluetooth Off");
                bluetoothList.setEnabled(false);
                bluetoothConnect.setEnabled(false);
                bluetoothSearch.setEnabled(false);
            }
            if (bluetoothConnect.isChecked()) {
                if (disposable==null) {
                    bluetoothConnect.setChecked(false);
                    // disposable = new Disposable(); // TODO gör ej abstrakt, alltså ärv klassen
                } else if (disposable.isDisposed()) {
                    bluetoothConnect.setChecked(false);
                } else {
                    bluetoothConnect.setChecked(true);
                }
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
                    //Handler handler = new Handler();
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
                        if (discoverAll) {
                            bluetoothAdapter.startDiscovery();
                        } else {
                            if (ble) {
                                scanLeDevice(true);
                            } else if (RxBle) {
                                deviceDiscovery();
                                discoveredRaspberryPi=false;
                                //scanBleDeviceInBackground(); // TODO ?
                                //scanBleDevices();
                            } else {
                                bluetoothAdapter.startDiscovery();
                            }
                        }
                        Log.d(Settingsmsg,"Enable search");
                    } else {
                        if (discoverAll) {
                            bluetoothAdapter.cancelDiscovery();
                        } else {
                            if (ble) {
                                scanLeDevice(false);
                            } else if (RxBle) {
                                // When done, just dispose.
                                if (scanSubscription != null) {
                                    scanSubscription.dispose();
                                }
                                //flowDisposable.dispose();
                                Log.d(Settingsmsg, "flowDisposable.dispose()");
                            } else {
                                bluetoothAdapter.cancelDiscovery();
                            }
                        }
                        Log.d(Settingsmsg,"Disable search");
                    }
                    return true;
                }
            });
            // Anslut till enheten Switch Listener
            bluetoothConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        //BluetoothDevice.createRfcommSocketToServiceRecord();
                        if (activeDevice != null) {
                            if (ble) {
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(Settingsmsg, "Connect Device");
                                        connectDevice(activeDevice);// BLE
                                    }
                                }, 100);
                                //connectDevice(activeDevice);// BLE
                                //BluetoothGatt bluetoothGatt = activeDevice.connectGatt(getActivity().getApplicationContext(), false, gattCallback);
                                //boolean outcome = connectThread.getDevice().createBond();
                                //Log.d(Settingsmsg, "Bounding outcome : " + outcome);
                            } else if (RxBle) {
                                connectBluetooth();
                            } else {
                                Log.d(Settingsmsg,"run thread");
                                if (!connectThread.hasSocket()) {
                                    connectBluetooth();
                                }
                                connectThread.run();
                            }
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "No device selected", Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        if (ble) {
                            Log.d(Settingsmsg, "Disconnect Gatt Server");
                            disconnectGattServer(); // BLE
                        } else if (RxBle) {
                            disposable.dispose();
                            infoRxbleOn = false;
                            //bluetoothConnect.setChecked(false);
                            return true;
                        } else {
                            Log.d(Settingsmsg,"cancel Thread");
                            connectThread.cancel();
                        }
                    }
                    return false;
                }
            });
            bluetoothRead.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        read();
                        Log.d(Settingsmsg, "Read");
                    }
                    return false;
                }
            });
        }

        // ########## ########## Program ########## ##########

        public static UUID convertFromInteger(int i) {
            final long MSB = 0x0000000000001000L;
            final long LSB = 0x800000805f9b34fbL;
            long value = i & 0xFFFFFFFF;
            return new UUID(MSB | (value << 32), LSB);
        }

        // RxBle
        private void deviceDiscovery() {
            scanSubscription = rxBleClient.scanBleDevices(
                    new ScanSettings.Builder()
                            // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                            .build()
                    // add filters if needed
            ).subscribe(scanResult -> {
                        // Process scan result here.
                        scanResult.getBleDevice();
                        Log.d(Settingsmsg,"Found device: " + scanResult.toString());
                    },
                    throwable -> {
                        // Handle an error here.
                    }
            );

            /*flowDisposable = rxBleClient.observeStateChanges()
                    .switchMap(state -> { // switchMap makes sure that if the state will change the rxBleClient.scanBleDevices() will dispose and thus end the scan
                        Log.d(Settingsmsg,"flowDisposable = rxBleClient.observeStateChanges()");
                        switch (state) {

                            case READY:
                                // everything should work
                                return rxBleClient.scanBleDevices();
                            case BLUETOOTH_NOT_AVAILABLE:
                                // basically no functionality will work here
                            case LOCATION_PERMISSION_NOT_GRANTED:
                                // scanning and connecting will not work
                            case BLUETOOTH_NOT_ENABLED:
                                // scanning and connecting will not work
                            case LOCATION_SERVICES_NOT_ENABLED:
                                // scanning will not work
                            default:
                                return Observable.empty();
                        }
                    })
                    .subscribe(
                            rxBleScanResult -> {
                                // Process scan result here.
                                Log.d(Settingsmsg,"Found device: " + rxBleScanResult.toString());
                            },
                            throwable -> {
                                // Handle an error here.
                            }
                    );*/
        }

        /*private void scanBleDeviceInBackground() {
            try {
                rxBleClient.getBackgroundScanner().scanBleDeviceInBackground(
                        callbackIntent,
                        new ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .build(),
                        new ScanFilter.Builder()
                                .setDeviceAddress("5C:31:3E:BF:F7:34")
                                // add custom filters if needed
                                .build()
                );
            } catch (BleScanException scanException) {
                Log.w("BackgroundScanActivity", "Failed to start background scan", scanException);
                //ScanExceptionHandler.handleException(this, scanException);
            }
        }*/

        private void scanBleDevices() {
            scanDisposable = rxBleClient.scanBleDevices(
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build(),
                    new ScanFilter.Builder()
//                            .setDeviceAddress("B4:99:4C:34:DC:8B")
                            // add custom filters if needed
                            .build()
            )
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(this::todo)
                    .subscribe(); // TODO do somethong
        }

        public static RxBleClient getRxBleClient() {
            return rxBleClient;
        }

        private void todo() {
            // TODO
        }

        // BLE
        public BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        };

        boolean mConnected = false;
        BluetoothGatt mGatt;

        private class GattClientCallback extends BluetoothGattCallback {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.d(Settingsmsg,"onConnectionStateChange newState: " + newState);
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_FAILURE) {
                    Log.d(Settingsmsg,"Connection Gatt failure status " + status);
                    disconnectGattServer();
                    return;
                } else if (status != GATT_SUCCESS) {
                    Log.d(Settingsmsg,"Connection not GATT sucess status " + status);
                    disconnectGattServer();
                    return;
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(Settingsmsg,"Connected to device " + gatt.getDevice().getAddress());
                    mConnected = true;
                    gatt.discoverServices();
                    gatt.beginReliableWrite();
                    Log.d(Settingsmsg,"Services: " + gatt.getServices().toString());
                    BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid,1,1);
                    characteristic.setValue("123");
                    gatt.writeCharacteristic(characteristic);
                    gatt.executeReliableWrite();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(Settingsmsg,"Disconnected from device");
                    disconnectGattServer();
                }
            }
        }

        private void connectDevice(BluetoothDevice device) {
            GattClientCallback gattClientCallback = new GattClientCallback();
            mGatt = device.connectGatt(getActivity().getApplicationContext(), false, gattClientCallback);
        }

        public void disconnectGattServer() {
            mConnected = false;
            if (mGatt != null) {
                mGatt.disconnect(); // redudant
                mGatt.close();
            }
        }

        // Vanlig Bluetooth
        private static void setActiveDevice() {
            int index = bluetoothList.findIndexOfValue(bluetoothList.getValue());
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (!(index==-1)) {
                activeDevice = (BluetoothDevice)pairedDevices.toArray()[index];
            }
        }

        public static void connectBluetooth() { // TODO Välj automatiskt Raspberrypien
            int index = bluetoothList.findIndexOfValue(bluetoothList.getValue());
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (!(index==-1)) {
                BluetoothDevice device = (BluetoothDevice)pairedDevices.toArray()[index];
                if (!ble&&!RxBle) {
                    connectThread = new ConnectThread(device);
                    Log.d(Settingsmsg, "Create Refcomm Socket: " + device.getName());
                } else if (RxBle) {
                    //if (device.getAddress().equals(raspberryPiMAC)) {
                        rxDevice = rxBleClient.getBleDevice(raspberryPiMAC);
                        connectionObservable = prepareConnectionObservable();
                        if (!infoRxbleOn) {
                            infoRxble();
                        }
                        connectRaspberryPi();
                        // TODO anslut automatiskt + ifsats som kollar om man redan är ansluten
                    /*} else {
                        rxDevice = rxBleClient.getBleDevice(device.getAddress());
                        connectRaspberryPi();
                        rxDevice.observeConnectionStateChanges().subscribe(
                                connectionState -> {
                                    Log.d(Settingsmsg, "observeConnectionStateChanges: " + connectionState); // TODO Undeersök vad som händer här
                                    //connectionState.equals(GATT_SUCCESS);
                                    //connectionState.equals(CONNECTED);
                                    // Process your way.
                                    switch (connectionState) {
                                        case CONNECTED:
                                            bluetoothConnect.setChecked(true);
                                        case DISCONNECTED:
                                            bluetoothConnect.setChecked(false);
                                    }
                                },
                                throwable -> {
                                    // Handle an error here.
                                }
                        );
                    }*/
                }
                uuid = device.getUuids()[0].getUuid();
                //Log.d(Settingsmsg, "UUID: " + uuid);
                //activeDevice = device;
                //bluetoothList.setSummary(activeDevice.getName()); // TODO
            } else {
                bluetoothList.setSummary("");
            }
            //Log.d(Settingsmsg,"No device selected");
        }

        private static void connectRaspberryPi() {
            //UUID characteristicUuid = (UUID) getIntent().getSerializableExtra(EXTRA_CHARACTERISTIC_UUID);
            //UUID characteristicUUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
            byte[] bytesToWrite = HexString.hexToBytes("Test message");
            if (disposable!=null) {
                Log.d(Settingsmsg, "is disposed: " + disposable.isDisposed());
            }
            Disposable disposableLocal = rxDevice.establishConnection(false) // <-- autoConnect flag
                    //.flatMapSingle(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUUID, bytesToWrite))
                    .subscribe(
                            rxBleConnection -> {
                                // All GATT operations are done through the rxBleConnection.
                                io.reactivex.Single <byte[]> value = rxBleConnection.readCharacteristic(characteristicUuid);
                                Log.d(Settingsmsg, "Read: " + value);
                                Log.d(Settingsmsg, "Services: " + rxBleConnection.discoverServices());
                                rxBleConnection.setupNotification(characteristicUuid);

                            },
                            /*characteristicValue -> {
                                Log.d(Settingsmsg,characteristicValue.toString());
                                // Characteristic value confirmed.
                            },*/
                            throwable -> {
                                // Handle an error here.
                            }
                    );
            disposable = disposableLocal;
            Log.d(Settingsmsg, "is disposed: " + disposable.isDisposed());
        }

        private static void connectRaspberryPi2() {
            //characteristicUuid = HEART_RATE_MEASUREMENT_CHAR_UUID;
            //characteristicUuid = characteristicUuid2; // error, null object
            Disposable connectionDisposable = connectionObservable
                    .flatMapSingle(RxBleConnection::discoverServices)
                    .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            characteristic -> {
                                updateUI(characteristic);
                                Log.i(Settingsmsg, "Hey, connection has been established!");
                            },
                            BluetoothSettings::onConnectionFailure,
                            BluetoothSettings::onConnectionFinished
                    );

            compositeDisposable.add(connectionDisposable);
        }

        private static Observable<RxBleConnection> prepareConnectionObservable() {
            return rxDevice
                    .establishConnection(false)
                    //.takeUntil(disconnectTriggerSubject)
                    .compose(ReplayingShare.instance());
        }

        private static void updateUI(BluetoothGattCharacteristic characteristic) {
            Log.d(Settingsmsg, characteristic != null ? "disconnect" : "connect");
            Log.d(Settingsmsg,"BluetoothGattCharacteristic.PROPERTY_WRITE: " + BluetoothGattCharacteristic.PROPERTY_WRITE);
            Log.d(Settingsmsg,"BluetoothGattCharacteristic.PROPERTY_READ: " + BluetoothGattCharacteristic.PROPERTY_READ);
            Log.d(Settingsmsg,"BluetoothGattCharacteristic.PROPERTY_NOTIFY: " + BluetoothGattCharacteristic.PROPERTY_NOTIFY);
            Log.d(Settingsmsg, "characteristic: " + characteristic);
        }

        private static void onConnectionFailure(Throwable throwable) {
            //noinspection ConstantConditions
            //Snackbar.make(getActivity().findViewById(R.id.layoutView), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
            Log.d(Settingsmsg,"Connection error: " + throwable);
            updateUI(null);
        }
        private static void onConnectionFinished() {
            Log.d(Settingsmsg, "Connection succeded");
            updateUI(null);
        }

        private static void read() {
            if (isConnected()||true) {
                Disposable disposable = connectionObservable
                        .firstOrError()
                        .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(bytes -> {
                            Log.d(Settingsmsg,"read: " + bytes.toString() + " + " + HexString.bytesToHex(bytes));
                        }, BluetoothSettings::onReadFailure);

                compositeDisposable.add(disposable);
            }
        }

        private static void read2() {
            rxDevice.establishConnection(false)
                    //.flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid))
                    .subscribe(characteristicValue -> {
                        // Read characteristic value.
                    });
        }

        private static void onReadFailure(Throwable throwable) {
            //noinspection ConstantConditions
            Log.d(Settingsmsg, "Read error: " + throwable);
        }

        private static boolean isConnected() {
            return rxDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
        }

        private static void infoRxble() {
            infoRxbleOn = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    rxDevice.observeConnectionStateChanges().subscribe(
                            connectionState -> {
                                Log.d(Settingsmsg, "observeConnectionStateChanges: " + connectionState); // TODO Undeersök vad som händer här
                                //connectionState.equals(GATT_SUCCESS);
                                //connectionState.equals(CONNECTED);
                                // Process your way.
                                switch (connectionState) {
                                    case CONNECTED:
                                        bluetoothConnect.setChecked(true);
                                    case DISCONNECTED:
                                        bluetoothConnect.setChecked(false);
                                }
                            },
                            throwable -> {
                                // Handle an error here.
                            }
                    );
                }
            }, 50);
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
                    if (connectThread != null) {
                        if (connectThread.isRunning()) {
                            connectThread.cancel();
                        }
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
                bluetoothList.setEnabled(true);
            } else {
                bluetoothList.setEnabled(false);
            }
        }

        /**
         * Activity for scanning and displaying available BLE devices.
         */
        private void scanLeDevice(final boolean enable) {
            final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
            if (enable) {
                bluetoothSearch.setChecked(true);
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        bluetoothAdapter.stopLeScan(mLeScanCallback);
                        //bluetoothLeScanner.stopScan(mLeScanCallback);
                        bluetoothSearch.setChecked(false);
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                //bluetoothLeScanner.startScan(mLeScanCallback);
                bluetoothAdapter.startLeScan(uuidRaspberryPi, mLeScanCallback);
            } else {
                mScanning = false;
                //bluetoothLeScanner.stopScan(mLeScanCallback);
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }

        // Hör till scanLeDevice
        private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(Settingsmsg,"Found: " + deviceName + deviceHardwareAddress);
                //enableBluetoothList(); TODO fixa så att listan uppdateras
                Toast.makeText(getActivity().getApplicationContext(), "Found: " + deviceName + " " + deviceHardwareAddress, Toast.LENGTH_SHORT).show();
            }
        };



        /*val leDeviceListAdapter: LeDeviceListAdapter = ...

        private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
                runOnUiThread {
            leDeviceListAdapter.addDevice(device)
            leDeviceListAdapter.notifyDataSetChanged()
        }
        }*/

        public static UUID getUUID() {
            //UUID uuid = device.getUuids()[0].getUuid();
            return uuid;
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
                                //connectBluetooth(); //TODO automatisk anslutning
                            } else {
                                bluetoothList.setSummary("No device avalible");
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
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(Settingsmsg, "Found: " + deviceName + " " + deviceHardwareAddress);
                    //Log.d(Settingsmsg,deviceName);
                    //enableBluetoothList(); TODO fixa så att listan uppdateras
                    if (deviceHardwareAddress.equals(raspberryPiMAC)) {
                        Toast.makeText(context, "Found " + deviceName, Toast.LENGTH_SHORT).show();
                        discoveredRaspberryPi=true;
                        bluetoothList.setValue(raspberryPiMAC);
                        bluetoothList.setSummary(bluetoothList.getEntry());
                        //Log.d(Settingsmsg, "Bondstate: " + device.getBondState());
                        if (device.getBondState()!=12) {
                            device.createBond();
                        }
                        //Log.d(Settingsmsg, "Bondstate: " + device.getBondState()); // Not bonded: 10, Bonding: 11, Bonded: 12

                    }
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
                    if (!discoveredRaspberryPi) {
                        Toast.makeText(context, "Did not find Raspberry Pi", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Search Finished", Toast.LENGTH_SHORT).show();
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
                        enableBluetoothList();
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_NONE.");
                        enableBluetoothList();
                    }
                }
            }
        };

        // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a
// result of read or notification operations.
        private static final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    //connected = true;
                    //updateConnectionState(R.string.connected);
                    //invalidateOptionsMenu();
                    bluetoothConnect.setChecked(true);
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    //connected = false;
                    //updateConnectionState(R.string.disconnected);
                    //invalidateOptionsMenu();
                    //clearUI();
                    bluetoothConnect.setChecked(false);
                } else if (BluetoothLeService.
                        ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    //displayGattServices(bluetoothLeService.getSupportedGattServices());
                    Log.d(Settingsmsg, "ACTION_GATT_SERVICES_DISCOVERED");
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
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

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        //log("Requested user enable Location. Try starting the scan again.");
    }
} // end of Settings.class
