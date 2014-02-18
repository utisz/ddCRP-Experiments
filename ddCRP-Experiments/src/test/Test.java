package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import model.SamplerStateTracker;

import org.la4j.matrix.sparse.CRSMatrix;

/**
 * An abstract class for implementing a test set from the data.
 * Each test might have different methods for sampling the data,
 * and so would implement their own generateTestSamples methods.
 *
 * @author Justin Cranshaw
 *
 */
abstract public class Test {
  
  protected ArrayList<ArrayList<TestSample>> testSamples = new ArrayList<ArrayList<TestSample>>();
  protected ArrayList<HashMap<Integer,Integer>> testVenueIds = new ArrayList<HashMap<Integer,Integer>>();  //list of maps, each for a city
  protected HashSet<TestSample> testSamplesSet = new HashSet<TestSample>();
  protected int numSamples;

  /**
   * Base class constructor
   */
  protected Test(int n) {
    numSamples = n;
  }

  public int getNumSamples() {
    return numSamples;
  }

  /**
   * Returns all the test samples for all cities
   * @return
   */
  public ArrayList<ArrayList<TestSample>> getTestSamples() {
    return testSamples;
  }

  /**
   * Returns all the test samples for all cities as a flat set
   * @return
   */
  public HashSet<TestSample> getTestSamplesSet() {
    return testSamplesSet;
  }
  
  /**
   * Retrns a list of @see TestSample for a city (the test venues for a city)
   * @param cityIndex
   * @return
   */
  public ArrayList<TestSample> getTestSamplesForCity(int cityIndex) {
    return testSamples.get(cityIndex);
  }

  /**
   * Returns a map of the venue_ida which are in the test set
   * @param cityIndex
   * @return
   */
  public HashMap<Integer, Integer> getTestVenueIdsForCity(int cityIndex) {
    return testVenueIds.get(cityIndex);
  }

  /**
   * ABSTRACT METHODS
   */

  /**
   * Returns a description of the test as a string
   * @return
   */
  abstract public String toString(); 


  /**
   * This method generates numSamples random test venue id's for each city and populates a map  
   */
  abstract public void generateTestSamples(); 

}
