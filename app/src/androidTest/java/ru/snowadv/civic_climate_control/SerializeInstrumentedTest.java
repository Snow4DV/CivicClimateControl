package ru.snowadv.civic_climate_control;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.snowadv.civic_climate_control.Adapter.AdapterService;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SerializeInstrumentedTest {
    @Test
    public void serializedUsbDevice_isDefaultCorrect() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String serialized = appContext.getString(R.string.default_adapter_json);
        String expected="{\"vendorId\":4617,\"deviceId\":16,\"" +
                "productName\":\"Climate Control Adapter\"}";
        Assert.assertEquals(serialized,expected);
    }

    @Test
    public void adapterServiceStarts() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AdapterService service = new AdapterService();
        service.re
        appContext.bindService()
    }
}