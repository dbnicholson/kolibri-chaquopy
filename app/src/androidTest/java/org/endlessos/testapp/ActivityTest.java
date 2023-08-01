package org.endlessos.testapp;

import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActivityTest {
    @Test
    public void testLaunch() {
        try(ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertEquals(State.RESUMED, scenario.getState());
        }
    }

    @Test
    public void testStates() {
        try(ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.moveToState(State.RESUMED);
            scenario.moveToState(State.STARTED);
            scenario.moveToState(State.CREATED);
            scenario.moveToState(State.DESTROYED);
        }
    }
}
