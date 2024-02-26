package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class ActivityName extends Augmentation implements ActivityLevelAugmentation {

	public ActivityName() {
		super("ActivityName");
		// TODO Auto-generated constructor stub
	}

	public void reset(XTrace trace) {
		// TODO Auto-generated method stub

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event, XTrace myTrace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		String activityName=XConceptExtension.instance().extractName(event);
		if (activityName==null)
			activityName="NO NAME";
		return activityName;
		

	}

}
