package com.miui.bonjour.impl.getter;

import android.os.Build;
import android.util.Log;

import com.miui.bonjour.impl.getter.impl.ExtraInfoGetterOnKitKat;
import com.miui.bonjour.impl.getter.impl.ExtraInfoGetterOnL;

/**
 * Created by ouyang on 15-5-15.
 */
public class ExtraInfoGetterFactory {

    private static final String TAG = "ExtraInfoGetterFactory";

    public static ExtraInfoGetter create() {
        ExtraInfoGetter getter = null;

        Log.d(TAG, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT == 19 || Build.VERSION.SDK_INT == 20) {
            getter = new ExtraInfoGetterOnKitKat();
        } else if (Build.VERSION.SDK_INT >= 21) {
            getter = new ExtraInfoGetterOnL();
        }

        return getter;
    }
}
