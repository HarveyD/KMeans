import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class kMeans {

	public static double cosineSimilarity(Document d, Centroid c) {
		double dotProduct = 0, magnitudeA = 0, magnitudeB = 0;

		HashMap<Integer, Double> smallSet = new HashMap<Integer, Double>();
		HashMap<Integer, Double> largeSet = new HashMap<Integer, Double>();

		if (d.getWordSet().getWords().size() > c.getWordSet().getWords().size()) {
			smallSet = c.getWordSet().getWords();
			largeSet = d.getWordSet().getWords();
		} else {
			smallSet = d.getWordSet().getWords();
			largeSet = c.getWordSet().getWords();
		}

		// Begin cosine similarity calculation
		for (Integer i : smallSet.keySet()) {
			if (largeSet.containsKey(i)) {
				dotProduct += smallSet.get(i) * largeSet.get(i);

				//Add document magnitude which isn't calculated each time
				
				magnitudeA += Math.pow(smallSet.get(i), 2);
				magnitudeB += Math.pow(largeSet.get(i), 2);
			}
		}

		magnitudeA = Math.sqrt(magnitudeA);
		magnitudeB = Math.sqrt(magnitudeB);

		return (dotProduct / (magnitudeA * magnitudeB));
	}

	// Return the inverse term frequency of a term within the set of documents
	public static double inverseTermFrequency(ArrayList<String> docName,
			String term) {
		double count = 0;
		boolean end = false;

		for (String doc : docName) {

			try {

				BufferedReader reader = new BufferedReader(new FileReader(doc));
				String nextLine;

				while ((nextLine = reader.readLine()) != null && !end) {
					String[] words = nextLine.split(" ");

					for (String s : words) {
						if (s.equalsIgnoreCase(term)) {
							count++;
							end = true;
							break;
						}
					}
				}

				reader.close();

			} catch (IOException e) {
				System.out.println("error in reading from file");
			}
		}

		return Math.log(docName.size() / count);
	}

	/*Returns the frequency of a term within a certain document
	public static double termFrequency(HashSet<String> docVocab, String term) {
		int count = 0;

		for (String s : docVocab) {
			if (s.equalsIgnoreCase(term)) {
				count++;
			}
		}

		return count / docVocab.size();
	}*/

	// BUILD A GLOBAL WORD LIST THAT IS USED IN ALL DOCUMENTS
	public static HashSet<String> globalVectorBuild(
			ArrayList<String> documentList) {

		HashSet<String> wordVector = new HashSet<String>();

		for (String s : documentList) {
			try {

				BufferedReader reader = new BufferedReader(new FileReader(s));
				String nextLine;

				while ((nextLine = reader.readLine()) != null) {

					String[] words = nextLine.split(" ");

					for (String w : words) {
						// Add stopword removal, lowercasing etc.

						wordVector.add(w.toLowerCase().replaceAll(
								"[^\\p{Alpha}]", ""));
					}
				}

				reader.close();

			} catch (IOException e) {
				System.out.println("error in reading from file");
			}
		}

		return wordVector;
	}

	public static void main(String[] args) {
		// Set Source Folder
		String sourceFolder = "blog_data_test";

		// Set Number of Clusters
		int k = 3;

		// READ DOCUMENTS AND BUILD DOCUMENT/WORD SETS
		ArrayList<File> fileList = new ArrayList<File>(FileFinder.GetAllFiles(
				sourceFolder, /* File Extension */null, /* Recurse */false));

		// List of all Documents found
		ArrayList<Document> docList = new ArrayList<Document>();

		// Global dictionary. String and it's Index
		
		//Get the top 100/200 words
		HashMap<String, Integer> globalDictionary = new HashMap<String, Integer>();

		ArrayList<String> stopwords = new ArrayList<String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					"src/stopwords.txt"));
			String nextLine;
			
			while ((nextLine = reader.readLine()) != null) {
				stopwords.add(nextLine);
			}

			reader.close();

		} catch (IOException e) {
			System.out.println("error in reading from file");
		}

		int documentNumber = 0;

		for (File f : fileList) {
			Document d = new Document(documentNumber, f.toString());

			// Word and it's count
			HashMap<String, Double> localDictionary = new HashMap<String, Double>();

			try {
				BufferedReader reader = new BufferedReader(new FileReader(f));
				String nextLine;

				// To normalise the frequencies
				int docSize = 0;

				while ((nextLine = reader.readLine()) != null) {
					String[] words = nextLine.split(" ");

					for (String w : words) {
						if (!stopwords.contains(w)) {

							int index;
							docSize++;

							// Add stopword removal, lowercasing etc.
							w = w.toLowerCase().replaceAll("[^\\p{Alpha}]", "");

							// Add to global dictionary
							if (globalDictionary.containsKey(w)) {
								index = globalDictionary.get(w);
							} else {
								index = globalDictionary.size();
								globalDictionary.put(w, index);
							}

							if (localDictionary.containsKey(w)) {
								double increment = localDictionary.get(w) + 1.0;
								localDictionary.put(w, increment);
							} else {
								localDictionary.put(w, 1.0);
							}

						}
						;
					}
				}

				ArrayList<Double> topScores = new ArrayList<Double>();
				
				for (String s : localDictionary.keySet()) {
					double freq = localDictionary.get(s) / localDictionary.size();
					topScores.add(freq);
		
					if(freq >0.025)
						d.addWord(globalDictionary.get(s), freq);
				}
				
			//	Collections.sort(topScores);
				
				//for(int i=0; i<100; i++){
					
				//}
				

				reader.close();

			} catch (IOException e) {
				System.out.println("error in reading from file");
			}

			docList.add(d);
			documentNumber++;
		}

		System.out.println(documentNumber
				+ " files added with a total dictionary size of: "
				+ globalDictionary.size() + " words.");

		// RANDOMLY ASSIGN CENTROID
		ArrayList<Centroid> centroidList = new ArrayList<Centroid>();

		for (int i = 0; i < k; i++) {
			int randDocID = (int) Math.floor(Math.random()
					* (docList.size() + 1));
			Centroid centroid = new Centroid(i, docList.get(randDocID)
					.getWordSet(), docList, globalDictionary);

			centroidList.add(centroid);
		}

		// Iterate until there were no cluster changes or were 100 iterations
		// (don't want it going forever lol).
		boolean go = true;
		int iterations = 0;
		
		while (go && iterations < 100) {
			go = false;
			// FIND THE CLOSEST CENTROID FOR EACH DOCUMENT USING COSINE
			// SIMILARITY
			for (Document d : docList) {
				double maxDist = 0;
				for (Centroid c : centroidList) {
					double result = cosineSimilarity(d, c);
					System.out.println("Similarity of " + result
							+ " to centroid " + c.ID);

					if (result > maxDist) {
						go = true;
						maxDist = result;
						System.out.println("Document: " + d.getID()
								+ " assigned to centroid" + c.ID);

						// Remove where document resided before and then
						// reassign.
						for (Centroid cent : centroidList) {
							if (cent.containsDoc(d.getID()))
								cent.removeDoc(d.getID());
						}
						c.assignDoc(d.getID());

					}
				}
			}
			iterations++;

			for (Centroid cent : centroidList) {
				cent.calculateCentroid();
			}
		}
		
		for (Centroid cent : centroidList) {
			HashMap<Integer, Double> documentt = new HashMap<Integer, Double>();
			ArrayList<Double> topScores = new ArrayList<Double>();
			
			for(Integer i: cent.docsAssigned){
				double result = cosineSimilarity(docList.get(i), cent);
				topScores.add(result);
				documentt.put(i, result);
			}
			
			Collections.sort(topScores ,Collections.reverseOrder());
			//Collections.sort(topScores);
			
			System.out.println(cent.ID + ": ");
			
			for(int i=0; i<5; i++){
				for(Integer ii: documentt.keySet()){
					if(documentt.get(ii).equals(topScores.get(i))){
						System.out.print(docList.get(ii).filename);
						System.out.println();
					}
				}
			}
			System.out.println();
		}		
	}
}

class Document {
	int ID;
	String filename;
	double distanceToCentroid;
	WordSet wordSet;

	Document(int ID, String name) {
		this.ID = ID;
		filename = name;
		wordSet = new WordSet();
	}

	void addWord(int index, double score) {
		wordSet.put(index, score);
	}

	WordSet getWordSet() {
		return wordSet;
	}

	void setDistanceCentroid(int dist) {
		distanceToCentroid = dist;
	}

	int getID() {
		return ID;
	}
}

class WordSet {
	// <Word Index Number, Score>
	HashMap<Integer, Double> words;

	WordSet() {
		words = new HashMap<Integer, Double>();
	}

	void put(int word, double score) {
		words.put(word, score);
	}

	double getScore(int index) {
		return words.get(index);
	}

	HashMap<Integer, Double> getWords() {
		return words;
	}

	// Fix
	void calculateScore(int index) {

	}
}

class Centroid {
	// The Centre of the Centroid is the average of all words within the
	// Documents assigned to it
	int ID;
	ArrayList<Document> allDocs;
	ArrayList<Integer> docsAssigned;
	HashMap<String, Integer> dictionary;
	WordSet wordSet;

	Centroid(int ID, WordSet wordSet, ArrayList<Document> allDocs,
			HashMap<String, Integer> dictionary) {
		this.ID = ID;
		this.wordSet = wordSet;
		this.allDocs = allDocs;
		this.dictionary = dictionary;
		docsAssigned = new ArrayList<Integer>();

	}

	void assignDoc(int docID) {
		docsAssigned.add(docID);
	}

	boolean containsDoc(int docID) {
		if (docsAssigned.contains(docID)) {
			return true;
		} else {
			return false;
		}
	}

	ArrayList<Integer> returnDocs() {
		return docsAssigned;
	}

	void removeDoc(int docID) {
		docsAssigned.remove(docsAssigned.indexOf(docID));
	}

	WordSet getWordSet() {
		return wordSet;
	}

	void calculateCentroid() {
		// Maybe just iterate through the whole dictionary?

		// Avg the vectors where docsAssigned = allDocs and assign to
		// centroidVector
		// for(HashMap<Integer, Double> words: wordSet.getWords())

		wordSet = new WordSet();

		for (String w : dictionary.keySet()) {
			Integer word = dictionary.get(w);
			double avg = 0.0;
			int count=0;
			for (Integer docID : docsAssigned) {
				if (allDocs.get(docID).getWordSet().getWords().get(word) != null) {
					count++;
					avg += allDocs.get(docID).getWordSet().getWords().get(word);
				}
			}

			// Average the frequency of the terms to form the new centroid score
			// :)
			if(avg!= 0.0)
				wordSet.put(word, avg / count);
		}
	}

}
