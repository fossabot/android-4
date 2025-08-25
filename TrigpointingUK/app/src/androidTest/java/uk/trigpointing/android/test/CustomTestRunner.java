package uk.trigpointing.android.test;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.test.runner.AndroidJUnitRunner;

/**
 * Custom test runner that disables Firebase during testing
 * This prevents Firebase protobuf crashes on Android API < 30
 */
public class CustomTestRunner extends AndroidJUnitRunner {
    private static final String TAG = "CustomTestRunner";

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        
        Log.i(TAG, "Creating test application without Firebase");
        return super.newApplication(cl, TestApplication.class.getName(), context);
    }
}
