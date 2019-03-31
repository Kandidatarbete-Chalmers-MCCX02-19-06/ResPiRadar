package com.example.RadarHealthMonitoring;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

/**
 * Skapar graferna
 */
class Graph {

    private LineGraphSeries<DataPoint> series;
    private Context context;
    private GraphView graph;
    private int color;
    private boolean tapListener;

    /**
     * Konstruktor
     * @param view
     * @param context
     */
    Graph(View view, final Context context, int color, boolean tapListener) {
        this.context = context;
        this.color = color;
        this.tapListener = tapListener;
        //GraphView graph = (GraphView) findViewById(R.id.graphPulse);
        graph = (GraphView) view;
        //graph.setBackgroundColor(m.getResources().getColor(R.color.colorGraphBackground));
        graph.getGridLabelRenderer().setGridColor(context.getResources().getColor(R.color.colorGraphGrid));
        graph.getGridLabelRenderer().setHorizontalLabelsColor(context.getResources().getColor(R.color.colorGraphGrid));
        graph.getGridLabelRenderer().setVerticalLabelsColor(context.getResources().getColor(R.color.colorGraphGrid));
        graph.getViewport().setBackgroundColor(context.getResources().getColor(R.color.colorGraphBackground));
        graph.getGridLabelRenderer().setTextSize(25);
        graph.getGridLabelRenderer().setPadding(35);

        graph.addSeries(newSeries());        //lägger till serien med mätvärden till grafen.
        graph.getViewport().setScrollable(true);        //scrollable in horizontal (x-axis)
        //graph.getViewport().setScrollableY(true);
        graph.getViewport().setScalable(true);      //för zoomning
        //graph.getViewport().setScalableY(true);
        graph.getViewport().setXAxisBoundsManual(true);     //set Viewport window size
        //graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(60);
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

    void resetSeries() {
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(60);
        graph.removeAllSeries();
        graph.addSeries(newSeries());
    }

    private LineGraphSeries<DataPoint> newSeries() {
        series = new LineGraphSeries<>();  //skapar en första mätserie
        series.setThickness(13);
        series.setColor(color);
        if (tapListener) {
            series.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    //Context context = getApplicationContext();
                    Toast.makeText(context, "" + String.format("%.0f", dataPoint.getY()) + " bpm at time " + String.format("%.0f", dataPoint.getX()) + " s", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return series;
    }

    Viewport getViewport() {
        return graph.getViewport();
    }

    void fixGraphView(Object[] dataPoints) {
        graph.removeAllSeries();
        graph.addSeries(newSeries());
        for (Object data : dataPoints) {
            series.appendData((DataPoint) data, false, 1000, true);
        }
    }
}
