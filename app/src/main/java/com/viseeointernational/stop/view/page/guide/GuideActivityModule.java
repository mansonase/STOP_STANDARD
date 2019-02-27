package com.viseeointernational.stop.view.page.guide;

import com.viseeointernational.stop.di.ActivityScoped;

import dagger.Module;
import dagger.Provides;

@Module
public class GuideActivityModule {

    @ActivityScoped
    @Provides
    GuideActivityContract.Presenter presenter(GuideActivityPresenter presenter) {
        return presenter;
    }
}
