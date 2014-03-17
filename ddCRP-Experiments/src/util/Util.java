package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import data.Data;
import model.CityTable;
import model.SamplerState;
import model.SamplerStateTracker;
import model.Theta;
import test.Test;



public class Util {

	public static final String cityNamesFile = "Data/cities.txt";
	public static final String venueCategoriesFile = "Data/venue_categories.txt";

	public static String outputDirectory;
	public static final String clustersOutCSVFilename = "clusters.tsv";
	public static final String topicsOutFilename = "top_words_per_topic.txt";
	public static String clustersOutCSVPath;
	public static String topicsOutPath;

	/**
	 * Set the value for outputDirectory, and update all the dependent paths, and create directory if
	 * it doesn't exist
	 */
	public static void setOutputDirectory(String dir) {
		outputDirectory = dir;

		// Create the directory if it doesn't exist
		File theDir = new File(outputDirectory);
		if (!theDir.exists())
		  theDir.mkdirs();  

		// Compute the new output paths given the outputDirectory
		if (outputDirectory.substring(outputDirectory.length() - 1) != "/")
			outputDirectory += "/";
		clustersOutCSVPath = topicsOutPath = outputDirectory;
		clustersOutCSVPath += clustersOutCSVFilename;
		topicsOutPath += topicsOutFilename;
	}

	/**
	 * Set the value for outputDirectory based on the command line arguments
	 */
	public static void setOutputDirectoryFromArgs(int numIter, double dirichletParam, double dDCRPSelfLink, double cRPSelfLink, Test test) {
		setOutputDirectory("results/" + test.getClass().getSimpleName() + "__" + test.getNumSamples() + "__" + numIter + "__" + dirichletParam + "__" + dDCRPSelfLink + "__" + cRPSelfLink);
	}

	/**
	 * Samples from a discrete distribution. The input is a list of probabilites (non negative and non zero)
	 * They need not sum to 1, the list will be normalized. 
	 * @param probs
	 * @return
	 */
	public static int sample(List<Double> probs)
	{
		ArrayList<Double> cumulative_probs = new ArrayList<Double>();
		Double sum_probs = new Double(0.0);
		for(Double prob:probs)
		{
			sum_probs = sum_probs + prob;
			cumulative_probs.add(sum_probs);
		}
		if(sum_probs!=1)		//normalizing
			for(int i=0;i<probs.size();i++)
			{
				probs.set(i, probs.get(i)/sum_probs);
				cumulative_probs.set(i, cumulative_probs.get(i)/sum_probs);
			}
		Random r  = new Random();
		Double nextRandom = r.nextDouble();
		for(int i=0;i<cumulative_probs.size();i++)		
			if(cumulative_probs.get(i)>nextRandom)			
				return i;
		
		return -1;		
	}
	
	/**
	 * Prints the table configuration for the current (last) state of the sampler for a given list index
	 * @param list_index
	 */
	public static void printTableConfiguration(int list_index, PrintStream out)
	{
		SamplerState s = SamplerStateTracker.returnCurrentSamplerState();
		int count  = 0;
		for(int table_id=0;table_id<s.getC().get(list_index).size();table_id++)
		{
      HashSet<Integer> customers = s.getCustomersAtTable(table_id, list_index);
			if(customers != null && customers.size() > 0)
			{
				count++;
				out.println("Table "+table_id+" Count "+customers.size()+" :\t"+customers);
			}
		}
		out.println("There are "+count+" occupied tables");
	}
	
	/**
	 * Utility method for outputting top words per topic to a file
	 */
	public static void outputTopKWordsPerTopic(Theta t, int k) {
		try {
			PrintStream out = new PrintStream(topicsOutPath);	
			t.printMostProbWordsPerTheta(k, out);
		} catch(FileNotFoundException ex) {
			ex.printStackTrace();
		} 
	}

	/**
	 * Utility method for generating an array of venue categories.
	 */
	public static ArrayList<String> getVenueCategories() {
		//read the venueCategories file
		ArrayList<String> venueCategories = new ArrayList<String>();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(venueCategoriesFile));
			String line;
			while((line = reader.readLine())!=null)
			 {
				venueCategories.add(line);
			 }
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return venueCategories;
	}

	/**
	 * Utility method for generating an array of venues.
	 */
	public static ArrayList<ArrayList<Venue>> getVenues() {
		//read the list of city names
		ArrayList<String> cityNames = new ArrayList<String>();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(cityNamesFile));			
			String line;
			while((line = reader.readLine())!=null) {
				if(line!=null) {
					String [] splits = line.split(",");
					if (splits[1] == "\"Washington")
						cityNames.add("\"Washington, D.C.\"");
					else
						cityNames.add(splits[1]);
				}
			}
			reader.close();
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		
		//read the venueCategories file
		ArrayList<String> venueCategories = getVenueCategories();

		//read the city_venue files for all cities
    ArrayList<ArrayList<Venue>> allVenues = new ArrayList<ArrayList<Venue>>(); 
    try
    {
      for(int i=1;i<=cityNames.size();i++)
      {
        allVenues.add(new ArrayList<Venue>()); //a new city
        BufferedReader reader = new BufferedReader(new FileReader("Data/cities_sim/city_"+i+"_venue_ids.txt"));
        String line;
        int j=0;
        while((line = reader.readLine())!=null)
        {
          String [] splits = line.split(",");
          Venue v = new Venue();
          v.setVenueName(splits[1]);
          v.setCityId(i-1);
          v.setObsId(j);
          v.setCityName(cityNames.get(i-1)); //i-1 because city index starts from 0
          v.setLat(Double.parseDouble(splits[splits.length-2]));
          v.setLon(Double.parseDouble(splits[splits.length-1]));
          //will put the category name later, because if the venue_name has ',', then the indices might be different and I can get the 
          //venue_cats later by mapping from the observations 
          allVenues.get(allVenues.size() -1).add(v);
          j++;
        }
      }
    }catch(FileNotFoundException ex){
      ex.printStackTrace();
    }catch(IOException ex){
      ex.printStackTrace();
    }

    return allVenues;
	}

	/**
	 * Utility method for generating the csv file for cities.
	 */
	public static void outputCSVforMap(SamplerState s)
	{		
		//read the city_venue files for all cities
		ArrayList<ArrayList<Venue>> allVenues = getVenues();
		ArrayList<ArrayList<Double>> allObservations = Data.getObservations(); // all observations
		
		ArrayList<String> venueCategories = getVenueCategories();

		HashSet<Integer> allTopicIds = s.getAllTopics();
		Iterator<Integer> iter = allTopicIds.iterator();
		PrintStream p = null;
		try {
			 p = new PrintStream(clustersOutCSVPath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(iter.hasNext()) //over all topic_ids
		{
			Integer topicId = iter.next();
			if(topicId != null)		
			{
				HashSet<CityTable> cityTables = s.getCityTablesForTopic(topicId);
				Iterator<CityTable> ctIter = cityTables.iterator();
				while(ctIter.hasNext()) //over all citytables in a topic
				{
					CityTable ct = ctIter.next();
					int cityId = ct.getCityId();
					int tableId = ct.getTableId();
					HashSet<Integer> customerIndices = s.getCustomersAtTable(tableId, cityId);
					ArrayList<Double> allObservationsInCity = allObservations.get(cityId);
					Iterator<Integer> venueIter = customerIndices.iterator();
					while(venueIter.hasNext()) //over venues at a table of a topic
					{
						Integer venueId = venueIter.next();
						if(venueId != null)
						{
							Venue v = allVenues.get(cityId).get(venueId);
							v.setTableId(tableId);
							v.setTopicId(topicId);							
							Double obs = allObservationsInCity.get(venueId);
							v.setVenueCategoryId(obs.intValue());
							v.setVenueCategory(venueCategories.get(obs.intValue()-1));							
							v.printVenueConfig(p);
						}
					}
				}
			}
		}
		p.close();
		
	}
	
	
	public static void outputCSVforMapDDCRP(SamplerState s)
	{		
		//read the city_venue files for all cities
		ArrayList<ArrayList<Venue>> allVenues = getVenues();
		ArrayList<ArrayList<Double>> allObservations = Data.getObservations(); // all observations
		
		ArrayList<String> venueCategories = getVenueCategories();

		HashSet<Integer> allTopicIds = s.getAllTopics();
		Iterator<Integer> iter = allTopicIds.iterator();
		PrintStream p = null;
		try {
			 p = new PrintStream(clustersOutCSVPath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(iter.hasNext()) //over all topic_ids
		{
			Integer topicId = iter.next();
			if(topicId != null)		
			{
				HashSet<CityTable> cityTables = s.getCityTablesForTopic(topicId);
				Iterator<CityTable> ctIter = cityTables.iterator();
				while(ctIter.hasNext()) //over all citytables in a topic
				{
					CityTable ct = ctIter.next();
					int cityId = ct.getCityId();
					int tableId = ct.getTableId();
					HashSet<Integer> customerIndices = s.getCustomersAtTable(tableId, cityId);
					ArrayList<Double> allObservationsInCity = allObservations.get(cityId);
					Iterator<Integer> venueIter = customerIndices.iterator();
					while(venueIter.hasNext()) //over venues at a table of a topic
					{
						Integer venueId = venueIter.next();
						if(venueId != null)
						{
							Venue v = allVenues.get(cityId).get(venueId);
							v.setTableId(tableId);
							v.setTopicId(topicId);							
							Double obs = allObservationsInCity.get(venueId);
							v.setVenueCategoryId(obs.intValue());
							v.setVenueCategory(venueCategories.get(obs.intValue()-1));							
							v.printVenueConfig(p);
						}
					}
				}
			}
		}
		p.close();
		
	}

	public static void outputCSVforMapDDCRPNew(SamplerState s)
	{		
		//read the city_venue files for all cities
		ArrayList<ArrayList<Venue>> allVenues = getVenues();
		ArrayList<ArrayList<Double>> allObservations = Data.getObservations(); // all observations
		
		ArrayList<String> venueCategories = getVenueCategories();

		ArrayList<HashSet<HashSet<Integer>>>tableSeatingSetList = s.getTableSeatingsSet();
		PrintStream p = null;
		try {
			 p = new PrintStream(clustersOutCSVPath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i=0;i<tableSeatingSetList.size();i++)
		{
			int cityId = i; //Question : cityId starts from 0 right?
			HashSet<HashSet<Integer>> tableSeatingSet = tableSeatingSetList.get(i);
			Iterator<HashSet<Integer>> tableIterator =  tableSeatingSet.iterator();
			int tableIdCounter = 0;
			while(tableIterator.hasNext()) //over tables of a city
			{
				HashSet<Integer> table = tableIterator.next();
				ArrayList<Double> allObservationsInCity = allObservations.get(cityId);
				Iterator<Integer> venueIter = table.iterator();
				while(venueIter.hasNext()) //over venues at a table of a city
				{
					Integer venueId = venueIter.next();
					if(venueId!=null)
					{
						Venue v = allVenues.get(cityId).get(venueId);
						v.setTableId(tableIdCounter); //this is not the actual table_id, but for mapping purposes, the same number should be ok.
						v.setTopicId(0);
						Double obs = allObservationsInCity.get(venueId);
						v.setVenueCategoryId(obs.intValue());
						v.setVenueCategory(venueCategories.get(obs.intValue()-1));							
						v.printVenueConfig(p);
					}
				}
				tableIdCounter++;
			}
		}
		p.close();
	}



}
