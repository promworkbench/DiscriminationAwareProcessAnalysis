package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Serializer;
//import org.processmining.datadiscovery.estimators.Type;
import org.processmining.framework.util.Pair;
import org.processmining.models.FunctionEstimator.AbstractDecisionTreeFunctionEstimator;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.guards.Expression;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.prediction.Augmentation.AttributeValue;
import org.processmining.prediction.Augmentation.Augmentation;
import org.processmining.prediction.Augmentation.DurationOfActivity;
import org.processmining.prediction.Augmentation.PreAttributeValue;
import org.processmining.prediction.Augmentation.sub_model_duration;
import org.processmining.xeslite.external.MapDBDatabaseImpl;
import org.processmining.xeslite.external.XFactoryExternalStore.MapDBDiskImpl;

import csplugins.id.mapping.ui.CheckComboBox;

class ObjectArraySerializer extends Serializer<Object[]>
{
	private static byte INTEGER=0;
	private static byte LONG=1;
	private static byte SHORT=2;
	private static byte FLOAT=3;
	private static byte STRING=4;
	private static byte DOUBLE=5;
	private static byte DATE=6;
	private static byte NULL=7;
	private int lengthArray;
	
	public ObjectArraySerializer(int lengthArray) {
		this.lengthArray=lengthArray;
	}
	
    @Override
    public void serialize(DataOutput out, Object[] value) throws IOException {
        for(Object c : value)
        {
        	if (c==null)
        		out.writeByte(NULL);
        	else if (c instanceof Integer)
        	{
        		out.writeByte(INTEGER);
        		out.writeInt((Integer) c);
        	}
        	else if (c instanceof Long)
        	{
        		out.writeByte(LONG);
        		out.writeLong((Long) c);
        	}
        	else if (c instanceof Short)
        	{
        		out.writeByte(SHORT);
        		out.writeShort((Short) c);
        	}
        	else if (c instanceof Float)
        	{
        		out.writeByte(FLOAT);
        		out.writeFloat((Float) c);
        	}
        	else if (c instanceof Double)
        	{
        		out.writeByte(DOUBLE);
        		out.writeDouble((Double) c);
        	}
        	else if (c instanceof Date)
        	{
        		out.writeByte(DATE);
        		out.writeLong(((Date) c).getTime());
        	} 
        	else if (c instanceof String)
        	{
        		out.writeByte(STRING);
        		out.writeUTF((String) c);
        	} 
        	else
        	{
        		System.err.println("The type "+c.getClass()+" is not supported");
        		out.writeByte(NULL);
        	}
        		
        }
    }

    @Override
    public Object[] deserialize(DataInput in, int available) throws IOException {
        Object[] ret = new Object[this.lengthArray];
        for(int i=0;i<lengthArray;i++)
        {
    		byte type = in.readByte();
    		if (type==NULL)
    			ret[i]=null;
        	if (type==INTEGER)
        		ret[i]=in.readInt();
        	else if (type==LONG)
        		ret[i]=in.readLong();
        	else if (type==SHORT)
        		ret[i]=in.readShort();
        	else if (type==FLOAT)
        		ret[i]=in.readFloat();
        	else if (type==DOUBLE)
        		ret[i]=in.readDouble();
        	else if (type==DATE)
        		ret[i]=new Date(in.readLong());
        	else if (type==STRING)
        		ret[i]=in.readUTF();

        }
        return ret;
    }

    @Override
    public boolean isTrusted() {
        return true;
    }

    @Override
    public boolean equals(Object[] a1, Object[] a2) {
        return Arrays.equals(a1,a2);
    }

    public int hashCode(Object[] bytes, int seed) {
        return Arrays.hashCode(bytes);
    }

    @Override
    public BTreeKeySerializer getBTreeKeySerializer(Comparator comparator) {
        if(comparator!=null && comparator!=Fun.COMPARATOR) {
            return super.getBTreeKeySerializer(comparator);
        }
        return BTreeKeySerializer.BASIC;
    }	
}


public class Predictor 
{
	public final static String notAllowedChars=".()&!|=<>-+*/% ";
	private Map<String, Type> types;
	private Map<String, Type> typesNDC;
	private Petrinet model;
	private Set<Object[]> instanceSet;
	private Augmentation[] augementationArray;
	private XLog originalLog;
	private Map<String, Set<String>> literalValues;
	private Map<String, Set<String>> literalValuesNDC;
	protected AbstractDecisionTreeFunctionEstimator df=null;
	private Map<XTrace, Object[]> instanceOfATrace=new HashMap<XTrace, Object[]>();
	private Map<XTrace,Map<String, Object>> instanceOfATraceWNDC=new HashMap<XTrace,Map<String,Object>>();
	private Map<Map<String, Object>, Integer> instancesOfNDC = new HashMap<Map<String, Object>, Integer>();
	private boolean binarySplit=false;
	private float confidenceThreshold=-1;
	private double minNumInstancePerLeaf=0.25;
	private int epsilon = 20;
	private int numFoldErrorPruning=-1;
	private boolean saveData=false;
	private boolean unPruned=true;
	private int numIntervals=-1;
	private Collection<String> activitiesToConsider = new ArrayList<String>();
	private Augmentation outputAttribute = null;
	private DiscretizationInterval intervals[]=null;
	private final ArrayList<String> activityCollection=new ArrayList<String>();
	private ResultReplay resReplay;
	private PNRepResult replayREsultForChoiceAndTime;
	//private ComplianceCheckerOutput compOut;
	private boolean isOutputAttributeChanged=true;
	private boolean isSetactivitiesToConsiderChanged;
	private DiscrMethod discrMethod=DiscrMethod.UNSET;
	private boolean hasLogBeenAugmented;
	private HashSet<String> timeIntervalAugmentations;
	private boolean regressionTree=false;
	private boolean hasAlgorithmChanged=true;
	private boolean discoveryParamChanged=true;
	private ArrayList<String> originalLogAttributes=new ArrayList<String>();
	private MapDBDatabaseImpl mapDBDatabaseImpl;
	private XLog log;
	private int numInstancesAddedForLearning;
	public final static String CASE_ACTIVITY="Case";
	private static final String REMAINING = "Remaining Instances";
	//-->
	private Map<String, Set<String>> traceAttributesValues = new HashMap<String, Set<String>>();
	private Set<String> traceAttributeNames;
	private String targetType = null;
	private String[] allwholePathOptions = {"trace_duration", "trace_delay"};
	private long traceDelayThreshold;
	private long minTraceDuration = 0;
	private long maxTraceDuration = 0;
//	private String selectedWholePathAttribute = null;
	private Set<Place> selectedORplaces = new HashSet<Place>();
	private Set<Transition> selectetSub_model;
	private Set<String> selectedTransitions;
	private String sensitiveAttrebute;
	private Set<String> protectedValues = new HashSet<String>(Arrays.asList("below"));
	private int sensitiveAttributeIndex ;
//	private Set<HashMap<String,Object>> fairInstanceSet;
	private Map<String, String> orderOfAttributes;
	private ArrayList<Map<String, Object>> orderedInstancesNDC;
	public Object[] outputValuesAsObjects;
	public TreeConstructor tc;
	public String targetActivityName;  //if the dependent attribute is one of the event attributes, it belongs to this activity
	public Set<String> desirableOutcomes = new HashSet<String>(Arrays.asList("below"));  // the set of desirable outcomes if the dependent attribute is literal or boolean or "below"or "above"if discrete date or continues
	private int targetThreshold = 50;  //if the dependent attribute is of type discrete date or continues, then it is the threshold of two possible good and bad outcome
	private int sensitiveThreshold = 50;
	private String dependentAttName;  // the complete dependent att name.  attName if trace attribute; actName_attName if event attribute
	private int NDCtoDC = 0;  // 1 if just desirable classified leave can be relabeled to NodDesirable; -1 other direction; 0 no limit (all leave can be relabeled)
	
	public void setNDCtoDC(int i) {
		if(i == 1 || i == -1 )
			NDCtoDC = i;
		else
			NDCtoDC = 0;
	}
	public void setDependentAttName(String name) {
		this.dependentAttName = name;
	}
	
	public Map<Map<String, Object>, Integer> getInstancesOfNDC() {
		return instancesOfNDC;
	}
	
	public void setEventTargetThreshold (int thresholdPercent) {
		this.targetThreshold = thresholdPercent;
	}
	
	public void setSensitiveThreshold (int thresholdPercent) {
		this.sensitiveThreshold = thresholdPercent;
	}
	
	public Map<String, Type> getTypesNDC() {
		return typesNDC;
	}
	
	public void setDesirableOutcome(Collection<String> desirableOutcomes) {
		this.desirableOutcomes = new HashSet<String>();
		this.desirableOutcomes.addAll(desirableOutcomes);
	} 
	
	public void setEpsilon(int epsilon) {
		this.epsilon = epsilon;
	}
	
	public Set<String> getDesirableOutcome() {
		return desirableOutcomes;
	}
	
	public Map<String, Set<String>> getLiteralValuesNDC () {
		return literalValuesNDC;
	}
	
	public void setTargetActivityName(String targetActivityName) {
		this.targetActivityName = targetActivityName;
	}
	
	public void setSensitiveAttributeIndex( int i) {
		sensitiveAttributeIndex = i;
	}
	
	public int getSensitiveAttributeIndex() {
		return sensitiveAttributeIndex;
	}
	
	public long getMinTraceDuration() {
		return minTraceDuration;
	}
	
	public long getMaxTraceDuration() {
		return maxTraceDuration;
	}
	
	public void setSelectedORplaces (Set<Place> selectedORPlaces) {
		this.selectedORplaces = new HashSet<Place>();
		this.selectedORplaces.addAll(selectedORPlaces);
		
	}
	
	public Set<Place> getSelectedORplace () {
		return selectedORplaces;
	}

	public void setSelectetSub_model (Set<Transition> selectedTransitions) {
		this.selectetSub_model = new HashSet<Transition>();
		this.selectetSub_model.addAll(selectedTransitions);
	}
	
	public Set<Transition> getSelectetSub_model() {
		return selectetSub_model;
	}
	
	public void setTraceDelayThreshold (long traceDelayThreshold) {
		XTrace trace = log.get(0);
		long min = wholeTraceDuration(trace);
		long max = wholeTraceDuration(trace);
		long avg = 0;
		for (XTrace t : log) {
			long temp = wholeTraceDuration(t);
			avg = avg + temp;
			if (temp < min) {
				min = temp;
			}
			if (temp > max) {
				max = temp;
			}
		}
		System.out.println(" min : " + min);
		System.out.println(" max : " + max);
		System.out.println(" avg : " + avg/log.size());
		this.traceDelayThreshold = ((max - min) * traceDelayThreshold) / 100 ;
	}
	
	public long getTraceDelayThreshold () {
		return traceDelayThreshold;
	}
	
	public void setSensitiveAttrebutName (String attName) {
		sensitiveAttrebute = attName;
	}
	
	public String getSensitiveAttrebute () {
		return sensitiveAttrebute;
	}

	public void setProtectedValues (Collection collection) {
		protectedValues = new HashSet<String>();
		protectedValues.clear();
		protectedValues.addAll(collection);
	}
	
//	public void setSelectedWholePathAttribute (String attributeName) {
//		selectedWholePathAttribute = new String();
//		selectedWholePathAttribute = attributeName;
//	}
	
	public Collection<String> getActivitiesToConsider () {
		if (activitiesToConsider.isEmpty()) {
			return null;
		}
		return activitiesToConsider;
	}
	//<--
	
	public JComponent getPrefuseTreeVisualization()
	{
		return df.getPrefuseTreeVisualization();
	}

	public JComponent getNormalTreeVisualization()
	{
		return df.getVisualization();
	}
	
	public JComponent getFairAndNormalTreeVisualization() throws Exception
	{
		return tc.getBothTreeVisualization();
	}
	
	public double classify(Map<String, Object> variableAssignment) throws Exception
	{
		return df.classify(variableAssignment);
	}
	
	public TreeConstructor getTreeConstructor() {
		return tc;
	}
	

	@SuppressWarnings("deprecation")
	public Pair<String[], XLog[]> clusterLog(boolean onlyCorrectlyClassified, double maxDeviation) throws Exception
	{
		if (maxDeviation>1 && maxDeviation<=100)
			maxDeviation/=100.0;
		else if (maxDeviation>1 || maxDeviation<0)
			return null;	
		List<Pair<String, Expression>> listExpressions;
		if (df instanceof Leafable)
		{
			listExpressions = ((Leafable)df).getExpressionsAtLeaves();
		}
		else
		{
			Map<Object, Pair<Expression, Double>> values = df.getEstimation(null, false);
			listExpressions=new LinkedList<Pair<String,Expression>>();
			for(Entry<Object, Pair<Expression, Double>> entry : values.entrySet())
			{
				listExpressions.add(new Pair<String,Expression>(entry.getKey().toString(),entry.getValue().getFirst()));
			}
		}
		if (listExpressions==null)
			return null;
		int size=listExpressions.size();
		if (onlyCorrectlyClassified)
			size++;
		String[] objectArray=new String[size];
		Expression[] exprArray=new Expression[size];
		XLog retValue[]=new XLog[size];


		int j=0;
		for(Pair<String, Expression> entry : listExpressions)
		{
			objectArray[j]=entry.getFirst();
			exprArray[j]=entry.getSecond();
			retValue[j++]=XFactoryRegistry.instance().currentDefault().createLog();
		}
		if (onlyCorrectlyClassified)
		{
			objectArray[j]=REMAINING;
			exprArray[j]=null;
			retValue[j]=XFactoryRegistry.instance().currentDefault().createLog();
		}
		for(XTrace trace : originalLog)
		{
			final Hashtable<String,Object> variableValues=new Hashtable<String, Object>();
			Object[] instance = instanceOfATrace.get(trace);
			
			for(int i=0;i<instance.length-1;i++)
				if (instance[i]!=null)
					variableValues.put(augementationArray[i].getAttributeName(), instance[i]);

			for(j=0;j<exprArray.length;j++)
			{
				if (exprArray[j]==null || exprArray[j].isTrue(variableValues))
				{
					boolean isOK=false;
					double valAsNumber=0;
					double secVal=-1;
					boolean isANumber=false;
					try
					{
						valAsNumber=Double.parseDouble(objectArray[j]);
						isANumber=true;
					}
					catch(NumberFormatException nfe) {}
					if (!isANumber)
					{
						try
						{
							String value[]=objectArray[j].replace('[', ' ').replace(']', ' ').replace(',', ' ').trim().split(" ");
									
							if (value.length==2)
							{
								valAsNumber=Double.parseDouble(value[0]);
								secVal=Double.parseDouble(value[1]);
							}
						}
						catch(NumberFormatException nfe) {}
					}
					if (onlyCorrectlyClassified && secVal>=valAsNumber && 
							variableValues.get(outputAttribute.getAttributeName()) instanceof Number)
					{
						double value=((Number)variableValues.get(outputAttribute.getAttributeName())).doubleValue();
						if (objectArray[j].indexOf(']')<0)
							isOK= (value>=valAsNumber && value< secVal);
						else
							isOK= (value>=valAsNumber && value <= secVal);
							
					}
					else if (onlyCorrectlyClassified && isANumber && 
							variableValues.get(outputAttribute.getAttributeName()) instanceof Number)
					{
						double actVal=((Number)variableValues.get(outputAttribute.getAttributeName())).doubleValue();
						if (Math.abs((actVal-valAsNumber)/actVal)<maxDeviation)
							isOK=true;
						else
							isOK=false;
								
					}
					
					if (!onlyCorrectlyClassified || objectArray[j].equals(REMAINING) || isOK || 
							objectArray[j].equals(variableValues.get(outputAttribute.getAttributeName())))
					{
						
						XTrace aNewTrace=XFactoryRegistry.instance().currentDefault().createTrace(trace.getAttributes());
						
						for(XEvent event : trace)
							if (!CASE_ACTIVITY.equals(XConceptExtension.instance().extractName(event)))
								aNewTrace.add(XFactoryRegistry.instance().currentDefault().createEvent(event.getAttributes()));
						retValue[j].add(aNewTrace);
						break;
					}
				}
			}
		}
		String[] description=new String[exprArray.length];
		for(int i=0;i<exprArray.length;i++)
		{
			if (objectArray[i]!=REMAINING)
				description[i]=objectArray[i].toString()+". Expression: "+exprArray[i];
			else
				description[i]=null;
		}
		return new Pair<String[],XLog[]>(description,retValue);
	}
	
	public Predictor(XLog log, ResultReplay resReplay) 
	{
		this.log=log;
		this.resReplay = resReplay;
	}
	
	public Predictor(XLog log, Petrinet model, PNRepResult res) 
	{
		this.log=log;
		this.model = model;
		this.replayREsultForChoiceAndTime = res;
	}
	
	public Petrinet getModel() {
		return model;
	}
	// set up "originalLog" 
	//      add "startCaseEvent" and "endCaseEvent" to every trace, with the first and last time stamp in the trace respectively
	// set up "originalLogAttributes"
	// 		gather all the event attributes in the log
	// set up "activityCollection"
	//		gather all the activity names in the log
	public void init()
	{
		MapDBDiskImpl factory = new MapDBDiskImpl();
		originalLog=factory.createLog(log.getAttributes());
		//-->
		XTrace firstTrace = log.get(0);
		traceAttributeNames = new HashSet<String>();
		traceAttributeNames.addAll(firstTrace.getAttributes().keySet());
		Set<String> temp = new HashSet<String>();
		for (String s : traceAttributeNames) {
			temp.add(replaceNotAllowedStrings(s));	
		}
		traceAttributeNames = temp; // now the set of attribute names of the trace does not have any forbidden character
		
		Collection<Place> places = new HashSet<Place>();
		places.addAll(model.getPlaces());
		
		if (replayREsultForChoiceAndTime != null) {  // adding the choice information and the duration of each action as attributes to the events
			ORplaces orp = new ORplaces( model);
			DurationOfActivity ad = new DurationOfActivity( model);
			for (SyncReplayResult singleVariantReplay : replayREsultForChoiceAndTime) {
				Set<Integer> allTraceIdxOfThisVariant = singleVariantReplay.getTraceIndex();
				for (Integer traceIdx : allTraceIdxOfThisVariant) {
	//				System.out.println("trace idx line 544 "+ traceIdx);
					orp.enrichTraceWithORChoices(log.get(traceIdx), singleVariantReplay, traceIdx);
					ad.setActivityDuration(log.get(traceIdx), singleVariantReplay);
				}
			}
			
		System.out.println("oout line 549");	
			
		}
		minTraceDuration = wholeTraceDuration(log.get(0));
		//<--
		HashSet<String> tempAttributeSet = new HashSet<String>();
		HashSet<String> tempActivitySet = new HashSet<String>();
		traceAttributesValues= new HashMap<String, Set<String>>(getTraceLiteralValuesMap(log));
		for (XTrace trace : log)
		{	
			traceAttributeNames.addAll(trace.getAttributes().keySet());
			XTrace newTrace = factory.createTrace(trace.getAttributes());
			XEvent startCaseEvent=new XEventImpl(); // creates a new "startCaseEvent" for the trace with all the attributes of the trace
			startCaseEvent.setAttributes(trace.getAttributes());
			XConceptExtension.instance().assignName(startCaseEvent, CASE_ACTIVITY);
			Date initTimestamp = null;
			for(XEvent event : trace) //assign the first time stump in the trace to the "startCaseEvent" of the new trace
			{
				initTimestamp = XTimeExtension.instance().extractTimestamp(event);
				if (initTimestamp!=null)
				{
					XTimeExtension.instance().assignTimestamp(startCaseEvent, initTimestamp);
					break;
				}
			}
			if (initTimestamp==null)
			initTimestamp = new Date(0);
			XLifecycleExtension.instance().assignTransition(startCaseEvent, "start");
			XEvent endCaseEvent=new XEventImpl(); // create a new "endCaseEvent" for the trace
			XConceptExtension.instance().assignName(endCaseEvent, CASE_ACTIVITY);
			for(int i=trace.size()-1;i>=0;i--) //assign the last time stump in the trace to the "endCaseEvent" of the new trace
			{
				XEvent event = trace.get(i);
				Date timestamp = XTimeExtension.instance().extractTimestamp(event);
				if (timestamp!=null)
				{
					XTimeExtension.instance().assignTimestamp(endCaseEvent, timestamp);
					break;
				}
			}
			XLifecycleExtension.instance().assignTransition(endCaseEvent, "complete");
			newTrace.add(startCaseEvent); // add the new "start event" to the trace
			for(XEvent event : trace)
			{
				tempActivitySet.add(XConceptExtension.instance().extractName(event));
				for(String attr : event.getAttributes().keySet()) // gathers all the attributes in the event except those that are mentioned
				{
					if (!attr.startsWith("concept:") && !attr.startsWith("time:") && !attr.startsWith("resource:") && !attr.startsWith("org:") && !attr.startsWith("role:"))
					{
						tempAttributeSet.add(attr);
					}					
				}

				XEvent newEvent=factory.createEvent(event.getAttributes());
				Date timestamp=XTimeExtension.instance().extractTimestamp(event);
				if (timestamp==null)
				{
					XTimeExtension.instance().assignTimestamp(newEvent, initTimestamp);
				}
				else
					initTimestamp=timestamp;
				newTrace.add(newEvent);
			}
			newTrace.add(endCaseEvent);   //each trace in "originalLog" would be as [startCaseEvent, trace, endCaseEvent]
			// also it is ensured that the time stump of all its events is not null (at list it is equal to "initTimestamp")
			// -->
			long duration = wholeTraceDuration(newTrace);
			if (duration < minTraceDuration) {
				minTraceDuration = duration;
			}
			if (duration > maxTraceDuration) {
				maxTraceDuration = duration;
			}
			
			originalLog.add(newTrace);
		}
		originalLogAttributes.addAll(tempAttributeSet); // collect all the event attribute names in the log except those
		// that start with "concept:", "time:", "resource:" and "org:"
		Collections.sort(originalLogAttributes);
		activityCollection.addAll(tempActivitySet); // collect all the event names in the log
		Collections.sort(this.activityCollection);
		
		for (String s : activityCollection) {
			activitiesToConsider.add(s);
		}
		
		cleanOriginalLogAttributes();
		augmentLog(new Augmentation[0],false,null);
		System.out.println(" augment log init();");
		augmentLogNDC(new Augmentation[0],false,null);
		System.out.println(" end of init();");
		
	}
	
	public Predictor(XLog log, Petrinet model, PNRepResult res, ResultReplay resReplay) {
		this.log = log;
		this.resReplay=resReplay;
		this.replayREsultForChoiceAndTime = res;
		this.model = model;
	}
	

	public static String getName(XAttributable element) {
		XAttributeLiteral name = (XAttributeLiteral) element.getAttributes().get("concept:name");
		return name.getValue();
	}
	
	public boolean configureAugmentation(Augmentation[] augmentationCollection)
	{
		for(Augmentation aug : augmentationCollection)
		{
			aug.setLog(originalLog);
			String paramNames[]=aug.getParameterNames();
			for(int i=0;i<paramNames.length;i++)
			{
				String[] value;
				do
				{
					if (aug.getPossibleValuesForParameter(i)==null)
					{
						value=new String[1];
						value[0]=(String) JOptionPane.showInputDialog(null, "Please set the value for "+paramNames[i],
							"Attribute "+aug.getAttributeName(),JOptionPane.PLAIN_MESSAGE,null,null,aug.getDefaultValueForParameter(i)[0]);
					}
					else
						if (!aug.multipleValuesForParameter(i))
						{
							value=new String[1];
							value[0]=(String) JOptionPane.showInputDialog(null, "Please set the value for "+paramNames[i],
								"Attribute "+aug.getAttributeName(),JOptionPane.PLAIN_MESSAGE,null,aug.getPossibleValuesForParameter(i),aug.getDefaultValueForParameter(i)[0]);
						}
						else
						{
							JPanel p=new JPanel(new BorderLayout());
							CheckComboBox cbb=new CheckComboBox(aug.getPossibleValuesForParameter(i));
							Dimension dim=cbb.getPreferredSize();
							cbb.addSelectedItems(aug.getDefaultValueForParameter(i));
							dim.width*=2;
							cbb.setPreferredSize(dim);
							p.add(cbb,BorderLayout.CENTER);
							p.add(new JLabel("Please set the value for "+paramNames[i]),BorderLayout.NORTH);
							int yn=JOptionPane.showConfirmDialog(null, 
									p,"Attribute "+aug.getAttributeName(),JOptionPane.YES_NO_OPTION);
							if (yn==JOptionPane.NO_OPTION)
								value=null;
							else
								value=(String[]) cbb.getSelectedItems().toArray(new String[0]);
							
						}
					if (value==null || value.length==0 || value[0]==null)
						return false;
				} while(!aug.setParameter(i, value));
			}
			if (aug.isTimeInterval())
			{
				timeIntervalAugmentations.add(aug.getAttributeName());
			}
		}
		return true;
	}
	
	public boolean augmentLogNDC(Augmentation[] augmentationCollection, boolean useMapDB, TaskForProgressBar task) 
	{
		typesNDC=new HashMap<String, Type>();
		literalValuesNDC=new HashMap<String, Set<String>>();
		instanceOfATraceWNDC = new HashMap<XTrace,Map<String,Object>>();
		instancesOfNDC = new HashMap<Map<String, Object>, Integer>();
		orderedInstancesNDC = new ArrayList<Map<String, Object>>();
		int total = 0;

		this.augementationArray=augmentationCollection;
		
		// if "Choice_Attribute" Augmentation is chosen, it adds an augmentation for each selected OR place
		int numselectedOrPlaces;
		boolean haveRawChoiceAug = false;
		Set<Augmentation> augSet = new HashSet<Augmentation>();
		for (Augmentation aug : augmentationCollection) {
			if (aug.getAttributeName().length() > 6 && aug.getAttributeName().substring(0, 7).equals("Choice_")) {
				haveRawChoiceAug = true;		//include any augmentation but choice ones	
			} else {
				augSet.add(aug);
			}
		}
		
		if (augmentationCollection.length > 0) {
			Augmentation[] newAugmentationCollection = new Augmentation[augSet.size() + selectedORplaces.size()];
			if (haveRawChoiceAug) {
				if (selectedORplaces != null) {
					int i = 0;
					for (Augmentation aug : augSet) {
						newAugmentationCollection[i] = aug;
						i++;
					}
					for (Place place : selectedORplaces) {
						Augmentation aug;
						aug = new AttributeValue(replaceNotAllowedStrings("Choice_"+place.getLabel()));
						newAugmentationCollection[i] = aug;
						i++;
					}
					augmentationCollection = new Augmentation[newAugmentationCollection.length];
					for (i=0; i< newAugmentationCollection.length; i++) {
						augmentationCollection[i] = newAugmentationCollection[i];
					}
				}
			}
		}
		
		this.augementationArray=augmentationCollection;
		
		LinkedList<Augmentation> traceAugs = new LinkedList<Augmentation>();  // the collection of attributes that belongs to the whole trace
		LinkedList<Augmentation> eventAugs = new LinkedList<Augmentation>();  // the collection of attributes that belongs to an event + choice Attributes
		
		// traceAugs are the augmentations related to trace which are presented in the trace attributes
		// eventAugs are the augmentations related to events which are presented in the event attributes
		// here the Choice attribute is an event augmentation
		for (Augmentation aug : augementationArray) {
			String augName = aug.getAttributeName();
			if (augName.equals("trace_delay") || augName.equals("trace_duration") || augName.equals("Sub_Model_Attribute")
					|| traceAttributeNames.contains(augName)) {
				traceAugs.add(aug);
			} else {
				eventAugs.add(aug);
			}
		}
		// compute the sub_model duration time and add it to the trace attributes
		Set<String> traceAugNames = new HashSet<String>();
		boolean flag = false;
		for(Augmentation aug : traceAugs) {
			if (aug.getAttributeName().equals("Sub_Model_Attribute")) {
				flag = true;
			}
		}
		// if the "Sub_Model_Attribute" Augmentation is chosen, this part computes and adds the relevant attribute to each trace
		if (flag && selectetSub_model != null) {
			sub_model_duration smd = new sub_model_duration(selectetSub_model, log, model, replayREsultForChoiceAndTime);
			Map<Integer, Long> smdValues = smd.sub_modelDurations();
			for (Integer traceIdx : smdValues.keySet()) {
				XTrace trace = originalLog.get(traceIdx);
				XAttributeMap amap = trace.getAttributes();
				XAttributeDiscreteImpl nvalue = new XAttributeDiscreteImpl("sub_model_duration", smdValues.get(traceIdx));
				if (amap.containsKey("sub_model_duration")) {
					amap.remove("sub_model_duration");
				}
				amap.put("sub_model_duration", nvalue);
				XEvent event = trace.get(0);
				XAttributeMap amapEvent = event.getAttributes();
				if (amapEvent.containsKey("sub_model_duration")) {
					amapEvent.remove("sub_model_duration");
				}
				amapEvent.put("sub_model_duration", nvalue);
			}
		}
		
		int numTraces = originalLog.size();
//		System.out.println("originalLog size : "+ originalLog.size());
		boolean isOutputAugATraceAug = false;
		boolean isOutputAugIsChoice = false;
		if (outputAttribute != null) {
			if (outputAttribute.getAttributeName().length() > 6 && outputAttribute.getAttributeName().substring(0, 7).equals("Choice_")) {
				String[] s = outputAttribute.getAttributeName().split("_to_", 2);
				Augmentation aug;
				aug = new AttributeValue(replaceNotAllowedStrings(s[0]));
				outputAttribute = aug;
				isOutputAugIsChoice = true;
			}
			
			for (Augmentation aug : traceAugs) {
				if (aug.getAttributeName().equals(outputAttribute.getAttributeName())) {
					isOutputAugATraceAug = true;
				}
			}
		}
		
		
		if (outputAttribute == null || isOutputAugATraceAug) {
			int elaboratedTrace = 0;
			for(XTrace trace : originalLog) {
				Map<String, Object> newInstanceNDC = new HashMap<String, Object>();
				doTraceAugmentations(trace, traceAugs, newInstanceNDC);
				if (eventAugs != null && !eventAugs.isEmpty()) {
					for (int eventIdx = trace.size()-1; eventIdx >= 0; eventIdx--) {
						XEvent event = trace.get(eventIdx);
						for (Augmentation eventAug : eventAugs) {
							String augName = eventAug.getAttributeName();
							if (augName.length()>6 && augName.substring(0, 7).equals("Choice_")) {
								String[] s = augName.split("_to_", 2);
								String choicePlaceName = s[0].substring(7,s[0].length());
								if (!newInstanceNDC.containsKey(choicePlaceName)) {
									XAttributeMap amap = event.getAttributes();
									boolean hasThisChoiceAtt = false;
									for (String key: amap.keySet()) {
										if (replaceNotAllowedStrings(key).equals("Choice_"+choicePlaceName)) {
											hasThisChoiceAtt = true;
										}
									}
									if (hasThisChoiceAtt) {
										XAttribute att = amap.get("Choice_"+choicePlaceName);
										String eventChoice = new String();
										eventChoice = (String) getAttributeValues(event.getAttributes().get("Choice_"+choicePlaceName));
					//					System.out.println(" The truble");
										if (eventChoice != null && !eventChoice.equals("NOT SET")) {
											newInstanceNDC.put(choicePlaceName, eventChoice);
											doUpdate(choicePlaceName, newInstanceNDC);
										}
									}
								}
							}  // end of choice augmentation
							else if (activitiesToConsider != null) {
								String eventName = (String) getAttributeValues(event.getAttributes().get("concept:name"));
								if (activitiesToConsider.contains(eventName)) {
									String eventAugName = replaceNotAllowedStrings(eventName)+"_"+eventAug.getAttributeName();
									if (!newInstanceNDC.containsKey(eventAugName)) {
										newInstanceNDC.put(eventAugName, eventAug.returnAttribute(event, trace, sensitiveAttrebute, protectedValues, traceDelayThreshold));
									}
									doUpdate(eventAugName, newInstanceNDC);
								}
							}
						} //end of aug \in eventAug
					}  // end of events in the trace
				}
				instanceOfATraceWNDC.put(trace, newInstanceNDC);
				if (!instancesOfNDC.keySet().contains(newInstanceNDC)) {
					orderedInstancesNDC.add(newInstanceNDC);
					instancesOfNDC.put( newInstanceNDC, 1);
					total++;
				} else {
					Integer num = instancesOfNDC.get(newInstanceNDC);
					instancesOfNDC.remove(newInstanceNDC);
					instancesOfNDC.put(newInstanceNDC, num+1);
					total++;
				}
				
				if (task!=null)
					task.myProgress((++elaboratedTrace*100)/numTraces);
	//			System.out.println("elaboratedTrace :: " + elaboratedTrace);
				
			}  // end for trace
		}  // end of if traceAugs.contains(outputAttribute)
		else {
			int elaboratedTrace = 0;
			for(XTrace trace : originalLog) {
				Map<String, Object> newInstanceNDC = new HashMap<String, Object>();
				doTraceAugmentations(trace, traceAugs, newInstanceNDC);
				if (eventAugs != null && !eventAugs.isEmpty()) {
					for (int eventIdx = 0; eventIdx < trace.size(); eventIdx++) {
						XEvent event = trace.get(eventIdx);
						for (Augmentation eventAug : eventAugs) {
							String augName = eventAug.getAttributeName();
							if (augName.length()>6 && augName.substring(0, 7).equals("Choice_")) {
								String[] s = augName.split(" -->", 2);
								String choicePlaceName = s[0].substring(7,s[0].length());
								//+++++++++++
								XAttributeMap amap = event.getAttributes();
								boolean hasThisChoiceAtt = false;
								for (String key: amap.keySet()) {
									if (replaceNotAllowedStrings(key).equals("Choice_"+choicePlaceName)) {
										hasThisChoiceAtt = true;
									}
								}
								if (hasThisChoiceAtt) {
									XAttribute att = amap.get("Choice_"+choicePlaceName);
									String eventChoice = new String();
									eventChoice = (String) getAttributeValues(event.getAttributes().get("Choice_"+choicePlaceName));
					//				System.out.println(" The truble");
									if (eventChoice != null && !eventChoice.equals("NOT SET")) {
										if (newInstanceNDC.containsKey(choicePlaceName)) {
											newInstanceNDC.remove(choicePlaceName);
										}
										newInstanceNDC.put(choicePlaceName, eventChoice);
										doUpdate(choicePlaceName, newInstanceNDC);
										if (isOutputAugIsChoice) {
											if (outputAttribute.getAttributeName().substring(7, outputAttribute.getAttributeName().length()).equals(choicePlaceName)) {
												instanceOfATraceWNDC.put(trace, newInstanceNDC);
												if (!instancesOfNDC.keySet().contains(newInstanceNDC)) {
													orderedInstancesNDC.add(newInstanceNDC);
													instancesOfNDC.put( newInstanceNDC, 1);
													total++;
												}  else {
													Integer num = instancesOfNDC.get(newInstanceNDC);
													instancesOfNDC.remove(newInstanceNDC);
													instancesOfNDC.put(newInstanceNDC, num+1);
													total++;
												}
											}
										}
									}
								}
								//+++++++++++
								
							} // end of choice augmentation  
						 else if (activitiesToConsider != null) {
								String eventName = (String) getAttributeValues(event.getAttributes().get("concept:name"));
								if (activitiesToConsider.contains(eventName)) {
									String eventAugName = replaceNotAllowedStrings(eventName)+"_"+eventAug.getAttributeName();
									if (newInstanceNDC.containsKey(eventAugName)) {
										newInstanceNDC.remove(eventAugName);
									}
									newInstanceNDC.put(eventAugName, eventAug.returnAttribute(event, trace, sensitiveAttrebute, protectedValues, traceDelayThreshold));
									if (eventAug.equals(outputAttribute) && eventName.equals(targetActivityName)) {
										instanceOfATraceWNDC.put(trace, newInstanceNDC);
										if (!instancesOfNDC.keySet().contains(newInstanceNDC)) {
											orderedInstancesNDC.add(newInstanceNDC);
											instancesOfNDC.put( newInstanceNDC, 1);
											total++;
										}  else {
											Integer num = instancesOfNDC.get(newInstanceNDC);
											instancesOfNDC.remove(newInstanceNDC);
											instancesOfNDC.put(newInstanceNDC, num+1);
											total++;
										}
									}
									doUpdate(eventAugName, newInstanceNDC);
								}
							}
						} //end of aug \in eventAug
					}  // end of for each event in the trace
				}
				if (task!=null)
					task.myProgress((++elaboratedTrace*100)/numTraces);
	//			System.out.println("elaboratedTrace "+elaboratedTrace);
			}  // end of for each trace in the log
		}
		System.out.println("instances size : "+instancesOfNDC.size());
		System.out.println("total : "+total);
		orderedInstancesNDC = new ArrayList< Map<String, Object>>();
		for (Map<String, Object> inst : instancesOfNDC.keySet()) {
			orderedInstancesNDC.add(inst);
		}
		
		Collections.sort(orderedInstancesNDC, new Comparator<Map<String, Object>>() {
			@Override 
			public int compare(Map<String, Object> m1, Map<String, Object> m2) {
				if (m1 == null && m2 == null) {
					return 0;
				} else if (m1 == null && m2 != null) {
					return -1;
				} else if (m1 != null && m2 == null) {
					return 1;
				} else {
					return comparInstance(m1, m2);
				}
			}
		} );
		
	hasLogBeenAugmented=true;
	if (task!=null)
	    task.myProgress(100);
	return true;
	}
	
	public int comparInstance(Map<String, Object> m1, Map<String, Object> m2) {
		if (m1 == null && m2 == null) {
			return 0;
		} else if (m1 == null && m2 != null) {
			return -1;
		} else if (m1 != null && m2 == null) {
			return 1;
		} else { 
			String[] keys = new String[typesNDC.size()];
			int j = 0;
			for (String str : typesNDC.keySet()) {
				keys[j] = str;
				j++;
			}
			Arrays.sort(keys, new Comparator<String>() {

				public int compare(String s1, String s2) {
					return s1.compareTo(s2);
				}
			});
			
			// move the time attribute names to the end
			String[] newKeys = new String[typesNDC.size()];
			int frontIdx = 0;
			int backIdx = keys.length-1;
			for (int i = keys.length-1; i >= 0; i--) {
				if (typesNDC.get(keys[i]).equals(Type.TIMESTAMP)) {
					newKeys[backIdx] = keys[i];
					backIdx--;
				} else {
					newKeys[frontIdx] = keys[i];
					frontIdx++;
				}
			}
			
			keys = newKeys;
			
			for (int i = 0; i < frontIdx; i++) {
				if (sensitiveAttrebute != null && outputAttribute != null) {
					if(!keys[i].equals(sensitiveAttrebute) && !keys[i].equals(outputAttribute.getAttributeName())) {
						Object o1 = m1.get(keys[i]);
						Object o2 = m2.get(keys[i]);
						if (compareObject(o1, o2) < 0 )
							return -1;
						else if (compareObject(o1, o2) > 0)
							return 1;
					}
				}
			}		
		}
		return 0;
	}
	
	public int compareObject(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null && o2 != null) {
			return -1;
		} else if (o1 != null && o2 == null) {
			return 1;
		} else { 
			if (o1 instanceof Boolean)
				if ((Boolean)o1 == true && (Boolean)o2 == false) {
					return 1;
				} else if ((Boolean)o1 == false && (Boolean)o2 == true) {
					return -1;
				} else return 0;
			else if (o1 instanceof Double)
				if ((Double)o1 > (Double)o2) {
					return 1;
				} else if ((Double)o1 < (Double)o2) {
					return -1;
				} else return 0;
			else if (o1 instanceof Float)
				if ((Float)o1 > (Float)o2) {
					return 1;
				} else if ((Float)o1 < (Float)o2) {
					return -1;
				} else return 0;
			else if (o1 instanceof Long)
				if ((Long)o1 > (Long)o2) {
					return 1;
				} else if ((Long)o1 < (Long)o2) {
					return -1;
				} else return 0;			
			else if (o1 instanceof Integer)
				if ((Integer)o1 > (Integer)o2) {
					return 1;
				} else if ((Integer)o1 < (Integer)o2) {
					return -1;
				} else return 0;
			else if (o1 instanceof Date) {
				Long time1 = ((Date)o1).getTime();
				Long time2 = ((Date)o1).getTime();
				if (time1 > time2) {
					return 1;
				} else if (time1 < time2) {
					return -1;
				} else return 0;
			}
			else if (o1 instanceof String)
				return ((String)o1).compareTo((String)o2);
			return 0;
		}
	}
		
	public boolean augmentLog(Augmentation[] augmentationCollection,boolean useMapDB, TaskForProgressBar task) 
	{
		types=new HashMap<String, Type>();
		literalValues=new HashMap<String, Set<String>>();

		try {
			if (mapDBDatabaseImpl!=null)
				mapDBDatabaseImpl.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!useMapDB)
		{
			instanceSet=new HashSet<Object[]>();
			mapDBDatabaseImpl=null;
		}
		else
		{
			try
			{
				mapDBDatabaseImpl = new MapDBDatabaseImpl();
				mapDBDatabaseImpl.createDB();
				DB db = mapDBDatabaseImpl.getDB();
				instanceSet=db.hashSet("instanceSet",new ObjectArraySerializer(augmentationCollection.length+1));
			}
			catch(IOException err)
			{
				err.printStackTrace();
				return false;
			}
		}
		
		long start=System.currentTimeMillis();
		//log=factory.createLog();
		this.augementationArray=augmentationCollection;
		timeIntervalAugmentations=new HashSet<String>();
		
		// -->
		Set<Augmentation> independentNonsensitiveAttributes = new HashSet<Augmentation>();
		int sensitiveIndex = -1;
		int dependentIndex = -1;
		int index = 0;
		for(Augmentation aug : augmentationCollection)
		{
			if (aug instanceof PreAttributeValue) {
				if( sensitiveAttrebute.equals(aug.getAttributeName())) {
					sensitiveIndex = index;
				}
			} else if (aug.equals(outputAttribute)) {
				dependentIndex = index;
			}
			index++;
		}
		
		for (int i=0; i<augmentationCollection.length; i++) {
			if (i != sensitiveIndex && i != dependentIndex) {
				independentNonsensitiveAttributes.add(augmentationCollection[i]);
			}
		}
		// <--
		
		Object[] newInstance=null;
		int numTraces=originalLog.size();
		int elaboratedTrace=0;
		orderOfAttributes = new HashMap<String, String>();
		int delayedTrace = 0;
		int delayedFemale = 0;
		int delayedMale = 0;
		int ontimeTrace = 0;
		int ontimeFemale = 0;
		int ontimeMale = 0;
		for(XTrace trace : originalLog)
		{
			for(Augmentation aug : augmentationCollection)
			{
				aug.reset(trace);   //   ????
			}
			for(XEvent event : trace)
			{
				
				//XEvent newEvent=factory.createEvent();
				//XConceptExtension.instance().assignName(newEvent, XConceptExtension.instance().extractName(event));
				newInstance = new Object[augementationArray.length+1];
				for(int i=0;i<augementationArray.length;i++)
				{
					// -->
					String augName = augementationArray[i].getAttributeName();
					
					if (augName.equals("trace_delay")) {
						long d = wholeTraceDuration(trace);
						if (d > traceDelayThreshold) {
							newInstance[i] = "delayed";
						} else {
							newInstance[i] = "on_time";
						}
					//	System.out.println("Predictor 704 C++++  "+augementationArray[i].getAttributeName()+newInstance[i].toString());
						if (types.get(augName)==null && newInstance[i]!=null) {
							types.put(augName, generateDataElement(newInstance[i]));
							orderOfAttributes.put(augName, augName);
						}
						if (newInstance[i] instanceof String)
						{
							Set<String> valueSet=literalValues.get(augName);
							if (valueSet==null)
							{
								valueSet=new HashSet<String>();
								literalValues.put(augName,valueSet);
							}
							valueSet.add((String) newInstance[i]);
								
		    		}
		    	} else if (augName.equals("trace_duration")) {
						newInstance[i] = wholeTraceDuration(trace);
					//	System.out.println("Predictor 704 C++++  "+augementationArray[i].getAttributeName()+newInstance[i].toString());
						if (types.get(augName)==null && newInstance[i]!=null) {
							types.put(augName, generateDataElement(newInstance[i]));
							orderOfAttributes.put(augName, augName);
						}
						if (newInstance[i] instanceof String)
						{
							Set<String> valueSet=literalValues.get(augName);
							if (valueSet==null)
							{
								valueSet=new HashSet<String>();
								literalValues.put(augName,valueSet);
							}
							valueSet.add((String) newInstance[i]);
								
		    		}
		    	} else if (augName.equals("Choice_Attribute")) {
						newInstance[i] = new String("not set");  // "not set" is used instead of ChoiceInTrace(trace)
			//			System.out.println("Predictor 704 C++++  "+augementationArray[i].getAttributeName()+newInstance[i].toString());
						if (types.get(augName)==null && newInstance[i]!=null) {
							types.put(augName, generateDataElement(newInstance[i]));
							orderOfAttributes.put(augName, augName);
						}
						if (newInstance[i] instanceof String)
						{
							Set<String> valueSet=literalValues.get(augName);
							if (valueSet==null)
							{
								valueSet=new HashSet<String>();
								literalValues.put(augName,valueSet);
							}
							valueSet.add((String) newInstance[i]);
								
		    		}
		    	} else if (augName.equals("Sub_Model_Attribute")) {
		    		newInstance[i] = 0;
		    //		System.out.println("SM++++  "+augementationArray[i].getAttributeName()+newInstance[i].toString());
		    		if (types.get(augName)==null && newInstance[i]!=null) {
						types.put(augName, generateDataElement(newInstance[i]));
						orderOfAttributes.put(augName, augName);
					}
		    		if (newInstance[i] instanceof String)
		    		{
		    			Set<String> valueSet=literalValues.get(augName);
		    			if (valueSet==null)
		    			{
		    				valueSet=new HashSet<String>();
		    				literalValues.put(augName,valueSet);
		    			}
		    			valueSet.add((String) newInstance[i]);
		    				
		    		}
					}else {
						
						try
						{
							newInstance[i]=augementationArray[i].returnAttribute(event, trace, sensitiveAttrebute, protectedValues, traceDelayThreshold);
				//			System.out.println("Predictor 739 ****  "+augementationArray[i].getAttributeName()+newInstance[i].toString());
							if (types.get(augementationArray[i].getAttributeName())==null && newInstance[i]!=null) {
								types.put(augementationArray[i].getAttributeName(), generateDataElement(newInstance[i]));
								orderOfAttributes.put(augName, augName);
							}
							if (newInstance[i] instanceof String)
							{
								Set<String> valueSet=literalValues.get(augementationArray[i].getAttributeName());
								if (valueSet==null)
								{
									valueSet=new HashSet<String>();
									literalValues.put(augementationArray[i].getAttributeName(),valueSet);
								}
								valueSet.add((String) newInstance[i]);
									
							}
						}
						catch(Exception err)
						{
							err.printStackTrace();
						}
					}
					//<--	
				}
				String transition=XLifecycleExtension.instance().extractTransition(event);
				if (transition==null || transition.equalsIgnoreCase("complete"))
				{
					newInstance[newInstance.length-1]=XConceptExtension.instance().extractName(event);
					instanceSet.add(newInstance);
				}
			}
			instanceOfATrace.put(trace, newInstance);
			if (task!=null)
				task.myProgress((++elaboratedTrace*100)/numTraces);
		}

		hasLogBeenAugmented=true;
		
		// ----------------->
		
		return true;
	}
	
	
	
	// it creates a map of the form <String, Set<Strings>>
	// the key is the name of literal attributes in the log
	// the value is the set of all possible values for the key attribute in the log
	private static Map<String, Set<String>> getLiteralValuesMap(XLog log) {
		
		Map<String, Set<String>> retValue=new HashMap<String, Set<String>>();
		
		for(XTrace trace : log) {
			
			
			for(XEvent event : trace) {
				
				for(XAttribute attributeEntry : event.getAttributes().values()) {
					
					if (attributeEntry instanceof XAttributeLiteral) {
						
						String value = ((XAttributeLiteral)attributeEntry).getValue();
						String varName=attributeEntry.getKey();
						Set<String> literalValues = retValue.get(varName);

						if (literalValues == null) {
							literalValues = new HashSet<String>();
							retValue.put(varName, literalValues);
						}
						
						literalValues.add(value);
					}
				}
			}
		}
		return retValue;
	}
	
	// -->
	// it creates a map of the form <String, Set<Strings>>
	// the key is the name of literal attributes in the log
	// the value is the set of all possible values for the key attribute in the log
	private static Map<String, Set<String>> getTraceLiteralValuesMap(XLog log) {
		
		Map<String, Set<String>> retValue=new HashMap<String, Set<String>>();
		
		for(XTrace trace : log) {
				
				for(XAttribute attributeEntry : trace.getAttributes().values()) {
					
					if (attributeEntry instanceof XAttributeLiteral) {
						
						String value = ((XAttributeLiteral)attributeEntry).getValue();
						String varName=attributeEntry.getKey();
						Set<String> literalValues = retValue.get(varName);

						if (literalValues == null) {
							literalValues = new HashSet<String>();
							retValue.put(varName, literalValues);
						}
						
						literalValues.add(value);
					}
				}
		}
		return retValue;
	}
	//<--

	public Map<String, Type> getTypes() {
		return Collections.unmodifiableMap(types);
	}
	
	// change the "activities" if it is changed
	public void setActivitiesToConsider(Collection<String> activitiesToConsider)
	{
		if (this.activitiesToConsider==null || this.activitiesToConsider.size()!=activitiesToConsider.size())
			isSetactivitiesToConsiderChanged=true;
		else
		{
			this.activitiesToConsider.removeAll(activitiesToConsider);
			if (this.activitiesToConsider.size()>0)
				isSetactivitiesToConsiderChanged=true;
			else
				isSetactivitiesToConsiderChanged=false;
		}
		this.activitiesToConsider=new HashSet<String>(activitiesToConsider);
	}
	
	public void setRegression(boolean regressionTree)
	{
		if (this.regressionTree!=regressionTree)
		{
			hasAlgorithmChanged=true;
			this.regressionTree=regressionTree;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void makePrediction(TaskForProgressBar task) throws Exception
	{
		boolean toDiscover=false;

			// -->
		
			
    	if (sensitiveAttrebute != null) {
   // 		for (int i = 0; i <= 100 ; i++) {  // this part is for different epsilon values
				
	//    
	//	   for (int i = 0; i <= 19 ; i++) {  // these 12 lines are for doing the experiment for different amount of discrimination in the data and the same setting at once (** start here **)
	//	   	int index = 0;
	//	   	for (int j = 0; j < augementationArray.length; j++) {
	//	   		if (augementationArray[j].getAttributeName().length() > 6 && augementationArray[j].getAttributeName().substring(0,6).equals("gender")) {
	//	   			index = j;
	//	   		}
	//	   	}
	//	   	System.out.println("gender"+ (i*5));
	//	   	Augmentation aug;
	//	   	aug = new AttributeValue("gender"+(i*5)); 
	//	   	augementationArray[index] = aug;
	//	   	sensitiveAttrebute = "gender"+(i*5);  //  (** end here **)
	//	    	
	//		
				
	        	augmentLogNDC(augementationArray, false, null);
	        	
	        	String classAttributeName = new String();
	  //      	System.out.println("entered hereeeee");
	        	if (targetActivityName != null) {
	        		classAttributeName = targetActivityName+"_"+(outputAttribute.getAttributeName());
	        	} else {
	        		classAttributeName = outputAttribute.getAttributeName();
	        		if (classAttributeName.length()>6 && classAttributeName.substring(0, 7).equals("Choice_")) {
	        			classAttributeName = classAttributeName.substring(7);
	        		}
	        	}
            
	        	tc = new TreeConstructor(classAttributeName, typesNDC, literalValuesNDC, orderedInstancesNDC, instancesOfNDC);
				
				tc.setBinarySplit(binarySplit);
				if (confidenceThreshold!=-1)
					tc.setConfidenceFactor(confidenceThreshold);
				tc.setMinNumObj((int) (numInstancesAddedForLearning*minNumInstancePerLeaf));
				if (numFoldErrorPruning!=-1)
					tc.setNumFolds(numFoldErrorPruning);
				tc.setDesairableOutcomes(desirableOutcomes);
				tc.setSensitivaAttributeName(sensitiveAttrebute);
				tc.setTargetThreshold(targetThreshold);
				tc.setSensitiveThreshold(sensitiveThreshold);
				tc.setProtectedValues(protectedValues);
				tc.setUnpruned(unPruned);
				tc.setCrossValidate(true);
				tc.setEpsilon(epsilon);
				tc.setNDCtoDC(NDCtoDC);
	//			tc.setEpsilon(i);
				tc.buildClassifire();
	//			task.myProgress(i*5);
				
	//			task.myProgress(i);
	//		}
		} else {
			discoveryParamChanged=false;
		}
		task.myProgress(100);
		
	}
		
	// return the different possible values of the output attribute
	private Object[] determineValues(String outputAttribute) {
		System.out.println( "   //"+outputAttribute );
		for(String s : typesNDC.keySet()) {
			System.out.println( "   //   "+s+ "     //     "+ typesNDC.get(s));
		}
		switch(typesNDC.get(outputAttribute))
		{

			case BOOLEAN :
				return new Boolean[] {false,true};
			case CONTINUOS :
			case DISCRETE :
			case TIMESTAMP :
			System.out.println( "   //");
				return intervals;
			case LITERAL :
				System.out.println( "   //" );
				return literalValues.get(outputAttribute).toArray();
		}
		return null;
	}

	private Object determineInterval(Number outputValue, DiscretizationInterval[] outputValuesAsObjects, Type type) {

		for(DiscretizationInterval interval : outputValuesAsObjects)
		{
			boolean aux;
			if (interval.isSecExtremeIncluded())
				aux=(outputValue.doubleValue() <= interval.getSecond());
			else
				aux=(outputValue.doubleValue() < interval.getSecond());
			if (outputValue.doubleValue() >= interval.getFirst() && aux)
				return interval;
		}
		return null;
	}

	public DiscretizationInterval[] setOutputAttribute(Augmentation attribute, int numberIntervals, DiscrMethod method, boolean regressionTree)
	{
		//assert(attribute!=null);
		if (attribute!=outputAttribute || numberIntervals!=numIntervals || discrMethod!=method)
			isOutputAttributeChanged=true;
		this.outputAttribute=attribute;
		this.numIntervals=numberIntervals;
		this.discrMethod=method;
		if (attribute==null)
		{
			intervals=null;
			return null;
		}
		if (regressionTree)
			intervals=null;
		else
			if (attribute.getAttributeName().length() >= 7 && !attribute.getAttributeName().substring(0, 7).equals("Choice_")) {
				switch(types.get(attribute.getAttributeName()))
				{
				case CONTINUOS :
				case TIMESTAMP :				
				{
					if (method==DiscrMethod.EQUAL_WIDTH)
					{
						Pair<Double, Double> pair=determineSmallestGreatest(attribute.getAttributeName());
						double range=pair.getSecond()-pair.getFirst();
						double intervalSize=range/numberIntervals;
						intervals=new DiscretizationInterval[numberIntervals];
						intervals[0]=new DiscretizationInterval(Double.NEGATIVE_INFINITY,pair.getFirst()+intervalSize);
						for(int i=1;i<numberIntervals-1;i++)
							intervals[i]=new DiscretizationInterval(pair.getFirst()+i*intervalSize,pair.getFirst()+(i+1)*intervalSize);
						intervals[numberIntervals-1]=new DiscretizationInterval(pair.getFirst()+(numberIntervals-1)*intervalSize,Double.POSITIVE_INFINITY);
						return intervals;	
					}
					else
					{
						ArrayList<DiscretizationInterval> intervalList=new ArrayList<DiscretizationInterval>(numberIntervals);
						Pair<TreeMap<Double,Integer>,Integer> frequencyMap=determineFrequency(attribute);
						double frequencyPerInterval=frequencyMap.getSecond()/numberIntervals;
						int intervalFrequency=0;
						double from=frequencyMap.getFirst().firstKey();
						double to=from;
						for(Entry<Double, Integer> entry : frequencyMap.getFirst().entrySet())
						{
							if (intervalFrequency < frequencyPerInterval)
							{
								to=entry.getKey();
								intervalFrequency+=entry.getValue();
							}
							else
							{
								intervalList.add(new DiscretizationInterval(from, to));
								from=to;
								intervalFrequency=0;
							}
						}
						intervalList.add(new DiscretizationInterval(from,to));
						intervals=intervalList.toArray(new DiscretizationInterval[0]);
					}
					return(intervals);
				}
				case DISCRETE :
				{
					Pair<Double, Double> pair=determineSmallestGreatest(attribute.getAttributeName());
					double range=pair.getSecond()-pair.getFirst();
					double intervalSize=range/numberIntervals;
					if (intervalSize>=1)
					{
						if (method==DiscrMethod.EQUAL_WIDTH)
						{
							intervals=new DiscretizationInterval[numberIntervals];
							double from=Math.floor(pair.getFirst());
							double to=Math.round(pair.getFirst()+intervalSize);
							for(int i=0;i<numberIntervals-1;i++)
							{
								intervals[i]=new DiscretizationInterval(from,to,true);
								from=to+1;
								to=Math.floor(from+intervalSize);
							}
							intervals[numberIntervals-1]=new DiscretizationInterval(from,pair.getSecond(),true);
						}
						else
						{
							ArrayList<DiscretizationInterval> intervalList=new ArrayList<DiscretizationInterval>(numberIntervals);
							Pair<TreeMap<Double,Integer>,Integer> frequencyMap=determineFrequency(attribute);
							double frequencyPerInterval=frequencyMap.getSecond()/numberIntervals;
							int intervalFrequency=0;
							double from=frequencyMap.getFirst().firstEntry().getKey();
							double to=0;
							for(Entry<Double, Integer> entry : frequencyMap.getFirst().entrySet())
							{
								if (intervalFrequency < frequencyPerInterval)
								{
									to=entry.getKey();
									intervalFrequency+=entry.getValue();
								}
								else
								{
									intervalList.add(new DiscretizationInterval(from, to,true));
									from=to+1;
									intervalFrequency=0;
								}
							}
							if (intervalFrequency>0)
								intervalList.add(new DiscretizationInterval(from, to,true));							
							intervals=intervalList.toArray(new DiscretizationInterval[0]);
						}
					}
					else
					{

						if (pair.getSecond()!=Double.NEGATIVE_INFINITY)
						{
							intervals=new DiscretizationInterval[(int) (pair.getSecond()-pair.getFirst())+1];

							for(int i=0;i<intervals.length;i++)
								intervals[i]=new DiscretizationInterval(pair.getFirst()+i,pair.getFirst()+i,true);
						}
						else
						{
							intervals=new DiscretizationInterval[1];
							intervals[0]=new DiscretizationInterval(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
						}
						
					}
					return intervals;
				}
				case LITERAL:
					intervals=null;
				default :
					break;	
				}
			}
			
		return null;

	}

	//determine the number of occurrence of each possible value of outputAttribute in the instanceSet and its total number of occurrence
	private Pair<TreeMap<Double, Integer>, Integer> determineFrequency(Augmentation outputAttribute) {
		int index=0;
		for(;!augementationArray[index].equals(outputAttribute);index++); // find the index of outputAttribute in the augmentationArray
		
		int totalOccurrence=0;
		TreeMap<Double,Integer> retValue=new TreeMap<Double, Integer>();
		Object value;
		

		for(Object[] instance : instanceSet)
		{
			value=instance[index];
			if (value!=null && value instanceof Number)
			{
				Integer numOccurrences=retValue.get(((Number)value).doubleValue());
				if (numOccurrences==null) numOccurrences=0;
				retValue.put(((Number) value).doubleValue(), numOccurrences+1);
				totalOccurrence++;
			}


		}
		return new Pair<TreeMap<Double,Integer>, Integer>(retValue, totalOccurrence);
	}

	private Pair<Double,Double> determineSmallestGreatest(String outputAttribute) {
		if (outputAttribute.endsWith("'"))
			outputAttribute=outputAttribute.substring(0, outputAttribute.length()-1);
		double smallest=Double.POSITIVE_INFINITY;
		double greatest=Double.NEGATIVE_INFINITY;
		int index=0;
		while(!augementationArray[index].getAttributeName().equals(outputAttribute))
			index++;
		Object value;


		for(Object[] instance : instanceSet)
		{
			value=instance[index];
			{
				if (value!=null && value instanceof Number)
				{
					smallest=Math.min(((Number) value).doubleValue(), smallest);
					greatest=Math.max(((Number) value).doubleValue(), greatest);
				}
				if (value!=null && value instanceof Date)
				{
					smallest=Math.min(((Date)value).getTime(), smallest);
					greatest=Math.max(((Date)value).getTime(), greatest);
				}
			}
		}
		return new Pair<Double, Double>(smallest, greatest);
	}

	private Object getAttributeValues(XAttribute xAttrib) 
	{
		if (xAttrib instanceof XAttributeBoolean)
			return((XAttributeBoolean)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeContinuous)
			return((XAttributeContinuous)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeDiscrete)
			return((XAttributeDiscrete)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeTimestamp)
			return((XAttributeTimestamp)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeLiteral)
			return((XAttributeLiteral)xAttrib).getValue();

		return null;
	}

	public void setBinarySplit(boolean binarySplit) {
		if (this.binarySplit != binarySplit)
		{
			this.binarySplit=binarySplit;
			discoveryParamChanged=true;
		}
	}

	public void setConfidenceThreshold(float confidenceThreshold) {
		if (this.confidenceThreshold != confidenceThreshold)
		{
			this.confidenceThreshold = confidenceThreshold;
			discoveryParamChanged=true;
		}	
	}

	public void setMinNumInstancePerLeaf(double d) {
		if (this.minNumInstancePerLeaf != d)
		{
			this.minNumInstancePerLeaf = d;
			discoveryParamChanged=true;
		}

	}
	
	public ArrayList<String> getOriginalLogAttributes() {
		return originalLogAttributes;
	}

	public void setSaveData(boolean saveData) {
		if (this.saveData != saveData)
		{
			this.saveData=saveData;
			discoveryParamChanged=true;
		}
	}

	public void setUnPruned(boolean unPruned) {
		if(this.unPruned != unPruned)
		{
			this.unPruned = unPruned;
			discoveryParamChanged=true;
		}
	}


	public int getNumInstances()
	{
		return df.getNumInstances();
	}

	public void setNumFolds(int numFoldErrorPruning) {
		if(this.numFoldErrorPruning != numFoldErrorPruning)
		{
			this.numFoldErrorPruning = numFoldErrorPruning;
			discoveryParamChanged=true;
		}
		
	}

	public EvaluationNDC getEvaluation() { 
		return tc.getEvaluation();
	}

	public Set<String> getLiteralValues(String attribute) {
		return Collections.unmodifiableSet(literalValues.get(attribute));
	}

	public Collection<String> getActivities() {
		return Collections.unmodifiableCollection(activityCollection);
	}

	public XLog getOriginalLog() {
		return originalLog;
	}

	public ResultReplay getResReplay() {
		return resReplay;
	}
	
	private static Type generateDataElement(Object value) {

		if (value instanceof Boolean) {
			return Type.BOOLEAN;
		} else if (value instanceof Long || value instanceof Integer) {
			return Type.DISCRETE;
		} else if (value instanceof Double || value instanceof Float) {
			return Type.CONTINUOS;
		} else if (value instanceof Date) {
			return Type.TIMESTAMP;
		} else if (value instanceof String) {
			return Type.LITERAL;
		}
		
		return null;	
	}

	public boolean isRegressionTree() {
		return regressionTree;
	}
	
	// return a Map of the form <attribute name, attribute type> for all the attributes in the log. (well, except those mentioned below) duplication is possible
	public static Map<String, Type> extractAttributeInformation(XLog log) {
		HashMap<String, Type> retValue = new HashMap<String, Type>();
		for (XTrace trace : log) {
			
			for (XEvent event : trace)
			{
				for(XAttribute attr : event.getAttributes().values())
				{
					if (!attr.getKey().startsWith("concept:") && !attr.getKey().startsWith("time:") && !attr.getKey().startsWith("resource:"))
					{
						Type classType = generateDataElement(attr);
						if (classType != null)
							retValue.put(attr.getKey(), classType);
					}
				}
			}
			
		}
		/*
		 * return: Mapping of Attribute name to the Attribute Data Type in a HashMap<String, Type>
		 */
		return retValue;
	}
	
	/**
	 * 
	 * returns the event attributes.
	 * @return
	 */
	public List<String> getAttributes() {
		return(originalLogAttributes);
	}

	public int getInstanceSetSize() {
		return instanceSet.size();
	}
	
	public Map<String, Set<String>> getTraceAttributeValuesMap() {
		return this.traceAttributesValues;
	}
	
	public Collection<String> getTraceAttributeNames() {
		for (String s : traceAttributeNames) {
			if (s.length() >= 7 && s.subSequence(0, 7).equals("Choice_")) {
				traceAttributeNames.remove(s);
			}
		}
		return traceAttributeNames;
	}
	
	//-->
   	
   	// input : all the places of a petrinet
   	// output : the set of place labels that are corresponding to 'OR' operation
   	public Set<String> getORPlaces (Collection<Place> places) {
   		Set<String> retValue = new HashSet<String>();
   		for(Place place : places) {
   			String s = new String();
   			String s1 = new String();
   			s = place.getLabel();
   			s1 = place.getLabel();
   			//System.out.println("predictor 1430 "+s);
   			if (s.charAt(0) == '(') {
   				//System.out.println("predictor 1432 %%%%% "+s);
   				s  = s.substring(2, s.length() - 2);
   				//System.out.println("([]) "+s);
   				String[] sides = s.split("]");
   				if (sides[1].substring(2, sides[1].length()).split(",").length > 1) {
   					//System.out.println("$$Choice## --> "+s1);
   					retValue.add(s1);
   				}
   			}
   		}
   		return retValue;
   	}
   	
  	
  	public long subModelDurationInTrace (XTrace trace) {
  		
  		Set<String> ST = new HashSet<String>();
  		for (String s : selectedTransitions) {
  			if (s.substring(s.length()-9,s.length()).equals("+complete")) {
  				s = s.substring(0,s.length()-9);
  				ST.add(s);
  			} else {
  				ST.add(s);
  			}
  		}
  		for (String s :ST) {
  		//	System.out.println("Predictor 1499 ST  "+s);
  		}
  		
  		
  		long totalTime = 0;
  		for (int i = 0; i < trace.size()-1; i++)	{
  			XEvent e1 = trace.get(i);
  			XEvent e2 = trace.get(i+1);
  			String e1name = XConceptExtension.instance().extractName(e1);
  			String e2name = XConceptExtension.instance().extractName(e2);
  			
  			if (ST.contains(e1name) && ST.contains(e2name))
  			{
  				//System.out.println("e1name  "+e1name+"   e2name  "+e2name+"  "+ST.contains(e1name)+" ^^" +ST.contains(e2name));
  				Date timestampE1=XTimeExtension.instance().extractTimestamp(e1);
  				Date timestampE2=XTimeExtension.instance().extractTimestamp(e2);
  				totalTime = totalTime + (timestampE2.getTime()-timestampE1.getTime());
  			}
  		}
		return totalTime;
  	}
  	
  	public long wholeTraceDuration(XTrace trace) {
  		XEvent firstEvent = trace.get(0);
		XEvent lastEvent = trace.get(trace.size()-1);
		Date timestampE1=XTimeExtension.instance().extractTimestamp(firstEvent);
		Date timestampE2=XTimeExtension.instance().extractTimestamp(lastEvent);
		return timestampE2.getTime()-timestampE1.getTime();
  	}
  	
  	public String[] getAllwholePathOptions () {
  		return allwholePathOptions;
  	}
   //<-- 
   	
   	public Map<String, Set<String>> getTraceAttributesValues() {
   		return traceAttributesValues;
   	}
   	
   	/**
   	 * 
   	 * it applies the trace augmentations on the given trace
   	 * @param trace
   	 * @param traceAugs
   	 * @param newInstanceNDC
   	 */
   	public void doTraceAugmentations(XTrace trace, LinkedList<Augmentation> traceAugs, Map<String, Object> newInstanceNDC) {
		for(Augmentation traceAug : traceAugs) {
			String traceAugName = traceAug.getAttributeName();
			if (traceAugName.equals("trace_delay")) {
				long d = wholeTraceDuration(trace);
				if (d > traceDelayThreshold) {
					newInstanceNDC.put(traceAugName, "delayed");
				} else {
					newInstanceNDC.put(traceAugName, "on_time");
				}
				doUpdate(traceAugName, newInstanceNDC);
			} else if (traceAugName.equals("trace_duration")) {
				newInstanceNDC.put(traceAugName, wholeTraceDuration(trace));
				doUpdate(traceAugName, newInstanceNDC);
			//} else if (traceAugName.equals("Sub_Model_Attribute")) {
    		//newInstanceNDC.put(traceAugName, subModelDurationInTrace(trace));
    			//doUpdate(traceAugName, newInstanceNDC);
			} else if (traceAugName.equals("Sub_Model_Attribute")) {
				XAttributeMap amap = trace.get(0).getAttributes();
				if (amap.containsKey("sub_model_duration")) {
					Object duration = getAttributeValues(amap.get("sub_model_duration"));
					newInstanceNDC.put("Sub_Model_Attribute", duration);
					doUpdate("Sub_Model_Attribute", newInstanceNDC);
				}
			} else{
				
				try
				{
					XEvent firstEvent = trace.get(0);
					newInstanceNDC.put(traceAugName, traceAug.returnAttribute(firstEvent, trace, sensitiveAttrebute, protectedValues, traceDelayThreshold));
					doUpdate(traceAugName, newInstanceNDC);
				}
				catch(Exception err)
				{
					err.printStackTrace();
				}
			}	
		} // end for aug \in traceAugs
   	}
   	
   	private long getLongValue(String string) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
   	 * This function updates the literalValueNDc and typesNDC according to the new instance.
   	 * @param augName
   	 * @param newInstanceNDC
   	 */
   	public void doUpdate(String augName, Map<String, Object> newInstanceNDC) {
   		augName = replaceNotAllowedStrings(augName);
   		if (typesNDC.get(augName)==null ) {
   			if ( newInstanceNDC.get(augName)!=null) {
   				typesNDC.put(augName, generateDataElement(newInstanceNDC.get(augName)));
   			}
		}
		if (newInstanceNDC.get(augName) instanceof String)
		{
			Set<String> valueSet=literalValuesNDC.get(augName);
			if (valueSet==null)
			{
				valueSet=new HashSet<String>();
				literalValuesNDC.put(augName,valueSet);
			}
			valueSet.add((String) newInstanceNDC.get(augName));
				
	   }
   	}
   	
   	// removes the not allowed char for the consistency
   	public String replaceNotAllowedStrings(String str) {
   		char[] array=str.toCharArray();
		for(int i=0;i<array.length;i++)
		{
			if (notAllowedChars.indexOf(array[i])!=-1)
				array[i]='_';
		}
		return (new String(array));
   	}
   	
   	// this function used to remove choice attributes and trace_duration and sub_model_duration from
   	// originalLogAttributes.
   	public void cleanOriginalLogAttributes() {
   		if (originalLogAttributes.size() > 0) {
   			Set<String> names = new HashSet<String>();
   			for (String attName : originalLogAttributes) {
   				names.add(attName);
   			}
   			for (String attName : names) {
   				if (attName.length() >= 7 && attName.substring(0, 7).equals("Choice_")) {
   					originalLogAttributes.remove(attName);
   				} else if (attName.equals("Sub_Model_Attribute") || attName.equals("trace_duration") 
   						|| attName.equals("trace_delay") || attName.equals("activityduration")) {
   					originalLogAttributes.remove(attName);
   				}
   			}
   		}
   	}
}
