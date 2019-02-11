package com.example.RadarHealthMonitoring;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

//GraphView
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

/**
 * Huvudaktiviteten för appen
 */
public class MainActivity extends AppCompatActivity {
    private double dataNumber = 0;
    private LineGraphSeries<DataPoint> seriesPulse;
    private LineGraphSeries<DataPoint> seriesBreathe;
    /**
     * Skapar huvudfönstret
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false); // så systemet inte sätter default

        //Graphs
        GraphView graphPulse = (GraphView) findViewById(R.id.graphPulse);
        seriesPulse = new LineGraphSeries<>(new DataPoint[]{      //skapar en första mätserie
                new DataPoint(0, 0)
        });
        seriesPulse.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Context context = getApplicationContext();
                Toast.makeText(context, "Value "+dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
        graphPulse.addSeries(seriesPulse);        //lägger till serien med mätvärden till grafen.
        graphPulse.getViewport().setScrollable(true);        //scrollable in horizontal (x-axis)
        graphPulse.getViewport().setScrollableY(true);
        graphPulse.getViewport().setScalable(true);      //för zoomning
        graphPulse.getViewport().setScalableY(true);
        graphPulse.getViewport().setXAxisBoundsManual(true);     //set Viewport window size
        graphPulse.getViewport().setYAxisBoundsManual(true);
        //graph.getViewport().setMaxXAxisSize(1);
        //graph.getViewport().setMinY(-1.5);      //begränsar y-axeln med värden
        //graph.getViewport().setMaxY(1.5);
        graphPulse.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        //graphPulse.getGridLabelRenderer().setVerticalAxisTitle("Heartrate (beats/min)");
        //graphPulse.setTitle("Heartrate");

        GraphView graphBreathe = (GraphView) findViewById(R.id.graphBreathe);
        seriesBreathe = new LineGraphSeries<>(new DataPoint[]{      //skapar en första mätserie
                new DataPoint(0, 0)
        });
        seriesBreathe.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Context context = getApplicationContext();
                Toast.makeText(context, "Value "+dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
        graphBreathe.addSeries(seriesBreathe);        //lägger till serien med mätvärden till grafen.
        graphBreathe.getViewport().setScrollable(true);        //scrollable in horizontal (x-axis)
        graphBreathe.getViewport().setScrollableY(true);
        graphBreathe.getViewport().setScalable(true);      //för zoomning
        graphBreathe.getViewport().setScalableY(true);
        graphBreathe.getViewport().setXAxisBoundsManual(true);     //set Viewport window size
        graphBreathe.getViewport().setYAxisBoundsManual(true);
        //graph.getViewport().setMaxXAxisSize(1);
        //graph.getViewport().setMinY(-1.5);      //begränsar y-axeln med värden
        //graph.getViewport().setMaxY(1.5);
        graphBreathe.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        //graphBreathe.getGridLabelRenderer().setVerticalAxisTitle("Breathing rate (beats/min)");
        //graphBreathe.setTitle("Breathing rate");
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
        double y = Math.sin(dataNumber);
        DataPoint data = new DataPoint(dataNumber,y);
        seriesPulse.appendData(data, true,100);
        seriesBreathe.appendData(data, true,100);
        dataNumber += 0.5;
    }

}
