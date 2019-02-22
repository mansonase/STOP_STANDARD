package com.viseeointernational.stop.view.page.main;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.viseeointernational.stop.App;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.constant.ConnectionType;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.util.DisplaySizeUtil;
import com.viseeointernational.stop.view.custom.BatteryView;
import com.viseeointernational.stop.view.custom.DeviceView;
import com.viseeointernational.stop.view.custom.IfDialog;
import com.viseeointernational.stop.view.page.BaseActivity;
import com.viseeointernational.stop.view.page.add.AddActivity;
import com.viseeointernational.stop.view.page.detail.DetailActivity;
import com.viseeointernational.stop.view.page.guide.GuideActivity;
import com.viseeointernational.stop.view.page.help.HelpActivity;
import com.viseeointernational.stop.view.page.setting.SettingActivity;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends BaseActivity implements MainActivityContract.View {

    public static final int REQUEST_BLUETOOTH = 1;

    @BindView(R.id.power)
    BatteryView power;
    @BindView(R.id.layout_device)
    LinearLayout layoutDevice;
    @BindView(R.id.image)
    CircleImageView image;
    @BindView(R.id.ring)
    ImageView ring;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.last_update)
    TextView lastUpdate;
    @BindView(R.id.on_off)
    CheckBox onOff;
    @BindView(R.id.state)
    TextView state;
    @BindView(R.id.time)
    TextView time;

    @Inject
    MainActivityContract.Presenter presenter;

    private Disposable disposable;

    private Map<String, DeviceView> deviceViewMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ((App) getApplication()).getAppComponent().mainActivityComponent().build().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.takeView(this);
    }

    @Override
    protected void onPause() {
        stopBlink();
        presenter.dropView();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        presenter.appExit();
        super.onDestroy();
    }

    @Override
    public void showEnableBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_BLUETOOTH);
    }

    @Override
    public void showGuide() {
        startActivity(new Intent(this, GuideActivity.class));
    }

    @Override
    public void showDevices(@NonNull List<Device> list, String checkedAddress) {
        deviceViewMap.clear();
        layoutDevice.removeAllViews();
        for (int i = 0; i < list.size(); i++) {
            Device device = list.get(i);
            DeviceView view = new DeviceView(this);
            view.setImage(device.imagePath);
            view.setConnected(device.connectionState == ConnectionType.CONNECTED);
            view.setMovements(device.movementsCount);
            view.setCheck(device.address.equals(checkedAddress));
            view.setTag(device.address);
            view.setOnClickListener(onClickListener);
            int dp50 = DisplaySizeUtil.dp2px(this, 50);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp50, dp50);
            int dp10 = DisplaySizeUtil.dp2px(this, 10);
            lp.setMargins(dp10, 0, dp10, 0);
            layoutDevice.addView(view, lp);
            deviceViewMap.put(device.address, view);
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof DeviceView) {
                DeviceView clickedView = (DeviceView) v;
                for (int i = 0; i < layoutDevice.getChildCount(); i++) {
                    View view = layoutDevice.getChildAt(i);
                    if (view instanceof DeviceView) {
                        DeviceView deviceView = (DeviceView) view;
                        deviceView.setCheck(false);
                    }
                }
                clickedView.setCheck(true);
                Object tag = clickedView.getTag();
                if (tag instanceof String) {
                    String address = (String) tag;
                    presenter.checkDevice(address);
                }
            }
        }
    };

    @Override
    public void showMovementCountChange(@NonNull String address, int count) {
        if (deviceViewMap.containsKey(address)) {
            deviceViewMap.get(address).setMovements(count);
        }
    }

    @Override
    public void showDeviceConnectionChange(@NonNull String address, boolean isConnected) {
        if (deviceViewMap.containsKey(address)) {
            deviceViewMap.get(address).setConnected(isConnected);
        }
    }

    @Override
    public void showBlink() {
        stopBlink();
        disposable = Observable.interval(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    int i;

                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (i % 2 == 0) {
                            ring.setImageDrawable(getResources().getDrawable(R.drawable.ring_red));
                        } else {
                            ring.setImageDrawable(getResources().getDrawable(R.drawable.ring_theme));
                        }
                        if (i == 5) {
                            stopBlink();
                        }
                        i++;
                    }
                });
    }

    private void stopBlink() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            ring.setImageDrawable(getResources().getDrawable(R.drawable.ring_theme));
        }
    }

    @Override
    public void showHeader(String s) {
        try {
            Picasso.with(this).load(new File(s)).placeholder(R.mipmap.ic_default_header).into(image);// .error
        } catch (Exception e) {
            e.printStackTrace();
            Picasso.with(this).load(R.mipmap.ic_default_header).into(image);
        }
    }

    @Override
    public void showName(String s) {
        name.setText(s);
    }

    @Override
    public void showMonitoringEnable(boolean enable) {
        onOff.setOnCheckedChangeListener(null);
        onOff.setChecked(enable);
        onOff.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            presenter.enableMonitoring(isChecked);
        }
    };

    @Override
    public void showLastUpdate(String s) {
        lastUpdate.setText(s);
    }

    @Override
    public void showState(String s) {
        state.setText(s);
    }

    @Override
    public void showTime(String s) {
        time.setText(s);
    }

    @Override
    public void showBattery(int i) {
        power.setPower(i);
    }

    @Override
    public void alertIfForceReset() {
        new IfDialog(this, new IfDialog.Callback() {
            @Override
            public void onYes(IfDialog dialog) {
                presenter.reset(true);
            }

            @Override
            public void onNo(IfDialog dialog) {
            }
        }).show(getText(R.string.dialog_force_reset));
    }

    @Override
    public void showDetail(String address) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.KEY_ADDRESS, address);
        startActivity(intent);
    }

    @Override
    public void showSetting(String address) {
        Intent intent = new Intent(this, SettingActivity.class);
        intent.putExtra(SettingActivity.KEY_ADDRESS, address);
        startActivity(intent);
    }

    @Override
    public void showAddNewDevice() {
        startActivity(new Intent(this, AddActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        presenter.result(requestCode, resultCode, data);
    }

    @OnClick({R.id.help, R.id.add, R.id.setup, R.id.detail, R.id.reset})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.help:
                startActivity(new Intent(this, HelpActivity.class));
                break;
            case R.id.add:
                presenter.addNewDevice();
                break;
            case R.id.setup:
                presenter.showSetting();
                break;
            case R.id.detail:
                presenter.showDetail();
                break;
            case R.id.reset:
                presenter.reset(false);
                break;
        }
    }

    private long exitTime;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showMessage(R.string.msg_exit);
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
