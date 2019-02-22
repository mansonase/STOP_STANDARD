package com.viseeointernational.stop.view.page;

public interface BasePresenter<T> {

    void takeView(T view);

    void dropView();
}
