package com.viseeointernational.stop.view.page;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.viseeointernational.stop.R;

public abstract class BaseActivity extends AppCompatActivity implements BaseView {

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void showMessage(int id) {
        showMessage(getText(id));
    }

    @Override
    public void showMessage(CharSequence text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoading() {
        progressDialog.setMessage(getText(R.string.waiting));
        progressDialog.show();
    }

    @Override
    public void showLoading(CharSequence content) {
        progressDialog.setMessage(content);
        progressDialog.show();
    }

    @Override
    public void cancelLoading() {
        progressDialog.dismiss();
    }

}
