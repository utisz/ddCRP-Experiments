package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import model.SamplerStateTracker;

import org.la4j.matrix.sparse.CRSMatrix;

import data.Data;

/**
 * This class is for splitting the data into test and train data.
 * @author rajarshd
 *
 */
public class TestUniform extends Test {
  
  /**
   * Constructor
   */
  public TestUniform(int n) {
    super(n);
  }

  /**
   * Generate samples by for each city, sample numSamples unifomrly from all venues 
   * in the city
   */
  public void generateTestSamples() {
    
    ArrayList<ArrayList<Double>> allObservations = Data.getObservations();  // In the general case, we may not want this in memory
    ArrayList<CRSMatrix> distanceMatrices = Data.getDistanceMatrices();
    
    Random r = new Random();
    for(int i=0;i<allObservations.size();i++) //for each city, generate 100 random points which are to be taken out
    {
      ArrayList<TestSample> cityTestSamples = new ArrayList<TestSample>();
      HashMap<Integer,Integer> cityVenueIds = new HashMap<Integer,Integer>(); //reason for storing in a map is to decrease the lookup time
      for(int count=0; count<numSamples; count++)
      {
        int venueIndex = r.nextInt(allObservations.get(i).size()); //venue index to be removed as test data     
        TestSample t = new TestSample(i, venueIndex, allObservations.get(i).get(venueIndex)); //1st arg: venue_index within a city; 2nd arg: city index, 3rd arg: venue category
        cityTestSamples.add(t);
        testSamplesSet.add(t);        
        //Now add to the map of test venues
        cityVenueIds.put(venueIndex, 0); //the key is the venue_id, the value is useless
      }
      testSamples.add(cityTestSamples); //all the test samples for a city
      testVenueIds.add(cityVenueIds);
    }

  }

  /**
   * Output the description of this test
   */
  public String toString() {
    String out = "Test Name: TestUniform\n";
    out += "Test Description: For each city, sample numSamples venues uniformly from all venues in the city\n";
    out += "numSamples: " + testSamples.size() + "\n";
    out += "Samples: \n";
    for (int i=0; i<testSamples.size(); i++) {
      ArrayList<TestSample> citySamples = testSamples.get(i);
      for (int j=0; j<citySamples.size(); j++) { 
        TestSample s = citySamples.get(j);
        out += Integer.toString(s.getListIndex()) + ":";
        out += Integer.toString(s.getObsIndex()) + ":";
        out += Double.toString(s.getObsCategory()) + " ";
      }
    }
    return out;
  }

}
