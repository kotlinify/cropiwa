package com.steelkiwi.cropiwa.sample.util;

import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * Created by yarolegovich https://github.com/yarolegovich
 * on 22.03.2017.
 */

public class Permissions {

    public static boolean isGranted(AppCompatActivity activity, String permission) {
        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

}
