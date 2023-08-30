package org.endlessos.testapp;

import java.io.File;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class KolibriService extends Service {
    private static final String TAG = "KolibriService";

    private final IBinder binder = new KolibriBinder();
    private Python python;
    private PyObject serverBus;
    private String serverUrl;

    public class KolibriBinder extends Binder {
        KolibriService getService() {
            return KolibriService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        python = Python.getInstance();
        PyObject mainModule = python.getModule("main");

        File kolibriHome = new File(this.getFilesDir(), "kolibri");
        mainModule.callAttr("setup", kolibriHome.toString());

        PyObject serverModule = python.getModule("server");
        serverBus = serverModule.callAttr("AppProcessBus", this);

        Log.i(TAG, "Starting Kolibri server");
        serverBus.callAttr("start");
        serverUrl = serverBus.callAttr("get_url").toString();
        Log.d(TAG, "Server started on " + serverUrl);

        Intent intent = new Intent(this, WorkerService.class);
        Log.i(TAG, "Binding Worker service");
        if (!bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to Worker service");
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping Kolibri server");
        serverBus.callAttr("stop");
        Log.d(TAG, "Server stopped");

        Log.i(TAG, "Unbinding Worker service");
        unbindService(connection);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    private ServiceConnection connection = new ServiceConnection() {
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
