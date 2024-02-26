package org.processmining.prediction;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

	@Plugin(
			name="Discrimination Aware Decision Tree", 
			level=PluginLevel.PeerReviewed,
			parameterLabels={"XESLog","Model", "Conformance Checking Result", "Result Replay"}, 
			returnLabels={"Predictor"}, 
			returnTypes={Predictor.class}, 
			userAccessible=true,
			categories=PluginCategory.Analytics,
			help="Creats two trees, a normal decision tree and a discrimination free decision tree. "
			)
public class PredictionPlugin {

	@PluginVariant(variantLabel="Without Replay Result",requiredParameterLabels = {0, 1, 2})
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Mahnaz", 
            email = "m.s.qafari@pads.rwth-aachen.de")		
	public Predictor performPrediction(PluginContext context, XLog log, Petrinet model, PNRepResult res) throws Exception{
		Predictor predict=new Predictor(log, model, res);
		return predict;
	}

	@PluginVariant(variantLabel="With Replay Result",requiredParameterLabels = {0, 1, 2, 3})
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Mahnaz", 
            email = "m.s.qafari@pads.rwth-aachen.de")		
	
	public Predictor performPrediction(PluginContext context, XLog log, Petrinet model, PNRepResult res, ResultReplay resReplay) throws Exception{
		Predictor predict=new Predictor(log, model, res, resReplay);
		return predict;
	}	
}
