package com.example.voiceassistant.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationRepository {
    private static NotificationRepository instance;
    private final List<NotificationItem> unreadNotifications = new CopyOnWriteArrayList<>();

    private NotificationRepository() {
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    public void addNotification(String appName, String sender, String content) {
        unreadNotifications.add(new NotificationItem(appName, sender, content));
    }

    public List<NotificationItem> getUnreadNotifications() {
        return new ArrayList<>(unreadNotifications);
    }

    public void clearNotifications() {
        unreadNotifications.clear();
    }

    public static class NotificationItem {
        private final String appName;
        private final String sender;
        private final String content;

        public NotificationItem(String appName, String sender, String content) {
            this.appName = appName;
            this.sender = sender;
            this.content = content;
        }

        public String getAppName() {
            return appName;
        }

        public String getSender() {
            return sender;
        }

        public String getContent() {
            return content;
        }
    }
}
