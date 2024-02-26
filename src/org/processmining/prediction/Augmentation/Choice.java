package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Choice  extends Augmentation implements ActivityLevelAugmentation {

	public Choice() {
		super("Choice");
	}

	public void reset(XTrace trace) {

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		
		return null;

	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String returnAttribute(XEvent event, String choicePlaceName) {
		String newValue = new String();
		for(String attName : event.getAttributes().keySet()) // gathers all the attributes in the event except those that are mentioned
		{
			if (attName.substring(0, 7).equals("Choice_") && attName.substring(7,7+choicePlaceName.length()).equals(choicePlaceName))
			{
				XAttribute att = event.getAttributes().get("Choice_"+choicePlaceName);
				newValue = att.toString();	
				return newValue;
			}
		}
		return "NOT SET";
	}

}
