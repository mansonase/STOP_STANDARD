package com.viseeointernational.stop.di.component;

import com.viseeointernational.stop.di.ActivityScoped;
import com.viseeointernational.stop.view.page.guide.GuideActivity;
import com.viseeointernational.stop.view.page.guide.GuideActivityModule;

import dagger.Subcomponent;

@ActivityScoped
@Subcomponent(modules = {GuideActivityModule.class})
public interface GuideActivityComponent {

    void inject(GuideActivity activity);

    @Subcomponent.Builder
    interface Builder {

        GuideActivityComponent build();
    }
}
