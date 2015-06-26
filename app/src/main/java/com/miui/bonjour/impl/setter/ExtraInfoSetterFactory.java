package com.miui.bonjour.impl.setter;

import android.os.Build;
import android.util.Log;

import com.miui.bonjour.impl.setter.impl.ExtraInfoSetterOnKitKat;
import com.miui.bonjour.impl.setter.impl.ExtraInfoSetterOnL;

/**
 * Created by ouyang on 15-5-14.
 */
public class ExtraInfoSetterFactory {

    private static final String TAG = "ExtraInfoSetterFactory";

    public static ExtraInfoSetter create() {
        ExtraInfoSetter setter = null;

        Log.d(TAG, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT == 19 || Build.VERSION.SDK_INT == 20) {
            setter = new ExtraInfoSetterOnKitKat();
        } else if (Build.VERSION.SDK_INT >= 21) {
            setter = new ExtraInfoSetterOnL();
        }

        return setter;
    }
}