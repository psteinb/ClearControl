package rtlib.core.device;

import rtlib.core.variable.booleanv.BooleanVariable;
import rtlib.core.variable.doublev.DoubleVariable;

public interface LaserDevice extends
														NamedDeviceInterface,
														VirtualDeviceInterface
{

	public int getWavelengthInNanoMeter();

	public void setTargetPowerInMilliWatt(double pTargetPowerinMilliWatt);

	public void setTargetPowerInPercent(double pTargetPowerInPercent);

	public double getTargetPowerInMilliWatt();

	public double getMaxPowerInMilliWatt();

	public double getCurrentPowerInMilliWatt();

	public BooleanVariable getLaserOnVariable();

	public DoubleVariable getTargetPowerInMilliWattVariable();

	public DoubleVariable getCurrentPowerInMilliWattVariable();

	public DoubleVariable getWavelengthInNanoMeterVariable();

}