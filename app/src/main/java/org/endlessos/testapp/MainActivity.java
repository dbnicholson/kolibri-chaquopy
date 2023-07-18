package org.endlessos.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView view = new WebView(this);
        setContentView(view);
        view.loadUrl("https://google.com");
    }
}
