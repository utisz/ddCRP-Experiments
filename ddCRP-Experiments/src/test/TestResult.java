package test;

import java.io.PrintStream;

import test.TestSample;

/**
 * Class representing the result of a test sample
 * @author rajarshd
 *
 */
public class TestResult {

	/**
	 * The sample who test results are these
	 */
	TestSample sample;
	
	/**
	 * Category prediction tasks
	 */
	double correctCategoryPredictionProb;    // prediction prob of correct category
	//double correctCategoryPredictionProbMAP; //taking the MAP states
	//double correctCategoryPredictionMultAll; //The two baselines
	//double correctCategoryPredictionMultEach;
	double predictedCategory; //corresponding category for the max prob of the predicted multinomial
	//double predictedMAPCategory; //corresponding category for the max prob of the predicted MAP multinomial
	boolean isPredictedCategoryCorrect; 
	//boolean isPredictedMAPCategoryCorrect; 
	int inTopTen;
	//int inTopTenEach;
	//int inTopTenAll;
	
	
	/**
	 * Location Prediction tasks
	 */
	double correctLocationPredictionProb;
	//double correctLocationPredictionProbMAP;
	double predictedLocation; //corresponding location for the max prob of the predicted multinomial
	//double predictedMAPLocation;//corresponding location for the max prob of the predicted MAP multinomial
	//double baseLineProb; // would be 1/{number of possible locations}.
	boolean isPredictedLocationCorrect;
	//boolean isPredictedMAPLocationCorrect;
	//boolean doWeBeatLocationBaseline;
	//boolean doWeBeatLocationBaselineMAP;
	
	static String delim = ",";
	
	/**
	 * 
	 * @param out
	 */
	public static  void printHeader(PrintStream out)
	{
		String header = "city"+delim+"index_within_city"+delim+"venue_cat"+delim;
		header=header + "correctCategoryPredictionProb" +delim +  "predictedCategory" + delim + "isPredictedCategoryCorrect" + delim + "inTopTen"+delim;
		header=header + "correctLocationPredictionProb" + delim + "predictedLocation" + delim + "isPredictedLocationCorrect";
		out.println(header);
	}
	
	/**
	 * -- NO MISTAKES HERE
	 * This prints the results object given the file descriptor
	 * venue_cat, city, index_within_city, correctCategoryPredictionMultAll, correctCategoryPredictionMultEach, correctCategoryPredictionProb, predictedCategory, isPredictedCategoryCorrect, 
	 * correctCategoryPredictionProbMAP, predictedMAPCategory, isPredictedMAPCategoryCorrect, inTopTen, inTopTenEach, inTopTenAll,
	 *now locations
	 * baseLineProb, correctLocationPredictionProb, predictedLocation, isPredictedLocationCorrect, predictedMAPLocation, isPredictedMAPLocationCorrect, doWeBeatLocationBaseline, 
	 * doWeBeatLocationBaselineMAP
	 * @param out
	 */
	public void printTestResults(PrintStream out)
	{
		String resultLine = sample.getListIndex()+delim+sample.getObsIndex() + delim + sample.getObsCategory()+delim;
		resultLine = resultLine + correctCategoryPredictionProb +delim +  predictedCategory + delim + isPredictedCategoryCorrect + delim + inTopTen + delim;
		resultLine = resultLine + correctLocationPredictionProb + delim + predictedLocation + delim + isPredictedLocationCorrect;
		out.println(resultLine);
	}
	
	
	
	public TestSample getSample() {
		return sample;
	}
	public void setSample(TestSample sample) {
		this.sample = sample;
	}
	public double getCorrectCategoryPredictionProb() {
		return correctCategoryPredictionProb;
	}
	public void setCorrectCategoryPredictionProb(
			double correctCategoryPredictionProb) {
		this.correctCategoryPredictionProb = correctCategoryPredictionProb;
	}
	/*public double getCorrectCategoryPredictionProbMAP() {
		return correctCategoryPredictionProbMAP;
	}
	public void setCorrectCategoryPredictionProbMAP(
			double correctCategoryPredictionProbMAP) {
		this.correctCategoryPredictionProbMAP = correctCategoryPredictionProbMAP;
	}
	public double getCorrectCategoryPredictionMultAll() {
		return correctCategoryPredictionMultAll;
	}
	public void setCorrectCategoryPredictionMultAll(
			double correctCategoryPredictionMultAll) {
		this.correctCategoryPredictionMultAll = correctCategoryPredictionMultAll;
	}
	public double getCorrectCategoryPredictionMultEach() {
		return correctCategoryPredictionMultEach;
	}
	public void setCorrectCategoryPredictionMultEach(
			double correctCategoryPredictionMultEach) {
		this.correctCategoryPredictionMultEach = correctCategoryPredictionMultEach;
	}*/
	public double getPredictedCategory() {
		return predictedCategory;
	}
	public void setPredictedCategory(double predictedCategory) {
		this.predictedCategory = predictedCategory;
	}
	public boolean isPredictedCategoryCorrect() {
		return isPredictedCategoryCorrect;
	}
	public void setPredictedCategoryCorrect(boolean isPredictedCategoryCorrect) {
		this.isPredictedCategoryCorrect = isPredictedCategoryCorrect;
	}
	public int getInTopTen() {
		return inTopTen;
	}
	public void setInTopTen(int inTopTen) {
		this.inTopTen = inTopTen;
	}
	/*public int getInTopTenEach() {
		return inTopTenEach;
	}
	public void setInTopTenEach(int inTopTenEach) {
		this.inTopTenEach = inTopTenEach;
	}
	public int getInTopTenAll() {
		return inTopTenAll;
	}
	public void setInTopTenAll(int inTopTenAll) {
		this.inTopTenAll = inTopTenAll;
	}*/
	public double getCorrectLocationPredictionProb() {
		return correctLocationPredictionProb;
	}
	public void setCorrectLocationPredictionProb(
			double correctLocationPredictionProb) {
		this.correctLocationPredictionProb = correctLocationPredictionProb;
	}
	/*public double getCorrectLocationPredictionProbMAP() {
		return correctLocationPredictionProbMAP;
	}
	public void setCorrectLocationPredictionProbMAP(
			double correctLocationPredictionProbMAP) {
		this.correctLocationPredictionProbMAP = correctLocationPredictionProbMAP;
	}*/
	public double getPredictedLocation() {
		return predictedLocation;
	}
	public void setPredictedLocation(double predictedLocation) {
		this.predictedLocation = predictedLocation;
	}
	/*public double getBaseLineProb() {
		return baseLineProb;
	}
	public void setBaseLineProb(double baseLineProb) {
		this.baseLineProb = baseLineProb;
	}*/
	public boolean isPredictedLocationCorrect() {
		return isPredictedLocationCorrect;
	}
	public void setPredictedLocationCorrect(boolean isPredictedLocationCorrect) {
		this.isPredictedLocationCorrect = isPredictedLocationCorrect;
	}
	/*public boolean isDoWeBeatLocationBaseline() {
		return doWeBeatLocationBaseline;
	}
	public void setDoWeBeatLocationBaseline(boolean doWeBeatLocationBaseline) {
		this.doWeBeatLocationBaseline = doWeBeatLocationBaseline;
	}*/
	
	
	
	
	
	
}
