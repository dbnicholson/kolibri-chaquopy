package org.endlessos.testapp;

import android.util.Log;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActivityTest {
    private static final String TAG = "ActivityTest";

    @Test
    public void testLaunch() {
        try(ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertEquals(State.RESUMED, scenario.getState());
        }
    }

    // @Test
    // public void testStates() {
    //     try(ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
    //         Log.i(TAG, "Moving to RESUMED, current state is " + scenario.getState());
    //         scenario.moveToState(State.RESUMED);
    //         Log.i(TAG, "Moving to STARTED, current state is " + scenario.getState());
    //         scenario.moveToState(State.STARTED);
    //         Log.i(TAG, "Moving to CREATED, current state is " + scenario.getState());
    //         scenario.moveToState(State.CREATED);
    //         Log.i(TAG, "Moving to DESTROYED, current state is " + scenario.getState());
    //         scenario.moveToState(State.DESTROYED);
    //     }
    // }
}
