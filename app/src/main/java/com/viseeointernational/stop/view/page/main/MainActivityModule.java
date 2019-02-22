package com.viseeointernational.stop.view.page.main;

import com.viseeointernational.stop.di.ActivityScoped;

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
