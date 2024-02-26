package org.processmining.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.models.FunctionEstimator.Type;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Attribute;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Discrimination;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.FastVector;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Instance;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Instances;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.J48WithNDCs;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.SelectedTag;

import com.google.common.collect.BiMap;

public class TreeConstructor {
	
	public final static String notAllowedChars=".()&!|=<>-+*/% ";
	private boolean treatNoLeafAsFalse = false;
	private String sensitivaAttributeName; // <--
	private int sensitiveAttributeIndex; // <--
	private String[] AttributeTypes; // <--
	private Instances trainDataSet; 			
	private Instances testDataSet; 
	private Instances dataset;
	// target
	private String classAttName; // <--
	private int classIndex;
	protected List<String> classValues = new ArrayList<String>(); // ArrayList of unique identifiers for target transitions
	protected BiMap<Object, Integer> classIndexMap; // Mapping from target classes their class index in weka
	protected BiMap<String, Object> classMapping; // Bidirectional mapping from unique identifiers of target transitions
	private Set<String> desirableOutCome;
	private int targetThreshold;
	
	//sensitive attribute
	private Set<String> protectedValues = new HashSet<String>(); // <--	protected static final String nullValue = "NOT SET";
	private int sensitiveThreshold;
	
	private Map<String, Type> attributeType;
	private Map<String, Set<String>> literalValues;
	private ArrayList<Map<String, Object>> instanceSet;
	private boolean unpruned = false;
	private float confidenceFactor;
	private int minNumObj;
	private int numFolds;
	private int epsilon = 20;
	private boolean binarySplits;
	private boolean saveInstanceData;
	private J48WithNDCs fairTree;
	private J48WithNDCs normalTree;
	private Map<Map<String, Object>, Integer> instancesWeight;
	
	private double discriminationInTrainingData;
	private double discriminationByFairTree;
	private double discriminationByNormalTree;
	private double accuracyFairTree;
	private double accuracyNormalTree;
	private float confidenceThreshold = 0.25f;
	private int numFoldErrorPruning = 3;
	private boolean crossValidate = true;
	private Map<String, Object> minValues;
	private Map<String, Object> maxValues;
	private EvaluationNDC evaluation;
	private int NDCtoDC = 0;

	
	private double protectedValueDesirableClass = 0;
	private double protectedValueNonDesirableClass = 0;
	private double favorableValueDesirableClass = 0;
	private double favorableValueNonDesirableClass = 0;
	
	public TreeConstructor(String classAttName, Map<String, Type> attributeType, Map<String, Set<String>> literalValues, ArrayList<Map<String, Object>>  instanceSet, Map<Map<String, Object>, Integer> instancesWeight) throws Exception {
		this.attributeType = attributeType;
		this.literalValues = literalValues;
		this.instanceSet = instanceSet;
		this.classAttName = replaceNotAllowedStrings(getRealAttNameIfChoice(classAttName));
		this.instancesWeight = instancesWeight;
		setMinAndMax();
	}
	
	// if the attribute is started with Choice_ then it is a choice att and its name in all data structures is the choice place label
	// in this case we ned to find the dame of the choice place
	public String getRealAttNameIfChoice(String attName) {
		if (attName.length() >= 7 ) {
			if(attName.substring(0, 7).equals("Choice_")) {
				String[] s = attName.split("_to_", 2);
				String choicePlaceName = s[0].substring(7,s[0].length());
				return choicePlaceName;
			}
		}
		return attName;
	}
	
	public int getNDCtoDC() {
		  return NDCtoDC;
	  }
	  
	  public void setNDCtoDC(int i) {
		  if (i == 1 || i == 0 || i == -1) {
			  NDCtoDC = i;
		  }
		  else 
			  NDCtoDC = 0;
	  }
	
	public void setTargetThreshold(int threshold) {
		this.targetThreshold = threshold;
	}
	
	public void setSensitiveThreshold(int threshold) {
		sensitiveThreshold = threshold;
	}
	
	public void setEpsilon(int epsilon) {
		this.epsilon = epsilon;
	}
	
	public void setDesairableOutcomes(Set<String> desirablseOutcomes) {
		this.desirableOutCome = new HashSet<String>();
		this.desirableOutCome.addAll(desirablseOutcomes);
	}
	
	public void setSensitivaAttributeName(String sensitiveAttName) {
		this.sensitivaAttributeName = replaceNotAllowedStrings(getRealAttNameIfChoice(sensitiveAttName));
	}
	
	public void setProtectedValues(Set<String> protectedValues) {
		this.protectedValues = new HashSet<String>();
		this.protectedValues.addAll(protectedValues);
	}

	public void buildClassifire() throws Exception {
		
		CreateDataSet();
		int num = dataset.numInstances();
		this.trainDataSet = new Instances(dataset, 100);
		for (int i = 0 ; i< num*0.6 ; i++) {
			trainDataSet.add(dataset.instance(i));
		}
		this.testDataSet = new Instances(dataset, 100);
		for (int i = (int) (num*0.6) ; i< num ; i++) {
			testDataSet.add(dataset.instance(i));
		}
		System.out.println("dataSet : "+ dataset.numInstances());
		System.out.println("trainDataSet : "+ trainDataSet.numInstances());
		System.out.println("testDataSet : "+ testDataSet.numInstances());
		System.out.println("*********************");

		fairTree = buildFairClassifire();
		System.out.println("fair Tree");
		fairTree.getepsilon();
//		System.out.println(fairTree.toString());
		System.out.println("*********************");
		
		normalTree = buildNormalClassifire();
		System.out.println("normal Tree");
	//	System.out.println(normalTree.toString());
		
		evaluateTrees();
	}
	
	public void evaluateTrees() throws Exception {
		if (fairTree == null || normalTree == null) {
			return;
		}
		System.out.println(fairTree.prefix());
		System.out.println("****************************");
//		System.out.println(fairTree.toSummaryString());
		evaluation = new EvaluationNDC(fairTree, normalTree, dataset, sensitiveAttributeIndex);
	}
		/**
		Evaluation evaluation = new Evaluation(testDataSet);
		for (int i = 0; i < testDataSet.numInstances() ; i++) {
			Instance inst = testDataSet.instance(i);
			double resultFair = evaluation.evaluateModelOnce(fairTree, inst);
			double resultNormal = evaluation.evaluateModelOnce(normalTree, inst);
			if (testDataSet.instance(i).stringValue(sensitiveAttributeIndex).equals("protected_value")) {
				nsiTestdata++;
				if (resultFair == 0 ) {
					nasiFairTree++;
		//			i1++;
	//				System.out.println("fair s a : " + i1 );
				}
				if (resultNormal == 0 ) {
					nasiNormalTree++;
		//			i2++;
			//		System.out.println("normal s a : " + i2 );
				}
			} else {
				if (resultFair == 0 ) {
					nafiFairTree++;
		//			i3++;
	//				System.out.println("fair f a : " + i3 );
				}
				if (resultNormal == 0 ) {
					nafiNormalTree++;
		//			i4++;
		//			System.out.println("normal f a : " + i4 );
				}
			}
			
			String instanceRealClass = testDataSet.instance(i).stringValue(testDataSet.classAttribute());
			if ((resultFair == 0 && instanceRealClass.equals("Desirable_Class")) || 
					resultFair == 1 && (instanceRealClass.equals("non_Desirable_Class"))) {
				numCorrectPredictionFairTree++;
			}
			if  ((resultNormal == 0 && instanceRealClass.equals("Desirable_Class")) || 
					(resultNormal == 1 && instanceRealClass.equals("non_Desirable_Class"))) {
				
				numCorrectPredictionNormalTree++;
			}
			
			accuracyFairTree = numCorrectPredictionFairTree / testDataSet.numInstances();
			accuracyNormalTree = numCorrectPredictionNormalTree / testDataSet.numInstances();
		}
		
		System.out.println("*********************");
		System.out.println("testDataSet size : " + testDataSet.numInstances() );
		System.out.println("nsiTestdata : " + nsiTestdata );
		System.out.println("nasiFairTree : " + nasiFairTree );
		System.out.println("nafiFairTree : " + nafiFairTree );
		System.out.println("///////////////////");
		System.out.println("nsiTestdata : " + nsiTestdata );
		System.out.println("nasiNormalTree : " + nasiNormalTree );
		System.out.println("nafiNormalTree : " + nafiNormalTree );
		System.out.println("Accuracy fair tree : " + accuracyFairTree);
		System.out.println("Accuracy normal tree : " + accuracyNormalTree );
		
		discriminationByFairTree = ( nafiFairTree / (testDataSet.numInstances()- nsiTestdata))-(nasiFairTree/nsiTestdata);
		discriminationByNormalTree = ( nafiNormalTree / (testDataSet.numInstances()- nsiTestdata))-( nasiNormalTree/nsiTestdata);
	}
	*/
	public DotPanel visualizeFairTree() throws Exception{
		if (fairTree == null) {
			return null;
		}
		fairTree.colorGraph(normalTree);
        Dot newDot = new Dot();
        String dotRepresentation = fairTree.graph();
        System.out.println("VISUALIZEFAIRTREE@@@@ "+dotRepresentation);
        newDot.setStringValue(dotRepresentation);
        DotPanel dotPanel = new DotPanel(newDot);
        return dotPanel;
		//JPanel panel =new JPanel(new BorderLayout());
	}
	
	public JPanel getBothTreeVisualization() throws Exception {
		JPanel returnedPanel = new JPanel();
		RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS);
		rl.setFill(true);
		returnedPanel.setLayout(rl);
		
		JComponent fairTreeVisualization = visualizeFairTree();
		JComponent normalTreeVisualization = visualizeNormalTree();
		if (normalTreeVisualization != null && fairTreeVisualization != null) {
			returnedPanel.add(normalTreeVisualization, new Float(50));
			returnedPanel.add(fairTreeVisualization, new Float(50));
			return returnedPanel;
		}
		return returnedPanel;
	}
	
	public DotPanel visualizeNormalTree() throws Exception {
		if (fairTree == null) {
			return null;
		}
        Dot newDot = new Dot();
        String dotRepresentation = normalTree.graph();
        System.out.println("VISUALIZENORMALTREE@@@@ "+dotRepresentation);

        newDot.setStringValue(dotRepresentation);
        DotPanel dotPanel = new DotPanel(newDot);
        return dotPanel;
		//JPanel panel =new JPanel(new BorderLayout());
	}
	
	public void CreateDataSet() {
		FastVector AttributesInfo = new FastVector(attributeType.size());
		AttributesInfo = createAttributeFastVector();
		
		Instances DataSet = new Instances("DataSet For fair Tree", AttributesInfo, 100);
		DataSet.setClassIndex(classIndex);

		for(Map<String,Object> inst : instanceSet) {
			Instance instance = createInstance(inst, DataSet);
			if (instance != null) {
				DataSet.add(instance);
			}
		}
		this.dataset = DataSet;
//		System.out.println("instance size = "+ DataSet.numInstances());
//		System.out.println("\nprotected Value Desirable Class = "+ protectedValueDesirableClass);
//		System.out.println("protected Value non Desirable Class = "+ protectedValueNonDesirableClass);
//		System.out.println("favorable Value Desirable Class = "+ favorableValueDesirableClass);
//		System.out.println("favorable Value non Desirable Class = "+ favorableValueNonDesirableClass); 
//		System.out.println("dataset size : "+ DataSet.numInstances());
				
		double a =protectedValueDesirableClass;
		double b = protectedValueNonDesirableClass;
		double c =  favorableValueDesirableClass;
		double d = favorableValueNonDesirableClass; 
		System.out.println("\nDiscrimination in data = "+ ((c/(c+d))-(a/(a+b))) ); 
	}

	protected FastVector createAttributeFastVector() {
		FastVector attributeListForHeader = new FastVector(attributeType.keySet().size());
		AttributeTypes = new String[attributeType.keySet().size()];
		
		int index = 0;
		/*
		 * For each entry <String, Type> in map, depending on the Type of the
		 * attribute, add a new Attribute (attribute name, format for values of
		 * said attribute) to the fastvector of the attributes for the header of the data set
		 * Also it sets the attributeType array to the type
		 */

		Set<String> attNames = attributeType.keySet();

		//first set the sensitive att as the first attribute in the vector
		sensitiveAttributeIndex = 0;
		for (String attName : attNames) {
			if (attName.equalsIgnoreCase(sensitivaAttributeName)) {
				FastVector values = new FastVector(2);
				values.addElement("protected_value");
				values.addElement("favorable_value");
				attributeListForHeader.addElement(new Attribute(attName, values));
			}
		}

		String[] array = new String[attNames.size()];
		int j = 0;
		for (String attName : attNames) {
			array[j] = attName;
			j++;
		}
		try
		{
			Arrays.sort(array, new Comparator<String>() {

				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}

			});
		}catch(NullPointerException err)
		{
			err.printStackTrace();
		}
		int idx = 1;
		for (int i = 0; i < array.length; i++) {
			String attName = array[i];
			if (!attName.equalsIgnoreCase(sensitivaAttributeName)) {
				if (attName.equalsIgnoreCase(classAttName)) {
					classIndex = idx;
					FastVector values = new FastVector(2);
					values.addElement("Desirable_Class");
					values.addElement("non_Desirable_Class");
					attributeListForHeader.addElement(new Attribute(attName, values));
				} else {
					switch (attributeType.get(attName)) {
						case LITERAL :
							FastVector values = new FastVector(literalValues.get(attName).size());
							for(String s : literalValues.get(attName)) {
								values.addElement(s);
							}
							attributeListForHeader.addElement(new Attribute(attName, values));
							AttributeTypes[index] = "NOMINAL";
							break;
						case TIMESTAMP :
							//TODO FM, why do we need this timeformat here?
							attributeListForHeader.addElement(new Attribute(attName, "yyyy-MM-dd'T'HH:mm:ss"));
							AttributeTypes[index] = "DATE";
							break;
						case DISCRETE : // Do the same as case CONTINOUS
						case CONTINUOS :
							attributeListForHeader.addElement(new Attribute(attName));
							AttributeTypes[index] = "NUMERIC";
							break;
						case BOOLEAN :
							FastVector boolValues = new FastVector(2);
							boolValues.addElement("True");
							boolValues.addElement("False");
							attributeListForHeader.addElement(new Attribute(attName,boolValues));
							AttributeTypes[index] = "NOMINAL";
					}
				}	
				idx++;
			}
		}
	return attributeListForHeader;
	}
	
	public Instance createInstance(Map<String,Object> inst, Instances DataSet) {
		Instance instance = new Instance(DataSet.numAttributes());
		instance.setDataset(DataSet);
    	if (instancesWeight.containsKey(inst)) {
    		instance.setWeight(instancesWeight.get(inst));
    		if (instancesWeight.get(inst)>1) {
    			instance.setWeight(instancesWeight.get(inst));
    			double d = instancesWeight.get(inst);
    			System.out.println(d);
    		}
    	}
    	
	//	System.out.println("num of att in dataset > "+ DataSet.numAttributes());
//    	String sValue = null;
 //   	String cValue = null;
		for (int index = 0 ; index < DataSet.numAttributes() ; index++ ) {
			Attribute att = DataSet.attribute(index);
			String attName = att.name();
			if (index == sensitiveAttributeIndex) {
				if (setClassorSensitiveValue( attName, inst.get(attName), true) == null) {
					return null;
				} else {
//					 sValue = setClassorSensitiveValue( attName, inst.get(attName), true);
					 instance.setValue(att, setClassorSensitiveValue( attName, inst.get(attName), true));
				}
			} else if (index == classIndex) {  //  start
				if (setClassorSensitiveValue( attName, inst.get(attName), false) == null) {
					return null;
				} else {
//					 cValue = setClassorSensitiveValue( attName, inst.get(attName), false);
					 instance.setValue(att, setClassorSensitiveValue( attName, inst.get(attName), false));
				}
				// end
			}else {
				switch (attributeType.get(attName)) {
					case LITERAL :
						if( inst.get(attName) == null || inst.get(attName).equals("NOT SET")) {
							  instance.setMissing(att);
							  break;
						  } else {
							  instance.setValue(att, inst.get(attName).toString());
							  break;
						  }

					case TIMESTAMP :
						if( inst.get(attName) == null || inst.get(attName).equals("NOT SET")) {
							  instance.setMissing(att);
							  break;
						  } else {
							  long time = ((Date) inst.get(attName)).getTime();
								instance.setValue(att, time);
								break;
						  }
					case DISCRETE : // Do the same as case CONTINOUS
					case CONTINUOS :
						 if( inst.get(attName) == null || inst.get(attName).equals("NOT SET")) {
							  instance.setMissing(att);
							  break;
						  } else {
							  if (inst.get(attName) instanceof Integer ) {
								  instance.setValue(att, ((Integer) inst.get(attName)).doubleValue());
								  break;
							  } else if (inst.get(attName) instanceof Long ) {
								  instance.setValue(att, ((Long) inst.get(attName)).doubleValue());
								  break;
							  }
							 
						  }
						
					case BOOLEAN :
						 if( inst.get(attName) == null || inst.get(attName).equals("NOT SET")) {
							  instance.setMissing(att);
							  break;
						  } else {
							  if (inst.get(attName).equals(true)) {
								  instance.setValue(att, "True");
							  } else {
								  instance.setValue(att, "False");
							  }
						  }
				 }
			}
		}
		
//		if (sValue.equals("protected_value") && cValue.equals("non_Desirable_Class"))
//			return null;
		
		if (instance.toString(sensitiveAttributeIndex).equals("protected_value")) {
			if (instance.toString(classIndex).equals("Desirable_Class")) {
				protectedValueDesirableClass++;
			} else {
				protectedValueNonDesirableClass++;
			}
		} else {
			if (instance.toString(classIndex).equals("Desirable_Class")) {
				favorableValueDesirableClass++;
			} else {
				favorableValueNonDesirableClass++;
			}
		}
		
		return instance;
	}
	
	public J48WithNDCs buildFairClassifire() {
		J48WithNDCs tree = new J48WithNDCs();
		System.out.println("\n\n ###################################### ");
		System.out.println(" epsilon --> "+ epsilon/100f);
		tree.setepsilon(epsilon/100f);
		tree.setMinNumObj(2);
		tree.setUnpruned(unpruned);
		if (confidenceThreshold!=-1) {
			tree.setConfidenceFactor(confidenceThreshold);
		}
		if (numFoldErrorPruning!=-1) {
			tree.setNumFolds(numFoldErrorPruning);
		}
		tree.setrelabel(true);
		tree.setBinarySplits(true);
		tree.setsplitCriterion(new SelectedTag(2, J48WithNDCs.TAGS_SPLITING));
		tree.setNDCtoDC(NDCtoDC);
		
		Discrimination.setSaDep("protected_value");
		Discrimination.setSaFav("favorable_value");
		Discrimination.setSaIndex(sensitiveAttributeIndex);
		Discrimination.sa = sensitivaAttributeName;
		Discrimination.setDC(0);

		try {
			tree.buildClassifier(trainDataSet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tree;
	}
	
	public J48WithNDCs buildNormalClassifire() {
		J48WithNDCs normalTree = new J48WithNDCs();
		normalTree.setepsilon(1);
		normalTree.setMinNumObj(2);
		normalTree.setUnpruned(unpruned);
		if (confidenceThreshold!=-1) {
			normalTree.setConfidenceFactor(confidenceThreshold);
		}
		if (numFoldErrorPruning!=-1) {
			normalTree.setNumFolds(numFoldErrorPruning);
		}
		normalTree.setrelabel(false);
		normalTree.setBinarySplits(true);
		normalTree.setsplitCriterion(new SelectedTag(1, J48WithNDCs.TAGS_SPLITING));
		
		Discrimination.setSaDep("protected_value");
		Discrimination.setSaFav("favorable_value");
		Discrimination.setSaIndex(sensitiveAttributeIndex);
		Discrimination.sa = sensitivaAttributeName;
		Discrimination.setDC(0);
		
		try {
			normalTree.buildClassifier(trainDataSet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return normalTree;
	}
	
	public void setMinAndMax() { 
		Set<String> nominalOrContinuesAtts = new HashSet<String>();
		for (String attName : attributeType.keySet()) {
			if (attributeType.get(attName) != Type.LITERAL && attributeType.get(attName) != Type.BOOLEAN) {
				nominalOrContinuesAtts.add(attName);
			}
		}
		minValues = new HashMap<String, Object>();
		maxValues = new HashMap<String, Object>();
		Map<String, Boolean> hasValidValue = new HashMap<String, Boolean>();
		for (String attName : attributeType.keySet()) {
			hasValidValue.put(attName, false); // meaning that this attribute has no valid value (just null or not set)
//			System.out.println("att type : " + attName);
		}
		
		for (Map<String,Object> instance : instanceSet) {
//			System.out.println("************** : ");
			for (String attName : instance.keySet()) {
	//			System.out.println("instance : " + attName);
				if (attributeType.get(attName) != null) {
					switch (attributeType.get(attName)) {
						case TIMESTAMP :
							if( instance.get(attName) == null || instance.get(attName).equals("NOT SET")) {
								  break;
							  } else {
								  long time = ((Date) instance.get(attName)).getTime();
								  if (hasValidValue.get(attName) == false ) {
									  hasValidValue.remove(attName);
									  hasValidValue.put(attName, true);
									  minValues.put(attName, time);
									  maxValues.put(attName, time);
								  } else {
									  if (time < (long) minValues.get(attName)) {
										  minValues.remove(attName);
										  minValues.put(attName, time);
									  } 
									  if (time > (long) maxValues.get(attName)) {
										  maxValues.remove(attName);
										  maxValues.put(attName, time);
									  } 
								  }
								  break;
							  }
						case DISCRETE : // Do the same as case CONTINOUS
						case CONTINUOS :
							if( instance.get(attName) == null || instance.get(attName).equals("NOT SET")) {
								  break;
							  } else {
								  Object obj = instance.get(attName);
								  if (instance.get(attName) instanceof Long) {
									  if (hasValidValue.get(attName) == false ) {
										  hasValidValue.remove(attName);
										  hasValidValue.put(attName, true);
										  minValues.put(attName, obj);
										  maxValues.put(attName, obj);
									  } else {
										  if (isBigger( minValues.get(attName),obj)) {
											  minValues.remove(attName);
											  minValues.put(attName, obj);
										  } 
										  if (isBigger(obj, maxValues.get(attName))) {
											  maxValues.remove(attName);
											  maxValues.put(attName, obj);
										  } 
									  }
								  }
								  break;
							  }
					}
				}
			}
		} 
	}
	
	public boolean isBigger(Object o1, Object o2) {
		if (o1 instanceof Integer && o2 instanceof Integer) {
			if ((Integer) o1 > (Integer) o2) {
				return true;
			}
		}
		if (o1 instanceof Date && o2 instanceof Date) {
			if (((Date) o1).getTime() > ((Date) o2).getTime()) {
				return true;
			}
		}
		if (o1 instanceof Long && o2 instanceof Long) {
			if ((Long) o1 > (Long) o2) {
				return true;
			}
		} 
		if (o1 instanceof Float && o2 instanceof Float) {
			if ((Float) o1 > (Float) o2) {
				return true;
			}
		}
		if (o1 instanceof Double && o2 instanceof Double) {
			if ((Double) o1 > (Double) o2) {
				return true;
			}
		}
		return false;
	}
	
	public void setNumFolds(int numFoldErrorPruning) {
		this.numFolds = numFoldErrorPruning;
	}
	public void setBinarySplits(boolean binarySplit) {
		this.binarySplits = binarySplit;
	}
	public void setSaveInstanceData(boolean saveData) {
		this.saveInstanceData = saveData;
	}
	
	public void setBinarySplit (boolean binarySplit) {
		this.binarySplits = binarySplit;
	}
	
	public void setConfidenceFactor(float confidenceThreshold) {
		this.confidenceThreshold = confidenceThreshold;
	}
	
	public void setNumFoldErrorPruning (int numFoldErrorPruning) {
		this.numFoldErrorPruning = numFoldErrorPruning;
	}
	
	public void setMinNumObj (int minNumObj) {
		this.minNumObj = minNumObj;
	}
	
	public void setUnpruned (boolean unpruned) {
		this.unpruned = unpruned;
	}
	
	public void setCrossValidate (boolean crossValidate) {
		this.crossValidate = true;
	}
	
	public EvaluationNDC getEvaluation() {
		return evaluation;
	}
	
	public String setClassorSensitiveValue(String attName, Object object, boolean isSensitiveAtt) {
		switch (attributeType.get(attName)) {
			case BOOLEAN :
			case LITERAL :
				if( object == null || object.equals("NOT SET")) {
					  return null;
				  } else {
					  if (isSensitiveAtt) {
						  if (protectedValues.contains(object.toString())) {
							  return "protected_value";
						  } else {
							  return "favorable_value";
						  }
					  } else {
						  if (desirableOutCome.contains(object.toString())) {
							  return "Desirable_Class";
						  } else {
							  return "non_Desirable_Class";
						  }
					  }
				  }
			case TIMESTAMP :
			case DISCRETE : // Do the same as case CONTINOUS
			case CONTINUOS :
				 if( object == null || object.equals("NOT SET")) {
					  return null;
				  } else {
					  if (!minValues.containsKey(attName)) {
						  return null;
					  } else {
				//		  if(maxValues.keySet().contains(attName)) {
				//			  System.out.println("yes");
				//		  } else {
				//			  System.out.println("no");
				//		  }
						  long max = (long) maxValues.get(attName);
						  long min = (long) minValues.get(attName);
						  long res = max - min;
						  long threshold = min + (( max - min) * targetThreshold / 100);
						  if (isSensitiveAtt) {
							  Object time = new Object();
							  if (object instanceof Date) {
								  time = ((Date) object).getTime();
							  } else {
								  time = object;
							  }
							  if (protectedValues.contains("below")) {
								  if ( (long) time < threshold) {
									  return "protected_value";
								  } else {
									  return "favorable_value";
								  }
							  } else {
								  if ((long) time < threshold) {
									  return "favorable_value";
								  } else {
									  return "protected_value";
								  }
							  }
						  } else {
							  Object time = new Object();
							  if (object instanceof Date) {
								  time = ((Date) object).getTime();
							  } else {
								  time = object;
							  }
							  if (desirableOutCome.contains("below")) {
								  if ((long) time < threshold) {
									  return "Desirable_Class";
								  } else {
									  return "non_Desirable_Class";
								  }
							  } else {
								  if ((long) time < threshold) {
									  return "non_Desirable_Class";
								  } else {
									  return "Desirable_Class";
								  }
							  } 
						  }
						  
					  }
				  }
		}
		return null; 
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
}


