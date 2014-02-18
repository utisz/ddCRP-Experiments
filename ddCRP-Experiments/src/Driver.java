import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import model.HyperParameters;
import model.SamplerStateTracker;
import model.SamplerState;
import model.Theta;
import model.Posterior;
import sampler.GibbsSampler;
import util.Util;
import Likelihood.DirichletLikelihood;
import Likelihood.Likelihood;
import data.Data;
import test.TestUniform;
import test.Predictor;
import test.TestSample;

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
			TestUniform test = new TestUniform(10);
			test.generateTestSamples();
			HashSet<TestSample> testSamples = test.getTestSamplesSet();

			int numIter = Integer.parseInt(args[0]);

			// set the output directory based on the parameters
			Util.setOutputDirectoryFromArgs(numIter, dirichlet_param, alpha, crp_alpha, test);
			// Util.outputTestMeta();

			doDDCRF(numIter, h, testSamples);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	public void doDDCRP(ArrayList<ArrayList<TestSample>> testSamples) {

	}

	public static void doDDCRF(int numIter, HyperParameters h, HashSet<TestSample> testSamples) {
		ArrayList<ArrayList<Double>> list_observations = Data.getObservations();	
		SamplerStateTracker.initializeSamplerState(list_observations);
		Likelihood l = new DirichletLikelihood(h);
		// TODO FIX THIS!!!!!!!
		// l.setTestSamples(new HashSet<TestSample>(testSamples));		

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

		Theta t = new Theta(sMAP, h);
		t.estimateThetas();
		Util.outputTopKWordsPerTopic(t, 15);

		// Run a test
	  // store the values of the SamplerState densities for future use
	  HashMap<SamplerState, Double> samplerStatePosteriorDensities = new HashMap<SamplerState, Double>();

	  // store the values of the SamplerState theta for future use
	  HashMap<SamplerState, Theta> samplerStateThetas = new HashMap<SamplerState, Theta>();

    ArrayList<SamplerState> states = SamplerStateTracker.samplerStates;
    for (SamplerState s : states) {
      double logPosteriorDensity = s.getLogPosteriorDensity(l);
      samplerStatePosteriorDensities.put(s, logPosteriorDensity);
      Theta theta = new Theta(s, l.getHyperParameters());
      theta.estimateThetas();
      samplerStateThetas.put(s, theta);
    }

		System.out.println("Running a test");
		for (TestSample sample : testSamples) {
			Predictor predictor = new Predictor(p, l, sample, samplerStatePosteriorDensities, samplerStateThetas);
			System.out.println("  predicted probability of true observation: " + predictor.computeProbabilityForSample());
		}		
	}

}