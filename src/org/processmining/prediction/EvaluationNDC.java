package org.processmining.prediction;

import java.util.Formatter;

import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Evaluation;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Instance;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.Instances;
import org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree.J48WithNDCs;


public class EvaluationNDC {
	private J48WithNDCs fairTree;
	private J48WithNDCs normalTree;
	private Instances dataset;
	private int classIndex;
	private int sensitiveAttributeIndex;
	private double[][] evaluateDiscrimination;
	private double[] evaluateAccuracy;
	private Instances trainDataset;
	private Instances testDataset;
	private Formatter x;
	
	public EvaluationNDC(J48WithNDCs fairTree, J48WithNDCs normalTree, Instances dataset, int sensitiveAttributeIndex) throws Exception {
		this.fairTree = fairTree;
		this.normalTree = normalTree;
		this.testDataset = dataset;
		this.classIndex = dataset.classIndex();
		this.dataset = dataset;
		this.classIndex = dataset.classIndex();
		this.sensitiveAttributeIndex = sensitiveAttributeIndex;
		createTestAndTrainSatasets(60);
		doEvaluate();
	}
	
	public void createTestAndTrainSatasets(int percent) {
		int num = dataset.numInstances();		
		trainDataset = new Instances(dataset, 100);		
		testDataset = new Instances(dataset, 100);		
		double w = 0;		
		int i = 0;		
		double sum = 0;
		
		 while (i < num) {		
		    	w = dataset.instance(i).weight();		
		    	double sum2 = sum;		
		    	double wtrain = 0;		
		    	double wtest = 0;		
		    	while (sum2 < (sum + w)) {		
		    		if ( sum2 % 10 < 6 && sum % 10 >= 0) {		
		    			wtrain++;		
		    		} else {		
		    			wtest++;		
		    		}		
		    		sum2++;		
		    	}		
		    	sum = sum + w;		
		    	if (wtrain > 0) {		
		    		Instance inst = dataset.instance(i);		
		    		inst.setWeight(wtrain);		
		    		trainDataset.add(inst);		
		    	}		
		    	if (wtest > 0) {		
		    		Instance inst = dataset.instance(i);		
		    		inst.setWeight(wtest);		
		    		testDataset.add(inst);		
		    	}		
		    	i++;		
		    }		
		    System.out.println("weight  : "+ sum);
	}
	
	public void doEvaluate() throws Exception {
		evaluateDiscrimination = new double[3][3];
		
		//discrimination in whole, train and test data
		evaluateDiscrimination[0][0] = discriminationInData(dataset);
		evaluateDiscrimination[0][1] = discriminationInData(trainDataset);
		evaluateDiscrimination[0][2] = discriminationInData(testDataset);
		
		double[][] resultDataset = evaluate(fairTree, normalTree, dataset);
		System.out.println("\n Whole data");
		System.out.println(" accurecy fair tree : " + resultDataset[1][0]+ " accurecy normal tree : " + resultDataset[1][1]);
		double[][] resulTrainDataset = evaluate(fairTree, normalTree, trainDataset);
		System.out.println("\n Train data");
		System.out.println(" accurecy fair tree : " + resulTrainDataset[1][0]+ " accurecy normal tree : " + resulTrainDataset[1][1]);
		double[][] resultTestDataset = evaluate(fairTree, normalTree, testDataset);
		System.out.println("\n test data");
		System.out.println(" accurecy fair tree : " +resultTestDataset[1][0]+ " accurecy normal tree : " + resultTestDataset[1][1]);
		
		//discrimination by fair tree in whole, train and test data
		evaluateDiscrimination[2][0] = resultDataset[0][0];
		evaluateDiscrimination[2][1] = resulTrainDataset[0][0];
		evaluateDiscrimination[2][2] = resultTestDataset[0][0];
		
		//discrimination by normal tree in whole, train and test data
		evaluateDiscrimination[1][0] = resultDataset[0][1];
		evaluateDiscrimination[1][1] = resulTrainDataset[0][1];
		evaluateDiscrimination[1][2] = resultTestDataset[0][1];
		
		// accuracy just in test data for fair and normal tree
		evaluateAccuracy = new double[2];
		evaluateAccuracy[0] = resultTestDataset[1][0];
		evaluateAccuracy[1] = resultTestDataset[1][1];
		
	//	writeToFile();
	}
	
	public double discriminationInData(Instances dataset) {
		double wid = 0; // sum of the weight of instances in dataset
		
		double wasid =  0; // sum of the weight of accepted Sensitive instances in dataset
		double wsid =  0; // sum of the weight of Sensitive instances in dataset
		
		double wafid =  0;  // sum of the weight of accepted Favorable instances in dataset
		double wfid =  0; // sum of the weight of Favorable instances in dataset

		int dataSize = dataset.numInstances();
		for (int i = 0; i < dataset.numInstances(); i++) {
			String sensitiveValue = dataset.instance(i).stringValue(sensitiveAttributeIndex);
			String classValue = dataset.instance(i).stringValue(classIndex);		
			if (sensitiveValue.equals("protected_value")) {
				wsid = wsid + dataset.instance(i).weight();
				if (classValue.equals("Desirable_Class")) {
					wasid = wasid + dataset.instance(i).weight();
				} 	
	//			else {
	//				nrsitd++;
	//			}
			} else {
				wfid = wfid + dataset.instance(i).weight();
				if (classValue.equals("Desirable_Class")) {
					wafid = wafid + dataset.instance(i).weight();
				}
		//		else {
		//			nrfitd++;
		//		}
			}
		}
		return ( wafid / wfid)-(wasid/wsid);
	}
	
	public double[][] evaluate (J48WithNDCs fairTree, J48WithNDCs normalTree, Instances dataset) throws Exception {
		
		double[][] result = new double[2][2];
		double nsiData =  0;  // sum of the weight of SensitiveInstancesIn dataset
		double niiData =  0;  // sum of the weight of instances in dataset
		double nasiFairTree =  0;  //  numAcceptedSensitiveInstancesFairTree
		double nafiFairTree =  0;  //  numAcceptedFavorabelInstancesInFairTree
		double nasiNormalTree =  0;  // numAcceptedSensitiveInstancesNormalTree
		double nafiNormalTree =  0;  // numAcceptedFavorabelInstancesInNormalTree
		double numCorrectPredictionFairTree = 0;
		double numCorrectPredictionNormalTree = 0;
		double correctnessFairTree = 0;
		double correctnessNormalTree = 0;
		
//		ev(dataset);
		
		Evaluation evaluation = new Evaluation(dataset);
		for (int i = 0; i < dataset.numInstances() ; i++) {
			Instance inst = dataset.instance(i);
		//	Instance inst2 = new Instance(inst);
			double resultFair = evaluation.evaluateModelOnce(fairTree, inst);
			double resultNormal = evaluation.evaluateModelOnce(normalTree, inst);
			niiData+=inst.weight();
			if (dataset.instance(i).stringValue(sensitiveAttributeIndex).equals("protected_value")) {
				nsiData+=inst.weight();
				if (resultFair == 0 ) {
					nasiFairTree+=inst.weight();
				}
				if (resultNormal == 0 ) {
					nasiNormalTree+=inst.weight();
				}
			} else {
				if (resultFair == 0 ) {
					nafiFairTree+=inst.weight();
				}
				if (resultNormal == 0 ) {
					nafiNormalTree+=inst.weight();
				}
			}
			
			String instanceRealClass = dataset.instance(i).stringValue(dataset.classAttribute());
			if ((resultFair == 0 && instanceRealClass.equals("Desirable_Class")) || 
					resultFair == 1 && (instanceRealClass.equals("non_Desirable_Class"))) {
				numCorrectPredictionFairTree += dataset.instance(i).weight();
			}
			if  ((resultNormal == 0 && instanceRealClass.equals("Desirable_Class")) || 
					(resultNormal == 1 && instanceRealClass.equals("non_Desirable_Class"))) {
				
				numCorrectPredictionNormalTree += dataset.instance(i).weight();
			}
		}
			
		double accuracyFairTree = numCorrectPredictionFairTree / niiData;
		double accuracyNormalTree = numCorrectPredictionNormalTree / niiData;
		result[1][0] = accuracyFairTree;
		result[1][1] = accuracyNormalTree;
	
		double discriminationByFairTree = ( nafiFairTree / (niiData- nsiData))-(nasiFairTree/nsiData);
		double discriminationByNormalTree = ( nafiNormalTree / (niiData- nsiData))-( nasiNormalTree/nsiData);
		result[0][0] = discriminationByFairTree;
		result[0][1] = discriminationByNormalTree;
		
		return result;
	}
/**	
	public void ev(Instances instances) throws Exception {
		double[][] result = new double[2][2];
		double nsiData =  0;  // numSensitiveInstancesIn dataset
		double nfiData = 0;
		double nasiFairTree =  0;  //  numAcceptedSensitiveInstancesFairTree
		double nafiFairTree =  0;  //  numAcceptedFavorabelInstancesInFairTree
		double nasiNormalTree =  0;  // numAcceptedSensitiveInstancesNormalTree
		double nafiNormalTree =  0;  // numAcceptedFavorabelInstancesInNormalTree
		double numCorrectPredictionFairTree = 0;
		double numCorrectPredictionNormalTree = 0;
		double correctnessFairTree = 0;
		double correctnessNormalTree = 0;
		
		for (int i = 0; i < dataset.numInstances() ; i++) {
			Instance inst = dataset.instance(i);
			int resultFair = (int) Math.round(fairTree.classifyInstance(inst));
			int resultNormal = (int) Math.round(normalTree.classifyInstance(inst));
			if (dataset.instance(i).stringValue(sensitiveAttributeIndex).equals("protected_value")) {
				nsiData++;
				if (resultFair == 0 ) {
					nasiFairTree++;
				}
				if (resultNormal == 0 ) {
					nasiNormalTree++;
				}
			} else {
				nfiData++;
				if (resultFair == 0 ) {
					nafiFairTree++;
				}
				if (resultNormal == 0 ) {
					nafiNormalTree++;
				}
			}
		}
		
//		System.out.println("ev disc fair :"+ ((nafiFairTree/nfiData)-(nasiFairTree/nsiData)));
//		System.out.println("ev disc normal :"+ ((nafiNormalTree/nfiData)-(nasiNormalTree/nsiData)));
	}
	*/
	public double[][] getDiscrimination() {
		return evaluateDiscrimination;
	}
	
////public void writeToFile() {
////	String filePath = "/users/qafari/results.text";
////	String text = new String();
////	for (int j=0 ; j < 3; j++) {
////		text = text+String.format ("%.4f,", evaluateDiscrimination[0][j]);
////		text = text+String.format ("%.4f,", evaluateDiscrimination[1][j]);
////		text = text+String.format ("%.4f,", evaluateDiscrimination[2][j]);
////	}
////	for (int j=0 ; j < 3; j++) {
////		
////	}
////	for (int j=0 ; j < 3; j++) {
////		
////	}
////	text = text+String.format ("%.4f,%.4f", evaluateAccuracy[0],evaluateAccuracy[1]);
////
////	appendUsingFileWriter(filePath, "&");
////	appendUsingFileWriter(filePath, text);
////}
	
	public String toStringDiscrimination() {
	//	String filePath = "/users/qafari/results.text";
		String output = new String();
		output = "Discrimination : \n                 Whole dataset   train dataset   test dataset \n  Data     ";
		String text = new String();
		for (int j=0 ; j < 3; j++) {
			output = output+ "          "+ String.format ("%.4f", evaluateDiscrimination[0][j]);
			text = text+String.format ("%.4f,", evaluateDiscrimination[0][j]);
		}
		output = output+ " \n  normal tree     ";
		for (int j=0 ; j < 3; j++) {
			output = output+ String.format ("%.4f", evaluateDiscrimination[1][j])+ "          ";
			text = text+String.format ("%.4f,", evaluateDiscrimination[1][j]);
		}
		output = output+ " \n  fair tree      ";
		for (int j=0 ; j < 3; j++) {
			output = output+ String.format ("%.4f", evaluateDiscrimination[2][j])+ "          ";
			text = text+String.format ("%.4f,", evaluateDiscrimination[2][j]);
		}
		text = text+String.format ("%.4f,%4f", evaluateAccuracy[0],evaluateAccuracy[1]);

//		appendUsingFileWriter(filePath, "&");
//		appendUsingFileWriter(filePath, text);
		return output;
	}
	
	public double[] getAccuracy() {
		return evaluateAccuracy;
	}
	
	public String toStringAccuracy() {
		String output = new String();
		output = "\n\n Accuracy : \n  fair tree     normal tree \n";
		output = output + String.format ("   %.4f", evaluateAccuracy[0])+ "       "+String.format ("%.4f", evaluateAccuracy[1]);
		output = output+ " \n";
		return output;
	}
	
///private static void appendUsingFileWriter(String filePath, String text) {
///	File file = new File(filePath);
///	FileWriter fr = null;
///	try {
///		// Below constructor argument decides whether to append or override
///		fr = new FileWriter(file, true);
///		fr.write(text+"\n");
///		
///
///	} catch (IOException e) {
///		e.printStackTrace();
///	} finally {
///		try {
///			fr.close();
///		} catch (IOException e) {
///			e.printStackTrace();
///		}
///	}
///}
}
