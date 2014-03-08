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
public interface LocationPredictor {
 
  public double computeLocationProbabilityForSample();
  
  public void computeProbabilityForAllLocations();
 
  /*
   * @return the tableId of the sample with the max probability at the correct category
   */ 
  public int predictMaxProbForLocations();
}