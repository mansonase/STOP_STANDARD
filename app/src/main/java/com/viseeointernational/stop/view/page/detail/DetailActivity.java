package com.viseeointernational.stop.view.page.detail;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.mikephil.charting.data.BarEntry;
import com.viseeointernational.stop.App;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.view.adapter.LogAdapter;
import com.viseeointernational.stop.view.custom.ChartView;
import com.viseeointernational.stop.view.custom.DateDialog;
import com.viseeointernational.stop.view.custom.TimeDialog;
import com.viseeointernational.stop.view.page.BaseActivity;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetailActivity extends BaseActivity implements DetailActivityContract.View {

    private static final String TAG = DetailActivity.class.getSimpleName();

    public static final String KEY_ADDRESS = "address";

    private static final String SELECTED_TYPE = "selected_type";

    @Inject
    DetailActivityContract.Presenter presenter;

    @Inject
    LogAdapter adapter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.lv)
    ListView lv;
    @BindView(R.id.hour)
    RadioButton hour;
    @BindView(R.id.day)
    RadioButton day;
    @BindView(R.id.month)
    RadioButton month;
    @BindView(R.id.year)
    RadioButton year;
    @BindView(R.id.date)
    TextView date;
    @BindView(R.id.text_hour)
    TextView textHour;
    @BindView(R.id.year_chart)
    ChartView yearChart;
    @BindView(R.id.hour_chart)
    ChartView hourChart;
    @BindView(R.id.day_chart)
    ChartView dayChart;
    @BindView(R.id.month_chart)
    ChartView monthChart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        ((App) getApplication()).getAppComponent().detailActivityComponent().activity(this).build().inject(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("DETAILS");
        toolbar.setNavigationIcon(R.mipmap.ic_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        lv.setAdapter(adapter);

        hour.setOnCheckedChangeListener(onCheckedChangeListener);
        day.setOnCheckedChangeListener(onCheckedChangeListener);
        month.setOnCheckedChangeListener(onCheckedChangeListener);
        year.setOnCheckedChangeListener(onCheckedChangeListener);

        if (savedInstanceState != null) {
            presenter.setType(savedInstanceState.getInt(SELECTED_TYPE));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.takeView(this);
    }

    @Override
    protected void onPause() {
        presenter.dropView();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putInt(SELECTED_TYPE, presenter.getType());
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                switch (buttonView.getId()) {
                    case R.id.hour:
                        presenter.checkHour();
                        break;
                    case R.id.day:
                        presenter.checkDay();
                        break;
                    case R.id.month:
                        presenter.checkMonth();
                        break;
                    case R.id.year:
                        presenter.checkYear();
                        break;
                }
            }
        }
    };

    @Override
    public void showDate(String s) {
        date.setText(s);
    }

    @Override
    public void showHour(String s) {
        textHour.setText(s);
    }

    @Override
    public void showHourChecked() {
        hour.setOnCheckedChangeListener(null);
        hour.setChecked(true);
        hour.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public void showDayChecked() {
        day.setOnCheckedChangeListener(null);
        day.setChecked(true);
        day.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public void showMonthChecked() {
        month.setOnCheckedChangeListener(null);
        month.setChecked(true);
        month.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public void showYearChecked() {
        year.setOnCheckedChangeListener(null);
        year.setChecked(true);
        year.setOnCheckedChangeListener(onCheckedChangeListener);
    }


    @Override
    public void showCalendar(long baseTime, long currentTime) {
        new DateDialog(this, new DateDialog.Callback() {
            @Override
            public void onSelect(DateDialog dialog, int year, int month, int day) {
                presenter.changeDate(year, month, day);
            }
        }).show(baseTime, currentTime);
    }

    @Override
    public void showTimePicker(int hour) {
        new TimeDialog(this, new TimeDialog.Callback() {
            @Override
            public void onSelect(TimeDialog dialog, int hour) {
                presenter.changeHour(hour);
            }
        }).show(hour);
    }

    @Override
    public void showLog(List<String> list) {
        adapter.setData(list);
    }

    @Override
    public void showHourChart(List<BarEntry> list, int position) {
        hourChart.setVisibility(View.VISIBLE);
        dayChart.setVisibility(View.GONE);
        monthChart.setVisibility(View.GONE);
        yearChart.setVisibility(View.GONE);
        hourChart.setData(list, position, 20, 0.85f);
    }

    @Override
    public void showDayChart(List<BarEntry> list, int position) {
        hourChart.setVisibility(View.GONE);
        dayChart.setVisibility(View.VISIBLE);
        monthChart.setVisibility(View.GONE);
        yearChart.setVisibility(View.GONE);
        dayChart.setData(list, position, 20,0.85f);
    }

    @Override
    public void showMonthChart(List<BarEntry> list, int position) {
        hourChart.setVisibility(View.GONE);
        dayChart.setVisibility(View.GONE);
        monthChart.setVisibility(View.VISIBLE);
        yearChart.setVisibility(View.GONE);
        monthChart.setData(list, position, 20,0.85f);
    }

    @Override
    public void showYearChart(List<BarEntry> list) {
        hourChart.setVisibility(View.GONE);
        dayChart.setVisibility(View.GONE);
        monthChart.setVisibility(View.GONE);
        yearChart.setVisibility(View.VISIBLE);
        yearChart.setData(list, -1, 12,0.51f);
    }

    @OnClick({R.id.previous_day, R.id.date, R.id.next_day, R.id.previous_hour, R.id.text_hour, R.id.next_hour})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.previous_day:
                presenter.previousDay();
                break;
            case R.id.date:
                presenter.showCalendar();
                break;
            case R.id.next_day:
                presenter.nextDay();
                break;
            case R.id.previous_hour:
                presenter.previousHour();
                break;
            case R.id.text_hour:
//                presenter.showTimePicker();
                break;
            case R.id.next_hour:
                presenter.nextHour();
                break;
        }
    }
}
