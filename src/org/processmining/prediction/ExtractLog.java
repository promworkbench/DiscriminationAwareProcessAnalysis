package org.processmining.prediction;

import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;


public class ExtractLog {
	@Plugin(
			name="Extract Augmented Log", 
			parameterLabels={"Predictor"}, 
			returnLabels={"XESLog"}, 
			returnTypes={XLog.class}, 
			userAccessible=true
			)
	
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Massimiliano de Leoni", 
            email = "m.d.leoni@tue.nl")		
	// it seems that this is working for a specific data log.
	// this plug in adds some of the last event attributes to the trace attributes
	public XLog performPrediction(PluginContext context, XLog log) throws Exception{
		for(XTrace trace : log)
		{
			XEvent lastEvent=trace.get(trace.size()-1);
			trace.getAttributes().put("gender", lastEvent.getAttributes().get("gender"));
			trace.getAttributes().put("agecategory", lastEvent.getAttributes().get("agecategory"));
			trace.getAttributes().put("office_u", lastEvent.getAttributes().get("office_u"));
			trace.getAttributes().put("office_w", lastEvent.getAttributes().get("office_w"));
			XAttributeDiscrete attrib = (XAttributeDiscrete) lastEvent.getAttributes().get("NumberComplaintsSoFar");
			attrib=new XAttributeDiscreteImpl("NumberComplaints", attrib.getValue());
			trace.getAttributes().put("NumberComplaints", attrib);
			attrib = (XAttributeDiscrete) lastEvent.getAttributes().get("NumberQuestionsSoFar");
			attrib=new XAttributeDiscreteImpl("NumberQuestions", attrib.getValue());			
			trace.getAttributes().put("NumberQuestions", attrib);
			attrib = (XAttributeDiscrete) lastEvent.getAttributes().get("NumberWerkMapMessagesSoFar");
			attrib=new XAttributeDiscreteImpl("NumberWerkMapMessages", attrib.getValue());			
			trace.getAttributes().put("NumberWerkMapMessages", attrib);
		}
		return(log);
	}
}
