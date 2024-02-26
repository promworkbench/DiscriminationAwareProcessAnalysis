package org.processmining.prediction.Augmentation;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
public class Sub_Model_Attribute extends Augmentation {

	public Sub_Model_Attribute(String attributeName) {
		super("Sub_Model_Attribute");
	}

	public void reset(XTrace trace) {
		// TODO Auto-generated method stub
		
	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub
		
	}
	
	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		XAttributeMap amap = event.getAttributes();
		if (amap.containsKey("sub_model_duration")) {
			return getAttributeValues(amap.get("sub_model_duration"));
		} 
		return null;
	}

}
