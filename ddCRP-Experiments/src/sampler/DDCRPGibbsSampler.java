/**
 * 
 */
package sampler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import model.HyperParameters;
import model.SamplerState;
import model.SamplerStateTracker;
import model.CityTable;
import test.TestSample;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.la4j.matrix.sparse.CRSMatrix;
import org.la4j.vector.Vector;

import util.Util;

import Likelihood.Likelihood;

import data.Data;

/**
 * Gibbs Sampler for Distance Dependent Chinese Restaurant Process. This is the implementation
 * of the sampling process described in <a href="http://www.cs.princeton.edu/~blei/papers/BleiFrazier2011.pdf"> paper </a>
 * @author rajarshd
 *
 */
public class DDCRPGibbsSampler {

  /**
   * A list of queues, each queue for maintaining a list of empty tables, which can be assigned when split of tables happen.
   */
  private static List<Queue<Integer>> emptyTables = new ArrayList<Queue<Integer>>();

  private final static Logger LOGGER = Logger.getLogger(GibbsSampler.class
          .getName());

  static{
    try {   
      LOGGER.setLevel(Level.INFO);
      FileHandler logFileHandler;
      logFileHandler = new FileHandler("log.txt");
      logFileHandler.setFormatter(new SimpleFormatter());
      LOGGER.addHandler(logFileHandler);    
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param l
   */ 
  public static void doSampling(Likelihood l, HashSet<TestSample> testSamples) 
  {
    //create the copy of the latest sampler state and assign it to the new one
    SamplerState s = SamplerStateTracker.samplerStates.get(SamplerStateTracker.current_iter).copy();
    s.setTestSamples(testSamples);
    SamplerStateTracker.samplerStates.add(s);
    //increase the number of iteration of sampling by 1
    SamplerStateTracker.current_iter = SamplerStateTracker.current_iter + 1;    

    //if queue already not initialized, then initialize a list of empty queues
    ArrayList<ArrayList<Double>> all_observations = Data.getObservations();
    if(emptyTables.size()!=all_observations.size())   
      for(int i=0;i<all_observations.size();i++)
        emptyTables.add(new LinkedList<Integer>());

    //Sampling for each list (city/document)
    for(int i=0;i<all_observations.size();i++)
    {
      LOGGER.log(Level.FINE, "Starting to sample for list "+i);     
      ArrayList<Double> list = all_observations.get(i); //each city in our case

      //Get the set of test indices, those which we should ignore while sampling
      //HashMap<Integer,Integer> venue_ids = Test.getTest_venue_ids(i);
      //HashMap<Integer,Integer> venue_ids = t.getTestVenueIdsForCity(i);

      //For each observation in the list sample customer assignments (for each venue in a city)       
      for(int j=0;j<list.size();j++) //Concern: No of observation in a list should not cross the size of integers
      {       
        //sample customer link for this observation
        //if(venue_ids.get(j)==null) //if it is not a part of the test sample.
          //sampleLink(j,i,l,venue_ids); //sending the list (city) and the index so that the observation can be accessed
        sampleLink(j,i,l);
      }
      LOGGER.log(Level.FINE, "Done for list "+i);
      //System.out.println("Done for list "+i);
    }
  }

  /**
   * This method samples a link for each observation. The parameters are index of the sample
   * and the index of the list it belongs to.
   * @param index
   * @param list_index
   * @param ll
   */
  private static void sampleLink(Integer index, int list_index, Likelihood ll)
  {
    LOGGER.log(Level.FINE, "Sampling link for index "+index+" list_index "+list_index);

    //check to see if the table has circle or not
    //get the table id where this observation is sitting
    SamplerState s = SamplerStateTracker.samplerStates.get(SamplerStateTracker.current_iter);
    int table_id = s.get_t(index, list_index); //the table id where the observation is sitting
    s.setSumOfLogPriors(0.0);

    // TO CHANGE /////////////////////////////////

    //get all the customers who are sitting in the table.
    HashSet<Integer> customersAtTable = s.getCustomersAtTable(table_id, list_index);

    //String customers_in_table = s.getCustomers_in_table(table_id, list_index);
    //String customer_indexes[] = customers_in_table.split(",");

    ArrayList<Integer> orig_table_members = new ArrayList<Integer>(); //this will hold the table members of the customer, if there is a split, we will update it to point to the members of the new table

    //Create a graph with customers_in_table as the vertices
    DirectedGraph<Integer, DefaultEdge> g =
              new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);

    for(Integer i : customersAtTable) 
    {
      orig_table_members.add(i);

      if(!g.containsVertex(i)) //didnot encounter this customer b4 
      {
        g.addVertex(i);               
      }   

      //now create an edge with its customer assignment
      Integer j = s.getC(i, list_index);

      //lets check we have a vertex already created for this customer or not?
      if(!g.containsVertex(j))
      { //nope will have to create a new object
        g.addVertex(j);
      }

      //adding the edge
      g.addEdge(i, j);   //1st arg, the current customer, 2nd arg, its assignment
    } //the graph should be ready

    //If the 'obs_to_sample' is a part of a cycle then removing its customer assignment cannot split the table. 
    //Check if there is a cycle containing obs_to_sample
    CycleDetector<Integer,DefaultEdge> cycleDetector = new CycleDetector<Integer,DefaultEdge>(g);
    boolean isCyclePresent = cycleDetector.detectCyclesContainingVertex(index);
    // any undirected cycle that is not a directed cycle has to have at least one vertext of out degree 2
    // since g has out degree of at most 1, any directed cycle is also an undirected cycle
    // so if there is no directed cycle, upon removing the link the table will be split
    if(!isCyclePresent) 
    {
      LOGGER.log(Level.FINE, index+" doesnot have a cycle and hence the table "+table_id+" will split");

      //Now collecting the table members of the observation we are sampling for
      //I will first create a undirected graph, without the outgoing edge from the observation, Then I will do a 
      //depth first traversal starting from the observation to be sampled and get all the other nodes of the components reachable from it
      UndirectedGraph<Integer,DefaultEdge> u_g =
          new AsUndirectedGraph<Integer, DefaultEdge>(g); //creating the undirected graph from the directed graph
      //now removing the edge (obs_to_sample -> its customer assignment)
      u_g.removeEdge(index, s.getC(index, list_index));

      //Lets do a depth first traversal now and get all the table members
      DepthFirstIterator<Integer,DefaultEdge> iter = new DepthFirstIterator<Integer,DefaultEdge>(u_g,index);
      ArrayList<Integer> new_table_members = new ArrayList<Integer>();
      while(iter.hasNext())      
        new_table_members.add(iter.next());   

      orig_table_members = new_table_members; //updating orig_table_members to point to new_table_members
      //Let's get the customers which remained in the original table after the split, do a depth first traversal starting from the original customer assignment of the customer to be sampled
      iter = new DepthFirstIterator<Integer,DefaultEdge>(u_g, s.getC(index, list_index));
      ArrayList<Integer> old_table_members = new ArrayList<Integer>();
      while(iter.hasNext())
        old_table_members.add(iter.next());

      //Ok, now since the table has split, update the sampler state accordingly
      s.setT(s.getT()+1); //incrementing the number of tables
      s.setC(null, index, list_index); //since this customer has 'no' customer assignment as of now
      int new_table_id = emptyTables.get(list_index).remove(); //getting an empty table
      LOGGER.log(Level.FINE, "The new table id after splitting is "+new_table_id);

      for(int l:new_table_members) //setting the table assignment to the new table number      
        s.set_t(new_table_id, l, list_index);
      int old_table_id = table_id;

      s.setCustomersAtTable(new HashSet<Integer>(old_table_members),  old_table_id, list_index);

      table_id = new_table_id; //updating, since this is the new table_id of the current customer we are trying to sample for.
      s.setCustomersAtTable(new HashSet<Integer>(new_table_members),  new_table_id, list_index);
    }

    //Now, will sample a new link for the customer
    //get the distance matrix for Prior computation
    ArrayList<CRSMatrix> distanceMatrices = Data.getDistanceMatrices();
    CRSMatrix distance_matrix = distanceMatrices.get(list_index); // getting the correct distance matrix 
    Vector priors = distance_matrix.getRow(index);
    //Iterate throught the Test indices, and zero out the prior for any test data, removing the possibility of linking to testing data
    /*for (Integer id : venue_ids.keySet()) {
      priors.set(id,0);
    }*/
    //Set the prior for self linkage
    priors.set(index, ll.getHyperParameters().getSelfLinkProb()); //since according to the ddcrp prior, the prob of a customer forming a link to itself is given by \alpha
    double sum = 0;
    for(int i=0;i<priors.length();i++)
      sum = sum + priors.get(i);
    priors = priors.divide(sum);  
    //Now for each possible 'new' customer assignment, ie for those whose priors != 0
    //we calculate the posterior probability of forming the link with that customer.
    //For that we calculate the change in likelihood if there are joins of tables

    // Compute components of the change in likelihood that don't varry across the posterior for a proposed link
    
    ArrayList<Double> posterior = new ArrayList<Double>(); //this will hold the posterior probabilities for all possible customer assignment and we will sample according to these probabilities
    ArrayList<Integer> indexes = new ArrayList<Integer>(); // for storing the indexes of the customers who could be possible assignments
    Double maxLogPosterior = Double.NEGATIVE_INFINITY;
    for(int i=0;i<priors.length();i++)
    {
      if(priors.get(i)!=0)
      {
        indexes.add(i); //adding the index of this possible customer assignment.
        //get the table id of this table        
        int table_proposed = s.get_t(i, list_index); //table_proposed is the proposed table to be joined
        if(table_proposed == table_id) //since the proposed table is the same, hence there will be no change in the likelihood if this is the customer assignment       
        { 
          double logPosterior = Math.log(priors.get(i));
          if (logPosterior > maxLogPosterior)
            maxLogPosterior = logPosterior;
          posterior.add(logPosterior); //since the posterior will be determined only by the prior probability
        }
        else //will have to compute the change in likelihood
        {         

          HashSet<Integer> proposedTableMembersSet = s.getCustomersAtTable(table_proposed, list_index);
          ArrayList<Integer> proposed_table_members = new ArrayList<Integer>(proposedTableMembersSet);

          //Now compute the change in likelihood
          double change_in_log_likelihood = compute_change_in_likelihood(ll,orig_table_members,proposed_table_members,list_index);          
          double logPosterior = Math.log(priors.get(i)) + change_in_log_likelihood;

          if (logPosterior > maxLogPosterior)
            maxLogPosterior = logPosterior;

          // //Now compute the change in likelihood
          posterior.add(logPosterior); //adding the prior and likelihood
        }
      }
    }
    // Subtract the maxLogPosterior from each term of posterior (avoid overflows), then exponentiate
    for (int i=0; i<posterior.size(); i++) {
      double post = posterior.get(i);
      if (post != Double.NEGATIVE_INFINITY)
        posterior.set(i, Math.exp(post - maxLogPosterior));
      else 
        posterior.set(i, 0.0);

    }

    //the posterior probabilities are computed for each possible customer assignment, Now lets sample from it.
    int sample = Util.sample(posterior);    

    int customer_assignment_index = indexes.get(sample); //this is the customer assignment in this iteration, phew!   
    LOGGER.log(Level.FINE, "The sampled link for customer indexed "+index +" of list "+list_index+" is "+customer_assignment_index);

    // add to the prior component to sum of log priors
    s.setSumOfLogPriors( s.getSumOfLogPriors() + Math.log(priors.get(customer_assignment_index)) );

    int assigned_table = s.get_t(customer_assignment_index, list_index);
    s.setC(customer_assignment_index, index, list_index); //setting the customer assignment
    if(assigned_table!=table_id) //this is a join of two tables 
    {
      LOGGER.log(Level.FINE, "Table "+table_id+" joins with "+assigned_table);
      s.setT(s.getT()-1); //since there is a join, there is a decrease by 1.      
      for(Integer members:orig_table_members)     
        s.set_t(assigned_table, members, list_index); //setting the table assignment of the old table members to the new assigned table     
      //update the map now

      //First, update the new assigned table to include the table members of the customer's table
      HashSet<Integer> hs_orig_members_in_new_table = new HashSet<Integer>(s.getCustomersAtTable(assigned_table, list_index));

      // now add the old members to the new table
      for(int i=0;i<orig_table_members.size();i++)
        hs_orig_members_in_new_table.add(orig_table_members.get(i));

      s.setCustomersAtTable(hs_orig_members_in_new_table, assigned_table, list_index);
      //Then, update the orig_table to null
      s.setCustomersAtTable(null,table_id, list_index);

      //Atlast, enqueue this table_id, since this table is empty
      emptyTables.get(list_index).add(table_id);

    }   
    LOGGER.log(Level.FINE, " DONE Sampling link for index "+index+" list_index "+list_index);
  }


  /**
   * Method to compute the change in log-likelihood due to join of two tables
   * @param l This will compute the log-likelihood
   * @param orig_table_members
   * @param proposed_table_members
   * @return
   */
  private static double compute_change_in_likelihood(Likelihood l,ArrayList<Integer> orig_table_members,ArrayList<Integer> proposed_table_members,int list_index )
  {
    double orig_table_loglikelihood = l.computeTableLogLikelihoodFromCustomers(orig_table_members, list_index);
    double proposed_table_loglikelihood = l.computeTableLogLikelihoodFromCustomers(proposed_table_members, list_index);
    
    //take union of the two lists
    ArrayList<Integer> union_list = new ArrayList<Integer>();
    for(Integer member:orig_table_members)    
      union_list.add(member);
    for(Integer member:proposed_table_members)
      union_list.add(member);
    double table_union_loglikelihood = l.computeTableLogLikelihoodFromCustomers(union_list, list_index); 
    
    double change_in_log_likelihood = table_union_loglikelihood - (orig_table_loglikelihood + proposed_table_loglikelihood);    
    return change_in_log_likelihood;
  }

}