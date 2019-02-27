package com.viseeointernational.stop.view.page.guide;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.viseeointernational.stop.App;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.view.page.BaseActivity;
import com.viseeointernational.stop.view.page.add.AddActivity;
import com.viseeointernational.stop.view.page.help.HelpActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class GuideActivity extends BaseActivity implements GuideActivityContract.View {

    private static final String URL_BUY_DEVICE = "http://www.viseeo.com";

    public static final int RESULT_BACK = 1;

    @Inject
    GuideActivityContract.Presenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        ButterKnife.bind(this);

        ((App) getApplication()).getAppComponent().guideActivityComponent().build().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.takeView(this);
    }

    @Override
    protected void onPause() {
        presenter.dropView();
        super.onPause();
    }

    @OnClick({R.id.add, R.id.help, R.id.buy})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.add:
                presenter.setIsFirstStart(false);
                startActivity(new Intent(this, AddActivity.class));
                finish();
                break;
            case R.id.help:
                startActivity(new Intent(this, HelpActivity.class));
                break;
            case R.id.buy:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(URL_BUY_DEVICE));
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_BACK);
        super.onBackPressed();
    }
}
