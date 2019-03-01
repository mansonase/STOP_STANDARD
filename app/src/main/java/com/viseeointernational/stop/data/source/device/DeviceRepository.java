package com.viseeointernational.stop.data.source.device;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.viseeointernational.stop.data.constant.AlertTuneType;
import com.viseeointernational.stop.data.constant.ConnectionType;
import com.viseeointernational.stop.data.constant.NotificationType;
import com.viseeointernational.stop.data.constant.StateType;
import com.viseeointernational.stop.data.constant.TimeFormatType;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.HistoryData;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.data.source.android.ble.BleEvent;
import com.viseeointernational.stop.data.source.android.ble.BleService;
import com.viseeointernational.stop.data.source.base.database.DeviceDao;
import com.viseeointernational.stop.data.source.base.database.StateDao;
import com.viseeointernational.stop.util.BitmapUtil;
import com.viseeointernational.stop.util.StringUtil;
import com.viseeointernational.stop.view.notification.Notifications;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DeviceRepository implements DeviceSource {

    private static final String TAG = DeviceRepository.class.getSimpleName();

    private DeviceDao deviceDao;
    private StateDao stateDao;
    private BleService bleService;
    private Context context;
    private Notifications notifications;

    private Map<String, Device> pairedDevices = new LinkedHashMap<>();

    @Inject
    public DeviceRepository(DeviceDao deviceDao, StateDao stateDao, BleService bleService, Context context, Notifications notifications) {
        this.deviceDao = deviceDao;
        this.stateDao = stateDao;
        this.bleService = bleService;
        this.context = context;
        this.notifications = notifications;
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean isBleEnable() {
        return bleService.isBleAvailable();
    }

    @Override
    public void onAppExit() {
        if (pairedDevices.size() == 0 || !bleService.isBleAvailable()) {
            EventBus.getDefault().unregister(this);
            stopAutoConnect();
            bleService.stopSelf();
        }
    }

    @Override
    public void getDevice(@NonNull final String address, @NonNull final GetDeviceCallback callback) {
        if (pairedDevices.containsKey(address)) {
            callback.onDeviceLoaded(pairedDevices.get(address));
        } else {
            Observable.create(new ObservableOnSubscribe<Device>() {
                @Override
                public void subscribe(ObservableEmitter<Device> emitter) throws Exception {
                    Device device = deviceDao.getDeviceByAddress(address);
                    if (device != null) {
                        emitter.onNext(device);
                        emitter.onComplete();
                    } else {
                        emitter.onError(new Throwable());
                    }
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Device>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(Device device) {
                            callback.onDeviceLoaded(device);
                        }

                        @Override
                        public void onError(Throwable e) {
                            callback.onDeviceNotAvailable();
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }

    @Override
    public void getPairedDeviceCount(@NonNull GetPairedDeviceCountCallback callback) {
        callback.onCountLoaded(pairedDevices.size());
    }

    @Override
    public void getStatesContainTimeFormat(@NonNull final String address, final long from, final long to, @NonNull final GetStatesContainTimeFormatCallback callback) {
        Observable.create(new ObservableOnSubscribe<List<State>>() {
            @Override
            public void subscribe(ObservableEmitter<List<State>> emitter) throws Exception {
                List<State> list = stateDao.getStateAsc(address, from, to);
                emitter.onNext(list);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<State>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<State> states) {
                        String format = TimeFormatType.DATE_1_1;
                        if (pairedDevices.containsKey(address)) {
                            format = pairedDevices.get(address).timeFormat;
                        }
                        callback.onStatesLoaded(states, format);
                    }

                    @Override
                    public void onError(Throwable e) {
                        String format = TimeFormatType.DATE_1_1;
                        if (pairedDevices.containsKey(address)) {
                            format = pairedDevices.get(address).timeFormat;
                        }
                        callback.onStatesLoaded(new ArrayList<State>(), format);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void getStatesDesc(@NonNull final String address, final long from, final long to, @NonNull final GetStatesDescCallback callback) {
        Observable.create(new ObservableOnSubscribe<List<State>>() {
            @Override
            public void subscribe(ObservableEmitter<List<State>> emitter) throws Exception {
                List<State> list = stateDao.getStateDesc(address, from, to);
                emitter.onNext(list);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<State>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<State> states) {
                        callback.onStatesLoaded(states);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onStatesLoaded(new ArrayList<State>());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void getResetStatesDesc(@NonNull final String address, final long from, final long to, @NonNull final GetResetStatesDescCallback callback) {
        Observable.create(new ObservableOnSubscribe<List<State>>() {
            @Override
            public void subscribe(ObservableEmitter<List<State>> emitter) throws Exception {
                List<State> list = stateDao.getStateWithTypeDesc(address, from, to, StateType.RESET);
                emitter.onNext(list);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<State>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<State> states) {
                        callback.onStatesLoaded(states);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onStatesLoaded(new ArrayList<State>());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    // 获取电量
    private String batteryListenerAddress;
    private BatteryListener batteryListener;
    private Disposable batteryDisposable;

    @Override
    public void setBatteryListener(@NonNull String address, @Nullable BatteryListener listener) {
        batteryListenerAddress = address;
        batteryListener = listener;
        if (batteryListener != null) {
            startGetBattery();
        } else {
            stopGetBattery();
        }
    }

    private void stopGetBattery() {
        if (batteryDisposable != null && !batteryDisposable.isDisposed()) {
            batteryDisposable.dispose();
            batteryDisposable = null;
        }
    }

    private void startGetBattery() {
        stopGetBattery();
        batteryDisposable = Observable.interval(0, 10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (!TextUtils.isEmpty(batteryListenerAddress)) {
                            bleService.write(batteryListenerAddress, new byte[]{(byte) 0xa3});// 获取电量
                        }
                    }
                });
    }

    // 监听触发
    private String movementListenerAddress;
    private MovementListener movementListener;

    @Override
    public void setMovementListener(@NonNull String address, @Nullable MovementListener listener) {
        movementListenerAddress = address;
        movementListener = listener;
    }

    // 监听触发次数
    private MovementCountChangeListener movementCountChangeListener;

    @Override
    public void setMovementCountChangeListener(@Nullable MovementCountChangeListener listener) {
        movementCountChangeListener = listener;
    }

    // 监听连接状态
    private DeviceConnectionChangeListener deviceConnectionChangeListener;

    @Override
    public void setDeviceConnectionChangeListener(@Nullable DeviceConnectionChangeListener listener) {
        deviceConnectionChangeListener = listener;
    }

    // 监听添加移除设备
    private DeviceCountChangeListener deviceCountChangeListener;

    @Override
    public void setDeviceCountChangeListener(@Nullable DeviceCountChangeListener listener) {
        deviceCountChangeListener = listener;
    }

    @Override
    public void saveDevice(@NonNull Device device) {
        Observable.just(device)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {
                        deviceDao.insertDevice(device);
                    }
                });
    }

    // 自动重连
    private Disposable autoDisposable;

    private void startAutoConnect() {
        stopAutoConnect();
        Log.d(TAG, "开始自动重连");
        autoDisposable = Observable.interval(0, 20, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .map(new Function<Long, Boolean>() {
                    @Override
                    public Boolean apply(Long aLong) throws Exception {// 判断是否有未连接的设备
                        Log.d(TAG, "正在检测 是否重连");
                        for (Map.Entry<String, Device> entry : pairedDevices.entrySet()) {
                            if (entry.getValue().connectionState == ConnectionType.DISCONNECTED) {
                                return true;
                            }
                        }
                        return false;
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            bleService.search(15);// 搜索后再重连
                        }
                    }
                });
    }

    private void stopAutoConnect() {
        Log.d(TAG, "停止自动重连");
        if (autoDisposable != null && !autoDisposable.isDisposed()) {
            autoDisposable.dispose();
            autoDisposable = null;
        }
    }

    @Override
    public void getPairedDevices(@NonNull final GetPairedDevicesCallback callback) {
        if (pairedDevices.size() == 0) {
            Observable.create(new ObservableOnSubscribe<List<Device>>() {
                @Override
                public void subscribe(ObservableEmitter<List<Device>> emitter) throws Exception {
                    List<Device> list = deviceDao.getPairedDevice();
                    emitter.onNext(list);
                    emitter.onComplete();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .doOnNext(new Consumer<List<Device>>() {
                        @Override
                        public void accept(List<Device> devices) throws Exception {
                            for (int i = 0; i < devices.size(); i++) {
                                Device device = devices.get(i);
                                device.currentState = stateDao.getLastState(device.address);
                                State updateState = stateDao.getLastStateWithoutType(device.address, StateType.RESET);
                                if (updateState != null) {
                                    device.lastUpdateTime = updateState.time;
                                } else {
                                    device.lastUpdateTime = device.time;
                                }
                                pairedDevices.put(device.address, device);
                            }
                            startAutoConnect();
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<List<Device>>() {
                        @Override
                        public void accept(List<Device> devices) throws Exception {
                            callback.onDevicesLoaded(devices);
                        }
                    });
        } else {
            List<Device> list = new ArrayList<>(pairedDevices.values());
            callback.onDevicesLoaded(list);
        }
    }

    @Override
    public void enableMonitoring(@NonNull final String address, final boolean enable) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return pairedDevices.get(address);
                        }
                        return deviceDao.getDeviceByAddress(address);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {
                        device.enableMonitoring = enable;
                        deviceDao.insertDevice(device);
                    }
                });
    }

    @Override
    public void setName(@NonNull final String address, @NonNull final String name) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return pairedDevices.get(address);
                        }
                        return deviceDao.getDeviceByAddress(address);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {
                        device.name = name;
                        deviceDao.insertDevice(device);
                    }
                });
    }

    @Override
    public void setHeader(@NonNull final String address, @NonNull final Bitmap bitmap) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return pairedDevices.get(address);
                        }
                        Device device = deviceDao.getDeviceByAddress(address);
                        if (device == null) {
                            device = new Device();
                            device.address = address;
                            return device;
                        }
                        return device;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {// 删掉旧头像
                        String prefix = device.address.replaceAll(":", "-");
                        File[] files = getHeaderDir().listFiles();
                        if (files != null) {
                            for (int i = files.length - 1; i >= 0; i--) {
                                String name = files[i].getName();
                                if (name.startsWith(prefix) && files[i].exists() && files[i].isFile()) {
                                    files[i].delete();
                                }
                            }
                        }
                    }
                })
                .doOnNext(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {// 保存新头像
                        String prefix = device.address.replaceAll(":", "-");
                        File file = new File(getHeaderDir(), prefix + System.currentTimeMillis() + ".jpg");
                        String path = file.getAbsolutePath();
                        BitmapUtil.saveBitmap(path, bitmap);
                        device.imagePath = path;
                    }
                })
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {// 更新数据库和缓存
                        deviceDao.insertDevice(device);
                    }
                });
    }

    private File getHeaderDir() {
        File dir = new File("/mnt/sdcard/ViseeO/STOP AT-1/Header");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    public void setTimeFormat(@NonNull final String address, @NonNull final String format) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return pairedDevices.get(address);
                        }
                        return deviceDao.getDeviceByAddress(address);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {
                        device.timeFormat = format;
                        deviceDao.insertDevice(device);
                    }
                });
    }

    @Override
    public void setAlertTune(@NonNull final String address, final int alertTune) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return pairedDevices.get(address);
                        }
                        return deviceDao.getDeviceByAddress(address);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {
                        if (alertTune != AlertTuneType.VIBRATION) {
                            device.alertTune = alertTune;
                            device.enableMonitoring = true;
                        } else {
                            device.enableMonitoring = false;
                        }
                        deviceDao.insertDevice(device);
                    }
                });
    }

    @Override
    public void setNotification(@NonNull final String address, final int notificationType) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return pairedDevices.get(address);
                        }
                        return deviceDao.getDeviceByAddress(address);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {
                        device.notificationType = notificationType;
                        deviceDao.insertDevice(device);
                    }
                });
    }

    @Override
    public void reset(@NonNull final String address, final double latitude, final double longitude, @NonNull final ResetCallback callback) {
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        if (pairedDevices.containsKey(address)) {
            final Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                callback.onDeviceDisconnected();
                return;
            }
            if (device.currentState == null || device.currentState.type == StateType.RESET) {
                callback.onAlreadyReset();
                return;
            }
            State state = new State();
            state.latitude = latitude;
            state.longitude = longitude;
            state.type = StateType.RESET;
            state.address = address;
            state.time = Calendar.getInstance().getTimeInMillis();
            device.currentState = state;
            device.movementsCount = 0;
            Observable.just(state)
                    .subscribeOn(Schedulers.io())
                    .doOnNext(new Consumer<State>() {
                        @Override
                        public void accept(State state) throws Exception {
                            stateDao.insertState(state);
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<State>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(State state) {
                            callback.onSuccessful();
                            if (movementListener != null && address.equals(movementListenerAddress)) {
                                movementListener.onMovementReceived(state, device.timeFormat);
                            }
                            if (movementCountChangeListener != null) {
                                movementCountChangeListener.onMovementCountChange(device.address, device.movementsCount);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            callback.onError();
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
            return;
        }
        callback.onDeviceNotAvailable();
    }

    private void handleBluetoothDisable() {
        for (Map.Entry<String, Device> entry : pairedDevices.entrySet()) {
            entry.getValue().connectionState = ConnectionType.DISCONNECTED;
            if (deviceConnectionChangeListener != null) {
                deviceConnectionChangeListener.onConnectionChange(entry.getKey(), false);
            }
        }
    }

    private void handleBluetoothEnable() {

    }

    private void handleGattConnected(String address) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            byte[] data = new byte[5];
            data[0] = (byte) 0xb2;
            data[1] = StringUtil.hexString2byte(device.pairId.substring(0, 2));
            data[2] = StringUtil.hexString2byte(device.pairId.substring(2, 4));
            data[3] = StringUtil.hexString2byte(device.pairId.substring(4, 6));
            data[4] = StringUtil.hexString2byte(device.pairId.substring(6, 8));
            bleService.write(address, data);// 连线
        } else {
            bleService.write(address, new byte[]{(byte) 0xb0});// 配对
        }
    }

    private void handleGattDisconnected(String address) {
        if (connectionCallback != null && address.equals(connectCallbackAddress)) {
            connectionCallback.onDisconnected();
            connectionCallback = null;
            connectCallbackAddress = null;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            Log.d(TAG, "lost connection + " + address);
            if (device.connectionState == ConnectionType.CONNECTED) {
                notifications.sendLostConnectionNotification(device.name, Calendar.getInstance().getTimeInMillis(), device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
            }
            device.connectionState = ConnectionType.DISCONNECTED;
        }
        if (deviceConnectionChangeListener != null) {
            deviceConnectionChangeListener.onConnectionChange(address, false);
        }
    }

    private void handleSearchDeviceFound(String address, String name, int rssi) {
        if (searchCallback != null && !pairedDevices.containsKey(address) && name != null && name.startsWith("STOP")) {// 只返回STOP未配对的设备
            Device foundDevice = new Device();
            foundDevice.address = address;
            foundDevice.name = name;
            foundDevice.rssi = rssi;
            searchCallback.onDeviceFound(foundDevice);
        }
        if (pairedDevices.containsKey(address)) {// 自动重连
            Device device = pairedDevices.get(address);
            if (device.connectionState == ConnectionType.DISCONNECTED) {
                device.connectionState = ConnectionType.CONNECTING;
                bleService.connect(address);
                Log.d(TAG, "执行重连" + address);
            }
        }
    }

    private void handleSearchFinish() {
        if (searchCallback != null) {
            searchCallback.onFinish();
            searchCallback = null;
        }
    }

    private byte[] pairIds = new byte[4];// 缓存pair id

    private void handleB0(String address, byte[] data) {
        System.arraycopy(data, 4, pairIds, 0, pairIds.length);// 缓存id
        byte[] b1 = new byte[5];
        b1[0] = (byte) 0xb1;
        System.arraycopy(pairIds, 0, b1, 1, 4);
        bleService.write(address, b1);// 确认配对
    }

    private void handleB1(String address, byte[] data) {
        if (data[4] == (byte) 0xaa) {// 成功
            byte[] b2 = new byte[5];
            b2[0] = (byte) 0xb2;
            System.arraycopy(pairIds, 0, b2, 1, 4);
            bleService.write(address, b2);// 连线
        } else {// 失败
            // todo 硬件会自动断开
        }
    }

    private void handleB2(final String address, final byte[] data) {
        Log.d(TAG, "b2连线成功" + address);
        if (connectionCallback != null && address.equals(connectCallbackAddress)) {
            connectionCallback.onConnected();
            connectionCallback = null;
            connectCallbackAddress = null;
        }
        if (deviceConnectionChangeListener != null) {
            deviceConnectionChangeListener.onConnectionChange(address, true);
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.connectionState = ConnectionType.CONNECTED;
            Observable.just(device)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<Device>() {
                        @Override
                        public void accept(Device device) throws Exception {
                            State state = stateDao.getLastStateWithoutType(device.address, StateType.RESET);
                            device.lastHistoryState = state;
                        }
                    });
            notifications.sendConnectedNotification(device.name);
            byte[] a4 = new byte[7];
            a4[0] = (byte) 0xa4;// 设置mode
            a4[1] = (byte) 0x0a;// 自动模式多久回报一次

            // mode
            a4[2] = (byte) 0x02;// 启动自摸模式
            if (device.enableAlert) {
                a4[2] |= (byte) 0x01;// 启动alert
            }
            if (device.enableG) {
                a4[2] |= (byte) 0x04;// 启动g振动
            }
            if (device.enableXYZ) {
                a4[2] |= (byte) 0x08;// 启动g振动
            }

            a4[3] = (byte) 0x01;// 更换过电池为0
            a4[4] = device.gValue;// g灵敏度
            a4[5] = device.xyzValue;// xyz灵敏度
            a4[6] = (byte) 0x01;
            bleService.write(device.address, a4);

            bleService.write(address, new byte[]{(byte) 0xa0});// 发A0
        } else {
            Observable.just(pairIds)
                    .map(new Function<byte[], Device>() {
                        @Override
                        public Device apply(byte[] bytes) throws Exception {// 保存pairid 并初始化
                            Device device = deviceDao.getDeviceByAddress(address);
                            String pairId = StringUtil.bytes2HexStringEx(bytes);
                            device.pairId = pairId;
                            device.time = Calendar.getInstance().getTimeInMillis();
                            device.timeFormat = TimeFormatType.DATE_1_1;
                            device.alertTune = AlertTuneType.BELL;
                            device.notificationType = NotificationType.ALWAYS;
                            device.enableAlert = true;
                            device.enableMonitoring = true;
                            device.enableG = true;
                            device.enableXYZ = true;
                            device.gValue = (byte) 0x03;
                            device.xyzValue = (byte) 0x0a;
                            device.connectionState = ConnectionType.CONNECTED;
                            deviceDao.insertDevice(device);
                            return device;
                        }
                    })
                    .doOnNext(new Consumer<Device>() {
                        @Override
                        public void accept(Device device) throws Exception {// 发送设置mode
                            byte[] data = new byte[7];
                            data[0] = (byte) 0xa4;// 设置mode
                            data[1] = (byte) 0x0a;// 自动模式多久回报一次

                            // mode
                            data[2] = (byte) 0x02;// 启动自摸模式
                            if (device.enableAlert) {
                                data[2] |= (byte) 0x01;// 启动alert
                            }
                            if (device.enableG) {
                                data[2] |= (byte) 0x04;// 启动g振动
                            }
                            if (device.enableXYZ) {
                                data[2] |= (byte) 0x08;// 启动g振动
                            }

                            data[3] = (byte) 0x00;// 更换过电池为0
                            data[4] = device.gValue;// g灵敏度
                            data[5] = device.xyzValue;// xyz灵敏度
                            data[6] = (byte) 0x01;
                            bleService.write(device.address, data);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Device>() {
                        @Override
                        public void accept(Device device) throws Exception {// 新设备装进序列
                            pairedDevices.put(address, device);
                            if (deviceCountChangeListener != null) {
                                deviceCountChangeListener.onDeviceCountChange(new ArrayList<Device>(pairedDevices.values()));
                            }
                            notifications.sendConnectedNotification(device.name);
                            bleService.write(device.address, new byte[]{(byte) 0xa0});// 发A0
                        }
                    });
        }
    }

    private void handleEE(final String address, final byte[] data) {
        Log.d(TAG, "b2连接失败" + address);
        if (connectionCallback != null && address.equals(connectCallbackAddress)) {// todo 硬件会自动断开gatt
            connectionCallback.onDisconnected();
            connectionCallback = null;
            connectCallbackAddress = null;
        }
        if (pairedDevices.containsKey(address)) {
            if (data[4] == (byte) 0x01) {// pair id 错误
                final Device device = pairedDevices.get(address);
                device.pairId = "";
                Observable.just(1)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                deviceDao.insertDevice(device);
                            }
                        });
                pairedDevices.remove(address);
                if (deviceCountChangeListener != null) {
                    deviceCountChangeListener.onDeviceCountChange(new ArrayList<Device>(pairedDevices.values()));
                }
            }
        }
    }

    private void handleA0(final String address, final byte[] data) {
        if (data[5] == (byte) 0x00) {// 新电池不收历史
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (device.lastHistoryState == null) {
                return;
            }
            if ((device.lastHistoryState.indexL != (byte) 0x00 || device.lastHistoryState.indexH != (byte) 0x00) &&
                    (device.lastHistoryState.indexL != (byte) 0xff && device.lastHistoryState.indexH != (byte) 0xff)) {
                if (data[6] == (byte) 0x00 && data[7] == (byte) 0x00) {
                    notifications.sendChangeNewBatteryNotification(device.name);
                    return;
                }
            }
        }
        Observable.create(new ObservableOnSubscribe<State>() {
            @Override
            public void subscribe(ObservableEmitter<State> emitter) throws Exception {
                if (pairedDevices.containsKey(address)) {
                    State state = pairedDevices.get(address).lastHistoryState;
                    emitter.onNext(state);
                    emitter.onComplete();
                    return;
                }
                emitter.onError(new Throwable());
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<State>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(State state) {
                        long now = Calendar.getInstance().getTimeInMillis();
                        if (now - state.time < 20000) {// 距离上次接受数据少于20秒时不读取历史
                            Log.d(TAG, "距离上次小于20秒");
                            return;
                        }
                        HistoryData historyData = new HistoryData(data[6], data[7], state.time);
                        if (pairedDevices.containsKey(address)) {
                            pairedDevices.get(address).historyData = historyData;
                            Log.d(TAG, "history 次数" + historyData.readA1Count);

                            byte[] a1 = new byte[5];
                            a1[0] = (byte) 0xa1;
                            a1[1] = (byte) 0x00;// page index 当为0是 要判断index % 2 获取 239或者240个数据 page index 不为0时全是240个数据
                            a1[2] = data[6];// indexH
                            a1[3] = data[7];// indexL
                            a1[4] = (byte) 0x02;// pages = 1 每次请求240分钟的数据
                            bleService.write(address, a1);
                            Log.d(TAG, address + "发送A1数据 " + StringUtil.bytes2HexString(a1));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void handleA1(final String address, final byte[] data) {
        Log.d(TAG, "收到A1数据 " + address);
        if (pairedDevices.containsKey(address)) {
            final Device device = pairedDevices.get(address);
            final HistoryData historyData = device.historyData;
            if (historyData == null) {
                return;
            }
            final byte pageIndex = data[6];
            Observable.create(new ObservableOnSubscribe<List<State>>() {
                @Override
                public void subscribe(ObservableEmitter<List<State>> emitter) throws Exception {// 获取要存储的数据
                    Log.d(TAG, device.address + " received a1 page index = " + pageIndex);
                    List<State> statesToSave = new ArrayList<>();// 要保存的数据
                    for (int i = 7; i < 127; i++) {
                        if (pageIndex == (byte) 0x00 && historyData.indexL % 2 == 0 && i == 4) {
                            if (historyData.timeCursor > historyData.lastTime) {
                                int movementsCount = data[i] & 0x0f;
                                if (movementsCount > 0) {
                                    State state = new State();
                                    state.time = historyData.timeCursor;
                                    state.movementsCount = movementsCount;
                                    state.type = StateType.HISTORY;
                                    state.address = device.address;
                                    statesToSave.add(state);
                                    device.movementsCount += movementsCount;
                                }
                                Log.d(TAG, "读取 time = " + historyData.timeCursor + "  count = " + movementsCount);
                                historyData.timeCursor -= 60000;
                            }
                        } else {
                            if (historyData.timeCursor > historyData.lastTime) {
                                int movementsCount = data[i] & 0x0f;
                                if (movementsCount > 0) {
                                    State state = new State();
                                    state.time = historyData.timeCursor;
                                    state.movementsCount = movementsCount;
                                    state.type = StateType.HISTORY;
                                    state.address = device.address;
                                    statesToSave.add(state);
                                    device.movementsCount += movementsCount;
                                }
                                Log.d(TAG, "读取 time = " + historyData.timeCursor + "  count = " + movementsCount);
                                historyData.timeCursor -= 60000;
                            }
                            if (historyData.timeCursor > historyData.lastTime) {
                                int movementsCount = data[i] >> 4 & 0x0f;
                                if (movementsCount > 0) {
                                    State state = new State();
                                    state.time = historyData.timeCursor;
                                    state.movementsCount = movementsCount;
                                    state.type = StateType.HISTORY;
                                    state.address = device.address;
                                    statesToSave.add(state);
                                    device.movementsCount += movementsCount;
                                }
                                Log.d(TAG, "读取 time = " + historyData.timeCursor + "  count = " + movementsCount);
                                historyData.timeCursor -= 60000;
                            }
                        }
                    }
                    emitter.onNext(statesToSave);
                    emitter.onComplete();
                }
            })
                    .doOnNext(new Consumer<List<State>>() {
                        @Override
                        public void accept(List<State> states) throws Exception {// 保存数据
                            Log.d(TAG, "保存历史 size = " + states.size());
                            stateDao.insertState(states);
                            Log.d(TAG, "保存历史 size = " + states.size() + "成功 ");
                        }
                    })
                    .map(new Function<List<State>, Boolean>() {
                        @Override
                        public Boolean apply(List<State> states) throws Exception {// 是否要还读
                            historyData.readA1Count--;
                            return historyData.readA1Count > 0;
                        }
                    })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(Boolean aBoolean) {
                            if (aBoolean) {
                                if (pageIndex == (byte) 0xff) {
                                    device.historyData = null;
                                    if (movementCountChangeListener != null) {
                                        movementCountChangeListener.onMovementCountChange(device.address, device.movementsCount);
                                    }
                                } else {
                                    byte indexH = historyData.indexH;
                                    byte indexL = historyData.indexL;
                                    byte[] a1 = new byte[5];
                                    a1[0] = (byte) 0xa1;
                                    a1[1] = (byte) (pageIndex + (byte) 0x01);// page index 当为0是 要判断index % 2 获取 119或者120个数据 page index 不为0时全是120个数据
                                    a1[2] = indexH;// indexH
                                    a1[3] = indexL;// indexL
                                    a1[4] = (byte) 0x02;// pages = 1 每次请求120分钟的数据
                                    bleService.write(device.address, a1);
                                    Log.d(TAG, device.address + "发送A1数据 " + StringUtil.bytes2HexString(a1));
                                }
                            } else {
                                device.historyData = null;
                                if (movementCountChangeListener != null) {
                                    movementCountChangeListener.onMovementCountChange(device.address, device.movementsCount);
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG, "read history failed " + e.getMessage());
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }

    private void handleA3(final String address, final byte[] data) {
        if (batteryListener != null && address.equals(batteryListenerAddress)) {
            int power;
            if (data[4] > (byte) 0xdc) {
                power = 100;
            } else if (data[4] >= (byte) 0xc2) {
                int temp = data[4] - (byte) 0xc2;
                power = (int) (temp / 26f * 95) + 5;
            } else {
                power = 3;
            }
            batteryListener.onPowerReceived(power);
            if (pairedDevices.containsKey(address)) {
                Device device = pairedDevices.get(address);
                int lastPower = device.battery;
                if (power < 10 && power < lastPower) {
                    notifications.sendLowPowerNotification();
                }
                device.battery = power;
            }
        }
    }

    private void handleC0(final String address, final byte[] data) {
        bleService.write(address, new byte[]{(byte) 0xa2});// 立即清除此通知
        saveC0State(address, data, Calendar.getInstance().getTimeInMillis());
    }

    private void handleA4(final String address, final byte[] data) {
        if (alertCallback != null) {
            if (pairedDevices.containsKey(address)) {
                Device device = pairedDevices.get(address);
                device.enableAlert = tempEnableAlert;
                Observable.just(device)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Device>() {
                            @Override
                            public void accept(Device device) throws Exception {
                                deviceDao.insertDevice(device);
                            }
                        });
            }
            alertCallback.onSuccessful();
            alertCallback = null;
            stopAlertListener();
        }
        if (gxyzEnableCallback != null) {
            if (pairedDevices.containsKey(address)) {
                Device device = pairedDevices.get(address);
                device.enableG = tempEnableG;
                device.enableXYZ = tempEnableXYZ;
                Observable.just(device)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Device>() {
                            @Override
                            public void accept(Device device) throws Exception {
                                deviceDao.insertDevice(device);
                            }
                        });
            }
            gxyzEnableCallback.onSuccessful();
            gxyzEnableCallback = null;
            stopGXYZEnableListener();
        }
        if (gValueCallback != null) {
            if (pairedDevices.containsKey(address)) {
                Device device = pairedDevices.get(address);
                device.gValue = tempGValue;
                Observable.just(device)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Device>() {
                            @Override
                            public void accept(Device device) throws Exception {
                                deviceDao.insertDevice(device);
                            }
                        });
            }
            gValueCallback.onSuccessful();
            gValueCallback = null;
            stopGValueListener();
        }
        if (xyzValueCallback != null) {
            if (pairedDevices.containsKey(address)) {
                Device device = pairedDevices.get(address);
                device.xyzValue = tempXYZValue;
                Observable.just(device)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Device>() {
                            @Override
                            public void accept(Device device) throws Exception {
                                deviceDao.insertDevice(device);
                            }
                        });
            }
            xyzValueCallback.onSuccessful();
            xyzValueCallback = null;
            stopXYZValueListener();
        }
    }

    private void handleAA(final String address, final byte[] data) {
        if (findCallback != null) {
            findCallback.onSuccessful();
            findCallback = null;
            stopFindListener();
        }
    }

    // 以下需要service回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleEvent(final BleEvent event) {
        switch (event.type) {
            case BleEvent.BLUETOOTH_ADAPTER_DISABLE:// 蓝牙关闭
                handleBluetoothDisable();
                break;
            case BleEvent.BLUETOOTH_ADAPTER_ENABLE:// 蓝牙启用
                handleBluetoothEnable();
                break;
            case BleEvent.GATT_CONNECTED:// gatt已连接
                handleGattConnected(event.address);
                break;
            case BleEvent.GATT_DISCONNECTED:// gatt断开连接
                handleGattDisconnected(event.address);
                break;
            case BleEvent.SEARCH_DEVICE_FOUND:// 发现设备
                handleSearchDeviceFound(event.address, event.name, event.rssi);
                break;
            case BleEvent.SEARCH_FINISH:// 搜索结束
                handleSearchFinish();
                break;
            case BleEvent.READ_DATA:
                if (event.value.length == 9 && event.value[3] == (byte) 0xb0) {// 请求配对反馈
                    handleB0(event.address, event.value);
                    return;
                }
                if (event.value.length == 6 && event.value[3] == (byte) 0xb1) {// 确认配对反馈
                    handleB1(event.address, event.value);
                    return;
                }
                if (event.value.length == 16 && event.value[3] == (byte) 0xb2) {// 连线成功反馈
                    handleB2(event.address, event.value);
                    return;
                }
                if (event.value.length == 6 && event.value[3] == (byte) 0xee) {// 连线失败反馈
                    handleEE(event.address, event.value);
                    return;
                }
                if (event.value.length == 16 && event.value[3] == (byte) 0xa0) {// A0反馈
                    handleA0(event.address, event.value);
                    return;
                }
                if (event.value.length == 128 && event.value[3] == (byte) 0xa1) {// A1反馈
                    handleA1(event.address, event.value);
                    return;
                }
                if (event.value.length == 6 && event.value[3] == (byte) 0xa3) {// A3反馈
                    handleA3(event.address, event.value);
                    return;
                }
                if (event.value.length == 16 && event.value[3] == (byte) 0xc0) {// C0反馈
                    handleC0(event.address, event.value);
                    return;
                }
                if (event.value.length == 10 && event.value[3] == (byte) 0xa4) {// A0反馈
                    handleA4(event.address, event.value);
                    return;
                }
                if (event.value.length == 5 && event.value[3] == (byte) 0xaa) {// AA反馈（蜂鸣器）
                    handleAA(event.address, event.value);
                    return;
                }
                if (event.value.length == 5 && event.value[3] == (byte) 0xa8) {// A8反馈（解除配对 目前硬件不给回复）
                    handleAA(event.address, event.value);
                    return;
                }
                break;
        }
    }

    private volatile Semaphore c0Semaphore = new Semaphore(1);

    private void saveC0State(final String address, final byte[] data, final long time) {
        final long from = time - 60000;
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                c0Semaphore.acquire();
                long count = stateDao.getStateCountWithIndex(address, from, time, data[6], data[7], data[9]);
                emitter.onNext(count == 0);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {// 发出通知
                            if (pairedDevices.containsKey(address)) {
                                Device device = pairedDevices.get(address);
                                if (device.notificationType == NotificationType.ALWAYS) {
                                    notifications.sendMovementNotification(device.address, device.name, time,
                                            device.timeFormat + " " + TimeFormatType.TIME_DEFAULT, !device.enableMonitoring ? AlertTuneType.VIBRATION : device.alertTune);
                                }
                            }
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .map(new Function<Boolean, State>() {
                    @Override
                    public State apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {// 存储数据
                            State state = new State();
                            state.time = time;
                            state.address = address;
                            int g = data[8] & 0x01;// 触发g
                            int xyz = data[9] >> 1 & 0x01;// 触发xyz
                            if (g == 1 && xyz == 1) {
                                state.type = StateType.FLIP_AND_SHAKE;
                            } else if (g == 1) {
                                state.type = StateType.SHAKE;
                            } else {
                                state.type = StateType.FLIP;
                            }
                            state.indexH = data[6];
                            state.indexL = data[7];
                            state.reportId = data[9];
                            state.movementsCount = 1;
                            stateDao.insertState(state);
                            return state;
                        }
                        return null;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<State>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(State state) {
                        if (pairedDevices.containsKey(address)) {// 更新缓存
                            Device device = pairedDevices.get(address);
                            device.currentState = state;
                            device.lastUpdateTime = state.time;
                            int count = device.movementsCount;
                            count++;
                            device.movementsCount = count;
                            if (device.address.equals(movementListenerAddress) && movementListener != null) {
                                movementListener.onMovementReceived(state, device.timeFormat);
                            }
                            if (movementCountChangeListener != null) {
                                movementCountChangeListener.onMovementCountChange(address, device.movementsCount);
                            }
                        }
                        c0Semaphore.release();
                    }

                    @Override
                    public void onError(Throwable e) {
                        c0Semaphore.release();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    // search
    private SearchCallback searchCallback;

    @Override
    public void search(int seconds, @NonNull final SearchCallback callback) {
        searchCallback = callback;
        if (!bleService.isBleAvailable()) {
            searchCallback.onBleNotAvailable();
            searchCallback = null;
            return;
        }
        bleService.search(seconds);
    }

    @Override
    public void stopSearch() {
        searchCallback = null;
        bleService.stopSearch();
    }

    // connect
    private ConnectionCallback connectionCallback;
    private String connectCallbackAddress;

    @Override
    public void connect(@NonNull String address, @Nullable ConnectionCallback callback) {
        connectCallbackAddress = address;
        connectionCallback = callback;
        if (!bleService.isBleAvailable()) {
            connectionCallback.onBleNotAvailable();
            connectionCallback = null;
            connectCallbackAddress = null;
            return;
        }
        bleService.connect(address);
    }

    // find
    private SettingCallback findCallback;
    private Disposable findDisposable;

    private void startFindListener() {
        findDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (findCallback != null) {
                            findCallback.onTimeOut();
                            findCallback = null;
                        }
                        stopFindListener();
                    }
                });
    }

    private void stopFindListener() {
        if (findDisposable != null && !findDisposable.isDisposed()) {
            findDisposable.dispose();
            findDisposable = null;
        }
    }

    @Override
    public void find(@NonNull String address, @NonNull SettingCallback callback) {
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                callback.onDeviceDisconnected();
                return;
            }
            findCallback = callback;
            bleService.write(address, new byte[]{(byte) 0xaa});// 响铃
            startFindListener();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    // enableAlert
    private SettingCallback alertCallback;
    private Disposable alertDisposable;
    private boolean tempEnableAlert;

    private void startAlertListener() {
        stopAlertListener();
        alertDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (alertCallback != null) {
                            alertCallback.onTimeOut();
                            alertCallback = null;
                        }
                        stopAlertListener();
                    }
                });
    }

    private void stopAlertListener() {
        if (alertDisposable != null && !alertDisposable.isDisposed()) {
            alertDisposable.dispose();
            alertDisposable = null;
        }
    }

    @Override
    public void enableAlert(@NonNull String address, boolean enable, @NonNull SettingCallback callback) {
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                callback.onDeviceDisconnected();
                return;
            }
            alertCallback = callback;
            tempEnableAlert = enable;
            byte[] data = new byte[7];
            data[0] = (byte) 0xa4;// 设置mode
            data[1] = (byte) 0x0a;// 自动模式多久回报一次

            // mode
            data[2] = (byte) 0x02;// 启动自摸模式
            if (enable) {
                data[2] |= (byte) 0x01;// 启动alert
            }
            if (device.enableG) {
                data[2] |= (byte) 0x04;// 启动g振动
            }
            if (device.enableXYZ) {
                data[2] |= (byte) 0x08;// 启动g振动
            }

            data[3] = (byte) 0x01;// 更换过电池为0
            data[4] = device.gValue;// g强度
            data[5] = device.xyzValue;
            data[6] = (byte) 0x01;
            bleService.write(address, data);
            startAlertListener();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    // unpair
    private SettingCallback unpairCallback;
    private Disposable unpairDisposable;
    private String tempUnpairAddress;

    private void startUnpairListener() {
        stopUnpairListener();
        unpairDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (unpairCallback != null) {
                            // todo 硬件有bug unpair不回复
//                            unpairCallback.onTimeOut();
                            removeUnpairDevice(tempUnpairAddress, unpairCallback);
                            unpairCallback = null;
                        }
                        stopUnpairListener();
                    }
                });
    }

    private void stopUnpairListener() {
        if (unpairDisposable != null && !unpairDisposable.isDisposed()) {
            unpairDisposable.dispose();
            unpairDisposable = null;
        }
    }

    private void removeUnpairDevice(String address, SettingCallback callback) {
        bleService.disconnect(address);
        Observable.just(address)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        deviceDao.deleteDeviceByAddress(s);
                    }
                });
        if (pairedDevices.containsKey(address)) {
            pairedDevices.remove(address);
            callback.onSuccessful();
        } else {
            callback.onDeviceNotAvailable();
        }
    }

    @Override
    public void unpair(@NonNull String address, boolean force, @NonNull SettingCallback callback) {
        if (force) {
            removeUnpairDevice(address, callback);
            return;
        }
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                callback.onDeviceDisconnected();
                return;
            }
            unpairCallback = callback;
            tempUnpairAddress = address;
            bleService.write(address, new byte[]{(byte) 0xa8});// 取消配对
            startUnpairListener();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    // enableG xyz
    private SettingCallback gxyzEnableCallback;
    private Disposable gxyzEnableDisposable;
    private boolean tempEnableG;
    private boolean tempEnableXYZ;

    private void startGXYZEnableListener() {
        stopGXYZEnableListener();
        gxyzEnableDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (gxyzEnableCallback != null) {
                            gxyzEnableCallback.onTimeOut();
                            gxyzEnableCallback = null;
                        }
                        stopGXYZEnableListener();
                    }
                });
    }

    private void stopGXYZEnableListener() {
        if (gxyzEnableDisposable != null && !gxyzEnableDisposable.isDisposed()) {
            gxyzEnableDisposable.dispose();
            gxyzEnableDisposable = null;
        }
    }

    @Override
    public void enableGAndXYZ(@NonNull String address, boolean enableG, boolean enableXYZ, @NonNull SettingCallback callback) {
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                callback.onDeviceDisconnected();
                return;
            }
            gxyzEnableCallback = callback;
            tempEnableG = enableG;
            tempEnableXYZ = enableXYZ;
            byte[] data = new byte[7];
            data[0] = (byte) 0xa4;// 设置mode
            data[1] = (byte) 0x0a;// 自动模式多久回报一次

            // mode
            data[2] = (byte) 0x02;// 启动自摸模式
            if (device.enableAlert) {
                data[2] |= (byte) 0x01;// 启动alert
            }
            if (enableG) {
                data[2] |= (byte) 0x04;// 启动g振动
            }
            if (enableXYZ) {
                data[2] |= (byte) 0x08;// 启动g振动
            }

            data[3] = (byte) 0x01;// 更换过电池为0
            data[4] = device.gValue;// g灵敏度
            data[5] = device.xyzValue;// xyz灵敏度
            data[6] = (byte) 0x01;
            bleService.write(address, data);
            startGXYZEnableListener();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    // g value
    private SettingCallback gValueCallback;
    private Disposable gValueDisposable;
    private byte tempGValue;

    private void startGValueListener() {
        stopGValueListener();
        gValueDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (gValueCallback != null) {
                            gValueCallback.onTimeOut();
                            gValueCallback = null;
                        }
                        stopGValueListener();
                    }
                });
    }

    private void stopGValueListener() {
        if (gValueDisposable != null && !gValueDisposable.isDisposed()) {
            gValueDisposable.dispose();
            gValueDisposable = null;
        }
    }

    @Override
    public void setG(@NonNull String address, byte g, @Nullable SettingCallback callback) {
        if (!bleService.isBleAvailable()) {
            if (callback != null) {
                callback.onBleNotAvailable();
            }
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                if (callback != null) {
                    callback.onDeviceDisconnected();
                }
                return;
            }
            gValueCallback = callback;
            tempGValue = g;
            byte[] data = new byte[7];
            data[0] = (byte) 0xa4;// 设置mode
            data[1] = (byte) 0x0a;// 自动模式多久回报一次

            // mode
            data[2] = (byte) 0x02;// 启动自摸模式
            if (device.enableAlert) {
                data[2] |= (byte) 0x01;// 启动alert
            }
            if (device.enableG) {
                data[2] |= (byte) 0x04;// 启动g振动
            }
            if (device.enableXYZ) {
                data[2] |= (byte) 0x08;// 启动g振动
            }

            data[3] = (byte) 0x01;// 更换过电池为0
            data[4] = g;// g灵敏度
            data[5] = device.xyzValue;// xyz灵敏度
            data[6] = (byte) 0x01;
            bleService.write(address, data);
            startGValueListener();
            return;
        }
        if (callback != null) {
            callback.onDeviceNotAvailable();
        }
    }

    // xyz value
    private SettingCallback xyzValueCallback;
    private Disposable xyzValueDisposable;
    private byte tempXYZValue;

    private void startXYZValueListener() {
        stopXYZValueListener();
        xyzValueDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (xyzValueCallback != null) {
                            xyzValueCallback.onTimeOut();
                            xyzValueCallback = null;
                        }
                        stopXYZValueListener();
                    }
                });
    }

    private void stopXYZValueListener() {
        if (xyzValueDisposable != null && !xyzValueDisposable.isDisposed()) {
            xyzValueDisposable.dispose();
            xyzValueDisposable = null;
        }
    }

    @Override
    public void setXYZ(@NonNull String address, byte xyz, @Nullable SettingCallback callback) {
        if (!bleService.isBleAvailable()) {
            if (callback != null) {
                callback.onBleNotAvailable();
            }
            return;
        }
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (!(device.connectionState == ConnectionType.CONNECTED)) {
                if (callback != null) {
                    callback.onDeviceDisconnected();
                }
                return;
            }
            xyzValueCallback = callback;
            tempXYZValue = xyz;
            byte[] data = new byte[7];
            data[0] = (byte) 0xa4;// 设置mode
            data[1] = (byte) 0x0a;// 自动模式多久回报一次

            // mode
            data[2] = (byte) 0x02;// 启动自摸模式
            if (device.enableAlert) {
                data[2] |= (byte) 0x01;// 启动alert
            }
            if (device.enableG) {
                data[2] |= (byte) 0x04;// 启动g振动
            }
            if (device.enableXYZ) {
                data[2] |= (byte) 0x08;// 启动g振动
            }

            data[3] = (byte) 0x01;// 更换过电池为0
            data[4] = device.gValue;// g灵敏度
            data[5] = xyz;// xyz灵敏度
            data[6] = (byte) 0x01;
            bleService.write(address, data);
            startXYZValueListener();
            return;
        }
        if (callback != null) {
            callback.onDeviceNotAvailable();
        }
    }
}
