package rtlib.stages.devices.smc100.adapters;

import rtlib.serial.adapters.SerialDeviceAdapterAdapter;
import rtlib.serial.adapters.SerialTextDeviceAdapter;

public class SMC100EnableAdapter extends
																SerialDeviceAdapterAdapter<Boolean>	implements
																																		SerialTextDeviceAdapter<Boolean>
{

	@Override
	public byte[] getGetValueCommandMessage()
	{
		return null;
	}

	@Override
	public Boolean parseValue(byte[] pMessage)
	{
		return false;
	}

	@Override
	public long getGetValueReturnWaitTimeInMilliseconds()
	{
		return 0;
	}

	@Override
	public byte[] getSetValueCommandMessage(Boolean pOldValue,
																					Boolean pNewValue)
	{
		if (!pOldValue == false && pNewValue == true)
			return SMC100Protocol.cEnableCommand.getBytes();
		else
			return null;
	}

	@Override
	public long getSetValueReturnWaitTimeInMilliseconds()
	{
		return SMC100Protocol.cWaitTimeInMilliSeconds;
	}

	@Override
	public boolean checkAcknowledgementSetValueReturnMessage(byte[] pMessage)
	{
		return true;
	}

	@Override
	public Character getGetValueReturnMessageTerminationCharacter()
	{
		return SMC100Protocol.cMessageTerminationCharacter;
	}

	@Override
	public Character getSetValueReturnMessageTerminationCharacter()
	{
		return SMC100Protocol.cMessageTerminationCharacter;
	}

	@Override
	public boolean hasResponseForSet()
	{
		return false;
	}

	@Override
	public boolean hasResponseForGet()
	{
		return false;
	}

}
