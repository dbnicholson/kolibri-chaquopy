package org.endlessos.testapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class PythonTest {
    @Test
    public void testPlatform() {
        Python python = Python.getInstance();
        PyObject sys = python.getModule("sys");
        String sysPlatform = sys.get("platform").toString();
        int sysApiLevel = sys.callAttr("getandroidapilevel").toInt();
        PyObject sysconfig = python.getModule("sysconfig");
        int sysconfigApiLevel = sysconfig.callAttr("get_config_var", "ANDROID_API_LEVEL").toInt();
        PyObject platform = python.getModule("platform");
        String release = platform.callAttr("release").toString();
        assertEquals("linux", sysPlatform);
        assertEquals(21, sysApiLevel);
        assertEquals(21, sysconfigApiLevel);
        assertTrue(release.contains("android"));
    }
}
