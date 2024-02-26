package org.processmining.prediction.Augmentation;

import java.util.Date;
import java.util.Set;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class RemainingTime extends Augmentation implements ActivityLevelAugmentation {

	private Date lastEventTimeStamp;

	public RemainingTime() {
		super("RemainingTime");
	}

	public void reset(XTrace trace) {
		lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1));
	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		if (lastEventTimeStamp!=null && timestamp!=null)
		{
			long remainingTime=lastEventTimeStamp.getTime()-timestamp.getTime();
			return remainingTime;
		} 
		return null;
	}

	public void setLog(XLog log) {
		
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
