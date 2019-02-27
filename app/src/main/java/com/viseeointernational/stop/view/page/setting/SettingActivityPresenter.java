package com.viseeointernational.stop.view.page.setting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.constant.AlertTuneType;
import com.viseeointernational.stop.data.constant.StateType;
import com.viseeointernational.stop.data.constant.TimeFormatType;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.data.source.device.DeviceSource;
import com.viseeointernational.stop.util.TimeUtil;
import com.zhihu.matisse.Matisse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class SettingActivityPresenter implements SettingActivityContract.Presenter {

    private static final String TAG = SettingActivityPresenter.class.getSimpleName();

    private SettingActivityContract.View view;

    private Context context;
    private DeviceSource deviceSource;

    private List<String> timeFormats = new ArrayList<>();
    private List<String> alertTunes = new ArrayList<>();
    private List<String> notifications = new ArrayList<>();

    @Inject
    String address;

    @Inject
    public SettingActivityPresenter(Context context, DeviceSource deviceSource) {
        this.context = context;
        this.deviceSource = deviceSource;

        timeFormats.clear();
        timeFormats.add(TimeFormatType.DATE_1_1);
        timeFormats.add(TimeFormatType.DATE_1_2);
        timeFormats.add(TimeFormatType.DATE_1_3);
        timeFormats.add(TimeFormatType.DATE_2_1);
        timeFormats.add(TimeFormatType.DATE_2_2);
        timeFormats.add(TimeFormatType.DATE_2_3);
        timeFormats.add(TimeFormatType.DATE_3_1);
        timeFormats.add(TimeFormatType.DATE_3_2);
        timeFormats.add(TimeFormatType.DATE_3_3);

        alertTunes.clear();
        alertTunes.add("Vibration");

        alertTunes.add("Bell");
        alertTunes.add("Buzzer");
        alertTunes.add("DingDong");
        alertTunes.add("Drum");

        alertTunes.add("FutureSiren");
        alertTunes.add("Horn");
        alertTunes.add("LaserGun");
        alertTunes.add("SciFiBeep");

        alertTunes.add("Siren1");
        alertTunes.add("Siren2");
        alertTunes.add("Violin");
        alertTunes.add("Warning");

        notifications.clear();
        notifications.add("Off");
        notifications.add("Always");
    }

    @Override
    public void takeView(final SettingActivityContract.View view) {
        this.view = view;
        init();
    }

    private void init() {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.getDevice(address, new DeviceSource.GetDeviceCallback() {
            @Override
            public void onDeviceLoaded(Device device) {
                if (view != null) {
                    view.showName(device.name);
                    view.showHeader(device.imagePath);
                    view.showGEnable(device.enableG);
                    view.showXYZEnable(device.enableXYZ);
                    view.showGValue(device.gValue & 0xff);
                    view.showXYZValue(device.xyzValue & 0xff);
                    view.showAlertEnable(device.enableAlert);
                    view.showTimeFormat(device.timeFormat);
                    if (!device.enableMonitoring) {
                        view.showAlertTune(alertTunes.get(AlertTuneType.VIBRATION));
                    } else {
                        view.showAlertTune(alertTunes.get(device.alertTune));
                    }
                    view.showNotification(notifications.get(device.notificationType));
                }
                if (startTime == 0 && endTime == 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    endTime = calendar.getTimeInMillis();

                    view.showStartTime(TimeUtil.getTime(startTime, device.timeFormat));
                    view.showEndTime(TimeUtil.getTime(endTime, device.timeFormat));
                    getResetData(address, startTime, endTime, device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                }
            }
        });
    }

    @Override
    public void dropView() {
        view = null;
    }

    private File getHeaderDir() {
        File dir = new File("/mnt/sdcard/ViseeO/STOP AT-1/Header");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getExcelDir() {
        File dir = new File("/mnt/sdcard/ViseeO/STOP AT-1/Excels");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    private Uri headerTempUri;

    @Override
    public void result(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SettingActivity.REQUEST_IMG:
                if (resultCode == Activity.RESULT_OK && view != null) {
                    List<Uri> ret = Matisse.obtainResult(data);
                    if (ret.size() > 0) {
                        headerTempUri = Uri.parse("file://" + "/" + getHeaderDir().getPath() + "/" + "temp.jpg");
                        view.showCropHeader(ret.get(0), headerTempUri);
                    }
                }
                break;
            case SettingActivity.REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(headerTempUri));
                        deviceSource.setHeader(address, bitmap);
                        if (view != null) {
                            view.showHeader(bitmap);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void showMap(State state) {
        if (view != null) {
            view.showMap(state.longitude, state.latitude);
        }
    }

    private long startTime;

    @Override
    public void setStartTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        startTime = calendar.getTimeInMillis();
        deviceSource.getDevice(address, new DeviceSource.GetDeviceCallback() {
            @Override
            public void onDeviceLoaded(Device device) {
                if (view != null) {
                    view.showStartTime(TimeUtil.getTime(startTime, device.timeFormat));
                }
                getResetData(address, startTime, endTime, device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
            }

            @Override
            public void onDeviceNotAvailable() {
            }
        });
    }

    @Override
    public void showStartCalendar() {
        if (view != null) {
            view.showStartCalendar(Calendar.getInstance().getTimeInMillis(), startTime);
        }
    }

    private long endTime;

    @Override
    public void setEndTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        endTime = calendar.getTimeInMillis();
        deviceSource.getDevice(address, new DeviceSource.GetDeviceCallback() {
            @Override
            public void onDeviceLoaded(Device device) {
                if (view != null) {
                    view.showEndTime(TimeUtil.getTime(endTime, device.timeFormat));
                }
                getResetData(address, startTime, endTime, device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
            }

            @Override
            public void onDeviceNotAvailable() {
            }
        });
    }

    @Override
    public void showEndCalendar() {
        if (view != null) {
            view.showEndCalendar(Calendar.getInstance().getTimeInMillis(), endTime);
        }
    }

    @Override
    public void saveName(String s) {
        if (!TextUtils.isEmpty(s)) {
            if (s.length() > 30) {
                if (view != null) {
                    view.showMessage(R.string.msg_name_too_long);
                }
                return;
            }
            deviceSource.setName(address, s);
            if (view != null) {
                view.showName(s);
                view.closeRenameDialog();
            }
        } else {
            if (view != null) {
                view.showMessage(R.string.msg_rename);
            }
        }
    }

    @Override
    public void showTimeFormatList() {
        if (view != null) {
            view.openTimeFormatSelector(timeFormats);
        }
    }

    @Override
    public void showAlertTuneList() {
        if (view != null) {
            view.openAlertTuneSelector(alertTunes);
        }
    }

    @Override
    public void showNotificationTypeList() {
        if (view != null) {
            view.openNotificationSelector(notifications);
        }
    }

    @Override
    public void saveTimeFormat(String s) {
        deviceSource.setTimeFormat(address, s);
        if (view != null) {
            view.showTimeFormat(s);
            view.showStartTime(TimeUtil.getTime(startTime, s));
            view.showEndTime(TimeUtil.getTime(endTime, s));
        }
    }

    @Override
    public void saveAlertTune(int i) {
        deviceSource.setAlertTune(address, i);
        if (view != null) {
            view.showAlertTune(alertTunes.get(i));
            switch (i) {
                case AlertTuneType.VIBRATION:
                    view.vibrate();
                    break;

                case AlertTuneType.BELL:
                    view.playSound(getSoundUri(R.raw.bell));
                    break;
                case AlertTuneType.BUZZER:
                    view.playSound(getSoundUri(R.raw.buzzer));
                    break;
                case AlertTuneType.DING_DONG:
                    view.playSound(getSoundUri(R.raw.ding_dong));
                    break;
                case AlertTuneType.DRUM:
                    view.playSound(getSoundUri(R.raw.drum));
                    break;

                case AlertTuneType.FUTURE_SIREN:
                    view.playSound(getSoundUri(R.raw.future_siren));
                    break;
                case AlertTuneType.HORN:
                    view.playSound(getSoundUri(R.raw.horn));
                    break;
                case AlertTuneType.LASER_GUN:
                    view.playSound(getSoundUri(R.raw.laser_gun));
                    break;
                case AlertTuneType.SCIFI_BEEP:
                    view.playSound(getSoundUri(R.raw.scifi_beep));
                    break;

                case AlertTuneType.SIREN_1:
                    view.playSound(getSoundUri(R.raw.siren_1));
                    break;
                case AlertTuneType.SIREN_2:
                    view.playSound(getSoundUri(R.raw.siren_2));
                    break;
                case AlertTuneType.VIOLIN:
                    view.playSound(getSoundUri(R.raw.violin));
                    break;
                case AlertTuneType.WARNING:
                    view.playSound(getSoundUri(R.raw.warning));
                    break;
            }
        }
    }

    private Uri getSoundUri(int id) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + id);
    }

    @Override
    public void saveNotification(int i) {
        deviceSource.setNotification(address, i);
        if (view != null) {
            view.showNotification(notifications.get(i));
        }
    }

    private boolean tempAlert;

    @Override
    public void enableAlert(boolean enable) {
        if (view != null) {
            view.showLoading();
        }
        tempAlert = !enable;
        deviceSource.enableAlert(address, enable, new DeviceSource.SettingCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                }
            }

            @Override
            public void onTimeOut() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_time_out);
                    view.showAlertEnable(tempAlert);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_ble_not_enable);
                    view.showAlertEnable(tempAlert);
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_offline);
                    view.showAlertEnable(tempAlert);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_not_available);
                    view.showAlertEnable(tempAlert);
                }
            }
        });
    }

    @Override
    public void find() {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.find(address, new DeviceSource.SettingCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                }
            }

            @Override
            public void onTimeOut() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_time_out);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_ble_not_enable);
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_offline);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_not_available);
                }
            }
        });
    }

    @Override
    public void unpair(boolean force) {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.unpair(address, force, new DeviceSource.SettingCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                    view.close();
                }
            }

            @Override
            public void onTimeOut() {
                view.cancelLoading();
                // todo 硬件不回复
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.alertIfForceUnpair();
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.cancelLoading();
                    view.alertIfForceUnpair();
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.alertIfForceUnpair();
                }
            }
        });
    }

    @Override
    public void download() {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.getStatesContainTimeFormat(address, startTime, endTime, new DeviceSource.GetStatesContainTimeFormatCallback() {
            @Override
            public void onStatesLoaded(List<State> states, String timeFormat) {
                if (states.size() == 0) {
                    if (view != null) {
                        view.cancelLoading();
                        view.showMessage(R.string.msg_no_data);
                    }
                } else {
                    String name = getExcelName(startTime, endTime);
                    createExcel(states, timeFormat, name);
                }
            }
        });
    }

    private String getExcelName(long from, long to) {
        String name = "data";
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(from);
        String year = calendar.get(Calendar.YEAR) + "";
        year = year.substring(2, 4);
        name += year;
        String month = (calendar.get(Calendar.MONTH) + 1) + "";
        if (month.length() == 1) {
            month = "0" + month;
        }
        name += month;
        String day = calendar.get(Calendar.DAY_OF_MONTH) + "";
        if (day.length() == 1) {
            day = "0" + day;
        }
        name += day + "_";
        calendar.setTimeInMillis(to);
        year = calendar.get(Calendar.YEAR) + "";
        year = year.substring(2, 4);
        name += year;
        month = (calendar.get(Calendar.MONTH) + 1) + "";
        if (month.length() == 1) {
            month = "0" + month;
        }
        name += month;
        day = calendar.get(Calendar.DAY_OF_MONTH) + "";
        if (day.length() == 1) {
            day = "0" + day;
        }
        name += day + ".xls";
        return name;
    }

    private void createExcel(final List<State> states, final String timeFormat, final String name) {
        Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> emitter) throws Exception {
                File file = new File(getExcelDir(), name);
                if (file.isFile() && file.exists()) {
                    file.delete();
                    file.createNewFile();
                }

                OutputStream outputStream = new FileOutputStream(file);
                WritableWorkbook writableWorkbook = Workbook.createWorkbook(outputStream);
                WritableSheet writableSheet = writableWorkbook.createSheet("sheet1", 0);
                Label label = new Label(0, 0, "Id");
                writableSheet.addCell(label);
                label = new Label(1, 0, "Action");
                writableSheet.addCell(label);
                label = new Label(2, 0, "Movements");
                writableSheet.addCell(label);
                label = new Label(3, 0, "Longitude");
                writableSheet.addCell(label);
                label = new Label(4, 0, "Latitude");
                writableSheet.addCell(label);
                label = new Label(5, 0, "Time");
                writableSheet.addCell(label);

                for (int i = 0; i < states.size(); i++) {
                    State state = states.get(i);
                    label = new Label(0, i + 1, i + "");
                    writableSheet.addCell(label);
                    String action;
                    switch (state.type) {
                        case StateType.FLIP:
                        default:
                            action = "Flip";
                            break;
                        case StateType.SHAKE:
                            action = "Shake";
                            break;
                        case StateType.FLIP_AND_SHAKE:
                            action = "Flip and shake";
                            break;
                        case StateType.RESET:
                            action = "Reset";
                            break;
                        case StateType.HISTORY:
                            action = "History";
                            break;
                    }
                    label = new Label(1, i + 1, action);
                    writableSheet.addCell(label);
                    label = new Label(2, i + 1, state.movementsCount + "");
                    writableSheet.addCell(label);
                    label = new Label(3, i + 1, state.longitude + "");
                    writableSheet.addCell(label);
                    label = new Label(4, i + 1, state.latitude + "");
                    writableSheet.addCell(label);
                    label = new Label(5, i + 1, TimeUtil.getTime(state.time, timeFormat + "  " + TimeFormatType.TIME_DEFAULT));
                    writableSheet.addCell(label);
                }
                writableWorkbook.write();
                writableWorkbook.close();
                emitter.onNext(file);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<File>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(File file) {
                        if (view != null) {
                            view.cancelLoading();
                            view.showMessage(R.string.msg_successful);
                            view.sendFile(file);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (view != null) {
                            view.cancelLoading();
                            view.showMessage(R.string.msg_failed);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private boolean tempEnableG;
    private boolean tempEnableXYZ;

    @Override
    public void enableGAndXYZ(boolean enableG, boolean enableXYZ) {
        if (view != null) {
            view.showLoading();
        }
        tempEnableG = !enableG;
        tempEnableXYZ = !enableXYZ;
        deviceSource.enableGAndXYZ(address, enableG, enableXYZ, new DeviceSource.SettingCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                }
            }

            @Override
            public void onTimeOut() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_time_out);
                    view.showGEnable(tempEnableG);
                    view.showXYZEnable(tempEnableXYZ);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_ble_not_enable);
                    view.showGEnable(tempEnableG);
                    view.showXYZEnable(tempEnableXYZ);
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_offline);
                    view.showGEnable(tempEnableG);
                    view.showXYZEnable(tempEnableXYZ);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_not_available);
                    view.showGEnable(tempEnableG);
                    view.showXYZEnable(tempEnableXYZ);
                }
            }
        });
    }

    @Override
    public void saveG(int g) {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.setG(address, (byte) g, new DeviceSource.SettingCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                }
            }

            @Override
            public void onTimeOut() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_time_out);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_ble_not_enable);
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_offline);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_not_available);
                }
            }
        });
    }

    @Override
    public void saveXYZ(int xyz) {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.setXYZ(address, (byte) xyz, new DeviceSource.SettingCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                }
            }

            @Override
            public void onTimeOut() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_time_out);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_ble_not_enable);
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_offline);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_not_available);
                }
            }
        });
    }

    private void getResetData(String address, long from, long to, final String format) {
        deviceSource.getResetStatesDesc(address, from, to, new DeviceSource.GetResetStatesDescCallback() {
            @Override
            public void onStatesLoaded(List<State> states) {
                if (view != null) {
                    view.cancelLoading();
                    view.showLocation(states, format);
                }
            }
        });
    }
}
