package model;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.la4j.matrix.sparse.CRSMatrix;

import data.Data;

import Likelihood.Likelihood;
import Likelihood.DirichletLikelihood;

import test.TestSample;

/**
 * This class is for storing the state of the sampler for an iteration.
 * An object of this class represent the state of the sampler at one iteration.
 * @author rajarshd
 */
public class SamplerState {

	/**
	 * The number of data instances.
	 */
	private static Long num_data;
	
	/**
	 * This stores the customer link for each data point. Each list represents a city/document etc.
	 */
	private ArrayList<ArrayList<Integer>> c;
	
	/**
	 * This stores the table assignment for each data point.Each list represents a city/document etc.
	 */
	private ArrayList<ArrayList<Integer>> t;
	
	/**
	 * The number of occupied tables at this iteration
	 */
	private Long T;
	
	/**
	 * The total number of topics
	 */
	private Long K;
	
	/**
	 * The max topicId till now in this iteration 
	 */
	private int maxTopicId;
	
	/**
	 * This stores the topic assignments for each data point (which is basically the topic assignment at the given table they are sitting at)
	 */
	private ArrayList<ArrayList<Integer>> k_c = new ArrayList<ArrayList<Integer>>();
	
	/**
	 * This stores the topic assignments for each table;
	 */
	//private ArrayList<ArrayList<Long>> k_t;
	
	/**
	 * This stores the number of tables(clusters) assigned to each topic.
	 */
	private HashMap<Integer,Integer> m;
	
	/**
	 * Map of table and the customer_ids.
	 * ArrayList is over documents (cities), hashmap keys are table ids, hasmap values are a hashset of customer ids
	 */
	private ArrayList<HashMap<Integer,HashSet<Integer>>> customersAtTableList;

  /**
   * This is a map of topics to lists, where each list represents a city and within each list, we have a map of table_id to the set of customer indexes sitting at the table   
   */
  // public static HashMap<Integer,ArrayList<HashMap<Integer,HashSet<Integer>>>> topic_members_by_city_by_table = new HashMap<Integer,ArrayList<HashMap<Integer,HashSet<Integer>>>>();
  
  /**
   * This is a map of topic IDs assigned to a CityTable.
   */	 
	private HashMap<CityTable, Integer> topicAtTable = new HashMap<CityTable, Integer>();

  /**
   * For a given topic ID, this maps to the set of CityTables assigned to that topic
   */	 
	private HashMap<Integer, HashSet<CityTable>> tablesAssignedToTopic = new HashMap<Integer, HashSet<CityTable>>();

	/**
	 * For each sampled latent variable of this state, sum up the prior component
	 * of the variable 
	 */
	private double sumOfLogPriors = Double.NEGATIVE_INFINITY;

	/**
	 * A HashSet of test samples 
	 */
  protected HashSet<TestSample> testSamples = new HashSet<TestSample>();


	/**
	 * 
	 * Getters and Setters
	 * 
	 */

  public HashSet<TestSample> getTestSamples() {
    return testSamples;
  }

  public void setTestSamples(HashSet<TestSample> testSamples) {
    this.testSamples = testSamples;
  }

	public double getSumOfLogPriors() {
		return sumOfLogPriors;
	}

	public void setSumOfLogPriors(double sumOfLogPriors) {
		this.sumOfLogPriors = sumOfLogPriors;
	}
	
	public HashMap<CityTable, Integer> getTopicAtTable() {
		return topicAtTable;
	}

	public void setTopicAtTable(HashMap<CityTable, Integer> topicAtTable) {
		this.topicAtTable = topicAtTable;
	}

	public HashMap<Integer, HashSet<CityTable>> getTablesAssignedToTopic() {
		return tablesAssignedToTopic;
	}

	public void setTablesAssignedToTopic(HashMap<Integer, HashSet<CityTable>> tablesAssignedToTopic) {
		this.tablesAssignedToTopic = tablesAssignedToTopic;
	}

	public static Long getNum_data() {
		return num_data;
	}

	public static void setNum_data(Long num_data) {
		SamplerState.num_data = num_data;
	}


	public Long getT() {
		return T;
	}

	public void setT(Long t) {
		T = t;
	}

	public Long getK() {
		return K;
	}

	public void setK(Long k) {
		K = k;
	}
	
	public int getMaxTopicId() {
		return maxTopicId;
	}
	
	public void setMaxTopicId(int maxTopicId) {
		this.maxTopicId = maxTopicId;
	}
	public HashMap<Integer,Integer> getM() {
		return m;
	}
	
	public void setM(HashMap<Integer,Integer> m) {
		this.m = m;
	}

	public ArrayList<ArrayList<Integer>> getC() {
		return c;
	}
	
	/**
	 * Returns the customer link of a particular customer given the list and the customer indexes
	 * @param customer_index
	 * @param city_index
	 * @return
	 */
	public int getC(int customer_index,int city_index)
	{
		return c.get(city_index).get(customer_index);
	}

	public void setC(ArrayList<ArrayList<Integer>> c) {
		this.c = c;
	}
	
	/**
	 * Sets the new customer assignment for a customer
	 * @param cust_assignment
	 * @param customer_index
	 * @param city_index
	 */
	public void setC(Integer cust_assignment, int customer_index,int city_index)
	{
		c.get(city_index).set(customer_index, cust_assignment);
	}

	public ArrayList<ArrayList<Integer>> getK_c() {
		return k_c;
	}

	public void setK_c(ArrayList<ArrayList<Integer>> k_c) {
		this.k_c = k_c;
	}

	/**public ArrayList<ArrayList<Long>> getK_t() {
		return k_t;
	}

	public void setK_t(ArrayList<ArrayList<Long>> k_t) {
		this.k_t = k_t;
	}**/

	public ArrayList<ArrayList<Integer>> get_t()
	{
		return t;
	}
	
	/**
	 * Returns the table assignment for a specific customer given the list and the customer index
	 * @param customer_index
	 * @param city_index
	 * @return
	 */
	public int get_t(int customer_index,int city_index)
	{
		return t.get(city_index).get(customer_index);
	}
	public void set_t(ArrayList<ArrayList<Integer>> t) {
		this.t = t;
	}
	/**
	 * Sets the new table assignment for a customer
	 * @param table_assignment
	 * @param customer_index
	 * @param city_index
	 */
	public void set_t(Integer table_assignment, int customer_index,int city_index)
	{
		t.get(city_index).set(customer_index, table_assignment);
	}


	public ArrayList<HashMap<Integer, HashSet<Integer>>> getCustomersAtTableList() {
		return customersAtTableList;
	}


	/**
	 * returns the set of customer indices sitting at a table in a given list
	 * @param table_id
	 * @param list_index
	 * @return
	 */
	public HashSet<Integer> getCustomersAtTable(int tableId,int listIndex)
	{		
		if(customersAtTableList.get(listIndex).get(tableId) == null)
			return null;
		return customersAtTableList.get(listIndex).get(tableId);
	}
	/**
	 * returns the set of onservations (NOT indices) of customers at a table in a city. Null if the table is empty or table doesn't exist.
	 * @param tableId
	 * @param listIndex
	 * @return
	 */
	public ArrayList<Double> getObservationsFromTable(int tableId, int listIndex)
	{
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations(); // all observations
		ArrayList<Double> observations_per_city = list_observations.get(listIndex);
		HashSet<Integer> customersAtTable = getCustomersAtTable(tableId, listIndex);
		ArrayList<Double> observationsFromTable = new ArrayList<Double>();
		if(customersAtTable!=null)
		{
			Iterator<Integer> iter = customersAtTable.iterator();
			while(iter.hasNext())
			{
				int customerIndex =  iter.next();
				TestSample ts = new TestSample(listIndex, customerIndex, -1);
				if (!testSamples.contains(ts)) {
					Double observation = observations_per_city.get(customerIndex);
					observationsFromTable.add(observation);
				} else {
					// System.out.println("removing test sample");
				}
			}
			if(observationsFromTable.size()==0)
				return null;
			else
				return observationsFromTable;
		}
		return null;
	}

	/**
	 * Returns the observations (NOT the indices) of the customers sitting at a table of a given city
	 * @param tableId
	 * @param listIndex
	 * @return
	 */
	public ArrayList<Double> getObservationAtTable(int tableId,int listIndex)
	{		
		if(customersAtTableList.get(listIndex).get(tableId) == null)
			return null;
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations(); // all observations
		ArrayList<Double> observations_per_city = list_observations.get(listIndex);
		ArrayList<Double> observationsAtTable = new ArrayList<Double>();
		HashSet<Integer> customer_indices =  customersAtTableList.get(listIndex).get(tableId);
		Iterator<Integer> iter = customer_indices.iterator();
		while(iter.hasNext())
		{
			int customer_index = iter.next();
			TestSample ts = new TestSample(listIndex, customer_index, -1);
			if (!testSamples.contains(ts)) {
				Double observation = observations_per_city.get(customer_index);
				observationsAtTable.add(observation);
			} else {
					// System.out.println("removing test sample");
			}
		}
		return observationsAtTable;
	}

	

	/**
	 * Sets the customers sitting at a table, given the indexes and the table number
	 * @param s
	 * @param table_id
	 * @param list_index
	 */
	public void setCustomersAtTable(HashSet<Integer> customers, int tableId, int listIndex)
	{
		customersAtTableList.get(listIndex).put(tableId, customers);
	}

	public void setCustomersAtTableList(
			ArrayList<HashMap<Integer, HashSet<Integer>>> customersAtTableList) {
		this.customersAtTableList = customersAtTableList;
	}
	

	/**
	 * Returns a new sampler state which is identical to the given sampler state.
	 * @return
	 */
	public SamplerState copy()
	{
		SamplerState s = new SamplerState();
		ArrayList<ArrayList<Integer>> new_c = new ArrayList<ArrayList<Integer>>(); //customer assignments
		ArrayList<ArrayList<Integer>> new_t = new ArrayList<ArrayList<Integer>>(); //table assignments per customer
		ArrayList<HashMap<Integer, HashSet<Integer>>> newCustomersAtTableList = new ArrayList<HashMap<Integer, HashSet<Integer>>>();  

		for(int i=0; i<c.size(); i++)
		{
			ArrayList<Integer> customer_assignments_copy = new ArrayList<Integer>(c.get(i)); //this will create a new list pointing to the same long objects, but its ok since Long is immutable.
			new_c.add(customer_assignments_copy);
			ArrayList<Integer> table_assignments_copy = new ArrayList<Integer>(t.get(i));
			new_t.add(table_assignments_copy);			
		}
		for(int i=0; i<customersAtTableList.size(); i++)
		{
			HashMap<Integer,HashSet<Integer>> customersAtTableOld = customersAtTableList.get(i);
			HashMap<Integer,HashSet<Integer>> customersAtTableCopy = new HashMap<Integer,HashSet<Integer>>();
			for (Entry<Integer, HashSet<Integer>> entry : customersAtTableOld.entrySet()) {
	    	Integer key = new Integer(entry.getKey());
	    	HashSet<Integer> oldValue = entry.getValue();
	    	HashSet<Integer> newValue = null;
	    	if (oldValue != null)
		    	newValue = new HashSet<Integer>(oldValue);

	    	customersAtTableCopy.put(key, newValue);
			}
			newCustomersAtTableList.add(customersAtTableCopy);
		}
		s.c = new_c;
		s.t = new_t;
		s.T = new Long(T);
		s.customersAtTableList = newCustomersAtTableList;

		// topics
		HashMap<Integer,Integer> newM = new HashMap<Integer,Integer>();

		for (Integer key : m.keySet()) {
    	newM.put(key, m.get(key));
		}

		s.K = new Long(K);
		s.m = newM;
		s.maxTopicId = maxTopicId;

		// copy the topic at table structure
		HashMap<CityTable, Integer> newTopicAtTable = new HashMap<CityTable, Integer>();
		for (Entry<CityTable, Integer> entry : topicAtTable.entrySet()) {
	    CityTable key = new CityTable(entry.getKey());
	    Integer value = new Integer(entry.getValue());
	    newTopicAtTable.put(key, value);
		}
		s.topicAtTable = newTopicAtTable;

 		HashMap<Integer, HashSet<CityTable>> newTablesAssignedToTopic = new HashMap<Integer, HashSet<CityTable>>();
 		for (Entry<Integer, HashSet<CityTable>> entry : tablesAssignedToTopic.entrySet()) {
	    Integer key = new Integer(entry.getKey());
	    HashSet<CityTable> oldValue = new HashSet<CityTable>(entry.getValue());
	    HashSet<CityTable> newValue = new HashSet<CityTable>();
	    for (CityTable ct : oldValue) {
	    	newValue.add(new CityTable(ct));
	    }
	    newTablesAssignedToTopic.put(key, newValue);
 		}
  	s.tablesAssignedToTopic = newTablesAssignedToTopic;

		return s;
	}

	/**
	 * Prints the object state
	 */
	public void prettyPrint(PrintStream out)
	{
		out.println("Total number of observations are " + SamplerState.num_data);
		out.println("Total number of documents: " + c.size());
		out.println("Total number of tables are " + T);
		out.println("Total number of topics " + K);
		out.println("Average number of tables per topic: " + T / (double) K);
		out.println("Number of tables per topic: " + m);
	}
	
	/**
	 * Returns for each city, a set of sets of customers sitting at each table
	 */
	public ArrayList<HashSet<HashSet<Integer>>> getTableSeatingsSet() {
		ArrayList<HashSet<HashSet<Integer>>> tableSeatings = new ArrayList<HashSet<HashSet<Integer>>>();
		for (ArrayList<Integer> cityTables : t) {
			HashMap<Integer, HashSet<Integer>> tableMembers = new HashMap<Integer, HashSet<Integer>>();
			for (int i=0; i<cityTables.size(); i++) {
				Integer tab = cityTables.get(i);
				// check if the table is empty
				if (tableMembers.get(tab) == null)
					tableMembers.put(tab, new HashSet<Integer>());
				HashSet<Integer> tableTabMembers = tableMembers.get(tab);
				tableTabMembers.add(i);
				tableMembers.put(tab, tableTabMembers);
			}
			// Now look over the hash, and put members in a set
			HashSet<HashSet<Integer>> cityTableSeatings = new HashSet<HashSet<Integer>>();
			for (HashSet<Integer> value : tableMembers.values()) {
				cityTableSeatings.add(value);
			}
			tableSeatings.add(cityTableSeatings);
		}
		return tableSeatings;
	}

	/**
	 * Return a number from 0 to 1 giving the Jiccard similarity between
	 * tables in s and tables in this.
	 */
	public double tableJiccardSimilarity(SamplerState s) {
		ArrayList<HashSet<HashSet<Integer>>> seatingsA = getTableSeatingsSet();
		ArrayList<HashSet<HashSet<Integer>>> seatingsB = s.getTableSeatingsSet();
		int sizeUnion = 0;
		int sizeIntersection = 0;

		for (int i=0; i<seatingsA.size(); i++) {
			HashMap<HashSet<Integer>,Integer> counts = new HashMap<HashSet<Integer>,Integer>();
			HashSet<HashSet<Integer>> citySeatingsA = seatingsA.get(i);
			HashSet<HashSet<Integer>> citySeatingsB = seatingsB.get(i);
			for (HashSet<Integer> t : citySeatingsA) {
				if (counts.get(t) == null)
					counts.put(t, 0);
				counts.put(t, counts.get(t)+1);
			}
			for (HashSet<Integer> t : citySeatingsB) {
				if (counts.get(t) == null)
					counts.put(t, 0);
				counts.put(t, counts.get(t)+1);
			}
			int citySizeUnion = counts.keySet().size();
			int citySizeIntersection = 0;
			// any key in counts that has a value of 2 is in the intersection
			for (int count : counts.values()) {
				if (count == 2) {
					citySizeIntersection += 1;
				}
				if (count > 2) {
					// something is wrong if this happens.
					System.out.println("More than 2 in the intersection!");
					System.out.println("  "+count);
				}
			}
			sizeUnion += citySizeUnion;
			sizeIntersection += citySizeIntersection;
		}

		return sizeIntersection / (double) sizeUnion;
	}

	/**
	 * equals comparator for SamplerStates, just checks the customer assignments and topic assignments
	 */
	@Override
	public boolean equals(Object obj) 
	{
		if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof SamplerState))return false;	
		SamplerState s = (SamplerState) obj;
		// return (this.c.equals(s.getC()) && this.k_c.equals(s.getK_c()));
		// return c.equals(s.getC());  // need to take into account k_c.  for now its buggy because of null.
		return getTableSeatingsSet().equals(s.getTableSeatingsSet());
	}

	/**
	 * Overrids hashCode based on c and k_c.  This concatenates the individual hashCodes as strings,
	 * then computes the hashCode of the resulting unique String.
	 */
	@Override
	public int hashCode() {
		// String s = String.valueOf(this.c.hashCode()) + ":" + String.valueOf(this.k_c.hashCode());
		// return c.hashCode();  // need to take into account k_c too.  for now its buggy because of null.
		return getTableSeatingsSet().hashCode();
	}
	
	/**
	 * returns the assigned topic for a city table
	 * @param ct
	 * @return
	 */
	public Integer getTopicForCityTable(CityTable ct)
	{
		return topicAtTable.get(ct);
	}
	
	/**
	 * increment the number of tables for topic. If new topic make a new entry in the table
	 * @param topic
	 */
	public void addTableCountsForTopic(int topic)
	{
		Integer countTables = m.get(topic);
		if(countTables == null) //new topic
			m.put(topic, 1);
		else
			m.put(topic, countTables+1);
	}
	
	/**
	 * decrease table count for a topic, if the count reduces to 0, then remove the topic
	 * @param topic
	 */
	public void decreaseTableCountsForTopic(Integer topic)
	{
		Integer countTables = m.get(topic);
		countTables = countTables - 1;
		if(countTables == 0) //remove the topic
		{
			m.remove(topic);
			K = K - 1; //decreasing the number of total topics
		}
		else
			m.put(topic, countTables);
	}
	
	/**
	 * Removes the corresponding city table entry from tablesAssignedToTopic t, if the topic has no tables
	 * across all cities, then remove the topic from the map.
	 * @param topic
	 */
	public void removeTableFromTopic(int topic, CityTable ct)
	{
		HashSet<CityTable> tables = tablesAssignedToTopic.get(topic);
		tables.remove(ct);
		if(tables.size() == 0) //topic doesnot have table in any city, then remove the topic		
			tablesAssignedToTopic.remove(topic);
	}

	/**
	 * Adds a new citytable to the topic. If its a new topic, makes a new entry 
	 * @param topic
	 * @param ct
	 */
	public void addTableToTopic(int topic, CityTable ct)
	{
		HashSet<CityTable> tables = tablesAssignedToTopic.get(topic);
		if(tables == null) //new topic
		{
			HashSet<CityTable> newSet = new HashSet<CityTable>();
			newSet.add(ct);
			tablesAssignedToTopic.put(topic, newSet);
			return;
		}
		tables.add(ct);
	}

	/**
	 * Returns a set of all currently used topics
	 * @return HashSet<Integer>
	 */
	public HashSet<Integer> getAllTopics() 
	{
		return new HashSet(tablesAssignedToTopic.keySet());
	}
	/**
	 * returns the set of city tables
	 * @param topicId
	 * @return
	 */
	public HashSet<CityTable> getCityTablesForTopic(int topicId)
	{
		return tablesAssignedToTopic.get(topicId);
	}
	
	/**
	 * Returns all the observations (NOT the indexes) of the customers of various cities having the topic t.
	 * Quite expensive operation, Is there a way to hash it?
	 * @param topic
	 * @return
	 */
	public ArrayList<Double> getAllObservationsForTopic(int topic)
	{
		ArrayList<ArrayList<Double>> listObservations = Data.getObservations(); // all observations
		ArrayList<Double> observationsForTopic = new ArrayList<Double>();
		HashSet<CityTable> tables = tablesAssignedToTopic.get(topic);
		for(CityTable table : tables) {
			int cityId = table.getCityId();
			int tableId = table.getTableId();
			ArrayList<Double> observationsAtList = listObservations.get(cityId);
			HashSet<Integer> customersAtTable = customersAtTableList.get(cityId).get(tableId);
			for (Integer customer : customersAtTable) {
				TestSample ts = new TestSample(cityId, customer, -1);
				if (!testSamples.contains(ts)) {			
					Double obs = observationsAtList.get(customer);
					observationsForTopic.add(obs);
				}	
			}
		}
		return observationsForTopic;
	}


	/**
	 * Returns all the observations (NOT the indexes) of the customers of various cities having the topic t.
	 * including those observations from table tableId at index listIndex
	 * Quite expensive operation, Is there a way to hash it?
	 * @param topic
	 * @param tableId
	 * @param listIndex
	 * @return
	 */
	public ArrayList<Double> getAllObservationsForTopicPlusTable(int topic, int tableToAddId, int listIndex)
	{
		// create a CityTable instance for the target CityTable
		CityTable tableToInclude = new CityTable(listIndex, tableToAddId);
		boolean tableIncluded = false;

		ArrayList<ArrayList<Double>> listObservations = Data.getObservations(); // all observations
		ArrayList<Double> observationsForTopic = new ArrayList<Double>();
		HashSet<CityTable> tables = tablesAssignedToTopic.get(topic);
		for(CityTable table : tables) {
			// check if this is the extra table we're supposed to include
			if (table.equals(tableToInclude))
				tableIncluded = true;
			int cityId = table.getCityId();
			int tableId = table.getTableId();
			ArrayList<Double> observationsAtList = listObservations.get(cityId);
			HashSet<Integer> customersAtTable = customersAtTableList.get(cityId).get(tableId);
			for (Integer customer : customersAtTable) {
				TestSample ts = new TestSample(cityId, customer, -1);
				if (!testSamples.contains(ts)) {					
					Double obs = observationsAtList.get(customer);
					observationsForTopic.add(obs);
				}
			}
		}

		// if tableToInclude was missing, add its observations
		if (!tableIncluded)
			observationsForTopic.addAll(getObservationAtTable(tableToAddId, listIndex));

		return observationsForTopic;
	}


	/**
	 * Returns all the observations (NOT the indexes) of the customers of various cities having the topic t.
	 * including those observations from table tableId at index listIndex
	 * Quite expensive operation, Is there a way to hash it?
	 * @param topic
	 * @param tableId
	 * @param listIndex
	 * @return
	 */
	public ArrayList<Double> getAllObservationsForTopicMinusTable(int topic, int tableToRemoveId, int listIndex)
	{
		// create a CityTable instance for the target CityTable
		CityTable tableToRemove = new CityTable(listIndex, tableToRemoveId);

		ArrayList<ArrayList<Double>> listObservations = Data.getObservations(); // all observations
		ArrayList<Double> observationsForTopic = new ArrayList<Double>();
		HashSet<CityTable> tables = tablesAssignedToTopic.get(topic);
		tables.remove(tableToRemove);  // Remove the table we're supposed to ignore from the hash set
		for(CityTable table : tables) {
			int cityId = table.getCityId();
			int tableId = table.getTableId();
			ArrayList<Double> observationsAtList = listObservations.get(cityId);
			HashSet<Integer> customersAtTable = customersAtTableList.get(cityId).get(tableId);
			for (Integer customer : customersAtTable) {
				TestSample ts = new TestSample(cityId, customer, -1);
				if (!testSamples.contains(ts)) {					
					Double obs = observationsAtList.get(customer);
					observationsForTopic.add(obs);
				}
			}
		}
		//adding back the topic to the set
		tables.add(tableToRemove);

		return observationsForTopic;
	}

	/**
	 * Returns the log posterior density of the sampler state at the observed data points
	 * @param liklihood
	 */
	public double getLogPosteriorDensity(Likelihood l) {
		return sumOfLogPriors + l.computeFullLogLikelihood(this);
	} 

	/**
	 * Same as above but for ddCRP
	 *
	 * TODO: need to subclass this
	 */
	public double getLogPosteriorDensityDDCRP(DirichletLikelihood l) {
		return sumOfLogPriors + l.computeFullLogLikelihoodDDCRP(this);
	} 

	
}
