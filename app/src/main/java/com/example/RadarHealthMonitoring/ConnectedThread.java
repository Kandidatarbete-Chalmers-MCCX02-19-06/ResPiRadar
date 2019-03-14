package com.example.RadarHealthMonitoring;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.RadarHealthMonitoring.Bluetooth.b;
import static com.example.RadarHealthMonitoring.Settings.BluetoothSettings.bs;


public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler handler = new Handler(); // handler that gets info from Bluetooth service
    public final int MESSAGE_READ = 0;
    public final int MESSAGE_WRITE = 1;
    public final int MESSAGE_TOAST = 2;

    public ConnectedThread(BluetoothSocket socket) {
        Log.d(TAG,"constructor ConnectedThread");
        mmSocket = socket;
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
                // Send the obtained bytes to the UI activity.
                Message readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        mmBuffer);
                readMsg.sendToTarget();
                String readBuf = new String(mmBuffer,0,numBytes); // received data
                Log.d(TAG, "Message recieved: " + readBuf);
            } catch (IOException e) {
                Log.e(TAG, "Input stream was disconnected", e);
                b.connected = false;
                b.bluetoothConnectChecked = false;
                b.bluetoothAutoConnectChecked = false;
                b.bluetoothSearchEnable = true;
                b.bluetoothWriteEnable = false;
                if (b.bluetoothSettingsActive) {
                    bs.connectedThreadDisconnect();
                }
                break;
            }
        }
    }

    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

            // Share the sent message with the UI activity.
            Message writtenMsg = handler.obtainMessage(
                    MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
            Log.d(TAG, writtenMsg.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    handler.obtainMessage(MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }

    // Call this method from the main activity to shut down the connection.
    public void cancel() { // TODO Remove
        b.connected = false;
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}

