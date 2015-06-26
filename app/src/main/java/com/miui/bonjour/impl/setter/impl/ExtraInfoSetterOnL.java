package com.miui.bonjour.impl.setter.impl;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.miui.bonjour.impl.setter.ExtraInfoSetter;

/**
 * Created by ouyang on 15-5-14.
 */
public class ExtraInfoSetterOnL implements ExtraInfoSetter {

    private static final String TAG = "ExtraInfoSetterOnL";

    @Override
    public boolean set(NsdServiceInfo info, Map<String, String> values) {
        boolean ret = false;

        do {
            Class clazz = NsdServiceInfo.class;
            Method method = null;

            try {
                Class<?>[] parameterTypes = new Class<?>[2];
                parameterTypes[0] = String.class;
                parameterTypes[1] = String.class;
                method = clazz.getMethod("setAttribute", parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                break;
            }

            if (method == null) {
                Log.d(TAG, "method not found: setAttribute");
                break;
            }

            method.setAccessible(true);

            for (Map.Entry<String, String> v : values.entrySet()) {
                try {
                    method.invoke(info, v.getKey(), v.getValue());
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