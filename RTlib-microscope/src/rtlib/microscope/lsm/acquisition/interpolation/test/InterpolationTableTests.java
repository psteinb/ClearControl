package rtlib.microscope.lsm.acquisition.interpolation.test;

import static org.junit.Assert.*;

import org.junit.Test;

import rtlib.gui.plots.MultiPlot;
import rtlib.microscope.lsm.acquisition.interpolation.InterpolationTable;
import rtlib.microscope.lsm.acquisition.interpolation.Row;

public class InterpolationTableTests
{

	@Test
	public void test()
	{
		final InterpolationTable lInterpolationTable = new InterpolationTable(2);

		final Row lAddRow3 = lInterpolationTable.addRow(3.0);
		final Row lAddRow1 = lInterpolationTable.addRow(1.0);
		final Row lAddRow2 = lInterpolationTable.addRow(2.0);
		
		
		Row lRow = lInterpolationTable.getRow(1);
		System.out.println(lRow);
		assertTrue(lRow.getX()==2.0);
		

		lAddRow1.setY(0, 1);
		lAddRow2.setY(0, 2);
		lAddRow3.setY(0, 3);

		lAddRow1.setY(1, 0);
		lAddRow2.setY(1, 1);
		lAddRow3.setY(1, 0.5);

		System.out.println(lInterpolationTable.getInterpolatedValue(0,
																	1.2));

		System.out.println(lInterpolationTable.getInterpolatedValue(1,
																	1.2));

		final MultiPlot lDisplayTable = lInterpolationTable.displayTable("test");

		/*while (lDisplayTable.isVisible())
		{
			ThreadUtils.sleep(10L, TimeUnit.MILLISECONDS);
		}/**/

		assertEquals(	1.0,
						lInterpolationTable.getNearestValue(0, 1.2),
						0.01);

		assertEquals(	1.2,
						lInterpolationTable.getInterpolatedValue(	0,
																	1.2),
						0.01);

	}
}
