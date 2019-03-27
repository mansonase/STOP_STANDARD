package com.viseeointernational.stop.view.page.detail;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.github.mikephil.charting.data.BarEntry;
import com.viseeointernational.stop.data.constant.ChartType;
import com.viseeointernational.stop.data.constant.StateType;
import com.viseeointernational.stop.data.constant.TimeFormatType;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.data.source.base.database.StateDao;
import com.viseeointernational.stop.data.source.device.DeviceSource;
import com.viseeointernational.stop.util.TimeUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DetailActivityPresenter implements DetailActivityContract.Presenter {

    private static final String TAG = DetailActivityPresenter.class.getSimpleName();

    private DetailActivityContract.View view;

    private DeviceSource deviceSource;
    private StateDao stateDao;

    private int year;
    private int month;
    private int day;
    private int hour;
    private int type;// hour day month year
    private String timeFormat;// device 时间格式
    private String monthXAxisFormat;
    private String yearXAxisFormat;

    private long from;// 统计图数据时间范围
    private long to;// 统计图数据时间范围
    private long interval;// 采样间隔
    private String xAxisFormat;// x轴值时间格式

    private List<State> tempLogData = new ArrayList<>();
    private List<State> tempChartData = new ArrayList<>();

    @Inject
    String address;

    @Inject
    public DetailActivityPresenter(DeviceSource deviceSource, StateDao stateDao) {
        this.deviceSource = deviceSource;
        this.stateDao = stateDao;
    }

    @Override
    public void takeView(final DetailActivityContract.View view) {
        this.view = view;
        deviceSource.setMovementListener(address, listener);
        init();
    }

    @Override
    public void dropView() {
        deviceSource.setMovementListener("", null);
        view = null;
    }

    private DeviceSource.MovementListener listener = new DeviceSource.MovementListener() {
        @Override
        public void onMovementReceived(@NonNull State state, @NonNull String timeFormat) {
//            if (from <= state.time && state.time < to) {
//                tempLogData.add(0, state);
//                tempChartData.add(0, state);
//                List<String> logs = makeLog(tempLogData, timeFormat);
//                if (view != null) {
//                    view.showLog(logs);
//                }
//
//                int position = makeChartCurrentPosition(false, from, to, interval);
//                List<State> chartData = new ArrayList<>();
//                chartData.addAll(tempChartData);
//                List<BarEntry> data = makeChart(chartData, from, to, interval, xAxisFormat);
//                if (view != null) {
//                    view.showChart(data, position);
//                }
//            }
        }
    };

    private void init() {
        if (year == 0 || day == 0 || hour == 0) {
            Calendar calendar = Calendar.getInstance();
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
        }
        if (type != 0 && !TextUtils.isEmpty(timeFormat)) {
            showCheckType(type);
            showDate(year, month, day);
            showHour(hour);
            updateDataByTime(year, month, day, hour, type);
            return;
        }
        deviceSource.getDevice(address, new DeviceSource.GetDeviceCallback() {
            @Override
            public void onDeviceLoaded(Device device) {
                timeFormat = device.timeFormat;
                monthXAxisFormat = getMonthXAxisFormat(timeFormat);
                yearXAxisFormat = getYearXAxisFormat(timeFormat);
                if (type == 0) {
                    type = device.defaultShow;
                }
                showCheckType(type);
                showDate(year, month, day);
                showHour(hour);
                updateDataByTime(year, month, day, hour, type);
            }

            @Override
            public void onDeviceNotAvailable() {
            }
        });
    }

    private void showCheckType(int type) {
        if (view != null) {
            switch (type) {
                case ChartType.HOUR:
                    view.showHourChecked();
                    break;
                case ChartType.DAY:
                    view.showDayChecked();
                    break;
                case ChartType.MONTH:
                    view.showMonthChecked();
                    break;
                case ChartType.YEAR:
                    view.showYearChecked();
                    break;
            }
        }
    }

    private void showDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        if (view != null) {
            view.showDate(TimeUtil.getTime(calendar.getTimeInMillis(), timeFormat));
        }
    }

    private void showHour(int hour) {
        String shour = hour + "";
        if (shour.length() == 1) {
            shour = "0" + shour;
        }
        shour += ":00";
        if (view != null) {
            view.showHour(shour);
        }
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void checkHour() {
        type = ChartType.HOUR;
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void checkDay() {
        type = ChartType.DAY;
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void checkMonth() {
        type = ChartType.MONTH;
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void checkYear() {
        type = ChartType.YEAR;
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void changeDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        showDate(year, month, day);
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void changeHour(int hour) {
        this.hour = hour;
        showHour(hour);
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void showCalendar() {
        if (view != null) {
            Calendar calendar = Calendar.getInstance();
            long today = calendar.getTimeInMillis();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            long currentTime = calendar.getTimeInMillis();
            view.showCalendar(today, currentTime);
        }
    }

    @Override
    public void previousDay() {
        day--;
        showDate(year, month, day);
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void nextDay() {
        day++;
        showDate(year, month, day);
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void showTimePicker() {
        if (view != null) {
            view.showTimePicker(hour);
        }
    }

    @Override
    public void previousHour() {
        if (hour > 0) {
            hour--;
        } else {
            day--;
            hour = 23;
            showDate(year, month, day);
        }
        showHour(hour);
        updateDataByTime(year, month, day, hour, type);
    }

    @Override
    public void nextHour() {
        if (hour < 23) {
            hour++;
        } else {
            day++;
            hour = 0;
            showDate(year, month, day);
        }
        showHour(hour);
        updateDataByTime(year, month, day, hour, type);
    }

    private String getMonthXAxisFormat(String timeFormat) {
        switch (timeFormat) {
            case TimeFormatType.DATE_1_1:
            case TimeFormatType.DATE_1_2:
            default:
                return "MM/dd";
            case TimeFormatType.DATE_1_3:
                return "dd/MM";
            case TimeFormatType.DATE_2_1:
            case TimeFormatType.DATE_2_2:
                return "MM-dd";
            case TimeFormatType.DATE_2_3:
                return "dd-MM";
            case TimeFormatType.DATE_3_1:
            case TimeFormatType.DATE_3_2:
                return "MM.dd";
            case TimeFormatType.DATE_3_3:
                return "dd.MM";
        }
    }

    private String getYearXAxisFormat(String timeFormat) {
        switch (timeFormat) {
            case TimeFormatType.DATE_1_1:
            default:
                return "yyyy/MM";
            case TimeFormatType.DATE_1_2:
            case TimeFormatType.DATE_1_3:
                return "MM/yyyy";
            case TimeFormatType.DATE_2_1:
                return "yyyy-MM";
            case TimeFormatType.DATE_2_2:
            case TimeFormatType.DATE_2_3:
                return "MM-yyyy";
            case TimeFormatType.DATE_3_1:
                return "yyyy.MM";
            case TimeFormatType.DATE_3_2:
            case TimeFormatType.DATE_3_3:
                return "MM.yyyy";
        }
    }

    private void updateDataByTime(int year, int month, int day, int hour, int type) {// log和统计图都更新
        switch (type) {
            case ChartType.HOUR:
            default:
                getDayData(year, month, day);
                break;
            case ChartType.DAY:
                getDayData(year, month, day);
                break;
            case ChartType.MONTH:
                getDayData(year, month, day);
                break;
            case ChartType.YEAR:
                getDayData(year, month, day);
                break;
        }
    }

    private void getHourData(int year, int month, int day, int hour) {

    }

    private void getDayData(final int year, final int month, final int day) {
        Observable.just(1)
                .map(new Function<Integer, List<BarEntry>>() {
                    @Override
                    public List<BarEntry> apply(Integer integer) throws Exception {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        List<BarEntry> ret = new ArrayList<>();
                        for (int i = 0; i < 24; i++) {
                            calendar.set(Calendar.HOUR_OF_DAY, i);
                            long from = calendar.getTimeInMillis();
                            calendar.set(Calendar.HOUR_OF_DAY, i + 1);
                            long to = calendar.getTimeInMillis();
                            List<State> states = stateDao.getStateAsc(address, from, to);
                            int count = 0;
                            for (int j = 0; j < states.size(); j++) {
                                State state = states.get(j);
                                count += state.movementsCount;
                            }
                            BarEntry barEntry = new BarEntry(i, count, TimeUtil.getTime(from, yearXAxisFormat));
                            ret.add(barEntry);
                        }
                        return ret;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<BarEntry>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (view != null) {
                            view.showLoading();
                        }
                    }

                    @Override
                    public void onNext(List<BarEntry> barEntries) {
                        if (view != null) {
                            view.cancelLoading();
                            view.showChart(barEntries, -1);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (view != null) {
                            view.cancelLoading();
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void getMonthData(int year, int month) {

    }

    private void getYearData(final int year) {
        Observable.just(1)
                .map(new Function<Integer, List<BarEntry>>() {
                    @Override
                    public List<BarEntry> apply(Integer integer) throws Exception {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        List<BarEntry> ret = new ArrayList<>();
                        for (int i = 0; i < 12; i++) {
                            calendar.set(Calendar.MONTH, i);
                            long from = calendar.getTimeInMillis();
                            calendar.set(Calendar.MONTH, i + 1);
                            long to = calendar.getTimeInMillis();
                            List<State> states = stateDao.getStateAsc(address, from, to);
                            int count = 0;
                            for (int j = 0; j < states.size(); j++) {
                                State state = states.get(j);
                                count += state.movementsCount;
                            }
                            BarEntry barEntry = new BarEntry(i, count, TimeUtil.getTime(from, yearXAxisFormat));
                            ret.add(barEntry);
                        }
                        return ret;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<BarEntry>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (view != null) {
                            view.showLoading();
                        }
                    }

                    @Override
                    public void onNext(List<BarEntry> barEntries) {
                        if (view != null) {
                            List<String> list = new ArrayList<>();
                            for (int i = 0; i < barEntries.size(); i++) {
                                list.add(barEntries.get(i).getData() + "  -->  " + (int) barEntries.get(i).getX() + "  movements");
                            }

                            view.cancelLoading();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (view != null) {
                            view.cancelLoading();
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void createChartParameters(long time, int type, String timeFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        switch (type) {
            case ChartType.HOUR:
            case ChartType.DAY:
            default:
                calendar.set(Calendar.HOUR_OF_DAY, 0);// 当天0时
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                from = calendar.getTimeInMillis();
                to = from + 60000 * 60 * 24;
                switch (type) {
                    default:
                        interval = 60000;// 1分钟
                        xAxisFormat = "HH:mm";
                        break;
                    case ChartType.DAY:
                        interval = 60000 * 60;// 1小时
                        xAxisFormat = "HH:00";
                        break;
                }
                break;
            case ChartType.MONTH:
            case ChartType.YEAR:
                switch (timeFormat) {
                    case TimeFormatType.DATE_1_1:
                    case TimeFormatType.DATE_1_2:
                    default:
                        xAxisFormat = "MM/dd";
                        break;
                    case TimeFormatType.DATE_1_3:
                        xAxisFormat = "dd/MM";
                        break;
                    case TimeFormatType.DATE_2_1:
                    case TimeFormatType.DATE_2_2:
                        xAxisFormat = "MM-dd";
                        break;
                    case TimeFormatType.DATE_2_3:
                        xAxisFormat = "dd-MM";
                        break;
                    case TimeFormatType.DATE_3_1:
                    case TimeFormatType.DATE_3_2:
                        xAxisFormat = "MM.dd";
                        break;
                    case TimeFormatType.DATE_3_3:
                        xAxisFormat = "dd.MM";
                        break;
                }
                switch (type) {
                    case ChartType.MONTH:
                        int month = calendar.get(Calendar.MONTH);
                        calendar.set(Calendar.DAY_OF_MONTH, 1);// 月初
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        from = calendar.getTimeInMillis();
                        calendar.set(Calendar.MONTH, month + 1);
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        to = calendar.getTimeInMillis();
                        interval = 60000 * 60 * 24;// 1天
                        break;
                    case ChartType.YEAR:
                        int year = calendar.get(Calendar.YEAR);
                        calendar.set(Calendar.WEEK_OF_YEAR, 1);// 年第一周周日开始
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        from = calendar.getTimeInMillis();
                        calendar.set(Calendar.YEAR, year + 1);
                        calendar.set(Calendar.WEEK_OF_YEAR, 1);// 年第一周周日开始
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        to = calendar.getTimeInMillis();
                        interval = 60000l * 60l * 24l * 7l;// 7天
                        break;
                }
                break;
        }
    }

    private void getChartStates(String address, final long from, final long to) {
        deviceSource.getStatesDesc(address, from, to, new DeviceSource.GetStatesDescCallback() {
            @Override
            public void onStatesLoaded(List<State> states) {
                tempChartData.clear();
                tempChartData.addAll(states);
                int position = makeChartCurrentPosition(true, from, to, interval);
                List<BarEntry> data = makeChart(states, from, to, interval, xAxisFormat);
                if (view != null) {
                    view.cancelLoading();
                    view.showChart(data, position);
                }
            }
        });
    }

    private List<String> makeLog(List<State> list, String format) {
        List<String> ret = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        int tempCount = 0;
        long tempTime = -1;
        int tempHour = -1;
        int tempMinute = -1;
        for (int i = 0; i < list.size(); i++) {
            State state = list.get(i);
            int count = state.movementsCount;
            long time = state.time;
            calendar.setTimeInMillis(time);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            if (tempTime == -1) {
                tempTime = time;
                tempHour = hour;
                tempMinute = minute;
            }
            if (state.type == StateType.RESET) {
                if (tempCount != 0) {
                    String log = TimeUtil.getTime(tempTime, format + "  HH:mm") + "  -->  " + tempCount + "  movements";
                    ret.add(log);
                }
                String log = TimeUtil.getTime(time, format + "  HH:mm:ss") + "  -->  reset";
                ret.add(log);
                tempCount = 0;
                tempTime = -1;
                tempHour = -1;
                tempMinute = -1;
            } else {
                if (tempHour != hour || tempMinute != minute) {
                    if (tempCount != 0) {
                        String log = TimeUtil.getTime(tempTime, format + "  HH:mm") + "  -->  " + tempCount + "  movements";
                        ret.add(log);
                    }
                    tempCount = 0;
                    tempHour = hour;
                    tempMinute = minute;
                }
                tempCount += count;
                tempTime = time;
            }
        }

        if (tempCount != 0) {
            String log = TimeUtil.getTime(tempTime, format + "  HH:mm") + "  -->  " + tempCount + "  movements";
            ret.add(log);
        }

        return ret;
    }

    private int makeChartCurrentPosition(boolean enableCurrentPosition, long from, long to, long interval) {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        if (enableCurrentPosition && from <= now && now < to) {
            return (int) ((now - from) / interval);
        }
        return -1;
    }

    private List<BarEntry> makeChart(List<State> list, long from, long to, long interval, String xAxisFormat) {
        List<BarEntry> ret = new ArrayList<>();
        for (int i = 0; from + interval * i <= to; i++) {
            int count = 0;
            for (int j = list.size() - 1; j >= 0; j--) {
                State state = list.get(j);
                long time = state.time;
                if (from + interval * i <= time && time < from + interval * (i + 1)) {
                    count += state.movementsCount;
                    list.remove(j);
                } else {
                    break;
                }
            }

            String xAxisValue = TimeUtil.getTime(from + interval * i, xAxisFormat);
            BarEntry barEntry = new BarEntry(i, count);
            barEntry.setData(xAxisValue);
            ret.add(barEntry);
        }
        return ret;
    }
}