package org.endlessos.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView view = new WebView(this);
        setContentView(view);
        view.loadUrl("https://google.com");

        Python python = Python.getInstance();
        PyObject main = python.getModule("main");
        PyObject out = main.callAttr("test", "test");
        Log.i(TAG, out.toString());
    }
}
