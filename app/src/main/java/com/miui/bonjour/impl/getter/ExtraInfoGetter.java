package com.miui.bonjour.impl.getter;

import android.net.nsd.NsdServiceInfo;

import java.util.Map;

/**
 * Created by ouyang on 15-5-15.
 */
public interface ExtraInfoGetter {

    Map<String, String> get(NsdServiceInfo info);
}
