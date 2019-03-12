package com.viseeointernational.stop.di.module;

import android.app.Activity;
import android.content.Context;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.adapter.LogAdapter;
import com.viseeointernational.stop.view.page.detail.DetailActivity;
import com.viseeointernational.stop.view.page.detail.DetailActivityContract;
import com.viseeointernational.stop.view.page.detail.DetailActivityPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class DetailActivityModule {

    @ActivityScoped
    @Provides
    DetailActivityContract.Presenter presenter(DetailActivityPresenter presenter) {
        return presenter;
    }

    @ActivityScoped
    @Provides
    LogAdapter adapter(Context context) {
        return new LogAdapter(context);
    }

    @ActivityScoped
    @Provides
    String address(Activity activity) {
        return activity.getIntent().getStringExtra(DetailActivity.KEY_ADDRESS);
    }
}
