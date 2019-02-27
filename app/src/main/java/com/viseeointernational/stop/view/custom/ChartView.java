package com.viseeointernational.stop.view.custom;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.viseeointernational.stop.R;

import java.util.ArrayList;
import java.util.List;

public class ChartView extends BarChart {

    private static final String TAG = ChartView.class.getSimpleName();

    public ChartView(Context context) {
        super(context);
        myInit();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        myInit();
    }

    public ChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        myInit();
    }

    protected void myInit() {
        getDescription().setEnabled(false);
        getLegend().setEnabled(false);
        setNoDataText("No data");
        setNoDataTextColor(getResources().getColor(R.color.text));

        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(getResources().getColor(R.color.textBlue));
        xAxis.setAxisLineWidth(2);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setTextSize(12);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(5, false);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (list.size() > (int) value) {
                    BarEntry barEntry = list.get((int) value);
                    Object object = barEntry.getData();
                    if (object instanceof String) {
                        return (String) object;
                    }
                }
                return "";
            }
        });

        YAxis yAxis = getAxisLeft();
        yAxis.setAxisLineColor(getResources().getColor(R.color.textBlue));
        yAxis.setTextColor(getResources().getColor(R.color.text));
        yAxis.setTextSize(12);
        yAxis.setDrawLabels(false);
        yAxis.setAxisLineWidth(2);
        yAxis.setDrawGridLines(false);
        yAxis.setAxisMinimum(0);
        yAxis.setLabelCount(5, false);
        yAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return String.valueOf((int) value);
            }
        });

        YAxis rightAxis = getAxisRight();
        rightAxis.setAxisLineColor(getResources().getColor(R.color.textBlue));
        rightAxis.setAxisLineWidth(2);
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawLabels(false);
        rightAxis.setAxisMinimum(0);

        setDoubleTapToZoomEnabled(false);
        setPinchZoom(true);
    }

    private IValueFormatter iValueFormatter = new IValueFormatter() {
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            if ((int) value == 0) {
                return "";
            }
            return (int) value + "";
        }
    };

    private List<BarEntry> list = new ArrayList<>();

    public void setData(List<BarEntry> list, int position) {
        Log.d(TAG, list.size() + "  " + position);
        this.list.clear();
        this.list.addAll(list);
        clear();
        BarDataSet dataSet;
        if (getData() != null && getData().getDataSetCount() != 0) {
            dataSet = (BarDataSet) getData().getDataSetByIndex(0);
            dataSet.setValues(list);
            getData().notifyDataChanged();
            notifyDataSetChanged();
        } else {
            dataSet = new BarDataSet(list, "");
            dataSet.setDrawValues(false);
            dataSet.setColor(getResources().getColor(R.color.themeDarkDark));
            dataSet.setValueFormatter(iValueFormatter);
            dataSet.setDrawValues(true);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(14);
            BarData barData = new BarData(dataSet);
            setData(barData);
        }
        setVisibleXRange(20, 20);
        setVisibleXRange(list.size(), 20);
        if (position != -1) {
            zoom(1, 1, position, 1, YAxis.AxisDependency.LEFT);
        }
//        zoom(1,1, 100, 0);
    }

}
