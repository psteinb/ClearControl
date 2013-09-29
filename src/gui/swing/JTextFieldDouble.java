package gui.swing;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import variable.doublev.DoubleVariable;

public class JTextFieldDouble extends JPanel
{
	private JLabel mNameLabel;
	private JTextField mValueTextField;

	private final String mLabelsFormatString;
	private double mMin, mMax;

	private final JTextFieldDouble mThis;
	private final DoubleVariable mDoubleVariable;

	private volatile double mNewValue;

	public JTextFieldDouble(final String pValueName,
													final boolean pNorthSouthLayout,
													final double pValue)
	{
		this(	pValueName,
					pNorthSouthLayout,
					Double.NEGATIVE_INFINITY,
					Double.POSITIVE_INFINITY,
					pValue);
	}

	public JTextFieldDouble(final String pValueName,
													final boolean pNorthSouthLayout,
													final double pMin,
													final double pMax,
													final double pValue)
	{
		this(pValueName, pNorthSouthLayout, 1024, pMin, pMax, pValue);
	}

	public JTextFieldDouble(final String pValueName,
													final boolean pNorthSouthLayout,
													final int pResolution,
													final double pMin,
													final double pMax,
													final double pValue)
	{
		this(	pValueName,
					pNorthSouthLayout,
					"%.1f",
					pResolution,
					pMin,
					pMax,
					pValue);
	}

	public JTextFieldDouble(final String pValueName,
													final boolean pNorthSouthLayout,
													final String pLabelsFormatString,
													final int pResolution,
													final double pMin,
													final double pMax,
													final double pValue)
	{
		super();

		mDoubleVariable = new DoubleVariable(pValueName, pValue)
		{
			@Override
			public double setEventHook(	final double pOldValue,
																	final double pNewValue)
			{

				if (pNewValue != mNewValue)
				{
					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							// System.out.println("mValueTextField.setText('' + pNewValue);");
							mValueTextField.setText("" + pNewValue);
						}
					});
				}

				return super.setEventHook(pOldValue, pNewValue);
			}
		};

		setLayout(new BorderLayout(0, 0));

		mNameLabel = new JLabel(pValueName);
		if (pNorthSouthLayout)
		{
			mNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		else
		{
			mNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
		}
		add(mNameLabel, pNorthSouthLayout	? BorderLayout.NORTH
																			: BorderLayout.WEST);

		mValueTextField = new JTextField("" + pValue);
		if (pNorthSouthLayout)
		{
			mNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		else
		{
			mNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
		}
		add(mValueTextField, pNorthSouthLayout ? BorderLayout.SOUTH
																					: BorderLayout.CENTER);

		mLabelsFormatString = pLabelsFormatString;
		mMin = pMin;
		mMax = pMax;
		mThis = this;

		mValueTextField.getDocument()
										.addDocumentListener(new DocumentListener()
										{
											@Override
											public void changedUpdate(final DocumentEvent e)
											{
												parseDoubleAndNotify();
											}

											@Override
											public void removeUpdate(final DocumentEvent e)
											{
												parseDoubleAndNotify();
											}

											@Override
											public void insertUpdate(final DocumentEvent e)
											{
												parseDoubleAndNotify();
											}

											public void parseDoubleAndNotify()
											{
												final String lTextString = mValueTextField.getText()
																																	.trim();
												if (lTextString.isEmpty())
												{
													return;
												}

												try
												{
													mNewValue = Double.parseDouble(lTextString);
													mDoubleVariable.setValue(mNewValue);

												}
												catch (final NumberFormatException e)
												{
													JOptionPane.showMessageDialog(null,
																												"String cannot be parsed as double!",
																												"Error Message",
																												JOptionPane.ERROR_MESSAGE);
													return;
												}
											}
										});

	}

	public DoubleVariable getDoubleVariable()
	{
		return mDoubleVariable;
	}

	private static double toDouble(	final int pResolution,
																	final double pMin,
																	final double pMax,
																	final int pIntValue)
	{
		return pMin + (double) pIntValue
						/ (pResolution - 1)
						* (pMax - pMin);
	}

	private static int toInt(	final int pResolution,
														final double pMin,
														final double pMax,
														final double pValue)
	{
		return (int) Math.round((pResolution - 1) * (clamp(	pMin,
																												pMax,
																												pValue) - pMin)
														/ (pMax - pMin));
	}

	private static double clamp(final double pMin,
															final double pMax,
															final double pValue)
	{
		return Math.min(pMax, Math.max(pMin, pValue));
	}

	public void setColumns(final int pNumberColumns)
	{
		mValueTextField.setColumns(pNumberColumns);
	}

	public void setValue(final double pValue)
	{
		mValueTextField.setText("" + pValue);
	}

}
