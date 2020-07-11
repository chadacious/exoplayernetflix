package com.example.exoplayer;

import android.media.MediaDrm;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class MediaDrmUtils {

    private static final String TAG = "MediaDrmUtils";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static int getWidevineSecurityLevel(MediaDrm mediaDrm) {
        Log.d(TAG, "getWidevineSecurityLevel() called with: mediaDrm = [" + mediaDrm + "]");
        String propertyString = mediaDrm.getPropertyString("securityLevel");
        if (propertyString.equals("L1")) {
            return 1;
        }
        return propertyString.equals("L3") ? 3 : -1;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void setAppId(MediaDrm mediaDrm) {
        Log.d(TAG, "setAppId() called with: mediaDrm = [" + mediaDrm + "]");
        try {
            mediaDrm.setPropertyString("appId", "com.netflix.mediaclient");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void setSecurityLevelL3(MediaDrm mediaDrm) {
        Log.d(TAG, "setSecurityLevelL3() called with: mediaDrm = [" + mediaDrm + "]");
        try {
            mediaDrm.setPropertyString("securityLevel", "L3");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static byte[] getDeviceId(MediaDrm mediaDrm) {
        return mediaDrm.getPropertyByteArray("deviceUniqueId");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getDeviceType(MediaDrm mediaDrm) {
        return mediaDrm.getPropertyString("systemId");
    }
}
