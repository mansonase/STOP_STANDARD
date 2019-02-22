package com.viseeointernational.stop.view.page;

import android.app.Activity;
import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment implements BaseView {

    @Override
    public void showMessage(int id) {
        if (getBaseActivity() != null) {
            getBaseActivity().showMessage(id);
        }
    }

    @Override
    public void showMessage(CharSequence text) {
        if (getBaseActivity() != null) {
            getBaseActivity().showMessage(text);
        }
    }

    @Override
    public void showLoading() {
        if (getBaseActivity() != null) {
            getBaseActivity().showLoading();
        }
    }

    @Override
    public void showLoading(CharSequence content) {
        if (getBaseActivity() != null) {
            getBaseActivity().showLoading(content);
        }
    }

    @Override
    public void cancelLoading() {
        if (getBaseActivity() != null) {
            getBaseActivity().cancelLoading();
        }
    }

    private BaseActivity getBaseActivity() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            return (BaseActivity) activity;
        }
        return null;
    }

}
