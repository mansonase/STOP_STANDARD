package com.viseeointernational.stop.util;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.view.notification.Notifications;

/**
 * 为了去掉前台服务通知
 */
public class AssistService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(Notifications.NOTIFICATION_ID_FOREGROUND, new Notification.Builder(this)
                .setContentText(getText(R.string.running))
                .setAutoCancel(true)
                .build());
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
}
