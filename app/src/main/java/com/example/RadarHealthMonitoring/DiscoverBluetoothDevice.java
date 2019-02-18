package com.example.RadarHealthMonitoring;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


public class DiscoverBluetoothDevice extends Service {
    private static final String Bmsg = "BluetoothMyActivity";

    /*public DiscoverBluetoothDevice(String name) {
        super(name);
    }*/

    /*public DiscoverBluetoothDevice () {

    }*/

    //@Override
    /*protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();
        Log.d(Bmsg, "got to onHandleIntent");
        // Do work here, based on the contents of dataString
        Toast.makeText(this, "got to onHandleIntent", Toast.LENGTH_LONG).show();
    }*/

    //@Override
    /*public void onCreate(Bundle savedInstanceState) {
    //...
        //super.onCreate(savedInstanceState);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        Log.d(Bmsg, "got to onCreate");
        Toast.makeText(this, "got to onCreate", Toast.LENGTH_LONG).show();
    }*/

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    //...
        Log.d(Bmsg, "Stop Service");
        Toast.makeText(this, "Stop Service", Toast.LENGTH_LONG).show();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Bmsg, "got to onBind");
        Toast.makeText(this, "got to onBond", Toast.LENGTH_LONG).show();
        return null;
    }

    /*@Override
    public boolean onStartJob(JobParameters params) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        Log.d(Bmsg, "got to onStartCommand");
        Toast.makeText(this, "got to onStartCommand", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(Bmsg, "Stop Service");
        Toast.makeText(this, "Stop Service", Toast.LENGTH_LONG).show();
        unregisterReceiver(receiver);
        return false;
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        Log.d(Bmsg, "got to onStartCommand");
        Toast.makeText(this, "got to onStartCommand", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }
}
