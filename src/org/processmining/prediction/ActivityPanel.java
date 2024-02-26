package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import csplugins.id.mapping.ui.CheckComboBox;

class MyModel extends DefaultListModel
{
	private final List<String> activityGroups;

	public MyModel(List<String> activities)
	{
		this.activityGroups=new LinkedList<String>(activities);
	}
	
	public Object getElementAt(int index) 
	{
		return activityGroups.get(index);
	}

	public List<String> getActivities()
	{
		return Collections.unmodifiableList(activityGroups);
	}
	
	public int getSize() {
		return activityGroups.size();
	}

	private void fireChanges(int index0, int index1) {
		super.fireContentsChanged(this, index0, index1);
	}
	
	private void fireAdditions(int index0, int index1) {
		super.fireIntervalAdded(this, index0, index1);
	}

	private void fireRemovals(int index0, int index1) {
		super.fireIntervalRemoved(this, index0, index1);
	}

	public void setActivities(Collection<String> newActivities)
	{
		LinkedList<String> list=new LinkedList<String>(newActivities);
		Collections.sort(list);
		setActivities(list);
	}

	public void setActivities(List<String> newActivities) {
		if (newActivities!=null)
		{
			int preSize=activityGroups.size();
			activityGroups.clear();
			activityGroups.addAll(newActivities);
			updateGraphicalTable(preSize);
		}
		else
		{
			int preSize=activityGroups.size();
			activityGroups.clear();
			fireRemovals(0, preSize-1);
		}
	}
	
	private void updateGraphicalTable(int preSize)
	{
		if (preSize<activityGroups.size())
		{
			fireAdditions(preSize,activityGroups.size()-1);
		}
		else if (preSize>activityGroups.size())
		{
			fireRemovals(activityGroups.size(), preSize-1);
		}
		if (activityGroups.size()>0)
			fireChanges(0,activityGroups.size()-1);
	}

	public void removeActivity(int i) {
		activityGroups.remove(i);
		fireRemovals(i,i);
	}

	public void addActivities(Collection<String> newActivities) {
		int preSize=activityGroups.size(); 
		activityGroups.addAll(newActivities);
		Collections.sort(activityGroups);
		updateGraphicalTable(preSize);
		
	}
	
	
}

// top left list front end for choosing the activities
// list of activities is one of inputs of the constructor
public class ActivityPanel extends RoundedPanel implements ActionListener, ActivitiesGrouper
{
	
	private static final String ADD = "A";
	private static final String REMOVE = "R";
	private static final String RESET = "RESET";
//	private static final String CASELEVEL="C";
	private static final String EXCLUDEALL = "EX";
	
	private JButton addButton=SlickerFactory.instance().createButton("Add Activity");
	private JButton removeButton=SlickerFactory.instance().createButton("Remove Activity");
	private JButton resetButton=SlickerFactory.instance().createButton("Reset to Include All Activities");	
	private JButton excludeButton=SlickerFactory.instance().createButton("Exclude All Activities");	
	private JToggleButton caseLevelButton=new JToggleButton("Case Level",false);
	private MyProMList aList;
	private final List<String> existingActivities;
	private VariablePanel varPanel;
	
	public ActivityPanel(Collection<String> collection,VariablePanel varPanel) {
		this.varPanel=varPanel;
		LinkedList<String> aux = new LinkedList<String>(collection);
		aux.remove(Predictor.CASE_ACTIVITY);
		Collections.sort(aux);
		existingActivities=Collections.unmodifiableList(aux);
		aList=new MyProMList("Activities to Consider:",new MyModel(existingActivities));
		aList.setToolTipText("All events referring to activities in this list are retained");
		this.setLayout(new BorderLayout());
		this.add(aList,BorderLayout.CENTER);
		JPanel buttonPnl=new JPanel();
		buttonPnl.add(addButton);
		addButton.addActionListener(this);
		addButton.setActionCommand(ADD);
		removeButton.addActionListener(this);
		removeButton.setActionCommand(REMOVE);
		buttonPnl.add(removeButton);
		resetButton.addActionListener(this);
		resetButton.setActionCommand(RESET);
		buttonPnl.add(resetButton);
		excludeButton.setActionCommand(EXCLUDEALL);
		excludeButton.addActionListener(this);
		buttonPnl.add(excludeButton);

		caseLevelButton.addActionListener(this);
//		caseLevelButton.setActionCommand(CASELEVEL);
	//	buttonPnl.add(caseLevelButton);
		this.add(buttonPnl, BorderLayout.SOUTH);
	}

	public void actionPerformed(ActionEvent arg0) {
		String actionCommand=arg0.getActionCommand();
		int[] indices=aList.getSelectedIndices();
		if (actionCommand == ADD)
		{
			LinkedList<String> activitiesToAdd=new LinkedList<String>(existingActivities);
			activitiesToAdd.removeAll(((MyModel)aList.getModel()).getActivities());
			JPanel p=new JPanel(new BorderLayout());
			CheckComboBox cbb=new CheckComboBox(activitiesToAdd);
			Dimension dim=cbb.getPreferredSize();
			dim.width*=2;
			cbb.setPreferredSize(dim);
			p.add(cbb,BorderLayout.CENTER);
			p.add(new JLabel("Select one or more activities"),BorderLayout.NORTH);
			int yn=JOptionPane.showConfirmDialog(null, 
					p,"Add Activities",JOptionPane.YES_NO_OPTION);
			if (yn==JOptionPane.NO_OPTION)
				return;
			Collection<String> newActivities = cbb.getSelectedItems();			
			((MyModel)aList.getModel()).addActivities(newActivities);
		}
		else if (actionCommand == REMOVE)
		{
			if (indices.length==0)
				return;			
			Arrays.sort(indices);
			for(int i=indices.length-1;i>=0;i--)
			{
				((MyModel)aList.getModel()).removeActivity(indices[i]);
			}
		}
		else if (actionCommand == RESET)
		{
			((MyModel)aList.getModel()).setActivities(existingActivities);			
			
		}
		else if (actionCommand == EXCLUDEALL)
		{
			((MyModel)aList.getModel()).setActivities(null);	
		}
//	else if (actionCommand == CASELEVEL)
//	{
//		aList.setEnabled(!caseLevelButton.isSelected());
//		addButton.setEnabled(!caseLevelButton.isSelected());
//		resetButton.setEnabled(!caseLevelButton.isSelected());
//		removeButton.setEnabled(!caseLevelButton.isSelected());
//		varPanel.setPanel(!caseLevelButton.isSelected());
//		
//	}
		aList.setSelectedIndices(new int[0]);
	}

	public void updated() {
		
	}

	public String getPanelName() {
		return "Activities";
	}

	public JComponent getComponent() {
		return this;
	}

	public void setScalableComponent(ScalableComponent scalable) {
		
	}

	public void setParent(ScalableViewPanel viewPanel) {
		
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public double getWidthInView() {
		return 2*this.getPreferredSize().getWidth();
	}

	public void willChangeVisibility(boolean to) {
		
	}

	public Collection<String> getActivitiesToConsider() {
		if (caseLevelButton.isSelected())
		{
			ArrayList<String> retValue=new ArrayList<String>();
			retValue.add(Predictor.CASE_ACTIVITY);
			return retValue;
		}
		else
			return ((MyModel)aList.getModel()).getActivities();
	}

}
