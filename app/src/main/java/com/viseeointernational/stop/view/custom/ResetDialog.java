package com.viseeointernational.stop.view.custom;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialog;
import android.view.View;

import com.viseeointernational.stop.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ResetDialog extends AppCompatDialog {

    private Callback callback;

    public ResetDialog(@NonNull Context context, @NonNull Callback callback) {
        super(context, R.style.DialogBase);
        this.callback = callback;
    }

    private ResetDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    private ResetDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_reset);
        ButterKnife.bind(this);
        setCanceledOnTouchOutside(true);
    }

    @OnClick({R.id.enable_setting, R.id.force_reset, R.id.cancel})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.enable_setting:
                callback.onGoToLocationSetting();
                dismiss();
                break;
            case R.id.force_reset:
                callback.onForceReset();
                dismiss();
                break;
            case R.id.cancel:
                dismiss();
                break;
        }
    }

    public interface Callback {

        void onGoToLocationSetting();

        void onForceReset();
    }
}
