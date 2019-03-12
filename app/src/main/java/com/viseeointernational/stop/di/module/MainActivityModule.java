package com.viseeointernational.stop.di.module;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.page.main.MainActivityContract;
import com.viseeointernational.stop.view.page.main.MainActivityPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class MainActivityModule {

    @ActivityScoped
    @Provides
    MainActivityContract.Presenter presenter(MainActivityPresenter presenter) {
        return presenter;
    }

}
