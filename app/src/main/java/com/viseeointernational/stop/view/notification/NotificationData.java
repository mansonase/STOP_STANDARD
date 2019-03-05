package com.viseeointernational.stop.view.notification;

import android.app.Notification;

import java.util.LinkedList;
import java.util.List;

public class NotificationData {

    private List<Content> list = new LinkedList<>();

    public void clear() {
        synchronized (this) {
            list.clear();
        }
    }

    public void add(Content content) {
        synchronized (this) {
            list.add(content);
        }
    }

    public void remove(int index) {
        synchronized (this) {
            if (list.size() > index) {
                list.remove(index);
            }
        }
    }

    public Content get(int index) {
        synchronized (this) {
            if (list.size() > index) {
                return list.get(index);
            }
            return null;
        }
    }

    public int size() {
        synchronized (this) {
            return list.size();
        }
    }

    public static class Content {
        public int notificationId;
        public Notification notification;
    }
}
