package com.droidkit.core;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import com.droidkit.util.SafeRunnable;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final Random random = new Random();

    public static boolean isUIThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static void showToast(final Activity context, final String text) {
        if(context != null) {
            context.runOnUiThread(new SafeRunnable() {
                @Override
                public void runSafely() {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static int dpToPx(final Context context, final int dp) {
        // Get the screen's density scale
        final float scale = context.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int)((dp * scale) + 0.5f);
    }

    public static final Pattern pattern = Pattern.compile("[0-9]+");

    public static String parseIntToString(String value) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
}
