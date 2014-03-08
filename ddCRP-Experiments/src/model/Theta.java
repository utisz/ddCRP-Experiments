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
public abstract class Theta {

  /**
   * The SamplerState for which we'd like to compute thetas for
   */ 
  protected SamplerState samplerState;

  /**
   * The model hyperparameters
   */ 
  protected HyperParameters hyperParameters;

  /**
   * The vocab file
   */ 
  protected static final String vocabFile = "Data/venue_categories.txt";

  /**
   * The vocabulary, numeric observations are indexes into Strings
   */ 
  protected ArrayList<String> vocabulary = new ArrayList<String>();

  /** 
   * The multinomial parameter seen for each city, for each table in the city.  
   * Each theta is a HashMap from Integer to Double (key:observation to value:probability)
   */
  protected HashMap<Integer, HashMap<Integer, Double>> thetaMap = new HashMap<Integer, HashMap<Integer, Double>>();

  /** 
   * The multinomial parameter seen for each city, for each table in the city.  
   * Each theta is a HashMap from String to Double (key:observation string to value:probability)
   */
  protected HashMap<Integer, HashMap<String, Double>> thetaMapString = new HashMap<Integer, HashMap<String, Double>>();


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
  public HashMap<Integer, HashMap<Integer, Double>> getThetaMap() {
    return thetaMap;
  }

  /*
   * Output the k most probable tokens per topic
   * Should pass an outstream to this method
   */
  public void printMostProbWordsPerTheta(int k, PrintStream out) {
    for (Map.Entry<Integer, HashMap<String, Double>> entry : thetaMapString.entrySet()) {
      Integer thetaId = entry.getKey();
      HashMap<String, Double> theta = entry.getValue();
      out.println("Theta " + thetaId);
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

  public double observationProbabilityInTheta(Integer observation, Integer thetaId) {
    if (thetaMap.get(thetaId) != null && thetaMap.get(thetaId).get(observation) != null)
      return thetaMap.get(thetaId).get(observation);
    else 
      return 0.0;
  }

  public abstract void estimateThetas();

  public abstract void estimateThetasMAP();

}