package Likelihood;

import java.util.ArrayList;
import java.util.HashSet;

import model.HyperParameters;
import model.SamplerState;


/**
 * Generic interface for computing likelihood of the data.
 * Your likelihood implementation should implement this interface
 * @author rajarshd
 *
 */
abstract public class Likelihood {
	
  protected HyperParameters hyperParameters;  // Need to abstract the notion of Hyperparameters.  Currently its for Dir.

  public HyperParameters getHyperParameters() {
    return(hyperParameters);
  }

	/**
	 * Method for computing log-likelihood of the data at a table.
	 * @param table_members list of indexes of the observation.
	 * @param list_index index of the list, the observation at the tables belong to.
	 * @return
	 */
	abstract public double computeTableLogLikelihoodFromCustomers(ArrayList<Integer> table_members,int list_index);


  /**
   * Method for computing log-likelihood of the data at a table.
   * @param observations list observation data
   * @return
   */
  abstract public double computeTableLogLikelihood(ArrayList<Double> observations);

  /**
   * Method for computing conditional log likelihood
   * @param observations
   * @param condObservations
   * @return
   */
  abstract public double computeConditionalLogLikelihood(ArrayList<Double> observations, ArrayList<Double> condObservations);

  /**
   * Method for computing the full log likelihood of a sampler state
   * @param SamplerState s
   * @return the log likelihood
   */
  abstract public double computeFullLogLikelihood(SamplerState s);


}
