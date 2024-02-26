package org.processmining.prediction.Augmentation;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult; 

public class DurationOfActivity {
	private Map<Transition, Set<Place>> inputPlacesToTransition = new HashMap<Transition, Set<Place>>();  // places reachable from a transition by one step
	private Map<Transition, Set<Place>> outputPlacesOfTransition = new HashMap<Transition, Set<Place>>();
	private Map<Place, Set<Transition>> inputTransitionsToPlaces = new HashMap<Place, Set<Transition>>();
	private Map<Place, Set<Transition>> outputTransitionsOfPlaces = new HashMap<Place, Set<Transition>>();
	private Map<Transition, Set<Transition>> reachableTransitionsOfTransition = new HashMap<Transition, Set<Transition>>(); // transitions reachable of each transition(possibly by more than one step invisible)
	private Map<Transition, Set<Transition>> catchingTransitionsOfEachTransition = new HashMap<Transition, Set<Transition>>(); // transitions that can reach a transition (possibly by more than one step invisible)
	private Petrinet model;
	private Set<Place> startPlaces = new HashSet<Place>();
	private Map<Transition, Integer> activeTransitions = new HashMap<Transition, Integer>(); // it indicates according to the tokens in the model each transition can be fired how many times
	private Map<Place, Integer> numTokenInPlaces = new HashMap<Place, Integer>();
	private Map<Transition, LinkedList<Pair<Integer, Long>>> transitionStartTime = new HashMap<Transition, LinkedList<Pair<Integer, Long>>>(); 
	int numDiv = 0;
	int numLog = 0;
	int numModel = 0;
	// it indicates which transition started but not ended. each unfinished transition is in which location of the trace and what is its start time
	// here we assume that the timestamp is the start time
	
	
	public DurationOfActivity(Petrinet model) {
		this.model = model;
		setOutgoingPlaces();
		setIncomingPlaces();
		setOutGoingTransitionsOfPlaces();
		setIncommingTransitionsToPlaces();
		setReachableTransitionsOfeachTransition();
		for (Transition transition: model.getTransitions()) {
			Set<Place> reachablePlaces = getReachablePlacesOfOneTransition(transition);
		}
		setTransitionsThatCanReachEachTransition();
		setStartPlaces();
	}
	
	public void printResultDeviation() {
		System.out.println("num Deviation : "+numDiv);
		System.out.println("num Log Move : "+numLog);
		System.out.println("num Model Move : "+numModel);
	}
	
	public void setOutgoingPlaces() {
		Collection<Place> allPlaces = model.getPlaces();
		Collection<Transition> allTransitions = model.getTransitions();
		for (Transition transition : allTransitions) {
			Set<Place> transitionOutGoingPlaces = new HashSet<Place>();
			for (Place place : allPlaces) {
				Arc arc = model.getArc(transition, place);
				if (arc != null) {
					transitionOutGoingPlaces.add(place);
				}
			}
			outputPlacesOfTransition.put(transition, transitionOutGoingPlaces);
		}
	}
	
	public void setIncomingPlaces() {
		Collection<Place> allPlaces = model.getPlaces();
		Collection<Transition> allTransitions = model.getTransitions();
		for (Transition transition : allTransitions) {
			Set<Place> transitionIncommingPlaces = new HashSet<Place>();
			for (Place place : allPlaces) {
				Arc arc = model.getArc(place, transition);
				if (arc != null) {
					transitionIncommingPlaces.add(place);
				}
			}
			inputPlacesToTransition.put(transition, transitionIncommingPlaces);
		}
	}
	
	public void setOutGoingTransitionsOfPlaces() {
		for (Place place : model.getPlaces()) {
			Set<Transition> outGoingTransitions = new HashSet<Transition>();
			for (Transition transition: model.getTransitions()) {
				Arc arc = model.getArc(place, transition);
				if (arc != null) {
					outGoingTransitions.add(transition);
				}
			}
			if (outGoingTransitions != null) {
				outputTransitionsOfPlaces.put(place, outGoingTransitions);
			}
		}
	}
	
	public void setIncommingTransitionsToPlaces()  {
		for (Place place : model.getPlaces()) {
			Set<Transition> incommingTransitions = new HashSet<Transition>();
			for (Transition transition: model.getTransitions()) {
				Arc arc = model.getArc(transition, place);
				if (arc != null) {
					incommingTransitions.add(transition);
				}
			}
			if (incommingTransitions != null) {
				inputTransitionsToPlaces.put(place, incommingTransitions);
			}
		}
	}
	
	public Set<Place> getReachablePlacesOfOneTransition(Transition transition) {
		if (outputPlacesOfTransition.get(transition) == null) {
			return null;
		} 
	//	System.out.println("transition : "+ transition.getLabel());
		Set<Place> reachablePlaces = new HashSet<Place>(); 
		reachablePlaces.addAll(outputPlacesOfTransition.get(transition));
		Set<Place> oneStepReachablePlaces = new HashSet<Place>(); 
		oneStepReachablePlaces.addAll(outputPlacesOfTransition.get(transition));
		boolean flag = false;  // meaning that there is no outgoing tau transition
		if (oneStepReachablePlaces != null && !oneStepReachablePlaces.isEmpty()) {
			for (Place place : oneStepReachablePlaces) {
			//	System.out.println("place : "+place.getLabel());
				for (Transition tr : outputTransitionsOfPlaces.get(place)) {
					if (tr.getLabel().length()>3 && tr.getLabel().substring(0, 4).equals("tau ")) {
				//		System.out.println("tr : "+tr.getLabel());
						reachablePlaces.addAll(getReachablePlacesOfOneTransition(tr));
						reachablePlaces.addAll(outputPlacesOfTransition.get(tr));
				//		for (Place p : reachablePlaces) {
				//			System.out.print(" --> "+p.getLabel());
				//		}
						flag = true;  // meaning : at least one outgoing tau transition has been found
					//	System.out.println( "( "+ flag + ")");
					}
				}
			}
		}
		if (flag == false) {
			return outputPlacesOfTransition.get(transition);
		}
		return reachablePlaces;
	}
	
	public void setReachableTransitionsOfeachTransition() {
		for (Transition transition: model.getTransitions()) {
			Set<Place> reachablePlaces = getReachablePlacesOfOneTransition(transition);
			Set<Transition> rechableTransitions = new HashSet<Transition>();
			if (!reachablePlaces.isEmpty()) {
				for (Place place : reachablePlaces) {
					rechableTransitions.addAll(outputTransitionsOfPlaces.get(place));
				}
				if (!rechableTransitions.isEmpty()) {
					reachableTransitionsOfTransition.put(transition, rechableTransitions);
				}
			}
		}
	}
	
	public Set<Place> getcatchingPlacesOfOneTransition(Transition transition) {
		if (inputPlacesToTransition.get(transition) == null) {
			return null;
		}
		
		Set<Place> catchingPlaces = new HashSet<Place>(); 
		catchingPlaces.addAll(inputPlacesToTransition.get(transition));
		Set<Place> oneStepCatchingPlaces = new HashSet<Place>(); 
		oneStepCatchingPlaces.addAll(inputPlacesToTransition.get(transition));
		boolean flag = false;  // meaning that there is no incomming tau transition
		if (oneStepCatchingPlaces != null && !oneStepCatchingPlaces.isEmpty()) {
			for (Place place : oneStepCatchingPlaces) {
				if (inputTransitionsToPlaces.get(place) != null || !inputTransitionsToPlaces.get(place).isEmpty()) {
					for (Transition tr : inputTransitionsToPlaces.get(place)) {
						if (tr.getLabel().length()>3 && tr.getLabel().substring(0, 4).equals("tau ")) {
							catchingPlaces.addAll(getcatchingPlacesOfOneTransition(tr));
							catchingPlaces.addAll(inputPlacesToTransition.get(tr));
							flag = true;  // meaning : at least one incomming tau transition has been found
						}
					}
				}
			}
		}
		if (flag == false) {
			return inputPlacesToTransition.get(transition);
		}
		return catchingPlaces;
	}
	
	public void setTransitionsThatCanReachEachTransition() {
		for (Transition transition: model.getTransitions()) {
			Set<Place> catchingPlaces = getcatchingPlacesOfOneTransition(transition); // those places that can catch the transition (reach the transition by several steps)
			Set<Transition> catchingTransitions = new HashSet<Transition>();  // those transitions that can catch the transition (reach the transition by several steps)
			if (!catchingPlaces.isEmpty()) {
				for (Place place : catchingPlaces) {
					catchingTransitions.addAll(inputTransitionsToPlaces.get(place));
				}
				if (!catchingTransitions.isEmpty()) {
					catchingTransitionsOfEachTransition.put(transition, catchingTransitions);
				}
			}
		}
	}
	
	public void setStartPlaces() {
		for(Place place : model.getPlaces()) {
			boolean flag = true;
			for (Transition transition : model.getTransitions()) {
				Arc arc = model.getArc(transition, place);
				if (arc != null) {
					flag = false;
				}
			}
			if (flag) {
				startPlaces.add(place);
			}
		}
	}
	
	public void setActivityDuration(XTrace trace, SyncReplayResult srr) {
	// for the sacke of checking +++++
	//	int i = 0;
	//	for (XEvent event : trace)  {
	//		Date startDate = XTimeExtension.instance().extractTimestamp(trace.get(i));
	//		XAttributeDiscreteImpl nvalue = new XAttributeDiscreteImpl("time", startDate.getTime());
	//		XAttributeMap amap = event.getAttributes();
	//		amap.put("time", nvalue);
	//		i++;
	//	}// end of for the sacke of checking +++++
		Integer idxOfModel = 0;
		Integer idxOfTrace = -1;
		for (Place place : startPlaces) { // add a token to each initial transition
			numTokenInPlaces.put(place,  1);
		}
		for (Transition transition : model.getTransitions()) { // which transitions are activated at the beginning
			boolean flag = true;
			for (Place place : inputPlacesToTransition.get(transition)) {
				Integer numOfTokensInThisPlace = numTokenInPlaces.get(place);
				if (numOfTokensInThisPlace == null || numOfTokensInThisPlace == 0 ) {
					flag = false;
				}
			}
			if (flag) {
				activeTransitions.put(transition, 1);
			}
		}
		List<StepTypes> stepTypes = srr.getStepTypes();
		List<Object> nodeInstance = srr.getNodeInstance();
		int numLogMove = 0;
		int numModelMove = 0;
	//	Map<Transition, Integer> activeTransitions1 = this.activeTransitions; // ++++++
	//	Map<Place, Integer> numTokenInPlaces1 = this.numTokenInPlaces;  // ++++++
	//	Map<Transition, LinkedList<Pair<Integer, Long>>> transitionStartTime1 = this.transitionStartTime;  // ++++++
		while (idxOfModel < nodeInstance.size())  {
			StepTypes thisStepType = stepTypes.get(idxOfModel);
			 if (thisStepType.toString().equals("Invisible step") ) {
	//			 System.out.println("invisible move");
             	Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
             	idxOfModel++;
             	String thisTransitionName = thisTransition.getLabel();
             	updatePlaceTokens(thisTransition);
             //	printPlacesWithToken();
             	updateActiveTransitions();
      //       	activeTransitions1 = this.activeTransitions; // ++++++
      //  		numTokenInPlaces1 = this.numTokenInPlaces;  // ++++++
      //		transitionStartTime1 = this.transitionStartTime;  // ++++++
             } else if (thisStepType.toString().equals("Sync move")) {
            //	 System.out.println("sync move");
             	Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
             	idxOfModel++;
             	idxOfTrace++;
             	addTransitionToUnfinishedTransitions(thisTransition, trace, idxOfTrace, thisStepType);
             	updatePlaceTokens(thisTransition);
             //	printPlacesWithToken();
             	updateActiveTransitions();
             	updateUnfinishedTransitions(thisTransition, trace, idxOfTrace, thisStepType);	
     //        	activeTransitions1 = this.activeTransitions; // ++++++
     //   		numTokenInPlaces1 = this.numTokenInPlaces;  // ++++++
     //   		transitionStartTime1 = this.transitionStartTime;  // ++++++
             } else if (thisStepType.toString().equals("Model move")) {
     //       	 System.out.println("model move");
            	 numModelMove++;
             	Transition thisTransition = (Transition) nodeInstance.get(idxOfModel);
             	idxOfModel++;
             	addTransitionToUnfinishedTransitions(thisTransition, trace, idxOfTrace, thisStepType);
             	updatePlaceTokens(thisTransition);
           //  	printPlacesWithToken();
             	updateActiveTransitions();
             	updateUnfinishedTransitions(thisTransition, trace, idxOfTrace, thisStepType);	
      //       	activeTransitions1 = this.activeTransitions; // ++++++
      //  		numTokenInPlaces1 = this.numTokenInPlaces;  // ++++++
      //  		transitionStartTime1 = this.transitionStartTime;  // ++++++
             } else {
            //	 System.out.println("log move");
            	 addActivityDurationForLogMove(trace, idxOfTrace+1);
            	 numLogMove++;
            	 idxOfModel++;
               	 idxOfTrace++;
             }
		}
		
		String deviation = new String();
		if ((numLogMove + numModelMove) > 0) {
			deviation = "true";
		} else {
			deviation = "false";
		}
		
		XAttributeMap amap = trace.getAttributes();
		XAttributeLiteralImpl div = new XAttributeLiteralImpl("deviation", deviation);
		if (!amap.containsKey("deviation")) {
			amap.put("deviation", div);
		}	

		XAttributeDiscreteImpl logMove = new XAttributeDiscreteImpl("number_logMove", numLogMove);
		if (!amap.containsKey("number_logMove")) {
			amap.put("number_logMove", logMove);
		}
		
		XAttributeDiscreteImpl modelMove = new XAttributeDiscreteImpl("number_modelMove", numLogMove);
		if (!amap.containsKey("number_modelMove")) {
			amap.put("number_modelMove", modelMove);
		}
		
		if (deviation.equals("true")){
			numDiv++;
		}
		if (numLogMove > 0) {
			numLog++;
		}
		if (numModelMove > 0) {
			numModel++;
		}
	}
	
	public void updatePlaceTokens(Transition thisTransition) {
		
		for (Place place : outputPlacesOfTransition.get(thisTransition)) { // update output places of the invisible transition
    		Integer num = numTokenInPlaces.get(place);
    		if (num != null && num >= 1) {
    			num++;
    			numTokenInPlaces.remove(place);
    			numTokenInPlaces.put(place, num);
    		} else {
    			numTokenInPlaces.put(place, 1);
    		}
    	}
    	for (Place place : inputPlacesToTransition.get(thisTransition)) { // update out put places of the invisible transition
    		Integer num = numTokenInPlaces.get(place);
    		if (num != null && num >= 1) {
    			num--;
    			numTokenInPlaces.remove(place);
    			if (num >= 1) {
        			numTokenInPlaces.put(place, num);
    			}
    		}
    	}
	}
	
	public void printPlacesWithToken() {
//		System.out.println("\n \n new update : ");
		for (Place place : numTokenInPlaces.keySet()) {
			Integer num = numTokenInPlaces.get(place);
//			if (num != null && num > 0 ) {
//				System.out.println("place : "+ place.getLabel() + " num tokens "+ num);
	//		}
		}
	}
	
	public void updateActiveTransitions() {
		activeTransitions = new HashMap<Transition, Integer>();
		for (Transition transition : model.getTransitions()) {
			Integer min = 0;
			for (Place p : inputPlacesToTransition.get(transition)) {   // put the number of tokens of one of the inpute places of the transition as the minimum 
				min = numTokenInPlaces.get(p);
				if (min == null) {
					min = 0;
				}
				break;
			} 
			boolean flag = true;
			if (min > 0) {
				for (Place place : inputPlacesToTransition.get(transition)) {
					Integer num = numTokenInPlaces.get(place);
					if ( num == null ) {
						flag = false;
						break;
					}
					if (num == 0 ) {
						flag = false;
						min = 0;
						break;
					}
					if ( num < min) {
						min = num;
					} else if (num == null || num == 0) {
						min = 0;
						break;
					}
				}
			}
			if (min > 0 && flag == true) {
				Integer oldNum = activeTransitions.get(transition);
				if (oldNum == null){
					activeTransitions.put(transition, 1);
				} else {
					Integer newNum = oldNum + 1;
					activeTransitions.remove(transition);
					activeTransitions.put(transition, newNum);
				}
			}
				
		}
	}
	
	public void updateUnfinishedTransitions(Transition transition, XTrace trace, Integer idxOfTrace, StepTypes stepType) {
		int num = 0;
		Set<Transition> catchingTransitions = new HashSet<Transition>();
		if (catchingTransitionsOfEachTransition.containsKey(transition)) {
			if (catchingTransitionsOfEachTransition.get(transition) != null || !catchingTransitionsOfEachTransition.get(transition).isEmpty()) {
				catchingTransitions.addAll(catchingTransitionsOfEachTransition.get(transition));
				for (Transition tr : catchingTransitions) {
					LinkedList<Pair<Integer, Long>> trInfo = transitionStartTime.get(tr);
					if (trInfo != null) {
						if (tr.equals(transition)) {
							if (trInfo.size() <= 1) {
								continue;
							}
						}
						Pair<Integer, Long> firstFireInfo = trInfo.get(0);
						trInfo.remove(0);
						if (trInfo.isEmpty()) {
							transitionStartTime.remove(tr);
						}
						Long endTime;
						if (stepType.toString().equals("Model move") && idxOfTrace < trace.size()-1) {
							Date startDate = XTimeExtension.instance().extractTimestamp(trace.get(idxOfTrace + 1));
					    	endTime = startDate.getTime();
						} else {
							Date startDate = XTimeExtension.instance().extractTimestamp(trace.get(idxOfTrace));
					    	endTime = startDate.getTime();
						}
				    	if (firstFireInfo != null && firstFireInfo.getFirst() != -1 && firstFireInfo.getFirst()!= null && firstFireInfo.getFirst() < trace.size()) {
			//	    		System.out.println("trace length " + trace.size()+ " event index "+ firstFireInfo.getFirst());
				    		XEvent event = trace.get(firstFireInfo.getFirst());
							XAttributeMap amap = event.getAttributes();
							long duration = endTime-firstFireInfo.getSecond();
							if (duration < -1) {
								duration = 0;
							}
							XAttributeDiscreteImpl nvalue = new XAttributeDiscreteImpl("activityduration", duration);
							if (!amap.containsKey("activityduration")) {
								amap.put("activityduration", nvalue);
							}
				    	}
					}
				}
			}
		}
	}
	
	public void addActivityDurationForLogMove(XTrace trace, Integer idxOfTrace) {
	//	System.out.println(idxOfTrace);
		XEvent event = trace.get(idxOfTrace);
		XAttributeMap amap = event.getAttributes();
		long activityDuration = 0;
		if (idxOfTrace < trace.size()-1) {
			Date startDate = XTimeExtension.instance().extractTimestamp(trace.get(idxOfTrace));
			Date endDate = XTimeExtension.instance().extractTimestamp(trace.get(idxOfTrace + 1));
			activityDuration = endDate.getTime() - startDate.getTime();
		}
		XAttributeDiscreteImpl nvalue = new XAttributeDiscreteImpl("activityduration", activityDuration);
		if (!amap.containsKey("activityduration")) {
			amap.put("activityduration", nvalue);
		}
	}
	
	public void addTransitionToUnfinishedTransitions(Transition transtion, XTrace trace, Integer idxOfTrace, StepTypes stepType) {
		Date startDate = XTimeExtension.instance().extractTimestamp(trace.get(idxOfTrace));
    	Long time = startDate.getTime();
    	if (stepType.toString().equals("Model move")) {
    		Pair<Integer, Long> transitionInfo = new Pair<Integer, Long>(-1, time);
    		LinkedList<Pair<Integer, Long>> list = transitionStartTime.get(transtion);
    		if (list != null) {
    			list.add(transitionInfo);
    		} else {
    			LinkedList<Pair<Integer, Long>> newList = new LinkedList<Pair<Integer, Long>>();
    			newList.add(transitionInfo);
    			transitionStartTime.put(transtion,  newList);
    		}
        	
    	} else {
    		Pair<Integer, Long> transitionInfo = new Pair<Integer, Long>(idxOfTrace, time);
    		LinkedList<Pair<Integer, Long>> list = transitionStartTime.get(transtion);
    		if (list != null) {
    			list.add(transitionInfo);
    		} else {
    			LinkedList<Pair<Integer, Long>> newList = new LinkedList<Pair<Integer, Long>>();
    			newList.add(transitionInfo);
    			transitionStartTime.put(transtion,  newList);
    		}
    	}
	}
}
