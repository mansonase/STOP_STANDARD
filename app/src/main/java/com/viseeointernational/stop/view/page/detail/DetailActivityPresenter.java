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

//    private List<BarEntry> tempChartData = new ArrayList<>();
//    private List<String> temp

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
            getLogData(year, month, day);
            getChartData(year, month, day, hour, type);
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
                getLogData(year, month, day);
                getChartData(year, month, day, hour, type);
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
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void checkDay() {
        type = ChartType.DAY;
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void checkMonth() {
        type = ChartType.MONTH;
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void checkYear() {
        type = ChartType.YEAR;
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void changeDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        showDate(year, month, day);
        getLogData(year, month, day);
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void changeHour(int hour) {
        this.hour = hour;
        showHour(hour);
        getChartData(year, month, day, hour, type);
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
        getLogData(year, month, day);
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void nextDay() {
        day++;
        showDate(year, month, day);
        getLogData(year, month, day);
        getChartData(year, month, day, hour, type);
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
            getLogData(year, month, day);
        }
        showHour(hour);
        getChartData(year, month, day, hour, type);
    }

    @Override
    public void nextHour() {
        if (hour < 23) {
            hour++;
        } else {
            day++;
            hour = 0;
            showDate(year, month, day);
            getLogData(year, month, day);
        }
        showHour(hour);
        getChartData(year, month, day, hour, type);
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

    private void getLogData(final int year, final int month, final int day) {
        Observable.just(1)
                .map(new Function<Integer, List<State>>() {
                    @Override
                    public List<State> apply(Integer integer) throws Exception {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        long from = calendar.getTimeInMillis();
                        calendar.set(Calendar.DAY_OF_MONTH, day + 1);
                        long to = calendar.getTimeInMillis();
                        return stateDao.getStateDesc(address, from, to);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<State>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (view != null) {
                            view.showLoading();
                        }
                    }

                    @Override
                    public void onNext(List<State> states) {
                        tempLogData = states;
                        makeLog(states);
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

    private void makeLog(final List<State> list) {
        Observable.just(1)
                .map(new Function<Integer, List<String>>() {
                    @Override
                    public List<String> apply(Integer integer) throws Exception {
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
                                    String log = TimeUtil.getTime(tempTime, timeFormat + "  HH:mm") + "  -->  " + tempCount + "  movements";
                                    ret.add(log);
                                }
                                String log = TimeUtil.getTime(time, timeFormat + "  HH:mm:ss") + "  -->  reset";
                                ret.add(log);
                                tempCount = 0;
                                tempTime = -1;
                                tempHour = -1;
                                tempMinute = -1;
                            } else {
                                if (tempHour != hour || tempMinute != minute) {
                                    if (tempCount != 0) {
                                        String log = TimeUtil.getTime(tempTime, timeFormat + "  HH:mm") + "  -->  " + tempCount + "  movements";
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
                            String log = TimeUtil.getTime(tempTime, timeFormat + "  HH:mm") + "  -->  " + tempCount + "  movements";
                            ret.add(log);
                        }

                        return ret;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<String>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<String> list) {
                        if (view != null) {
                            view.cancelLoading();
                            view.showLog(list);
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

    private DeviceSource.MovementListener listener = new DeviceSource.MovementListener() {
        @Override
        public void onMovementReceived(@NonNull State state, @NonNull String timeFormat) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(state.time);
            int y = calendar.get(Calendar.YEAR);
            int m = calendar.get(Calendar.MONTH);
            int d = calendar.get(Calendar.DAY_OF_MONTH);
            int h = calendar.get(Calendar.HOUR_OF_DAY);
            if (y == year && m == month && d == day) {
                tempLogData.add(0, state);
                makeLog(tempLogData);
            }

            switch (type) {
                case ChartType.HOUR:
                    if (y == year && m == month && d == day && h == hour) {
                        tempChartData.add(state);
                        makeChart(tempChartData, false);
                    }
                    break;
                case ChartType.DAY:
                    if (y == year && m == month && d == day) {
                        tempChartData.add(state);
                        makeChart(tempChartData, false);
                    }
                    break;
                case ChartType.MONTH:
                    if (y == year && m == month) {
                        tempChartData.add(state);
                        makeChart(tempChartData, false);
                    }
                    break;
                case ChartType.YEAR:
                    if (y == year) {
                        tempChartData.add(state);
                        makeChart(tempChartData, false);
                    }
                    break;
            }
        }
    };

    private void getChartData(final int year, final int month, final int day, final int hour, final int type) {
        Observable.just(1)
                .map(new Function<Integer, List<State>>() {
                    @Override
                    public List<State> apply(Integer integer) throws Exception {
                        switch (type) {
                            case ChartType.HOUR:
                            default:
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, day);
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                long from = calendar.getTimeInMillis();
                                calendar.set(Calendar.HOUR_OF_DAY, hour + 1);
                                long to = calendar.getTimeInMillis();
                                return stateDao.getStateAsc(address, from, to);

                            case ChartType.DAY:
                                calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, day);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                from = calendar.getTimeInMillis();
                                calendar.set(Calendar.DAY_OF_MONTH, day + 1);
                                to = calendar.getTimeInMillis();
                                return stateDao.getStateAsc(address, from, to);

                            case ChartType.MONTH:
                                calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, 1);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                from = calendar.getTimeInMillis();
                                calendar.set(Calendar.MONTH, month + 1);
                                to = calendar.getTimeInMillis();
                                return stateDao.getStateAsc(address, from, to);

                            case ChartType.YEAR:
                                calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, 0);
                                calendar.set(Calendar.DAY_OF_MONTH, 1);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                from = calendar.getTimeInMillis();
                                calendar.set(Calendar.YEAR, year + 1);
                                to = calendar.getTimeInMillis();
                                return stateDao.getStateAsc(address, from, to);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<State>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (view != null) {
                            view.showLoading();
                        }
                    }

                    @Override
                    public void onNext(List<State> states) {
                        tempChartData = states;
                        makeChart(states, true);
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

    private void makeChart(final List<State> list, boolean enablePosition) {
        final int position;
        if (enablePosition) {
            switch (type) {
                default:
                    position = -1;
                    break;
                case ChartType.HOUR:
                    Calendar calendar = Calendar.getInstance();
                    int y = calendar.get(Calendar.YEAR);
                    int m = calendar.get(Calendar.MONTH);
                    int d = calendar.get(Calendar.DAY_OF_MONTH);
                    int h = calendar.get(Calendar.HOUR_OF_DAY);
                    int mm = calendar.get(Calendar.MINUTE);
                    if (y == year && m == month && d == day && h == hour) {
                        position = mm - 1;
                    } else {
                        position = -1;
                    }
                    break;
                case ChartType.DAY:
                    calendar = Calendar.getInstance();
                    y = calendar.get(Calendar.YEAR);
                    m = calendar.get(Calendar.MONTH);
                    d = calendar.get(Calendar.DAY_OF_MONTH);
                    h = calendar.get(Calendar.HOUR_OF_DAY);
                    if (y == year && m == month && d == day) {
                        position = h - 1;
                    } else {
                        position = -1;
                    }
                    break;
                case ChartType.MONTH:
                    calendar = Calendar.getInstance();
                    y = calendar.get(Calendar.YEAR);
                    m = calendar.get(Calendar.MONTH);
                    d = calendar.get(Calendar.DAY_OF_MONTH);
                    if (y == year && m == month) {
                        position = d - 1;
                    } else {
                        position = -1;
                    }
                    break;
                case ChartType.YEAR:
                    calendar = Calendar.getInstance();
                    y = calendar.get(Calendar.YEAR);
                    m = calendar.get(Calendar.MONTH);
                    if (y == year) {
                        position = m;
                    } else {
                        position = -1;
                    }
                    break;
            }
        } else {
            position = -1;
        }

        Observable.just(1)
                .map(new Function<Integer, List<BarEntry>>() {
                    @Override
                    public List<BarEntry> apply(Integer integer) throws Exception {

                        List<ChartData> temp = new ArrayList<>();
                        switch (type) {
                            case ChartType.HOUR:
                            default:
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, day);
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                long startTime = calendar.getTimeInMillis();
                                for (int i = 0; i < 60; i++) {
                                    ChartData chartData = new ChartData(startTime + i * 60000, startTime + (i + 1) * 60000, "HH:mm");
                                    temp.add(chartData);
                                }
                                break;
                            case ChartType.DAY:
                                calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, day);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                startTime = calendar.getTimeInMillis();
                                for (int i = 0; i < 24; i++) {
                                    ChartData chartData = new ChartData(startTime + i * 60000 * 60, startTime + (i + 1) * 60000 * 60, "HH:00");
                                    temp.add(chartData);
                                }
                                break;
                            case ChartType.MONTH:
                                calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, 1);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                startTime = calendar.getTimeInMillis();
                                calendar.set(Calendar.MONTH, month + 1);
                                calendar.set(Calendar.DAY_OF_MONTH, 0);
                                int dayCount = calendar.get(Calendar.DAY_OF_MONTH);
                                for (int i = 0; i < dayCount; i++) {
                                    ChartData chartData = new ChartData(startTime + i * 60000L * 60 * 24, startTime + (i + 1) * 60000L * 60 * 24, monthXAxisFormat);
                                    temp.add(chartData);
                                }
                                break;
                            case ChartType.YEAR:
                                calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.DAY_OF_MONTH, 1);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                for (int i = 0; i < 12; i++) {
                                    calendar.set(Calendar.MONTH, i);
                                    long from = calendar.getTimeInMillis();
                                    calendar.set(Calendar.MONTH, i + 1);
                                    long to = calendar.getTimeInMillis();
                                    ChartData chartData = new ChartData(from, to, "MM");
                                    temp.add(chartData);
                                }
                                break;
                        }

                        for (int i = 0; i < list.size(); i++) {
                            State state = list.get(i);
                            for (int j = 0; j < temp.size(); j++) {
                                ChartData chartData = temp.get(j);
                                if (chartData.isMatch(state.time)) {
                                    chartData.addCount(state.movementsCount);
                                    break;
                                }
                            }
                        }

                        List<BarEntry> ret = new ArrayList<>();
                        for (int i = 0; i < temp.size(); i++) {
                            ChartData chartData = temp.get(i);
                            BarEntry barEntry = new BarEntry(i, chartData.getCount(), TimeUtil.getTime(chartData.getStartTime(), chartData.getTimeFormat()));
                            ret.add(barEntry);
                        }
                        return ret;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<BarEntry>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<BarEntry> barEntries) {
                        if (view != null) {
                            view.cancelLoading();
                            switch (type) {
                                case ChartType.HOUR:
                                    view.showHourChart(barEntries, position);
                                default:
                                    break;
                                case ChartType.DAY:
                                    view.showDayChart(barEntries, position);
                                    break;
                                case ChartType.MONTH:
                                    view.showMonthChart(barEntries, position);
                                    break;
                                case ChartType.YEAR:
                                    view.showYearChart(barEntries);
                                    break;
                            }
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

    private static class ChartData {

        private long startTime;
        private long endTime;
        private int count;
        private String timeFormat;

        public ChartData(long startTime, long endTime, String timeFormat) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.timeFormat = timeFormat;
        }

        public boolean isMatch(long time) {
            return startTime <= time && time < endTime;
        }

        public void addCount(int c) {
            count += c;
        }

        public int getCount() {
            return count;
        }

        public long getStartTime() {
            return startTime;
        }

        public String getTimeFormat() {
            return timeFormat;
        }
    }
}