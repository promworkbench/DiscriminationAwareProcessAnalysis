package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class OtherEventAttributes extends Augmentation implements ActivityLevelAugmentation {
	private String eventName;
	
	public OtherEventAttributes(String attributeName) {
		super(attributeName);
		// TODO Auto-generated constructor stub
	}

	
	public OtherEventAttributes(String attributeName, String eventName) {
		super(attributeName);
		this.eventName = eventName;
		// TODO Auto-generated constructor stub
	} 
	
	public String getName() {
		return eventName+"_"+attributeName;
	}
	
	private XTrace trace=null;
	
	
	public void reset(XTrace trace) {
		this.trace=trace;
	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		if (event.getAttributes().containsKey(super.toString())) {
			return getAttributeValues(event.getAttributes().get(super.toString()));
		} 
		return null;
	}

	public void setLog(XLog log) {
		
	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}

}