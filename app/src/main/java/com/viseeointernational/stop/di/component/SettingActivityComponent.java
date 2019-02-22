package com.viseeointernational.stop.di.component;

import android.app.Activity;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.page.setting.SettingActivity;
import com.viseeointernational.stop.view.page.setting.SettingActivityModule;

import dagger.BindsInstance;
import dagger.Subcomponent;

@ActivityScoped
@Subcomponent(modules = {SettingActivityModule.class})
public interface SettingActivityComponent {

    void inject(SettingActivity activity);

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        Builder activity(Activity activity);

        SettingActivityComponent build();
    }
}
