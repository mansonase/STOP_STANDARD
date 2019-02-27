package com.viseeointernational.stop.view.page.guide;

import com.viseeointernational.stop.data.source.base.sharedpreferences.SharedPreferencesHelper;

import javax.inject.Inject;

public class GuideActivityPresenter implements GuideActivityContract.Presenter {

    private static final String TAG = GuideActivityPresenter.class.getSimpleName();

    private GuideActivityContract.View view;

    private SharedPreferencesHelper sharedPreferencesHelper;

    @Inject
    public GuideActivityPresenter(SharedPreferencesHelper sharedPreferencesHelper) {
        this.sharedPreferencesHelper = sharedPreferencesHelper;
    }

    @Override
    public void takeView(final GuideActivityContract.View view) {
        this.view = view;
    }

    @Override
    public void dropView() {
        view = null;
    }

    @Override
    public void setIsFirstStart(boolean isFirstStart) {
        sharedPreferencesHelper.setIsFirstStart(isFirstStart);
    }
}