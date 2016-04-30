package clearcontrol.microscope.lightsheet.component.lightsheet.gui.jfx;

import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.gui.jfx.onoff.OnOffArrayPane;
import clearcontrol.gui.jfx.sliderpanel.CustomVariablePane;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheetInterface;

public class LightSheetPanel extends CustomVariablePane
{

	public LightSheetPanel(LightSheetInterface pLightSheetInterface)
	{
		super();

		addTab("DOFs");

		addSliderForVariable(	"X :",
													pLightSheetInterface.getXVariable(),
													5.0).setUpdateIfChanging(true);

		addSliderForVariable(	"Y :",
													pLightSheetInterface.getYVariable(),
													5.0).setUpdateIfChanging(true);

		addSliderForVariable(	"Z :",
													pLightSheetInterface.getZVariable(),
													5.0).setUpdateIfChanging(true);

		addSliderForVariable(	"Alpha :",
													pLightSheetInterface.getAlphaInDegreesVariable(),
													0.2).setUpdateIfChanging(true);

		addSliderForVariable(	"Beta :",
													pLightSheetInterface.getBetaInDegreesVariable(),
													0.2).setUpdateIfChanging(true);

		addSliderForVariable(	"Width :",
													pLightSheetInterface.getWidthVariable(),
													5.0).setUpdateIfChanging(true);

		addSliderForVariable(	"Height :",
													pLightSheetInterface.getHeightVariable(),
													5.0).setUpdateIfChanging(true);



		addSliderForVariable(	"Power :",
													pLightSheetInterface.getPowerVariable(),
													0.1).setUpdateIfChanging(true);

		OnOffArrayPane lLaserOnOffArray = addOnOffArray("Laser :");

		int lNumberOfLaserDigitalControls = pLightSheetInterface.getNumberOfLaserDigitalControls();
		for (int l = 0; l < lNumberOfLaserDigitalControls; l++)
		{
			lLaserOnOffArray.addSwitch(	"L" + l,
																	pLightSheetInterface.getLaserOnOffArrayVariable(l));
		}

		addTab("Advanced");
		
		addSliderForVariable(	"EffectiveExposure :",
													pLightSheetInterface.getEffectiveExposureInMicrosecondsVariable(),
													1.0,
													1000000.0,
													1.0,
													10000.0).setUpdateIfChanging(true);
		
		addSliderForVariable(	"LineExposure :",
													pLightSheetInterface.getLineExposureInMicrosecondsVariable(),
													1.0,
													1000000.0,
													1.0,
													10000.0).setUpdateIfChanging(true);
		
		addSliderForVariable(	"Overscan :",
													pLightSheetInterface.getOverScanVariable(),
													0.0,
													1.0,
													1.0,
													0.01).setUpdateIfChanging(true);
		
		addSliderForVariable(	"Readout Time :",
													pLightSheetInterface.getReadoutTimeInMicrosecondsPerLineVariable(),
													0.0,
													10.0,
													0.0,
													1.0).setUpdateIfChanging(true);
		

		
		
		addTab("Functions");

		addFunctionPane("X function", pLightSheetInterface.getXFunction());
		addFunctionPane("Y function", pLightSheetInterface.getYFunction());
		addFunctionPane("Z function", pLightSheetInterface.getZFunction());
		
		addFunctionPane("Alpha function", pLightSheetInterface.getAlphaFunction());
		addFunctionPane("Beta function", pLightSheetInterface.getBetaFunction());
		
		addFunctionPane("Width function", pLightSheetInterface.getWidthFunction());
		addFunctionPane("Height function", pLightSheetInterface.getHeightFunction());
		
		addFunctionPane("Power function", pLightSheetInterface.getPowerFunction());

	}

}