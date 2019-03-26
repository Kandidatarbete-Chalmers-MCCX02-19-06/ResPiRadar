package com.example.RadarHealthMonitoring;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.RadarHealthMonitoring.Bluetooth.b;


public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    private static final String TAG = "ConnectedThread";
    //private Handler handler = new Handler(); // handler that gets info from Bluetooth service
    public final int MESSAGE_READ = 0;
    public final int MESSAGE_WRITE = 1;
    public final int MESSAGE_TOAST = 2;
    public static final String READ = "READ";
    public static final String READ_VALUE = "READ_VALUE";

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
               /* Message readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        mmBuffer);
                readMsg.sendToTarget();*/
                String readBuf = new String(mmBuffer,0,numBytes); // received data
                //Log.d(TAG, "Message recieved: " + readBuf);
                Intent readIntent = new Intent(ConnectedThread.READ);
                readIntent.putExtra(READ_VALUE,readBuf);
                b.sendBroadcast(readIntent);
            } catch (IOException e) {
                Log.e(TAG, "Input stream was disconnected", e);
                b.bluetoothDisconnected(true);
                b.connectThread.cancel();
                break;
            }
        }
    }

    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

            // Share the sent message with the UI activity.
            /*Message writtenMsg = handler.obtainMessage(
                    MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
            Log.d(TAG, writtenMsg.toString());*/
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            /*Message writeErrorMsg =
                    handler.obtainMessage(MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);*/
        }
    }
}

