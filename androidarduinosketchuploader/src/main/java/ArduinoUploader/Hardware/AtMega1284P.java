package ArduinoUploader.Hardware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ArduinoUploader.Hardware.Memory.EepromMemory;
import ArduinoUploader.Hardware.Memory.FlashMemory;
import ArduinoUploader.Hardware.Memory.IMemory;

public class AtMega1284P extends Mcu {

    @Override
    public byte getDeviceCode() {

        return (byte) 0x82;
    }

    @Override
    public byte getDeviceRevision() {
        return (byte)0;
    }

    @Override
    public byte getLockBytes() {
        return (byte)1;
    }

    @Override
    public byte getFuseBytes() {
        return (byte)3;
    }

    @Override
    public byte getTimeout() {

        return (byte) 200;
    }

    @Override
    public byte getStabDelay() {
        return (byte)100;
    }

    @Override
    public byte getCmdExeDelay() {
        return (byte)25;
    }

    @Override
    public byte getSynchLoops() {
        return (byte)32;
    }

    @Override
    public byte getByteDelay() {
        return (byte)0;
    }

    @Override
    public byte getPollIndex() {
        return (byte)3;
    }

    @Override
    public byte getPollValue() {
        return (byte)0x53;
    }

    @Override
    public String getDeviceSignature() {
        return "1E-97-05";
    }

    @Override
    public Map<Command, byte[]> getCommandBytes() {
// return new Dictionary<Command, byte[]> { {Command.PgmEnable, new byte[] {0xac, 0x53, 0x00, 0x00}} };
        return new HashMap<Command, byte[]>() {
            {
                put(Command.PgmEnable, new byte[]{(byte) 0xac, 0x53, 0x00, 0x00});
            }
        };
    }

    @Override
    public List<IMemory> getMemory() {
        FlashMemory tempVar = new FlashMemory();
        tempVar.setSize(128 * 1024);
        tempVar.setPageSize(256);
        tempVar.setPollVal1((byte) 0xFF);
        tempVar.setPollVal2((byte) 0xFF);
        tempVar.setDelay((byte) 10);

        tempVar.setCmdBytesRead(new byte[]{0x20, 0x00, 0x00});

        tempVar.setCmdBytesWrite(new byte[]{0x40, 0x4c, 0x00});
        EepromMemory tempVar2 = new EepromMemory();
        tempVar2.setSize(4 * 1024);
        tempVar2.setPollVal1((byte) 0xFF);
        tempVar2.setPollVal2((byte) 0xFF);
        tempVar2.setDelay((byte) 10);

        tempVar2.setCmdBytesRead(new byte[]{(byte) 0xa0, (byte)0x00, (byte)0x00});

        tempVar2.setCmdBytesWrite(new byte[]{(byte) 0xc1, (byte) 0xc2, (byte)0x00});
        return new ArrayList<IMemory>(Arrays.asList(tempVar, tempVar2));
    }
}