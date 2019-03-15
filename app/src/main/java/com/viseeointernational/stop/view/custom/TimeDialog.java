package com.viseeointernational.stop.view.custom;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatDialog;
import android.widget.TimePicker;

import com.viseeointernational.stop.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimeDialog extends AppCompatDialog {

    @BindView(R.id.time_picker)
    TimePicker timePicker;

    private Callback callback;

    public TimeDialog(@NonNull Context context, Callback callback) {
        super(context, R.style.DialogBase);
        this.callback = callback;
    }

    private TimeDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    private TimeDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_time);
        ButterKnife.bind(this);

        timePicker.setIs24HourView(true);
    }

    public void show(int hour) {
        super.show();
        if (Build.VERSION.SDK_INT >= 23) {
            timePicker.setHour(hour);
            timePicker.setMinute(0);
        }
        timePicker.setOnTimeChangedListener(onTimeChangedListener);
    }

    private TimePicker.OnTimeChangedListener onTimeChangedListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            dismiss();
            callback.onSelect(TimeDialog.this, hourOfDay);
        }
    };

    public interface Callback {

        void onSelect(TimeDialog dialog, int hour);
    }
}
