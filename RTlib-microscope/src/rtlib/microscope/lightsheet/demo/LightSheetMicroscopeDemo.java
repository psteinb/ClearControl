package rtlib.microscope.lightsheet.demo;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.junit.Test;

import rtlib.cameras.StackCameraDeviceInterface;
import rtlib.cameras.devices.orcaflash4.OrcaFlash4StackCamera;
import rtlib.cameras.devices.sim.StackCameraDeviceSimulator;
import rtlib.core.concurrent.future.FutureBooleanList;
import rtlib.core.variable.VariableListenerAdapter;
import rtlib.microscope.lightsheet.LightSheetMicroscope;
import rtlib.microscope.lightsheet.gui.LightSheetMicroscopeGUI;
import rtlib.microscope.lightsheet.illumination.LightSheet;
import rtlib.stack.StackInterface;
import rtlib.stack.processor.StackIdentityPipeline;
import rtlib.symphony.devices.SignalGeneratorInterface;
import rtlib.symphony.devices.nirio.NIRIOSignalGenerator;
import rtlib.symphony.devices.sim.SignalGeneratorSimulatorDevice;
import rtlib.symphony.gui.ScoreVisualizerJFrame;
import rtlib.symphony.movement.Movement;
import rtlib.symphony.score.ScoreInterface;

public class LightSheetMicroscopeDemo
{

	@Test
	public void demoOnSimulators() throws InterruptedException,
																ExecutionException
	{
		final SignalGeneratorInterface lSignalGeneratorDevice = new SignalGeneratorSimulatorDevice();
		final StackCameraDeviceInterface<UnsignedShortType, ShortOffHeapAccess> lCamera = new StackCameraDeviceSimulator<>(	null,
																																																												new UnsignedShortType(),
																																																												lSignalGeneratorDevice.getTriggerVariable());

		demoWith(lCamera, lSignalGeneratorDevice);

	}

	@Test
	public void demoOnRealHardware() throws InterruptedException,
																	ExecutionException
	{
		final SignalGeneratorInterface lSignalGeneratorDevice = new NIRIOSignalGenerator();
		final StackCameraDeviceInterface<UnsignedShortType, ShortOffHeapAccess> lCamera = OrcaFlash4StackCamera.buildWithExternalTriggering(0);

		demoWith(lCamera, lSignalGeneratorDevice);

	}

	public void demoWith(	StackCameraDeviceInterface<UnsignedShortType, ShortOffHeapAccess> pCamera,
												SignalGeneratorInterface pSignalGeneratorDevice) throws InterruptedException,
																																				ExecutionException
	{
		pCamera.getStackWidthVariable().setValue(128);
		pCamera.getStackHeightVariable().setValue(128);
		pCamera.getExposureInMicrosecondsVariable().setValue(5000);

		final LightSheetMicroscope lLightSheetMicroscope = new LightSheetMicroscope("demoscope");


		final StackIdentityPipeline<UnsignedShortType, ShortOffHeapAccess> lStackIdentityPipeline = new StackIdentityPipeline<UnsignedShortType, ShortOffHeapAccess>();

		lStackIdentityPipeline.getOutputVariable()
													.addListener(new VariableListenerAdapter<StackInterface<UnsignedShortType, ShortOffHeapAccess>>()
													{

														@Override
														public void setEvent(	StackInterface<UnsignedShortType, ShortOffHeapAccess> pCurrentValue,
																									StackInterface<UnsignedShortType, ShortOffHeapAccess> pNewValue)
														{
															System.out.println(pNewValue);
														}

													});

		lLightSheetMicroscope.getDeviceLists()
													.addStackCameraDevice(pCamera,
																								lStackIdentityPipeline);

		lLightSheetMicroscope.getDeviceLists()
													.addSignalGeneratorDevice(pSignalGeneratorDevice);

		final LightSheet lLightSheet = new LightSheet("demolightsheet",
																									9.4,
																									512,
																									2);
		lLightSheetMicroscope.getDeviceLists()
													.addLightSheetDevice(lLightSheet);

		lLightSheet.getLightSheetLengthInMicronsVariable().setValue(100);
		lLightSheet.getEffectiveExposureInMicrosecondsVariable()
								.setValue(5000);

		lLightSheet.getImageHeightVariable()
								.setValue(pCamera.getStackHeightVariable().getValue());

		final Movement lBeforeExposureMovement = new Movement("BeforeExposure");
		final Movement lExposureMovement = new Movement("Exposure");

		lBeforeExposureMovement.setDuration(lLightSheet.getBeforeExposureMovementDuration(TimeUnit.NANOSECONDS),
																				TimeUnit.NANOSECONDS);
		lExposureMovement.setDuration(lLightSheet.getExposureMovementDuration(TimeUnit.NANOSECONDS),
																	TimeUnit.NANOSECONDS);

		lLightSheet.addStavesToBeforeExposureMovement(lBeforeExposureMovement);
		lLightSheet.addStavesToExposureMovement(lExposureMovement);

		final ScoreInterface lStagingScore = pSignalGeneratorDevice.getStagingScore();

		lStagingScore.addMovement(lBeforeExposureMovement);
		lStagingScore.addMovement(lExposureMovement);

		final ScoreVisualizerJFrame lVisualizer = ScoreVisualizerJFrame.visualize("LightSheetDemo",
																																							lStagingScore);

		final LightSheetMicroscopeGUI lGUI = new LightSheetMicroscopeGUI(lLightSheetMicroscope);

		assertTrue(lGUI.open());
		assertTrue(lLightSheetMicroscope.open());
		Thread.sleep(1000);

		lGUI.connectGUI();

		System.out.println("Start building queue");

		for (int i = 0; i < 128; i++)
			lLightSheetMicroscope.addCurrentStateToQueue();
		lLightSheetMicroscope.addCurrentStateToQueueNotCounting();
		System.out.println("finished building queue");

		while (lVisualizer.isVisible())
		{
			System.out.println("playQueue!");
			final FutureBooleanList lPlayQueue = lLightSheetMicroscope.playQueue();

			System.out.print("waiting...");
			final Boolean lBoolean = lPlayQueue.get();
			System.out.print(" ...done!");
			// System.out.println(lBoolean);
			// Thread.sleep(4000);
		}

		assertTrue(lLightSheetMicroscope.close());
		assertTrue(lGUI.close());

	}

}