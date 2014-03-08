package model;

import data.Data;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
public class ThetaDDCRP extends Theta {

  public ThetaDDCRP(SamplerState samplerState, HyperParameters hyperParameters) {
    super(samplerState, hyperParameters);
  }

  /*
   * A helper method to compute the unqiue table id given cityId and the non-unique tableId
   */
  public static int getUniqueTableId(Integer cityId, Integer tableId) {
    CityTable ct = new CityTable(cityId, tableId);
    return ct.hashCode();
  }

  /*
   * Computes the value of the theta vectors for this stampler state, for each city, for each table in the city
   * Each table has a CRSMatrix theta, where
   * theta_j = (N_j + a_j) / (n + sum_i(a_i))
   * where a_j is the Dirichlet prior parameter
   */
  @Override
  public void estimateThetas() {
    HashMap<Integer, HashMap<Integer, Double>> newThetaMap = new HashMap<Integer, HashMap<Integer, Double>>();
    HashMap<Integer, HashMap<String, Double>> newThetaMapString = new HashMap<Integer, HashMap<String, Double>>();

    // For each city
    ArrayList<HashMap<Integer, HashSet<Integer>>> customersAtTableList = samplerState.getCustomersAtTableList();
    for (int cityId=0; cityId<customersAtTableList.size(); cityId++) {
      // For each table id
      Set<Integer> tableIds = customersAtTableList.get(cityId).keySet();
      for (Integer tableId : tableIds) {
        // get all the observations for this table and add them to the counts
        ArrayList<Double> tableObservations = samplerState.getObservationsFromTable(tableId, cityId);
        if (tableObservations != null && tableObservations.size() > 0) {
          // Compute the unique tableId used in the thetaMaps
          Integer uniqueTableId = getUniqueTableId(cityId, tableId);

          // Initialize the tables's theta vector
          HashMap<Integer, Double> theta = new HashMap<Integer, Double>(); 
          HashMap<String, Double> thetaString = new HashMap<String, Double>(); 

          // add the dirichlet parameters
          ArrayList<Double> dirichletParam = hyperParameters.getDirichletParam();
          for (int j=0; j<dirichletParam.size(); j++) {
            theta.put(j, dirichletParam.get(j));
            thetaString.put(vocabulary.get(j), dirichletParam.get(j));
          }

          for (Double obs : tableObservations) {
            Integer observation = obs.intValue() - 1;
            String observationString = vocabulary.get(observation);
            double currentObservationCount = theta.get(observation);
            theta.put(observation, currentObservationCount + 1);
            thetaString.put(observationString, currentObservationCount + 1);        
          }

          // get the normalizing constant
          double norm = 0.0;
          for (int j=0; j<hyperParameters.getVocabSize(); j++) {
            norm += theta.get(j);
          }

          // divide by the normalizing constant
          for (int j=0; j<hyperParameters.getVocabSize(); j++) {
            String obsStringJ = vocabulary.get(j);
            double thetaJ = theta.get(j) / norm;
            theta.put(j, thetaJ);
            thetaString.put(obsStringJ, thetaJ);
          }    

          newThetaMap.put(uniqueTableId, theta);
          newThetaMapString.put(uniqueTableId, thetaString);
        }
      }
    }

    thetaMap = newThetaMap;
    thetaMapString = newThetaMapString;
  }

  /*
   * Computes the MAP estimate of the theta vectors for this stampler state, for each city, for each table in the city
   * Each table has a CRSMatrix theta, where
   * theta_j = (N_j + a_j - 1) / (n + sum_i(a_i - 1))
   * where a_j is the Dirichlet prior parameter
   */
  @Override  
  public void estimateThetasMAP() {
    HashMap<Integer, HashMap<Integer, Double>> newThetaMap = new HashMap<Integer, HashMap<Integer, Double>>();
    HashMap<Integer, HashMap<String, Double>> newThetaMapString = new HashMap<Integer, HashMap<String, Double>>();

    // For each city
    ArrayList<HashMap<Integer, HashSet<Integer>>> customersAtTableList = samplerState.getCustomersAtTableList();
    for (int cityId=0; cityId<customersAtTableList.size(); cityId++) {
      // For each table id
      Set<Integer> tableIds = customersAtTableList.get(cityId).keySet();
      for (Integer tableId : tableIds) {
        // get all the observations for this table and add them to the counts
        ArrayList<Double> tableObservations = samplerState.getObservationsFromTable(tableId, cityId);
        if (tableObservations != null & tableObservations.size() > 0) {
          // Compute the unique tableId used in the thetaMaps
          Integer uniqueTableId = getUniqueTableId(cityId, tableId);

          // Initialize the tables's theta vector
          HashMap<Integer, Double> theta = new HashMap<Integer, Double>(); 
          HashMap<String, Double> thetaString = new HashMap<String, Double>(); 
      
          // add the dirichlet parameters (a_i - 1)
          ArrayList<Double> dirichletParam = hyperParameters.getDirichletParam();
          for (int j=0; j<dirichletParam.size(); j++) {
            theta.put(j, dirichletParam.get(j) - 1);
            thetaString.put(vocabulary.get(j), dirichletParam.get(j) - 1);
          }

          for (Double obs : tableObservations) {
            Integer observation = obs.intValue() - 1;
            String observationString = vocabulary.get(observation);
            double currentObservationCount = theta.get(observation);
            theta.put(observation, currentObservationCount + 1);
            thetaString.put(observationString, currentObservationCount + 1);        
          }

          // get the normalizing constant
          double norm = 0.0;
          for (int j=0; j<hyperParameters.getVocabSize(); j++) {
            norm += theta.get(j);
          }

          // divide by the normalizing constant
          for (int j=0; j<hyperParameters.getVocabSize(); j++) {
            String obsStringJ = vocabulary.get(j);
            double thetaJ = theta.get(j) / norm;
            theta.put(j, thetaJ);
            thetaString.put(obsStringJ, thetaJ);
          }  

          newThetaMap.put(uniqueTableId, theta);
          newThetaMapString.put(uniqueTableId, thetaString); 
        } 
      }
    } 

    thetaMap = newThetaMap;
    thetaMapString = newThetaMapString;
  }
}