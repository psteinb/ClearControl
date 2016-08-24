package clearcontrol.microscope.lightsheet.interactive;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.VariableSetListener;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.device.VirtualDevice;
import clearcontrol.device.change.ChangeListener;
import clearcontrol.device.task.LoopTaskDevice;
import clearcontrol.hardware.cameras.StackCameraDeviceInterface;
import clearcontrol.microscope.MicroscopeInterface;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.LightSheetMicroscopeInterface;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheet;
import clearcontrol.microscope.state.AcquisitionStateInterface;
import clearcontrol.microscope.state.AcquisitionStateManager;

public class InteractiveAcquisition extends LoopTaskDevice
{

	private static final int cRecyclerMinimumNumberOfAvailableStacks = 60;
	private static final int cRecyclerMaximumNumberOfAvailableStacks = 60;
	private static final int cRecyclerMaximumNumberOfLiveStacks = 60;

	private final LightSheetMicroscopeInterface mLightSheetMicroscope;

	private volatile InteractiveAcquisitionModes mCurrentAcquisitionMode = InteractiveAcquisitionModes.None;

	private final BoundedVariable<Double> mExposureVariableInSeconds;
	private final Variable<Boolean> mTriggerOnChangeVariable,
			mUseCurrentAcquisitionStateVariable;

	private volatile boolean mUpdate = true;

	private ChangeListener<VirtualDevice> mChangeListener;

	private Variable<Boolean>[] mActiveCameraVariableArray;

	private AcquisitionStateManager mAcquisitionStateManager;

	@SuppressWarnings("unchecked")
	public InteractiveAcquisition(String pDeviceName,
																LightSheetMicroscope pLightSheetMicroscope,
																AcquisitionStateManager pAcquisitionStateManager)
	{
		super(pDeviceName, 1, TimeUnit.SECONDS);
		mLightSheetMicroscope = pLightSheetMicroscope;
		mAcquisitionStateManager = pAcquisitionStateManager;

		@SuppressWarnings("rawtypes")
		VariableSetListener lListener = (o, n) -> {
			if (!o.equals(n))
				mUpdate = true;
		};

		mExposureVariableInSeconds = new BoundedVariable<Double>(	pDeviceName + "Exposure",
																															0.0,
																															0.0,
																															Double.POSITIVE_INFINITY,
																															0.0);

		mTriggerOnChangeVariable = new Variable<Boolean>(	pDeviceName + "TriggerOnChange",
																											false);

		mUseCurrentAcquisitionStateVariable = new Variable<Boolean>(pDeviceName + "UseCurrentAcquisitionState",
																																false);

		mExposureVariableInSeconds.addSetListener(lListener);
		mTriggerOnChangeVariable.addSetListener(lListener);
		getLoopPeriodVariable().addSetListener(lListener);

		getLoopPeriodVariable().set(1.0);
		getExposureVariable().set(0.010);

		int lNumberOfCameras = getNumberOfCameras();
		mActiveCameraVariableArray = new Variable[lNumberOfCameras];
		for (int c = 0; c < lNumberOfCameras; c++)
		{
			mActiveCameraVariableArray[c] = new Variable<Boolean>("ActiveCamera" + c,
																														true);

			mActiveCameraVariableArray[c].addSetListener(lListener);
		}

		mChangeListener = (o) -> {
			// System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!Received request to update queue!!");
			mUpdate = true;
		};
	}

	@Override
	public boolean open()
	{
		getLightSheetMicroscope().addChangeListener(mChangeListener);
		return super.open();
	}

	@Override
	public boolean close()
	{
		getLightSheetMicroscope().removeChangeListener(mChangeListener);
		return super.close();
	}

	@Override
	public boolean loop()
	{

		try
		{
			try
			{

				if (mUpdate || getLightSheetMicroscope().getQueueLength() == 0)
				{

					if (mCurrentAcquisitionMode == InteractiveAcquisitionModes.Acquisition2D)
					{

						if (getUseCurrentAcquisitionStateVariable().get())
						{
						}
						else
						{

							getLightSheetMicroscope().useRecycler("2DInteractive",
																										cRecyclerMinimumNumberOfAvailableStacks,
																										cRecyclerMaximumNumberOfAvailableStacks,
																										cRecyclerMaximumNumberOfLiveStacks);

							getLightSheetMicroscope().clearQueue();

							for (int c = 0; c < getNumberOfCameras(); c++)
							{
								getLightSheetMicroscope().setC(	c,
																								mActiveCameraVariableArray[c].get());
							}
							getLightSheetMicroscope().setExposure((long) (mExposureVariableInSeconds.get() * 1000000L),
																										TimeUnit.MICROSECONDS);

							for (int l = 0; l < getNumberOfLightsSheets(); l++)
								if (getLightSheetMicroscope().getI(l))
								{
									System.out.println("ACTIVATING LIGHTSHEET " + l);
									getLightSheetMicroscope().getDevice(LightSheet.class,
																											l)
																						.update();
									break;
								}

							getLightSheetMicroscope().addCurrentStateToQueue();

							getLightSheetMicroscope().finalizeQueue();
						}
					}
					else if (mCurrentAcquisitionMode == InteractiveAcquisitionModes.Acquisition3D)
					{
						getLightSheetMicroscope().useRecycler("3DInteractive",
																									cRecyclerMinimumNumberOfAvailableStacks,
																									cRecyclerMaximumNumberOfAvailableStacks,
																									cRecyclerMaximumNumberOfLiveStacks);

						@SuppressWarnings("unchecked")
						AcquisitionStateInterface<LightSheetMicroscopeInterface> lCurrentState = (AcquisitionStateInterface<LightSheetMicroscopeInterface>) mAcquisitionStateManager.getCurrentState();

						lCurrentState.applyState(mLightSheetMicroscope);
					}

				}

				if (getLightSheetMicroscope().getQueueLength() == 0)
				{
					// this leads to a call to stop() which stops the loop
					return false;
				}

				if (mCurrentAcquisitionMode != InteractiveAcquisitionModes.None)
				{
					if (getTriggerOnChangeVariable().get() && !mUpdate)
						return true;

					// play queue
					// System.out.println("Playing Queue...");
					getLightSheetMicroscope().playQueueAndWaitForStacks(10,
																															TimeUnit.SECONDS);
				}

				if (mUpdate)
					mUpdate = false;
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}
		catch (TimeoutException e)
		{
			e.printStackTrace();
		}

		System.out.println("loop");

		return true;
	}

	public void start2DAcquisition()
	{
		System.out.println("Starting 2D Acquisition...");
		mCurrentAcquisitionMode = InteractiveAcquisitionModes.Acquisition2D;
		startTask();
	}

	public void start3DAcquisition()
	{
		System.out.println("Starting 3D Acquisition...");
		mCurrentAcquisitionMode = InteractiveAcquisitionModes.Acquisition2D;
		startTask();
	}

	public void stopAcquisition()
	{
		System.out.println("Stopping Acquisition...");
		mCurrentAcquisitionMode = InteractiveAcquisitionModes.None;
		stopTask();
	}

	public BoundedVariable<Double> getExposureVariable()
	{
		return mExposureVariableInSeconds;
	}

	public Variable<Boolean> getTriggerOnChangeVariable()
	{
		return mTriggerOnChangeVariable;
	}

	public Variable<Boolean> getUseCurrentAcquisitionStateVariable()
	{
		return mUseCurrentAcquisitionStateVariable;
	}

	public LightSheetMicroscopeInterface getLightSheetMicroscope()
	{
		return mLightSheetMicroscope;
	}

	public int getNumberOfCameras()
	{
		int lNumberOfCameras = mLightSheetMicroscope.getNumberOfDevices(StackCameraDeviceInterface.class);
		return lNumberOfCameras;
	}

	private int getNumberOfLightsSheets()
	{
		int lNumberOfLightsSheets = mLightSheetMicroscope.getNumberOfDevices(LightSheet.class);
		return lNumberOfLightsSheets;
	}

	public Variable<Boolean> getActiveCameraVariable(int pCameraIndex)
	{
		return mActiveCameraVariableArray[pCameraIndex];
	}

}