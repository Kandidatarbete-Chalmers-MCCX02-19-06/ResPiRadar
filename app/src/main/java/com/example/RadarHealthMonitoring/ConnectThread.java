package com.example.RadarHealthMonitoring;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import static com.example.RadarHealthMonitoring.Bluetooth.b;

class ConnectThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private boolean isRunning;
    private Handler handler = new Handler();
    private boolean hasSocket = false;
    private int delayTime;

    ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        UUID deviceUUID;
        if (!(device.getUuids() == null)) {
            deviceUUID = device.getUuids()[0].getUuid();
        } else {
            deviceUUID = UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb");
        }
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
        hasSocket = true;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        isRunning = true;
        b.uiBluetoothConnecting();
        b.bluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            Log.d(TAG,"try mmSocket.connect()");
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            isRunning = false;
            Log.d(TAG,"failed mmSocket.connect()" + connectException);

            try {
                mmSocket.close();
                Log.d(TAG,"try mmSocket.close();");
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
                Log.d(TAG,"failed mmSocket.close();");
            }
            hasSocket = false;
            b.connectManager();
            return;
        }

        // The connection attempt succeeded. Perform work associated with the connection in a separate thread.
        isRunning = false;
        b.bluetoothConnected();
        b.connectedThread = new ConnectedThread(mmSocket);
        b.connectedThread.start();
        Log.d(TAG,"The connection attempt succeeded.");
        Intent intent = new Intent(Bluetooth.TOAST);
        intent.putExtra(b.TEXT,"Connected to Raspberry Pi");
        b.sendBroadcast(intent);
        //Toast.makeText(b.getApplicationContext(), "Connected to Raspberry Pi", Toast.LENGTH_LONG).show();
    }

    // Closes the client socket and causes the thread to finish.
    void cancel() {
        Log.d("ConnectThread", "cancel");
        isRunning = false;
        try {
            Log.d(TAG,"try mmSocket.close()");
            mmSocket.close();
            b.bluetoothDisconnected(false);
        } catch (IOException e) {
            Log.d(TAG, "Could not close the client socket", e);
            Log.d(TAG,"failed mmSocket.close()");
        }
        hasSocket=false;
    }

    boolean isRunning() {
        return isRunning;
    } // only for run

    boolean hasSocket() {
        return hasSocket;
    }

    void setHasSocket(boolean hasSocket) {
        this.hasSocket = hasSocket;
    }
}
