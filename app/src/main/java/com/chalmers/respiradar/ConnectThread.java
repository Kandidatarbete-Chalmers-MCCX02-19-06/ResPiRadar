package com.chalmers.respiradar;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;
import static com.chalmers.respiradar.BluetoothService.b;

class ConnectThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothSocket mmSocket;
    private boolean isRunning;
    private boolean hasSocket;

    ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        UUID deviceUUID;
        if (!(device.getUuids() == null)) {
            deviceUUID = device.getUuids()[0].getUuid();
        } else {
            deviceUUID = UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb"); // default uuid
        }
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
        } catch (IOException e) {
            //Log.e(TAG, "Socket's create() method failed", e);
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
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            isRunning = false;
            //Log.e(TAG,"failed mmSocket.connect()" + connectException);

            try {
                mmSocket.close();
            } catch (IOException closeException) {
                //Log.e(TAG, "Could not close the client socket", closeException);
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
        Intent intent = new Intent(BluetoothService.TOAST);
        intent.putExtra(b.TEXT,b.getString(R.string.connected_to_rpi));
        b.sendBroadcast(intent);
    }

    // Closes the client socket and causes the thread to finish.
    void cancel() {
        isRunning = false;
        try {
            mmSocket.close();
            b.bluetoothDisconnected(true);
        } catch (IOException e) {
            //Log.e(TAG, "Could not close the client socket", e);
        }
        hasSocket=false;
    }

    boolean isRunning() {
        return isRunning;
    } // only for run

    boolean hasSocket() {
        return hasSocket;
    }
}
