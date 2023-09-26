package org.endlessos.testapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import android.content.Context;
import android.util.Log;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class KolibriUtils {
    private static final String TAG = Constants.TAG;

    private static boolean kolibriInitialized = false;

    public static File getKolibriHome(Context context) {
        return new File(context.getFilesDir(), "kolibri");
    }

    public static File getSetupLockFile(Context context) {
        return new File(context.getFilesDir(), "setup.lock");
    }

    public static synchronized void setupKolibri(Context context) throws IOException {
        if (kolibriInitialized) {
            Log.d(TAG, "Skipping Kolibri setup");
        }

        final File lockFile = getSetupLockFile(context);
        Log.i(TAG, "Acquiring Kolibri setup lock " + lockFile.toString());
        try (
            final FileOutputStream lockStream = new FileOutputStream(lockFile);
            final FileChannel lockChannel = lockStream.getChannel();
            final FileLock lock = lockChannel.lock();
        ) {
            final String kolibriHome = getKolibriHome(context).toString();
            Python python = Python.getInstance();
            PyObject mainModule = python.getModule("testapp.main");
            Log.i(TAG, "Setting up Kolibri in " + kolibriHome);
            mainModule.callAttr("setup", kolibriHome);
            kolibriInitialized = true;
        }
    }
}
