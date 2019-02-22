package com.viseeointernational.stop.view.page.add.connect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.viseeointernational.stop.BuildConfig;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.view.custom.IfDialog;
import com.viseeointernational.stop.view.custom.NotifyDialog;
import com.viseeointernational.stop.view.page.BaseFragment;
import com.viseeointernational.stop.view.page.add.AddActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ConnectFragment extends BaseFragment implements ConnectFragmentContract.View {

    public static final String KEY_ADDRESS = "address";

    private static final int REQUEST_IMG = 1;
    private static final int REQUEST_CROP = 2;
    public static final int REQUEST_BLUETOOTH = 3;

    @BindView(R.id.name)
    EditText name;
    @BindView(R.id.header)
    ImageView header;
    Unbinder unbinder;

    @Inject
    ConnectFragmentContract.Presenter presenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AddActivity) getActivity()).getAddActivityComponent().connectFragmentComponent().fragment(this).build().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_connect, container, false);
        unbinder = ButterKnife.bind(this, root);
        presenter.takeView(this);
        return root;
    }

    @Override
    public void onDestroyView() {
        presenter.dropView();
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void close() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void showName(String s) {
        name.setText(s);
    }

    @Override
    public void showHeader(String imagePath) {
        try {
            Picasso.with(getContext()).load(new File(imagePath)).placeholder(R.mipmap.ic_default_header).into(header);
        } catch (Exception e) {
            e.printStackTrace();
            Picasso.with(getContext()).load(R.mipmap.ic_default_header).into(header);
        }
    }

    @Override
    public void showHeader(Bitmap bitmap) {
        header.setImageBitmap(bitmap);
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
    public void alertIfReconnect() {
        new IfDialog(getContext(), new IfDialog.Callback() {
            @Override
            public void onYes(IfDialog dialog) {
                presenter.saveNameAndConnect(name.getText().toString());
            }

            @Override
            public void onNo(IfDialog dialog) {
                notifyChangeBattery();
            }
        }).show(getText(R.string.dialog_add_reconnect));
    }

    private void notifyChangeBattery() {
        new NotifyDialog(getContext()).show(getText(R.string.dialog_change_battery));
    }

    @Override
    public void showEnableBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_BLUETOOTH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.result(requestCode, resultCode, data);
    }

    @OnClick({R.id.header, R.id.connect, R.id.back})
    public void onViewClicked(View view) {
        switch (view.getId()) {
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
            case R.id.connect:
                presenter.saveNameAndConnect(name.getText().toString());
                break;
            case R.id.back:
                AddActivity activity = (AddActivity) getActivity();
                if (activity != null) {
                    activity.showSearch();
                }
                break;
        }
    }
}
