package org.endlessos.testapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private WebView view;
    private KolibriService service;
    private String serverUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity");

        view = new WebView(this);
        WebSettings viewSettings = view.getSettings();
        viewSettings.setJavaScriptEnabled(true);
        viewSettings.setDomStorageEnabled(true);
        viewSettings.setAllowFileAccessFromFileURLs(true);
        viewSettings.setAllowUniversalAccessFromFileURLs(true);
        viewSettings.setMediaPlaybackRequiresUserGesture(false);
        view.setWebViewClient(new WebViewClient() {
             @Override
             public boolean shouldOverrideUrlLoading (WebView view,
                                                      WebResourceRequest request) {
                 return false;
             }
        });
        setContentView(view);

        Intent intent = new Intent(this, KolibriService.class);
        Log.i(TAG, "Binding Kolibri service");
        if (!bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to Kolibri service");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Unbinding Kolibri service");
        unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder ibinder) {
            Log.d(TAG, "Kolibri service connected");
            KolibriService.KolibriBinder binder = (KolibriService.KolibriBinder) ibinder;
            service = binder.getService();
            serverUrl = service.getServerUrl();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Loading URL " + serverUrl);
                    view.loadUrl(serverUrl);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Kolibri service disconnected");
            serverUrl = null;
            service = null;
        }
    };
}
