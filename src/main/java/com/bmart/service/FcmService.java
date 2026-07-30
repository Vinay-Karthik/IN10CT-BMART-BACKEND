package com.bmart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmService {

    /**
     * Firebase Cloud Messaging (FCM) Push Notification Integration Handler.
     * To activate live FCM push notifications:
     * 1. Add `com.google.firebase:firebase-admin` dependency to pom.xml
     * 2. Initialize FirebaseApp with serviceAccountKey.json credentials
     * 3. Dispatch Message.builder() via FirebaseMessaging.getInstance().send(message)
     */
    public void sendPushNotification(String fcmDeviceToken, String title, String body) {
        log.info("[FCM Push Notification Simulated] Token: {}, Title: {}, Body: {}", fcmDeviceToken, title, body);
    }
}
