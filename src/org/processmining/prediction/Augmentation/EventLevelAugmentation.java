package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;

public class EventLevelAugmentation extends Augmentation implements ActivityLevelAugmentation {
	private String eventName;
	
	public EventLevelAugmentation(String attributeName) {
		super(attributeName);
		// TODO Auto-generated constructor stub
	}
	
	public EventLevelAugmentation(String attributeName, String eventName) {
		super(attributeName);
		this.eventName = eventName;
		// TODO Auto-generated constructor stub
	} 
	
	public String getName() {
		return eventName+"_"+attributeName;
	}
	
	private XTrace trace=null;
	private int currPos=0;
	
	
	public void reset(XTrace trace) {
		this.trace=trace;
		currPos=0;
	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		String attrValue;
		if (currPos == 0 )
		{
			attrValue="NOTHING";
		}
		else
		{
			XEvent previousEvent=trace.get(currPos-1);		
			attrValue=XConceptExtension.instance().extractName(previousEvent);
			if (attrValue==null)
				attrValue="";
			if (attrValue.equals(Predictor.CASE_ACTIVITY))
				attrValue="NOTHING";
		}
		currPos++;
		return attrValue;
	}

	public void setLog(XLog log) {
		
	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}

}
