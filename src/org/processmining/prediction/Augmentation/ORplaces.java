package org.processmining.prediction.Augmentation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;


//returns all the OR places of a petrinet (created by inductive Miner) 
//also the input and output arcs to OR places are computed

public class ORplaces {
	private Set<Place> allORPlaces = new HashSet<Place>();
	private Map<Place, Set<Transition>> inputToORplaces = new HashMap<Place, Set<Transition>>();
	private Map<Place, Set<Transition>> outputOfORplaces = new HashMap<Place, Set<Transition>>();
	
	public ORplaces(Petrinet model) {
		Collection<Place> allPlaces = model.getPlaces();
		Collection<Transition> allTransitions = model.getTransitions();
		for (Place place : allPlaces) {
			Set<Transition> outputTransitions = new HashSet<Transition>();
			Set<Transition> inputTransitions = new HashSet<Transition>();
			for (Transition transition : allTransitions) {
				Arc arc = model.getArc(place, transition);
				if (arc != null) {
					outputTransitions.add(transition);
				}
				arc = model.getArc(transition, place);
				if (arc != null) {
					inputTransitions.add(transition);
				}
			}
			if (outputTransitions.size() > 1) {
				allORPlaces.add(place);
				outputOfORplaces.put(place, outputTransitions);
				inputToORplaces.put(place, outputTransitions);
			}
			
		}

	}
	
	public Set<Transition> getOutputTransitions (Place place) {
		return outputOfORplaces.get(place);
	}
	
	public Set<Transition> getInputTransitions (Place place) {
		return inputToORplaces.get(place);
	}
	
	public Set<Place> getORPlaces () {
		return allORPlaces;
	}
	
	
	// add an attribute to the trace mentioning in which position of the replaye this OR choice has happend 
	// attribute is in the form (Or place name, Or place position)
	// if this Or place happens more than once, for each time one copy of the trace with one of the positions of the choice
	public Set<XTrace> replicateTraceByORPlace (XTrace trace, Place place, SyncReplayResult srr) {
		if (!allORPlaces.contains(place)) {
			return null;
		}
		Set<XTrace> newTraces = new HashSet<XTrace>();
		Set<Transition> inputTransitions = inputToORplaces.get(place);
		Set<String> inputTransitionNames = new HashSet<String>();
		for (Transition t : inputTransitions) {
			inputTransitionNames.add(t.getLabel());
		}
		List<Object> nodeInstance = srr.getNodeInstance();
		List<StepTypes> stepTypes = srr.getStepTypes();
		Map<Integer, String> positionsOfORinReplay = new HashMap<Integer, String>();
		Integer idxOfModel = 0;
		while (idxOfModel < nodeInstance.size()) {
			StepTypes thisStepType = stepTypes.get(idxOfModel);
			if (nodeInstance.get(idxOfModel) instanceof Transition) {
				if (thisStepType.toString().equals("Sync move") || thisStepType.toString().equals("Invisible step")) {
					Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
					String transitionName = thisTransition.getLabel();
					if (inputToORplaces.get(place).contains(transitionName)) {
						positionsOfORinReplay.put(idxOfModel, place.getLabel());
					}
				}
			}
			idxOfModel++;
		}
		
		if (positionsOfORinReplay.size() == 0) {
			XAttributeMap amap = trace.getAttributes();
			XAttributeLiteralImpl nvalue = new XAttributeLiteralImpl(place.getLabel(), "-1");
			amap.put(place.getLabel(), nvalue);
			newTraces.add(trace);
		} else {
			for (Integer index : positionsOfORinReplay.keySet()) {
				XTrace aNewTrace=XFactoryRegistry.instance().currentDefault().createTrace(trace.getAttributes());
				for(XEvent event : trace) {
					aNewTrace.add(XFactoryRegistry.instance().currentDefault().createEvent(event.getAttributes()));
				}	
				XAttributeMap amap = aNewTrace.getAttributes();
				XAttributeLiteralImpl nvalue = new XAttributeLiteralImpl(place.getLabel(), index.toString());
				amap.put(place.getLabel(), nvalue);
				newTraces.add(aNewTrace);
			}
		}
		return newTraces;
	}
	
}
