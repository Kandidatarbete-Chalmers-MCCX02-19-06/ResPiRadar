package com.example.RadarHealthMonitoring.Archive;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.example.RadarHealthMonitoring.Settings;

import java.io.IOException;
import java.util.UUID;

import static com.example.RadarHealthMonitoring.Archive.SettingsArchiveWithBLE.BluetoothSettings.bluetoothAdapter;

class ConnectThreadOld extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private boolean isRunning;
    private Handler handler = new Handler();
    private boolean hasSocket = false;

    public ConnectThreadOld(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        UUID deviceUUID = device.getUuids()[0].getUuid();
        //UUID deviceUUID = UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb");
        Log.d("ConnectionThread", "UUID: " + deviceUUID);

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
            Log.d(TAG,"try device.createRfcommSocketToServiceRecord(deviceUUID)");
        } catch (IOException e) {
            Log.d(TAG, "Socket's create() method failed", e);
            Log.d(TAG,"failed device.createRfcommSocketToServiceRecord(deviceUUID)");
        }
        mmSocket = tmp;
        hasSocket=true;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        bluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            //mmDevice.createBond();
            isRunning = true;
            Log.d(TAG,"try mmSocket.connect()");
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            isRunning = false;
            Log.d(TAG,"failed mmSocket.connect()" + connectException + " Is connected: ");

            try {
                mmSocket.close();
                Log.d(TAG,"try mmSocket.close();");
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
                Log.d(TAG,"failed mmSocket.close();");
            }
            hasSocket=false;
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        //manageMyConnectedSocket(mmSocket);
        SettingsArchiveWithBLE.BluetoothSettings.bluetoothConnect.setChecked(true);
        Log.d(TAG,"The connection attempt succeeded.");
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            Log.d(TAG,"try mmSocket.close()");
            mmSocket.close();
            isRunning = false;
            SettingsArchiveWithBLE.BluetoothSettings.bluetoothConnect.setChecked(false);
        } catch (IOException e) {
            Log.d(TAG, "Could not close the client socket", e);
            Log.d(TAG,"failed mmSocket.close()");
        }
        hasSocket=false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean hasSocket() {
        return hasSocket;
    }

    public BluetoothDevice getDevice() {
        return mmDevice;
    }
}
