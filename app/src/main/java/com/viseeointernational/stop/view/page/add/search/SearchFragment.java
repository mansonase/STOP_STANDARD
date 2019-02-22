package com.viseeointernational.stop.view.page.add.search;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.view.adapter.DeviceAdapter;
import com.viseeointernational.stop.view.page.BaseFragment;
import com.viseeointernational.stop.view.page.add.AddActivity;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class SearchFragment extends BaseFragment implements SearchFragmentContract.View {

    public static final int REQUEST_BLUETOOTH = 1;

    @BindView(R.id.searching)
    ImageView searching;
    @BindView(R.id.lv)
    ListView lv;
    Unbinder unbinder;

    @Inject
    SearchFragmentContract.Presenter presenter;

    @Inject
    DeviceAdapter adapter;

    private Animation animation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AddActivity) getActivity()).getAddActivityComponent().searchFragmentComponent().build().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);
        unbinder = ButterKnife.bind(this, root);

        animation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setRepeatCount(-1);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(400);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                presenter.selectDevice(adapter.getData().get(position));
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.takeView(this);
    }

    @Override
    public void onPause() {
        presenter.dropView();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void showEnableBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_BLUETOOTH);
    }

    @Override
    public void showDevices(List<Device> list) {
        adapter.setData(list);
    }

    @Override
    public void showSearching() {
        searching.setVisibility(View.VISIBLE);
        searching.startAnimation(animation);
    }

    @Override
    public void stopSearching() {
        searching.clearAnimation();
        searching.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.result(requestCode, resultCode, data);
    }

    @Override
    public void showConnect(String address) {
        AddActivity activity = (AddActivity) getActivity();
        if (activity != null) {
            activity.showConnect(address);
        }
    }

    @OnClick({R.id.rescan, R.id.cancel})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rescan:
                presenter.search();
                break;
            case R.id.cancel:
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
                break;
        }
    }
}
