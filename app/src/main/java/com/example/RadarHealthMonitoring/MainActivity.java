package com.example.RadarHealthMonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.jjoe64.graphview.series.DataPoint;

import static com.example.RadarHealthMonitoring.Bluetooth.b;


/**
 * Huvudaktiviteten för appen
 */
public class MainActivity extends AppCompatActivity {

    private double dataNumber = 0;
    private Graph graphPulse;
    private Graph graphBreathe;
    private static final String msg = "MyActivity";
    boolean bluetoothConnectFromMenu = false;
    MenuItem bluetoothMenuItem;
    static MainActivity m; // for static activity

    Intent intentBluetooth;

    /**
     * Skapar huvudfönstret
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m = MainActivity.this;
        PreferenceManager.setDefaultValues(this, R.xml.settings, false); // så systemet inte sätter default
        /* Graphs */
        graphPulse = new Graph(findViewById(R.id.graphPulse),getApplicationContext());
        graphBreathe = new Graph(findViewById(R.id.graphBreathe),getApplicationContext());
        /* Bluetooth */
        intentBluetooth = new Intent(this, Bluetooth.class);
        startService(intentBluetooth);

    }

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
        bluetoothMenuItem = menu.findItem(R.id.bluetooth);
        if(b.bluetoothOnChecked) {
            bluetoothMenuItem.setIcon(R.drawable.ic_bluetooth_white_24dp);
        }
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
            case R.id.bluetooth:
                // TODO if BT off
                if (!b.bluetoothOnChecked) {
                    b.startBluetooth(true);
                } else {
                    if (!b.bluetoothAutoConnectChecked) {
                        b.autoConnect = true;
                        b.autoConnect();
                        //return false;
                    } else {
                        if (b.connectThread != null) {
                            b.connectThread.cancel();
                            b.autoConnect = false;
                            b.bluetoothAdapter.cancelDiscovery();
                        } else {
                            //return true;
                        }
                    }
                }
                //return false;
                //return false;
                //bluetoothConnectFromMenu = !bluetoothConnectFromMenu;
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
}
