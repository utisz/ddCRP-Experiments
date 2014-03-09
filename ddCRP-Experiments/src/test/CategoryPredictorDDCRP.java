package test;

import Likelihood.Likelihood;
import data.Data;
import test.Test;
import model.Posterior;
import model.CityTable;
import model.Theta;
import model.ThetaDDCRP;
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
public class CategoryPredictorDDCRP extends CategoryPredictor {

  private HashMap<CityTable, HashMap<Integer, Double>> multHash = new HashMap<CityTable, HashMap<Integer, Double>>(); 

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
    
  public CategoryPredictorDDCRP
  (Posterior posterior, Likelihood likelihood, TestSample sample, HashMap<Integer, Double> samplerStatePosteriorDensities, HashMap<Integer, Theta> samplerStateThetas) {
    super(posterior, likelihood, sample, samplerStatePosteriorDensities, samplerStateThetas);
  }

  @Override
  public double computeLogProbabilityForSampleAtValueMAP(SamplerState s, TestSample mySample, Integer observation) {
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

      // In the current sampler state, get the tableId of the linked-to table
      int linkedToTable = s.get_t(priorIndex, listIndex);

      // Get the multinomial distribution for this table
      ThetaDDCRP theta = new ThetaDDCRP(s, likelihood.getHyperParameters());
      CityTable ct = new CityTable(listIndex, linkedToTable);
      // First check the hash
      HashMap<Integer, Double> mult = multHash.get(ct);
      if (mult == null) {
        mult = theta.estimateThetaAtCityAndTable(listIndex, linkedToTable);
        multHash.put(ct, mult);
      }
      Double probObservation = mult.get(observation);
      if (probObservation == null)
        probObservation = 0.000000000001;

      probability += dDCRPPrior * probObservation;  
    }

    return Math.log(probability);
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
        // In the current sampler state, get the tableId of the linked-to table
        int linkedToTable = s.get_t(priorIndex, listIndex);
       
        // Get the multinomial distribution for this table
        ThetaDDCRP theta = new ThetaDDCRP(s, likelihood.getHyperParameters());
        CityTable ct = new CityTable(listIndex, linkedToTable);
        // First check the hash
        HashMap<Integer, Double> mult = multHash.get(ct);
        if (mult == null) {
          mult = theta.estimateThetaAtCityAndTable(listIndex, linkedToTable);
          multHash.put(ct, mult);
        }
        Double probObservation = mult.get(observation);
        if (probObservation == null)
          probObservation = 0.000000000001;

        double logPosteriorDensity = samplerStatePosteriorDensities.get(index);

        logStateProbability.add(  Math.log(dDCRPPrior) + Math.log(probObservation) + logPosteriorDensity );
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