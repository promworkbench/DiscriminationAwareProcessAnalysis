package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.prediction.Augmentation.Augmentation;

import com.fluxicon.slickerbox.components.NiceDoubleSlider;
import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import csplugins.id.mapping.ui.CheckComboBox;

// this is the JPaanel that takes the parameters and the configuration related to the decision tree
// if it is decision tree or regression tree
// number of items in each leaf
// and so on
public class ConfigurationPanel extends JPanel implements ViewInteractionPanel {
	
	public final static String notAllowedChars=".()&!|=<>-+*/% ";
//	private final JCheckBox pruneBox;
	private final NiceDoubleSlider confThreshold;
	private final NiceIntegerSlider minNumInstancePerLeaf;
	private final NiceIntegerSlider epsilon;
//	private final NiceIntegerSlider numFoldErrorPruning;
//	private final JCheckBox saveDataBox;
//	private final JCheckBox binaryBox;
	private JComboBox outputAttribCbx = SlickerFactory.instance().createComboBox(new String[]{"Trace Attribute", "Choice", "Sub_model"});
	private JComboBox sensitiveAttribCbx;
	private Augmentation lastSelectedAttribute=null;
	private Augmentation lastSelectedSensitiveAttribute = null;
	private DecisionTreePanel frame;
	private JRadioButton NDCtoDC;
	private JRadioButton DCtoNDC;
	private JRadioButton noLimit;
//	private JRadioButton normVisBtn;
//	private JRadioButton prefuseVisBtn;
	private int numInstances;
	private String dependentVariableType = null;
	private String wholeTraceAttribute = null;
	private String dependentChoicePlace = null;
	private String[][] dependentTransitions = null;
	private String outputAttName;
	private String sensitiveAttName;
	private boolean fromVarPanel = true;

	public ConfigurationPanel(final DecisionTreePanel frame, int numInstances)
	{
		this.frame=frame;
		
		
		
		SlickerFactory instance=SlickerFactory.instance();
//		decRBtn=instance.createRadioButton("Decision Tree");
//		decRBtn.setSelected(true);

//		decRBtn.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				binaryBox.setEnabled(true);
//				saveDataBox.setEnabled(true);
//				frame.setRegressionTree(false);
//			}
//		});
		
		
//		regRBtn=instance.createRadioButton("Decision/Regression Tree");
//		regRBtn.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				binaryBox.setEnabled(false);
//				binaryBox.setSelected(false);
//				saveDataBox.setEnabled(false);
//				saveDataBox.setSelected(false);
//				frame.setRegressionTree(true);
//			}
//		});	
	
		NDCtoDC = instance.createRadioButton("nonDesirable to Desirable relabeling");
		NDCtoDC.setSelected(false);
		NDCtoDC.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				frame.getPredictor().setNDCtoDC(1);
			}
		});
		DCtoNDC = instance.createRadioButton("Desirable to nonDesirable relabeling");
		DCtoNDC.setSelected(false);
		DCtoNDC.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				frame.getPredictor().setNDCtoDC(-1);
			}
		});
		noLimit = instance.createRadioButton("no relabeling limit");
		noLimit.setSelected(true);
	    noLimit.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				frame.getPredictor().setNDCtoDC(0);
			}
		});
		
		ButtonGroup bgrp=new ButtonGroup();
		bgrp.add(NDCtoDC);
		bgrp.add(DCtoNDC);
		bgrp.add(noLimit);
		
//		normVisBtn=instance.createRadioButton("Helicopter Visualization");
//		normVisBtn.setSelected(true);
		
//		prefuseVisBtn=instance.createRadioButton("Explorative Visualization");
//		prefuseVisBtn.setSelected(true);
		
//		bgrp=new ButtonGroup();
//		bgrp.add(normVisBtn);
//		bgrp.add(prefuseVisBtn);
		
//		pruneBox = instance.createCheckBox("Prune Tree", true);
//		binaryBox = instance.createCheckBox("Binary Tree", false);
//		saveDataBox = instance.createCheckBox("Instance data to be associated with tree's elements (slow!)", false);
		epsilon = instance.createNiceIntegerSlider("Set the epsilon", 0, 100, 20, Orientation.HORIZONTAL);
		confThreshold = instance.createNiceDoubleSlider("Set confidence threshold for pruning", 0.1, 1, 0.25, Orientation.HORIZONTAL);
		minNumInstancePerLeaf = instance.createNiceIntegerSlider("Minimum Number of instances per leaf (%)", 2, 100, 5, Orientation.HORIZONTAL);
	//	numFoldErrorPruning = instance.createNiceIntegerSlider("Number of folds for reduced error pruning", 2, 10, 2, Orientation.HORIZONTAL);
		
		this.numInstances=numInstances;
		
		setLayout(new GridLayout(13,1));
		Set<String> attributeNames = frame.getPredictor().getTraceAttributeValuesMap().keySet();
		for (String s : attributeNames) {
			System.out.println("attributeNames : "+s);
		}
		add(SlickerFactory.instance().createLabel("Sensitive Attribute:"));
		sensitiveAttribCbx = SlickerFactory.instance().createComboBox(frame.getPredictor().getTraceAttributeValuesMap().keySet().toArray());
		add(sensitiveAttribCbx);
		add(SlickerFactory.instance().createLabel("Dependent Attribute:"));
	//	outputAttribCbx = SlickerFactory.instance().createComboBox(new String[]{"Trace Attribute", "Choice", "Sub_model"});
		add(outputAttribCbx);	
		add(NDCtoDC);
		add(DCtoNDC);
		add(noLimit);

		RoundedPanel algorithmPnl=new RoundedPanel();
		algorithmPnl.setLayout(new FlowLayout(FlowLayout.CENTER));
//		algorithmPnl.add(decRBtn);
//		algorithmPnl.add(regRBtn);
		add(algorithmPnl);
		RoundedPanel visMethodPnl = new RoundedPanel();
		visMethodPnl.setLayout(new FlowLayout(FlowLayout.CENTER));
//		visMethodPnl.add(normVisBtn);
//		visMethodPnl.add(prefuseVisBtn);
		add(visMethodPnl);
//		add(pruneBox);
//		add(binaryBox);
//		add(saveDataBox);
		add(epsilon);
		add(confThreshold);
		add(minNumInstancePerLeaf);
	//	add(numFoldErrorPruning);
		JPanel panel=new JPanel();
		JButton button=SlickerFactory.instance().createButton("Update Decision Tree");
		button.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				try {
					frame.setEnabled(false);
					
					TaskForProgressBar task1=new TaskForProgressBar(null,"CP 154 Learning Decision Tree","",0,100) {

						protected Void doInBackground() throws Exception {
							frame.createPanel(this);
							return null;
						}

						protected void done() {
							frame.setEnabled(true);
						}

					};

					task1.execute();					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		});;
		panel.add(button);
		add(panel);
		
		outputAttribCbx.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				if (fromVarPanel) {
					fromVarPanel = false;
					return;
				}
				if (outputAttribCbx.getSelectedItem() instanceof Augmentation) {
					//-->
					//here we seperate trace and event augmentations. for this application Choice is considered as a trace augmentation
					LinkedList<Augmentation> traceAugs = new LinkedList<Augmentation>();
				    LinkedList<Augmentation> eventAugs = new LinkedList<Augmentation>();
				    
				    Set<Augmentation> AugmentationSet = new HashSet<Augmentation>();
				    AugmentationSet.addAll(frame.getVariablePanel().getFinalSelectedAugmentations());
				    
				    for (Augmentation aug :AugmentationSet) {
				    	String augName = aug.getAttributeName();
				    	if (augName.equals("trace_delay") || augName.equals("trace_duration") || augName.equals("Sub_Model_Attribute")
				    			|| (augName.length() > 6 && augName.substring(0,7).equals("Choice_")) || frame.getPredictor().getTraceAttributeNames().contains(augName)) {
				    		traceAugs.add(aug);
				    	} else {
				    		eventAugs.add(aug);
				    	}
				      }
					
					String outputAttributeName =((Augmentation) outputAttribCbx.getSelectedItem()).getAttributeName();
					outputAttName = getRealAttNameIfChoice(outputAttributeName);
					String selectedActivity = null;
					// <--
					Map<String, Type> attTypes = frame.getPredictor().getTypes();
					setOutputAttribute((Augmentation) outputAttribCbx.getSelectedItem());
					System.out.println(outputAttName);
					System.out.println(":D :D"); 
					boolean isClassAttAnEventAug = false;
				    
				    
				    // if the selected target aug is an event attribute, ask which activity does it belong
					String selectedItemName = ((Augmentation) outputAttribCbx.getSelectedItem()).getAttributeName();
				    System.out.println("new output  "+selectedItemName);
				    if (eventAugs.contains(outputAttribCbx.getSelectedItem()))
				    {
				    	isClassAttAnEventAug = true;
				    	JPanel p=new JPanel(new BorderLayout());
				    	LinkedList<String> activitiesToConsider = new LinkedList<String>();
				   // 	for(String s : frame.getPredictor().getActivitiesToConsider())
				   // 	{			
				   // 		activitiesToConsider.add(s);
				   // 		System.out.println("activitiesToConsider"+s);
				   // 	}
				    	JComboBox cbb=SlickerFactory.instance().createComboBox(frame.getPredictor().getActivitiesToConsider().toArray());
				    	Dimension dim=cbb.getPreferredSize();
				    	dim.width*=2;
				    	cbb.setPreferredSize(dim);
				    	p.add(cbb,BorderLayout.CENTER);
				    	p.add(new JLabel("The Dependent Attribute Belong To Which Activity?"),BorderLayout.NORTH);
				    	int yn=JOptionPane.showConfirmDialog(null, 
				    			p,"Dependent Attribute : "+selectedItemName,JOptionPane.YES_NO_OPTION);
				    	if (yn==JOptionPane.NO_OPTION)
				    		return;
				    	String actionName = (String) cbb.getSelectedItem();
				    	setGoodOutcomes (replaceNotAllowedStrings(actionName), true, false);
				    	frame.getPredictor().setTargetActivityName((String) cbb.getSelectedItem());
				    	frame.getPredictor().setDependentAttName(((String) cbb.getSelectedItem())+"_"+selectedItemName);
				    	selectedActivity = (String) cbb.getSelectedItem();
				    } else if ((!isClassAttAnEventAug || 
				    		((Augmentation)outputAttribCbx.getSelectedItem()).getAttributeName().substring(0, 7).equals("Choice_"))) {
				    	if ( sensitiveAttName != null) {
					    	setGoodOutcomes ("dummy", false, false);
				    	}  
				    	selectedActivity = null;
				    	frame.getPredictor().setTargetActivityName(null);
				    	frame.getPredictor().setDependentAttName(selectedItemName);
				    }
				    
				  
				    
				    setOutputAttribute((Augmentation) outputAttribCbx.getSelectedItem());
					System.out.println(outputAttName);
					System.out.println(":D");
				}
			}
		});
		// -->
		sensitiveAttribCbx.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				
				if (sensitiveAttribCbx.getSelectedItem() instanceof Augmentation) {
					//-->
					LinkedList<Augmentation> traceAugs = new LinkedList<Augmentation>();
				    LinkedList<Augmentation> eventAugs = new LinkedList<Augmentation>();
				    
				    Set<Augmentation> AugmentationSet = new HashSet<Augmentation>();
				    AugmentationSet.addAll(frame.getVariablePanel().getFinalSelectedAugmentations());
				    
				    for (Augmentation aug :AugmentationSet) {
				    	String augName = aug.getAttributeName();
				    	if (augName.equals("trace_delay") || augName.equals("trace_duration") || augName.equals("Sub_Model_Attribute")
				    			|| (augName.length() > 6 && augName.substring(0,7).equals("Choice_")) || frame.getPredictor().getTraceAttributeNames().contains(augName)) {
				    		traceAugs.add(aug);
				    	} else {
				    		eventAugs.add(aug);
				    	}
				      }
					
					String sensitiveAttributeName =((Augmentation) sensitiveAttribCbx.getSelectedItem()).getAttributeName();
					sensitiveAttName = getRealAttNameIfChoice(sensitiveAttributeName);
					String selectedActivity = null;
					// <--
					Map<String, Type> attTypes = frame.getPredictor().getTypesNDC();
					setSensitiveAttName(sensitiveAttName);
					System.out.println("sensitiveAttNam: " + sensitiveAttName);
					System.out.println(":D :D"); 
				    
				    
				    // if the selected target aug is an event attribute, ask which activity does it belong
					String selectedItemName = ((Augmentation) sensitiveAttribCbx.getSelectedItem()).getAttributeName();
				    System.out.println("new sensitive att :  "+selectedItemName);
				    if (eventAugs.contains(sensitiveAttribCbx.getSelectedItem()))
				    {
				    	JPanel p=new JPanel(new BorderLayout());
				    	LinkedList<String> activitiesToConsider = new LinkedList<String>();
				    	JComboBox cbb=SlickerFactory.instance().createComboBox(frame.getPredictor().getActivitiesToConsider().toArray());
				    	Dimension dim=cbb.getPreferredSize();
				    	dim.width*=2;
				    	cbb.setPreferredSize(dim);
				    	p.add(cbb,BorderLayout.CENTER);
				    	p.add(new JLabel("The Sensitive Attribute Belong To Which Activity?"),BorderLayout.NORTH);
				    	int yn=JOptionPane.showConfirmDialog(null, 
				    			p,"Sensitive Attribute : "+selectedItemName,JOptionPane.YES_NO_OPTION);
				    	if (yn==JOptionPane.NO_OPTION)
				    		return;
				    	String actionName = (String) cbb.getSelectedItem();
				    	setGoodOutcomes (replaceNotAllowedStrings(actionName), true, true);
				    //  frame.getPredictor().setTargetActivityName((String) cbb.getSelectedItem());
				    	frame.getPredictor().setSensitiveAttrebutName(((String) cbb.getSelectedItem())+"_"+selectedItemName);
				    	selectedActivity = (String) cbb.getSelectedItem();
				    } else {
				    	setGoodOutcomes ("dummy", false, true);
				    	frame.getPredictor().setSensitiveAttrebutName(sensitiveAttName);
				    	System.out.println("sensitiveAttName : "+ sensitiveAttName);
				    }		
					System.out.println(":D");
				}
			}
		});
	}
	
	public void setGoodOutcomes(String actionName, boolean isEventAtt, final boolean isSensitiveAtt) {
		String attName = new String();
		if (isEventAtt) {
			if (isSensitiveAtt) {
				attName = actionName+"_"+sensitiveAttName;
			} else {
				attName = actionName+"_"+outputAttName;
			}
		} else {
			if (isSensitiveAtt) {
				attName = sensitiveAttName;
			} else {
				attName = outputAttName;
			}
		}
		Map<String, Type> types = frame.getPredictor().getTypesNDC();
		Type type = frame.getPredictor().getTypesNDC().get(attName);
    	if ( type != null) {
			switch (type) {
				case LITERAL :
					Set<String> allValues = frame.getPredictor().getLiteralValuesNDC().get(attName);

					int num = allValues.size();
					while (num == allValues.size()) {
						JPanel p=new JPanel(new BorderLayout());
				    	LinkedList<String> listAllValues = new LinkedList<String>();
				    	for(String s : allValues)
				    	{			
				    		listAllValues.add(s);
				    	}
				    	Collections.sort(listAllValues);
				    	CheckComboBox cbb=new CheckComboBox(listAllValues);
				    	Dimension dim=cbb.getPreferredSize();
				    	dim.width*=2;
				    	cbb.setPreferredSize(dim);
				    	p.add(cbb,BorderLayout.CENTER);
				    	String title = new String();
				    	String message = new String();
				    	if (isSensitiveAtt) {
				    		title = "The Protected Values";
				    		message = "Select the protected value(s)";
				    	} else {
				    		title = "The Desirable Dependent Values";
				    		message = "The Desirable Dependent Values";
				    	}
				    	p.add(new JLabel(title),BorderLayout.NORTH);
				    	int yn=JOptionPane.showConfirmDialog(null, 
				    			p,message,JOptionPane.YES_NO_OPTION);
				    	if (yn==JOptionPane.NO_OPTION)
				    		return;
				    	if (cbb.getSelectedItems() != null && cbb.getSelectedItems().size() < allValues.size()) {
				    		if (isSensitiveAtt) {
				    			frame.getPredictor().setProtectedValues(cbb.getSelectedItems());
				    		} else {
				    			frame.getPredictor().setDesirableOutcome(cbb.getSelectedItems());
				    		}
				    		
				    		num = 0;
				    	}
				    	else {
				    		JPanel error =new JPanel(new BorderLayout());
				    		error.add(new JLabel("All values can not be chosen!"),BorderLayout.NORTH);
					    	int y=JOptionPane.showConfirmDialog(null, 
					    			p,"Try again",JOptionPane.OK_OPTION);
				    	}
					}
					break;

				case TIMESTAMP :
				case DISCRETE : // Do the same as case CONTINOUS
				case CONTINUOS :
					SlickerFactory newInstance=SlickerFactory.instance();
					NiceIntegerSlider ratio = newInstance.createNiceIntegerSlider("Set the threshold for dependente value %", 0, 100, 50, Orientation.HORIZONTAL);
					JRadioButton below = new JRadioButton("Below threshold", false);
					JRadioButton above = new JRadioButton("Above threshold", true);
					ButtonGroup group;
					group = new ButtonGroup();
			    	group.add(below);
			    	group.add(above);
			    	below.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							Set<String> dr = new HashSet<String>();
				    		dr.add("below");
				    		if (isSensitiveAtt) {
				    			frame.getPredictor().setProtectedValues(dr);
				    		} else {
				    			frame.getPredictor().setDesirableOutcome(dr);
				    		}
						}
			    	});
			    	above.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							Set<String> dr = new HashSet<String>();
				    		dr.add("above");
				    		if (isSensitiveAtt) {
				    			frame.getPredictor().setProtectedValues(dr);
				    		} else {
				    			frame.getPredictor().setDesirableOutcome(dr);
				    		}
						}
			    	});
					JPanel p=new JPanel(new GridBagLayout());
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.insets = new Insets(2, 2, 2, 2);
					constraints.gridwidth = 6;
					constraints.weightx = 1;
					constraints.weighty = 1;
					constraints.fill = GridBagConstraints.BOTH;
					constraints = new GridBagConstraints();
					p.add(ratio, constraints);
					constraints.insets = new Insets(2,2,2,2);
					constraints.gridwidth = 1;
					constraints.gridx = 3;
					constraints.gridy = 3;
					p.add(below, constraints);
					constraints = new GridBagConstraints();
					constraints.insets = new Insets(2,2,2,2);
					constraints.gridwidth = 1;
					constraints.gridx = 1;
					constraints.gridy = 3;
					p.add(above, constraints);
					JLabel label = new JLabel("The Desirable Dependent Values");
					constraints = new GridBagConstraints();
					constraints.insets = new Insets(2,2,2,2);
					constraints.gridwidth = 3;
					constraints.gridx = 1;
					constraints.gridy =2 ;
					p.add(label, constraints);
			    	int yn=JOptionPane.showConfirmDialog(null, 
			    			p,"Select the desirable outcome(s)",JOptionPane.YES_NO_OPTION);
			    	if (yn==JOptionPane.NO_OPTION)
			    		return;
			    	
			    	if (isSensitiveAtt) {
			    		frame.getPredictor().setSensitiveThreshold(ratio.getValue());
			    	} else {
			    		frame.getPredictor().setEventTargetThreshold(ratio.getValue());
			    	}
			    	break;
				case BOOLEAN :
					JPanel pBoolean=new JPanel(new BorderLayout());
			    	LinkedList<String> listAllValuesBoolean = new LinkedList<String>();
			    	listAllValuesBoolean.add("true");		
			    	listAllValuesBoolean.add("False");
			    	CheckComboBox cbbBoolean=new CheckComboBox(listAllValuesBoolean);
			    	Dimension dimBoolean=cbbBoolean.getPreferredSize();
			    	dimBoolean.width*=2;
			    	cbbBoolean.setPreferredSize(dimBoolean);
			    	pBoolean.add(cbbBoolean,BorderLayout.CENTER);
			    	pBoolean.add(new JLabel("The Desirable Dependent Values"),BorderLayout.NORTH);
			    	int yb=JOptionPane.showConfirmDialog(null, 
			    			pBoolean,"Select the desirable outcome(s)",JOptionPane.YES_NO_OPTION);
			    	if (yb==JOptionPane.NO_OPTION)
			    		return;
			    	if (isSensitiveAtt) {
		    			frame.getPredictor().setProtectedValues(cbbBoolean.getSelectedItems());
		    		} else {
		    			frame.getPredictor().setDesirableOutcome(cbbBoolean.getSelectedItems());
		    		}
			}
    	}
	}
	
	public void setChoiceOutputAttributePlace(String placeLabel, Augmentation attribute) {
		frame.setOutputAttribute(attribute);
		frame.setChoiceOutputAttributePlace(placeLabel);
		lastSelectedAttribute=attribute;
	}
	// <--
	public void setOutputAttribute(Augmentation attribute)
	{
		if (attribute!=lastSelectedAttribute)
		{
			frame.setOutputAttribute(attribute);
			lastSelectedAttribute=attribute;
		}		
	}
	
	public void setAttributeAugmentation(final Augmentation[] array)
	{
		try {
			frame.setEnabled(false);
			
			if (array.length==0 || !frame.configureAugmentation(array))
			{
				outputAttribCbx.removeAllItems();
				outputAttribCbx.addItem("Please augment the event log");
				sensitiveAttribCbx.removeAllItems();
				sensitiveAttribCbx.addItem("Please augment the event log");
				setSensitiveAttName(null);
				setOutputAttribute(null);
				frame.createPanel(null);
				frame.setEnabled(true);
				return;
			}
			
			
			final TaskForProgressBar task2=new TaskForProgressBar(frame,"CP 429 Preparation","",0,100) {

				protected Void doInBackground() throws Exception {
					outputAttribCbx.removeAllItems();
					sensitiveAttribCbx.removeAllItems();
					boolean lastSelectAttributeIsPresent=false;
					try
					{
						Arrays.sort(array, new Comparator<Augmentation>() {

							public int compare(Augmentation o1, Augmentation o2) {
								return o1.getAttributeName().compareToIgnoreCase(o2.getAttributeName());
							}

						});
					}catch(NullPointerException err)
					{
						err.printStackTrace();
					}
					ActionListener listener=outputAttribCbx.getActionListeners()[0];
					outputAttribCbx.removeActionListener(listener);
					ActionListener listenerS =sensitiveAttribCbx.getActionListeners()[0];
					sensitiveAttribCbx.removeActionListener(listenerS);
					for(Augmentation aug : array)
					{
			////		if (aug==lastSelectedAttribute)
			////			lastSelectAttributeIsPresent=true;
			////		if (frame.getPredictor().getLiteralValuesNDC().containsKey(aug.getAttributeName())) {
			////			if (frame.getPredictor().getLiteralValuesNDC().get(aug.getAttributeName()).size() > 1) {
			////				outputAttribCbx.addItem(aug);
			////				sensitiveAttribCbx.addItem(aug);
			////			}
			////		} else {
							outputAttribCbx.addItem(aug);
							sensitiveAttribCbx.addItem(aug);
			////		}
					}

					if (lastSelectAttributeIsPresent)
					{
						Augmentation aux = lastSelectedAttribute;
						lastSelectedAttribute=null;
						outputAttribCbx.setSelectedItem(aux);
						sensitiveAttribCbx.setSelectedItem(aux);
					}
					else if (array.length > 0) {
						outputAttribCbx.setSelectedIndex(0);
						sensitiveAttribCbx.setSelectedItem(0);
					}	
					
					outputAttribCbx.addActionListener(listener);
					sensitiveAttribCbx.addActionListener(listenerS);
					
					frame.createPanel(this);
					frame.setEnabled(true);
					return null;
				}				

			};

			TaskForProgressBar task1=new TaskForProgressBar(frame,"CP 471 Creating decision-tree training instances","",0,100) {

				private boolean outcome;

				protected Void doInBackground() throws Exception {
					outcome=frame.augmentLog(this);
					return null;
				}

				protected void done() {
					if (outcome)
						task2.execute();
					else
					{
						frame.setEnabled(true);
						myProgress(100);
					}

				}

			};

			task1.execute();




		} catch (Exception e) {
			e.printStackTrace();
			frame.setEnabled(true);
		}
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		// TODO Auto-generated method stub
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Configuration";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth();
	}

	public void setParent(ScalableViewPanel viewPanel) {

	}

	public void setScalableComponent(ScalableComponent scalable) {

	}

	public void willChangeVisibility(boolean to) {
		
	}

	public void updated() {

	}

	public boolean prunedTree() {
//		return pruneBox.isSelected();
		return true;
	}

	public float getConfidenceThreshold() {
		return (float) confThreshold.getValue();
	}
	
	public Integer getEpsilon() {
		return epsilon.getValue();
	}

	public double getMinNumInstancePerLeaf() {
		return minNumInstancePerLeaf.getValue()/100D;
	}

	public int getNumFoldErrorPruning() {
		return 10;
	}
	
	public boolean isRegressionTree()
	{
//		return regRBtn.isSelected();
		return false;
	}

	public boolean binaryTree() {
	//	return binaryBox.isSelected();
		return true;
	}

	public boolean saveData() {
//		return saveDataBox.isSelected();
		return true;
	}

	public boolean isNormalVisualizationSelected() {
//		return normVisBtn.isSelected();
		return true;
	}
	
	public void setSensitiveAttName(String attName) {
		sensitiveAttName = attName;
	}
	
	public String getRealAttNameIfChoice(String attName) {
		if (attName.length() >= 7 ) {
			if(attName.substring(0, 7).equals("Choice_")) {
				String[] s = attName.split("_to_", 2);
				String choicePlaceName = s[0].substring(7,s[0].length());
				return choicePlaceName;
			}
		}
		return attName;
	}
	
	public void setFromVarPanel(boolean b) {
		fromVarPanel = b;
	}
	
   	// removes the not allowed char for the consistency
   	public String replaceNotAllowedStrings(String str) {
   		char[] array=str.toCharArray();
		for(int i=0;i<array.length;i++)
		{
			if (notAllowedChars.indexOf(array[i])!=-1)
				array[i]='_';
		}
		return (new String(array));
   	}

}
