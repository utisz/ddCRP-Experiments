package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import model.SamplerStateTracker;

import org.la4j.matrix.sparse.CRSMatrix;

import data.Data;
import util.Util;
import util.Venue;

/**
 * This class is for splitting the data into test and train data.
 * @author rajarshd
 *
 */
public class TestUniformCategory extends Test {
  
  /**
   * Constructor
   */
  public TestUniformCategory(int n) {
    super(n);
  }

  /**
   * Generate samples by for each city, sample numSamples unifomrly from all venues 
   * in the city
   */
  public void generateTestSamples() {
    
    ArrayList<ArrayList<Double>> allObservations = Data.getObservations();  // In the general case, we may not want this in memory
    ArrayList<CRSMatrix> distanceMatrices = Data.getDistanceMatrices();
    ArrayList<ArrayList<Venue>> venues = Util.getVenues();

    Random r = new Random();
    for(int i=0;i<allObservations.size();i++) //for each city, generate 100 random points which are to be taken out
    {
      ArrayList<Venue> cityVenues = venues.get(i);

      // build a hash from category Id to an list of venues of that category for each venue ()
      HashMap<Integer, ArrayList<Venue>> cityVenuesHash = new HashMap<Integer, ArrayList<Venue>>();
      for (Venue v : cityVenues) {
        int cat = v.getVenueCategoryId();
        if (cityVenuesHash.get(cat) == null)
          cityVenuesHash.put(cat, new ArrayList<Venue>());
        ArrayList<Venue> venuesWithCat = cityVenuesHash.get(cat);
        venuesWithCat.add(v);
        cityVenuesHash.put(cat, venuesWithCat);
      }
      // put the grid key strings in an arrayList to access later
      ArrayList<Integer> cats = new ArrayList<Integer>(cityVenuesHash.keySet());

      ArrayList<TestSample> cityTestSamples = new ArrayList<TestSample>();
      HashMap<Integer,Integer> cityVenueIds = new HashMap<Integer,Integer>(); //reason for storing in a map is to decrease the lookup time
      for(int count=0; count<numSamples; count++)
      {
        int catIndex = r.nextInt(cats.size()); // index into gridKeys, to the grid to be sampled from
        Integer cat = cats.get(catIndex);
        ArrayList<Venue> catVenues = cityVenuesHash.get(cat);
        int venueSampleIndex = r.nextInt(catVenues.size());
        Venue venueSample = catVenues.get(venueSampleIndex);
        int venueIndex = venueSample.getObsId();
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
    String out = "Test Name: TestUniformSpace\n";
    out += "Test Description: For each city, sample numSamples venues were approximately uniformly drawn across space in the city\n";
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
