package org.processmining.prediction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class LogInfo {
	private Map<Transition, Set<Place>> inputPlacesToTransition = new HashMap<Transition, Set<Place>>();  // places reachable from a transition by one step
	private Map<Transition, Set<Place>> outputPlacesOfTransition = new HashMap<Transition, Set<Place>>();
	private Map<Place, Set<Transition>> inputTransitionsToPlaces = new HashMap<Place, Set<Transition>>();
	private Map<Place, Set<Transition>> outputTransitionsOfPlaces = new HashMap<Place, Set<Transition>>();
	private Map<Transition, Set<Transition>> reachableTransitionsOfTransition = new HashMap<Transition, Set<Transition>>(); // transitions reachable of each transition(possibly by more than one step invisible)
	private Map<Transition, Set<Transition>> catchingTransitionsOfEachTransition = new HashMap<Transition, Set<Transition>>(); // transitions that can reach a transition (possibly by more than one step invisible)
	private Petrinet model;
	private Set<Place> startPlaces = new HashSet<Place>();
	
	public LogInfo(Petrinet model) {
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
	
	public Map<Transition, Set<Place>> getInputPlacesToTransition() {
		return inputPlacesToTransition;
	}
	
	public Map<Transition, Set<Place>> getOutputPlacesOfTransition() {
		return outputPlacesOfTransition;
	}
	
	public Map<Place, Set<Transition>> getinputTransitionsToPlaces() {
		return inputTransitionsToPlaces;
	}
	
	public Map<Place, Set<Transition>> getOutputTransitionsOfPlaces() {
		return outputTransitionsOfPlaces;
	}
	
	public Map<Transition, Set<Transition>> getReachableTransitionsOfTransition() {
		return reachableTransitionsOfTransition;
	}
	
	public Map<Transition, Set<Transition>> getCatchingTransitionsOfEachTransition() {
		return catchingTransitionsOfEachTransition;
	}
}
