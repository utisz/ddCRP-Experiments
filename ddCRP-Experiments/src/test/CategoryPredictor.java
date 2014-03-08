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



/**
 * An abstract class for predicting categories from a model
 */
abstract public class CategoryPredictor {

  private class ProbObservationThread implements Runnable {
    int index;

    ProbObservationThread(int index) {
      this.index = index;
    }

    @Override
    public void run() { 
      probabilityForObservation.set(this.index, computeLogProbabilityForSampleAtValue(sample, this.index));
    }
  }  
    
  Posterior posterior;
  
  TestSample sample;
  
  Likelihood likelihood;

  // store the values of the SamplerState densities for future use, key is index into SamplerState
  HashMap<Integer, Double> samplerStatePosteriorDensities;

  // store the values of the SamplerState theta for future use, key is index into SamplerState
  HashMap<Integer, Theta> samplerStateThetas;  

  // The full multinomial distribution for each observation
  ArrayList<Double> probabilityForObservation;

  public CategoryPredictor(Posterior posterior, Likelihood likelihood, TestSample sample, HashMap<Integer, Double> samplerStatePosteriorDensities, HashMap<Integer, Theta> samplerStateThetas) {
    this.posterior = posterior;
    this.likelihood = likelihood;
    this.sample = sample;
    this.samplerStatePosteriorDensities = samplerStatePosteriorDensities;  // probably makes more sense to put this in Posterior.java
    this.samplerStateThetas = samplerStateThetas; // dito this
  }

  public void computeProbabilityOfAllOutcomes() {
    probabilityForObservation = new ArrayList<Double>();
    for (int i=0; i<likelihood.getHyperParameters().getVocabSize(); i++)
      probabilityForObservation.add(Double.NEGATIVE_INFINITY);

    for (int i=0; i<likelihood.getHyperParameters().getVocabSize(); i++) 
      probabilityForObservation.set(i, computeLogProbabilityForSampleAtValue(sample, i));

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

  public double computeProbabilityForSample() {
    computeProbabilityOfAllOutcomes();
    return probabilityForObservation.get(sample.getObsCategory().intValue() - 1);   
  }

  public int isSampleInTopTen() {
    computeProbabilityOfAllOutcomes();
    double probAtSample = probabilityForObservation.get(sample.getObsCategory().intValue() - 1);  
    ArrayList<Double> sortedProbabilityForObservation = new ArrayList<Double>(probabilityForObservation);
    Collections.sort(sortedProbabilityForObservation);
    Collections.reverse(sortedProbabilityForObservation);
    for (int i=0; i<10; i++) {
      if (probAtSample >= sortedProbabilityForObservation.get(i))
        return 1;
    }
    return 0;
  }

  public double predictMaxProbForSample() {
    computeProbabilityOfAllOutcomes();
    double maxProb = -1.0;
    int maxProbIndex = 0;
    for (int i=0; i<probabilityForObservation.size(); i++) {
      double prob = probabilityForObservation.get(i);
      if (prob > maxProb) {
        maxProb = prob;
        maxProbIndex = i;
      }
    }
    return (double) maxProbIndex + 1; // observation category is one plus the index
  }

  abstract public Double computeLogProbabilityForSampleAtValue(TestSample mySample, Integer observation);

  abstract public double computeProbabilityForSampleMAP(SamplerState s);

}
