package model;

import java.util.ArrayList;

/**
 * Class to store the hyper-parameters.
 * @author rajarshd
 *
 */
public class HyperParameters {
	
	/**
	 * The number of words in the vocab
	 */
	private static int VOCAB_SIZE;

	/**
	 * The Dirichlet hyper params
	 */
	private ArrayList<Double> dirichletParam;
	
	/**
	 * The self link probability in the ddCRP prior
	 */
	private double selfLinkProb;
	
	/**
	 * The self link probability in the CRP prior
	 */
	private double selfLinkProbCRP; 

	

	public HyperParameters(int vocabSize, ArrayList<Double> dirichlet, double ddcrp,double crp) {
		VOCAB_SIZE = vocabSize;
		dirichletParam = dirichlet;
		selfLinkProb = ddcrp;
		selfLinkProbCRP = crp;
	}

	/**
	 * Get the vocab size
	 */
	public int getVocabSize() {
		return VOCAB_SIZE;
	}

	/**
	 * Getter for the dirichlet parameter
	 */
	public ArrayList<Double> getDirichletParam() {
		return dirichletParam;
	}

  /**
   * Getter for the self link probability
   */
	public double getSelfLinkProb() {
		return selfLinkProb;
	}
	
	public double getSelfLinkProbCRP() {
		return selfLinkProbCRP;
	}
}
