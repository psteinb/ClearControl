package rtlib.microscope.lsm.acquisition.test;

import org.junit.Test;

import rtlib.microscope.lsm.acquisition.StackAcquisition;

public class StackAcquisitionTests
{

	@Test
	public void test()
	{
		StackAcquisition lStackAcquisition = new StackAcquisition(null);
		
		lStackAcquisition.setup(-1, 0, 1, 0.1, 0.2, 0.05);
		
	}

}