package ru.parking.localservice_realm;

import android.os.HandlerThread;


public class RealmThread extends HandlerThread {

    public static final String DEFAULT_REALM_THREAD_NAME = "RealmThread";

    public RealmThread() {
        super(DEFAULT_REALM_THREAD_NAME);
    }

    public RealmThread(String name) {
        super(name);
    }
}
