package org.processmining.prediction.Augmentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class ActivityDuration extends Augmentation implements ActivityLevelAugmentation 
{
	private Map<String,List<XEvent>> mapNamesToEvents=new HashMap<String, List<XEvent>>();
	private long duration=-1;
	
	public ActivityDuration() {
		super("ActivityDuration");
	}

	public void reset(XTrace trace) {
		mapNamesToEvents.clear();
		duration=-1;
	}

	public void setLog(XLog log) {
	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		XAttributeMap amap = event.getAttributes();
		if (amap.containsKey("activityduration")) {
			duration = Long.parseLong(amap.get("activityduration").toString());
			return duration;
		}
		return null;

	}
	
	@Override
	public boolean isTimeInterval() {
		return true;
	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}	
}
