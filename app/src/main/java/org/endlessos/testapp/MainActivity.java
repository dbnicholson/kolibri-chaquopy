package org.endlessos.testapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String TAG = Constants.TAG;

    static final int MSG_SET_SERVER_URL = 1;

    private WebView view;
    private Messenger messenger = new Messenger(new ActivityHandler(Looper.getMainLooper()));

    private Messenger kolibriService;
    private String serverUrl;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Creating activity");

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
        view.loadUrl("file:///android_asset/welcomeScreen/index.html");
        setContentView(view);

        Intent intent = new Intent(this, KolibriService.class);
        Log.i(TAG, "Binding Kolibri service");
        if (!bindService(intent, kolibriConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to Kolibri service");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Starting activity");

        Intent intent = new Intent(this, WorkerService.class);
        Log.i(TAG, "Binding Worker service");
        if (!bindService(intent, workerConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to Worker service");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Stopping activity");
        Log.i(TAG, "Unbinding Worker service");
        unbindService(workerConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying activity");
        Log.i(TAG, "Unbinding Kolibri service");
        unbindService(kolibriConnection);
    }

    public class ActivityHandler extends Handler {
        public ActivityHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_SERVER_URL:
                    Bundle data = msg.getData();
                    if (data == null) {
                        Log.w(TAG, "Received reply with no data");
                        break;
                    }
                    serverUrl = data.getString("serverUrl");
                    if (serverUrl == null) {
                        Log.w(TAG, "Received null server URL");
                        break;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Loading URL " + serverUrl);
                            view.loadUrl(serverUrl);
                        }
                    });
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection kolibriConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder ibinder) {
            Log.d(TAG, "Kolibri service connected");
            kolibriService = new Messenger(ibinder);
            Message msg = Message.obtain(null,
                                         KolibriService.MSG_GET_SERVER_URL,
                                         MainActivity.MSG_SET_SERVER_URL,
                                         0);
            msg.replyTo = messenger;
            try {
                kolibriService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to send message: " + e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Kolibri service disconnected");
            serverUrl = null;
            kolibriService = null;
        }
    };

    private ServiceConnection workerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder ibinder) {
            Log.d(TAG, "Worker service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Worker service disconnected");
        }
    };
}
