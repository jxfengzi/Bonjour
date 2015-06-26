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
public class ExtraInfoGetterOnKitKat implements ExtraInfoGetter {

    private static final String TAG = "ExtraInfoGetterOnKitKat";

    @Override
    public Map<String, String> get(NsdServiceInfo info) {
        Map<String, String> properties = null;

        do {
            Class clazz = NsdServiceInfo.class;
            Method method = null;

            try {
                method = clazz.getMethod("getTxtRecord");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            if (method == null) {
                Log.d(TAG, "method not found: getTxtRecord");
                break;
            }

            method.setAccessible(true);

            Object txtRecord = null;

            try {
                txtRecord = method.invoke(info);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (txtRecord == null) {
                Log.d(TAG, "txtRecord is null");
                break;
            }

            properties = getDnsSdTxtRecord(txtRecord);
        } while (false);

        return properties;
    }

    private Map<String, String> getDnsSdTxtRecord(Object txtRecord) {
        Map<String, String> properties = null;

        do {
            Class<?> clazz = null;
            try {
                clazz = Class.forName("android.net.nsd.DnsSdTxtRecord");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }

            if (clazz == null) {
                Log.d(TAG, "class not found: android.net.nsd.DnsSdTxtRecord");
                break;
            }

            Method methodSize = null;
            Method methodGetValueAsString = null;

            try {
                methodSize = clazz.getMethod("size");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            try {
                Class<?>[] parameterTypes = new Class<?>[1];
                parameterTypes[0] = Integer.class;
                methodGetValueAsString = clazz.getMethod("getValueAsString", parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            if (methodSize == null) {
                Log.d(TAG, "method not found: size");
                break;
            }

            if (methodGetValueAsString == null) {
                Log.d(TAG, "method not found: getValueAsString");
                break;
            }

            methodSize.setAccessible(true);
            methodGetValueAsString.setAccessible(true);

            Object size = null;

            try {
                size = methodSize.invoke(txtRecord);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                break;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                break;
            }

            if (size == null) {
                Log.d(TAG, "size is null");
                break;
            }

            if (!size.getClass().isInstance(Integer.class)) {
                Log.d(TAG, "size is not integer");
                break;
            }

            properties = new HashMap<String, String>();

            int intSize = (Integer) size;
            for (int i = 0; i < intSize; ++i) {
                try {
                    Object value = methodGetValueAsString.invoke(txtRecord, i);
                    if (value != null) {
                        Log.d(TAG, "value: " + value.toString());
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    break;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } while (false);

        return properties;
    }
}
