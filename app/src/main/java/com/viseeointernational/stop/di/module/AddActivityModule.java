package com.viseeointernational.stop.di.module;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.di.component.ConnectFragmentComponent;
import com.viseeointernational.stop.di.component.SearchFragmentComponent;
import com.viseeointernational.stop.view.page.add.connect.ConnectFragment;
import com.viseeointernational.stop.view.page.add.search.SearchFragment;

import dagger.Module;
import dagger.Provides;

@Module(subcomponents = {ConnectFragmentComponent.class, SearchFragmentComponent.class})
public class AddActivityModule {

    @ActivityScoped
    @Provides
    SearchFragment searchFragment() {
        return new SearchFragment();
    }

    @ActivityScoped
    @Provides
    ConnectFragment connectFragment() {
        return new ConnectFragment();
    }
}
