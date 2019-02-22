package com.viseeointernational.stop.view.page.add;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;

import com.viseeointernational.stop.App;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.di.component.AddActivityComponent;
import com.viseeointernational.stop.view.page.BaseActivity;
import com.viseeointernational.stop.view.page.add.connect.ConnectFragment;
import com.viseeointernational.stop.view.page.add.search.SearchFragment;

import javax.inject.Inject;

import butterknife.ButterKnife;

public class AddActivity extends BaseActivity {

    private AddActivityComponent addActivityComponent;

    public AddActivityComponent getAddActivityComponent() {
        return addActivityComponent;
    }

    @Inject
    SearchFragment searchFragment;

    @Inject
    ConnectFragment connectFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        ButterKnife.bind(this);

        addActivityComponent = ((App) getApplication()).getAppComponent().addActivityComponent().build();
        addActivityComponent.inject(this);

        showSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void showSearch() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content, searchFragment).commit();
    }

    public void showConnect(String address) {
        Bundle bundle = new Bundle();
        bundle.putString(ConnectFragment.KEY_ADDRESS, address);
        connectFragment.setArguments(bundle);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content, connectFragment).commit();
    }

}
