package score;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import score.interfaces.MovementInterface;

public class CompiledScore
{
	private final ArrayList<CompiledMovement> mCompiledMovementList = new ArrayList<CompiledMovement>();
	private final String mName;

	private volatile boolean mIsUpToDate = false;
	private IntBuffer mDeltaTimeShortBuffer;
	private IntBuffer mSyncShortBuffer;
	private IntBuffer mNumberfOfTimePointsBuffer;
	private ShortBuffer mMatricesShortBuffer;
	private final double mBufferDeltaTimeUnitInNanoseconds;

	public CompiledScore(	final String pName,
												final double pBufferDeltaTimeUnitInNanoseconds)
	{
		mName = pName;
		mBufferDeltaTimeUnitInNanoseconds = pBufferDeltaTimeUnitInNanoseconds;
	}

	public void addMovement(final MovementInterface pMovement)
	{
		final CompiledMovement lCompiledMovement = new CompiledMovement(pMovement);
		mCompiledMovementList.add(lCompiledMovement);
		mIsUpToDate = false;
	}

	public void clear()
	{
		mCompiledMovementList.clear();
		mIsUpToDate = false;
	}

	public ArrayList<CompiledMovement> getMovements()
	{
		return mCompiledMovementList;
	}

	public int getNumberOfMovements()
	{
		return mCompiledMovementList.size();
	}

	public IntBuffer getDeltaTimeBuffer()
	{
		ensureBuffersAreUpToDate();
		return mDeltaTimeShortBuffer;
	}

	public IntBuffer getSyncBuffer()
	{
		ensureBuffersAreUpToDate();
		return mSyncShortBuffer;
	}

	public IntBuffer getNumberOfTimePointsBuffer()
	{
		ensureBuffersAreUpToDate();
		return mNumberfOfTimePointsBuffer;
	}

	public ShortBuffer getScoreBuffer()
	{
		ensureBuffersAreUpToDate();
		return mMatricesShortBuffer;
	}

	private void ensureBuffersAreUpToDate()
	{
		if (mIsUpToDate)
		{
			return;
		}

		final int lNumberOfMatrices = mCompiledMovementList.size();

		final int lDeltaTimeBufferLengthInBytes = 4 * lNumberOfMatrices;

		if (mDeltaTimeShortBuffer == null || mDeltaTimeShortBuffer.capacity() < lDeltaTimeBufferLengthInBytes)
		{
			mDeltaTimeShortBuffer = ByteBuffer.allocateDirect(lDeltaTimeBufferLengthInBytes)
																				.order(ByteOrder.nativeOrder())
																				.asIntBuffer();
		}

		final int lSyncBufferLengthInBytes = 4 * lNumberOfMatrices;

		if (mSyncShortBuffer == null || mSyncShortBuffer.capacity() < lSyncBufferLengthInBytes)
		{
			mSyncShortBuffer = ByteBuffer.allocateDirect(lSyncBufferLengthInBytes)
																		.order(ByteOrder.nativeOrder())
																		.asIntBuffer();
		}

		final int lNumberOfTimePointsBufferLengthInBytes = 4 * lNumberOfMatrices;

		if (mNumberfOfTimePointsBuffer == null || mNumberfOfTimePointsBuffer.capacity() < lNumberOfTimePointsBufferLengthInBytes)
		{
			mNumberfOfTimePointsBuffer = ByteBuffer.allocateDirect(lNumberOfTimePointsBufferLengthInBytes)
																							.order(ByteOrder.nativeOrder())
																							.asIntBuffer();
		}

		final int lMatricesBufferLengthInBytes = Movement.cDefaultNumberOfStavesPerMovement * getTotalNumberOfTimePoints()
																							* 2;

		if (mMatricesShortBuffer == null || mMatricesShortBuffer.capacity() < lMatricesBufferLengthInBytes)
		{
			mMatricesShortBuffer = ByteBuffer.allocateDirect(lMatricesBufferLengthInBytes)
																				.order(ByteOrder.nativeOrder())
																				.asShortBuffer();
		}

		mDeltaTimeShortBuffer.clear();
		mSyncShortBuffer.clear();
		mNumberfOfTimePointsBuffer.clear();
		mMatricesShortBuffer.clear();

		for (final CompiledMovement lCompiledMovement : mCompiledMovementList)
		{
			mDeltaTimeShortBuffer.put((int) (lCompiledMovement.getDeltaTimeInMicroseconds() * 1000 / mBufferDeltaTimeUnitInNanoseconds));

			final byte lSyncMode = (byte) (lCompiledMovement.isSync()	? 0
																																: lCompiledMovement.isSyncOnRisingEdge() ? 1
																																																				: 2);
			final byte lSyncChannel = (byte) lCompiledMovement.getSyncChannel();
			final int lSync = twoBytesToShort(lSyncChannel, lSyncMode);
			mSyncShortBuffer.put(lSync);

			mNumberfOfTimePointsBuffer.put(lCompiledMovement.getNumberOfTimePoints());

			final ShortBuffer lMovementBuffer = lCompiledMovement.getMovementBuffer();
			lMovementBuffer.rewind();

			mMatricesShortBuffer.put(lMovementBuffer);
		}

		mDeltaTimeShortBuffer.flip();
		mSyncShortBuffer.flip();
		mNumberfOfTimePointsBuffer.flip();
		mMatricesShortBuffer.flip();

		mIsUpToDate = true;
	}

	private int getTotalNumberOfTimePoints()
	{
		int lTotalNumberOfTimePoints = 0;
		for (final CompiledMovement lCompiledMovement : mCompiledMovementList)
		{
			lTotalNumberOfTimePoints += lCompiledMovement.getNumberOfTimePoints();
		}
		return lTotalNumberOfTimePoints;
	}

	@Override
	public String toString()
	{
		return String.format("CompiledScore-%s", mName);
	}

	private static short twoBytesToShort(	final byte pHigh,
																				final byte pLow)
	{
		final short lShort = (short) (pHigh << 8 | pLow & 0xFF);
		return lShort;
	}

}
