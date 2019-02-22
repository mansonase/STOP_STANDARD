package com.viseeointernational.stop.view.page.setting;

import android.app.Activity;
import android.content.Context;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.adapter.GpsAdapter;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public class SettingActivityModule {

    @ActivityScoped
    @Provides
    SettingActivityContract.Presenter presenter(SettingActivityPresenter presenter) {
        return presenter;
    }

    @ActivityScoped
    @Provides
    static GpsAdapter adapter(Context context) {
        return new GpsAdapter(context);
    }

    @ActivityScoped
    @Provides
    String address(Activity activity) {
        return activity.getIntent().getStringExtra(SettingActivity.KEY_ADDRESS);
    }
}
