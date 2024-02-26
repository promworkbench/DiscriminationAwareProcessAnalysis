package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class NumberExecution extends Augmentation {

	private final String activityName;
	private int numExecution=0;

	public NumberExecution(String activityName)
	{
		super("NumExecution_"+activityName);
		this.activityName=activityName;
	}
	
	public void reset(XTrace trace) {
		numExecution=0;
	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		if (activityName.equals(XConceptExtension.instance().extractName(event))
				&& (XLifecycleExtension.instance().extractTransition(event) == null ||
						XLifecycleExtension.instance().extractTransition(event).trim().equalsIgnoreCase("completed") ||
						XLifecycleExtension.instance().extractTransition(event).trim().equalsIgnoreCase("complete") ||
						XLifecycleExtension.instance().extractTransition(event).trim().equals("")))
		{
			return(numExecution++);
		}
		else
			return(numExecution);
	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub
		
	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}


}
