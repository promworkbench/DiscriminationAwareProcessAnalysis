package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.processmining.models.FunctionEstimator.Type;

import com.fluxicon.slickerbox.factory.SlickerFactory;

public class AttributeSettingPanel {
	private Map<String, Object> minValues;
	private Map<String, Object> maxValues;
	private Map<String, Object> meanValues;
	private Map<String, Type> attributeType;
	private Set<Map<String, Object>> instances;
	
	AttributeSettingPanel(final DecisionTreePanel frame, boolean isEventAtt) {
		attributeType = frame.getPredictor().getTypesNDC();
		//instances = frame.getPredictor().getInstancesOfNDC();
		//instances = frame.getPredictor().getInstancesOfNDC();
		JPanel p=new JPanel();
		RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS);
		rl.setFill(true);
		p.setLayout(rl);
    	LinkedList<String> activitiesToConsider = new LinkedList<String>();
    	JComboBox cbb=SlickerFactory.instance().createComboBox(frame.getPredictor().getActivitiesToConsider().toArray());
    	Dimension dim=cbb.getPreferredSize();
    	dim.width*=2;
    	cbb.setPreferredSize(dim);
    	p.add(cbb,BorderLayout.CENTER);
    	p.add(new JLabel("The Sensitive Attribute Belong To Which Activity?"),BorderLayout.NORTH);
    	int yn=JOptionPane.showConfirmDialog(null, 
    			p,"Set the activity containing the sensitive attribute",JOptionPane.YES_NO_OPTION);
    	if (yn==JOptionPane.NO_OPTION)
    		return;
    	String actionName = (String) cbb.getSelectedItem();
   // 	setGoodOutcomes (actionName, true, true);
		
		}
	public void setMinAndMax() { 
		Set<String> nominalOrContinuesAtts = new HashSet<String>();
		for (String attName : attributeType.keySet()) {
			if (attributeType.get(attName) != Type.LITERAL) {
				nominalOrContinuesAtts.add(attName);
			}
		}
		minValues = new HashMap<String, Object>();
		maxValues = new HashMap<String, Object>();
		Map<String, Boolean> hasValidValue = new HashMap<String, Boolean>();
		for (String attName : attributeType.keySet()) {
			hasValidValue.put(attName, false); // meaning that this attribute has no valid value (just null or not set)
	//		System.out.println("att type : " + attName);
		}
		
		for (Map<String,Object> instance : instances) {
			for (String attName : instance.keySet()) {
		//		System.out.println("instance : " + attName);
				switch (attributeType.get(attName)) {
					case TIMESTAMP :
						if( instance.get(attName) == null || instance.get(attName).equals("NOT SET")) {
							  break;
						  } else {
							  long time = ((Date) instance.get(attName)).getTime();
							  if (hasValidValue.get(attName) == false ) {
								  hasValidValue.remove(attName);
								  hasValidValue.put(attName, true);
								  minValues.put(attName, time);
								  maxValues.put(attName, time);
							  } else {
								  if (time < (long) minValues.get(attName)) {
									  minValues.remove(attName);
									  minValues.put(attName, time);
								  } 
								  if (time > (long) maxValues.get(attName)) {
									  maxValues.remove(attName);
									  maxValues.put(attName, time);
								  } 
							  }
							  break;
						  }
					case DISCRETE : // Do the same as case CONTINOUS
					case CONTINUOS :
						if( instance.get(attName) == null || instance.get(attName).equals("NOT SET")) {
							  break;
						  } else {
							  Object obj = instance.get(attName);
							  if (instance.get(attName) instanceof Integer)
								  
							  if (hasValidValue.get(attName) == false ) {
								  hasValidValue.remove(attName);
								  hasValidValue.put(attName, true);
								  minValues.put(attName, obj);
							  } else {
								  if (isBigger( minValues.get(attName),obj)) {
									  minValues.remove(attName);
									  minValues.put(attName, obj);
								  } 
								  if (isBigger(obj, maxValues.get(attName))) {
									  maxValues.remove(attName);
									  maxValues.put(attName, obj);
								  } 
							  }
							  break;
						  }
				}
			}
		} 
	}
	
	public boolean isBigger(Object o1, Object o2) {
		if (o1 instanceof Integer && o2 instanceof Integer) {
			if ((Integer) o1 > (Integer) o2) {
				return true;
			}
		}
		if (o1 instanceof Date && o2 instanceof Date) {
			if (((Date) o1).getTime() > ((Date) o2).getTime()) {
				return true;
			}
		}
		if (o1 instanceof Long && o2 instanceof Long) {
			if ((Long) o1 > (Long) o2) {
				return true;
			}
		} 
		if (o1 instanceof Float && o2 instanceof Float) {
			if ((Float) o1 > (Float) o2) {
				return true;
			}
		}
		if (o1 instanceof Double && o2 instanceof Double) {
			if ((Double) o1 > (Double) o2) {
				return true;
			}
		}
		return false;
	}
	
}
