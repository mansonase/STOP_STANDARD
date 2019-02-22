package com.viseeointernational.stop.view.custom;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.widget.TextView;

import com.viseeointernational.stop.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IfDialog extends AppCompatDialog {

    @BindView(R.id.title)
    TextView title;

    private Callback callback;

    public IfDialog(@NonNull Context context, @NonNull Callback callback) {
        super(context, R.style.DialogBase);
        this.callback = callback;
    }

    private IfDialog(Context context, int theme) {
        super(context, theme);
    }

    private IfDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_if);
        ButterKnife.bind(this);
        setCanceledOnTouchOutside(false);
    }

    public void show(@NonNull CharSequence title) {
        super.show();
        this.title.setText(title);
    }

    @OnClick({R.id.yes, R.id.no})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.yes:
                callback.onYes(this);
                dismiss();
                break;
            case R.id.no:
                callback.onNo(this);
                dismiss();
                break;
        }
    }

    public interface Callback {

        void onYes(IfDialog dialog);

        void onNo(IfDialog dialog);
    }
}
