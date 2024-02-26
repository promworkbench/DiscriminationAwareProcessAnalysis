package org.processmining.prediction.Augmentation;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.model.XTrace;

public class NewTraceForMultipleChoices {

	private XTrace trace;
	private String choiceLabel;
	private List<String> rightEventSet;
	private Set<XTrace> newTraces;
	
	public NewTraceForMultipleChoices (XTrace trace, String choicelabel) {
		this.trace = trace;
		this.choiceLabel = choicelabel;
		
	}
	
	public void setRightEventSet() {
		rightEventSet = new LinkedList<>();
		String s = choiceLabel;
		s  = s.substring(2, s.length() - 2);
		String[] sides = s.split("]");
		for (String strr : sides[0].split(",")) {
			if (strr.substring(strr.length()-9,strr.length()).equals("+complete")) {
				strr = strr.substring(0,strr.length()-9);
			}
			rightEventSet.add(strr);
		}
	}
	
	
}
