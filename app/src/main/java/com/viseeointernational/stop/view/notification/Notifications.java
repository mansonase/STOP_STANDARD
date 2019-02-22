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

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.constant.AlertTuneType;
import com.viseeointernational.stop.util.TimeUtil;
import com.viseeointernational.stop.view.page.main.MainActivity;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Notifications {

    private static final String TAG = Notifications.class.getSimpleName();

    public static final int NOTIFICATION_ID_FOREGROUND = 1;
    private static final int NOTIFICATION_ID_MSG = 2;
    private static final int NOTIFICATION_ID_MOVEMENTS_START = 3;

    public static final String CHANNEL_ID_FOREGROUND = "1";
    private static final String CHANNEL_ID_MSG = "2";
    private static final String CHANNEL_ID_MOVEMENTS_PREFIX = "3.";

    private Context context;
    private NotificationManager notificationManager;

    private Map<String, Integer> MovementsNotificationIds = new HashMap<>();
    private int lastMovementsNotificationId = NOTIFICATION_ID_MOVEMENTS_START;

    @Inject
    public Notifications(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public Notification getForegroundNotification() {
        Notification.Builder builder = new Notification.Builder(context);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID_FOREGROUND);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID_FOREGROUND, context.getText(R.string.channel_ble_service), NotificationManager.IMPORTANCE_NONE);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            builder.setChannelId(CHANNEL_ID_FOREGROUND);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(context.getText(R.string.app_name));
        builder.setContentText(context.getText(R.string.running));
        builder.setLights(Color.GREEN, 0, 0);
        builder.setVibrate(null);
        builder.setSound(null);
        builder.setAutoCancel(true);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }

    private void sendMsgNotification(CharSequence text) {
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
//        builder.setAutoCancel(true);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        notificationManager.notify(NOTIFICATION_ID_MSG, builder.build());
    }

    public void sendNoDeviceFoundNotification() {
        sendMsgNotification(context.getText(R.string.notification_no_device));
    }

    public void sendLostConnectionNotification(String name, long time, String format) {
        sendMsgNotification(name + " " + context.getText(R.string.notification_lost_connection) + " " + TimeUtil.getTime(time, format));
    }

    public void sendConnectedNotification(String name) {
        sendMsgNotification(name + " " + context.getText(R.string.notification_connected));
    }

    public void sendLowPowerNotification() {
        sendMsgNotification(context.getText(R.string.notification_low_power));
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
        if (alertTuneType == AlertTuneType.VIBRATION) {
            builder.setVibrate(new long[]{0, 200, 100, 200});
        } else {
            builder.setVibrate(null);
        }
        builder.setSound(getMovementsSoundUri(alertTuneType));
        builder.setContentText(name + " " + context.getText(R.string.notification_movements) + " " + TimeUtil.getTime(time, format));
//        builder.setAutoCancel(true);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        int notificationId;
        if (MovementsNotificationIds.containsKey(deviceId)) {
            notificationId = MovementsNotificationIds.get(deviceId);
        } else {
            notificationId = lastMovementsNotificationId;
            MovementsNotificationIds.put(deviceId, lastMovementsNotificationId);
            lastMovementsNotificationId++;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    @SuppressLint("NewApi")
    private void checkMovementsChannel(String channelId, int alertTuneType) {
        NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
        if (channel == null) {
            channel = new NotificationChannel(channelId, context.getText(R.string.channel_movements), NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            Uri soundUri = getMovementsSoundUri(alertTuneType);
            channel.enableVibration(soundUri == null);
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
            case AlertTuneType.DING_DONG:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ding_dong);
            case AlertTuneType.DRUM:
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.drum);
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
