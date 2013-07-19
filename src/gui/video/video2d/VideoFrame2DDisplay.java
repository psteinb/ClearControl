package gui.video.video2d;

import gui.video.video2d.jogl.VideoWindow;

import java.io.IOException;

import javax.media.opengl.GLException;

import stack.Stack;
import variable.booleanv.BooleanVariable;
import variable.doublev.DoubleVariable;
import variable.objectv.ObjectVariable;
import device.NamedDevice;
import device.SignalStartableDevice;

public class VideoFrame2DDisplay extends NamedDevice
{
	private final VideoWindow mVideoWindow;

	private final ObjectVariable<Stack> mObjectVariable;

	private final BooleanVariable mDisplayOn;
	private final BooleanVariable mManualMinMaxIntensity;
	private final DoubleVariable mMinimumIntensity;
	private final DoubleVariable mMaximumIntensity;

	public VideoFrame2DDisplay()
	{
		this("2D Video Display", 512, 512, 1);
	}

	public VideoFrame2DDisplay(	final int pVideoWidth,
															final int pVideoHeight)
	{
		this("2D Video Display", pVideoWidth, pVideoHeight, 1);
	}

	public VideoFrame2DDisplay(	final String pWindowName,
															final int pVideoWidth,
															final int pVideoHeight)
	{
		this(pWindowName, pVideoWidth, pVideoHeight, 1);
	}

	public VideoFrame2DDisplay(	final String pWindowName,
															final int pVideoWidth,
															final int pVideoHeight,
															final int pBytesPerPixel)
	{
		super(pWindowName);

		mVideoWindow = new VideoWindow(	pWindowName,
																		pBytesPerPixel,
																		pVideoWidth,
																		pVideoHeight);

		mObjectVariable = new ObjectVariable<Stack>(pWindowName)
		{

			@Override
			public Stack setEventHook(Stack pNewFrameReference)
			{
				// System.out.println(pNewFrameReference.buffer);

				mVideoWindow.setSourceBuffer(pNewFrameReference.getByteBuffer());
				mVideoWindow.setWidth(pNewFrameReference.getWidth());
				mVideoWindow.setHeight(pNewFrameReference.getHeight());
				mVideoWindow.setBytesPerPixel(pNewFrameReference.bpp);
				mVideoWindow.notifyNewFrame();

				mVideoWindow.display();
				pNewFrameReference.releaseFrame();
				return super.setEventHook(pNewFrameReference);
			}

		};

		mDisplayOn = new BooleanVariable("DisplayOn", true)
		{
			@Override
			public double setEventHook(final double pNewValue)
			{
				final boolean lDisplayOn = BooleanVariable.double2boolean(pNewValue);
				mVideoWindow.setDisplayOn(lDisplayOn);
				return super.setEventHook(pNewValue);
			}
		};

		mManualMinMaxIntensity = new BooleanVariable(	"ManualMinMaxIntensity",
																									false)
		{
			@Override
			public double setEventHook(final double pNewValue)
			{
				final boolean lManualMinMax = BooleanVariable.double2boolean(pNewValue);
				mVideoWindow.setManualMinMax(lManualMinMax);
				return super.setEventHook(pNewValue);
			}
		};

		mMinimumIntensity = new DoubleVariable("MinimumIntensity", 0)
		{
			@Override
			public double setEventHook(final double pNewMinIntensity)
			{
				final double lMinIntensity = Math.pow(pNewMinIntensity, 6);
				mVideoWindow.setMinIntensity(lMinIntensity);
				return super.setEventHook(pNewMinIntensity);
			}
		};

		mMaximumIntensity = new DoubleVariable("MaximumIntensity", 1)
		{
			@Override
			public double setEventHook(final double pNewMaxIntensity)
			{
				final double lMaxIntensity = Math.pow(pNewMaxIntensity, 6);
				mVideoWindow.setMaxIntensity(lMaxIntensity);
				return super.setEventHook(pNewMaxIntensity);
			}
		};
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

	public ObjectVariable<Stack> getFrameReferenceVariable()
	{
		return mObjectVariable;
	}

	public void setVisible(final boolean pIsVisible)
	{
		mVideoWindow.setVisible(pIsVisible);
	}

	public void setLinearInterpolation(final boolean pLinearInterpolation)
	{
		mVideoWindow.setLinearInterpolation(pLinearInterpolation);
	}

	public void setSyncToRefresh(final boolean pSyncToRefresh)
	{
		mVideoWindow.setSyncToRefresh(pSyncToRefresh);
	}

	@Override
	public boolean open()
	{
		setVisible(true);
		return true;
	}

	@Override
	public boolean close()
	{
		setVisible(false);
		try
		{
			mVideoWindow.close();
			return true;
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean start()
	{
		mDisplayOn.setValue(true);
		return true;
	}

	@Override
	public boolean stop()
	{
		mDisplayOn.setValue(false);
		return true;
	}

	public void disableClose()
	{
		mVideoWindow.disableClose();
	}

}