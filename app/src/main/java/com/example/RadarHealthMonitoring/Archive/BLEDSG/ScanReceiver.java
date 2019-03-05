package com.example.RadarHealthMonitoring.Archive.BLEDSG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.polidea.rxandroidble2.exceptions.BleScanException;

import androidx.annotation.RequiresApi;

public class ScanReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        //BackgroundScanner backgroundScanner = Settings.BluetoothSettings.getRxBleClient().getBackgroundScanner();

        try {
            //final List<ScanResult> scanResults = backgroundScanner.onScanResultReceived(intent);
            //Log.i("ScanReceiver", "Scan results received: " + scanResults);
        } catch (BleScanException exception) {
            Log.w("ScanReceiver", "Failed to scan devices", exception);
        }
    }
}
