package com.viseeointernational.stop.di.component;

import android.app.Activity;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.page.detail.DetailActivity;
import com.viseeointernational.stop.di.module.DetailActivityModule;

import dagger.BindsInstance;
import dagger.Subcomponent;

@ActivityScoped
@Subcomponent(modules = {DetailActivityModule.class})
public interface DetailActivityComponent {

    void inject(DetailActivity activity);

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        Builder activity(Activity activity);

        DetailActivityComponent build();
    }
}
