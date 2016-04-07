package rtlib.lasers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rtlib.core.device.NamedVirtualDevice;
import rtlib.core.variable.types.objectv.ObjectVariable;

public class LaserDeviceBase extends NamedVirtualDevice	implements
																												LaserDeviceInterface
{

	private final ScheduledExecutorService mScheduledExecutorService = Executors.newScheduledThreadPool(1);

	protected ObjectVariable<Double> mSpecInMilliWattPowerVariable,
			mMaxPowerInMilliWattVariable, mTargetPowerInMilliWattVariable,
			mCurrentPowerInMilliWattVariable;
	protected ObjectVariable<Integer> mWorkingHoursVariable,
			mSetOperatingModeVariable, mDeviceIdVariable,
			mWavelengthVariable;
	protected ObjectVariable<Boolean> mPowerOnVariable,
			mLaserOnVariable;
	private Runnable mCurrentPowerPoller;

	private ScheduledFuture<?> mCurrentPowerPollerScheduledFutur;

	public LaserDeviceBase(final String pDeviceName)
	{
		super(pDeviceName);
	}

	@Override
	public boolean open()
	{
		boolean lOpen;
		try
		{
			lOpen = super.open();

			return lOpen;
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean start()
	{
		try
		{

			setTargetPowerInPercent(0);
			setPowerOn(true);

			mCurrentPowerPoller = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						final double lNewPowerValue = mCurrentPowerInMilliWattVariable.get();
						mCurrentPowerInMilliWattVariable.sync(lNewPowerValue,
																									true);
					}
					catch (final Throwable e)
					{
						e.printStackTrace();
					}
				}
			};
			mCurrentPowerPollerScheduledFutur = mScheduledExecutorService.scheduleAtFixedRate(mCurrentPowerPoller,
																																												1,
																																												300,
																																												TimeUnit.MILLISECONDS);

			setLaserOn(true);
			return true;
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean stop()
	{
		try
		{
			setLaserOn(false);
			mCurrentPowerPollerScheduledFutur.cancel(true);
			return true;
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean close()
	{
		try
		{
			setTargetPowerInPercent(0);
			setLaserOn(false);
			setPowerOn(false);
			return super.close();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public final ObjectVariable<Integer> getDeviceIdVariable()
	{
		return mDeviceIdVariable;
	}

	public final int getDeviceId()
	{
		return mDeviceIdVariable.get();
	}

	@Override
	public final ObjectVariable<Integer> getWavelengthInNanoMeterVariable()
	{
		return mWavelengthVariable;
	}

	@Override
	public final int getWavelengthInNanoMeter()
	{
		return mWavelengthVariable.get();
	}

	public final ObjectVariable<Double> getSpecPowerVariable()
	{
		return mSpecInMilliWattPowerVariable;
	}

	public final double getSpecPowerInMilliWatt()
	{
		return mSpecInMilliWattPowerVariable.get();
	}

	public final ObjectVariable<Integer> getWorkingHoursVariable()
	{
		return mWorkingHoursVariable;
	}

	public final int getWorkingHours()
	{
		return mWorkingHoursVariable.get();
	}

	public final ObjectVariable<Double> getMaxPowerVariable()
	{
		return mMaxPowerInMilliWattVariable;
	}

	@Override
	public final double getMaxPowerInMilliWatt()
	{
		return mMaxPowerInMilliWattVariable.get();
	}

	public final ObjectVariable<Integer> getOperatingModeVariable()
	{
		return mSetOperatingModeVariable;
	}

	public final void setOperatingMode(final int pMode)
	{
		mSetOperatingModeVariable.set(pMode);
	}

	public final ObjectVariable<Boolean> getPowerOnVariable()
	{
		return mPowerOnVariable;
	}

	public final void setPowerOn(final boolean pState)
	{
		mPowerOnVariable.set(pState);
	}

	@Override
	public final ObjectVariable<Boolean> getLaserOnVariable()
	{
		return mLaserOnVariable;
	}

	public final void setLaserOn(final boolean pState)
	{
		mLaserOnVariable.set(pState);
	}

	public final double getTargetPowerInPercent()
	{
		return mTargetPowerInMilliWattVariable.get() / getMaxPowerInMilliWatt();
	}

	@Override
	public final void setTargetPowerInPercent(final double pPowerInPercent)
	{
		final double lPowerInMilliWatt = pPowerInPercent * getMaxPowerInMilliWatt();
		mTargetPowerInMilliWattVariable.set(lPowerInMilliWatt);
	}

	@Override
	public final double getTargetPowerInMilliWatt()
	{
		return mTargetPowerInMilliWattVariable.get();
	}

	@Override
	public final void setTargetPowerInMilliWatt(final double pPowerInMilliWatt)
	{
		mTargetPowerInMilliWattVariable.set(pPowerInMilliWatt);
	}

	@Override
	public final ObjectVariable<Double> getTargetPowerInMilliWattVariable()
	{
		return mTargetPowerInMilliWattVariable;
	}

	@Override
	public final ObjectVariable<Double> getCurrentPowerInMilliWattVariable()
	{
		return mCurrentPowerInMilliWattVariable;
	}

	@Override
	public final double getCurrentPowerInMilliWatt()
	{
		return mCurrentPowerInMilliWattVariable.get();
	}

	public final double getCurrentPowerInPercent()
	{
		return getCurrentPowerInMilliWatt() / getMaxPowerInMilliWatt();
	}

	@Override
	public String toString()
	{
		return String.format(	"LaserDeviceBase [mDeviceIdVariable=%d, mWavelengthVariable=%d, mMaxPowerVariable=%g]",
													(int) mDeviceIdVariable.get(),
													(int) mWavelengthVariable.get(),
													mMaxPowerInMilliWattVariable.get());
	}

}
