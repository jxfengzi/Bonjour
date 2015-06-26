package com.miui.bonjour.impl.setter;

import android.net.nsd.NsdServiceInfo;

import java.util.Map;

/**
 * Created by ouyang on 15-5-14.
 */
public interface ExtraInfoSetter {

    boolean set(NsdServiceInfo info, Map<String, String> values);

}
