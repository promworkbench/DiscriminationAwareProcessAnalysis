package org.processmining.prediction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
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
	public final static String notAllowedChars=".()&!|=<>-+*/% ";
	private Set<Place> allORPlaces = new HashSet<Place>();
	//private Map<Place, Set<String>> inputToORplacesTransitionLabels = new HashMap<Place, Set<String>>();
	private Map<Place, Set<Transition>> inputToORplacesTransitions = new HashMap<Place, Set<Transition>>();
	//private Map<Place, Set<String>> outputOfORplacesTransitionLabels = new HashMap<Place, Set<String>>();
	private Map<Place, Set<Transition>> outputOfORplacesTransitions = new HashMap<Place, Set<Transition>>();
	
	public ORplaces(Petrinet model) {
		Collection<Place> allPlaces = model.getPlaces();
		Collection<Transition> allTransitions = model.getTransitions();
		for (Place place : allPlaces) {
			Set<Transition> outputTransitions = new HashSet<Transition>();
			Set<String> outputTransitionLabels = new HashSet<String>();
			Set<Transition> inputTransitions = new HashSet<Transition>();
			Set<String> inputTransitionLabels = new HashSet<String>();
			for (Transition transition : allTransitions) {
				Arc arc = model.getArc(place, transition);
				if (arc != null) {
					outputTransitionLabels.add(transition.getLabel());
					outputTransitions.add(transition);
				}
				arc = model.getArc(transition, place);
				if (arc != null) {
					inputTransitionLabels.add(transition.getLabel());
					inputTransitions.add(transition);
				}
			}
			if (outputTransitionLabels.size() > 1) {
				allORPlaces.add(place);
				outputOfORplacesTransitions.put(place, outputTransitions);
				inputToORplacesTransitions.put(place, inputTransitions);
			}
			
		}

	}
	
	public Set<Transition> getOutputTransitions (Place place) {
		return outputOfORplacesTransitions.get(place);
	}
	
	public Set<Transition> getInputTransitions (Place place) {
		return inputToORplacesTransitions.get(place);
	}
	
	public Set<Place> getORPlaces () {
		return allORPlaces;
	}
	
	
	// add an attribute to the OR event mentioning what was the choice that was taken
	// attribute is in the form (Or place name, Or choice result)
	public void enrichTraceWithORChoices (XTrace trace, SyncReplayResult res, int idx) {

		if (allORPlaces.isEmpty()) {
			return;
		}
//		if (idx == 6) {
//			System.out.println("here is the truble");
//		}
		Integer idxOfModel = 0;
		Integer idxOfTrace = -1;
		LinkedList<Place> visitedORPlaces = new LinkedList<Place>();
		List<StepTypes> stepTypes = res.getStepTypes();
		List<Object> nodeInstance = res.getNodeInstance();
		Map<Place, LinkedList<Integer>> whereORPlacesHappend = new HashMap<Place, LinkedList<Integer>>();
		int lastSeenIndex = 0;
		while (idxOfModel < nodeInstance.size())  {
			StepTypes thisStepType = stepTypes.get(idxOfModel);
    	//	System.out.print(thisStepType.toString()+ " idx model "+ idxOfModel+ " idxTrace : "+ idxOfTrace);
			if (nodeInstance.get(idxOfModel) instanceof Transition) {
                if (thisStepType.toString().equals("Invisible step") || thisStepType.toString().equals("Sync move")) {
                	Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
                	String thisTransitionName = thisTransition.getLabel();
     //           	System.out.println(" this transition : "+ thisTransitionName);
                	for (Place place : allORPlaces) {
               // 		System.out.println("place : "+ place.getLabel()+ " transition : "+ thisTransition.getLabel());
                		if (inputToORplacesTransitions.get(place).contains(thisTransition)) {
                			visitedORPlaces.add(place);
                		//	System.out.println("add place : "+ place.getLabel());
                		}
                		if (outputOfORplacesTransitions.get(place).contains(thisTransition) && visitedORPlaces.contains(place)) {
                		//	System.out.println(" add atribute at index " + idxOfTrace);
                			visitedORPlaces.removeLastOccurrence(place);
                			XAttributeMap amap = trace.get(lastSeenIndex).getAttributes();
                			String label = "Choice_"+replaceNotAllowedStrings(place.getLabel());
							XAttributeLiteralImpl nvalue = new XAttributeLiteralImpl(label, thisTransitionName);
							if (amap.containsKey(label)) {
								amap.remove(label);
							}
							amap.put(label, nvalue);
                		}
                	}
                }
                if (thisStepType.toString().equals("Model move") ) {
                	idxOfModel++;
                }
                if (thisStepType.toString().equals("Invisible step")) {
                	idxOfModel++;
                }
                if (thisStepType.toString().equals("Sync move")) {
                	idxOfModel++;
                	idxOfTrace++;
                	lastSeenIndex = idxOfTrace;
                }
			}
			if ( thisStepType.toString().equals("Log move")) {
            	idxOfTrace++;
            	idxOfModel++;
            	lastSeenIndex = idxOfTrace;
            }
		}
//		System.out.println("or line 119");
	}
	
	public String replaceNotAllowedStrings(String str) {
   		char[] array=str.toCharArray();
		for(int i=0;i<array.length;i++)
		{
			if (notAllowedChars.indexOf(array[i])!=-1)
				array[i]='_';
		}
		return (new String(array));
   	}

	public Map<String, Set<String>> getOutputLabelsORPlaces () {
		
		Map<String, Set<String>> map = new HashMap<String,Set<String>>();
		for (Place place : allORPlaces) {
			String placeName = place.getLabel();
			Set<Transition> outputTransitions = outputOfORplacesTransitions.get(place);
			Set<String> outputTransitionLabels = new HashSet<String>();
			for (Transition transition : outputTransitions) {
				outputTransitionLabels.add(transition.getLabel());
			}
			map.put(replaceNotAllowedStrings(placeName), outputTransitionLabels);
		}

		return 	map;
		}
	
}
