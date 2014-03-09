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
import test.LocationPredictorDDCRP;
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
			doDDCRP(numIter, h, testSamples);
			//clearing the sampler state
			SamplerStateTracker.samplerStates = new ArrayList<SamplerState>();
			//set the current-iter to 0
			SamplerStateTracker.current_iter = 0;
			doDDCRF(numIter, h, testSamples);

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
			double predictedCategoryMultAll = baselines.predictMaxProbForSampleAcrossAllCities(sample);
			double predictedCategoryMultEach = baselines.predictMaxProbForSampleForEachCity(sample);
			double correctCategory = sample.getObsCategory();
			int isPredictedCatCorrectMultAll = 0;
			int isPredictedCatCorrectMultEach = 0;
			if(correctCategory == predictedCategoryMultAll)
			{
				isPredictedCatCorrectMultAll = 1;
			}
			if(correctCategory == predictedCategoryMultEach)
			{
				isPredictedCatCorrectMultEach = 1;
			}
		
			TestResult result = new TestResult();
			result.setSample(sample); //setting the test sample
			
			result.setCorrectCategoryPredictionProb(multAll);
			result.setInTopTen(inTopTenAll);
			result.setPredictedCategory(predictedCategoryMultAll);
			result.setPredictedCategoryCorrect(isPredictedCatCorrectMultAll);
			
			result.printTestResults(outBaseLineAll);
			
			//not setting the location results since they dont apply
			result = new TestResult();
			result.setSample(sample); //setting the test sample
			result.setCorrectCategoryPredictionProb(multEach);
			result.setInTopTen(inTopTenEach);
			result.setPredictedCategory(predictedCategoryMultEach);
			result.setPredictedCategoryCorrect(isPredictedCatCorrectMultEach);
			
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
	  
	  ArrayList<SamplerState> states = SamplerStateTracker.samplerStates;
	    for (Integer i=0; i<states.size(); i++) {
	    	SamplerState s = states.get(i);
	      double logPosteriorDensity = s.getLogPosteriorDensity(l);
	      samplerStatePosteriorDensities.put(i, logPosteriorDensity);
	    }
	  
	  ThetaDDCRP thetaMAP = new ThetaDDCRP(sMAP, l.getHyperParameters());
		thetaMAP.estimateThetas();

		System.out.println("Running a test");
		
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
		
		//prepping the output file
		String fileNameDDCRP = "DDCRP_Results.csv";
		String fileNameDDCRPMAP = "DDCRP_MAP_Results.csv";
		PrintStream outDDCRP = new PrintStream(outputDir+fileNameDDCRP);
		PrintStream outDDCRPMAP = new PrintStream(outputDir+fileNameDDCRPMAP);
		//print HEADER
		TestResult.printHeader(outDDCRP);
		TestResult.printHeader(outDDCRPMAP);

		for (TestSample sample : testSamples) {
			
			CategoryPredictorDDCRP categoryPredictorDDCRP = new CategoryPredictorDDCRP(p, l, sample, samplerStatePosteriorDensities, samplerStateThetas);;
			double probCorrectDDCRP = categoryPredictorDDCRP.computeProbabilityForSample();
			double probCorrectMapDDCRP = categoryPredictorDDCRP.computeProbabilityForSampleMAP(sMAP);
			double predictedCategory = categoryPredictorDDCRP.predictMaxProbForSample();
			double predictedCategoryMAP = categoryPredictorDDCRP.predictMaxProbForSampleMAP();
			double correctCategory = sample.getObsCategory();
			int isPredictedCatCorrect = 0;
			int isPredictedCatCorrectMAP = 0;
			if(correctCategory == predictedCategory)
			{
				isPredictedCatCorrect = 1;
			}
			if(correctCategory == predictedCategoryMAP)
			{
				isPredictedCatCorrectMAP = 1;
			}
			int inTopTen = categoryPredictorDDCRP.isSampleInTopTen();
			int inTopTenMap = categoryPredictorDDCRP.isSampleInTopTenMAP();
			

			ArrayList<TestSample> inCitySamples = testSamplesByCity.get(sample.getListIndex());
			LocationPredictorDDCRP locationPredictorDDCRP = new LocationPredictorDDCRP(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			double ddCRPLocationProb = locationPredictorDDCRP.computeLocationProbabilityForSample();
			double ddCRPLocationProbMAP = locationPredictorDDCRP.computeLocationProbabilityForSampleMAP();
			
			int predictedLocation = locationPredictorDDCRP.predictMaxProbForLocations();
			int predictedLocationMAP = locationPredictorDDCRP.predictMaxProbForLocationsMAP();
			int correctLoc = sample.getObsIndex();
			int isPredictedLocationCorrect = 0;
			int isPredictedLocationCorrectMAP = 0;
			
			if(predictedLocation == correctLoc)
				isPredictedLocationCorrect = 1;
			
			if(predictedLocationMAP == correctLoc)
				isPredictedLocationCorrectMAP = 1;
			
			//At this point we will have all the prediction results
			TestResult result = new TestResult();
			result.setSample(sample); //setting the test sample
			
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectDDCRP);
			result.setPredictedCategory(predictedCategory);
			result.setInTopTen(inTopTen);
			result.setPredictedCategoryCorrect(isPredictedCatCorrect);
			result.setCorrectLocationPredictionProb(ddCRPLocationProb);
			result.setPredictedLocation(predictedLocation);
			result.setPredictedLocationCorrect(isPredictedLocationCorrect);
			
			//setting the location prediction results
			//TIME TO PRINT
			result.printTestResults(outDDCRP);
			
			result = new TestResult();
			result.setSample(sample); //setting the test sample
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectMapDDCRP);
			result.setPredictedCategory(predictedCategoryMAP);
			result.setInTopTen(inTopTenMap);
			result.setPredictedCategoryCorrect(isPredictedCatCorrectMAP);
			result.setCorrectLocationPredictionProb(ddCRPLocationProbMAP);
			result.setPredictedLocation(predictedLocationMAP);
			result.setPredictedLocationCorrect(isPredictedLocationCorrectMAP);
			
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
		outDDCRPMAP.close();
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
			double probCorrectMapDDCRF = categoryPredictorDDCRF.computeProbabilityForSampleMAP(sMAP); //same as above -- just taking the MAP estimate
			//adding in the baselines

			double predictedCategory = categoryPredictorDDCRF.predictMaxProbForSample();
			double predictedCategoryMAP = categoryPredictorDDCRF.predictMaxProbForSampleMAP();
			double correctCategory = sample.getObsCategory(); //correct category of the sample; to be compared to predictedVal
			int isPredictedCatCorrect = 0;
			if(predictedCategory == correctCategory)
				isPredictedCatCorrect = 1;
			int isPredictedCatCorrectMAP = 0;
			if(correctCategory == predictedCategoryMAP)
			{
				isPredictedCatCorrectMAP = 1;
			}
			int inTopTen = categoryPredictorDDCRF.isSampleInTopTen();
			int inTopTenMap = categoryPredictorDDCRF.isSampleInTopTenMAP();
			
			ArrayList<TestSample> inCitySamples = testSamplesByCity.get(sample.getListIndex());
			LocationPredictorDDCRF locationPredictorDDCRF = new LocationPredictorDDCRF(p, l, sample, inCitySamples, samplerStatePosteriorDensities, samplerStateThetas);
			
			double ddCRFLocationProb = locationPredictorDDCRF.computeLocationProbabilityForSample();
			double ddCRFLocationProbMAP = locationPredictorDDCRF.computeLocationProbabilityForSampleMAP();
			
			int predictedLocation = locationPredictorDDCRF.predictMaxProbForLocations();
			int predictedLocationMAP = locationPredictorDDCRF.predictMaxProbForLocationsMAP();
			int correctLoc = sample.getObsIndex();
			int isPredictedLocationCorrect = 0;
			int isPredictedLocationCorrectMAP = 0;
			
			if(predictedLocation == correctLoc)
				isPredictedLocationCorrect = 1;
			
			if(predictedLocationMAP == correctLoc)
				isPredictedLocationCorrectMAP = 1;
	
			//At this point we will have all the prediction results
			
			TestResult result = new TestResult();
			result.setSample(sample); //setting the test sample
			
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectDDCRF);
			result.setPredictedCategory(predictedCategory);
			result.setPredictedCategoryCorrect(isPredictedCatCorrect);
			result.setInTopTen(inTopTen);
			
			//setting the location prediction results
			result.setCorrectLocationPredictionProb(ddCRFLocationProb);
			result.setPredictedLocation(predictedLocation);
			result.setPredictedLocationCorrect(isPredictedLocationCorrect);
			
			//TIME TO PRINT
			result.printTestResults(outDDCRF);
			
			result = new TestResult();
			result.setSample(sample); //setting the test sample
			//setting the category prediction result
			result.setCorrectCategoryPredictionProb(probCorrectMapDDCRF);
			result.setPredictedCategory(predictedCategoryMAP);
			result.setPredictedCategoryCorrect(isPredictedCatCorrectMAP);
			result.setInTopTen(inTopTenMap);
			
			//setting the location prediction results
			result.setCorrectLocationPredictionProb(ddCRFLocationProbMAP);
			result.setPredictedLocation(predictedLocationMAP);
			result.setPredictedLocationCorrect(isPredictedLocationCorrectMAP);
			
			//setting the location prediction results			
			result.printTestResults(outDDCRFMAP);
			
			
		}		
		outDDCRF.close();
		outDDCRFMAP.close();

	}

}
