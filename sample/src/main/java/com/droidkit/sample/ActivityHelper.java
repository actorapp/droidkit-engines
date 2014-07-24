package com.droidkit.sample;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;


public class ActivityHelper {
    public static void tide (Activity a) {
        if (a != null) {
//            a.requestWindowFeature(Window.FEATURE_NO_TITLE);
            a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            a.getWindow().setBackgroundDrawableResource(R.drawable.transparent);
        }
    }
}
