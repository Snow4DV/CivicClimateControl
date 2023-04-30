package ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import ArduinoUploader.*;
import ArduinoUploader.BootloaderProgrammers.*;
import ArduinoUploader.BootloaderProgrammers.Protocols.*;
import ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.*;

public class ReturnSupportedDeviceCodesRequest extends Request
{
	public ReturnSupportedDeviceCodesRequest()
	{

		setBytes(new byte[] {Constants.CmdReturnSupportedDeviceCodes});
	}
}