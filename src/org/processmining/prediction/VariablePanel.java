package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.prediction.Augmentation.ActivityDuration;
import org.processmining.prediction.Augmentation.ActivityName;
import org.processmining.prediction.Augmentation.AttributeValue;
import org.processmining.prediction.Augmentation.Augmentation;
import org.processmining.prediction.Augmentation.ElapsedTime;
import org.processmining.prediction.Augmentation.EventNumber;
import org.processmining.prediction.Augmentation.Executor;
import org.processmining.prediction.Augmentation.Fitness;
import org.processmining.prediction.Augmentation.Group;
import org.processmining.prediction.Augmentation.NextActivity;
import org.processmining.prediction.Augmentation.NumberExecution;
import org.processmining.prediction.Augmentation.PreAttributeValue;
import org.processmining.prediction.Augmentation.PreviousActivity;
import org.processmining.prediction.Augmentation.RemainingTime;
import org.processmining.prediction.Augmentation.Resource;
import org.processmining.prediction.Augmentation.ResourceWorkload;
import org.processmining.prediction.Augmentation.Role;
import org.processmining.prediction.Augmentation.Timestamp;
import org.processmining.prediction.Augmentation.TotalResourceWorkload;
import org.processmining.prediction.Augmentation.traceAttributeValue;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

class MLTreeSelectionModel extends DefaultTreeSelectionModel {
	private static final long serialVersionUID = -4270031800448415780L;
	private VariablePanel variablePanel;

	public MLTreeSelectionModel(VariablePanel variablePanel) {
		this.variablePanel=variablePanel;
	}

	@Override
	public void addSelectionPath(TreePath path) {
		// Don't do overriding logic here because addSelectionPaths is ultimately called.
		super.addSelectionPath(path);
	}

	@Override
	public void addSelectionPaths(TreePath[] paths) {
		if(paths != null) {
			for(TreePath path : paths) {

				if (path.getLastPathComponent() instanceof Augmentation)
				{
					TreePath[] aux=new TreePath[]{path};

						if (!isPathSelected(path))
							super.addSelectionPaths(aux);
						else
							super.removeSelectionPaths(aux);
				}
				else
				{
					TreeNode node=(TreeNode) path.getLastPathComponent();
					ArrayList<TreePath> subPaths=new ArrayList<TreePath>();
					for(int i=0;i<node.getChildCount();i++)
					{
						TreeNode subNode=node.getChildAt(i);
						Object[] aux=Arrays.copyOf(path.getPath(), path.getPathCount()+1);
						aux[path.getPathCount()]=subNode;
						subPaths.add(new TreePath(aux));
					}
					addSelectionPaths(subPaths.toArray(new TreePath[0]));
				}

			}
		}
	}
}

class MyJTree extends JTree
{
	public MyJTree(DefaultMutableTreeNode root) {
		super(root);
		
	}

	@Override
	public void setSelectionPath(TreePath path) {
		
		addSelectionPath(path);
	}

	@Override
	public void setSelectionPaths(TreePath[] paths) {

		addSelectionPaths(paths);

		return;
	}

	@Override
	public void setSelectionRow(int row) {
		addSelectionRow(row);
	}
}


public class VariablePanel extends JPanel implements ViewInteractionPanel {

	private static final Color PURPLE = new Color(102, 0, 153);
	private JTree attributesTree;
	private JButton decUpdateBtn=SlickerFactory.instance().createButton("Create Augmented Event Log");
	private JButton fitnessBtn=SlickerFactory.instance().createButton("Open the fitness frame");
	private Collection<String> attributeSet;
	private Collection<String> traceAttributeSet;  // <--
	private String selectedORplace;		//<--
	private DecisionTreePanel DTP;		// <--
	private NiceIntegerSlider traceDelayThresholdSlider; // <--
	private Collection<String> activitySet;
	private ConfigurationPanel configurationFrame;
	private JPanel northPanel=null;
	private JCheckBox fitnessCBox=SlickerFactory.instance().createCheckBox("Consider Fitness as Characteristic", false);
	private JCheckBox mapDBCBox=SlickerFactory.instance().createCheckBox("Swapping to Disk if unsufficient memory", false);
	private Collection<Augmentation> finalSelectedAugmentations;

	private AlignmentFrame frame=null;
	private Augmentation fitness=null;
	private Map<String, Type> attributeType;
	private static Map<String, Color> colorMap=Collections.synchronizedMap(new HashMap<String,Color>());
	
	public VariablePanel(DecisionTreePanel dtp, Collection<String> traceAttributeSet, Collection<String> attributeSet, Collection<String> activitySet, final ConfigurationPanel configurationFrame, 
			ResultReplay resReplay, Map<String, Type> map) {
		this.attributeSet=attributeSet;
		this.activitySet=activitySet;
		this.configurationFrame=configurationFrame;
		this.attributeType=map;
		this.traceAttributeSet = traceAttributeSet;
		this.DTP = dtp;  // <--
		if (resReplay!=null)
		{
			frame=new AlignmentFrame(activitySet,resReplay);
			fitness=new Fitness(resReplay);
		}
		fitnessBtn.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(true);
			}
		});

		decUpdateBtn.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				
				//-->
				DTP.getConfigurationPanel().setFromVarPanel(true);
				Collection<Augmentation> selectedAugmentations = new HashSet<Augmentation>();
				selectedAugmentations.addAll(getSelectedItems());
				for (Augmentation ag : getSelectedItems()) {
					System.out.println("~~~~aug~~~~~~ "+ ag.toString());
					if (ag.toString().equals("Choice_Attribute")) {
						try {
							SelectionUtil selUtil = new SelectionUtil(DTP.getContext(), DTP.getPredictor().getModel());
							Set<DirectedGraphNode> selection = selUtil.getChoice("Select Choice Place(s)", true);
							Set<Place> places = new HashSet<Place>();
							for (DirectedGraphNode node : selection) {
								places.add((Place) node);
							}
							DTP.getPredictor().setSelectedORplaces(places);
							selectedAugmentations.remove(ag);
							for (Place place : places) {
								Augmentation aug;
								aug = new AttributeValue("Choice_"+place.getLabel()+"_to_"+outGoingTransitions(place));
								selectedAugmentations.add(aug);
							}
						} catch (Exception e) {
						e.printStackTrace();
						}
					} else if (ag.toString().equals("Sub_Model_Attribute")) {
				
						try {
							SelectionUtil selUtil = new SelectionUtil(DTP.getContext(), DTP.getPredictor().getModel());
							Set<DirectedGraphNode> selection = selUtil.getChoice("Select a Set of Transitions", false);
							Set<Transition> transitions = new HashSet<Transition>();
							for (DirectedGraphNode node : selection) {
								transitions.add((Transition) node);
							}
							Augmentation aug;
							DTP.getPredictor().setSelectetSub_model(transitions);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (ag.toString().equals("trace_delay") || ag.toString().equals("PreValue_trace_delay")) {
						JPanel p=new JPanel(new BorderLayout());
						SlickerFactory instance = SlickerFactory.instance();	
						traceDelayThresholdSlider = instance.createNiceIntegerSlider("Set The Threshold for trace delay", 0, 100, 50, Orientation.HORIZONTAL);
				    	p.add(traceDelayThresholdSlider,BorderLayout.CENTER);
				    	p.add(new JLabel("Trace delay thereshold"),BorderLayout.NORTH);
				    	int yn=JOptionPane.showConfirmDialog(null, 
				    			p,"TRACE DELAY THRESHOLD",JOptionPane.YES_NO_OPTION);
				    	if (yn==JOptionPane.NO_OPTION)
				    		return;
				    
				    	DTP.getPredictor().setTraceDelayThreshold(traceDelayThresholdSlider.getValue());
				    	}
					}
				//<--
				try {
					finalSelectedAugmentations = new HashSet<Augmentation>();
					finalSelectedAugmentations.addAll(selectedAugmentations);
					configurationFrame.setAttributeAugmentation(selectedAugmentations.toArray(new Augmentation[0]));
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		setPanel(true);
	}
	
	public static Map<String, Color> getColors()
	{
		return Collections.unmodifiableMap(colorMap);
	}
	
	public Collection<Augmentation> getFinalSelectedAugmentations() {
		return finalSelectedAugmentations;
	}

	
	public void setPanel(boolean activityLevel)
	{
		attributesTree=generateTree(traceAttributeSet, attributeSet,activitySet,activityLevel);
		if (northPanel!=null)
			northPanel.removeAll();
		else
		{
			this.setLayout(new BorderLayout());
			northPanel=new JPanel(new BorderLayout());
			this.add(northPanel,BorderLayout.NORTH);
		}
		northPanel.add(SlickerFactory.instance().createLabel("Characteristics to consider when building the decision/regression tree:"),
				BorderLayout.NORTH);
		northPanel.add(new JScrollPane(attributesTree),BorderLayout.CENTER);
		if (frame!=null)
		{
			JPanel centerPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
			centerPanel.add(fitnessCBox);
			centerPanel.add(fitnessBtn);
			this.add(centerPanel,BorderLayout.CENTER);
		}
		JPanel southPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
		southPanel.add(mapDBCBox);
		southPanel.add(decUpdateBtn);
		configurationFrame.setAttributeAugmentation(new Augmentation[0]);
		this.add(southPanel,BorderLayout.SOUTH);
		this.validate();
		this.repaint();
	}
	
	

	private JTree generateTree(Collection<String> traceAttributeSet, Collection<String> attributeSet, Collection<String> activitySet, boolean activityLevel) {
		DefaultMutableTreeNode root=new DefaultMutableTreeNode("Available Features");
		//Green Color
		DefaultMutableTreeNode dataPerspective=new DefaultMutableTreeNode("Data Perspective");
		root.add(dataPerspective);
		DefaultMutableTreeNode attribValues=new DefaultMutableTreeNode("Attribute Post-Values");
		dataPerspective.add(attribValues);
		DefaultMutableTreeNode attribPreValues=new DefaultMutableTreeNode("Attribute Pre-Values");
		dataPerspective.add(attribPreValues);
		
		DefaultMutableTreeNode timePerspective=new DefaultMutableTreeNode("Time Perspective");
		root.add(timePerspective);

		DefaultMutableTreeNode resourcePerspective=new DefaultMutableTreeNode("Resource Perspective");
		if(activityLevel)
			root.add(resourcePerspective);
		//Purple Color
		DefaultMutableTreeNode controlPerspective=new DefaultMutableTreeNode("Control-flow Perspective");
		DefaultMutableTreeNode numExecActivities=new DefaultMutableTreeNode("Number Executions of Activities");
		root.add(controlPerspective);
		DefaultMutableTreeNode traceAttributes =new DefaultMutableTreeNode("Trace Attributes");
		root.add(traceAttributes);
		controlPerspective.add(numExecActivities);
		String[] attributeArray=attributeSet.toArray(new String[0]);
		Arrays.sort(attributeArray);
		Augmentation aug;
		for(String attribute : attributeArray)
		{
			if (!attribute.startsWith("concept:") && !attribute.startsWith("lifecycle:") && !attribute.startsWith("time:"))
			{
				aug=new AttributeValue(attribute);
				attribValues.add(aug);
				colorMap.put(aug.getAttributeName(), Color.GREEN);
				if (activityLevel)
				{
					aug=new PreAttributeValue(attribute);
					attribPreValues.add(aug);
					colorMap.put(aug.getAttributeName(), Color.GREEN);
				}
				//if (attributeType.get(attribute)==Type.CONTINUOS || attributeType.get(attribute)==Type.DISCRETE)
				//	sumAttributeValues.add(new SumAttributeValue(attribute));
			}
		}
		// -->
		if (traceAttributeSet.size()>1 ) {
			//DefaultMutableTreeNode traceAttributes=new DefaultMutableTreeNode("Trace Attributes");
			//root.add(traceAttributes);
			for(String attribute : traceAttributeSet)
			{
				if (!attribute.startsWith("concept:") && !attribute.startsWith("lifecycle:") && !attribute.startsWith("time:"))
				{
					aug=new AttributeValue(attribute);
					traceAttributes.add(aug);
					colorMap.put(aug.getAttributeName(), Color.GREEN);
					if (activityLevel)
					{
						aug=new traceAttributeValue(attribute);
						//traceAttributes.add(aug);
						colorMap.put(aug.getAttributeName(), Color.GREEN);
					}
					//if (attributeType.get(attribute)==Type.CONTINUOS || attributeType.get(attribute)==Type.DISCRETE)
					//	sumAttributeValues.add(new SumAttributeValue(attribute));
				}
			}		
		}
		
		aug=new AttributeValue("trace_delay");
		traceAttributes.add(aug);
		
		DefaultMutableTreeNode newAttributes =new DefaultMutableTreeNode("New Attributes");
		root.add(newAttributes);
		aug=new AttributeValue("Choice_Attribute");
		newAttributes.add(aug);
		colorMap.put(aug.getAttributeName(), Color.GREEN);
		aug=new AttributeValue("Sub_Model_Attribute");
		newAttributes.add(aug);
		colorMap.put(aug.getAttributeName(), Color.GREEN);
		DefaultMutableTreeNode otherAttributes=new DefaultMutableTreeNode("Other Attributes");
		for (String attName : DTP.getPredictor().getOriginalLogAttributes()) {
			otherAttributes.add(new AttributeValue(attName));
		}
		root.add(otherAttributes);
		//<--

		aug=new ElapsedTime();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		if (activityLevel)
		{
			aug=new RemainingTime();
			timePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
		}
		if (activityLevel)
		{
			aug=new ActivityDuration();
			timePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
		}
		aug=new Timestamp();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		
		if (activityLevel)
		{
			aug=new Executor();
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		if (activityLevel)
		{
			aug=new Role();
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		if (activityLevel)
		{
			aug=new Resource();
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		if (activityLevel)
		{
			aug=new Group();
			resourcePerspective.add(aug);

		}
		if (activityLevel)
		{
			TotalResourceWorkload trw=new TotalResourceWorkload();
			resourcePerspective.add(trw);
			colorMap.put(trw.getAttributeName(), Color.CYAN);
			aug=new ResourceWorkload(trw);
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new NumberExecution(activity);
				numExecActivities.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}
			
		}
		if (activityLevel)
		{
			aug=new NextActivity(activitySet.toArray(new String[0]));
			colorMap.put(aug.getAttributeName(), PURPLE);
			controlPerspective.add(aug);
			aug=new PreviousActivity();
			colorMap.put(aug.getAttributeName(), PURPLE);
			controlPerspective.add(aug);
		}
		if (activityLevel)
		{
			aug=new ActivityName();
			controlPerspective.add(aug);
			colorMap.put(aug.getAttributeName(), PURPLE);

		}
		aug=new EventNumber(!activityLevel);
		controlPerspective.add(aug);
		colorMap.put(aug.getAttributeName(), PURPLE);
		JTree retValue=new MyJTree(root);
		retValue.setSelectionModel(new MLTreeSelectionModel(this));

		return retValue;
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Attributes";
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
	

	@SuppressWarnings("unchecked")
	public Collection<Augmentation> getSelectedItems() {
		Set<Augmentation> selectedAugmentations=new HashSet<Augmentation>();
		TreePath paths[]=attributesTree.getSelectionPaths();
		if (paths!=null)
			for (TreePath path : paths)
				if (path.getLastPathComponent() instanceof Augmentation)
					selectedAugmentations.add((Augmentation) path.getLastPathComponent());
		if (frame!=null)
		{
			selectedAugmentations.addAll(frame.createAugmentation());
			if (fitnessCBox.isSelected())
			{
				selectedAugmentations.add(fitness);
			}
		}
		return selectedAugmentations;
	}

	public boolean isMapDBSelected() {
		return mapDBCBox.isSelected();
	}
	
	public String outGoingTransitions(Place place) {
		Petrinet model = DTP.getPredictor().getModel();
		String label = new String("[ ");
		for (Transition transition : model.getTransitions()) {
			Arc arc = model.getArc(place, transition);
			if (arc != null) {
				label = label+transition.getLabel()+", ";
			}  
		}
		label = label+" ]";
		return label;
	}

}
