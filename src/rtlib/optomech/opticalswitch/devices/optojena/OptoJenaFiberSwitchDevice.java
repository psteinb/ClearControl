package rtlib.optomech.opticalswitch.devices.optojena;

import rtlib.core.configuration.MachineConfiguration;
import rtlib.core.device.PositionDeviceInterface;
import rtlib.core.variable.types.objectv.ObjectVariable;
import rtlib.optomech.OptoMechDeviceInterface;
import rtlib.optomech.opticalswitch.devices.optojena.adapters.FiberSwitchPositionAdapter;
import rtlib.serial.SerialDevice;

public class OptoJenaFiberSwitchDevice extends SerialDevice	implements
																														PositionDeviceInterface,
																														OptoMechDeviceInterface
{

	private ObjectVariable<Integer> mPositionVariable;

	public OptoJenaFiberSwitchDevice(final int pDeviceIndex)
	{
		this(MachineConfiguration.getCurrentMachineConfiguration()
															.getSerialDevicePort(	"fiberswitch.optojena",
																										pDeviceIndex,
																										"NULL"));

		mPositionVariable = new ObjectVariable<Integer>("SwitchPosition",
																										0);
	}

	public OptoJenaFiberSwitchDevice(final String pPortName)
	{
		super("OptoJenaFiberSwitch", pPortName, 76800);

		final FiberSwitchPositionAdapter lFiberSwitchPosition = new FiberSwitchPositionAdapter(this);

		mPositionVariable = addSerialVariable("OpticalSwitchPosition",
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
	public ObjectVariable<Integer> getPositionVariable()
	{
		return mPositionVariable;
	}

	@Override
	public int getPosition()
	{
		return mPositionVariable.get();
	}

	@Override
	public void setPosition(int pPosition)
	{
		mPositionVariable.set(pPosition);
	}

	@Override
	public int[] getValidPositions()
	{
		return new int[]
		{ 1, 2, 3, 4, 5, 6 };
	}

}
