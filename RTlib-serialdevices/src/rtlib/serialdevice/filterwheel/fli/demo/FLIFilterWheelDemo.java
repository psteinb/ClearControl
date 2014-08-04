package rtlib.serialdevice.filterwheel.fli.demo;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rtlib.core.variable.doublev.DoubleVariable;
import rtlib.serialdevice.filterwheel.fli.FLIFilterWheelDevice;

public class FLIFilterWheelDemo
{

	@Test
	public void test() throws InterruptedException
	{
		final FLIFilterWheelDevice lFLIFilterWheelDevice = new FLIFilterWheelDevice("COM9");

		assertTrue(lFLIFilterWheelDevice.open());

		final DoubleVariable lPositionVariable = lFLIFilterWheelDevice.getPositionVariable();
		final DoubleVariable lSpeedVariable = lFLIFilterWheelDevice.getSpeedVariable();

		for (int i = 0; i < 10000; i++)
		{
			int lTargetPosition = i % 10;
			lPositionVariable.set((double) lTargetPosition);
			lSpeedVariable.set((double) (i / 30));
			Thread.sleep(30);
			int lCurrentPosition = (int) lPositionVariable.getValue();
			System.out.format("i=%d, tp=%d, cp=%d\n",
												i,
												lTargetPosition,
												lCurrentPosition);
		}

		assertTrue(lFLIFilterWheelDevice.close());

	}

}