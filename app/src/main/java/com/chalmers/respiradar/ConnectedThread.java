package com.chalmers.respiradar;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.chalmers.respiradar.BluetoothService.b;


class ConnectedThread extends Thread {

    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private static final String TAG = "ConnectedThread";
    static final String READ = "READ";
    static final String READ_VALUE = "READ_VALUE";

    ConnectedThread(BluetoothSocket socket) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                // Send the String data to MainActivity
                String readBuf = new String(mmBuffer,1,numBytes); // received data
                Intent readIntent = new Intent(ConnectedThread.READ);
                readIntent.putExtra(READ_VALUE,readBuf);
                b.sendBroadcast(readIntent);
            } catch (IOException e) {
                b.bluetoothDisconnected(true);
                if (b.connectThread != null) {
                    b.connectThread.cancel();
                }
                break;
            }
        }
    }

    // Call this from the main activity to send data to the remote device.
    void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
        }
    }
}

