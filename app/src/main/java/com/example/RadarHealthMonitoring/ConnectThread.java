package com.example.RadarHealthMonitoring;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import static com.example.RadarHealthMonitoring.Bluetooth.b;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothAutoConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothConnect;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothSearch;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bluetoothWrite;

class ConnectThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private boolean isRunning;
    private Handler handler = new Handler();
    private boolean hasSocket = false;
    private int delayTime;

    //BluetoothCommunicationService commService;
    //ConnectedThread connectedThread;

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
        hasSocket = true;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        b.bluetoothAdapter.cancelDiscovery();

        if (b.autoConnect) {
            delayTime = 100;
        } else {
            delayTime = 1;
        }
        //for (int i = 1; i<3;i++) {
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
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

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(mmSocket);
                b.bluetoothConnectChecked = true;
                b.bluetoothAutoConnectChecked = true;
                b.bluetoothWriteEnable = true;
                b.connected = true;
                if (b.bluetoothSettingsActive) {
                    bluetoothConnect.setChecked(true);
                    bluetoothAutoConnect.setChecked(true);
                    bluetoothSearch.setEnabled(false);
                    bluetoothWrite.setEnabled(true);
                }
                //b.connectedThread = new BluetoothCommunicationService.ConnectedThread(mmSocket);
                b.connectedThread = new ConnectedThread(mmSocket);
                b.connectedThread.start();
                Log.d(TAG,"The connection attempt succeeded.");
                Toast.makeText(b.getApplicationContext(), "Connected to Raspberry Pi", Toast.LENGTH_LONG).show();
            //}
        //}, 1); // delay maybe needed
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            Log.d(TAG,"try mmSocket.close()");
            //b.connectedThread.cancel();
            mmSocket.close();
            isRunning = false;
            b.bluetoothConnectChecked = false;
            b.bluetoothAutoConnectChecked = false;
            b.bluetoothSearchEnable = true;
            b.bluetoothWriteEnable = false;
            b.connected = false;
            if (b.bluetoothSettingsActive) {
                bluetoothConnect.setChecked(false);
                bluetoothAutoConnect.setChecked(false);
                bluetoothSearch.setEnabled(true);
                bluetoothWrite.setEnabled(false);
            }
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

    public BluetoothSocket getSocket() {
        return mmSocket;
    }
}
