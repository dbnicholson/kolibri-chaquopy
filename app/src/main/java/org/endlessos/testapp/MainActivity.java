package org.endlessos.testapp;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private WebView view;
    private Python python;
    private PyObject mainModule;
    private Thread serverThread;
    private String serverUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity");

        view = new WebView(this);
        setContentView(view);

        python = Python.getInstance();
        mainModule = python.getModule("main");

        File kolibriHome = new File(this.getFilesDir(), "kolibri");
        mainModule.callAttr("setup", kolibriHome.toString());

        serverThread = new Thread(new ServerThread(this), "ServerThread");
        serverThread.start();
    }

    public void loadUrl(String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Loading URL " + url);
                view.loadUrl(url);
            }
        });
    }

    class ServerThread implements Runnable {
        private MainActivity activity;

        public ServerThread(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            Log.i(TAG, "Starting Kolibri server");
            mainModule.callAttr("start", activity);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying activity");
        Log.i(TAG, "Stopping Kolibri server");
        mainModule.callAttr("stop");
    }
}
