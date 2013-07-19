package variable.persistence.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import variable.doublev.DoubleVariable;
import variable.objectv.ObjectVariable;
import variable.persistence.DoubleVariableAsFile;
import variable.persistence.ObjectVariableAsFile;
import variable.persistence.VariableBundleAsFile;

public class VariableAsFileTests
{

	@Test
	public void testDoubleVariableAsFile() throws IOException,
																				InterruptedException
	{
		final File lTempFile = File.createTempFile(	"VariableAsFileTests",
																								"testDoubleVariableAsFile");
		final DoubleVariableAsFile lDoubleVariable1 = new DoubleVariableAsFile(	lTempFile,
																																						"x",
																																						1);

		lDoubleVariable1.setValue(2);
		Thread.sleep(100);
		final double lValue = lDoubleVariable1.getValue();

		assertEquals(2, lValue, 0.1);

		final DoubleVariableAsFile lDoubleVariable2 = new DoubleVariableAsFile(	lTempFile,
																																						"x",
																																						1);

		final double lValue2 = lDoubleVariable2.getValue();
		assertEquals(lValue, lValue2, 0.1);

		lDoubleVariable1.close();
		lDoubleVariable2.close();

	}

	@Test
	public void testObjectVariableAsFile() throws IOException,
																				InterruptedException
	{
		final File lTempFile = File.createTempFile(	"VariableAsFileTests",
																								"testObjectVariableAsFile");
		final ObjectVariableAsFile<String> lObjectVariable1 = new ObjectVariableAsFile<String>(	"x",
																																														lTempFile,
																																														"1");

		lObjectVariable1.setReference("2");
		Thread.sleep(100);

		final String lValue = lObjectVariable1.getReference();

		assertEquals("2", lValue);

		final ObjectVariableAsFile<String> lObjectVariable2 = new ObjectVariableAsFile<String>(	"y",
																																														lTempFile,
																																														"1");

		final String lValue2 = lObjectVariable2.getReference();
		assertEquals(lValue, lValue2);

		lObjectVariable1.close();
		lObjectVariable2.close();
	}

	@Test
	public void testVariableBundleAsFile() throws IOException,
																				InterruptedException
	{
		final File lTempFile = File.createTempFile(	"VariableAsFileTests",
																								"testVariableBundleAsFile");
		System.out.println(lTempFile);

		final DoubleVariable x1 = new DoubleVariable("x", 1);
		final ObjectVariable<String> y1 = new ObjectVariable<String>(	"y",
																																	"1");

		final VariableBundleAsFile lVariableBundleAsFile1 = new VariableBundleAsFile(	"bundle",
																																									lTempFile);

		lVariableBundleAsFile1.addVariable("path1.bla", x1);
		lVariableBundleAsFile1.addVariable("path2.blu", y1);

		x1.setValue(2);
		y1.setReference("3");
		Thread.sleep(100);

		final DoubleVariable x2 = new DoubleVariable("x", 1);
		final ObjectVariable<String> y2 = new ObjectVariable<String>(	"y",
																																	"1");

		final VariableBundleAsFile lVariableBundleAsFile2 = new VariableBundleAsFile(	"bundle",
																																									lTempFile);

		lVariableBundleAsFile2.addVariable("path1.bla", x2);
		lVariableBundleAsFile2.addVariable("path2.blu", y2);

		lVariableBundleAsFile2.read();

		assertEquals(x1.getValue(), x2.getValue(), 0.01);
		assertEquals(y1.getReference(), y2.getReference());

		lVariableBundleAsFile1.close();
		lVariableBundleAsFile2.close();

	}
}