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
 * Helper Class to make predictions about unseen data given 
 * the estimated posterior. Given a set of
 * @author jcransh
 */
public class LocationPredictorDDCRF extends CategoryPredictorDDCRF implements LocationPredictor {

  HashMap<TestSample, Double> probabilityForLocation = new HashMap<TestSample, Double>();

  ArrayList<TestSample> inCitySamples;

  public LocationPredictorDDCRF(Posterior posterior, Likelihood likelihood, TestSample sample, ArrayList<TestSample> inCitySamples, HashMap<Integer, Double> samplerStatePosteriorDensities, HashMap<Integer, Theta> samplerStateThetas) {
    super(posterior, likelihood, sample, samplerStatePosteriorDensities, samplerStateThetas);
    this.inCitySamples = inCitySamples;
  }

  @Override
  public double computeLocationProbabilityForSample() {
    return probabilityForLocation.get(sample);   
  }
 
  @Override
  public void computeProbabilityForAllLocations() {
    Integer trueObservation = sample.getObsCategory().intValue() - 1;
    for (TestSample s : inCitySamples) {
      double prob = computeLogProbabilityForSampleAtValue(s, trueObservation);
      probabilityForLocation.put(s, prob);
    }

    // Handle underflow problems
    Double maxLogProb = Double.NEGATIVE_INFINITY;
    for (Double logProb : probabilityForLocation.values()) {
      if (logProb > maxLogProb)
        maxLogProb = logProb;
    }

    // scale by maxLogProb then exponentiate
    for (Entry<TestSample, Double> entry : probabilityForLocation.entrySet()) {
      TestSample key = entry.getKey();
      Double value = entry.getValue();
      probabilityForLocation.put(key, Math.exp(value - maxLogProb));
    }

    // Normalize
    double sum = 0;
    for (Double value : probabilityForLocation.values()) 
      sum += value;
    for (Entry<TestSample, Double> entry : probabilityForLocation.entrySet()) {
      TestSample key = entry.getKey();
      Double value = entry.getValue();
      probabilityForLocation.put(key, value / sum);
    }
  }
  
  public void computeProbabilityForAllLocationsMAP(SamplerState s) {
    Integer trueObservation = sample.getObsCategory().intValue() - 1;
    for (TestSample sam : inCitySamples) {
      double prob = computeLogProbabilityForSampleAtValueMAP(s, sam, trueObservation);
      probabilityForLocation.put(sam, prob);
    }

    // Handle underflow problems
    Double maxLogProb = Double.NEGATIVE_INFINITY;
    for (Double logProb : probabilityForLocation.values()) {
      if (logProb > maxLogProb)
        maxLogProb = logProb;
    }

    // scale by maxLogProb then exponentiate
    for (Entry<TestSample, Double> entry : probabilityForLocation.entrySet()) {
      TestSample key = entry.getKey();
      Double value = entry.getValue();
      probabilityForLocation.put(key, Math.exp(value - maxLogProb));
    }

    // Normalize
    double sum = 0;
    for (Double value : probabilityForLocation.values()) 
      sum += value;
    for (Entry<TestSample, Double> entry : probabilityForLocation.entrySet()) {
      TestSample key = entry.getKey();
      Double value = entry.getValue();
      probabilityForLocation.put(key, value / sum);
    }
  }

  /*
   * This method is the same as the DDCRP implementation. Suggest we should figure out a better
   * class structure
   */
  @Override
  public int predictMaxProbForLocations() {
    double maxProb = -1.0;
    TestSample maxProbSample = null;
    for (TestSample s: inCitySamples) {
      double prob = probabilityForLocation.get(s);
      if (prob > maxProb) {
        maxProb = prob;
        maxProbSample = s;
      }
    }
    if (computeLocationProbabilityForSample() == maxProb) // if there is a tie, we pick the correct sample
      return sample.getObsIndex();
    else
      return maxProbSample.getObsIndex(); 
  }

  // public int  predictMaxProbForLocationsMAP(SamplerState s) {
  //   computeProbabilityForAllLocationsMAP(s);
  //   double maxProb = -1.0;
  //   TestSample maxProbSample = null;
  //   for (TestSample sam: inCitySamples) {
  //     double prob = probabilityForLocation.get(sam);
  //     if (prob > maxProb) {
  //       maxProb = prob;
  //       maxProbSample = sam;
  //     }
  //   }
  //   return maxProbSample.getObsIndex();
  // }


  // /*
  //  * Using the location probability using the MAP plugin estimate
  //  */
  // public double computeLocationProbabilityForSampleMAP(SamplerState s) {
  //   computeProbabilityForAllLocationsMAP(s);
  //   return probabilityForLocation.get(sample);   
  // }
  
}