import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import model.HyperParameters;
import model.SamplerStateTracker;
import model.SamplerState;
import model.Theta;
import model.ThetaDDCRF;
import model.ThetaDDCRP;
import model.Posterior;
import sampler.GibbsSampler;
import sampler.DDCRPGibbsSampler;
import util.Util;
import Likelihood.DirichletLikelihood;
import Likelihood.Likelihood;
import data.Data;
import test.TestUniform;
import test.TestUniformSpace;
import test.TestUniformCategory;
import test.CategoryPredictorDDCRF;
import test.CategoryPredictorDDCRP;
import test.LocationPredictorDDCRF;
import test.TestSample;
import test.Baselines;

public class Driver {
	
	public static int vocab_size;

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		try {

			vocab_size = Integer.parseInt(args[1]);
			System.out.println("Vocab Size is "+vocab_size);
			
			//set the hyper-parameters
			double dirichlet_param = Double.parseDouble(args[2]);
			System.out.println("Dirichlet parameter is "+dirichlet_param);
			ArrayList<Double> dirichlet = new ArrayList<Double>();
			for(int i=0;i<vocab_size;i++)
				dirichlet.add(dirichlet_param);
			double alpha = Double.parseDouble(args[3]);
			double crp_alpha = Double.parseDouble(args[4]);
			System.out.println("Self Linkage Prob is "+alpha);
			HyperParameters h = new HyperParameters(vocab_size, dirichlet, alpha,crp_alpha);
	
			// generate some test samples
			TestUniform test = new TestUniform(25);
			test.generateTestSamples();
			HashSet<TestSample> testSamples = test.getTestSamplesSet();

			int numIter = Integer.parseInt(args[0]);

			// set the output directory based on the parameters
			Util.setOutputDirectoryFromArgs(numIter, dirichlet_param, alpha, crp_alpha, test);
			// Util.outputTestMeta();

			doDDCRP(numIter, h, testSamples);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	public static void doDDCRP(int numIter, HyperParameters h, HashSet<TestSample> testSamples) {
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();	
		SamplerStateTracker.initializeSamplerState(list_observations);
		Likelihood l = new DirichletLikelihood(h);

		SamplerStateTracker.max_iter = numIter;
		System.out.println("ddCRP Gibbs Sampler will run for "+SamplerStateTracker.max_iter+" iterations.");
				
		//do sampling		
		long init_time = System.currentTimeMillis();
		for(int i=1;i<=SamplerStateTracker.max_iter;i++)
		{
			long init_time_iter = System.currentTimeMillis();
			DDCRPGibbsSampler.doSampling(l, testSamples);
			System.out.println("----------------------");
			System.out.println("Iteration "+i+" done");
			System.out.println("Took "+(System.currentTimeMillis() - init_time_iter)/(double)1000+" seconds");
			SamplerStateTracker.returnCurrentSamplerState().prettyPrint(System.out);
			double posteriorLogPrior = SamplerStateTracker.returnCurrentSamplerState().getLogPosteriorDensityDDCRP((DirichletLikelihood)l);
			System.out.println("Posterior log density: " + posteriorLogPrior);
			System.out.println("----------------------");
		}
		
		long diff = System.currentTimeMillis() - init_time; 
		System.out.println("Time taken for Sampling "+(double)diff/1000+" seconds");		
		
		Posterior p = new Posterior(0, h);
		SamplerState sMAP = p.getMapEstimateDensityDDCRP(l);
		System.out.println("----------------------");
		System.out.println("FINAL STATE");
		System.out.println("----------------------");		
		sMAP.prettyPrint(System.out);

	  // store the values of the SamplerState densities for future use
	  HashMap<Integer, Double> samplerStatePosteriorDensities = new HashMap<Integer, Double>();

	  // store the values of the SamplerState theta for future use
	  HashMap<Integer, Theta> samplerStateThetas = new HashMap<Integer, Theta>();

    // ArrayList<SamplerState> states = SamplerStateTracker.samplerStates;
    // for (Integer i=0; i<states.size(); i++) {
    // 	SamplerState s = states.get(i);
    //   double logPosteriorDensity = s.getLogPosteriorDensityDDCRP((DirichletLikelihood)l);
    //   samplerStatePosteriorDensities.put(i, logPosteriorDensity);
    //   ThetaDDCRP theta = new ThetaDDCRP(s, l.getHyperParameters());
    //   theta.estimateThetas();
    //   samplerStateThetas.put(i, theta);
    // }
    ThetaDDCRP thetaMAP = new ThetaDDCRP(sMAP, l.getHyperParameters());
		thetaMAP.estimateThetas();

		System.out.println("Running a test");
		System.out.println("ddCRF, ddCRF-MAP, Mult-All, Mult-Each, ddCRF-Location, Prediction, True, InTopTen, InTopTenEach, InTopTenAll");

		for (TestSample sample : testSamples) {
			// Predictor predictor = new Predictor(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			// double ddCRF = predictor.computeProbabilityForSample();
			// double ddCRFMap = predictor.computeProbabilityForSampleMAP(sMAP, thetaMAP);
			// double multAll = baselines.predictMultProbAcrossAllCities(sample);
			// double multEach =  baselines.predictMultProbForEachCity(sample);
			// double ddCRFLocation = predictor.computeLocationProbabilityForSample();
			// double predictedVal = predictor.predictMaxProbForSample();
			// double sampleVal = sample.getObsCategory();
			// int inTopTen = predictor.isSampleInTopTen();
			// int inTopTenEach = baselines.inTopTenMultProbForEachCity(sample);
			// int inTopTenAll = baselines.inTopTenMultProbAcrossAllCities(sample);

			CategoryPredictorDDCRP categoryPredictorDDCRP = new CategoryPredictorDDCRP(p, l, sample, samplerStatePosteriorDensities, samplerStateThetas);;
			// double probCorrectDDCRP = categoryPredictorDDCRP.computeProbabilityForSample();
			double probCorrectDDCRP = 0.0;
			double probCorrectMapDDCRP = categoryPredictorDDCRP.computeProbabilityForSampleMAP(sMAP, thetaMAP);

			// ArrayList<TestSample> inCitySamples = testSamplesByCity.get(sample.getListIndex());
			// LocationPredictorDDCRF locationPredictorDDCRF = new LocationPredictorDDCRF(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			// double ddCRFLocation = locationPredictorDDCRF.computeLocationProbabilityForSample();

			System.out.println(probCorrectDDCRP + "," + probCorrectMapDDCRP);

			// System.out.println(probCorrectDDCRF + "," + probCorrectDDCRF + "," + multAll + "," + multEach + "," + ddCRFLocation + "," + predictedVal + "," + sampleVal + "," + inTopTen + "," + inTopTenEach + "," + inTopTenAll);
		}		

	}

	public static void doDDCRF(int numIter, HyperParameters h, HashSet<TestSample> testSamples) {
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();	
		SamplerStateTracker.initializeSamplerState(list_observations);
		Likelihood l = new DirichletLikelihood(h);

		SamplerStateTracker.max_iter = numIter;
		System.out.println("Gibbs Sampler will run for "+SamplerStateTracker.max_iter+" iterations.");
				
		//do sampling		
		long init_time = System.currentTimeMillis();
		for(int i=1;i<=SamplerStateTracker.max_iter;i++)
		{
			long init_time_iter = System.currentTimeMillis();
			GibbsSampler.doSampling(l, i>1, testSamples);
			System.out.println("----------------------");
			System.out.println("Iteration "+i+" done");
			System.out.println("Took "+(System.currentTimeMillis() - init_time_iter)/(double)1000+" seconds");
			SamplerStateTracker.returnCurrentSamplerState().prettyPrint(System.out);
			double posteriorLogPrior = SamplerStateTracker.returnCurrentSamplerState().getLogPosteriorDensity(l);
			System.out.println("Posterior log density: " + posteriorLogPrior);

			double logLik = l.computeFullLogLikelihood(SamplerStateTracker.returnCurrentSamplerState());
			System.out.println("Log likelihood: " + logLik);
			System.out.println("----------------------");
		}
		
		long diff = System.currentTimeMillis() - init_time; 
		System.out.println("Time taken for Sampling "+(double)diff/1000+" seconds");		
		/*for(int i=0;i<list_observations.size();i++)
			Util.printTableConfiguration(i, new PrintStream("tables/table_configuration"+i+".txt"));*/
		

		Posterior p = new Posterior(0, h);
		SamplerState sMAP = p.getMapEstimateDensity(l);
		System.out.println("----------------------");
		System.out.println("FINAL STATE");
		System.out.println("----------------------");		
		sMAP.prettyPrint(System.out);

		//Printing the output csv file
		Util.outputCSVforMap(sMAP);

		ThetaDDCRF t = new ThetaDDCRF(sMAP, h);
		t.estimateThetas();
		Util.outputTopKWordsPerTopic(t, 15);


		// Run a test

		Baselines baselines = new Baselines(testSamples);
		baselines.fitMultinomialAcrossAllCities();
		baselines.fitMultinomialForEachCity();

	  // store the values of the SamplerState densities for future use
	  HashMap<Integer, Double> samplerStatePosteriorDensities = new HashMap<Integer, Double>();

	  // store the values of the SamplerState theta for future use
	  HashMap<Integer, Theta> samplerStateThetas = new HashMap<Integer, Theta>();

    ArrayList<SamplerState> states = SamplerStateTracker.samplerStates;
    for (Integer i=0; i<states.size(); i++) {
    	SamplerState s = states.get(i);
      double logPosteriorDensity = s.getLogPosteriorDensity(l);
      samplerStatePosteriorDensities.put(i, logPosteriorDensity);
      ThetaDDCRF theta = new ThetaDDCRF(s, l.getHyperParameters());
      theta.estimateThetas();
      samplerStateThetas.put(i, theta);
    }
    ThetaDDCRF thetaMAP = new ThetaDDCRF(sMAP, l.getHyperParameters());
		thetaMAP.estimateThetas();

		System.out.println("Running a test");
		System.out.println("ddCRF, ddCRF-MAP, Mult-All, Mult-Each, ddCRF-Location, Prediction, True, InTopTen, InTopTenEach, InTopTenAll");

		// Gather the test samples by city, for the location prediciton task
		HashMap<Integer, ArrayList<TestSample>> testSamplesByCity = new HashMap<Integer, ArrayList<TestSample>>();
		for (TestSample sample : testSamples) {
			Integer listIndex = sample.getListIndex();
			if (testSamplesByCity.get(listIndex) == null)
				testSamplesByCity.put(listIndex, new ArrayList<TestSample>());
			ArrayList<TestSample> citySamples = testSamplesByCity.get(listIndex);
			citySamples.add(sample);
			testSamplesByCity.put(listIndex, citySamples);
		}

		for (TestSample sample : testSamples) {
			// Predictor predictor = new Predictor(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			// double ddCRF = predictor.computeProbabilityForSample();
			// double ddCRFMap = predictor.computeProbabilityForSampleMAP(sMAP, thetaMAP);
			// double multAll = baselines.predictMultProbAcrossAllCities(sample);
			// double multEach =  baselines.predictMultProbForEachCity(sample);
			// double ddCRFLocation = predictor.computeLocationProbabilityForSample();
			// double predictedVal = predictor.predictMaxProbForSample();
			// double sampleVal = sample.getObsCategory();
			// int inTopTen = predictor.isSampleInTopTen();
			// int inTopTenEach = baselines.inTopTenMultProbForEachCity(sample);
			// int inTopTenAll = baselines.inTopTenMultProbAcrossAllCities(sample);

			CategoryPredictorDDCRF categoryPredictorDDCRF = new CategoryPredictorDDCRF(p, l, sample, samplerStatePosteriorDensities, samplerStateThetas);;
			double probCorrectDDCRF = categoryPredictorDDCRF.computeProbabilityForSample();
			double probCorrectMapDDCRF = categoryPredictorDDCRF.computeProbabilityForSampleMAP(sMAP, thetaMAP);

			ArrayList<TestSample> inCitySamples = testSamplesByCity.get(sample.getListIndex());
			LocationPredictorDDCRF locationPredictorDDCRF = new LocationPredictorDDCRF(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			double ddCRFLocation = locationPredictorDDCRF.computeLocationProbabilityForSample();

			System.out.println(probCorrectDDCRF + "," + probCorrectMapDDCRF + "," + ddCRFLocation);

			// System.out.println(probCorrectDDCRF + "," + probCorrectDDCRF + "," + multAll + "," + multEach + "," + ddCRFLocation + "," + predictedVal + "," + sampleVal + "," + inTopTen + "," + inTopTenEach + "," + inTopTenAll);
		}		


	}

}
