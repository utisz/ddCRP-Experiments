package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

import data.Data;

public class Baselines {
	
	HashSet<TestSample> testSamples;
	HashMap<Double,Double> multinomailAcrossAllCities;
	ArrayList<HashMap<Double,Double>> multinomialForEachCity;

	public Baselines(HashSet<TestSample> testSamples) {
		this.testSamples = testSamples;
	}

	public void fitMultinomialAcrossAllCities() {
		this.multinomailAcrossAllCities = multinomialAcrossAllCities();
	}

	public void fitMultinomialForEachCity() {
		this.multinomialForEachCity = multinomialForEachCity();
	}

	public double predictMultProbAcrossAllCities(TestSample s) {
    int listIndex = s.getListIndex();
    int obsIndex = s.getObsIndex();
    ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
    double obs = list_observations.get(listIndex).get(obsIndex) - 1;
    Double prob = multinomailAcrossAllCities.get(obs);
    if (prob == null)
    	prob = 0.0;
    return prob;
	}

	public double predictMultProbForEachCity(TestSample s) {
    int listIndex = s.getListIndex();
    int obsIndex = s.getObsIndex();
    ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
    double obs = list_observations.get(listIndex).get(obsIndex) - 1;
    Double prob = multinomialForEachCity.get(listIndex).get(obs);
    if (prob == null)
    	prob = 0.0;
    return prob;
	}

	public int inTopTenMultProbForEachCity(TestSample s) {
    int listIndex = s.getListIndex();
    int obsIndex = s.getObsIndex();
    ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
    double obs = list_observations.get(listIndex).get(obsIndex) - 1;
    Double probAtSample = multinomialForEachCity.get(listIndex).get(obs);
    if (probAtSample == null)
    	probAtSample = 0.0;

    // have to convert MutinomailForEachCity into an ArrayList
    ArrayList<Double> probs = new ArrayList<Double>();
    for (int i=0; i<419; i++) {  // TODO vocab size is hard coded here, fix
    	Double prob = multinomialForEachCity.get(listIndex).get((double)i);
      if (prob == null)
    		prob = 0.0;
    	probs.add(prob);
    }

    ArrayList<Double> sortedProbabilityForObservation = new ArrayList<Double>(probs);
    Collections.sort(sortedProbabilityForObservation);
    Collections.reverse(sortedProbabilityForObservation);
    for (int i=0; i<10; i++) {
      if (probAtSample >= sortedProbabilityForObservation.get(i))
        return 1;
    }
    return 0;
	}

	public int inTopTenMultProbAcrossAllCities(TestSample s) {
    int listIndex = s.getListIndex();
    int obsIndex = s.getObsIndex();
    ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
    double obs = list_observations.get(listIndex).get(obsIndex) - 1;
    Double probAtSample = multinomailAcrossAllCities.get(obs);
    if (probAtSample == null)
    	probAtSample = 0.0;

    // have to convert MutinomailForEachCity into an ArrayList
    ArrayList<Double> probs = new ArrayList<Double>();
    for (int i=0; i<419; i++) {  // TODO vocab size is hard coded here, fix
    	Double prob = multinomailAcrossAllCities.get((double)i);
      if (prob == null)
    		prob = 0.0;
    	probs.add(prob);
    }

    ArrayList<Double> sortedProbabilityForObservation = new ArrayList<Double>(probs);
    Collections.sort(sortedProbabilityForObservation);
    Collections.reverse(sortedProbabilityForObservation);
    for (int i=0; i<10; i++) {
      if (probAtSample >= sortedProbabilityForObservation.get(i))
        return 1;
    }
    return 0;		
	}

	/**
	 * Computes MLE of the multinomial distribution.
	 * @param numCities - number of cities, we are computing for
	 */
	public HashMap<Double,Double> multinomialAcrossAllCities()
	{
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
		
		//Total number of observations
		int numObs = 0;
		for(int i=0; i<list_observations.size(); i++) {
			ArrayList<Double> observations = list_observations.get(i);
			for(int j=0; j<observations.size(); j++)
			{
				TestSample ts = new TestSample(i, j, -1);
				if (!testSamples.contains(ts)) 
					numObs += 1;
			}
		}

		HashMap<Double,Double> observationCounts = new HashMap<Double,Double>();
		for(int i=0;i<list_observations.size();i++)
		{
			ArrayList<Double> observations = list_observations.get(i);
			for(int j=0; j<observations.size(); j++)
			{
				TestSample ts = new TestSample(i, j, -1);
				if (!testSamples.contains(ts)) {
					Double obs = observations.get(j);
					obs = obs - 1 ; //index shift
					if(observationCounts.get(obs) == null) //new category			
						observationCounts.put(obs, 1/(double)numObs);
					else
						observationCounts.put(obs, observationCounts.get(obs) + 1/(double)numObs );
				}
			}
		}
		return observationCounts;
	}
	

	/**
	 * Multinomial params for each city.
	 * @return
	 */
	public ArrayList<HashMap<Double,Double>> multinomialForEachCity()
	{
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
		ArrayList<HashMap<Double,Double>> observationCountsList = new ArrayList<HashMap<Double,Double>>();
		
		for(int i=0;i<list_observations.size();i++)
		{
			ArrayList<Double> observations = list_observations.get(i);
			HashMap<Double,Double> observationCounts = new HashMap<Double,Double>();
			int numObs = 0;
			for(int j=0; j<observations.size(); j++)
			{
				TestSample ts = new TestSample(i, j, -1);
				if (!testSamples.contains(ts)) 
					numObs += 1;
			}

			for(int j=0; j<observations.size(); j++)
			{
				TestSample ts = new TestSample(i, j, -1);
				if (!testSamples.contains(ts)) {		
					Double obs = observations.get(j);						
					obs = obs - 1 ; //index shift
					if(observationCounts.get(obs) == null) //new category			
						observationCounts.put(obs, 1/(double)numObs);
					else
						observationCounts.put(obs, observationCounts.get(obs) + 1/(double)numObs );
				}				
			}
			observationCountsList.add(observationCounts);
		}
		return observationCountsList;
	}
	

}
