package com.droidkit.sample;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;

import com.droidkit.core.Logger;
import com.droidkit.util.SafeRunnable;


public class BaseActivity extends ActionBarActivity {

    protected static final Handler handler = new Handler(Looper.getMainLooper());

    protected final Object progressDialogSync = new Object();
    protected volatile ProgressDialog progressDialog;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.tide(this);

        // ...
    }

    public void showProgressDialog(final String title, final String text) {
        runOnUiThread(new SafeRunnable() {
            @Override
            public void runSafely() {
                synchronized (progressDialogSync) {
                    if(progressDialog == null) {
                        progressDialog = new ProgressDialog(BaseActivity.this);
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(true);
                    }
                    if(!TextUtils.isEmpty(title)) {
                        progressDialog.setTitle(title);
                    } else {
                        progressDialog.setTitle("");
                    }

                    if(!TextUtils.isEmpty(text)) {
                        progressDialog.setMessage(text);
                    } else {
                        progressDialog.setMessage("");
                    }
                    progressDialog.show();
                }
            }
        });
    }

    public void hideProgressDialog() {
        runOnUiThread(new SafeRunnable() {
            @Override
            public void runSafely() {
                if(progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (final IllegalArgumentException e) {
                        // Sometimes we may get "View not attached to window manager"
                        Logger.e(e);
                    }
                }
            }
        });
    }

}