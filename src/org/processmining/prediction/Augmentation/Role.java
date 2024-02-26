package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Role extends Augmentation implements ActivityLevelAugmentation {

	public Role() {
		super("Role");
		// TODO Auto-generated constructor stub
	}

	public void reset(XTrace trace) {

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		String role=org.deckfour.xes.extension.std.XOrganizationalExtension.instance().extractRole(event);
		if (role==null) role="NOT SET";
		return role;
	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}

}
