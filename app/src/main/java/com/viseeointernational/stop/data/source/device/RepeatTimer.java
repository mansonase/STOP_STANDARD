package com.viseeointernational.stop.data.source.device;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class RepeatTimer {

    private static final String TAG = RepeatTimer.class.getSimpleName();

    private Disposable disposable;
    private Callback callback;

    public RepeatTimer(@NonNull Callback callback) {
        this.callback = callback;
    }

    public void startCount() {
        stopCount();
        disposable = Observable.interval(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Log.d(TAG, "超时");
                        callback.onTimeOut();
                    }
                });
    }

    public void stopCount() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
    }

    public interface Callback {

        void onTimeOut();
    }
}
