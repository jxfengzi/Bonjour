package com.miui.bonjour.impl.getter.impl;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.miui.bonjour.impl.getter.ExtraInfoGetter;

/**
 * Created by ouyang on 15-5-15.
 */
public class ExtraInfoGetterOnL implements ExtraInfoGetter {

    private static final String TAG = "ExtraInfoGetterOnL";

    @Override
    public Map<String, String> get(NsdServiceInfo info) {
        Map<String, String> properties = null;

        do {
            Class clazz = info.getClass();
            Method method = null;

            try {
                method = clazz.getMethod("getAttributes");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            if (method == null) {
                Log.d(TAG, "method not found: getAttributes");
                break;
            }

            method.setAccessible(true);

            Object attributes = null;

            try {
                attributes = method.invoke(info);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                break;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                break;
            }

            if (attributes == null) {
                Log.d(TAG, "attributes is null");
                break;
            }

            properties = getAttributes(attributes);
        } while (false);

        return properties;
    }

    private Map<String, String> getAttributes(Object attributes) {
        Map<String, String> properties = null;

        do {
            properties = new HashMap<String, String>();

            Map<String, byte[]> a = (Map<String, byte[]>) attributes;

            for (Map.Entry<String, byte[]> v : a.entrySet()) {
                properties.put(v.getKey(), new String(v.getValue()));
            }
        } while (false);

        return properties;
    }
}
