package com.chalmers.respiradar;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import java.io.IOException;
import java.util.UUID;
import static com.chalmers.respiradar.BluetoothService.b;

/**
 * Thread to connect to Raspberry Pi
 * Determines when connected
 * Creates a Bluetooth socket from a Bluetooth device
 */
class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private boolean isRunning; // if the thread is running
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
        } catch (NullPointerException connectException) {
            // Should not come to here, but sometimes Bluetooth malfunctions
            isRunning = false;
            hasSocket = false;
            b.connectManager();
            return;
        }

        // The connection attempt succeeded.
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
        } catch (NullPointerException e) {
            //Log.e(TAG, "Could not close the client socket", e);
        }
        hasSocket=false;
    }

    boolean isRunning() {
        return isRunning;
    }

    boolean hasSocket() {
        return hasSocket;
    }
}
