package com.chalmers.respiradar;

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

import java.util.Locale;

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
    Graph(View view, final Context context, int color, boolean tapListener, int screenWidth) {
        this.context = context;
        this.color = color;
        this.tapListener = tapListener;
        // Graph
        graph = (GraphView) view;
        graph.getGridLabelRenderer().setGridColor(context.getResources().getColor(R.color.colorGraphGrid));
        graph.getGridLabelRenderer().setHorizontalLabelsColor(context.getResources().getColor(R.color.colorGraphGrid));
        graph.getGridLabelRenderer().setVerticalLabelsColor(context.getResources().getColor(R.color.colorGraphGrid));
        graph.getViewport().setBackgroundColor(context.getResources().getColor(R.color.colorGraphBackground));
        graph.getGridLabelRenderer().setTextSize((float)(25*screenWidth/720)); // Scale text size and padding to screen size
        graph.getGridLabelRenderer().setPadding((int)(35*screenWidth/720));
        // Series
        graph.addSeries(newSeries());        //lägger till serien med mätvärden till grafen.
        graph.getViewport().setScrollable(true);        //scrollable in horizontal (x-axis)
        graph.getViewport().setScalable(true);      //för zoomning
        graph.getViewport().setXAxisBoundsManual(true);     //set Viewport window size
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(60);
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
                    Toast.makeText(context, String.format(Locale.getDefault(),"%.0f",
                            dataPoint.getY()) + " bpm at time " + String.format(Locale.getDefault(),"%.0f",
                            dataPoint.getX()) + " s", Toast.LENGTH_SHORT).show();
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
