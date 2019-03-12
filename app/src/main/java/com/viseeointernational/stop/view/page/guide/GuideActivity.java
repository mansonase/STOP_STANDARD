package com.viseeointernational.stop.view.page.guide;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.view.page.BaseActivity;
import com.viseeointernational.stop.view.page.add.AddActivity;
import com.viseeointernational.stop.view.page.help.HelpActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class GuideActivity extends BaseActivity {

    private static final String URL_BUY_DEVICE = "http://www.viseeo.com";

    public static final int RESULT_BACK = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @OnClick({R.id.add, R.id.help, R.id.buy})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.add:
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
