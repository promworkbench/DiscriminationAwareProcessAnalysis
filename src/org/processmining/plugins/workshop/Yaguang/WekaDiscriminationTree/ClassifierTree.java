/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 675 Mass
 * Ave, Cambridge, MA 02139, USA.
 */

/*
 * ClassifierTree.java Copyright (C) 1999 Eibe Frank
 *
 */

package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for handling a tree structure used for classification.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.17.2.1 $
 */
public class ClassifierTree implements Drawable, Serializable {

	/** The model selection method. */
	protected ModelSelection m_toSelectModel;

	/** Local model at node. */
	protected ClassifierSplitModel m_localModel;

	/** References to sons. */
	protected ClassifierTree[] m_sons;

	/** True if node is leaf. */
	protected boolean m_isLeaf;

	/** True if node is empty. */
	protected boolean m_isEmpty;
	
	protected boolean m_isDifferent;

	/** The training instances. */
	protected Instances m_train;

	/** The pruning instances. */
	protected Distribution m_test;

	/** The id for the node. */
	protected int m_id;
	
	/** indicate of the node is changed. */
	protected boolean m_isChanged = false;
	
	double epsilon=J48WithNDCs.m_epsilon;
	static boolean stop=false;
	private int NDCtoDC = 0;
	

	/**
	 * For getting a unique ID when outputting the tree (hashcode isn't guaranteed
	 * unique)
	 */
	private static long PRINTED_NODES = 0;

	/**
	 * Gets the next unique node ID.
	 *
	 * @return the next unique node ID.
	 */
	protected static long nextID() {

		return PRINTED_NODES++;
	}
	
  	public Map<Double, Double[]> doRemoveUselessNodes(Map<Double, Double[]> info, Double[][] discriminationInfo) {
  		Map<Double, Double[]> effectiveNodesInfo = new HashMap<Double, Double[]>();
  		Double discInCurrentClassifier = (discriminationInfo[0][0]/discriminationInfo[0][1])-(discriminationInfo[1][0]/discriminationInfo[1][1]);
  		
  		for (Double nodeID : info.keySet()) {
  			Double[] res = new Double[4];
  			Double newDisc;
  			if (info.get(nodeID)[0]==0) {
  				newDisc = ((discriminationInfo[0][0]-info.get(nodeID)[4])/discriminationInfo[0][1])-((discriminationInfo[1][0]-info.get(nodeID)[3])/discriminationInfo[1][1]);	
  			} else {
  				newDisc = ((discriminationInfo[0][0]+info.get(nodeID)[4])/discriminationInfo[0][1])-((discriminationInfo[1][0]+info.get(nodeID)[3])/discriminationInfo[1][1]);
  			}
  			
  			if (newDisc < discInCurrentClassifier) {
  				if ((NDCtoDC == 1 && info.get(nodeID)[0]==1) || (NDCtoDC == -1 && info.get(nodeID)[0]==0) || NDCtoDC == 0) {
  					res[0] = discInCurrentClassifier - newDisc;
  	  				res[1] = info.get(nodeID)[2] - info.get(nodeID)[1];
  	  				res[2] = info.get(nodeID)[1];
  	  				res[3] = info.get(nodeID)[2];
  	  				effectiveNodesInfo.put(nodeID, res);
  				}
  	//			System.out.println("node id :"+ nodeID+ " new disc : "+ res[0] + " new err : "+ res[1]);
  			}
  		}
  		
  		return effectiveNodesInfo;
  	}
  	
  	private Set<Integer> kNapsack(Map<Double, Double[]> effectiveNodesInfo, Double[][] discriminationInfo) {
  		Double discInCurrentClassifier = (discriminationInfo[0][0]/discriminationInfo[0][1])-(discriminationInfo[1][0]/discriminationInfo[1][1]);
  		int numItems = effectiveNodesInfo.size();
  		double[][] info = new double[numItems][3];
  		int idx = 0;
  		for (Double nodeID : effectiveNodesInfo.keySet()) {
  			info[idx][0] = nodeID;
  			info[idx][1] = effectiveNodesInfo.get(nodeID)[0];
  			info[idx][2] = effectiveNodesInfo.get(nodeID)[0]/Math.abs((effectiveNodesInfo.get(nodeID)[1])+1);
  			idx++;
  		}
  		
  		Set<Integer> result = new HashSet<Integer>();
  		
  		int numLeafChosen = 0;
    	while (discInCurrentClassifier > epsilon && numLeafChosen < info.length) {
    		int index = pickMaxIdx(info);
    		Double i = index/1d;
    		System.out.println(" new disc : "+ effectiveNodesInfo.get(info[index][0])[0] + " new err : "
    				+ effectiveNodesInfo.get(info[index][0])[1]+" correct : "+ effectiveNodesInfo.get(info[index][0])[2]+
    				" incorrect : "+ effectiveNodesInfo.get(info[index][0])[3]);
    		info[index][2] = -1;
    		numLeafChosen++;
    		result.add((int) info[index][0]);
    		discInCurrentClassifier = discInCurrentClassifier - info[index][1];
    	}
    	
    	double number = 0;
    	for (int i = 0; i < info.length; i++) {
    		if (info[i][2] == -1) {
    			System.out.println("number instances to be changed : " + effectiveNodesInfo.get(info[i][0])[1]);
    			number = number + effectiveNodesInfo.get(info[i][0])[1];
    		}
    	}
    	
    	System.out.println("Total number of instances changed : " + Math.abs(number));
        System.out.println("number of leaves changed : " + numLeafChosen);
        return result;
    }
  	
  	public int pickMaxIdx(double[][] info) {
  		double max = info[0][2];
  		int idx = 0;
  		for (int i = 0 ; i < info.length; i++) {
  			if (info[i][2] > max) {
  				max = info[i][2];
  				idx = i;
  			}
  		}
  		return idx;
  	}
  	
  	// Map<Double, Double[]>  key : node id ; value : Double[4] [0] : effect on discrimination
  	//															[1] : 
  //	private Map<Double, Double[]> inverseKNapsack(Map<Double, Double[]> effectiveNodesInfo) {
  	private Set<Integer> inverseKNapsack(Map<Double, Double[]> effectiveNodesInfo) {
  //  	int W = (int) Math.round(epsilon*1000);
    	int w; 
    	int numItems = effectiveNodesInfo.size(); 
        
        int [][] itemsInfo = new int[numItems][3];
        int j = 0;
        int total = 0 ;
        for (Double nodeID : effectiveNodesInfo.keySet()) {
        	if ((int) Math.round(nodeID) != -1) {
        		itemsInfo[j][0] = (int) Math.round(nodeID);
            	itemsInfo[j][1] = (int) Math.round(effectiveNodesInfo.get(nodeID)[0]*1000);
            	total = total + itemsInfo[j][1];
            	itemsInfo[j][2] = Math.abs((int) Math.round(effectiveNodesInfo.get(nodeID)[1]));
            	System.out.println("itemsInfo[j][0] "+ itemsInfo[j][0] + "   itemsInfo[j][1] "+ itemsInfo[j][1]+ "  itemsInfo[j][2] "+ itemsInfo[j][2]);
            	j++;
        	}
        }
        numItems = j;
        // Build table K[][] in bottom up manner 
        System.out.println("epsilon : "+ epsilon);
        int W = total - (int) Math.round(epsilon*1000);
        System.out.println("W  : "+ W);
        int K[][] = new int[numItems + 1][W + 1]; 
        int keep[][] = new int[numItems + 1][W + 1];
        
        for (int i = 0; i< numItems; i++) { 
            for (j = 0; j<= W; j++) { 
                if (i == 0 || j == 0) 
                    K[i][j] = 0; 
                else if (itemsInfo[i][1]<= W && itemsInfo[i][2] + K[i-1][W - itemsInfo[i][1]] > K[i - 1][j]) {
                    K[i][j] = itemsInfo[i][2] + K[i - 1][W - itemsInfo[i][1]]; 
                    keep[i][j] = 1;
                    System.out.println("i : "+ i);
                }
                else {
                	K[i][j] = K[i - 1][j]; 
                	keep[i][j] = 0;
                }
            } 
        } 
        
        Set<Integer> result = new HashSet<Integer>();
        int W2 = W;
        for (int i = numItems-1; i >= 0; i--) {
        	if (keep[i][W2] == 1) {
        		W2 = W2 - itemsInfo[i][1];
        		itemsInfo[i][0] = -1;
      //  		System.out.println("leaf : " +i + " is removed");
        	} else {
        		result.add(itemsInfo[i][0]);
        	}
        } 
        
        Set<Double> keySet = new HashSet<Double>();
        for(Double d : effectiveNodesInfo.keySet()) {
        	keySet.add(d);
        }
        for (Double nodeID : keySet) {
        	if (!result.contains((int) Math.round(nodeID))) {
        		effectiveNodesInfo.remove(nodeID);
        	}
        }
        
     //   return effectiveNodesInfo;
        return result;
    }
  	
  	public void doChangeLeafLabels(Set<Integer> nodeIdsToRelabel){
 // 		System.out.println("id : "+ m_id);
        double s=0,f=0,temp;
         
          int i,c=0;
          if (m_isLeaf){
             if(nodeIdsToRelabel.contains(m_id)){
            	 temp=localModel().distribution().m_perClass[0];
            	 localModel().distribution().m_perClass[0]=localModel().distribution().m_perClass[1];
            	 localModel().distribution().m_perClass[1]=temp;
            	 stop=true;
             }
          } else {
        	  for (i=0;i<m_sons.length;i++){
     //   		  System.out.println("sun ("+ i +")");
        		  son(i).doChangeLeafLabels(nodeIdsToRelabel);
        		  if(!m_isChanged) {
        			  if (son(i).m_isLeaf && nodeIdsToRelabel.contains(son(i).m_id)) {
        	//			  System.out.println(m_id);
        				  temp=localModel().distribution().m_perClassPerBag[i][0];
        				  localModel().distribution().m_perClassPerBag[i][0]=localModel().distribution().m_perClassPerBag[i][1];
        				  localModel().distribution().m_perClassPerBag[i][1]=temp;
        				  m_isChanged = true;
         	//			  System.out.println("00000000000000");
        			  }
        	//		  break;
        		  }
        	  }
          } // end of else        
  	}
  	
  	
	
	//-----------------------------change------------------------
	public void doRelabel(Map<Double, Double[]> info, Double[][] discriminationInfo, int NDCtoDC) {
		this.NDCtoDC = NDCtoDC;
  		Map<Double, Double[]> effectiveNodesInfo = doRemoveUselessNodes(info, discriminationInfo);
  	//	Map<Double, Double[]> nodeInfoToRelabel = inverseKNapsack(effectiveNodesInfo);
  	//	Set<Integer> nodeIdsToRelabel = inverseKNapsack(effectiveNodesInfo);
  	//	Set<Integer> nodeIdsToRelabel = kNapsack(nodeInfoToRelabel, discriminationInfo);
  		Set<Integer> nodeIdsToRelabel = kNapsack(effectiveNodesInfo, discriminationInfo);
  		doChangeLeafLabels(nodeIdsToRelabel);
  		// the set of nodes that by relabeling them the discrimination decreases
	}
	//--------------------  --------end---------------------------

	/**
	 * Resets the unique node ID counter (e.g. between repeated separate print
	 * types)
	 */
	protected static void resetID() {

		PRINTED_NODES = 0;
	}
	
//-----------------------------------change---------------------------------
	public final double[][] doDistributionForInstance(Instance instance, boolean useLaplace) throws Exception {

		double[][] doubles = new double[instance.numClasses()][2];

		for (int i = 0; i < doubles.length; i++) {
			double[] temp = doGetProbs(i, instance, 1);
			doubles[i][0] = temp[0];
			doubles[i][1] = temp[1];
		}

		return doubles;
	}
	
	private double[] doGetProbs(int classIndex, Instance instance, double weight) throws Exception {

		double probID[] = new double[2];

		if (m_isLeaf) {
			probID[0] = weight * localModel().classProb(classIndex, instance, -1);
			probID[1] = m_id;
			return probID;
		} else {
			int treeIndex = localModel().whichSubset(instance);
			if (treeIndex == -1) {
				double[] weights = localModel().weights(instance);
				for (int i = 0; i < m_sons.length; i++) {
					if (!son(i).m_isEmpty) {
						probID = son(i).doGetProbs(classIndex, instance, weights[i] * weight);
					}
				}
				return probID;
			} else {
				if (son(treeIndex).m_isEmpty) {
					probID[0] = weight * localModel().classProb(classIndex, instance, treeIndex);
					probID[1] = -1;
					return probID;
				} else {
					return son(treeIndex).doGetProbs(classIndex, instance, weight);
				}
			}
		}
	}
//--------------------------------------end-------------------------------

	/**
	 * Constructor.
	 */
	public ClassifierTree(ModelSelection toSelectLocModel) {
		m_toSelectModel = toSelectLocModel;
	}

	/**
	 * Method for building a classifier tree.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	public void buildClassifier(Instances data) throws Exception {

		if (data.checkForStringAttributes()) {
			throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
		}
		data = new Instances(data);
		data.deleteWithMissingClass();
		buildTree(data, false);
	}

	/**
	 * Builds the tree structure.
	 *
	 * @param data
	 *            the data for which the tree structure is to be generated.
	 * @param keepData
	 *            is training data to be kept?
	 * @exception Exception
	 *                if something goes wrong
	 */
	public void buildTree(Instances data, boolean keepData) throws Exception {

		Instances[] localInstances;

		if (keepData) {
			m_train = data;
		}
		m_test = null;
		m_isLeaf = false;
		m_isEmpty = false;
		m_isDifferent = false;
		m_sons = null;
		m_localModel = m_toSelectModel.selectModel(data);
		if (m_localModel.numSubsets() > 1) {
			localInstances = m_localModel.split(data);
			data = null;
			m_sons = new ClassifierTree[m_localModel.numSubsets()];
			for (int i = 0; i < m_sons.length; i++) {
				m_sons[i] = getNewTree(localInstances[i]);
				localInstances[i] = null;
			}
		} else {
			m_isLeaf = true;
			if (Utils.eq(data.sumOfWeights(), 0))
				m_isEmpty = true;
			data = null;
		}
		assignIDs(-1);
	}

	/**
	 * Builds the tree structure with hold out set
	 *
	 * @param train
	 *            the data for which the tree structure is to be generated.
	 * @param test
	 *            the test data for potential pruning
	 * @param keepData
	 *            is training Data to be kept?
	 * @exception Exception
	 *                if something goes wrong
	 */
	public void buildTree(Instances train, Instances test, boolean keepData) throws Exception {

		Instances[] localTrain, localTest;
		int i;

		if (keepData) {
			m_train = train;
		}
		m_isLeaf = false;
		m_isEmpty = false;
		m_isDifferent = false;
		m_sons = null;
		m_localModel = m_toSelectModel.selectModel(train, test);
		m_test = new Distribution(test, m_localModel);
		if (m_localModel.numSubsets() > 1) {
			localTrain = m_localModel.split(train);
			localTest = m_localModel.split(test);
			train = test = null;
			m_sons = new ClassifierTree[m_localModel.numSubsets()];
			for (i = 0; i < m_sons.length; i++) {
				m_sons[i] = getNewTree(localTrain[i], localTest[i]);
				localTrain[i] = null;
				localTest[i] = null;
			}
		} else {
			m_isLeaf = true;
			if (Utils.eq(train.sumOfWeights(), 0))
				m_isEmpty = true;
			train = test = null;
		}
		assignIDs(-1);
	}

	/**
	 * Classifies an instance.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	public double classifyInstance(Instance instance) throws Exception {

		double maxProb = -1;
		double currentProb;
		int maxIndex = 0;
		int j;

		for (j = 0; j < instance.numClasses(); j++) {
			currentProb = getProbs(j, instance, 1);
			if (Utils.gr(currentProb, maxProb)) {
				maxIndex = j;
				maxProb = currentProb;
			}
		}

		return (double) maxIndex;
	}

	/**
	 * Cleanup in order to save memory.
	 */
	public final void cleanup(Instances justHeaderInfo) {

		m_train = justHeaderInfo;
		m_test = null;
		if (!m_isLeaf)
			for (int i = 0; i < m_sons.length; i++)
				m_sons[i].cleanup(justHeaderInfo);
	}

	/**
	 * Returns class probabilities for a weighted instance.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	public final double[] distributionForInstance(Instance instance, boolean useLaplace) throws Exception {

		double[] doubles = new double[instance.numClasses()];

		for (int i = 0; i < doubles.length; i++) {
			if (!useLaplace) {
				doubles[i] = getProbs(i, instance, 1);
			} else {
				doubles[i] = getProbsLaplace(i, instance, 1);
			}
		}

		return doubles;
	}

	/**
	 * Assigns a uniqe id to every node in the tree.
	 */
	public int assignIDs(int lastID) {

		int currLastID = lastID + 1;

		m_id = currLastID;
		if (m_sons != null) {
			for (int i = 0; i < m_sons.length; i++) {
				currLastID = m_sons[i].assignIDs(currLastID);
			}
		}
		return currLastID;
	}
	
	public boolean isLeaf() {
		return m_isLeaf;
	}
	/**
	 * Returns the type of graph this classifier represents.
	 * 
	 * @return Drawable.TREE
	 */
	public int graphType() {
		return Drawable.TREE;
	}
	
	/**
	 * Returns graph describing the tree.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	public String graph() throws Exception {

		StringBuffer text = new StringBuffer();

		assignIDs(-1);
		text.append("digraph J48Tree {\n");
		if (m_isLeaf) {
			if (m_isDifferent) {
				text.append(
					"N" + m_id + " [label=\"" + m_localModel.dumpLabel(0, m_train) + "\" " + "shape=box style=filled fillcolor=yellow");
			}
			else {
				text.append("N" + m_id + " [label=\"" + m_localModel.dumpLabel(0, m_train) + "\" " + "shape=box style=filled");
			}
			if (m_train != null && m_train.numInstances() > 0) {
				text.append("data =\n" + m_train + "\n");
				text.append(",\n");

			}
			text.append("]\n");
		} else {
			try {
				text.append("N" + m_id + " [label=\"" + m_localModel.leftSide(m_train) + "\" ");
			}
			catch (Exception ex) {
				// we add it with empty learned
				text.append("N" + m_id + " [label=\"\" ");

			}
			if (m_train != null && m_train.numInstances() > 0) {
				text.append("data =\n" + m_train + "\n");
				text.append(",\n");
			}
			text.append("]\n");
			graphTree(text);
		}

		return text.toString() + "}\n";
	}

	/**
	 * Returns tree in prefix order.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	public String prefix() throws Exception {

		StringBuffer text;

		text = new StringBuffer();
		if (m_isLeaf) {
			text.append("[" + m_localModel.dumpLabel(0, m_train) + "]");
		} else {
			prefixTree(text);
		}

		return text.toString();
	}
	
	// -->
	public String getLabel() throws Exception {
		return  m_localModel.dumpLabel(0, m_train);
	}
	
	public void setIsDifferent (boolean isDifferent) {
		this.m_isDifferent = isDifferent;
	}
	/**
	 * Returns source code for the tree as an if-then statement. The class is
	 * assigned to variable "p", and assumes the tested instance is named "i". The
	 * results are returned as two stringbuffers: a section of code for assignment
	 * of the class, and a section of code containing support code (eg: other
	 * support methods).
	 *
	 * @param className
	 *            the classname that this static classifier has
	 * @return an array containing two stringbuffers, the first string containing
	 *         assignment code, and the second containing source for support code.
	 * @exception Exception
	 *                if something goes wrong
	 */
	public StringBuffer[] toSource(String className) throws Exception {

		StringBuffer[] result = new StringBuffer[2];
		if (m_isLeaf) {
			result[0] = new StringBuffer("    p = " + m_localModel.distribution().maxClass(0) + ";\n");
			result[1] = new StringBuffer("");
		} else {
			StringBuffer text = new StringBuffer();
			String nextIndent = "      ";
			StringBuffer atEnd = new StringBuffer();

			long printID = ClassifierTree.nextID();

			text.append("  static double N").append(Integer.toHexString(m_localModel.hashCode()) + printID)
					.append("(Object []i) {\n").append("    double p = Double.NaN;\n");

			text.append("    if (").append(m_localModel.sourceExpression(-1, m_train)).append(") {\n");
			text.append("      p = ").append(m_localModel.distribution().maxClass(0)).append(";\n");
			text.append("    } ");
			for (int i = 0; i < m_sons.length; i++) {
				text.append("else if (" + m_localModel.sourceExpression(i, m_train) + ") {\n");
				if (m_sons[i].m_isLeaf) {
					text.append("      p = " + m_localModel.distribution().maxClass(i) + ";\n");
				} else {
					StringBuffer[] sub = m_sons[i].toSource(className);
					text.append(sub[0]);
					atEnd.append(sub[1]);
				}
				text.append("    } ");
				if (i == m_sons.length - 1) {
					text.append('\n');
				}
			}

			text.append("    return p;\n  }\n");

			result[0] = new StringBuffer("    p = " + className + ".N");
			result[0].append(Integer.toHexString(m_localModel.hashCode()) + printID).append("(i);\n");
			result[1] = text.append(atEnd);
		}
		return result;
	}

	/**
	 * Returns number of leaves in tree structure.
	 */
	public int numLeaves() {

		int num = 0;
		int i;

		if (m_isLeaf)
			return 1;
		else
			for (i = 0; i < m_sons.length; i++)
				num = num + m_sons[i].numLeaves();

		return num;
	}

	/**
	 * Returns number of nodes in tree structure.
	 */
	public int numNodes() {

		int no = 1;
		int i;

		if (!m_isLeaf)
			for (i = 0; i < m_sons.length; i++)
				no = no + m_sons[i].numNodes();

		return no;
	}

	/**
	 * Prints tree structure.
	 */
	public String toString() {

		try {
			StringBuffer text = new StringBuffer();

			if (m_isLeaf) {
				text.append(": ");
				text.append(m_localModel.dumpLabel(0, m_train));
			} else
				dumpTree(0, text);
			text.append("\n\nNumber of Leaves  : \t" + numLeaves() + "\n");
			text.append("\nSize of the tree : \t" + numNodes() + "\n");

			return text.toString();
		} catch (Exception e) {
			return "Can't print classification tree.";
		}
	}

	/**
	 * Returns a newly created tree.
	 *
	 * @param data
	 *            the training data
	 * @exception Exception
	 *                if something goes wrong
	 */
	protected ClassifierTree getNewTree(Instances data) throws Exception {

		ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
		newTree.buildTree(data, false);

		return newTree;
	}

	/**
	 * Returns a newly created tree.
	 *
	 * @param data
	 *            the training data
	 * @param test
	 *            the pruning data.
	 * @exception Exception
	 *                if something goes wrong
	 */
	protected ClassifierTree getNewTree(Instances train, Instances test) throws Exception {

		ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
		newTree.buildTree(train, test, false);

		return newTree;
	}

	/**
	 * Help method for printing tree structure.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	private void dumpTree(int depth, StringBuffer text) throws Exception {

		int i, j;

		for (i = 0; i < m_sons.length; i++) {
			text.append("\n");
			;
			for (j = 0; j < depth; j++)
				text.append("|   ");
			text.append(m_localModel.leftSide(m_train));
			text.append(m_localModel.rightSide(i, m_train));
			if (m_sons[i].m_isLeaf) {
				text.append(": ");
				text.append(m_localModel.dumpLabel(i, m_train));
			} else
				m_sons[i].dumpTree(depth + 1, text);
		}
	}

	/**
	 * Help method for printing tree structure as a graph.
	 *
	 * @exception Exception
	 *                if something goes wrong
	 */
	private void graphTree(StringBuffer text) throws Exception {
		if (m_sons != null) {
			for (int i = 0; i < m_sons.length; i++) {
				text.append("N" + m_id + "->" + "N" + m_sons[i].m_id + " [label=\""
						+ m_localModel.rightSide(i, m_train).trim() + "\"]\n");
				if (m_sons[i].m_isLeaf) {
			//		System.out.println("m_sons["+i+"].m_isLeaf"+m_sons[i].m_isLeaf);
					if (m_sons[i].m_isDifferent) {
							text.append("N" + m_sons[i].m_id + " [label=\"" + m_localModel.dumpLabel(i, m_train) + "\" "
									+ "shape=box style=filled  fillcolor=yellow");
					} else {
						text.append("N" + m_sons[i].m_id + " [label=\"" + m_localModel.dumpLabel(i, m_train) + "\" "
								+ "shape=box style=filled fillcolor=green");
					}
					if (m_train != null && m_train.numInstances() > 0) {
						text.append("data =\n" + m_sons[i].m_train + "\n");
						text.append(",\n");
					}
					text.append("]\n");
				} else {
					text.append("N" + m_sons[i].m_id + " [label=\"" + m_sons[i].m_localModel.leftSide(m_train) + "\" ");
					if (m_train != null && m_train.numInstances() > 0) {
						text.append("data =\n" + m_sons[i].m_train + "\n");
						text.append(",\n");
					}
					text.append("]\n");
					m_sons[i].graphTree(text);
				}
			}
		}
	}

	/**
	 * Prints the tree in prefix form
	 */
	private void prefixTree(StringBuffer text) throws Exception {

		text.append("[");
		text.append(m_localModel.leftSide(m_train) + ":");
		for (int i = 0; i < m_sons.length; i++) {
			if (i > 0) {
				text.append(",\n");
			}
			text.append(m_localModel.rightSide(i, m_train));
		}
		for (int i = 0; i < m_sons.length; i++) {
			if (m_sons[i].m_isLeaf) {
				text.append("[");
				text.append(m_localModel.dumpLabel(i, m_train));
				text.append("]");
			} else {
				m_sons[i].prefixTree(text);
			}
		}
		text.append("]");
	}

	/**
	 * Help method for computing class probabilities of a given instance.
	 *
	 * @param classIndex
	 *            the class index
	 * @param instance
	 *            the instance to compute the probabilities for
	 * @param weight
	 *            the weight to use
	 * @return the laplace probs
	 * @throws Exception
	 *             if something goes wrong
	 */
	private double getProbsLaplace(int classIndex, Instance instance, double weight) throws Exception {

		double prob = 0;

		if (m_isLeaf) {
			return weight * localModel().classProbLaplace(classIndex, instance, -1);
		} else {
			int treeIndex = localModel().whichSubset(instance);
			if (treeIndex == -1) {
				double[] weights = localModel().weights(instance);
				for (int i = 0; i < m_sons.length; i++) {
					if (!son(i).m_isEmpty) {
						prob += son(i).getProbsLaplace(classIndex, instance, weights[i] * weight);
					}
				}
				return prob;
			} else {
				if (son(treeIndex).m_isEmpty) {
					return weight * localModel().classProbLaplace(classIndex, instance, treeIndex);
				} else {
					return son(treeIndex).getProbsLaplace(classIndex, instance, weight);
				}
			}
		}
	}

	/**
	 * Help method for computing class probabilities of a given instance.
	 *
	 * @param classIndex
	 *            the class index
	 * @param instance
	 *            the instance to compute the probabilities for
	 * @param weight
	 *            the weight to use
	 * @return the probs
	 * @throws Exception
	 *             if something goes wrong
	 */
	private double getProbs(int classIndex, Instance instance, double weight) throws Exception {

		double prob = 0;

		if (m_isLeaf) {
			prob = weight * localModel().classProb(classIndex, instance, -1);
//			System.out.println("m_id : "+ m_id+ " classIdx : "+classIndex+ "pr : "+ prob);
			return prob;
		} else {
			int treeIndex = localModel().whichSubset(instance);
			if (treeIndex == -1) {
				double[] weights = localModel().weights(instance);
				for (int i = 0; i < m_sons.length; i++) {
					if (!son(i).m_isEmpty) {
						prob += son(i).getProbs(classIndex, instance, weights[i] * weight);
					}
				}
				return prob;
			} else {
				if (son(treeIndex).m_isEmpty) {
					return weight * localModel().classProb(classIndex, instance, treeIndex);
				} else {
					return son(treeIndex).getProbs(classIndex, instance, weight);
				}
			}
		}
	}

	/**
	 * Method just exists to make program easier to read.
	 */
	private ClassifierSplitModel localModel() {

		return (ClassifierSplitModel) m_localModel;
	}

	/**
	 * Method just exists to make program easier to read.
	 */
	private ClassifierTree son(int index) {

		return (ClassifierTree) m_sons[index];
	}

	/**
	 * Set m_isDifferent to true for those leafs of the classifireTree whose label are dofferent with the 
	 * corresponding leaf in the input classifireTree
	 * @throws Exception 
	 */
	 
	 
	 public void color(ClassifierTree m_root) throws Exception {
		 if (m_sons != null) {

				for (int i = 0; i < m_sons.length; i++) {
					if (m_sons[i].m_isLeaf) {
						String s1 = m_localModel.dumpLabel(i, m_train);
						String s2 = m_root.m_localModel.dumpLabel(i, m_root.m_train);
						if(!s1.equals(s2)) {
							m_sons[i].m_isDifferent = true;
			//				System.out.println(m_isDifferent);
						}
						
					} else {
						m_isDifferent = false;
						}
					m_sons[i].color(m_root.m_sons[i]);
					}
				}
			}

			
	}
	 
////public void color(ClassifierTree m_root) {
////	if(m_isLeaf) {
////		System.out.println(m_localModel.m_distribution.maxClass(0)+"   "+m_root.m_localModel.m_distribution.maxClass(0));
////		if (m_localModel.m_distribution.maxClass(0)!=m_root.m_localModel.m_distribution.maxClass(0)) {
////			m_isDifferent = true;
////		} else {
////			m_isDifferent = false;				
////		}
////	} else {
////		for (int i = 0; i < m_sons.length; i++) {
////			m_sons[i].color(m_root.m_sons[i]);
////		}
////	}
////	
////}

