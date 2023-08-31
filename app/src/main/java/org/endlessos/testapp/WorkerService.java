package org.endlessos.testapp;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class WorkerService extends Service {
    private static final String TAG = Constants.TAG;

    private final IBinder binder = new WorkerBinder();
    private Python python;
    private PyObject workerBus;

    public class WorkerBinder extends Binder {
        WorkerService getService() {
            return WorkerService.this;
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

        PyObject workerModule = python.getModule("worker");
        workerBus = workerModule.callAttr("WorkerProcessBus", this);

        Log.i(TAG, "Starting Kolibri worker");
        workerBus.callAttr("start");
        Log.d(TAG, "Worker started");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping Kolibri worker");
        workerBus.callAttr("stop");
        Log.d(TAG, "Worker stopped");
    }
}
