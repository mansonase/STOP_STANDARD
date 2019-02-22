package com.viseeointernational.stop.view.page;

public interface BaseView {

    void showMessage(int id);

    void showMessage(CharSequence text);

    void showLoading();

    void showLoading(CharSequence content);

    void cancelLoading();
}
