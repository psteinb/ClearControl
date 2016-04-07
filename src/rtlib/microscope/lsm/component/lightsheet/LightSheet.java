package rtlib.microscope.lsm.component.lightsheet;

import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;

import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import rtlib.core.concurrent.executors.AsynchronousExecutorServiceAccess;
import rtlib.core.configuration.MachineConfiguration;
import rtlib.core.device.NamedVirtualDevice;
import rtlib.core.math.functions.UnivariateAffineComposableFunction;
import rtlib.core.math.functions.UnivariateAffineFunction;
import rtlib.core.variable.Variable;
import rtlib.core.variable.VariableSetListener;
import rtlib.microscope.lsm.component.lightsheet.si.BinaryStructuredIlluminationPattern;
import rtlib.microscope.lsm.component.lightsheet.si.StructuredIlluminationPatternInterface;
import rtlib.symphony.movement.Movement;
import rtlib.symphony.staves.ConstantStave;
import rtlib.symphony.staves.EdgeStave;
import rtlib.symphony.staves.IntervalStave;
import rtlib.symphony.staves.RampSteppingStave;
import rtlib.symphony.staves.StaveInterface;

public class LightSheet extends NamedVirtualDevice implements
																									LightSheetInterface,
																									AsynchronousExecutorServiceAccess
{

	private final Variable<UnivariateAffineComposableFunction> mXFunction = new Variable<>(	"LightSheetXFunction",
																																																			new UnivariateAffineFunction());
	private final Variable<UnivariateAffineComposableFunction> mYFunction = new Variable<>(	"LightSheetYFunction",
																																																			new UnivariateAffineFunction());
	private final Variable<UnivariateAffineComposableFunction> mZFunction = new Variable<>(	"LightSheetZFunction",
																																																			new UnivariateAffineFunction());

	private final Variable<UnivariateAffineComposableFunction> mWidthFunction = new Variable<>(	"LightSheetWidthFunction",
																																																					new UnivariateAffineFunction());
	private final Variable<UnivariateAffineComposableFunction> mHeightFunction = new Variable<>("LightSheetHeightFunction",
																																																					new UnivariateAffineFunction());

	private final Variable<UnivariateAffineComposableFunction> mAlphaFunction = new Variable<>(	"LightSheetAlphaFunction",
																																																					new UnivariateAffineFunction());
	private final Variable<UnivariateAffineComposableFunction> mBetaFunction = new Variable<>("LightSheetBetaFunction",
																																																				new UnivariateAffineFunction());

	private final Variable<UnivariateAffineComposableFunction> mPowerFunction = new Variable<>(	"LightSheetPowerFunction",
																																																					new UnivariateAffineFunction());

	private final Variable<PolynomialFunction> mWidthPowerFunction = new Variable<>("LightSheetWidthPowerFunction",
																																															new PolynomialFunction(new double[]
																																															{ 1,
																																																0 }));

	private final Variable<PolynomialFunction> mHeightPowerFunction = new Variable<>(	"LightSheetHeightPowerFunction",
																																																new PolynomialFunction(new double[]
																																																{ 1,
																																																	0 }));

	private final Variable<Double> mEffectiveExposureInMicrosecondsVariable = new Variable<Double>(	"EffectiveExposureInMicroseconds",
																																																							5000.0);
	private final Variable<Long> mImageHeightVariable = new Variable<Long>(	"ImageHeight",
																																											2 * 1024L);
	private final Variable<Double> mReadoutTimeInMicrosecondsPerLineVariable = new Variable<Double>("ReadoutTimeInMicrosecondsPerLine",
																																																							9.74);
	private final Variable<Double> mOverScanVariable = new Variable<Double>("OverScan",
																																											1.2);

	private final Variable<Double> mXVariable = new Variable<Double>(	"LightSheetX",
																																								0.0);
	private final Variable<Double> mYVariable = new Variable<Double>(	"LightSheetY",
																																								0.0);
	private final Variable<Double> mZVariable = new Variable<Double>(	"LightSheetZ",
																																								0.0);

	private final Variable<Double> mAlphaInDegreesVariable = new Variable<Double>("LightSheetAlphaInDegrees",
																																														0.0);
	private final Variable<Double> mBetaInDegreesVariable = new Variable<Double>(	"LightSheetBetaInDegrees",
																																														0.0);
	private final Variable<Double> mWidthVariable = new Variable<Double>(	"LightSheetRange",
																																										0.0);
	private final Variable<Double> mHeightVariable = new Variable<Double>("LightSheetLength",
																																										0.0);
	private final Variable<Double> mPowerVariable = new Variable<Double>(	"LightSheetLengthPower",
																																										1.0);
	private final Variable<Boolean> mAdaptPowerToWidthHeightVariable = new Variable<Boolean>(	"AdaptLightSheetPowerToWidthHeight",
																																																				false);

	private final Variable<Double> mLineExposureInMicrosecondsVariable = new Variable<Double>("LineExposureInMicroseconds",
																																																				10.0);

	private final Variable<Boolean>[] mLaserOnOffVariableArray;

	private final Variable<Boolean>[] mSIPatternOnOffVariableArray;

	private final Variable<StructuredIlluminationPatternInterface>[] mStructuredIlluminationPatternVariableArray;

	private Movement mBeforeExposureMovement, mExposureMovement;

	private RampSteppingStave mBeforeExposureZStave,
			mBeforeExposureYStave, mExposureYStave, mExposureZStave;

	private ConstantStave mBeforeExposureXStave, mExposureXStave,
			mBeforeExposureBStave, mExposureBStave, mBeforeExposureWStave,
			mExposureWStave, mBeforeExposureLAStave, mExposureLAStave;
	private IntervalStave mNonSIIluminationLaserTriggerStave;

	private EdgeStave mBeforeExposureTStave, mExposureTStave;

	private final int mNumberOfLaserDigitalControls;

	@SuppressWarnings("unchecked")
	public LightSheet(String pName,
										final double pReadoutTimeInMicrosecondsPerLine,
										final long pNumberOfLines,
										final int pNumberOfLaserDigitalControls)
	{
		super(pName);

		mNumberOfLaserDigitalControls = pNumberOfLaserDigitalControls;

		final VariableSetListener lDoubleVariableListener = (u, v) -> {
			update();
		};

		mLaserOnOffVariableArray = new Variable[mNumberOfLaserDigitalControls];

		mSIPatternOnOffVariableArray = new Variable[mNumberOfLaserDigitalControls];

		mStructuredIlluminationPatternVariableArray = new Variable[mNumberOfLaserDigitalControls];

		mReadoutTimeInMicrosecondsPerLineVariable.set(pReadoutTimeInMicrosecondsPerLine);
		mImageHeightVariable.set(pNumberOfLines);

		mBeforeExposureLAStave = new ConstantStave(	"laser.beforeexp.am",
																								0);
		mExposureLAStave = new ConstantStave("laser.exposure.am", 0);

		mBeforeExposureXStave = new ConstantStave("lightsheet.x.be", 0);
		mBeforeExposureYStave = new RampSteppingStave("lightsheet.y.be");
		mBeforeExposureZStave = new RampSteppingStave("lightsheet.z.be");
		mBeforeExposureBStave = new ConstantStave("lightsheet.b.be", 0);
		mBeforeExposureWStave = new ConstantStave("lightsheet.r.be", 0);
		mBeforeExposureTStave = new EdgeStave("trigger.out.be", 1, 1, 0);

		mExposureXStave = new ConstantStave("lightsheet.x.e", 0);
		mExposureYStave = new RampSteppingStave("lightsheet.y.e");
		mExposureZStave = new RampSteppingStave("lightsheet.z.e");
		mExposureBStave = new ConstantStave("lightsheet.b.e", 0);
		mExposureWStave = new ConstantStave("lightsheet.r.e", 0);
		mExposureTStave = new EdgeStave("trigger.out.e", 1, 0, 0);

		mNonSIIluminationLaserTriggerStave = new IntervalStave(	"trigger.out",
																														0,
																														1,
																														1,
																														0);

		for (int i = 0; i < mLaserOnOffVariableArray.length; i++)
		{
			final String lLaserName = "Laser" + i + ".exposure.trig";

			mStructuredIlluminationPatternVariableArray[i] = new Variable("StructuredIlluminationPattern",
																																					new BinaryStructuredIlluminationPattern());

			mLaserOnOffVariableArray[i] = new Variable<Boolean>(lLaserName,
																																false);
			mLaserOnOffVariableArray[i].addSetListener(lDoubleVariableListener);

			mSIPatternOnOffVariableArray[i] = new Variable<Boolean>(lLaserName + "SIPatternOnOff",
																																		false);
			mSIPatternOnOffVariableArray[i].addSetListener(lDoubleVariableListener);
		}

		mReadoutTimeInMicrosecondsPerLineVariable.addSetListener(lDoubleVariableListener);
		mOverScanVariable.addSetListener(lDoubleVariableListener);
		mEffectiveExposureInMicrosecondsVariable.addSetListener(lDoubleVariableListener);
		mImageHeightVariable.addSetListener(lDoubleVariableListener);

		mXVariable.addSetListener(lDoubleVariableListener);
		mXVariable.addSetListener(lDoubleVariableListener);
		mZVariable.addSetListener(lDoubleVariableListener);
		mBetaInDegreesVariable.addSetListener(lDoubleVariableListener);
		mAlphaInDegreesVariable.addSetListener(lDoubleVariableListener);
		mHeightVariable.addSetListener(lDoubleVariableListener);
		mWidthVariable.addSetListener(lDoubleVariableListener);
		mPowerVariable.addSetListener(lDoubleVariableListener);
		mOverScanVariable.addSetListener(lDoubleVariableListener);
		mAdaptPowerToWidthHeightVariable.addSetListener(lDoubleVariableListener);

		for (int i = 0; i < mLaserOnOffVariableArray.length; i++)
		{
			mStructuredIlluminationPatternVariableArray[i].addSetListener((	u,
																																			v) -> {
				update();
			});
		}

		final VariableSetListener<?> lObjectVariableListener = (u, v) -> {
			update();
		};

		resetFunctions();

		mXFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);
		mYFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);
		mZFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);

		mAlphaFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);
		mBetaFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);
		mWidthFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);
		mHeightFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);
		mPowerFunction.addSetListener((VariableSetListener<UnivariateAffineComposableFunction>) lObjectVariableListener);

		mWidthPowerFunction.addSetListener((VariableSetListener<PolynomialFunction>) lObjectVariableListener);
		mHeightPowerFunction.addSetListener((VariableSetListener<PolynomialFunction>) lObjectVariableListener);

	}

	@Override
	public void resetFunctions()
	{
		mXFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																				.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																			+ ".x.f"));

		mYFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																				.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																			+ ".y.f"));

		mZFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																				.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																			+ ".z.f"));

		mWidthFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																						.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																					+ ".w.f"));

		mHeightFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																						.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																					+ ".h.f"));

		mAlphaFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																						.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																					+ ".a.f"));

		mBetaFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																					.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																				+ ".b.f"));

		mPowerFunction.set(MachineConfiguration.getCurrentMachineConfiguration()
																						.getUnivariateAffineFunction("device.lsm.lighsheet." + getName()
																																					+ ".p.f"));

		// TODO: load a polynomial:
		mWidthPowerFunction.set(new PolynomialFunction(new double[]
		{ 1 }));

		mHeightPowerFunction.set(new PolynomialFunction(new double[]
		{ 1 }));/**/
	}

	public void setBeforeExposureMovement(Movement pBeforeExposureMovement)
	{
		mBeforeExposureMovement = pBeforeExposureMovement;
		ensureStavesAddedToBeforeExposureMovement(mBeforeExposureMovement);
	}

	public void setExposureMovement(Movement pExposureMovement)
	{
		mExposureMovement = pExposureMovement;
		ensureStavesAddedToExposureMovement(mExposureMovement);
	}

	private void ensureStavesAddedToBeforeExposureMovement(Movement pBeforeExposureMovement)
	{
		final MachineConfiguration lCurrentMachineConfiguration = MachineConfiguration.getCurrentMachineConfiguration();

		// Analog outputs before exposure:
		mBeforeExposureXStave = pBeforeExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".x.index",
																																																										2),
																																		mBeforeExposureXStave);

		mBeforeExposureYStave = pBeforeExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".y.index",
																																																										3),
																																		mBeforeExposureYStave);

		mBeforeExposureZStave = pBeforeExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".z.index",
																																																										4),
																																		mBeforeExposureZStave);

		mBeforeExposureBStave = pBeforeExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".b.index",
																																																										5),
																																		mBeforeExposureBStave);

		mBeforeExposureWStave = pBeforeExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".w.index",
																																																										6),
																																		mBeforeExposureWStave);

		mBeforeExposureLAStave = pBeforeExposureMovement.ensureSetStave(lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".la.index",
																																																										7),
																																		mBeforeExposureLAStave);

		mBeforeExposureTStave = pBeforeExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																												+ ".t.index",
																																																										8 + 7),
																																		mBeforeExposureTStave);

	}

	private void ensureStavesAddedToExposureMovement(Movement pExposureMovement)
	{
		final MachineConfiguration lCurrentMachineConfiguration = MachineConfiguration.getCurrentMachineConfiguration();

		// Analog outputs at exposure:

		mExposureXStave = pExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".x.index",
																																																				2),
																												mExposureXStave);

		mExposureYStave = pExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".y.index",
																																																				3),
																												mExposureYStave);

		mExposureZStave = pExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".z.index",
																																																				4),
																												mExposureZStave);

		mExposureBStave = pExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".b.index",
																																																				5),
																												mExposureBStave);

		mExposureWStave = pExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".w.index",
																																																				6),
																												mExposureWStave);

		mExposureLAStave = pExposureMovement.ensureSetStave(lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".la.index",
																																																				7),
																												mExposureLAStave);

		mExposureTStave = pExposureMovement.ensureSetStave(	lCurrentMachineConfiguration.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																																						+ ".t.index",
																																																				8 + 7),
																												mExposureTStave);

		for (int i = 0; i < mLaserOnOffVariableArray.length; i++)
			mNonSIIluminationLaserTriggerStave = setLaserDigitalTriggerStave(	pExposureMovement,
																																				i,
																																				mNonSIIluminationLaserTriggerStave);

	}

	private <O extends StaveInterface> O setLaserDigitalTriggerStave(	Movement pExposureMovement,
																																		int i,
																																		O pStave)
	{
		final int lLaserDigitalLineIndex = MachineConfiguration.getCurrentMachineConfiguration()
																														.getIntegerProperty("device.lsm.lightsheet." + getName().toLowerCase()
																																										+ ".ld.index"
																																										+ i,
																																								8 + i);
		return mExposureMovement.ensureSetStave(lLaserDigitalLineIndex,
																						pStave);
	}

	@Override
	public void update()
	{
		synchronized (this)
		{
			// System.out.println("Updating: " + getName());
			final double lReadoutTimeInMicroseconds = getBeforeExposureMovementDuration(TimeUnit.MICROSECONDS);
			final double lExposureMovementTimeInMicroseconds = getExposureMovementDuration(TimeUnit.MICROSECONDS);

			mBeforeExposureMovement.setDuration(round(lReadoutTimeInMicroseconds),
																					TimeUnit.MICROSECONDS);
			mExposureMovement.setDuration(round(lExposureMovementTimeInMicroseconds),
																		TimeUnit.MICROSECONDS);

			final double lLineExposureTimeInMicroseconds = lReadoutTimeInMicroseconds + lExposureMovementTimeInMicroseconds;
			mLineExposureInMicrosecondsVariable.set(lLineExposureTimeInMicroseconds);

			final double lGalvoYOffsetBeforeRotation = mYVariable.get();
			final double lGalvoZOffsetBeforeRotation = mZVariable.get();

			final double lGalvoYOffset = galvoRotateY(lGalvoYOffsetBeforeRotation,
																								lGalvoZOffsetBeforeRotation);
			final double lGalvoZOffset = galvoRotateZ(lGalvoYOffsetBeforeRotation,
																								lGalvoZOffsetBeforeRotation);

			final double lLightSheetHeight = mHeightFunction.get()
																											.value(mHeightVariable.get()) * mOverScanVariable.get();
			final double lGalvoAmplitudeY = galvoRotateY(	lLightSheetHeight,
																										0);
			final double lGalvoAmplitudeZ = galvoRotateZ(	lLightSheetHeight,
																										0);

			final double lGalvoYLowValue = getYFunction().get()
																										.value(lGalvoYOffset - lGalvoAmplitudeY);
			final double lGalvoYHighValue = getYFunction().get()
																										.value(lGalvoYOffset + lGalvoAmplitudeY);

			final double lGalvoZLowValue = getZFunction().get()
																										.value(lGalvoZOffset - lGalvoAmplitudeZ);
			final double lGalvoZHighValue = getZFunction().get()
																										.value(lGalvoZOffset + lGalvoAmplitudeZ);

			mBeforeExposureYStave.setSyncStart(0);
			mBeforeExposureYStave.setSyncStop(1);
			mBeforeExposureYStave.setStartValue((float) lGalvoYHighValue);
			mBeforeExposureYStave.setStopValue((float) lGalvoYLowValue);
			mBeforeExposureYStave.setExponent(0.2f);

			mBeforeExposureZStave.setSyncStart(0);
			mBeforeExposureZStave.setSyncStop(1);
			mBeforeExposureZStave.setStartValue((float) lGalvoZHighValue);
			mBeforeExposureZStave.setStopValue((float) lGalvoZLowValue);

			mExposureYStave.setSyncStart(0);
			mExposureYStave.setSyncStop(1);
			mExposureYStave.setStartValue((float) lGalvoYLowValue);
			mExposureYStave.setStopValue((float) lGalvoYHighValue);
			mExposureYStave.setOutsideValue((float) lGalvoYHighValue);
			mExposureYStave.setNoJump(true);

			mExposureZStave.setSyncStart(0);
			mExposureZStave.setSyncStop(1);
			mExposureZStave.setStartValue((float) lGalvoZLowValue);
			mExposureZStave.setStopValue((float) lGalvoZHighValue);
			mExposureZStave.setOutsideValue((float) lGalvoZHighValue);
			mExposureZStave.setNoJump(true);

			mBeforeExposureXStave.setValue((float) getXFunction().get()
																														.value(mXVariable.get()));
			mExposureXStave.setValue((float) getXFunction().get()
																											.value(mXVariable.get()));

			mBeforeExposureBStave.setValue((float) getBetaFunction().get()
																															.value(mBetaInDegreesVariable.get()));
			mExposureBStave.setValue((float) getBetaFunction().get()
																												.value(mBetaInDegreesVariable.get()));

			/*final double lFocalLength = mFocalLengthInMicronsVariable.get();
			final double lLambdaInMicrons = mLambdaInMicronsVariable.get();
			final double lLightSheetRangeInMicrons = mWidthVariable.getValue();

			final double lIrisDiameterInMm = GaussianBeamGeometry.getBeamIrisDiameter(lFocalLength,
																																								lLambdaInMicrons,
																																								lLightSheetRangeInMicrons);/**/
			double lWidthValue = getWidthFunction().get()
																							.value(mWidthVariable.get());

			mBeforeExposureWStave.setValue((float) lWidthValue);
			mExposureWStave.setValue((float) lWidthValue);

			final double lOverscan = mOverScanVariable.get();
			final double lMarginTimeInMicroseconds = (lOverscan - 1) / (2 * lOverscan)
																								* lExposureMovementTimeInMicroseconds;
			final double lMarginTimeRelativeUnits = microsecondsToRelative(	lExposureMovementTimeInMicroseconds,
																																			lMarginTimeInMicroseconds);

			boolean lIsStepping = true;
			for (int i = 0; i < mLaserOnOffVariableArray.length; i++)
				lIsStepping &= mSIPatternOnOffVariableArray[i].get();

			mBeforeExposureYStave.setStepping(lIsStepping);
			mExposureYStave.setStepping(lIsStepping);
			mBeforeExposureZStave.setStepping(lIsStepping);
			mExposureZStave.setStepping(lIsStepping);

			for (int i = 0; i < mLaserOnOffVariableArray.length; i++)
			{
				final Variable<Boolean> lLaserBooleanVariable = mLaserOnOffVariableArray[i];

				if (mSIPatternOnOffVariableArray[i].get())
				{

					final StructuredIlluminationPatternInterface lStructuredIlluminatioPatternInterface = mStructuredIlluminationPatternVariableArray[i].get();
					final StaveInterface lSIIlluminationLaserTriggerStave = lStructuredIlluminatioPatternInterface.getStave(lMarginTimeRelativeUnits);
					lSIIlluminationLaserTriggerStave.setEnabled(lLaserBooleanVariable.get());

					setLaserDigitalTriggerStave(mExposureMovement,
																			i,
																			lSIIlluminationLaserTriggerStave);
				}
				else
				{
					mNonSIIluminationLaserTriggerStave.setEnabled(lLaserBooleanVariable.get());
					mNonSIIluminationLaserTriggerStave.setStart((float) lMarginTimeRelativeUnits);
					mNonSIIluminationLaserTriggerStave.setStop((float) (1 - lMarginTimeRelativeUnits));
					setLaserDigitalTriggerStave(mExposureMovement,
																			i,
																			mNonSIIluminationLaserTriggerStave);
				}

			}

			double lPowerValue = mPowerFunction.get()
																					.value(mPowerVariable.get());

			if (mAdaptPowerToWidthHeightVariable.get())
			{
				double lWidthPowerFactor = mWidthPowerFunction.get()
																											.value(lWidthValue);

				double lHeightPowerFactor = mHeightPowerFunction.get()
																												.value(lLightSheetHeight / lOverscan);/**/

				lPowerValue *= lWidthPowerFactor * lHeightPowerFactor;
			}

			mBeforeExposureLAStave.setValue(0f);
			mExposureLAStave.setValue((float) lPowerValue);

		}
	}

	@Override
	public int getNumberOfLaserDigitalControls()
	{
		return mNumberOfLaserDigitalControls;
	}

	public long getExposureMovementDuration(TimeUnit pTimeUnit)
	{
		return pTimeUnit.convert(	mEffectiveExposureInMicrosecondsVariable.get()
																																			.longValue(),
															TimeUnit.MICROSECONDS);
	}

	public long getBeforeExposureMovementDuration(TimeUnit pTimeUnit)
	{
		return pTimeUnit.convert(	(long) (mReadoutTimeInMicrosecondsPerLineVariable.get() * mImageHeightVariable.get() / 2),
															TimeUnit.MICROSECONDS);
	}

	private double galvoRotateY(double pY, double pZ)
	{
		final double lAlpha = Math.toRadians(mAlphaFunction.get()
																												.value(mAlphaInDegreesVariable.get()));
		return pY * cos(lAlpha) - pZ * sin(lAlpha);
	}

	private double galvoRotateZ(double pY, double pZ)
	{
		final double lAlpha = Math.toRadians(mAlphaFunction.get()
																												.value(mAlphaInDegreesVariable.get()));
		return pY * sin(lAlpha) + pZ * cos(lAlpha);
	}

	@Override
	public Variable<Long> getImageHeightVariable()
	{
		return mImageHeightVariable;
	}

	public void setEffectiveExposureInMicroseconds(final int pEffectiveExposureInMicroseconds)
	{
		mEffectiveExposureInMicrosecondsVariable.set((double) pEffectiveExposureInMicroseconds);
	}

	@Override
	public Variable<Double> getEffectiveExposureInMicrosecondsVariable()
	{
		return mEffectiveExposureInMicrosecondsVariable;
	}

	@Override
	public Variable<Double> getLineExposureInMicrosecondsVariable()
	{
		return mLineExposureInMicrosecondsVariable;
	}

	@Override
	public Variable<Double> getOverScanVariable()
	{
		return mOverScanVariable;
	}

	@Override
	public Variable<Double> getReadoutTimeInMicrosecondsPerLineVariable()
	{
		return mReadoutTimeInMicrosecondsPerLineVariable;
	}

	@Override
	public Variable<Double> getXVariable()
	{
		return mXVariable;
	}

	@Override
	public Variable<Double> getYVariable()
	{
		return mYVariable;
	}

	@Override
	public Variable<Double> getZVariable()
	{
		return mZVariable;
	}

	@Override
	public Variable<Double> getAlphaInDegreesVariable()
	{
		return mAlphaInDegreesVariable;
	}

	@Override
	public Variable<Double> getBetaInDegreesVariable()
	{
		return mBetaInDegreesVariable;
	}

	@Override
	public Variable<Double> getWidthVariable()
	{
		return mWidthVariable;
	}

	@Override
	public Variable<Double> getHeightVariable()
	{
		return mHeightVariable;
	}

	@Override
	public Variable<Double> getPowerVariable()
	{
		return mPowerVariable;
	}

	@Override
	public Variable<Boolean> getAdaptPowerToWidthHeightVariable()
	{
		return mAdaptPowerToWidthHeightVariable;
	}

	@Override
	public Variable<StructuredIlluminationPatternInterface> getSIPatternVariable(int pLaserIndex)
	{
		return mStructuredIlluminationPatternVariableArray[pLaserIndex];
	}

	@Override
	public int getNumberOfPhases(int pLaserIndex)
	{
		return mStructuredIlluminationPatternVariableArray[pLaserIndex].get()
																																		.getNumberOfPhases();
	}

	@Override
	public Variable<Boolean> getSIPatternOnOffVariable(int pLaserIndex)
	{
		return mSIPatternOnOffVariableArray[pLaserIndex];
	}

	@Override
	public Variable<Boolean> getLaserOnOffArrayVariable(int pLaserIndex)
	{
		return mLaserOnOffVariableArray[pLaserIndex];
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getXFunction()
	{
		return mXFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getYFunction()
	{
		return mYFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getZFunction()
	{
		return mZFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getWidthFunction()
	{
		return mWidthFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getHeightFunction()
	{
		return mHeightFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getAlphaFunction()
	{
		return mAlphaFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getBetaFunction()
	{
		return mBetaFunction;
	}

	@Override
	public Variable<UnivariateAffineComposableFunction> getPowerFunction()
	{
		return mPowerFunction;
	}

	@Override
	public Variable<PolynomialFunction> getWidthPowerFunction()
	{
		return mWidthPowerFunction;
	}

	@Override
	public Variable<PolynomialFunction> getHeightPowerFunction()
	{
		return mHeightPowerFunction;
	}

	public RampSteppingStave getGalvoScannerStaveBeforeExposureZ()
	{
		return mBeforeExposureZStave;
	}

	public RampSteppingStave getGalvoScannerStaveBeforeExposureY()
	{
		return mBeforeExposureYStave;
	}

	public ConstantStave getIllumPifocStaveBeforeExposureX()
	{
		return mBeforeExposureXStave;
	}

	public RampSteppingStave getGalvoScannerStaveExposureZ()
	{
		return mExposureZStave;
	}

	public RampSteppingStave getGalvoScannerStaveExposureY()
	{
		return mExposureYStave;
	}

	public ConstantStave getIllumPifocStaveExposureX()
	{
		return mExposureXStave;
	}

	public EdgeStave getTriggerOutStaveBeforeExposure()
	{
		return mBeforeExposureTStave;
	}

	public EdgeStave getTriggerOutStaveExposure()
	{
		return mExposureTStave;
	}

	public ConstantStave getLaserAnalogModulationBeforeExposure()
	{
		return mBeforeExposureLAStave;
	}

	public ConstantStave getLaserAnalogModulationExposure()
	{
		return mExposureLAStave;
	}

	private static double microsecondsToRelative(	final double pTotalTime,
																								final double pSubTime)
	{
		return pSubTime / pTotalTime;
	}

}