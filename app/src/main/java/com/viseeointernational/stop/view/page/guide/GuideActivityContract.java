package com.viseeointernational.stop.view.page.guide;

import com.github.mikephil.charting.data.BarEntry;
import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

import java.util.List;

public interface GuideActivityContract {

    interface View extends BaseView {

    }

    interface Presenter extends BasePresenter<View> {

        void setIsFirstStart(boolean isFirstStart);
    }
}
