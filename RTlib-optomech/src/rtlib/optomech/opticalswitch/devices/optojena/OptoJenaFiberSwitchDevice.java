package rtlib.optomech.opticalswitch.devices.optojena;

import rtlib.core.configuration.MachineConfiguration;
import rtlib.core.variable.types.doublev.DoubleVariable;
import rtlib.optomech.OpticalSwitchDeviceInterface;
import rtlib.optomech.opticalswitch.devices.optojena.adapters.FiberSwitchPositionAdapter;
import rtlib.serial.SerialDevice;

public class OptoJenaFiberSwitchDevice extends SerialDevice	implements
															OpticalSwitchDeviceInterface
{

	private final DoubleVariable mPositionVariable;

	public OptoJenaFiberSwitchDevice(final int pDeviceIndex)
	{
		this(MachineConfiguration.getCurrentMachineConfiguration()
									.getSerialDevicePort(	"fiberswitch.optojena",
															pDeviceIndex,
															"NULL"));
	}

	public OptoJenaFiberSwitchDevice(final String pPortName)
	{
		super("OptoJenaFiberSwitch", pPortName, 76800);

		final FiberSwitchPositionAdapter lFiberSwitchPosition = new FiberSwitchPositionAdapter(this);

		mPositionVariable = addSerialDoubleVariable("OpticalSwitchPosition",
													lFiberSwitchPosition);

	}

	@Override
	public boolean open()
	{
		final boolean lIsOpened = super.open();
		setPosition(0);

		return lIsOpened;
	}

	@Override
	public DoubleVariable getPositionVariable()
	{
		return mPositionVariable;
	}

	@Override
	public int getPosition()
	{
		return (int) mPositionVariable.getValue();
	}

	@Override
	public void setPosition(int pPosition)
	{
		mPositionVariable.set((double) pPosition);
	}

	@Override
	public int[] getValidPositions()
	{
		return new int[]
		{ 1, 2, 3, 4, 5, 6 };
	}

}
