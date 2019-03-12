package com.viseeointernational.stop.di.component;

import android.support.v4.app.Fragment;

import com.viseeointernational.stop.di.FragmentScoped;
import com.viseeointernational.stop.view.page.add.connect.ConnectFragment;
import com.viseeointernational.stop.di.module.ConnectFragmentModule;

import dagger.BindsInstance;
import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {ConnectFragmentModule.class})
public interface ConnectFragmentComponent {

    void inject(ConnectFragment fragment);

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        Builder fragment(Fragment fragment);

        ConnectFragmentComponent build();
    }
}
