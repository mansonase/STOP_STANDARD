package com.viseeointernational.stop.view.page.detail;

import com.github.mikephil.charting.data.BarEntry;
import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

import java.util.List;

public interface DetailActivityContract {

    interface View extends BaseView {

        void showTime(String s);

        void showCalendar(long baseTime, long currentTime);

        void showChart(List<BarEntry> list, int position);

        void showLog(List<String> list);
    }

    interface Presenter extends BasePresenter<View> {

        void setType(int type);

        void setTime(long time);

        void setTypeAndTime(int type, long time);

        int getType();

        long getTime();

        void previousDay();

        void nextDay();

        void loadCalendar();
    }
}
