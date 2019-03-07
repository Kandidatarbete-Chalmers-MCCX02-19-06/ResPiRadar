package com.example.RadarHealthMonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.RadarHealthMonitoring.Archive.DiscoverBluetoothDevice;
import com.jjoe64.graphview.series.DataPoint;


/**
 * Huvudaktiviteten för appen
 */
public class MainActivity extends AppCompatActivity {

    private double dataNumber = 0;
    private Graph graphPulse;
    private Graph graphBreathe;
    private static final String msg = "MyActivity";

    Intent intentBluetooth;

    /**
     * Skapar huvudfönstret
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false); // så systemet inte sätter default
        /* Graphs */
        graphPulse = new Graph(findViewById(R.id.graphPulse),getApplicationContext());
        graphBreathe = new Graph(findViewById(R.id.graphBreathe),getApplicationContext());
        /* Bluetooth */
        intentBluetooth = new Intent(this, Bluetooth.class);
        startService(intentBluetooth);
        //Bluetooth Bluetooth = new Bluetooth();

        //Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();


        /*// Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);*/

    }

    /*// Create a BroadcastReceiver for ACTION_FOUND.
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
            Log.d(msg, "got to WANTED");
        }
    };*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(intentBluetooth);
    }

    /**
     * Skapar menyn i huvudfönstret
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Om knapparna i menyraden aktiveras
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intentSettings = new Intent(this, Settings.class);
                this.startActivity(intentSettings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void addData(View view) {        //knapp för att lägga till ett värde till serien
        double yPulse = Math.sin(dataNumber*2)/2+0.5;
        double yBreathe = Math.sin(dataNumber)/2+0.5;
        DataPoint dataPulse = new DataPoint(dataNumber,yPulse);
        DataPoint dataBreathe = new DataPoint(dataNumber,yBreathe);
        graphPulse.getSeries().appendData(dataPulse, true,100); // seriesPulse
        graphBreathe.getSeries().appendData(dataBreathe, true,100); // seriesBreathe
        dataNumber += 0.5;
    }

    public void discoverBluetoothDevice(View view) {
        //Intent intentBlue = new Intent(this, DiscoverBluetoothDevice.class); // getBaseContext()
        //intentBlue.
        //startService(intentBlue);
        startService(new Intent(getBaseContext(), DiscoverBluetoothDevice.class));
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        //bindService(intentBlue);
        Log.d(msg, "Service Started");

        /*JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        JobInfo jobInfo = new JobInfo.Builder(11, new ComponentName(this, DiscoverBluetoothDevice.class))
                // only add if network access is required
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        jobScheduler.schedule(jobInfo);*/
    }
    // Method to stop the service
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), DiscoverBluetoothDevice.class));
    }
}
