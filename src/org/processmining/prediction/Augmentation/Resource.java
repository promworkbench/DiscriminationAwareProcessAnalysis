package org.processmining.prediction.Augmentation;



import java.util.Set;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Resource  extends Augmentation implements ActivityLevelAugmentation {

	public Resource() {
		super("Resource");
	}

	public void reset(XTrace trace) {

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		String resource=org.deckfour.xes.extension.std.XOrganizationalExtension.instance().extractResource(event);
		if (resource!=null)
			return resource;
		else {
			Object newValue = new Object();
			for(String attName : event.getAttributes().keySet()) // gathers all the attributes in the event except those that are mentioned
			{
				if (attName.equals("recource") || attName.equals("Resource") || attName.equals("org:recource"))
				{
					XAttribute att = event.getAttributes().get(attName);
					newValue = att.toString();	
					return newValue;
				}
			}
		}
		return "NOT SET";

	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}

}
