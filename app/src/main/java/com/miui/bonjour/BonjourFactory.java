package com.miui.bonjour;

import android.content.Context;
import android.os.Build;

import com.miui.bonjour.impl.AndroidBonjourImpl;
import com.miui.bonjour.impl.JavaBonjourImpl;


/**
 * Created by ouyang on 2015/5/13.
 */
public class BonjourFactory {

    public static Bonjour create(Context context) {
        Bonjour bonjour = null;

        if (Build.VERSION.SDK_INT >= 19) {
            bonjour = AndroidBonjourImpl.getInstance(context);
        } else {
            bonjour = JavaBonjourImpl.getInstance(context);
        }

        return bonjour;
    }
}
