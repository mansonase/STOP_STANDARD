package com.viseeointernational.stop.di.module;

import android.content.Context;

import com.viseeointernational.stop.di.FragmentScoped;
import com.viseeointernational.stop.view.adapter.DeviceAdapter;
import com.viseeointernational.stop.view.page.add.search.SearchFragmentContract;
import com.viseeointernational.stop.view.page.add.search.SearchFragmentPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class SearchFragmentModule {

    @FragmentScoped
    @Provides
    SearchFragmentContract.Presenter presenter(SearchFragmentPresenter presenter) {
        return presenter;
    }

    @FragmentScoped
    @Provides
    DeviceAdapter provideAdapter(Context context) {
        return new DeviceAdapter(context);
    }
}
