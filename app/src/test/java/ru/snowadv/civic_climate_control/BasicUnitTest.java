package ru.snowadv.civic_climate_control;

import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import ru.snowadv.civic_climate_control.Adapter.AdapterState;

public class CastAndSerializationTest {

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
    public void test_castEnumToInt() {
        String fanDirEnums = Arrays.toString(AdapterState.FanDirection.values());
        Assert.assertEquals("[UP, DOWN, UP_DOWN, DOWN_WINDSHIELD, WINDSHIELD]", fanDirEnums);
        System.out.println(fanDirEnums);
    }

    @Test
    public void test_serializeAdapterState() {
        AdapterState state = new AdapterState(AdapterState.FanLevel.LEVEL_1, 15, 20, 0,false, true);
        Gson gson = new Gson();
        System.out.println(gson.toJson(state));
    }

    @Test
    public void test_deserializeAdapterState() {

    }


}
