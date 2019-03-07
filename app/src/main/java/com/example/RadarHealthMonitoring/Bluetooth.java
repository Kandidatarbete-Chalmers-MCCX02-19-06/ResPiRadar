package com.example.RadarHealthMonitoring;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class Bluetooth extends Service {

   /* public Bluetooth() { // constructor

    }*/

    void onMainDestroy() { // onDestroy for MainActivity
        unregisterReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverState); // viktigt att stänga av
        unregisterReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverAction);
        unregisterReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverSearch);
        unregisterReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverSearchFinished);
        unregisterReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverScan);
        unregisterReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverBondChange);

    }

    @Override
    public void onCreate() {
        Log.d("Bluetooth", "onCreate");
        IntentFilter BTIntentChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverState, BTIntentChange);
        IntentFilter BTIntentFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverAction, BTIntentFound);
        IntentFilter BTIntentSearch = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverSearch, BTIntentSearch);
        IntentFilter BTIntentSearchFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverSearchFinished, BTIntentSearchFinished);
        IntentFilter BTIntentScanChange = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverScan, BTIntentScanChange);
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(Settings.BluetoothSettings.BluetoothBroadcastReceiverBondChange, BTIntentBondChange);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Bluetooth", "onStartCommand");
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        Log.d("Bluetooth", "onDestroy");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Bluetooth", "onBind");
        return null;
    }
}
