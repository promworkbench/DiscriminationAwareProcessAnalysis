package org.processmining.prediction.Augmentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult; 

public class sub_model_duration {
	
private long duration;
private PNRepResult res;
private Petrinet model;
private Set<Place> sub_modelPlaces;
private Set<Transition> sub_model;
private Map<Transition, Set<Transition>> nextTransitions = new HashMap<Transition, Set<Transition>>();
private SyncReplayResult traceReplay;
private Set<Transition> subModelOutGoingTransitions = new HashSet<Transition>();
 // it computes the number of transitions in the sub_model that are visited but not finished
// it indicates the number of the transitions of the sub_model in the waiting list for their execution to be finished
private XLog log;
private Map<Transition, Integer> transitiontokenConsumption = new HashMap<Transition, Integer> (); // for each trace it indicates that the execution 
//of this transition is the end time of how many transition in the sub_model -- in other words, how many transitions have to be removed from the waiting list
private Map<Transition, Integer> transitiontokenProductivity = new HashMap<Transition, Integer> ();
private Map<Integer, Long> sub_model_execution_time = new HashMap<Integer, Long>();

public sub_model_duration(Set<Transition> sub_model, XLog log, Petrinet model, PNRepResult res) {
	this.model = model;
	this.res = res;
	this.log = log;
	this.sub_model = sub_model;
	setNextTransitions();
	driveSub_modelPaces();
	setTransitionstokenConsumption();
	setTransitionstokenProduction();
}

// for each transition in the sub_model it returns its set of outGoing transitions
// later on for calculating the time we need to subtract the time of the transition and the first one in its 
//next transitions that occurs
public void setNextTransitions() {
	Collection<Place> allPlaces = model.getPlaces();
	Collection<Transition> allTransitions = model.getTransitions();
	for (Transition transition : sub_model) {
		Set<Place> transitionOutGoingPlaces = new HashSet<Place>();
		for (Place place : allPlaces) {
			Arc arc = model.getArc(transition, place);
			if (arc != null) {
				transitionOutGoingPlaces.add(place);
			}
		}
		Set<Transition> outGoingTransitions = new HashSet<Transition>();
		for (Place place: transitionOutGoingPlaces) {
			for (Transition tr : model.getTransitions()) {
				Arc arc = model.getArc(place, tr);
				if (arc != null) {
					outGoingTransitions.add(tr);
					if (!sub_model.contains(tr) ) {
						subModelOutGoingTransitions.add(tr);
					}
				}
			}
		}
		if (outGoingTransitions != null) {
			nextTransitions.put(transition, outGoingTransitions);
		}
	}
}

// compute the sub_model places including the places that come in, out 
// and are between the sub_model transitions
public void driveSub_modelPaces () {
	sub_modelPlaces = new HashSet<Place>();
	for (Transition tr1 : sub_model) {
		for (Place pl : model.getPlaces()) {
			Arc arc1 = model.getArc(tr1, pl);
			Arc arc2 = model.getArc(pl, tr1);
			if (arc1 != null || arc2 != null) {
				sub_modelPlaces.add(pl);
			}
		}
	}
	
}

//for each transition it indicate how much token it consumes from the sub_model
public void setTransitionstokenConsumption() {
	transitiontokenConsumption = new HashMap<Transition, Integer>();
	for (Transition transition  : model.getTransitions()) {
		Set<Place> inputPlaces = new HashSet<Place>();
		// compute the input places of the transition 
		for (Place place : model.getPlaces()) {
			Arc arc = model.getArc(place, transition);
			if (arc != null) {
				inputPlaces.add(place);
			}
		}
		
		// compute the number of input transitions that are in the sub model
		Integer num = 0;
		for (Place p : inputPlaces) {
			if (sub_modelPlaces.contains(p))	{
				num++;
			}
		}
		transitiontokenConsumption.put(transition, num);
	}
}

//for each transition it indicate how much token it adds to the sub model
public void setTransitionstokenProduction() {
	transitiontokenProductivity = new HashMap<Transition, Integer>();
	for (Transition transition  : model.getTransitions()) {
		Set<Place> outputPlaces = new HashSet<Place>();
		// compute the input places of the transition 
		for (Place place : model.getPlaces()) {
			Arc arc = model.getArc( transition, place);
			if (arc != null) {
				outputPlaces.add(place);
			}
		}
		
		// compute the number of input transitions that are in the sub model
		Integer num = 0;
		for (Place p : outputPlaces) {
			if (sub_modelPlaces.contains(p))	{
				num++;
			}
		}
		transitiontokenProductivity.put(transition, num);
	}
}

//it gives a mapping of the index of traces and the duration of the sub model in it
public Map<Integer, Long> sub_modelDurations() {
	Map<Integer, List<Pair<Transition, Long>>> intermediate = new HashMap<Integer, List<Pair<Transition, Long>>>();
	sub_model_execution_time = new HashMap<Integer, Long>();
	for (SyncReplayResult singleVariantReplay : res) {
		Set<Integer> allTraceIdxOfThisVariant = singleVariantReplay.getTraceIndex();
		List<StepTypes> stepTypes = singleVariantReplay.getStepTypes();
		List<Object> nodeInstance = singleVariantReplay.getNodeInstance();
		for (Integer traceIdx : allTraceIdxOfThisVariant) {
			List<Pair<Transition, Long>> thisTraceReplay = new ArrayList<Pair<Transition, Long>>();
			Integer idxOfModel = 0;
			Integer idxOfLog = -1;
			Long start = XExtendedEvent.wrap(log.get(traceIdx).get(0)).getTimestamp().getTime();
			Long end = (long) 0;
			Long total = (long) 0;
			Integer numOfTokensInSubModel = 0;
			int lastIndex = 0;
			while (idxOfModel < nodeInstance.size()) {
				StepTypes thisStepType = stepTypes.get(idxOfModel);
				if (nodeInstance.get(idxOfModel) instanceof Transition) {
                    if (thisStepType.toString().equals("Invisible step")) {
                    	numOfTokensInSubModel-=transitiontokenConsumption.get(nodeInstance.get(idxOfModel));
                    	numOfTokensInSubModel+=transitiontokenProductivity.get(nodeInstance.get(idxOfModel));
                    	if (numOfTokensInSubModel <= 0) {
                    		numOfTokensInSubModel = 0;
						}
					}
					if (thisStepType.toString().equals("Sync move")) {
						idxOfLog++;
						if (idxOfLog >= 0) {
							Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
							Long millisecondEventTime = XExtendedEvent.wrap(log.get(traceIdx).get(idxOfLog)).getTimestamp().getTime();
							Integer power = transitiontokenConsumption.get(nodeInstance.get(idxOfModel));
							if (power > 0) {
								if (numOfTokensInSubModel > 0) {
									numOfTokensInSubModel-=power;
									if (numOfTokensInSubModel <= 0) {
										numOfTokensInSubModel = 0;
										total = total +  millisecondEventTime - start;
									}
								}
							}
							if (transitiontokenProductivity.get(nodeInstance.get(idxOfModel)) > 0) {	
								if (numOfTokensInSubModel == 0) {
									start = millisecondEventTime;;
								}
								lastIndex = idxOfLog;
								numOfTokensInSubModel+=transitiontokenProductivity.get(nodeInstance.get(idxOfModel));
							}
							thisTraceReplay.add(new Pair(thisTransition, millisecondEventTime));
						}
					} 
					if (thisStepType.toString().equals("Model move")) {
						Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
						Long millisecondEventTime = new Long(0);
						if (lastIndex < log.get(traceIdx).size()-2) {
							millisecondEventTime =  XExtendedEvent.wrap(log.get(traceIdx).get(lastIndex+1)).getTimestamp().getTime();
						} else {
							millisecondEventTime =  XExtendedEvent.wrap(log.get(traceIdx).get(lastIndex)).getTimestamp().getTime();
						}
						Integer power = transitiontokenConsumption.get(nodeInstance.get(idxOfModel));
						if (power > 0) {
							if (numOfTokensInSubModel > 0) {
								numOfTokensInSubModel-=power;
								if (numOfTokensInSubModel <= 0) {
									numOfTokensInSubModel = 0;
									if (millisecondEventTime > start) {
										total = total +  millisecondEventTime - start;
									}
								}
							}
						}
						if (transitiontokenProductivity.get(nodeInstance.get(idxOfModel)) > 0) {	
							if (numOfTokensInSubModel == 0 && millisecondEventTime > start) {
								start = millisecondEventTime;
							}
							lastIndex = idxOfLog;
							numOfTokensInSubModel+=transitiontokenProductivity.get(nodeInstance.get(idxOfModel));
						}
						thisTraceReplay.add(new Pair(thisTransition, millisecondEventTime));
					} 
				}
				if (thisStepType.toString().equals("Log move")) {
					idxOfLog++;
				}
				idxOfModel++;
			}
			intermediate.put(traceIdx, thisTraceReplay);
			
			if (numOfTokensInSubModel > 0) {
				if (lastIndex < log.get(traceIdx).size()-2) {
					Long millisecondEventTime =  XExtendedEvent.wrap(log.get(traceIdx).get(lastIndex+1)).getTimestamp().getTime();
					if (millisecondEventTime > start) {
						total = total +  millisecondEventTime - start;
					}
				} else {
					Long millisecondEventTime =  XExtendedEvent.wrap(log.get(traceIdx).get(lastIndex)).getTimestamp().getTime();
					if (millisecondEventTime > start) {
						total = total +  millisecondEventTime - start;
					}
				}
			}
			sub_model_execution_time.put(traceIdx,total);	
		}
	}
	return sub_model_execution_time;	
}

}
