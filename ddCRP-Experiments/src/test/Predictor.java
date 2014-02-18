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
public class Predictor {

  private class ProbObservationThread implements Runnable {
    int index;

    ProbObservationThread(int index) {
      this.index = index;
    }

    @Override
    public void run() { 
      probabilityForObservation.set(this.index, computeLogProbabilityForSampleAtValue(this.index));
    }
  }

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
  
  Test test;
  
  Posterior posterior;
  
  TestSample sample;
  
  Likelihood likelihood;

  ArrayList<Double> probabilityForObservation;

  // store the values of the SamplerState densities for future use
  HashMap<SamplerState, Double> samplerStatePosteriorDensities;

  // store the values of the SamplerState theta for future use
  HashMap<SamplerState, Theta> samplerStateThetas;

  public Predictor(Posterior posterior, Likelihood likelihood, TestSample sample, HashMap<SamplerState, Double> samplerStatePosteriorDensities, HashMap<SamplerState, Theta> samplerStateThetas) {
    this.test = test;
    this.posterior = posterior;
    this.sample = sample;
    this.likelihood = likelihood;
    this.samplerStatePosteriorDensities = samplerStatePosteriorDensities;
    this.samplerStateThetas = samplerStateThetas;
  }

  public Predictor() {}

  public double computeProbabilityForSample() {
    computeProbabilityOfAllOutcomes();
    return probabilityForObservation.get(sample.getVenueCategory().intValue() - 1);   
  }

  public void computeProbabilityOfAllOutcomes() {
    probabilityForObservation = new ArrayList<Double>();
    for (int i=0; i<likelihood.getHyperParameters().getVocabSize(); i++)
      probabilityForObservation.add(Double.NEGATIVE_INFINITY);

    // Make this threaded
    ExecutorService exec = Executors.newFixedThreadPool(20);
    for (int i=0; i<likelihood.getHyperParameters().getVocabSize(); i++) { 
      exec.execute(new ProbObservationThread(i));
    }
    exec.shutdown();
    try {
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      System.out.println("Error in thread");
    }

    // System.out.println("Just outside of threaded loop: " + probabilityForObservation);

    // Handle underflow problems
    Double maxLogProb = Double.NEGATIVE_INFINITY;
    for (int i=0; i<probabilityForObservation.size(); i++) {
      double logProb = probabilityForObservation.get(i);
      if (logProb > maxLogProb)
        maxLogProb = logProb;
    }

    // scale by maxLogProb then exponentiate
    for (int i=0; i<probabilityForObservation.size(); i++)
      probabilityForObservation.set(i, Math.exp(probabilityForObservation.get(i) - maxLogProb) );

    // now normalize
    double normConst = 0.0;
    for (Double p : probabilityForObservation)
      normConst += p;
    for (int i=0; i<probabilityForObservation.size(); i++)
      probabilityForObservation.set(i, probabilityForObservation.get(i) / normConst);    
  }
 
  public Double computeLogProbabilityForSampleAtValue(Integer observation) {
    double probability = 0.0;

    int listIndex = sample.getCityIndex();
    int cityIndex = sample.getListIndex();

    // Get the priors for the current sample
    ArrayList<CRSMatrix> distanceMatrices = Data.getDistanceMatrices();
    CRSMatrix distance_matrix = distanceMatrices.get(listIndex); // getting the correct distance matrix 
    SparseVector sparsePriors = (SparseVector) distance_matrix.getRow(cityIndex);
    
    // Get the non-zero entries of the prior
    GetNonZeroPriorProcedure proc = new GetNonZeroPriorProcedure();
    sparsePriors.eachNonZero(proc);
    HashMap<Integer, Double> nonZeroPrior = proc.nonZeroIndices;

    // Set the prior for self linkage, and normalize the prior
    nonZeroPrior.put(listIndex, likelihood.getHyperParameters().getSelfLinkProb()); 
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
        CityTable ct = new CityTable(listIndex, linkedToTable);
        Integer linkedToTopic = s.getTopicForCityTable(ct);

        // get the emmission probability of the new data given the state
        // we know the topic, we could just give an MLE plugin estimate for the mult distribution,
        // or we can work ou the marginalized probability
        Theta theta = samplerStateThetas.get(s);
        double probObservation = theta.observationProbabilityInTopic(observation, linkedToTopic);

        // get the CRP prior based on the linkedToTopic
        Integer numTablesAtTopic = s.getM().get(linkedToTopic);
        double cRPPrior = 0.0;
        double cRPSelfLinkProb = likelihood.getHyperParameters().getSelfLinkProbCRP();
        double cRPPriorNormConst = s.getT() + cRPSelfLinkProb; // the normalizing constant for the CRP prior is the total number of ddCRP tables plus the self link prob
        if (numTablesAtTopic == 1)
          cRPPrior = cRPSelfLinkProb;
        else
          cRPPrior = numTablesAtTopic;
        cRPPrior = cRPPrior / cRPPriorNormConst; // normalize the prior

        // get the posterior density at the samper state
        double logPosteriorDensity = samplerStatePosteriorDensities.get(s);
        logStateProbability.add( Math.log(dDCRPPrior) + Math.log(cRPPrior) + Math.log(probObservation) + logPosteriorDensity );
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