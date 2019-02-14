package com.example.RadarHealthMonitoring;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.jjoe64.graphview.series.DataPoint;


/**
 * Huvudaktiviteten för appen
 */
public class MainActivity extends AppCompatActivity {

    private double dataNumber = 0;
    private Graph graphPulse;
    private Graph graphBreathe;

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
}
