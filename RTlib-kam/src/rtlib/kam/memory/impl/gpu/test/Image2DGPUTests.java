package rtlib.kam.memory.impl.gpu.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import rtlib.core.units.Magnitudes;
import rtlib.kam.context.impl.gpu.ContextGPU;
import rtlib.kam.memory.impl.gpu.Image2DGPU;
import coremem.memmap.FileMappedMemoryRegion;
import coremem.offheap.OffHeapMemoryRegion;
import coremem.util.SizeOf;


public class Image2DGPUTests
{
	private static final long cSizeX = 2048;
	private static final long cSizeY = 2047;

	private static final long cVolume = cSizeX * cSizeY;

	@Test
	public void testLifeCycle()
	{
		final ContextGPU lBestOpenCLContext = ContextGPU.getBestOpenCLContext();

		final Image2DGPU<Float> lImage2DGPU = new Image2DGPU<Float>(lBestOpenCLContext,
																																float.class,
																																true,
																																true,
																																cSizeX,
																																cSizeY);

		assertEquals(cSizeX, lImage2DGPU.getWidth());
		assertEquals(cSizeY, lImage2DGPU.getHeight());

		assertEquals(cVolume, lImage2DGPU.getVolume());

		assertEquals(cVolume, lImage2DGPU.getLengthInElements());

		assertEquals(	cVolume * SizeOf.sizeOfFloat(),
									lImage2DGPU.getSizeInBytes());

		assertFalse(lImage2DGPU.isFree());

		lImage2DGPU.free();

		assertTrue(lImage2DGPU.isFree());

	}

	@Test
	public void testReadWritePointerAccessible() throws InterruptedException
	{
		// System.out.println("testReadWritePointerAccessible().BEGIN");
		final ContextGPU lBestOpenCLContext = ContextGPU.getBestOpenCLContext();

		final Image2DGPU<Short> lImage2DGPU = new Image2DGPU<Short>(lBestOpenCLContext,
																																short.class,
																																true,
																																true,
																																cSizeX,
																																cSizeY);

		final OffHeapMemoryRegion lOffHeapMemoryRegion = new OffHeapMemoryRegion(cVolume * SizeOf.sizeOfShort());

		for (int i = 0; i < cVolume; i++)
			lOffHeapMemoryRegion.setShortAligned(i, (short) i);

		for (int i = 0; i < cVolume; i++)
		{
			final short lShort = lOffHeapMemoryRegion.getShortAligned(i);
			assertEquals((short) i, lShort);
		}

		lImage2DGPU.readFrom(lOffHeapMemoryRegion);
		lImage2DGPU.getCurrentQueue().waitForCompletion();
		// Thread.sleep(2000);

		for (int i = 0; i < cVolume; i++)
			lOffHeapMemoryRegion.setShortAligned(i, (short) 0);

		lImage2DGPU.writeTo(lOffHeapMemoryRegion);
		lImage2DGPU.getCurrentQueue().waitForCompletion();
		// Thread.sleep(2000);

		for (int i = 0; i < cVolume; i++)
		{
			final short lShort = lOffHeapMemoryRegion.getShortAligned(i);
			// System.out.println(lShort);
			assertEquals((short) i, lShort);
		}

		lImage2DGPU.free();

		assertTrue(lImage2DGPU.isFree());
		// System.out.println("testReadWritePointerAccessible().END");
	}

	// @Test
	public void testReadWriteToMappableFile()	throws IOException,
																						InterruptedException
	{
		final ContextGPU lBestOpenCLContext = ContextGPU.getBestOpenCLContext();

		final Image2DGPU<Short> lImage2DGPU = new Image2DGPU<Short>(lBestOpenCLContext,
																																short.class,
																																true,
																																true,
																																cSizeX,
																																cSizeY);

		final File lTempFile = File.createTempFile(	this.getClass()
																										.getSimpleName(),
																								"testWriteToMappableMemory");
		final FileMappedMemoryRegion lFileMappedMemoryRegion = new FileMappedMemoryRegion(	lTempFile,
																					cVolume * SizeOf.sizeOfShort());

		lImage2DGPU.readFromMapped(lFileMappedMemoryRegion);
		lImage2DGPU.writeToMapped(lFileMappedMemoryRegion);
		lFileMappedMemoryRegion.free();
		lImage2DGPU.free();

		assertTrue(lTempFile.exists());
		assertEquals(	cVolume * SizeOf.sizeOfShort(),
									Files.size(lTempFile.toPath()));
	}

	@Test
	public void testWritePerformance()
	{
		// System.out.println("testWritePerformance().BEGIN");
		final ContextGPU lBestOpenCLContext = ContextGPU.getBestOpenCLContext();

		final Image2DGPU<Byte> lImage2DGPU = new Image2DGPU<Byte>(lBestOpenCLContext,
																															byte.class,
																															true,
																															true,
																															cSizeX,
																															cSizeY);

		final OffHeapMemoryRegion lOffHeapMemoryRegion = new OffHeapMemoryRegion(cVolume * SizeOf.sizeOfByte());

		for (int i = 0; i < cVolume; i++)
			lOffHeapMemoryRegion.setByteAligned(i, (byte) i);

		final int lNumberOfCycles = 10;

		final long lStartNanos = System.nanoTime();
		for (int cycle = 0; cycle < lNumberOfCycles; cycle++)
		{
			lImage2DGPU.readFrom(lOffHeapMemoryRegion);
		}
		lImage2DGPU.getCurrentQueue().waitForCompletion();

		final long lStopNanos = System.nanoTime();

		final double lGigaBytesPerSecond = (Magnitudes.unit2giga(lNumberOfCycles * cVolume)) / Magnitudes.nano2unit(lStopNanos - lStartNanos);

		System.out.println("Image2DGPU write speed: " + lGigaBytesPerSecond
												+ " GB/s");

		assertTrue(lGigaBytesPerSecond > 0);

		lImage2DGPU.free();

		assertTrue(lImage2DGPU.isFree());
		// System.out.println("testWritePerformance().END");
	}

	@Test
	public void testReadPerformance()
	{
		// System.out.println("testReadPerformance().BEGIN");
		final ContextGPU lBestOpenCLContext = ContextGPU.getBestOpenCLContext();

		final Image2DGPU<Byte> lImage2DGPU = new Image2DGPU<Byte>(lBestOpenCLContext,
																															byte.class,
																															true,
																															true,
																															cSizeX,
																															cSizeY);

		final OffHeapMemoryRegion lOffHeapMemoryRegion = new OffHeapMemoryRegion(cVolume * SizeOf.sizeOfByte());

		for (int i = 0; i < cVolume; i++)
			lOffHeapMemoryRegion.setByteAligned(i, (byte) i);
		lImage2DGPU.readFrom(lOffHeapMemoryRegion);
		lImage2DGPU.getCurrentQueue().waitForCompletion();

		final int lNumberOfCycles = 10;

		final long lStartNanos = System.nanoTime();
		for (int cycle = 0; cycle < lNumberOfCycles; cycle++)
		{
			lImage2DGPU.writeTo(lOffHeapMemoryRegion);
		}
		lImage2DGPU.getCurrentQueue().waitForCompletion();

		final long lStopNanos = System.nanoTime();

		final double lGigaBytesPerSecond = (Magnitudes.unit2giga(lNumberOfCycles * cVolume)) / Magnitudes.nano2unit(lStopNanos - lStartNanos);

		System.out.println("Image2DGPU read speed: " + lGigaBytesPerSecond
												+ " GB/s");

		assertTrue(lGigaBytesPerSecond > 0);

		lImage2DGPU.free();

		assertTrue(lImage2DGPU.isFree());
		// System.out.println("testReadPerformance().END");
	}

}
