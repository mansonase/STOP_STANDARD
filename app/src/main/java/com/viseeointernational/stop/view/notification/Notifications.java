package com.viseeointernational.stop.view.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import com.viseeointernational.stop.BuildConfig;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.constant.AlertTuneType;
import com.viseeointernational.stop.util.TimeUtil;
import com.viseeointernational.stop.view.page.main.MainActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class Notifications {

    private static final String TAG = Notifications.class.getSimpleName();

    public static final int NOTIFICATION_ID_FOREGROUND = 1;
    private static final int NOTIFICATION_ID_MSG_START = 10;
    private static final int NOTIFICATION_ID_MOVEMENTS_START = 20;

    public static final String CHANNEL_ID_FOREGROUND = "1";
    private static final String CHANNEL_ID_MSG = "2";
    private static final String CHANNEL_ID_MOVEMENTS_PREFIX = "3.";

    private Context context;
    private NotificationManager notificationManager;

    private Map<String, Integer> msgNotificationIds = new HashMap<>();
    private int lastMsgNotificationId = NOTIFICATION_ID_MSG_START;

    private Map<String, Integer> movementsNotificationIds = new HashMap<>();
    private int lastMovementsNotificationId = NOTIFICATION_ID_MOVEMENTS_START;

    private NotificationData notificationData = new NotificationData();

    @Inject
    public Notifications(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        startAutoSend();
    }

    private Disposable disposable;

    private void startAutoSend() {
        stopAutoSend();
        disposable = Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (notificationData.size() > 0) {
                            NotificationData.Content content = notificationData.get(0);
                            notificationData.remove(0);
                            notificationManager.notify(content.notificationId, content.notification);
                        }
                    }
                });
    }

    public void stopAutoSend() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
    }

    private void sendMsgNotification(String deviceId, CharSequence text) {
        Notification.Builder builder = new Notification.Builder(context);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID_MSG);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID_MSG, context.getText(R.string.channel_msg), NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
            builder.setChannelId(CHANNEL_ID_MSG);
        }
        builder.setStyle(new Notification.BigTextStyle());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(context.getText(R.string.app_name));
        builder.setContentText(text);
        builder.setAutoCancel(true);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        int notificationId;
        if (msgNotificationIds.containsKey(deviceId)) {
            notificationId = msgNotificationIds.get(deviceId);
        } else {
            notificationId = lastMsgNotificationId;
            msgNotificationIds.put(deviceId, lastMsgNotificationId);
            lastMsgNotificationId++;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    public void sendChangeNewBatteryNotification(String deviceId, String name) {
        sendMsgNotification(deviceId, name + " " + context.getText(R.string.notification_reconnect_low_power));
    }

    public void sendNoDeviceFoundNotification() {
        sendMsgNotification("10", context.getText(R.string.notification_no_device));
    }

    public void sendLostConnectionNotification(String deviceId, String name, long time, String format) {
        sendMsgNotification(deviceId, name + " " + context.getText(R.string.notification_lost_connection) + " " + TimeUtil.getTime(time, format));
    }

    public void sendConnectedNotification(String deviceId, String name) {
        sendMsgNotification(deviceId, name + " " + context.getText(R.string.notification_connected));
    }

    public void sendLowPowerNotification(String deviceId, String name) {
        sendMsgNotification(deviceId, name + " " + context.getText(R.string.notification_low_power));
    }

    /**
     * 发送触发通知
     *
     * @param deviceId
     * @param name
     * @param time
     * @param format
     * @param alertTuneType
     */
    public void sendMovementNotification(String deviceId, String name, long time, String format, int alertTuneType) {
        Notification.Builder builder = new Notification.Builder(context);
        if (Build.VERSION.SDK_INT >= 26) {
            String channelId = CHANNEL_ID_MOVEMENTS_PREFIX + alertTuneType;
            checkMovementsChannel(channelId, alertTuneType);
            builder.setChannelId(channelId);
        }
        builder.setStyle(new Notification.BigTextStyle());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(context.getText(R.string.app_name));
        builder.setLights(Color.GREEN, 1000, 1000);
        builder.setVibrate(BuildConfig.VIBRATE_PATTERN);
        builder.setSound(getMovementsSoundUri(alertTuneType));
        builder.setContentText(name + " " + context.getText(R.string.notification_movements) + " " + TimeUtil.getTime(time, format));
        builder.setAutoCancel(true);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        int notificationId;
        if (movementsNotificationIds.containsKey(deviceId)) {
            notificationId = movementsNotificationIds.get(deviceId);
        } else {
            notificationId = lastMovementsNotificationId;
            movementsNotificationIds.put(deviceId, lastMovementsNotificationId);
            lastMovementsNotificationId++;
        }
        NotificationData.Content content = new NotificationData.Content();
        content.notificationId = notificationId;
        content.notification = builder.build();
        notificationData.add(content);
    }

    @SuppressLint("NewApi")
    private void checkMovementsChannel(String channelId, int alertTuneType) {
        NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
        if (channel == null) {
            channel = new NotificationChannel(channelId, context.getText(R.string.channel_movements), NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(BuildConfig.VIBRATE_PATTERN);
            Uri soundUri = getMovementsSoundUri(alertTuneType);
            channel.setSound(soundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Uri getMovementsSoundUri(int alertTuneType) {
        switch (alertTuneType) {
            default:
                return null;

            case AlertTuneType.BELL:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.bell);
            case AlertTuneType.BUZZER:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.buzzer);
            case AlertTuneType.DING_DONG:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ding_dong);
            case AlertTuneType.DRUM:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.drum);

            case AlertTuneType.FUTURE_SIREN:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.future_siren);
            case AlertTuneType.HORN:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.horn);
            case AlertTuneType.LASER_GUN:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.laser_gun);
            case AlertTuneType.SCIFI_BEEP:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.scifi_beep);

            case AlertTuneType.SIREN_1:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.siren_1);
            case AlertTuneType.SIREN_2:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.siren_2);
            case AlertTuneType.VIOLIN:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.violin);
            case AlertTuneType.WARNING:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.warning);
        }
    }
}
