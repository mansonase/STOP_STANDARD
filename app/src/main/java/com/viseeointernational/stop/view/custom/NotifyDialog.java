package com.viseeointernational.stop.view.custom;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.viseeointernational.stop.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotifyDialog extends Dialog {

    @BindView(R.id.text)
    TextView text;

    public NotifyDialog(@NonNull Context context) {
        super(context, R.style.DialogBase);
    }

    private NotifyDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    private NotifyDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notify);
        ButterKnife.bind(this);

        setCanceledOnTouchOutside(false);
    }

    public void show(@NonNull CharSequence msg) {
        super.show();
        text.setText(msg);
    }

    @OnClick(R.id.ok)
    public void onViewClicked() {
        dismiss();
    }
}
