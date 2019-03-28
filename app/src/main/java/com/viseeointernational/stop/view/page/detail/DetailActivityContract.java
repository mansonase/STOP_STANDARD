package com.viseeointernational.stop.view.page.detail;

import com.github.mikephil.charting.data.BarEntry;
import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

import java.util.List;

public interface DetailActivityContract {

    interface View extends BaseView {

        void showDate(String s);

        void showHour(String s);

        void showHourChecked();

        void showDayChecked();

        void showMonthChecked();

        void showYearChecked();

        void showCalendar(long baseTime, long currentTime);

        void showTimePicker(int hour);

        void showHourChart(List<BarEntry> list, int position);

        void showDayChart(List<BarEntry> list, int position);

        void showMonthChart(List<BarEntry> list, int position);

        void showYearChart(List<BarEntry> list);

        void showLog(List<String> list);
    }

    interface Presenter extends BasePresenter<View> {

        void setType(int type);

        int getType();

        void checkHour();

        void checkDay();

        void checkMonth();

        void checkYear();

        void changeDate(int year, int month, int day);

        void changeHour(int hour);

        void showCalendar();

        void previousDay();

        void nextDay();

        void showTimePicker();

        void previousHour();

        void nextHour();
    }
}
