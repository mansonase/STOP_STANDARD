package com.viseeointernational.stop.view.custom;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.viseeointernational.stop.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RenameDialog extends Dialog {

    @BindView(R.id.name)
    EditText name;
    private Callback callback;

    public RenameDialog(@NonNull Context context, @NonNull Callback callback) {
        super(context, R.style.DialogBase);
        this.callback = callback;
    }

    private RenameDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    private RenameDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_rename);
        ButterKnife.bind(this);

        setCanceledOnTouchOutside(true);
    }

    @Override
    public void show() {
        super.show();
        name.setText("");
    }

    @OnClick({R.id.ok, R.id.dismiss})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.ok:
                callback.onOk(name.getText().toString());
                dismiss();
                break;
            case R.id.dismiss:
                dismiss();
                break;
        }
    }

    public interface Callback {

        void onOk(String name);
    }
}
