package rtlib.gui.video.video2d;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import rtlib.core.concurrent.asyncprocs.AsynchronousProcessorBase;
import rtlib.core.device.NamedVirtualDevice;
import rtlib.core.variable.types.booleanv.BooleanVariable;
import rtlib.core.variable.types.doublev.DoubleVariable;
import rtlib.core.variable.types.objectv.ObjectVariable;
import rtlib.gui.video.StackDisplayInterface;
import rtlib.gui.video.video2d.videowindow.VideoWindow;
import rtlib.stack.StackInterface;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

import coremem.ContiguousMemoryInterface;

public class Stack2DDisplay<T extends NativeType<T>, A extends ArrayDataAccess<A>>	extends
																																										NamedVirtualDevice implements
																																																			StackDisplayInterface<T, A>
{
	private final VideoWindow<T> mVideoWindow;

	private final ObjectVariable<StackInterface<T, A>> mInputStackVariable;
	private ObjectVariable<StackInterface<T, A>> mOutputStackVariable;

	private volatile StackInterface<T, A> mReceivedStackCopy;

	private final BooleanVariable mDisplayOn;
	private final BooleanVariable mManualMinMaxIntensity;
	private final DoubleVariable mMinimumIntensity;
	private final DoubleVariable mMaximumIntensity;

	private final DoubleVariable mStackSliceNormalizedIndex;

	private AsynchronousProcessorBase<StackInterface<T, A>, Object> mAsynchronousDisplayUpdater;

	private final Object mReleaseLock = new Object();

	public Stack2DDisplay(T pType)
	{
		this("2D Video Display", pType, 512, 512, 1);
	}

	public Stack2DDisplay(T pType,
												final int pVideoWidth,
												final int pVideoHeight)
	{
		this("2D Video Display", pType, pVideoWidth, pVideoHeight, 10);
	}

	public Stack2DDisplay(final String pWindowName,
												T pType,
												final int pVideoWidth,
												final int pVideoHeight)
	{
		this(pWindowName, pType, pVideoWidth, pVideoHeight, 10);
	}

	public Stack2DDisplay(final String pWindowName,
												T pType,
												final int pWindowWidth,
												final int pWindowHeight,
												final int pUpdaterQueueLength)
	{
		super(pWindowName);

		mVideoWindow = new VideoWindow<T>(pWindowName,
																			pType,
																			pWindowWidth,
																			pWindowHeight);

		mVideoWindow.setVisible(true);

		final MouseAdapter lMouseAdapter = new MouseAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent pMouseEvent)
			{
				if (pMouseEvent.isAltDown() && pMouseEvent.isButtonDown(1))
				{
					final double nx = ((double) pMouseEvent.getX()) / mVideoWindow.getWindowWidth();
					mStackSliceNormalizedIndex.setValue(nx);
					mAsynchronousDisplayUpdater.passOrFail(mReceivedStackCopy);
				}

				super.mouseDragged(pMouseEvent);
			}
		};

		mVideoWindow.getGLWindow().addMouseListener(lMouseAdapter);

		mAsynchronousDisplayUpdater = new AsynchronousProcessorBase<StackInterface<T, A>, Object>("AsynchronousDisplayUpdater",
																																															pUpdaterQueueLength)
		{
			@Override
			public Object process(final StackInterface<T, A> pStack)
			{
				try
				{
					if (pStack != mReceivedStackCopy)
					{

						if (mReceivedStackCopy == null || mReceivedStackCopy.getWidth() != pStack.getWidth()
								|| mReceivedStackCopy.getHeight() != pStack.getHeight()
								|| mReceivedStackCopy.getDepth() != pStack.getDepth()
								|| mReceivedStackCopy.getSizeInBytes() != pStack.getSizeInBytes())
						{
							if (mReceivedStackCopy != null)
							{
								final StackInterface<T, A> lStackToFree = mReceivedStackCopy;
								mReceivedStackCopy = pStack.duplicate();
								lStackToFree.free();
							}
							else
								mReceivedStackCopy = pStack.duplicate();
						}
						else if (!mReceivedStackCopy.isFree())
						{
							mReceivedStackCopy.getContiguousMemory()
																.copyFrom(pStack.getContiguousMemory());

						}

					}

					displayStack(mReceivedStackCopy);

					if (mOutputStackVariable != null)
					{
						mOutputStackVariable.setReference(pStack);
					}
					else
						pStack.release();

				}
				catch (coremem.rgc.FreedException e)
				{
					System.err.println(this.getClass().getSimpleName() + ": Underlying ressource has been freed while processing last stack");
				}

				return null;
			}
		};

		mAsynchronousDisplayUpdater.start();

		mInputStackVariable = new ObjectVariable<StackInterface<T, A>>(pWindowName + "StackInput")
		{

			@Override
			public StackInterface<T, A> setEventHook(	final StackInterface<T, A> pOldStack,
																								final StackInterface<T, A> pNewStack)
			{
				if (!mAsynchronousDisplayUpdater.passOrFail(pNewStack))
				{
					pNewStack.release();
				}
				return super.setEventHook(pOldStack, pNewStack);
			}

		};

		mDisplayOn = new BooleanVariable("DisplayOn", true)
		{
			@Override
			public Double setEventHook(	final Double pOldValue,
																	final Double pNewValue)
			{
				final boolean lDisplayOn = BooleanVariable.double2boolean(pNewValue);
				mVideoWindow.setDisplayOn(lDisplayOn);
				return super.setEventHook(pOldValue, pNewValue);
			}
		};

		mManualMinMaxIntensity = new BooleanVariable(	"ManualMinMaxIntensity",
																									false)
		{
			@Override
			public Double setEventHook(	final Double pOldValue,
																	final Double pNewValue)
			{
				final boolean lManualMinMax = BooleanVariable.double2boolean(pNewValue);
				mVideoWindow.setManualMinMax(lManualMinMax);
				return super.setEventHook(pOldValue, pNewValue);
			}
		};

		mMinimumIntensity = new DoubleVariable("MinimumIntensity", 0)
		{
			@Override
			public Double setEventHook(	final Double pOldValue,
																	final Double pNewMinIntensity)
			{
				final double lMinIntensity = Math.pow(pNewMinIntensity, 6);
				mVideoWindow.setMinIntensity(lMinIntensity);
				return super.setEventHook(pOldValue, pNewMinIntensity);
			}
		};

		mMaximumIntensity = new DoubleVariable("MaximumIntensity", 1)
		{
			@Override
			public Double setEventHook(	final Double pOldValue,
																	final Double pNewMaxIntensity)
			{
				final double lMaxIntensity = Math.pow(pNewMaxIntensity, 6);
				mVideoWindow.setMaxIntensity(lMaxIntensity);
				return super.setEventHook(pOldValue, pNewMaxIntensity);
			}
		};

		mStackSliceNormalizedIndex = new DoubleVariable("StackSliceNormalizedIndex",
																										Double.NaN);
	}

	private void displayStack(final StackInterface<T, A> pStack)
	{

		final int lStackWidth = (int) pStack.getWidth();
		final int lStackHeight = (int) pStack.getHeight();
		final int lStackDepth = (int) pStack.getDepth();
		if (lStackDepth > 1)
		{

			int lStackZIndex = (int) (mStackSliceNormalizedIndex.getValue() * lStackDepth);
			if (lStackZIndex < 0)
				lStackZIndex = 0;
			else if (lStackZIndex >= lStackDepth)
				lStackZIndex = lStackDepth - 1;
			else if (Double.isNaN(lStackZIndex))
				lStackZIndex = (int) Math.round(lStackDepth / 2.0);

			final ContiguousMemoryInterface lContiguousMemory = pStack.getContiguousMemory(lStackZIndex);
			mVideoWindow.sendBuffer(lContiguousMemory,
															lStackWidth,
															lStackHeight);
			mVideoWindow.waitForBufferCopy(1, TimeUnit.SECONDS);
		}
		else
		{
			final ContiguousMemoryInterface lContiguousMemory = pStack.getContiguousMemory(0);
			mVideoWindow.sendBuffer(lContiguousMemory,
															lStackWidth,
															lStackHeight);
			mVideoWindow.waitForBufferCopy(1, TimeUnit.SECONDS);
		}
		mVideoWindow.setWidth(lStackWidth);
		mVideoWindow.setHeight(lStackHeight);

	}

	@Override
	public ObjectVariable<StackInterface<T, A>> getOutputStackVariable()
	{
		return mOutputStackVariable;
	}

	@Override
	public void setOutputStackVariable(ObjectVariable<StackInterface<T, A>> pOutputStackVariable)
	{
		mOutputStackVariable = pOutputStackVariable;
	}

	public BooleanVariable getDisplayOnVariable()
	{
		return mDisplayOn;
	}

	public BooleanVariable getManualMinMaxIntensityOnVariable()
	{
		return mManualMinMaxIntensity;
	}

	public DoubleVariable getMinimumIntensityVariable()
	{
		return mMinimumIntensity;
	}

	public DoubleVariable getMaximumIntensityVariable()
	{
		return mMaximumIntensity;
	}

	public ObjectVariable<StackInterface<T, A>> getInputStackVariable()
	{
		return mInputStackVariable;
	}

	public void setVisible(final boolean pIsVisible)
	{
		mVideoWindow.setVisible(pIsVisible);
	}

	@Override
	public boolean open()
	{
		mDisplayOn.setValue(true);
		setVisible(true);
		mVideoWindow.start();
		return true;
	}

	@Override
	public boolean close()
	{
		setVisible(false);
		try
		{
			mVideoWindow.stop();
			mDisplayOn.setValue(false);
			mVideoWindow.close();
			return true;
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public void disableClose()
	{
		mVideoWindow.disableClose();
	}

}
