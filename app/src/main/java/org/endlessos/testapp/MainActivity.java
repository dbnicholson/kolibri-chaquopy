package org.endlessos.testapp;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private WebView view;
    private Python python;
    private PyObject serverBus;
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

        python = Python.getInstance();
        PyObject mainModule = python.getModule("main");

        File kolibriHome = new File(this.getFilesDir(), "kolibri");
        mainModule.callAttr("setup", kolibriHome.toString());

        PyObject serverModule = python.getModule("server");
        serverBus = serverModule.callAttr("AppProcessBus", this);

        Log.i(TAG, "Starting Kolibri server");
        serverBus.callAttr("start");
        serverUrl = serverBus.callAttr("get_url").toString();
        Log.d(TAG, "Server ready on " + serverUrl);

        Log.i(TAG, "Loading URL " + serverUrl);
        view.loadUrl(serverUrl);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroying activity");
        Log.i(TAG, "Stopping Kolibri server");
        serverBus.callAttr("stop");
        Log.d(TAG, "Server stopped");
        super.onDestroy();
    }
}
