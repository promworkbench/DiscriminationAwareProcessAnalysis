package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.prediction.Augmentation.Augmentation;

public class DecisionTreePanel extends JPanel {
	private VariablePanel varPanel = null;
	private ScalableViewPanel tvScalable = null;

	private ConfigurationPanel confPanel;
	private Predictor predictor;
	private ActivitiesGrouper actPanel;
	private DiscretizationPanel discrPanel = null;
	private Augmentation outputAttribute = null;
	private int numIntervals = 10;
	private DiscrMethod method = DiscrMethod.EQUAL_WIDTH;
	private boolean recomputeIntervals;
	private PluginContext proMContext;
	private String choiceOutputAttributePlace; // <--
	
	public DecisionTreePanel(PluginContext context, Predictor predictor) throws Exception {
		this.predictor = predictor;
		this.proMContext = context;
		this.setLayout(new BorderLayout());
		List<String> attributes = predictor.getAttributes();
		confPanel = new ConfigurationPanel(this, predictor.getInstanceSetSize());
		varPanel = new VariablePanel(this, predictor.getTraceAttributeNames(),attributes, predictor.getActivities(), confPanel, predictor.getResReplay(),predictor.getTypes());
		actPanel = new ActivityPanel(predictor.getActivities(), varPanel);
		createPanel(null);  // calling the makePrediction method ==> creation of decision tree ==> Tree visualization
	}

	public void resampleOutputAttribute(int numIntervals, DiscrMethod method) {
		this.numIntervals = numIntervals;
		this.method = method;
		setOutputAttribute(outputAttribute);
		this.setEnabled(false);
		
		TaskForProgressBar task1=new TaskForProgressBar(this,"DTP 51 Learning Decision Tree","",0,100) {

			protected Void doInBackground() throws Exception {
				((DecisionTreePanel)component).createPanel(this);
				return null;
			}

			protected void done() {
				((DecisionTreePanel)component).setEnabled(true);
			}

		};

		task1.execute();

	}

	public void setOutputAttribute(Augmentation attribute) {
		this.outputAttribute = attribute;
		recomputeIntervals = true;
	}
	// -->
	public void setChoiceOutputAttributePlace(String choicePlace) {
		this.choiceOutputAttributePlace = choicePlace;
		recomputeIntervals = true;
	}
	
	public VariablePanel getVariablePanel() {
		return varPanel;
	}
	// <--
	
	public boolean augmentLog(TaskForProgressBar task) {
		


        final Augmentation[] augmentations=varPanel.getFinalSelectedAugmentations().toArray(new Augmentation[0]);
        if (augmentations==null)
        	return false;
        
        predictor.augmentLog(augmentations,varPanel.isMapDBSelected(),null);
        return predictor.augmentLogNDC(augmentations, false ,task);
	}
	
	public void setEnabled(boolean aFlag) {
		super.setEnabled(aFlag);
		confPanel.setEnabled(aFlag);
		if (discrPanel!=null)
		{
			discrPanel.setEnabled(aFlag);
			setEnabled(discrPanel,false);
		}
		if (varPanel!=null)
		{
			varPanel.setEnabled(aFlag);
			setEnabled(varPanel,aFlag);
		}
		if (tvScalable!=null)
		{
			tvScalable.setEnabled(aFlag);
			setEnabled(tvScalable,aFlag);
		}
		setEnabled(confPanel,aFlag);
		
	};
	
	

	private void setEnabled(Container container, boolean aFlag) {
		for(Component comp : container.getComponents())
		{
			comp.setEnabled(aFlag);
			if (comp instanceof Container)
				setEnabled((Container) comp,aFlag);
		}
		
	}
	// creates the whole panel using activity, configuration and discretization panel components 
	// set some of the private variables of predictor class
	// visualize the decision tree
	
	@SuppressWarnings("unchecked")
	public void createPanel(TaskForProgressBar task) throws Exception {
		JComponent tvaux;
		boolean ok = false;
		if(task!=null)
			task.myProgress(0);
		if (tvScalable != null)
			this.remove(tvScalable);
		if (predictor.getTypesNDC().isEmpty()) {
			tvaux = new JLabel("Please select which attributes to consider first, using the settings on the right.");
			tvaux.setFont(tvaux.getFont().deriveFont(24F).deriveFont(Font.BOLD));
			discrPanel = null;
		} else {
			try {
				if (recomputeIntervals) {
					DiscretizationInterval[] intervals = predictor.setOutputAttribute(outputAttribute, numIntervals,
							method, confPanel.isRegressionTree());
					if (intervals != null) {
						discrPanel = new DiscretizationPanel(this, intervals, numIntervals, method);
					} else
						discrPanel = null;
					recomputeIntervals = false;
				}
				predictor.setUnPruned(!confPanel.prunedTree());
				predictor.setConfidenceThreshold(confPanel.getConfidenceThreshold());
				predictor.setEpsilon(confPanel.getEpsilon());
				predictor.setMinNumInstancePerLeaf(confPanel.getMinNumInstancePerLeaf());
				predictor.setNumFolds(confPanel.getNumFoldErrorPruning());
				predictor.setBinarySplit(confPanel.binaryTree());
				predictor.setSaveData(confPanel.saveData());
				Collection<String> dummy = actPanel.getActivitiesToConsider();
				predictor.setActivitiesToConsider(actPanel.getActivitiesToConsider());
				predictor.makePrediction(task);
				if (confPanel.isNormalVisualizationSelected())
					//tvaux = predictor.getNormalTreeVisualization();
					tvaux = predictor.getFairAndNormalTreeVisualization();
				else
					//tvaux = predictor.getPrefuseTreeVisualization();
					tvaux = predictor.getFairAndNormalTreeVisualization();
				ok = true;
			} catch (weka.core.UnsupportedAttributeTypeException e) {
				tvaux = new JLabel(
						"Impossible to construct a decision tree: the dependent feature has only taken on one single value");
				tvaux.setFont(tvaux.getFont().deriveFont(24F).deriveFont(Font.BOLD));
			} catch (Exception e) {
				if (e.getMessage()!=null)
				{
					e.printStackTrace();
					tvaux = new JLabel("Please select the sensitive and the dependent attributes.");
				}
				else
				{
					if (outputAttribute==null)
						tvaux = new JLabel("Please select the sensitive and the dependent attributes.");
					else
					{
						if (predictor.getSensitiveAttrebute() == null)
						{
							tvaux = new JLabel("Select the sensitive and the dependent attributes!");	
						} 
						else 
						{
							tvaux = new JLabel("Impossible to construct a decision tree because of some internal error. Try to change"
									+ " the dependent feature");	
						}
						e.printStackTrace();
					}
				}
				tvaux.setFont(tvaux.getFont().deriveFont(24F).deriveFont(Font.BOLD));
			}
			
		}
		if (task!=null)
			task.myProgress(100);
		final JComponent tv = tvaux;
		//tv.setBackground(Color.GRAY);
		tvScalable = new ScalableViewPanel(new ScalableComponent() {

			public void setScale(double newScale) {
			}

			public void removeUpdateListener(UpdateListener listener) {
			}

			public double getScale() {
				return 1;
			}

			public JComponent getComponent() {
				return tv;
			}

			public void addUpdateListener(UpdateListener listener) {
			}
		});
		if (varPanel != null)
			tvScalable.addViewInteractionPanel(varPanel, SwingConstants.EAST);
		if (confPanel != null)
			tvScalable.addViewInteractionPanel(confPanel, SwingConstants.EAST);
		if (ok)
			tvScalable.addViewInteractionPanel(new Summary(predictor.getEvaluation()), SwingConstants.SOUTH);
		if (actPanel != null)
			tvScalable.addViewInteractionPanel(actPanel, SwingConstants.WEST);
		
		if (actPanel.getActivitiesToConsider().contains(Predictor.CASE_ACTIVITY))
			tvScalable.addViewInteractionPanel(new LogClusterPanel(proMContext, predictor), SwingConstants.WEST);
		this.add(tvScalable, BorderLayout.CENTER);
		if (discrPanel != null)
			tvScalable.addViewInteractionPanel(discrPanel, SwingConstants.EAST);
		this.validate();
		this.repaint();
	}

	public void setRegressionTree(boolean b) {
		if (b != predictor.isRegressionTree()) {
			predictor.setRegression(b);
			recomputeIntervals = true;
		}

	}

	public boolean configureAugmentation(Augmentation[] array) {
		return predictor.configureAugmentation(array);
		
	}
	//-->
	public Predictor getPredictor() {
		return predictor;
	}
	
	public PluginContext getContext() {
		return proMContext;
	}
	
	public ConfigurationPanel getConfigurationPanel() {
		return confPanel;
	}
	//<--
}
