/**
 * 
 */
package Likelihood;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.math3.special.Gamma;

import model.HyperParameters;
import model.SamplerState;

import data.Data;

/**
 * Dirichlet Likelihood for the collapsed Gibbs Sampler
 * @author rajarshd
 *
 */
public class DirichletLikelihood extends Likelihood {

	private static HashMap<Double,Double> cached_gamma_values = new HashMap<Double,Double>();

	public DirichletLikelihood(HyperParameters h) {
		hyperParameters = h;
	}

	@Override
	public Double computeTableLogLikelihoodFromCustomers(ArrayList<Integer> table_members,
			int list_index) {
		
    if (table_members.size() == 0)
      return null;

		//get the observations
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
		ArrayList<Double> observations = list_observations.get(list_index);
		HashMap<Double,Integer> obs_category_count = new HashMap<Double,Integer>(); //this map will store the index of the venue category and the respective counts of the table members
		//creating the map
		for(int i=0;i<table_members.size();i++)
		{
			Double obs_category = observations.get(table_members.get(i)) - 1;
			if(obs_category_count.get(obs_category) == null) //new category			
				obs_category_count.put(obs_category, 1);
			else
				obs_category_count.put(obs_category, obs_category_count.get(obs_category) + 1 );	
		}
		
		//get the dirichlet hyper-parameter
		ArrayList<Double> dirichletParams = hyperParameters.getDirichletParam();
		double  sum_venue_cat_alpha=0, sum_log_gamma_sum_venue_cat_alpha = 0,sum_alpha =0,sum_log_gamma_alpha=0 ;
		
		for(int i=0;i<dirichletParams.size();i++) //loop for each possible venue category
		{
			Integer category_count = obs_category_count.get(new Double(i));
			if(category_count == null) 
				category_count = 0; //in case no venue of a certain category isnt present, the count is 0
			sum_alpha = sum_alpha + dirichletParams.get(i); 			
			sum_venue_cat_alpha = sum_venue_cat_alpha + dirichletParams.get(i) + category_count;
			sum_log_gamma_sum_venue_cat_alpha = sum_log_gamma_sum_venue_cat_alpha + logGamma(dirichletParams.get(i)+category_count);
			sum_log_gamma_alpha = sum_log_gamma_alpha + logGamma(dirichletParams.get(i));
		}
		
		double log_numerator = sum_log_gamma_sum_venue_cat_alpha - logGamma(sum_venue_cat_alpha);
		double log_denominator = sum_log_gamma_alpha - Gamma.logGamma(sum_alpha); //NO need to compute the denominator as it is same for all tables, given an alpha
		
		double log_likelihood = log_numerator - log_denominator;
		
		return log_likelihood;
	}
	

	/**
	 * Method for computing log-likelihood of the data at a table.
	 * @param table_members list of indexes of the observation.
	 * @param list_index index of the list, the observation at the tables belong to.
	 * @return
	 */
	@Override
	public Double computeTableLogLikelihood(ArrayList<Double> observations) {
    if (observations.size() == 0)
      return null;

		// Counts for each observation
		HashMap<Double,Integer> observationCounts = new HashMap<Double,Integer>(); 
		for (Double obs : observations) {
			obs = obs - 1; // index shift
			if(observationCounts.get(obs) == null) //new category			
				observationCounts.put(obs, 1);
			else
				observationCounts.put(obs, observationCounts.get(obs) + 1 );	
		}

		//get the dirichlet hyper-parameter
		ArrayList<Double> dirichletParams = hyperParameters.getDirichletParam();
		double sumObsAlpha=0, sumLogGammaSumObsAlpha=0, sumAlpha=0, sumLogGammaAlpha=0 ;
		
		for(int i=0;i<dirichletParams.size();i++) //loop for each possible venue category
		{
			Integer obsCount = observationCounts.get(new Double(i));
			if(obsCount == null) 
				obsCount = 0; //in case no venue of a certain category isnt present, the count is 0
			sumAlpha = sumAlpha + dirichletParams.get(i); 			
			sumObsAlpha = sumObsAlpha + dirichletParams.get(i) + obsCount;
			sumLogGammaSumObsAlpha = sumLogGammaSumObsAlpha + logGamma(dirichletParams.get(i)+obsCount);
			sumLogGammaAlpha = sumLogGammaAlpha + logGamma(dirichletParams.get(i));
		}
		
		double logNumerator = sumLogGammaSumObsAlpha - logGamma(sumObsAlpha);
		double logDenominator = sumLogGammaAlpha - Gamma.logGamma(sumAlpha); //NO need to compute the denominator as it is same for all tables, given an alpha
		double logLikelihood = logNumerator - logDenominator;
		
		return logLikelihood;
	}


	/**
	 * Checks if the value of the gamma is cached, if so returns it, else caches it
	 * @param arg
	 * @return
	 */
	private static double logGamma(double arg)
	{
		if(cached_gamma_values.get(arg) == null)
		{
			double log_gamma = Gamma.logGamma(arg);
			cached_gamma_values.put(arg, log_gamma);
			return log_gamma;
		}
		else				
			return cached_gamma_values.get(arg);		
	}



	 /**
   * Computes the sum of the dirichlet conditional likelihood, p(x_t | x_{-t}^l), where x_t is the set of 
   * customer observations from table t, and x_{-t}^l is the set of customer observations assigned to topic l
   * aside from those at table t. 
   * @author jcransh
   * @param table_members
   * @param listIndex
   * @param cond_table_members
   */
  public Double computeConditionalLogLikelihood(ArrayList<Double> observations, ArrayList<Double> condObservations) {
		ArrayList<Double> dirichletParam =  hyperParameters.getDirichletParam();

    if (observations.size() == 0)
      return null;

		// Count observations for condTableMembers 
  	HashMap<Double, Integer> condObservationCounts = new HashMap<Double, Integer>();
  	for (Double obs : condObservations) {
			if(condObservationCounts.get(obs) == null) //new category			
				condObservationCounts.put(obs, 1);
			else
				condObservationCounts.put(obs, condObservationCounts.get(obs) + 1 );  		
  	}

  	// compute the log likelihood. for each observation compute p(observation_i | condObservations)
  	double logLik = 0.0;
  	for (Double obs : observations) {
  		Integer condObsCount = 0;
  		if(condObservationCounts.get(obs) != null) 
  			condObsCount = condObservationCounts.get(obs);
  		Integer obsInt = obs.intValue()-1;
  		double term = condObsCount + dirichletParam.get(obsInt);
  		logLik += Math.log(term);
  	}

  	double preNorm = logLik;

  	// compute the normalizing constant A + N - 1
  	double normConst = 0.0;
  	for (int i=0; i<dirichletParam.size(); i++)
  		normConst += dirichletParam.get(i);
  	normConst += observations.size() + condObservations.size() - 1;
  	logLik -= observations.size() * Math.log(normConst);

  	return logLik;
  }


  /**
   * Returns the full log likelihood of the model at the given sampler state
   * @return
   */
  @Override
  public Double computeFullLogLikelihood(SamplerState s) {
    double ll = 0;
    HashSet<Integer> topics = s.getAllTopics();
    for (Integer topic : topics) {
      ArrayList<Double> obs = s.getAllObservationsForTopic(topic);
      if (obs.size() > 0)
        ll += computeTableLogLikelihood(obs);  
    }
    return ll;
  }

  /**
   * SAME AS ABOVE BUT FOR DDCRP. 
   * MUST SUBCLASS THIS FOR RELEASE
   */
  public Double computeFullLogLikelihoodDDCRP(SamplerState s) {
    ArrayList<HashMap<Integer, HashSet<Integer>>> customersAtTableList = s.getCustomersAtTableList();
    double ll = 0;
    for (int listIndex=0; listIndex<customersAtTableList.size(); listIndex++) {
      HashMap<Integer, HashSet<Integer>> customersAtTable = customersAtTableList.get(listIndex);
      for (Integer tableId : customersAtTable.keySet()) {
        if (customersAtTable.get(tableId) != null) {
          HashSet<Integer> hs = customersAtTable.get(tableId);
          ArrayList<Integer> tableMembers = new ArrayList<Integer>(hs);
          ll += computeTableLogLikelihoodFromCustomers(tableMembers, listIndex);
        }
      }
    }
    return ll;
  }


}
