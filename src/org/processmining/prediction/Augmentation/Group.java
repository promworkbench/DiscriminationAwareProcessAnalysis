package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Group extends Augmentation implements ActivityLevelAugmentation {

	public Group() {
		super("Executor Group");
	}

	public void reset(XTrace trace) {

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		String group=org.deckfour.xes.extension.std.XOrganizationalExtension.instance().extractGroup(event);
		if (group!=null)
			return group;
		else {
			Object newValue = new Object();
			for(String attName : event.getAttributes().keySet()) // gathers all the attributes in the event except those that are mentioned
			{
				if (attName.equals("group") || attName.equals("Group") || attName.equals("org:group"))
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
