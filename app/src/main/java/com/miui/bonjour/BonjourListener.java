package com.miui.bonjour;

import com.miui.bonjour.serviceinfo.BonjourServiceInfo;

/**
 * Created by ouyang on 15-5-14.
 */
public interface BonjourListener {

    void onStarted();

    void onStartFailed();

    void onStopped();

    void onServiceFound(BonjourServiceInfo serviceInfo);

    void onServiceLost(BonjourServiceInfo serviceInfo);
}
