package score;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import score.interfaces.MovementInterface;
import score.interfaces.StaveInterface;
import score.staves.ConstantStave;

public class Movement extends NameableAbstract	implements
																						MovementInterface
{

	public static final int cDefaultNumberOfStavesPerMovement = 16;

	private double mDeltaTimeInMicroeconds;
	
	private final StaveInterface[] mStaveListArray;

	private ShortBuffer mMovementShortBuffer;
	private boolean mIsUpToDateBasedOnStaveList = false;
	
	public static final Movement NullMovement = new Movement("NullMovement");

	public Movement(final String pName)
	{
		this(pName, cDefaultNumberOfStavesPerMovement);
	}

	public Movement(final String pName, final int pNumberOfStaves)
	{
		super(pName);
		mStaveListArray = new StaveInterface[pNumberOfStaves];
		for (int i = 0; i < pNumberOfStaves; i++)
		{
			mStaveListArray[i] = new ConstantStave("Zero", 0);
		}
	}
	
	public void setDeltaTimeInMicroseconds(final double pDeltaTimeInMicroeconds)
	{
		mDeltaTimeInMicroeconds = pDeltaTimeInMicroeconds;
	}
	
	public double getDeltaTimeInMicroseconds()
	{
		return mDeltaTimeInMicroeconds;
	}
	
	@Override
	public int getMaxNumberOfTimePointsPerBuffer()
	{
		return StaveAbstract.cMaximumNumberOfTimePointsPerBuffer;
	}

	public boolean setStave(final int pStaveIndex,
													final StaveInterface pNewStave)
	{
		mStaveListArray[pStaveIndex] = pNewStave;
		mIsUpToDateBasedOnStaveList = false;
		return true;
	}

	private StaveInterface getFirstStave()
	{
		return mStaveListArray[0];
	}

	public int computeMovementBufferLength()
	{
		final StaveInterface lFirstStave = getFirstStave();
		final int lStaveBufferLength = lFirstStave.getStaveBufferLength();
		final int lMovementBufferLength = mStaveListArray.length * lStaveBufferLength;
		return lMovementBufferLength;
	}

	public ShortBuffer getMovementBuffer()
	{
		if (!isUpToDate())
		{
			updateMovementBuffer();
			mIsUpToDateBasedOnStaveList = true;
		}

		return mMovementShortBuffer;
	}
	
	

	private void updateMovementBuffer()
	{
		final int lMovementBufferLength = computeMovementBufferLength();
		final int lCurrentMovementBufferCapacity = mMovementShortBuffer == null	? 0
																																						: mMovementShortBuffer.capacity();
		if (lCurrentMovementBufferCapacity < lMovementBufferLength)
		{
			final int lMovementBufferLengthInBytes = lMovementBufferLength * 2;
			mMovementShortBuffer = ByteBuffer.allocateDirect(lMovementBufferLengthInBytes)
																				.order(ByteOrder.nativeOrder())
																				.asShortBuffer();
		}

		mMovementShortBuffer.limit(lMovementBufferLength);
		mMovementShortBuffer.clear();
		for (StaveInterface lStave : mStaveListArray)
		{
			final ShortBuffer lStaveShortBuffer = lStave.getStaveBuffer();
			lStaveShortBuffer.rewind();
		}

		while (mMovementShortBuffer.hasRemaining())
		{
			for (StaveInterface lStave : mStaveListArray)
			{
				final ShortBuffer lStaveShortBuffer = lStave.getStaveBuffer();
				final short lShortValue = lStaveShortBuffer.get();
				mMovementShortBuffer.put(lShortValue);
			}
		}
		mMovementShortBuffer.flip();

	}

	public boolean isUpToDate()
	{
		boolean lIsUpToDate = mIsUpToDateBasedOnStaveList;
		for (StaveInterface lStave : mStaveListArray)
		{
			lIsUpToDate &= lStave.isUpToDate();
		}
		return lIsUpToDate;
	}

	public int getNumberOfStaves()
	{
		final int lNumberOfChannels = mStaveListArray.length;
		return lNumberOfChannels;
	}

	public int getNumberOfTimePoints()
	{
		StaveInterface lFirstStave = getFirstStave();
		return lFirstStave.getNumberOfTimePoints();
	}
	
	public void requestUpdateAllStaves()
	{
		for (StaveInterface lStave : mStaveListArray)
		{
			lStave.requestUpdate();
		}
	}

	@Override
	public double getDurationInMilliseconds()
	{
		return StaveAbstract.cMaximumNumberOfTimePointsPerBuffer*(getDeltaTimeInMicroseconds()*0.001);
	}

	@Override
	public String toString()
	{
		return String.format("Movement[%s]", getName());
	}







}
