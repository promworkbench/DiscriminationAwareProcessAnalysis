package org.processmining.plugins.workshop.Yaguang.ca;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Example of how to use the algorithm ClaSP, saving the results in a given file
 *
 * @author agomariz
 */
public class MainTestCMClaSP_saveToFile {

	public void execute_CMClaSP(double input_support, String file_address, String output_address) {
		double support = input_support;

		boolean keepPatterns = true;
		boolean verbose = true;
		boolean findClosedPatterns = true;
		boolean executePruningMethods = true;
		// if you set the following parameter to true, the sequence ids of the sequences
		// where
		// each pattern appears will be shown in the result
		boolean outputSequenceIdentifiers = false;

		AbstractionCreator abstractionCreator = AbstractionCreator_Qualitative.getInstance();
		IdListCreator idListCreator = IdListCreatorStandard_Map.getInstance();

		SequenceDatabase sequenceDatabase = new SequenceDatabase(abstractionCreator, idListCreator);

		// double relativeSupport =
		// sequenceDatabase.loadFile(fileToPath("contextClaSP.txt"), support);
		double relativeSupport = 0d;
		try {
			relativeSupport = sequenceDatabase.loadFile(file_address, support);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// double relativeSupport = sequenceDatabase.loadFile(fileToPath("gazelle.txt"),
		// support);

		System.out.println("relativeSupport: "+relativeSupport);
		
		AlgoCM_ClaSP algorithm = new AlgoCM_ClaSP(relativeSupport, abstractionCreator, findClosedPatterns,
				executePruningMethods);

		// System.out.println(sequenceDatabase.toString());
		try {
			algorithm.runAlgorithm(sequenceDatabase, keepPatterns, verbose, output_address, outputSequenceIdentifiers);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Minsup (relative) : " + support);
		System.out.println(algorithm.getNumberOfFrequentPatterns() + " patterns found.");

		if (verbose && keepPatterns) {
			System.out.println(algorithm.printStatistics());
		}

	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		// Load a sequence database
		double support = 0.5;

		boolean keepPatterns = true;
		boolean verbose = true;
		boolean findClosedPatterns = true;
		boolean executePruningMethods = true;
		// if you set the following parameter to true, the sequence ids of the sequences
		// where
		// each pattern appears will be shown in the result
		boolean outputSequenceIdentifiers = false;

		AbstractionCreator abstractionCreator = AbstractionCreator_Qualitative.getInstance();
		IdListCreator idListCreator = IdListCreatorStandard_Map.getInstance();

		SequenceDatabase sequenceDatabase = new SequenceDatabase(abstractionCreator, idListCreator);

		// double relativeSupport =
		// sequenceDatabase.loadFile(fileToPath("contextClaSP.txt"), support);
		double relativeSupport = sequenceDatabase.loadFile(fileToPath("contextClaSp.txt"), support);
		// double relativeSupport = sequenceDatabase.loadFile(fileToPath("gazelle.txt"),
		// support);

		AlgoCM_ClaSP algorithm = new AlgoCM_ClaSP(relativeSupport, abstractionCreator, findClosedPatterns,
				executePruningMethods);

		// System.out.println(sequenceDatabase.toString());
		algorithm.runAlgorithm(sequenceDatabase, keepPatterns, verbose, "/Users/yaguangsun/file/work/paper-project/output111.txt", outputSequenceIdentifiers);
		System.out.println("Minsup (relative) : " + support);
		System.out.println(algorithm.getNumberOfFrequentPatterns() + " patterns found.");

		if (verbose && keepPatterns) {
			System.out.println(algorithm.printStatistics());
		}

		// uncomment if we want to see the Trie graphically
		// ShowTrie.showTree(algorithm.getFrequentAtomsTrie());
	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = MainTestCMClaSP_saveToFile.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
}
