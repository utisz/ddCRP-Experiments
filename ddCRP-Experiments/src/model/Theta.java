package model;

import data.Data;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Collections;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;



/**
 * Given a customer assignment and topic assignment(sampler state), this computes the inferred 
 * multinomial distribution parameters at each table
 * @author jcransh
 */
public class Theta {

  /**
   * The SamplerState for which we'd like to compute thetas for
   */ 
  private SamplerState samplerState;

  /**
   * The model hyperparameters
   */ 
  private HyperParameters hyperParameters;

  /**
   * The vocab file
   */ 
  private static final String vocabFile = "Data/venue_categories.txt";

  /**
   * The vocabulary, numeric observations are indexes into Strings
   */ 
  private ArrayList<String> vocabulary = new ArrayList<String>();

  /** 
   * The multinomial parameter seen for each city, for each table in the city.  
   * Each theta is a HashMap from Integer to Double (key:observation to value:probability)
   */
  private HashMap<Integer, HashMap<Integer, Double>> topicToThetaMap = new HashMap<Integer, HashMap<Integer, Double>>();

  /** 
   * The multinomial parameter seen for each city, for each table in the city.  
   * Each theta is a HashMap from String to Double (key:observation string to value:probability)
   */
  private HashMap<Integer, HashMap<String, Double>> topicToThetaMapString = new HashMap<Integer, HashMap<String, Double>>();


  /** 
   * Constructor
   * @author jcransh
   */
  public Theta(SamplerState samplerState, HyperParameters hyperParameters)
  {
    this.samplerState = samplerState;
    this.hyperParameters = hyperParameters;

    try {
      BufferedReader reader = new BufferedReader(new FileReader(vocabFile));
      String line;
      while ((line = reader.readLine())!=null) 
        vocabulary.add(line);     
    } catch(FileNotFoundException ex){
      ex.printStackTrace();
    } catch(IOException ex){
      ex.printStackTrace();
    }
  }

  /** 
   * Constructor
   * @author jcransh
   */
  public Theta(){}

  /**
   * Getter for the samplerState
   */
  public SamplerState getSamplerState() {
    return samplerState;
  }

  /**
   * Setter for the samplerState
   */
  public void setSamplerState(SamplerState s) {
    samplerState = s;
  }

  /**
   * Getter for the samplerState
   */
  public HyperParameters getHyperParameters() {
    return hyperParameters;
  }

  /**
   * Setter for the samplerState
   */
  public void setHyperParameters(HyperParameters h) {
    hyperParameters = h;
  }  

 /**
   * Getter for the computed theta map
   */
  public HashMap<Integer, HashMap<Integer, Double>> getTopicToThetaMap() {
    return topicToThetaMap;
  }

  /*
   * Computes the value of the theta vectors for this stampler state, for each city, for each table in the city
   * Each table has a CRSMatrix theta, where
   * theta_j = (N_j + a_j) / (n + sum_i(a_i))
   * where a_j is the Dirichlet prior parameter
   */
  public void estimateThetas() {
    HashMap<Integer, HashMap<Integer, Double>> newTopicToThetaMap = new HashMap<Integer, HashMap<Integer, Double>>();
    HashMap<Integer, HashMap<String, Double>> newTopicToThetaMapString = new HashMap<Integer, HashMap<String, Double>>();

    HashSet<Integer> topics = samplerState.getAllTopics(); 
    for (Integer topic : topics) {
      // Initialize the topics's theta vector
      HashMap<Integer, Double> topicTheta = new HashMap<Integer, Double>(); 
      HashMap<String, Double> topicThetaString = new HashMap<String, Double>(); 
      
      // add the dirichlet parameters
      ArrayList<Double> dirichletParam = hyperParameters.getDirichletParam();
      for (int j=0; j<dirichletParam.size(); j++) {
        topicTheta.put(j, dirichletParam.get(j));
        topicThetaString.put(vocabulary.get(j), dirichletParam.get(j));
      }

      // get all the observatiosn for this topic and add them to the counts
      ArrayList<Double> topicObservations = samplerState.getAllObservationsForTopic(topic);
      for (Double obs : topicObservations) {
        Integer observation = obs.intValue() - 1;
        String observationString = vocabulary.get(observation);
        double currentObservationCount = topicTheta.get(observation);
        topicTheta.put(observation, currentObservationCount + 1);
        topicThetaString.put(observationString, currentObservationCount + 1);        
      }

      // get the normalizing constant
      double norm = 0.0;
      for (int j=0; j<hyperParameters.getVocabSize(); j++) {
        norm += topicTheta.get(j);
      }

      // divide by the normalizing constant
      for (int j=0; j<hyperParameters.getVocabSize(); j++) {
        String obsStringJ = vocabulary.get(j);
        double thetaJ = topicTheta.get(j) / norm;
        topicTheta.put(j, thetaJ);
        topicThetaString.put(obsStringJ, thetaJ);
      }    

      newTopicToThetaMap.put(topic, topicTheta);
      newTopicToThetaMapString.put(topic, topicThetaString);
    } 

    topicToThetaMap = newTopicToThetaMap;
    topicToThetaMapString = newTopicToThetaMapString;
  }

  /*
   * Computes the MAP estimate of the theta vectors for this stampler state, for each city, for each table in the city
   * Each table has a CRSMatrix theta, where
   * theta_j = (N_j + a_j - 1) / (n + sum_i(a_i - 1))
   * where a_j is the Dirichlet prior parameter
   */
  public void estimateThetasMAP() {
    HashMap<Integer, HashMap<Integer, Double>> newTopicToThetaMap = new HashMap<Integer, HashMap<Integer, Double>>();
    HashMap<Integer, HashMap<String, Double>> newTopicToThetaMapString = new HashMap<Integer, HashMap<String, Double>>();

    HashSet<Integer> topics = samplerState.getAllTopics(); 
    for (Integer topic : topics) {
      // Initialize the topics's theta vector
      HashMap<Integer, Double> topicTheta = new HashMap<Integer, Double>(); 
      HashMap<String, Double> topicThetaString = new HashMap<String, Double>(); 
      
      // add the dirichlet parameters (a_i - 1)
      ArrayList<Double> dirichletParam = hyperParameters.getDirichletParam();
      for (int j=0; j<dirichletParam.size(); j++) {
        topicTheta.put(j, dirichletParam.get(j) - 1);
        topicThetaString.put(vocabulary.get(j), dirichletParam.get(j) - 1);
      }

      // get all the observatiosn for this topic and add them to the counts
      ArrayList<Double> topicObservations = samplerState.getAllObservationsForTopic(topic);
      for (Double obs : topicObservations) {
        Integer observation = obs.intValue() - 1;
        String observationString = vocabulary.get(observation);
        double currentObservationCount = topicTheta.get(observation);
        topicTheta.put(observation, currentObservationCount + 1);
        topicThetaString.put(observationString, currentObservationCount + 1);        
      }

      // get the normalizing constant
      double norm = 0.0;
      for (int j=0; j<hyperParameters.getVocabSize(); j++) {
        norm += topicTheta.get(j);
      }

      // divide by the normalizing constant
      for (int j=0; j<hyperParameters.getVocabSize(); j++) {
        String obsStringJ = vocabulary.get(j);
        double thetaJ = topicTheta.get(j) / norm;
        topicTheta.put(j, thetaJ);
        topicThetaString.put(obsStringJ, thetaJ);
      }    

      newTopicToThetaMap.put(topic, topicTheta);
      newTopicToThetaMapString.put(topic, topicThetaString);
    } 

    topicToThetaMap = newTopicToThetaMap;
    topicToThetaMapString = newTopicToThetaMapString;
  }


  /*
   * Output the k most probable tokens per topic
   * Should pass an outstream to this method
   */
  public void printMostProbWordsPerTopic(int k, PrintStream out) {
    for (Map.Entry<Integer, HashMap<String, Double>> entry : topicToThetaMapString.entrySet()) {
      Integer topic = entry.getKey();
      HashMap<String, Double> theta = entry.getValue();
      out.println("Topic " + topic);
      out.println("---------------");
      ArrayList<Map.Entry> entries = new ArrayList(theta.entrySet());

      Collections.sort(
         entries,  
         new Comparator() {  
             public int compare(Object o1, Object o2) {  
                 Map.Entry e1 = (Map.Entry) o1;  
                 Map.Entry e2 = (Map.Entry) o2;  
                 return ((Comparable) e2.getValue()).compareTo(e1.getValue());  
             }  
         }
      ); 

      for (int i=0; i<k; i++) {
        Map.Entry e = entries.get(i);
        out.println(e.getKey() + " --- " + e.getValue());
      }
      out.println("");

    } 
  }

  public double observationProbabilityInTopic(Integer observation, Integer topic) {
    return topicToThetaMap.get(topic).get(observation);
  }
}