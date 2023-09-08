package org.endlessos.testapp;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class KolibriService extends Service {
    private static final String TAG = Constants.TAG;

    static final int MSG_GET_SERVER_URL = 1;

    private Messenger messenger = new Messenger(new KolibriHandler(Looper.getMainLooper()));
    private Python python;
    private PyObject serverBus;
    private String serverUrl;

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        python = Python.getInstance();

        try {
            KolibriUtils.setupKolibri(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to setup Kolibri: " + e.toString());
            return;
        }

        PyObject serverModule = python.getModule("testapp.server");
        serverBus = serverModule.callAttr("AppProcessBus", this);

        Log.i(TAG, "Starting Kolibri server");
        serverBus.callAttr("start");
        serverUrl = serverBus.callAttr("get_url").toString();
        Log.d(TAG, "Server started on " + serverUrl);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping Kolibri server");
        serverBus.callAttr("stop");
        Log.d(TAG, "Server stopped");
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public class KolibriHandler extends Handler {
        public KolibriHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_SERVER_URL:
                    Bundle data = new Bundle();
                    data.putString("serverUrl", serverUrl);
                    Message reply = Message.obtain(null, msg.arg1);
                    reply.setData(data);
                    try {
                        msg.replyTo.send(reply);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to send message: " + e.toString());
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
