package com.miui.bonjour.impl.setter.impl;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.miui.bonjour.impl.setter.ExtraInfoSetter;

/**
 * Created by ouyang on 15-5-14.
 */
public class ExtraInfoSetterOnKitKat implements ExtraInfoSetter {

    private static final String TAG = "ExtraInfoSetterOnKitKat";

    @Override
    public boolean set(NsdServiceInfo info, Map<String, String> values) {
        boolean ret = false;

        do {
            Class<?> clazzDnsSdTxtRecord = null;
            Class clazz = NsdServiceInfo.class;
            Method method = null;

            try {
                clazzDnsSdTxtRecord = Class.forName("android.net.nsd.DnsSdTxtRecord");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }

            try {
                Class<?>[] parameterTypes = new Class<?>[1];
                parameterTypes[0] = clazzDnsSdTxtRecord;
                method = clazz.getMethod("setTxtRecord", parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            if (method == null) {
                Log.d(TAG, "method not found: setTxtRecord");
                break;
            }

            method.setAccessible(true);

            Object t = createDnsSdTxtRecord();
            if (t != null) {
                setDnsSdTxtRecord(t, values);
            }

            Log.d(TAG, "txtRecord: " + t.toString());

            try {
                method.invoke(info, t);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            ret = true;
        } while (false);

        return ret;
    }

    private Object createDnsSdTxtRecord() {
        Object value = null;

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

            for (Constructor<?> c : clazz.getConstructors()) {
                if (c.getTypeParameters().length == 0) {

                    try {
                        value = clazz.newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }
        } while (false);

        return value;
    }

    private boolean setDnsSdTxtRecord(Object txtRecord, Map<String, String> values) {
        boolean ret = false;

        do {
            Class c = txtRecord.getClass();
            Method method = null;

            try {
                Class<?>[] parameterTypes = new Class<?>[2];
                parameterTypes[0] = String.class;
                parameterTypes[1] = String.class;
                method = c.getMethod("set", parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            if (method == null) {
                Log.d(TAG, "method not found: set");
                break;
            }

            method.setAccessible(true);
            for (Map.Entry<String, String> v : values.entrySet()) {
                try {
                    method.invoke(txtRecord, v.getKey(), v.getValue());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            ret = true;
        } while (false);

        return ret;
    }
}
