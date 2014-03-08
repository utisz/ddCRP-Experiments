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
import test.TestResult;
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
	 * The prediction results will be logged in this directory.
	 */
	public static String outputDir;

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
	
			//get the type of sampling to be done
			int sampleType = Integer.parseInt(args[5]);
			int numSamples = Integer.parseInt(args[6]);
			HashSet<TestSample> testSamples = null;
			if(sampleType == 1) //uniform across venues
			{
				// generate some test samples
				TestUniform test = new TestUniform(numSamples);
				test.generateTestSamples();
				testSamples = test.getTestSamplesSet();
			}
			else if(sampleType == 2) //uniform across space
			{
				TestUniformSpace test = new TestUniformSpace(numSamples);
				test.generateTestSamples();
				testSamples = test.getTestSamplesSet();
			}
			else if(sampleType == 3) //uniform across category
			{
				TestUniformCategory test = new TestUniformCategory(numSamples);
				test.generateTestSamples();
				testSamples = test.getTestSamplesSet();
			}

			outputDir = args[7];
			
			int numIter = Integer.parseInt(args[0]);
			
			// set the output directory based on the parameters
			//Util.setOutputDirectoryFromArgs(numIter, dirichlet_param, alpha, crp_alpha, test);
			// Util.outputTestMeta();

			doBaseLines(testSamples);
			//doDDCRP(numIter, h, testSamples);
			//clearing the sampler state
			//SamplerStateTracker.samplerStates = new ArrayList<SamplerState>();
			//set the current-iter to 0
			//SamplerStateTracker.current_iter = 0;
			//doDDCRF(numIter, h, testSamples);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	/**
	 * Baseline results
	 * @param testSamples
	 */
	public static void doBaseLines(HashSet<TestSample> testSamples) throws FileNotFoundException
	{
		// Run a test
		Baselines baselines = new Baselines(testSamples);
		baselines.fitMultinomialAcrossAllCities();
		baselines.fitMultinomialForEachCity();
		
		//prep the output files
		String fileNameBaseLineAll = "BaseLineAll_Results.csv";
		String fileNameBaseLineEach = "BaseLineEach_Results.csv";
		PrintStream outBaseLineAll = new PrintStream(outputDir+fileNameBaseLineAll);
		PrintStream outBaseLineEach = new PrintStream(outputDir+fileNameBaseLineEach);
		//print HEADER
		TestResult.printHeader(outBaseLineAll);
		TestResult.printHeader(outBaseLineEach);
		
		//iterate over test samples and do prediction
		for (TestSample sample : testSamples) {
			
			double multAll = baselines.predictMultProbAcrossAllCities(sample);
			double multEach =  baselines.predictMultProbForEachCity(sample);
			int inTopTenEach = baselines.inTopTenMultProbForEachCity(sample); // next two are baselines of that
			int inTopTenAll = baselines.inTopTenMultProbAcrossAllCities(sample);
			
		
			TestResult result = new TestResult();
			result.setSample(sample); //setting the test sample
			
			result.setCorrectCategoryPredictionProb(multAll);
			result.setInTopTen(inTopTenAll);
			//Have to implement PredictedCategory and isPredictedCategoryCorrect
			
			result.printTestResults(outBaseLineAll);
			
			//not setting the location results since they dont apply
			result = new TestResult();
			result.setSample(sample); //setting the test sample
			result.setCorrectCategoryPredictionProb(multEach);
			result.setInTopTen(inTopTenEach);
			//Have to implement PredictedCategory and isPredictedCategoryCorrect
			
			result.printTestResults(outBaseLineEach);
			
			//not setting the location results since they dont apply
			
		}
		outBaseLineAll.close();
		outBaseLineEach.close();
	}

	public static void doDDCRP(int numIter, HyperParameters h, HashSet<TestSample> testSamples) throws FileNotFoundException {
		
		System.out.println("Starting DDCRP");
		
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
			System.out.println("---------------------	-");
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
	
		//System.out.println("ddCRF, ddCRF-MAP, Mult-All, Mult-Each, ddCRF-Location, Prediction, True, InTopTen, InTopTenEach, InTopTenAll");
		
		//prepping the output file
		String fileNameDDCRP = "DDCRP_Results.csv";
		String fileNameDDCRPMAP = "DDCRP_MAP_Results.csv";
		PrintStream outDDCRP = new PrintStream(outputDir+fileNameDDCRP);
		PrintStream outDDCRPMAP = new PrintStream(outputDir+fileNameDDCRPMAP);
		//print HEADER
		TestResult.printHeader(outDDCRP);
		TestResult.printHeader(outDDCRPMAP);
		
		
		
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
			
			//At this point we will have all the prediction results
			TestResult result = new TestResult();
			result.setSample(sample); //setting the test sample
			
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectDDCRP);
			
			//These are the things which are needed to be filled up still
			/**
			 *
			 * result.setPredictedCategory(predictedVal);
			result.setPredictedCategoryCorrect(isPredictedValCorrect);
			result.setInTopTen(inTopTen);
			//setting the location prediction results
			result.setCorrectLocationPredictionProb(ddCRFLocation);
			//result.setPredictedLocation(predictedLocation);
			//result.setPredictedLocationCorrect(isPredictedLocationCorrect);
			
			*/
			//setting the location prediction results
			//TIME TO PRINT
			result.printTestResults(outDDCRP);
			
			result = new TestResult();
			result.setSample(sample); //setting the test sample
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectMapDDCRP);
			/* Need to implement this still
			result.setPredictedCategory(predictedVal);
			result.setPredictedCategoryCorrect(isPredictedValCorrect);
			result.setInTopTen(inTopTen);
			*/
			
			//setting the location prediction results
			
			
			//These are the things which are needed to be filled up still
			/**
			 *
			 * result.setCorrectLocationPredictionProb(ddCRMAPFLocation);
			//result.setPredictedLocation(predictedLocation);
			//result.setPredictedLocationCorrect(isPredictedLocationCorrect);
			
			*/
			
			result.printTestResults(outDDCRPMAP);
			
			
			
			// System.out.println(probCorrectDDCRF + "," + probCorrectDDCRF + "," + multAll + "," + multEach + "," + ddCRFLocation + "," + predictedVal + "," + sampleVal + "," + inTopTen + "," + inTopTenEach + "," + inTopTenAll);
			
		}		
		outDDCRP.close();
	}

	public static void doDDCRF(int numIter, HyperParameters h, HashSet<TestSample> testSamples) throws FileNotFoundException {
		
		System.out.println("Start of DDCRF...");
		
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();	
		SamplerStateTracker.initializeSamplerState(list_observations);
		Likelihood l = new DirichletLikelihood(h);

		SamplerStateTracker.max_iter = numIter;
		System.out.println("DDCRF Gibbs Sampler will run for "+SamplerStateTracker.max_iter+" iterations.");
				
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
		//System.out.println("ddCRF, ddCRF-MAP, Mult-All, Mult-Each, ddCRF-Location, Prediction, True, InTopTen, InTopTenEach, InTopTenAll");

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

		//prepping the output filea
		String fileNameDDCRF = "DDCRF_Results.csv";
		String fileNameDDCRFMAP = "DDCRF_MAP_Results.csv";
		PrintStream outDDCRF = new PrintStream(outputDir+fileNameDDCRF);
		PrintStream outDDCRFMAP = new PrintStream(outputDir+fileNameDDCRFMAP);
		//print HEADER
		TestResult.printHeader(outDDCRF);
		TestResult.printHeader(outDDCRFMAP);
		
		for (TestSample sample : testSamples) {
			// Predictor predictor = new Predictor(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			// double ddCRF = predictor.computeProbabilityForSample(); //predicted prob. of correct category
			// double ddCRFMap = predictor.computeProbabilityForSampleMAP(sMAP, thetaMAP); //same as above -- just taking the MAP estimate
			// double multAll = baselines.predictMultProbAcrossAllCities(sample);
			// double multEach =  baselines.predictMultProbForEachCity(sample);
			// double ddCRFLocation = predictor.computeLocationProbabilityForSample(); //normalized prob of the venue at the correct location.
			// double predictedVal = predictor.predictMaxProbForSample();// will return the catedory for the max prob
			// double sampleVal = sample.getObsCategory(); //correct category of the sample; to be compared to predictedVal
			// int inTopTen = predictor.isSampleInTopTen(); // binary whether in top 10 category
			// int inTopTenEach = baselines.inTopTenMultProbForEachCity(sample); // next two are baselines of that
			// int inTopTenAll = baselines.inTopTenMultProbAcrossAllCities(sample);
			
			//Additionally we need to log - real category and city and venue_id (Done)
			//Also lets implement a predictedVal for baselines multAll and multEach
			//We also have to implement the predictedLocation	 -- to compute the number of times we predicted the correct location.
			//Also put information such as num_iterations
			
			/**
			 * ddCRF-Location -- normalized prob of the correct category at the correct location -- we want to implement whether this prob is the max across location
			 * Prediction -- this returns the category of the most probable category
			 *  
			 */

			CategoryPredictorDDCRF categoryPredictorDDCRF = new CategoryPredictorDDCRF(p, l, sample, samplerStatePosteriorDensities, samplerStateThetas);;
			double probCorrectDDCRF = categoryPredictorDDCRF.computeProbabilityForSample(); //predicted prob. of correct category
			double probCorrectMapDDCRF = categoryPredictorDDCRF.computeProbabilityForSampleMAP(sMAP, thetaMAP); //same as above -- just taking the MAP estimate
			//adding in the baselines
			
			double predictedVal = categoryPredictorDDCRF.predictMaxProbForSample();
			//double predictedMAPVal = THIS NEEDS TO BE IMPLEMENTED 
			double sampleVal = sample.getObsCategory(); //correct category of the sample; to be compared to predictedVal
			boolean isPredictedValCorrect = false;
			if(predictedVal == sampleVal)
				isPredictedValCorrect = true;
			boolean isPredictedMapValCorrect = false; //THIS NEEDS TO BE IMPLEMENTED
			
			
			int inTopTen = categoryPredictorDDCRF.isSampleInTopTen(); // binary whether in top 10 category
			
			
			ArrayList<TestSample> inCitySamples = testSamplesByCity.get(sample.getListIndex());
			LocationPredictorDDCRF locationPredictorDDCRF = new LocationPredictorDDCRF(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			double ddCRFLocation = locationPredictorDDCRF.computeLocationProbabilityForSample(); //normalized prob of the venue at the correct location.

			System.out.println(probCorrectDDCRF + "," + probCorrectMapDDCRF + "," + ddCRFLocation);

			// System.out.println(probCorrectDDCRF + "," + probCorrectDDCRF + "," + multAll + "," + multEach + "," + ddCRFLocation + "," + predictedVal + "," + sampleVal + "," + inTopTen + "," + inTopTenEach + "," + inTopTenAll);
			
			//At this point we will have all the prediction results
			TestResult result = new TestResult();
			result.setSample(sample); //setting the test sample
			
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectDDCRF);
			result.setPredictedCategory(predictedVal);
			result.setPredictedCategoryCorrect(isPredictedValCorrect);
			result.setInTopTen(inTopTen);
			
			//setting the location prediction results
			result.setCorrectLocationPredictionProb(ddCRFLocation);
			
			//These are the things which are needed to be filled up still
			/**
			 * 
			//result.setPredictedLocation(predictedLocation);
			//result.setPredictedLocationCorrect(isPredictedLocationCorrect);
			
			*/
			//TIME TO PRINT
			result.printTestResults(outDDCRF);
			
			result = new TestResult();
			result.setSample(sample); //setting the test sample
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectMapDDCRF);
			/* Need to implement this still
			result.setPredictedCategory(predictedVal);
			result.setPredictedCategoryCorrect(isPredictedValCorrect);
			result.setInTopTen(inTopTen);
			*/
			
			//setting the location prediction results
			
			
			//These are the things which are needed to be filled up still
			/**
			 *
			 * result.setCorrectLocationPredictionProb(ddCRMAPFLocation);
			//result.setPredictedLocation(predictedLocation);
			//result.setPredictedLocationCorrect(isPredictedLocationCorrect);
			
			*/
			
			result.printTestResults(outDDCRFMAP);
			
			
		}		
		outDDCRF.close();
		outDDCRFMAP.close();

	}

}
