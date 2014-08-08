package rtlib.gui.video.video2d.demo;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import rtlib.core.concurrent.thread.ThreadUtils;
import rtlib.core.variable.booleanv.BooleanVariable;
import rtlib.core.variable.doublev.DoubleVariable;
import rtlib.core.variable.objectv.ObjectVariable;
import rtlib.gui.swing.JButtonBoolean;
import rtlib.gui.swing.JSliderDouble;
import rtlib.gui.video.video2d.VideoFrame2DDisplay;
import rtlib.stack.Stack;

public class VideoFrame2DDisplayDemo extends JFrame
{

	static volatile boolean sDisplay = true;
	static volatile double sValue = 1;

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final VideoFrame2DDisplayDemo VideoFrame = new VideoFrame2DDisplayDemo();
					VideoFrame.setVisible(true);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		});

	}

	private final JPanel mcontentPane;

	/**
	 * Create the VideoFrame.
	 */
	public VideoFrame2DDisplayDemo()
	{
		// setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		mcontentPane = new JPanel();
		mcontentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		mcontentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(mcontentPane);

		final VideoFrame2DDisplay<Byte> lVideoDisplayDevice = new VideoFrame2DDisplay<Byte>(512,
																																						512);
		lVideoDisplayDevice.setLinearInterpolation(true);
		lVideoDisplayDevice.setSyncToRefresh(false);
		lVideoDisplayDevice.setVisible(true);

		final ObjectVariable<Stack<Byte>> lFrameVariable = lVideoDisplayDevice.getFrameReferenceVariable();

		final JSliderDouble lJSliderDouble = new JSliderDouble("gray value");
		mcontentPane.add(lJSliderDouble, BorderLayout.SOUTH);

		final JButtonBoolean lJButtonBoolean = new JButtonBoolean(false,
																															"Display",
																															"No Display");
		mcontentPane.add(lJButtonBoolean, BorderLayout.NORTH);

		final BooleanVariable lStartStopVariable = lJButtonBoolean.getBooleanVariable();

		lStartStopVariable.sendUpdatesTo(new DoubleVariable("StartStopVariableHook",
																												0)
		{
			@Override
			public double setEventHook(	final double pOldValue,
																	final double pNewValue)
			{
				final boolean lBoolean = BooleanVariable.double2boolean(pNewValue);
				sDisplay = lBoolean;
				System.out.println("sDisplay=" + sDisplay);
				return super.setEventHook(pOldValue, pNewValue);
			}
		});

		final DoubleVariable lDoubleVariable = lJSliderDouble.getDoubleVariable();

		final Stack<Byte> lFrame = new Stack<Byte>(	0L,
																					0L,
																					512,
																					512,
																					1,
																					Byte.class);

		lDoubleVariable.sendUpdatesTo(new DoubleVariable(	"SliderDoubleEventHook",
																											0)
		{

			@Override
			public double setEventHook(	final double pOldValue,
																	final double pNewValue)
			{
				sValue = pNewValue;
				System.out.println(pNewValue);

				// generateNoiseBuffer(lFrame.buffer, pNewValue);
				// lFrameVariable.setReference(lFrame);
				return super.setEventHook(pOldValue, pNewValue);
			}
		});

		Runnable lRunnable = () -> {
			while (true)
			{
				if (sDisplay)
				{
					// TODO: get teh bufer using KAM source!!
					// generateNoiseBuffer(lFrame.getByteBuffer(), sValue);
					lFrameVariable.setReference(lFrame);
				}
				ThreadUtils.sleep(1, TimeUnit.MILLISECONDS);
			}
		};

		Thread lThread = new Thread(lRunnable);
		lThread.setName(this.getName());
		lThread.setDaemon(true);
		lThread.start();

	}


}
