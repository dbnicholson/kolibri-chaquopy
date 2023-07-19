package org.endlessos.testapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class PythonTest {
    @Test
    public void testSomething() {
        Python python = Python.getInstance();
        PyObject main = python.getModule("main");
        PyObject out = main.callAttr("test", "test");
        assertEquals(out.toString(), "received \"test\"");
    }
}
