package com.viseeointernational.stop.di.component;

import android.app.Activity;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.page.add.AddActivity;
import com.viseeointernational.stop.view.page.add.AddActivityModule;

import dagger.BindsInstance;
import dagger.Subcomponent;

@ActivityScoped
@Subcomponent(modules = {AddActivityModule.class})
public interface AddActivityComponent {

    void inject(AddActivity activity);

    @Subcomponent.Builder
    interface Builder {

        AddActivityComponent build();
    }

    SearchFragmentComponent.Builder searchFragmentComponent();

    ConnectFragmentComponent.Builder connectFragmentComponent();
}
