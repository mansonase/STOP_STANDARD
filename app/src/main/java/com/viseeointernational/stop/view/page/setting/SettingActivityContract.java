package com.viseeointernational.stop.view.page.setting;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

import java.io.File;
import java.util.List;

public interface SettingActivityContract {

    interface View extends BaseView {

        void showHeader(String imagePath);

        void showHeader(Bitmap bitmap);

        void showName(String s);

        void openTimeFormatSelector(List<String> list);

        void openAlertTuneSelector(List<String> list);

        void playSound(Uri uri);

        void vibrate();

        void openNotificationSelector(List<String> list);

        void showTimeFormat(String s);

        void showAlertTune(String s);

        void showNotification(String s);

        void showAlertEnable(boolean enable);

        void showLocation(List<State> list, String format);

        void close();

        void alertIfForceUnpair();

        void sendFile(File file);

        void closeRenameDialog();

        void showStartCalendar(long baseTime, long currentTime);

        void showEndCalendar(long baseTime, long currentTime);

        void showStartTime(String s);

        void showEndTime(String s);

        void showMap(double longitude, double latitude);

        void showCropHeader(Uri originHeader, Uri tempImage);

        void showGEnable(boolean enable);

        void showGValue(int value);

        void showXYZEnable(boolean enable);

        void showXYZValue(int value);
    }

    interface Presenter extends BasePresenter<View> {

        void result(int requestCode, int resultCode, Intent data);

        void showMap(State state);

        void setStartTime(long time);

        void showStartCalendar();

        void setEndTime(long time);

        void showEndCalendar();

        void saveName(String s);

        void showTimeFormatList();

        void showAlertTuneList();

        void showNotificationTypeList();

        void saveTimeFormat(String s);

        void saveAlertTune(int i);

        void saveNotification(int i);

        void enableAlert(boolean enable);

        void find();

        void unpair(boolean force);

        void download();

        void saveTempEnableGAndEYZ(boolean enableG, boolean enableXYZ);

        void enableGAndXYZ(boolean enableG, boolean enableXYZ);

        void saveG(int g);

        void saveXYZ(int xyz);
    }
}
