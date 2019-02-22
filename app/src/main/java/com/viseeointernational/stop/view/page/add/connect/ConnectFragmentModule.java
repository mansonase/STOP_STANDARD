package com.viseeointernational.stop.view.page.add.connect;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.viseeointernational.stop.di.FragmentScoped;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectFragmentModule {

    @FragmentScoped
    @Provides
    ConnectFragmentContract.Presenter presenter(ConnectFragmentPresenter presenter) {
        return presenter;
    }

    @FragmentScoped
    @Nullable
    @Provides
    String address(Fragment fragment) {
        if (fragment.getArguments() != null) {
            return fragment.getArguments().getString(ConnectFragment.KEY_ADDRESS, null);
        }
        return null;
    }
}
