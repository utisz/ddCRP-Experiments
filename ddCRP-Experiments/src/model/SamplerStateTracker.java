package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;


/**
 * This class will store all the sampler state objects of each iteration
 * @author rajarshd
 *
 */
public class SamplerStateTracker {
	
	/**
	 * The current iteration number. The first iteration is zeroth iteration.
	 */
	public static int current_iter;
	
	/**
	 * The maximum possible sampling iteration 
	 */
	public static int max_iter;
	
	/**
	 * List to hold the sampler states for each iteration. 
	 */
	public static ArrayList<SamplerState> samplerStates = new ArrayList<SamplerState>();
	
	/**
	 * Returns the current sampler state.
	 * @return
	 */
	public static SamplerState returnCurrentSamplerState()
	{
		if(current_iter >= 0 && current_iter==samplerStates.size()-1)
			return samplerStates.get(current_iter);
		else
			return null;
	}
	
	/**
	 * This initializes the sampler state. All observations point to themselves ie they have the customer assignments as themselves.
	 * As a result, each table consists of only one customer because of self links.
	 * @param list_observations
	 */
	public static void initializeSamplerState(ArrayList<ArrayList<Double>> list_observations)
	{
		if(current_iter == 0)
		{
			long num_data = 0;
			for(int i=0;i<list_observations.size();i++)
				num_data = num_data + list_observations.get(i).size();
			
			SamplerState state0 = new SamplerState(); //initial state		
			//setting the state
			SamplerState.setNum_data(num_data);
			ArrayList<ArrayList<Integer>> customer_assignments = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> table_assignments = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Long>> topic_assignments_table = new ArrayList<ArrayList<Long>>();
			ArrayList<ArrayList<Long>> topic_assignments_customer = new ArrayList<ArrayList<Long>>();
			
			HashMap<Integer, HashSet<CityTable>> tablesAssignedToTopic = new HashMap<Integer, HashSet<CityTable>>();
			HashMap<CityTable, Integer> topicAtTable = new HashMap<CityTable, Integer>();

			/////
			// ArrayList<HashMap<Integer,StringBuffer>> list_customers_in_table = new  ArrayList<HashMap<Integer,StringBuffer>>();
			ArrayList<HashMap<Integer, HashSet<Integer>>> customersAtTableList = new ArrayList<HashMap<Integer, HashSet<Integer>>>();

			HashMap<Integer,Integer> count_each_topic = new HashMap<Integer,Integer>();
			int num_topics = 50; //setting the initial number of topics to 50
			Random gen = new Random();			
			for(int i=0;i<list_observations.size();i++)		//keeping i as int, hoping that the number of cities/documents will be no greater than the size of integers
			{
				ArrayList<Integer> customer_assignment_per_list = new ArrayList<Integer>();
				ArrayList<Integer> table_assignment_per_list = new ArrayList<Integer>();
				ArrayList<Long> topic_assignments_customer_per_list = new ArrayList<Long>();
				
				/////
				// HashMap<Integer,StringBuffer> customers_in_table_per_list = new HashMap<Integer,StringBuffer>(); 
				HashMap<Integer, HashSet<Integer>> customersAtTable = new HashMap<Integer, HashSet<Integer>>();

				for(int j=0;j<list_observations.get(i).size();j++) //note: the customers are indexed from 0
				{//initializing the customer assignments for each point to itself and hence each customer in its own table
					customer_assignment_per_list.add(j); 
					table_assignment_per_list.add(j);

					/////
					// customers_in_table_per_list.put(j, new StringBuffer(Long.toString(j)));
				  HashSet<Integer> hs = new HashSet<Integer>();
				  hs.add(j);
					customersAtTable.put(j, hs);
					
					int topic = gen.nextInt(num_topics);
					// topic_assignments_table_per_list.add(new Long(topic));
					topic_assignments_customer_per_list.add(new Long(topic));
					Integer count = count_each_topic.get(topic);
					if(count == null) //new entry			
						count_each_topic.put(topic, 1);
					else
						count_each_topic.put(topic, count+1);

					// initialize the topic structure
					CityTable ct = new CityTable(i,j);

					// ct is assigned to topic topic 
					topicAtTable.put(ct, topic);

					// add ct to the HashSet of tables assigned to topic topic
					if (tablesAssignedToTopic.get(topic) == null) 
					{
						HashSet<CityTable> ctHashSet = new HashSet<CityTable>();
				  	ctHashSet.add(ct);
						tablesAssignedToTopic.put(topic, ctHashSet);
					}
					else 
					{
						HashSet<CityTable> ctHashSet = tablesAssignedToTopic.get(topic);
						ctHashSet.add(ct);
						tablesAssignedToTopic.put(topic, ctHashSet);
					}

				}
				customer_assignments.add(customer_assignment_per_list);
				table_assignments.add(table_assignment_per_list);

				/////
				// list_customers_in_table.add(customers_in_table_per_list);
				customersAtTableList.add(customersAtTable);

				// topic_assignments_table.add(topic_assignments_table_per_list);
				topic_assignments_customer.add(topic_assignments_customer_per_list);
			}
			state0.setC(customer_assignments);
			state0.set_t(table_assignments);
			state0.setT(num_data); //number of tables equal to num_data
			state0.setCustomersAtTableList(customersAtTableList);

			// Initialize the topics
			state0.setK(new Long(num_topics));
			state0.setM(count_each_topic);
			state0.setMaxTopicId(state0.getK().intValue() - 1); //since numbering topic starts from 0.
			state0.setTablesAssignedToTopic(tablesAssignedToTopic);
			state0.setTopicAtTable(topicAtTable);

			//Now putting into the arraylist of sampler states
			current_iter = 0;
			samplerStates.add(state0);
		}
	}
	
	

}
