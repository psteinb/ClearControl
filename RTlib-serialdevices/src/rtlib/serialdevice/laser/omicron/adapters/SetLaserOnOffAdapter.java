package rtlib.serialdevice.laser.omicron.adapters;

import rtlib.serial.adapters.SerialTextDeviceAdapter;
import rtlib.serialdevice.laser.omicron.adapters.protocol.ProtocolXX;

public class SetLaserOnOffAdapter extends OmicronAdapter implements
																												SerialTextDeviceAdapter
{

	@Override
	public byte[] getGetValueCommandMessage()
	{
		return null;
	}

	@Override
	public Double parseValue(final byte[] pMessage)
	{
		return null;
	}

	@Override
	public byte[] getSetValueCommandMessage(final double pValue)
	{
		return pValue > 0	? ProtocolXX.cSetLaserOnCommand.getBytes()
											: ProtocolXX.cSetLaserOffCommand.getBytes();
	}

	@Override
	public boolean checkAcknowledgementSetValueReturnMessage(final byte[] pMessage)
	{
		return super.checkAcknowledgementSetValueReturnMessage(pMessage);
	}

}