package com.example.RadarHealthMonitoring;

import android.content.Context;
import android.view.View;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

/**
 * Skapar graferna
 */
class Graph {

    private double dataNumber = 0;
    private LineGraphSeries<DataPoint> series;
    private Context context;

    /**
     * Konstruktor
     * @param view
     * @param context
     */
    Graph(View view, final Context context) {
        //GraphView graph = (GraphView) findViewById(R.id.graphPulse);
        GraphView graph = (GraphView) view;
        series = new LineGraphSeries<>(new DataPoint[]{      //skapar en första mätserie
                new DataPoint(0, 0)
        });
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                //Context context = getApplicationContext();
                Toast.makeText(context, "Value "+dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
        graph.addSeries(series);        //lägger till serien med mätvärden till grafen.
        graph.getViewport().setScrollable(true);        //scrollable in horizontal (x-axis)
        //graph.getViewport().setScrollableY(true);
        graph.getViewport().setScalable(true);      //för zoomning
        //graph.getViewport().setScalableY(true);
        graph.getViewport().setXAxisBoundsManual(true);     //set Viewport window size
        graph.getViewport().setYAxisBoundsManual(true);
        //graph.getViewport().setMaxXAxisSize(1);
        //graph.getViewport().setMinY(-1.5);      //begränsar y-axeln med värden
        //graph.getViewport().setMaxY(1.5);
        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        //graph.getGridLabelRenderer().setVerticalAxisTitle("Heartrate (beats/min)");
        //graph.setTitle("Heartrate");
    }

    /**
     * Getter för Serierna
     * @return series
     */
    LineGraphSeries<DataPoint> getSeries() {
        return series;
    }
}
