package test;

import Likelihood.Likelihood;
import data.Data;
import test.Test;
import model.Posterior;
import model.CityTable;
import model.Theta;
import model.SamplerState;
import model.SamplerStateTracker;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.la4j.matrix.sparse.CRSMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.sparse.CompressedVector;
import org.la4j.factory.CRSFactory;
import org.la4j.vector.functor.VectorProcedure;
import org.la4j.vector.sparse.SparseVector;


/**
 * Helper Class to make predictions about unseen data given 
 * the estimated posterior 
 * @author jcransh
 */
public class CategoryPredictorDDCRF extends CategoryPredictor {

  private static class GetNonZeroPriorProcedure implements VectorProcedure {
    public GetNonZeroPriorProcedure() {
      this.nonZeroIndices = new HashMap<Integer, Double>();
    }

    public HashMap<Integer, Double> nonZeroIndices;

    @Override
    public void apply(int i, double value) {
      this.nonZeroIndices.put(i, value);
    }
  }
    
  public CategoryPredictorDDCRF(Posterior posterior, Likelihood likelihood, TestSample sample, HashMap<Integer, Double> samplerStatePosteriorDensities, HashMap<Integer, Theta> samplerStateThetas) {
    super(posterior, likelihood, sample, samplerStatePosteriorDensities, samplerStateThetas);
  }

  @Override
  public double computeProbabilityForSampleMAP(SamplerState s, Theta theta) {
    int observation = sample.getObsCategory().intValue() - 1;

    double probability = 0.0;

    int listIndex = sample.getListIndex();
    int obsIndex = sample.getObsIndex();

    // Get the priors for the current sample
    ArrayList<CRSMatrix> distanceMatrices = Data.getDistanceMatrices();
    CRSMatrix distance_matrix = distanceMatrices.get(listIndex); // getting the correct distance matrix 
    SparseVector sparsePriors = (SparseVector) distance_matrix.getRow(obsIndex);
    
    // Get the non-zero entries of the prior
    GetNonZeroPriorProcedure proc = new GetNonZeroPriorProcedure();
    sparsePriors.eachNonZero(proc);
    HashMap<Integer, Double> nonZeroPrior = proc.nonZeroIndices;

    // Set the prior for self linkage, and normalize the prior
    nonZeroPrior.put(obsIndex, likelihood.getHyperParameters().getSelfLinkProb()); 
    double sum = 0;
    for (Entry<Integer, Double> entry : nonZeroPrior.entrySet()) {
      Integer priorIndex = entry.getKey();
      Double priorValue = entry.getValue();
      sum += priorValue;
    }
    for (Entry<Integer, Double> entry : nonZeroPrior.entrySet()) {
      Integer priorIndex = entry.getKey();
      Double priorValue = entry.getValue();
      nonZeroPrior.put(priorIndex, priorValue / sum);
    }

    for (Entry<Integer, Double> entry : nonZeroPrior.entrySet()) {
      Integer priorIndex = entry.getKey();
      Double dDCRPPrior = entry.getValue();

      // In the current sampler state, get the table and the topic of the linked-to table
      int linkedToTable = s.get_t(priorIndex, listIndex);

      // If the linkedToTable a self link? If so, we need to sample a new topic
      if (priorIndex == obsIndex) {
        // copute the CRP Prior
        double cRPSelfLinkProb = likelihood.getHyperParameters().getSelfLinkProbCRP();
        double cRPPriorNormConst = s.getT() + cRPSelfLinkProb; // the normalizing constant for the CRP prior is the total number of ddCRP tables plus the self link prob

        // for each topic
        HashMap<Integer,Integer> numTablesPerTopic = s.getM();
        for (Entry<Integer, Integer> tablesEntry : numTablesPerTopic.entrySet()) {
          Integer topic = tablesEntry.getKey();
          Integer numTablesAtTopic = tablesEntry.getValue();
          double cRPPrior = numTablesAtTopic / cRPPriorNormConst; // normalize the prior
          double probObservation = theta.observationProbabilityInTheta(observation, topic);
          probability += dDCRPPrior * cRPPrior * probObservation;  
        }
        // now for self-linkage (i.e. creating a new topic)
        // For a new topic with only table, that has only one customer
        // the probability of observing a single is based on the dirichlet param
        double cRPPrior = cRPSelfLinkProb / cRPPriorNormConst; // normalize the prior
        double probObservation = 1 / (double) 419;  // TODO: FIX THIS. THIS ASSUMES FLAT DIRICHLET PARAM
        probability += dDCRPPrior * cRPPrior * probObservation;  
      }
      // Otherwise, the topic is determined by the table we're linking to
      else {
        CityTable ct = new CityTable(listIndex, linkedToTable);
        Integer linkedToTopic = s.getTopicForCityTable(ct);
        double probObservation = theta.observationProbabilityInTheta(observation, linkedToTopic);
        probability += dDCRPPrior * probObservation;  
      }
    }

    return probability;
  }

  @Override 
  public Double computeLogProbabilityForSampleAtValue(TestSample mySample, Integer observation) {
    double probability = 0.0;

    int listIndex = mySample.getListIndex();
    int obsIndex = mySample.getObsIndex();

    // Get the priors for the current sample
    ArrayList<CRSMatrix> distanceMatrices = Data.getDistanceMatrices();
    CRSMatrix distance_matrix = distanceMatrices.get(listIndex); // getting the correct distance matrix 
    SparseVector sparsePriors = (SparseVector) distance_matrix.getRow(obsIndex);
    
    // Get the non-zero entries of the prior
    GetNonZeroPriorProcedure proc = new GetNonZeroPriorProcedure();
    sparsePriors.eachNonZero(proc);
    HashMap<Integer, Double> nonZeroPrior = proc.nonZeroIndices;

    // Set the prior for self linkage, and normalize the prior
    nonZeroPrior.put(obsIndex, likelihood.getHyperParameters().getSelfLinkProb()); 
    double sum = 0.0;
    for (Entry<Integer, Double> entry : nonZeroPrior.entrySet()) {
      Integer priorIndex = entry.getKey();
      Double priorValue = entry.getValue();
      sum += priorValue;
    }
    for (Entry<Integer, Double> entry : nonZeroPrior.entrySet()) {
      Integer priorIndex = entry.getKey();
      Double priorValue = entry.getValue();
      nonZeroPrior.put(priorIndex, priorValue / sum);
    }
 
    ArrayList<SamplerState> states = SamplerStateTracker.samplerStates;

    // underflow magic
    double logSumProb = 0.0;
    ArrayList<Double> logProbability = new ArrayList<Double>();
    double maxLogProbability = Double.NEGATIVE_INFINITY;

    for (Entry<Integer, Double> entry : nonZeroPrior.entrySet()) {
      Integer priorIndex = entry.getKey();
      Double dDCRPPrior = entry.getValue();

      // have to do some more underflow magic to handle the probabilities
      double logSumProbOverStates = 0.0;
      ArrayList<Double> logStateProbability = new ArrayList<Double>();

      // sum over each sampler state ( discounting the first two iterations as burnin )
      for (int index=2; index<states.size(); index++) {
        SamplerState s = states.get(index);
        // In the current sampler state, get the table and the topic of the linked-to table
        int linkedToTable = s.get_t(priorIndex, listIndex);
       
        // get the emmission probability of the new data given the state
        // we know the topic, we could just give an MLE plugin estimate for the mult distribution,
        // or we can work ou the marginalized probability
        Theta theta = samplerStateThetas.get(index);

        double logPosteriorDensity = samplerStatePosteriorDensities.get(index);

        // If the linkedToTable a self link? If so, we need to sample a new topic
        if (priorIndex == obsIndex) {
          // copute the CRP Prior
          double cRPSelfLinkProb = likelihood.getHyperParameters().getSelfLinkProbCRP();
          double cRPPriorNormConst = s.getT() + cRPSelfLinkProb; // the normalizing constant for the CRP prior is the total number of ddCRP tables plus the self link prob

          // for each topic
          HashMap<Integer,Integer> numTablesPerTopic = s.getM();
          for (Entry<Integer, Integer> tablesEntry : numTablesPerTopic.entrySet()) {
            Integer topic = tablesEntry.getKey();
            Integer numTablesAtTopic = tablesEntry.getValue();
            double cRPPrior = numTablesAtTopic / cRPPriorNormConst; // normalize the prior
            double probObservation = theta.observationProbabilityInTheta(observation, topic);
            probability += dDCRPPrior * cRPPrior * probObservation;  
          }
          // now for self-linkage (i.e. creating a new topic)
          // For a new topic with only table, that has only one customer
          // the probability of observing a single is based on the dirichlet param
          double cRPPrior = cRPSelfLinkProb / cRPPriorNormConst; // normalize the prior
          double probObservation = 1 / (double) 419;  // TODO: FIX THIS. THIS ASSUMES FLAT DIRICHLET PARAM
          logStateProbability.add( Math.log(dDCRPPrior) + Math.log(cRPPrior) + Math.log(probObservation) + logPosteriorDensity );
        }
        // Otherwise, the topic is determined by the table we're linking to
        else {
          CityTable ct = new CityTable(listIndex, linkedToTable);
          Integer linkedToTopic = s.getTopicForCityTable(ct);
          double probObservation = theta.observationProbabilityInTheta(observation, linkedToTopic);
          logStateProbability.add( Math.log(dDCRPPrior) + Math.log(probObservation) + logPosteriorDensity );
        }
      }

      double maxLogStateProbability = Double.NEGATIVE_INFINITY;

      // for underflow, get the max of logStateProbability
      for (Double p : logStateProbability) {
        if (p > maxLogStateProbability)
          maxLogStateProbability = p;
      }
      // subtract the max from each term, exponentiate, and sum
      for (Double p : logStateProbability)
        logSumProbOverStates += Math.exp(p - maxLogStateProbability);
      // now add the max back in 
      logSumProbOverStates = Math.log(logSumProbOverStates) + maxLogStateProbability;

      // add this to the outer sum array
      logProbability.add(logSumProbOverStates);
    
    }

    // now do the same tick on logProbability as logSumProbOverStates (should pull this out to a Util)
    // for underflow, get the max of logStateProbability
    for (Double p : logProbability) {
      if (p > maxLogProbability)
        maxLogProbability = p;
    }

    // subtract the max from each term, exponentiate, and sum
    for (Double p : logProbability)
      logSumProb += Math.exp(p - maxLogProbability);
    // now add the max back in 
    logSumProb = Math.log(logSumProb) + maxLogProbability;

    return logSumProb;
  }

  
}