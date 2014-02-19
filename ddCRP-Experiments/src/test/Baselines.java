package test;

import java.util.ArrayList;
import java.util.HashMap;

import data.Data;

public class Baselines {
	
	/**
	 * Computes MLE of the multinomial distribution.
	 * @param numCities - number of cities, we are computing for
	 */
	public static HashMap<Double,Double> multinomialAcrossAllCities()
	{
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
		
		//Total number of observations
		int numObs = 0;
		for(int i=0;i<list_observations.size();i++)
			numObs = numObs + list_observations.get(i).size();
		
		HashMap<Double,Double> observationCounts = new HashMap<Double,Double>();
		for(int i=0;i<list_observations.size();i++)
		{
			ArrayList<Double> observations = list_observations.get(i);
			for(Double obs:observations)
			{
				obs = obs - 1 ; //index shift
				if(observationCounts.get(obs) == null) //new category			
					observationCounts.put(obs, 1/(double)numObs);
				else
					observationCounts.put(obs, observationCounts.get(obs) + 1/(double)numObs );
			}
		}
		return observationCounts;
	}
	
	/**
	 * Multinomial params for each city.
	 * @return
	 */
	public static ArrayList<HashMap<Double,Double>> multinomialForEachCity()
	{
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();
		ArrayList<HashMap<Double,Double>> observationCountsList = new ArrayList<HashMap<Double,Double>>();
		
		for(int i=0;i<list_observations.size();i++)
		{
			ArrayList<Double> observations = list_observations.get(i);
			HashMap<Double,Double> observationCounts = new HashMap<Double,Double>();
			int numObs = observations.size();
			for(Double obs:observations)
			{
				obs = obs - 1 ; //index shift
				if(observationCounts.get(obs) == null) //new category			
					observationCounts.put(obs, 1/(double)numObs);
				else
					observationCounts.put(obs, observationCounts.get(obs) + 1/(double)numObs );
				
			}
			observationCountsList.add(observationCounts);
		}
		return observationCountsList;
	}
	

}
