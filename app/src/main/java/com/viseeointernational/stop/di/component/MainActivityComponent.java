package com.viseeointernational.stop.di.component;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.page.main.MainActivity;
import com.viseeointernational.stop.di.module.MainActivityModule;

import dagger.Subcomponent;

@ActivityScoped
@Subcomponent(modules = {MainActivityModule.class})
public interface MainActivityComponent {

    void inject(MainActivity activity);

    @Subcomponent.Builder
    interface Builder {
        MainActivityComponent build();
    }
}
