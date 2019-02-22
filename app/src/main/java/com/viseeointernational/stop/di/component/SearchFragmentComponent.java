package com.viseeointernational.stop.di.component;

import com.viseeointernational.stop.di.FragmentScoped;
import com.viseeointernational.stop.view.page.add.search.SearchFragment;
import com.viseeointernational.stop.view.page.add.search.SearchFragmentModule;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {SearchFragmentModule.class})
public interface SearchFragmentComponent {

    void inject(SearchFragment fragment);

    @Subcomponent.Builder
    interface Builder {

        SearchFragmentComponent build();
    }
}
