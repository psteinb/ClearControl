package rtlib.microscope.lightsheet.component.lightsheet.si;

import rtlib.hardware.signalgen.staves.ClosurePatternSteppingStave;
import rtlib.hardware.signalgen.staves.SteppingFunction;

public class ClosureStructuredIlluminationPattern	extends
																									GenericStructuredIlluminationPattern<ClosurePatternSteppingStave>	implements
																																																										StructuredIlluminationPatternInterface
{

	public ClosureStructuredIlluminationPattern(SteppingFunction pSteppingFunction,
																							int pNumberOfPhases)
	{
		super(new ClosurePatternSteppingStave("trigger.out.e",
																					pSteppingFunction),
					pNumberOfPhases);
	}

	public void setSteppingFunction(SteppingFunction pSteppingFunction)
	{
		setStave(new ClosurePatternSteppingStave(	"trigger.out.e",
																							pSteppingFunction));
	}

}