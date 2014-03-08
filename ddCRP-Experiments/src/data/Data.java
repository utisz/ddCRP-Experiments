package data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.la4j.matrix.sparse.CRSMatrix;

public class Data {
	
	
	private static final String corpusFile = "Data/corpus_large_cities.txt";
	private static final String distanceFile = "Data/distance_large_cities.txt";
	
	/**
	 * This is a list of list of observations. Each list represent a line in the corpus file.
	 * TO DISCUSS: Should this be List<Double?>
	 */
	private static ArrayList<ArrayList<Double>> list_observations ;
	
	/**
	 * A list of sparse matrix, each for storing the distance
	 */
	private static ArrayList<CRSMatrix> distanceMatrices ;
	
	/**
	 * Method to populate the observations from the corpus.
	 */
	private static void populateObservationsList()
	{
		try
		{
			list_observations = new ArrayList<ArrayList<Double>>();
			//read the corpus file
			int num_obs = 0;
			BufferedReader reader = new BufferedReader(new FileReader(corpusFile));
			String line;
			// while((line = reader.readLine())!=null) //each line represents the observations a city or a document etc....
			for (int k=0; k<3; k++)  // just take the first few cities for testing
			{
				line = reader.readLine();				
				String[] observations = line.split(" ");
				ArrayList<Double> list = new ArrayList<Double>();
				for(int i=0;i<observations.length;i++)
				{
					Double d = Double.parseDouble(observations[i]);
					list.add(d);				
				}
				list_observations.add(list);
				num_obs = num_obs + observations.length;
			}			
			reader.close();			
			System.out.println("Number of data points are "+num_obs);
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * Method to populate the distance matrices from the distance file.
	 */
	private static void populateDistanceMatrices()
	{
		try
		{
			distanceMatrices = new ArrayList<CRSMatrix>();
			//read the distance file
			BufferedReader reader = new BufferedReader(new FileReader(distanceFile));
			String line;					
			while((line = reader.readLine())!=null) //each line represents a city or a document etc....
			{			
				String[] eachDistances = line.split(" ");
				//get the number of total observations in each line
				String[] last_obs_pair =  eachDistances[eachDistances.length-1].split(":");
				int numObservations = Integer.parseInt(last_obs_pair[0]);
				//create a sparse matrix for this line
				//TO Discuss: Should i create an (n+1 * n+1) matrix to keep the notation a[i][j] constant
				CRSMatrix dist_mat = new CRSMatrix(numObservations,numObservations); //create a square matrix. This is a Compressed Row Storage sparse matrix.
				//Fill up the distance matrix
				for(String obs:eachDistances)
				{
					String[] obs_pairs = obs.split(":");
					int first_id = Integer.parseInt(obs_pairs[0]);
					int second_id = Integer.parseInt(obs_pairs[1]);
					Double distance = Double.parseDouble(obs_pairs[2]);
					dist_mat.set(first_id-1,second_id-1, distance);					
				}
				distanceMatrices.add(dist_mat);
			}	
			reader.close();
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	/**
	 * Populates the observations from the corpus (if not already done) and returns the list_observations
	 * @return
	 */
	public static ArrayList<ArrayList<Double>> getObservations()
	{
		if(list_observations == null)
			populateObservationsList();
		return list_observations;
	}
	
	/**
	 * Populates the distance matrices from the corpus (if not already done) and returns the list of matrices
	 * @return
	 */
	public static ArrayList<CRSMatrix> getDistanceMatrices()
	{
		if(distanceMatrices==null)
			populateDistanceMatrices();
		return distanceMatrices;
	}
}
