package ru.snowadv.civic_climate_control;

import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Test;

import ru.snowadv.civic_climate_control.adapter.AdapterState;

public class BasicUnitTest {

    Gson gson = new Gson();
    @Test
    public void serializedUsbDevice_isCorrect() throws Exception {
        String serialized =
                new SerializableUsbDevice(0x1209,0x0010,
                        "Climate Control Adapter")
                        .toJson();
        String expected="{\"vendorId\":4617,\"deviceId\":16,\"" +
                "productName\":\"Climate Control Adapter\"}";
        Assert.assertEquals(serialized,expected);
    }

    @Test
    public void test_serializeAdapterState() {
        AdapterState state = new AdapterState(0, 15,
                20, 0,0, true);

        Assert.assertEquals("{\"fanLevel\":\"LEVEL_1\",\"tempLeft\":15,\"tempRight\":20," +
                "\"fanDirection\":\"DOWN_WINDSHIELD\",\"ac\":false,\"auto\":true}", gson.toJson(state));
    }

    @Test
    public void test_deserializeAdapterState() {
        String stateJson = "{\"fanLevel\":\"LEVEL_1\",\"tempLeft\":15,\"tempRight\":20," +
                "\"fanDirection\":\"DOWN_WINDSHIELD\",\"ac\":false,\"auto\":true}";
        gson.fromJson(stateJson, AdapterState.class);
    }


}
