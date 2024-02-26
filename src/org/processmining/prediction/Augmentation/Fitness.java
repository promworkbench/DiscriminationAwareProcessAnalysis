package org.processmining.prediction.Augmentation;

import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.DataConformance.ResultReplay;

public class Fitness extends Augmentation implements ActivityLevelAugmentation {
	private float fitness;
	private ResultReplay resReplay;

	public Fitness(ResultReplay resReplay) {
		super("Trace Fitness");
		this.resReplay=resReplay;
	}

	public void reset(XTrace trace) {
		String traceName=XConceptExtension.instance().extractName(trace);
		fitness=resReplay.getAlignmentByTraceName(traceName).getFitness();

	}

	public void setLog(XLog log) {

	}

	public Object returnAttribute(XEvent event, XTrace trace, String sensitiveAttrebute, Set<String> protectedValues, long traceDelayThreshold) {
		return fitness;

	}

	public Object returnAttribute(XTrace trace, String augName) {
		// TODO Auto-generated method stub
		return null;
	}

}
