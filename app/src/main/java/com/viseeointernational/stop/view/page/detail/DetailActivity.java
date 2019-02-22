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
import com.viseeointernational.stop.view.page.BaseActivity;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetailActivity extends BaseActivity implements DetailActivityContract.View {

    private static final String TAG = DetailActivity.class.getSimpleName();

    public static final String KEY_ADDRESS = "address";

    private static final String CURRENT_TYPE = "current_type";
    private static final String CURRENT_TIME = "current_time";

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
    @BindView(R.id.chart)
    ChartView chart;
    @BindView(R.id.date)
    TextView date;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        ((App)getApplication()).getAppComponent().detailActivityComponent().activity(this).build().inject(this);

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
            int currentType = savedInstanceState.getInt(CURRENT_TYPE);
            long currentTime = savedInstanceState.getLong(CURRENT_TIME);
            presenter.setTypeAndTime(currentType, currentTime);
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
        outState.putInt(CURRENT_TYPE, presenter.getType());
        outState.putLong(CURRENT_TIME, presenter.getTime());
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                switch (buttonView.getId()) {
                    case R.id.hour:
                        presenter.setType(ChartType.HOUR);
                        break;
                    case R.id.day:
                        presenter.setType(ChartType.DAY);
                        break;
                    case R.id.month:
                        presenter.setType(ChartType.MONTH);
                        break;
                    case R.id.year:
                        presenter.setType(ChartType.YEAR);
                        break;
                }
            }
        }
    };

    @Override
    public void showTime(String s) {
        date.setText(s);
    }

    @Override
    public void showCalendar(long baseTime, long currentTime) {
        new DateDialog(this, new DateDialog.Callback() {
            @Override
            public void onSelect(DateDialog dialog, long time) {
                presenter.setTime(time);
            }
        }).show(baseTime, currentTime);
    }

    @Override
    public void showLog(List<String> list) {
        adapter.setData(list);
    }

    @Override
    public void showChart(List<BarEntry> list, int position) {
        chart.setData(list, position);
    }

    @OnClick({R.id.left, R.id.date, R.id.right})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.left:
                presenter.previousDay();
                break;
            case R.id.date:
                presenter.loadCalendar();
                break;
            case R.id.right:
                presenter.nextDay();
                break;
        }
    }
}
