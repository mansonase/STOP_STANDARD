package com.viseeointernational.stop.data.source.device;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.viseeointernational.stop.data.constant.AlertTuneType;
import com.viseeointernational.stop.data.constant.ChartType;
import com.viseeointernational.stop.data.constant.ConnectionType;
import com.viseeointernational.stop.data.constant.NotificationType;
import com.viseeointernational.stop.data.constant.StateType;
import com.viseeointernational.stop.data.constant.TimeFormatType;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.data.source.android.ble.BleEvent;
import com.viseeointernational.stop.data.source.android.ble.BleService;
import com.viseeointernational.stop.data.source.base.database.DeviceDao;
import com.viseeointernational.stop.data.source.base.database.StateDao;
import com.viseeointernational.stop.util.BitmapUtil;
import com.viseeointernational.stop.util.StringUtil;
import com.viseeointernational.stop.util.TimeUtil;
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
    private Notifications notifications;

    private Map<String, Device> pairedDevices = new LinkedHashMap<>();

    @Inject
    public DeviceRepository(DeviceDao deviceDao, StateDao stateDao, BleService bleService, Notifications notifications) {
        this.deviceDao = deviceDao;
        this.stateDao = stateDao;
        this.bleService = bleService;
        this.notifications = notifications;
        EventBus.getDefault().register(this);
    }

    /**********************************************自动重连****************************************************/
    private Disposable autoDisposable;

    private void startAutoConnect() {
        stopAutoConnect();
        autoDisposable = Observable.interval(0, 20, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .map(new Function<Long, Boolean>() {
                    @Override
                    public Boolean apply(Long aLong) throws Exception {// 判断是否有未连接的设备
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
                    public void accept(Boolean aBoolean) throws Exception {// 搜索后再重连
                        if (aBoolean) {
                            bleService.search(10);
                        }
                        Log.d(TAG, "是否有重连 = " + aBoolean);
                    }
                });
    }

    private void stopAutoConnect() {
        if (autoDisposable != null && !autoDisposable.isDisposed()) {
            autoDisposable.dispose();
            autoDisposable = null;
        }
    }

    /**************************************************************************************************/

    @Override
    public boolean isBleEnable() {
        return bleService.isBleAvailable();
    }

    @Override
    public void onAppExit() {
        if (pairedDevices.size() == 0 || !bleService.isBleAvailable()) {
            EventBus.getDefault().unregister(this);
            notifications.stopAutoSend();
            stopAutoConnect();
            bleService.stopSelf();
        }
    }

    /**********************************************添加获取设备***************************************************/
    @Override
    public void saveDevice(@NonNull Device device) {
        saveDeviceToDatabase(device);
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
    public void getPairedDevices(@NonNull final GetPairedDevicesCallback callback) {
        if (pairedDevices.size() > 0) {
            List<Device> list = new ArrayList<>(pairedDevices.values());
            callback.onDevicesLoaded(list);
            return;
        }
        Observable.just(1)
                .subscribeOn(Schedulers.io())
                .map(new Function<Integer, List<Device>>() {
                    @Override
                    public List<Device> apply(Integer integer) throws Exception {// 从数据库获取已配对数据
                        return deviceDao.getPairedDevice();
                    }
                })
                .doOnNext(new Consumer<List<Device>>() {
                    @Override
                    public void accept(List<Device> devices) throws Exception {// 具有pairid的设备放入缓存
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
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Device>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        List<Device> list = new ArrayList<>(pairedDevices.values());
                        callback.onDevicesLoaded(list);
                        startAutoConnect();// 开启自动重连
                    }

                    @Override
                    public void onError(Throwable e) {
                        List<Device> list = new ArrayList<>(pairedDevices.values());
                        callback.onDevicesLoaded(list);
                        startAutoConnect();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    @Override
    public void getPairedDeviceCount(@NonNull GetPairedDeviceCountCallback callback) {
        callback.onCountLoaded(pairedDevices.size());
    }

    /**********************************************获取状态***************************************************/
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
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    /**********************************************监听电量***************************************************/
    private String batteryListenerAddress;
    private BatteryListener batteryListener;
    private Disposable batteryDisposable;

    private void handleBatteryListen(final String address, int power) {
        if (batteryListener != null && address.equals(batteryListenerAddress)) {
            batteryListener.onPowerReceived(power);
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

    private void stopGetBattery() {
        if (batteryDisposable != null && !batteryDisposable.isDisposed()) {
            batteryDisposable.dispose();
            batteryDisposable = null;
        }
    }

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

    /**********************************************监听触发***************************************************/
    private String movementListenerAddress;
    private MovementListener movementListener;

    private void handleMovementListen(String address, State state, String timeFormat) {
        if (movementListener != null && address.equals(movementListenerAddress)) {
            movementListener.onMovementReceived(state, timeFormat);
        }
    }

    @Override
    public void setMovementListener(@NonNull String address, @Nullable MovementListener listener) {
        movementListenerAddress = address;
        movementListener = listener;
    }

    /**********************************************监听触发次数***************************************************/
    private MovementCountChangeListener movementCountChangeListener;

    private void handleMovementCountListen(String address, int count) {
        if (movementCountChangeListener != null) {
            movementCountChangeListener.onMovementCountChange(address, count);
        }
    }

    @Override
    public void setMovementCountChangeListener(@Nullable MovementCountChangeListener listener) {
        movementCountChangeListener = listener;
    }

    /**********************************************监听连接状态***************************************************/
    private DeviceConnectionChangeListener deviceConnectionChangeListener;

    private void handleDeviceConnectionChangeListen(String address, boolean isConnected) {
        if (deviceConnectionChangeListener != null) {
            deviceConnectionChangeListener.onConnectionChange(address, isConnected);
        }
    }

    @Override
    public void setDeviceConnectionChangeListener(@Nullable DeviceConnectionChangeListener listener) {
        deviceConnectionChangeListener = listener;
    }

    /**********************************************监听添加移除设备***************************************************/
    private DeviceCountChangeListener deviceCountChangeListener;

    private void handleDeviceCountChangeListen() {
        if (deviceCountChangeListener != null) {
            deviceCountChangeListener.onDeviceCountChange(new ArrayList<Device>(pairedDevices.values()));
        }
    }

    @Override
    public void setDeviceCountChangeListener(@Nullable DeviceCountChangeListener listener) {
        deviceCountChangeListener = listener;
    }

    /********************************************gatt已连接回调*****************************************************/

    private void handleGattConnected(String address) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            byte[] b2 = new byte[5];
            b2[0] = (byte) 0xb2;
            b2[1] = StringUtil.hexString2byte(device.pairId.substring(0, 2));
            b2[2] = StringUtil.hexString2byte(device.pairId.substring(2, 4));
            b2[3] = StringUtil.hexString2byte(device.pairId.substring(4, 6));
            b2[4] = StringUtil.hexString2byte(device.pairId.substring(6, 8));
            bleService.write(address, b2);// 连线
        } else {
            bleService.write(address, new byte[]{(byte) 0xb0});// 配对
        }
    }

    /************************************************gatt断开连接回调*************************************************/
    private void handleGattDisconnected(String address) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (device.connectionState == ConnectionType.CONNECTED) {
                notifications.sendLostConnectionNotification(device.address, device.name, Calendar.getInstance().getTimeInMillis(), device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
            }
            device.connectionState = ConnectionType.DISCONNECTED;
        }
    }

    /************************************************配对回调*************************************************/

    private byte[] pairIds = new byte[4];// 缓存pair id

    private void handleB0(String address, byte[] data) {
        System.arraycopy(data, 4, pairIds, 0, pairIds.length);// 拷贝
        byte[] b1 = new byte[5];
        b1[0] = (byte) 0xb1;
        System.arraycopy(pairIds, 0, b1, 1, 4);
        bleService.write(address, b1);// 确认配对
    }

    /************************************************确认配对回调*************************************************/
    private void handleB1(String address, byte[] data) {
        if (data[4] == (byte) 0xaa) {// 成功
            byte[] b2 = new byte[5];
            b2[0] = (byte) 0xb2;
            System.arraycopy(pairIds, 0, b2, 1, 4);
            bleService.write(address, b2);// 连线
        } else {
            // 失败 硬件会自动断开
        }
    }

    /************************************************连线失败*************************************************/

    private void handleEE(final String address, final byte[] data) {
        Log.d(TAG, "b2连接失败" + address);
        // 硬件会自动断开gatt
        if (data[4] == (byte) 0x01) {// pair id 错误
            if (pairedDevices.containsKey(address)) {
                Device device = pairedDevices.get(address);
                device.pairId = "";
                saveDeviceToDatabase(device);
                pairedDevices.remove(address);
                handleDeviceCountChangeListen();
            }
        }
    }

    /************************************************连线成功*************************************************/

    private void handleB2(String address) {
        Log.d(TAG, "b2连线成功" + address);
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.isInit = false;
            device.connectionState = ConnectionType.CONNECTED;
            notifications.sendConnectedNotification(device.address, device.name);
            bleService.write(device.address, getA4Data(device.enableAlert, device.enableG, device.enableXYZ, false, device.gValue, device.xyzValue));
            bleService.write(address, new byte[]{(byte) 0xa0});// 发A0拿历史数据
        } else {
            initNewDevice(address);
        }
    }

    private void initNewDevice(final String address) {
        Observable.just(pairIds)
                .map(new Function<byte[], Device>() {
                    @Override
                    public Device apply(byte[] bytes) throws Exception {// 保存pairid 并初始化
                        Device device = deviceDao.getDeviceByAddress(address);
                        device.isInit = true;
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
                        device.defaultShow = ChartType.HOUR;
                        device.connectionState = ConnectionType.CONNECTED;
                        deviceDao.insertDevice(device);
                        return device;
                    }
                })
                .doOnNext(new Consumer<Device>() {
                    @Override
                    public void accept(Device device) throws Exception {// 发送设置mode
                        bleService.write(device.address, getA4Data(device.enableAlert, device.enableG, device.enableXYZ,
                                true, device.gValue, device.xyzValue));
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
                        pairedDevices.put(address, device);
                        handleDeviceCountChangeListen();
                        notifications.sendConnectedNotification(device.address, device.name);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void handleA0(final String address, final byte[] data) {
        if (!pairedDevices.containsKey(address)) {
            return;
        }
        final Device device = pairedDevices.get(address);
        Observable.just(1)
                .map(new Function<Integer, State>() {
                    @Override
                    public State apply(Integer integer) throws Exception {
                        State state = stateDao.getLastStateWithoutType(device.address, StateType.RESET);
                        if (state != null) {
                            return state;
                        }
                        return new State();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<State>() {
                    @Override
                    public void accept(State state) throws Exception {
                        if (data[6] == (byte) 0x00 && data[7] == (byte) 0x00) {
                            notifications.sendChangeNewBatteryNotification(device.address, device.name);
                            throw new Exception();
                        }
                        if (data[5] == (byte) 0x00) {
                            Log.d(TAG, "新电池不接受历史");
                            throw new Exception();
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(new Observer<State>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(State state) {
                        long startTime;
                        long now = Calendar.getInstance().getTimeInMillis();
                        if (state.time != 0) {
                            startTime = state.time;
                        } else {
                            startTime = now - (data[6] & 0xff * 0x100 + data[7] & 0xff) * 1000 * 60;
                        }
                        Log.d(TAG, "上次state时间 " + TimeUtil.getTime(startTime, TimeFormatType.DATE_3_1 + "  " + TimeFormatType.TIME_DEFAULT));
                        device.historyDataSet = new HistoryDataSet(device.address, data[6], data[7], startTime, now);
                        bleService.write(device.address, device.historyDataSet.createA1Cmd());
                        device.historyTimer = new OperateTimer(new OperateTimer.Callback() {
                            @Override
                            public void onTimeOut() {
                                bleService.write(device.address, device.historyDataSet.getA1());
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        device.historyDataSet = null;
                        device.isInit = true;
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void handleA1(final String address, final byte[] data) {
        if (!pairedDevices.containsKey(address)) {
            return;
        }
        final Device device = pairedDevices.get(address);
        if (device.historyTimer != null) {
            device.historyTimer.stopCount();
        }
        if (device.historyDataSet == null) {
            device.isInit = true;
            device.historyTimer = null;
            return;
        }
        Observable.just(1)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        for (int i = 7; i < 127; i++) {
                            if (data[6] == (byte) 0x00 && i == 7 && (device.historyDataSet.indexL & 0xff) % 2 == 0) {// 第一组0-3bit有效
                                int count = data[i] & 0x0f;
                                if (!device.historyDataSet.isEnd()) {
                                    device.historyDataSet.putState(count);
                                }
                                device.historyDataSet.next();
                            } else {// 0-7bit有效
                                int count = (data[i] >> 4) & 0x0f;
                                if (!device.historyDataSet.isEnd()) {
                                    device.historyDataSet.putState(count);
                                }
                                device.historyDataSet.next();
                                count = data[i] & 0x0f;
                                if (!device.historyDataSet.isEnd()) {
                                    device.historyDataSet.putState(count);
                                }
                                device.historyDataSet.next();
                            }
                        }
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        if (device.historyDataSet.isEnd()) {
                            List<State> states = device.historyDataSet.getReceivedData();
                            int totalCount = device.historyDataSet.getTotalCount();
                            device.historyDataSet = null;
                            device.isInit = true;
                            device.historyTimer = null;
                            stateDao.insertState(states);
                            return totalCount;
                        }
                        bleService.write(address, device.historyDataSet.createA1Cmd());
                        device.historyTimer = new OperateTimer(new OperateTimer.Callback() {
                            @Override
                            public void onTimeOut() {
                                bleService.write(device.address, device.historyDataSet.getA1());
                            }
                        });
                        return -1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Integer integer) {
                        if (integer != -1) {
                            Log.d(TAG, "历史数据读取完毕 count = " + integer);
                            device.movementsCount += integer;
                            handleMovementCountListen(device.address, device.movementsCount);
                        } else {
                            Log.d(TAG, "历史数据还有等待读取");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void handleC0(final String address, final byte[] data) {
        bleService.write(address, new byte[]{(byte) 0xa2});// 立即清除此通知
        if (pairedDevices.containsKey(address)) {
            if (!pairedDevices.get(address).isInit) {
                Log.d(TAG, "未初始化完成");
                return;
            }
        }
        saveC0State(address, data, Calendar.getInstance().getTimeInMillis());
    }

    // 以下需要service回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleEvent(final BleEvent event) {
        switch (event.type) {
            case BleEvent.BLUETOOTH_ADAPTER_DISABLE:// 蓝牙关闭
                for (Map.Entry<String, Device> entry : pairedDevices.entrySet()) {// 所有设备断开连接
                    entry.getValue().connectionState = ConnectionType.DISCONNECTED;
                    handleDeviceConnectionChangeListen(entry.getKey(), false);
                }
                break;
            case BleEvent.BLUETOOTH_ADAPTER_ENABLE:// 蓝牙启用
                break;
            case BleEvent.GATT_CONNECTED:// gatt已连接
                handleGattConnected(event.address);
                break;
            case BleEvent.GATT_DISCONNECTED:// gatt断开连接
                handleConnectionDisconnected(event.address);// 主动连接时
                handleGattDisconnected(event.address);
                handleDeviceConnectionChangeListen(event.address, false);
                break;
            case BleEvent.SEARCH_DEVICE_FOUND:// 发现设备
                handleAutoReconnect(event.address);
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
                    handleDeviceConnectionChangeListen(event.address, true);
                    handleB2(event.address);
                    handleConnectionConnected(event.address);
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
                    int power;
                    if (event.value[4] > (byte) 0xdc) {
                        power = 100;
                    } else if (event.value[4] >= (byte) 0xc2) {
                        int temp = event.value[4] - (byte) 0xc2;
                        power = (int) (temp / 26f * 95) + 5;
                    } else {
                        power = 3;
                    }
                    handleBatteryListen(event.address, power);
                    handleA3(event.address, power);
                    return;
                }
                if (event.value.length == 16 && event.value[3] == (byte) 0xc0) {// C0反馈
                    handleC0(event.address, event.value);
                    return;
                }
                if (event.value.length == 10 && event.value[3] == (byte) 0xa4) {// A4反馈
                    handleXYZSetting(event.address, event.value);
                    handleGSetting(event.address, event.value);
                    handleGXYZEnableSetting(event.address, event.value);
                    handleAlertEnableSetting(event.address, event.value);
                    return;
                }
                if (event.value.length == 5 && event.value[3] == (byte) 0xaa) {// AA反馈（蜂鸣器）
                    handleAA(event.address, event.value);
                    return;
                }
                if (event.value.length == 5 && event.value[3] == (byte) 0xa8) {// A8反馈（解除配对 目前硬件不给回复）
                    return;
                }
                break;
        }
    }

    private void handleA3(String address, int power) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            int lastPower = device.battery;
            if (power < 15 && (power < lastPower || lastPower == 0)) {
                notifications.sendLowPowerNotification(device.address, device.name);
            }
            device.battery = power;
        }
    }

    private void handleAutoReconnect(String address) {
        if (pairedDevices.containsKey(address)) {// 自动重连
            Device device = pairedDevices.get(address);
            if (device.connectionState == ConnectionType.DISCONNECTED) {
                device.connectionState = ConnectionType.CONNECTING;
                bleService.connect(address, true);
                Log.d(TAG, "搜索到设备 执行重连");
            }
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
                            handleMovementListen(device.address, state, device.timeFormat);
                            device.movementsCount++;
                            handleMovementCountListen(address, device.movementsCount);
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

    /**********************************************保存是否通知带铃声****************************************************/
    @Override
    public void enableMonitoring(@NonNull String address, boolean enable) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.enableMonitoring = enable;
            saveDeviceToDatabase(device);
        }
    }

    /**********************************************保存名字****************************************************/
    @Override
    public void setName(@NonNull final String address, @NonNull final String name) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.name = name;
            saveDeviceToDatabase(device);
        } else {
            Observable.just(1)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(Integer integer) {
                            Device device = deviceDao.getDeviceByAddress(address);
                            device.name = name;
                            deviceDao.insertDevice(device);
                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }

    /**********************************************保存头像****************************************************/
    @Override
    public void setHeader(@NonNull final String address, @NonNull final Bitmap bitmap) {
        Observable.just(pairedDevices.containsKey(address))
                .map(new Function<Boolean, Device>() {
                    @Override
                    public Device apply(Boolean aBoolean) throws Exception {// 获取设备
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
                .subscribe(new Observer<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Device device) {// 更新数据库
                        deviceDao.insertDevice(device);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
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

    /**********************************************保存时间格式****************************************************/
    @Override
    public void setTimeFormat(@NonNull final String address, @NonNull final String format) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.timeFormat = format;
            saveDeviceToDatabase(device);
        }
    }

    /**********************************************保存铃声类型****************************************************/
    @Override
    public void setAlertTune(@NonNull final String address, final int alertTune) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            if (alertTune != AlertTuneType.VIBRATION) {
                device.alertTune = alertTune;
                device.enableMonitoring = true;
            } else {
                device.enableMonitoring = false;
            }
            saveDeviceToDatabase(device);
        }
    }

    /**********************************************保存通知类型****************************************************/
    @Override
    public void setNotification(@NonNull final String address, final int notificationType) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.notificationType = notificationType;
            saveDeviceToDatabase(device);
        }
    }

    @Override
    public void setDefaultShow(@NonNull String address, int type) {
        if (pairedDevices.containsKey(address)) {
            Device device = pairedDevices.get(address);
            device.defaultShow = type;
            saveDeviceToDatabase(device);
        }
    }

    /**********************************************reset功能****************************************************/
    @Override
    public void reset(@NonNull final String address, final double latitude, final double longitude, @NonNull final ResetCallback callback) {
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
            if (device.currentState == null || device.currentState.type == StateType.RESET) {
                callback.onAlreadyReset();
                return;
            }
            State state = new State();
            state.address = address;
            state.type = StateType.RESET;
            state.time = Calendar.getInstance().getTimeInMillis();
            state.latitude = latitude;
            state.longitude = longitude;
            saveStateToDatabase(state);

            device.currentState = state;
            handleMovementListen(address, state, device.timeFormat);
            device.movementsCount = 0;
            handleMovementCountListen(device.address, device.movementsCount);
            callback.onSuccessful();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    /**********************************************搜索功能****************************************************/
    private SearchCallback searchCallback;

    private void handleSearchDeviceFound(String address, String name, int rssi) {
        if (searchCallback != null && !pairedDevices.containsKey(address) && name != null && name.startsWith("STOP")) {// 只返回STOP未配对的设备
            Device device = new Device();
            device.address = address;
            device.name = name;
            device.rssi = rssi;
            searchCallback.onDeviceFound(device);
        }
    }

    private void handleSearchFinish() {
        if (searchCallback != null) {
            searchCallback.onFinish();
            searchCallback = null;
        }
    }

    @Override
    public void search(int seconds, @NonNull final SearchCallback callback) {
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        searchCallback = callback;
        bleService.search(seconds);
    }

    @Override
    public void stopSearch() {
        searchCallback = null;
        bleService.stopSearch();
    }

    /**********************************************主动连接****************************************************/
    private ConnectionCallback connectionCallback;
    private String connectCallbackAddress;

    private void handleConnectionDisconnected(String address) {
        if (connectionCallback != null && address.equals(connectCallbackAddress)) {
            connectionCallback.onDisconnected();
            connectionCallback = null;
        }
    }

    private void handleConnectionConnected(String address) {
        if (connectionCallback != null && address.equals(connectCallbackAddress)) {
            connectionCallback.onConnected();
            connectionCallback = null;
        }
    }

    @Override
    public void connect(@NonNull String address, @NonNull ConnectionCallback callback) {
        if (!bleService.isBleAvailable()) {
            callback.onBleNotAvailable();
            return;
        }
        connectCallbackAddress = address;
        connectionCallback = callback;
        bleService.connect(address, true);
    }

    /**********************************************find功能*************************************************** */
    private SettingCallback findCallback;
    private OperateTimer findTimer;
    private String tempFindAddress;

    private void handleAA(final String address, final byte[] data) {
        if (findCallback != null && address.equals(tempFindAddress)) {
            findTimer.stopCount();
            findCallback.onSuccessful();
            findCallback = null;
        }
    }

    @Override
    public void find(@NonNull String address, @NonNull final SettingCallback callback) {
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
            tempFindAddress = address;
            findCallback = callback;
            bleService.write(address, new byte[]{(byte) 0xaa});// 响铃
            findTimer = new OperateTimer(new OperateTimer.Callback() {
                @Override
                public void onTimeOut() {
                    callback.onTimeOut();
                }
            });
            findTimer.startCount();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    /**********************************************触发震动是否响*************************************************** */
    private SettingCallback enableAlertCallback;
    private OperateTimer enableAlertTimer;
    private boolean tempEnableAlert;
    private String tempAlertEnableAddress;

    private void handleAlertEnableSetting(String address, byte[] data) {
        if (enableAlertCallback != null && pairedDevices.containsKey(address) && address.equals(tempAlertEnableAddress)) {
            enableAlertTimer.stopCount();
            Device device = pairedDevices.get(address);
            boolean enable = (data[5] & 0x01) == 0x01;
            Log.d(TAG, "alert = " + enable);
            device.enableAlert = enable;
            saveDeviceToDatabase(device);
            if (enable == tempEnableAlert) {
                enableAlertCallback.onSuccessful();
            } else {
                enableAlertCallback.onFailed();
            }
            enableAlertCallback = null;
        }
    }

    @Override
    public void enableAlert(@NonNull String address, boolean enable, @NonNull final SettingCallback callback) {
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
            tempAlertEnableAddress = address;
            enableAlertCallback = callback;
            tempEnableAlert = enable;
            bleService.write(address, getA4Data(enable, device.enableG, device.enableXYZ, false, device.gValue, device.xyzValue));
            enableAlertTimer = new OperateTimer(new OperateTimer.Callback() {
                @Override
                public void onTimeOut() {
                    callback.onTimeOut();
                }
            });
            enableAlertTimer.startCount();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    /**********************************************unpair*************************************************** */
    private OperateTimer unpairTimer;

    private void deleteDevice(final String address) {
        Observable.just(1)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Integer integer) {
                        deviceDao.deleteDeviceByAddress(address);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        if (pairedDevices.containsKey(address)) {
            pairedDevices.remove(address);
        }
        handleDeviceCountChangeListen();
    }

    @Override
    public void unpair(@NonNull final String address, boolean force, @NonNull final SettingCallback callback) {
        if (force) {
            bleService.write(address, new byte[]{(byte) 0xa8});// 取消配对
            deleteDevice(address);
            callback.onSuccessful();
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
            bleService.write(address, new byte[]{(byte) 0xa8});// 取消配对
            unpairTimer = new OperateTimer(new OperateTimer.Callback() {
                @Override
                public void onTimeOut() {
                    deleteDevice(address);
                    callback.onSuccessful();
                }
            });
            unpairTimer.startCount();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    /**********************************************设置gxyz是否启用*************************************************** */
    private SettingCallback gxyzEnableCallback;
    private OperateTimer gxyzEnableTimer;
    private boolean tempEnableG;
    private boolean tempEnableXYZ;
    private String tempGXYZEnableAddress;

    private void handleGXYZEnableSetting(String address, byte[] data) {
        if (gxyzEnableCallback != null && pairedDevices.containsKey(address) && address.equals(tempGXYZEnableAddress)) {
            gxyzEnableTimer.stopCount();
            Device device = pairedDevices.get(address);
            boolean enableG = (data[5] >> 2 & 0x01) == 0x01;
            boolean enableXYZ = (data[5] >> 3 & 0x01) == 0x01;
            Log.d(TAG, "g = " + enableG + " xyz = " + enableXYZ);
            device.enableG = enableG;
            device.enableXYZ = enableXYZ;
            saveDeviceToDatabase(device);
            if (enableG == tempEnableG && enableXYZ == tempEnableXYZ) {
                gxyzEnableCallback.onSuccessful();
            } else {
                gxyzEnableCallback.onFailed();
            }
            gxyzEnableCallback = null;
        }
    }

    @Override
    public void enableGAndXYZ(@NonNull String address, boolean enableG, boolean enableXYZ, @NonNull final SettingCallback callback) {
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
            tempGXYZEnableAddress = address;
            gxyzEnableCallback = callback;
            tempEnableG = enableG;
            tempEnableXYZ = enableXYZ;
            bleService.write(address, getA4Data(device.enableAlert, enableG, enableXYZ, false, device.gValue, device.xyzValue));
            gxyzEnableTimer = new OperateTimer(new OperateTimer.Callback() {
                @Override
                public void onTimeOut() {
                    callback.onTimeOut();
                }
            });
            gxyzEnableTimer.startCount();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    /**********************************************设置g灵敏度*************************************************** */
    private SettingCallback gValueCallback;
    private OperateTimer gValueTimer;
    private byte tempGValue;
    private String tempGValueAddress;

    private void handleGSetting(String address, byte[] data) {
        if (gValueCallback != null && pairedDevices.containsKey(address) && address.equals(tempGValueAddress)) {
            gValueTimer.stopCount();
            Device device = pairedDevices.get(address);
            device.gValue = data[6];
            Log.d(TAG, "g = " + (data[6] & 0xff));
            saveDeviceToDatabase(device);
            if (data[6] == tempGValue) {
                gValueCallback.onSuccessful();
            } else {
                gValueCallback.onFailed();
            }
            gValueCallback = null;
        }
    }

    @Override
    public void setG(@NonNull String address, byte g, @NonNull final SettingCallback callback) {
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
            tempGValueAddress = address;
            gValueCallback = callback;
            tempGValue = g;
            bleService.write(address, getA4Data(device.enableAlert, device.enableG, device.enableXYZ, false, g, device.xyzValue));
            gValueTimer = new OperateTimer(new OperateTimer.Callback() {
                @Override
                public void onTimeOut() {
                    callback.onTimeOut();
                }
            });
            gValueTimer.startCount();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    /**********************************************设置xyz灵敏度*************************************************** */
    private SettingCallback xyzValueCallback;
    private OperateTimer xyzValueTimer;
    private byte tempXYZValue;
    private String tempXYZValueAddress;

    private void handleXYZSetting(String address, byte[] data) {
        if (xyzValueCallback != null && pairedDevices.containsKey(address) && address.equals(tempXYZValueAddress)) {
            xyzValueTimer.stopCount();
            Device device = pairedDevices.get(address);
            device.xyzValue = data[7];
            Log.d(TAG, "xyz = " + (data[7] & 0xff));
            saveDeviceToDatabase(device);
            if (data[7] == tempXYZValue) {
                xyzValueCallback.onSuccessful();
            } else {
                xyzValueCallback.onFailed();
            }
            xyzValueCallback = null;
        }
    }

    @Override
    public void setXYZ(@NonNull String address, byte xyz, @NonNull final SettingCallback callback) {
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
            tempXYZValueAddress = address;
            xyzValueCallback = callback;
            tempXYZValue = xyz;
            bleService.write(address, getA4Data(device.enableAlert, device.enableG, device.enableXYZ, false, device.gValue, xyz));
            xyzValueTimer = new OperateTimer(new OperateTimer.Callback() {
                @Override
                public void onTimeOut() {
                    callback.onTimeOut();
                }
            });
            xyzValueTimer.startCount();
            return;
        }
        callback.onDeviceNotAvailable();
    }

    // 获取setmode数据
    private byte[] getA4Data(boolean enableAlert, boolean enableG, boolean enableXYZ, boolean changeBattery, byte g, byte xyz) {
        byte[] a4 = new byte[7];
        a4[0] = (byte) 0xa4;// 设置mode
        a4[1] = (byte) 0x0a;// 自动模式多久回报一次

        // mode
        a4[2] = (byte) 0x00;// 关闭每隔一段时间自动返回数据
        if (enableAlert) {
            a4[2] |= (byte) 0x01;// 启动alert
        }
        if (enableG) {
            a4[2] |= (byte) 0x04;// 启动g振动
        }
        if (enableXYZ) {
            a4[2] |= (byte) 0x08;// 启动g振动
        }

        a4[3] = changeBattery ? (byte) 0x00 : (byte) 0x01;// 更换过电池为0
        a4[4] = g;// g灵敏度
        a4[5] = xyz;// xyz灵敏度
        a4[6] = (byte) 0x01;
        return a4;
    }

    // 保存设备
    private void saveDeviceToDatabase(final Device device) {
        Observable.just(1)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Integer integer) {
                        deviceDao.insertDevice(device);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    // 保存状态
    private void saveStateToDatabase(final State state) {
        Observable.just(1)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Integer integer) {
                        stateDao.insertState(state);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }
}
