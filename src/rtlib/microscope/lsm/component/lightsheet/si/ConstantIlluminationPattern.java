package rtlib.microscope.lsm.component.lightsheet.si;

import rtlib.symphony.staves.IntervalStave;
import rtlib.symphony.staves.StaveInterface;

public class ConstantIlluminationPattern extends
																				StructuredIlluminationPatternBase	implements
																																					StructuredIlluminationPatternInterface
{

	private final IntervalStave mStave;

	public ConstantIlluminationPattern()
	{
		super();
		mStave = new IntervalStave("trigger.out.e", 0, 1, 1, 0);
	}

	@Override
	public StaveInterface getStave(double pMarginTimeRelativeUnits)
	{
		mStave.setStart((float) clamp01(pMarginTimeRelativeUnits));
		mStave.setStop((float) clamp01(1 - pMarginTimeRelativeUnits));
		return mStave;
	}

	@Override
	public int getNumberOfPhases()
	{
		return 1;
	}

}