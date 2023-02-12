package ru.snowadv.civic_climate_control;

import org.junit.Assert;
import org.junit.Test;

public class SerializeUnitTest {

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


}
