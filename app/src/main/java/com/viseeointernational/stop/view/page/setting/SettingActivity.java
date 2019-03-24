package com.viseeointernational.stop.view.page.setting;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.viseeointernational.stop.App;
import com.viseeointernational.stop.BuildConfig;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.view.adapter.GpsAdapter;
import com.viseeointernational.stop.view.custom.DateDialog;
import com.viseeointernational.stop.view.custom.IfDialog;
import com.viseeointernational.stop.view.custom.RenameDialog;
import com.viseeointernational.stop.view.custom.ScrollViewListView;
import com.viseeointernational.stop.view.custom.SelectDialog;
import com.viseeointernational.stop.view.page.BaseActivity;
import com.viseeointernational.stop.view.page.map.MapActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingActivity extends BaseActivity implements SettingActivityContract.View {

    public static final String KEY_ADDRESS = "address";

    public static final int REQUEST_IMG = 1;
    public static final int REQUEST_CROP = 2;

    @Inject
    SettingActivityContract.Presenter presenter;
    @Inject
    GpsAdapter adapter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.header)
    ImageView header;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.lv)
    ScrollViewListView lv;
    @BindView(R.id.alert_on_off)
    CheckBox alertOnOff;
    @BindView(R.id.time_format)
    TextView timeFormat;
    @BindView(R.id.alert_tune)
    TextView alertTune;
    @BindView(R.id.notification)
    TextView notification;
    @BindView(R.id.date_start)
    TextView dateStart;
    @BindView(R.id.date_end)
    TextView dateEnd;
    @BindView(R.id.g_switch)
    CheckBox gSwitch;
    @BindView(R.id.g_value)
    SeekBar gValue;
    @BindView(R.id.xyz_switch)
    CheckBox xyzSwitch;
    @BindView(R.id.g)
    TextView g;
    @BindView(R.id.default_show)
    TextView defaultShow;

    private RenameDialog renameDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);

        ((App) getApplication()).getAppComponent().settingActivityComponent().activity(this).build().inject(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.title_setting);
        toolbar.setNavigationIcon(R.mipmap.ic_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        lv.setAdapter(adapter);
        adapter.setCallback(new GpsAdapter.Callback() {
            @Override
            public void onMapClick(GpsAdapter adapter, State state) {
                presenter.showMap(state);
            }
        });

        renameDialog = new RenameDialog(this, new RenameDialog.Callback() {
            @Override
            public void onOk(String name) {
                presenter.saveName(name);
            }
        });

        gValue.setOnSeekBarChangeListener(onSeekBarChangeListener);
//        xyzValue.setOnSeekBarChangeListener(onSeekBarChangeListener);

        presenter.takeView(this);
    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch (seekBar.getId()) {
                case R.id.g_value:
                    g.setText((progress + 3) + "");
                    break;
//                case R.id.xyz_value:
//                    xyz.setText((progress + 10) + "");
//                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    protected void onDestroy() {
        presenter.dropView();
        super.onDestroy();
    }

    @Override
    public void showHeader(String imagePath) {
        try {
            Picasso.with(this).load(new File(imagePath)).placeholder(R.mipmap.ic_default_header).into(header);
        } catch (Exception e) {
            e.printStackTrace();
            Picasso.with(this).load(R.mipmap.ic_default_header).into(header);
        }
    }

    @Override
    public void showHeader(Bitmap bitmap) {
        header.setImageBitmap(bitmap);
    }

    @Override
    public void showName(String s) {
        name.setText(s);
    }

    @Override
    public void openTimeFormatSelector(List<String> list) {
        new SelectDialog(this, new SelectDialog.Callback() {
            @Override
            public void onSelect(SelectDialog dialog, int position, String item) {
                presenter.saveTimeFormat(item);
            }
        }).show(list);
    }

    @Override
    public void openAlertTuneSelector(List<String> list) {
        new SelectDialog(this, new SelectDialog.Callback() {
            @Override
            public void onSelect(SelectDialog dialog, int position, String item) {
                presenter.saveAlertTune(position);
            }
        }).show(list);
    }

    @Override
    public void playSound(Uri uri) {
        RingtoneManager.getRingtone(this, uri).play();
    }

    @Override
    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[]{0, 200, 100, 200}, -1);
        }
    }

    @Override
    public void openNotificationSelector(List<String> list) {
        new SelectDialog(this, new SelectDialog.Callback() {
            @Override
            public void onSelect(SelectDialog dialog, int position, String item) {
                presenter.saveNotification(position);
            }
        }).show(list);
    }

    @Override
    public void openDefaultShowSelector(List<String> list) {
        new SelectDialog(this, new SelectDialog.Callback() {
            @Override
            public void onSelect(SelectDialog dialog, int position, String item) {
                presenter.saveDefaultShow(position);
            }
        }).show(list);
    }

    @Override
    public void showDefaultShowType(String s) {
        defaultShow.setText(s);
    }

    @Override
    public void showTimeFormat(String s) {
        timeFormat.setText(s);
    }

    @Override
    public void showAlertTune(String s) {
        alertTune.setText(s);
    }

    @Override
    public void showNotification(String s) {
        notification.setText(s);
    }

    @Override
    public void showAlertEnable(boolean enable) {
        alertOnOff.setOnCheckedChangeListener(null);
        alertOnOff.setChecked(enable);
        alertOnOff.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.alert_on_off:
                    presenter.enableAlert(isChecked);
                    break;
                case R.id.g_switch:
                    presenter.saveTempEnableGAndEYZ(!gSwitch.isChecked(), xyzSwitch.isChecked());
                    if (!isChecked && !xyzSwitch.isChecked()) {
                        showXYZEnable(true);
                        presenter.enableGAndXYZ(false, true);
                    } else {
                        presenter.enableGAndXYZ(gSwitch.isChecked(), xyzSwitch.isChecked());
                    }
                    break;
                case R.id.xyz_switch:
                    presenter.saveTempEnableGAndEYZ(gSwitch.isChecked(), !xyzSwitch.isChecked());
                    if (!isChecked && !gSwitch.isChecked()) {
                        showGEnable(true);
                        presenter.enableGAndXYZ(true, false);
                    } else {
                        presenter.enableGAndXYZ(gSwitch.isChecked(), xyzSwitch.isChecked());
                    }
                    break;
            }
        }
    };

    @Override
    public void showLocation(List<State> list, String format) {
        adapter.setData(list, format);
    }

    @Override
    public void close() {
        onBackPressed();
    }

    @Override
    public void alertIfForceUnpair() {
        new IfDialog(this, new IfDialog.Callback() {
            @Override
            public void onYes(IfDialog dialog) {
                presenter.unpair(true);
            }

            @Override
            public void onNo(IfDialog dialog) {
            }
        }).show(getText(R.string.dialog_force_unpair));
    }

    @Override
    public void sendFile(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
        intent.putExtra(Intent.EXTRA_TEXT, file.getName());
        if (Build.VERSION.SDK_INT >= 24) {
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.FILE_PROVIDER, file);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, getText(R.string.title_send_file)));
        } else {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            startActivity(Intent.createChooser(intent, getText(R.string.title_send_file)));
        }
    }

    @Override
    public void closeRenameDialog() {
        renameDialog.dismiss();
    }

    @Override
    public void showStartCalendar(long baseTime, long currentTime) {
        new DateDialog(this, new DateDialog.Callback() {
            @Override
            public void onSelect(DateDialog dialog, int year, int month, int day) {
                presenter.setStartTime(year, month, day);
            }
        }).show(baseTime, currentTime);
    }

    @Override
    public void showEndCalendar(long baseTime, long currentTime) {
        new DateDialog(this, new DateDialog.Callback() {
            @Override
            public void onSelect(DateDialog dialog, int year, int month, int day) {
                presenter.setEndTime(year, month, day);
            }
        }).show(baseTime, currentTime);
    }

    @Override
    public void showStartTime(String s) {
        dateStart.setText(s);
    }

    @Override
    public void showEndTime(String s) {
        dateEnd.setText(s);
    }

    @Override
    public void showMap(double longitude, double latitude) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(MapActivity.KEY_LONGITUDE, longitude);
        intent.putExtra(MapActivity.KEY_LATITUDE, latitude);
        startActivity(intent);
    }

    @Override
    public void showCropHeader(Uri originHeader, Uri tempImage) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(originHeader, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImage);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(intent, REQUEST_CROP);
    }

    @Override
    public void showGEnable(boolean enable) {
        gSwitch.setOnCheckedChangeListener(null);
        gSwitch.setChecked(enable);
        gSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public void showGValue(int value) {
        gValue.setProgress(value - 3);
        g.setText(value + "");
    }

    @Override
    public void showXYZEnable(boolean enable) {
        xyzSwitch.setOnCheckedChangeListener(null);
        xyzSwitch.setChecked(enable);
        xyzSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

//    @Override
//    public void showXYZValue(int value) {
//        xyzValue.setProgress(value - 10);
//        xyz.setText(value + "");
//    }

    @OnClick({R.id.default_show, R.id.default_show_tag, R.id.sensor_help, R.id.save_g, R.id.header, R.id.rename, R.id.time_format_tag, R.id.alert_tune_tag,
            R.id.notification_tag, R.id.find, R.id.un_pair, R.id.time_format, R.id.alert_tune, R.id.notification, R.id.download, R.id.date_start, R.id.date_end})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.sensor_help:
                // todo 此按钮目前没用到
                break;
            case R.id.default_show:
            case R.id.default_show_tag:
                presenter.showDefaultShowTypeList();
                break;
            case R.id.save_g:
                presenter.saveG(gValue.getProgress() + 3);
                break;
            case R.id.header:
                Matisse.from(this)
                        .choose(MimeType.ofImage())
                        .countable(true)
                        .capture(true)
                        .captureStrategy(new CaptureStrategy(true, BuildConfig.FILE_PROVIDER))
                        .maxSelectable(1)
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        .thumbnailScale(0.7f)
                        .theme(R.style.Matisse)
                        .imageEngine(new PicassoEngine())
                        .forResult(REQUEST_IMG);
                break;
            case R.id.rename:
                renameDialog.show();
                break;
            case R.id.time_format_tag:
            case R.id.time_format:
                presenter.showTimeFormatList();
                break;
            case R.id.alert_tune_tag:
            case R.id.alert_tune:
                presenter.showAlertTuneList();
                break;
            case R.id.notification_tag:
            case R.id.notification:
                presenter.showNotificationTypeList();
                break;
            case R.id.find:
                presenter.find();
                break;
            case R.id.un_pair:
                new IfDialog(this, new IfDialog.Callback() {
                    @Override
                    public void onYes(IfDialog dialog) {
                        presenter.unpair(false);
                    }

                    @Override
                    public void onNo(IfDialog dialog) {
                    }
                }).show(getText(R.string.dialog_unpair_warning));
                break;
            case R.id.download:
                presenter.download();
                break;
            case R.id.date_start:
                presenter.showStartCalendar();
                break;
            case R.id.date_end:
                presenter.showEndCalendar();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.result(requestCode, resultCode, data);
    }
}
