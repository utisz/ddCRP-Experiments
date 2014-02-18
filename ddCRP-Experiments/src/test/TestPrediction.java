package test;

/**
 * Represents a prediction for a venue in the test data
 * @author Justin Cranshaw
 *
 */
public class TestPrediction {

  private int cityIndex; //city of the venue
  private int listIndex; //index of the venue within the list of venues in a city
  private double venueCategory; // the observed category of the venue  QUESTION: WHY DOUBLE?
  private double predictedVenueCategory; // the predicted value by the classifier
    
  public TestPrediction(int listIndex, int cityIndex, double venueCategory, double predictedVenueCategory)
  {
    this.listIndex = listIndex;
    this.cityIndex = cityIndex;
    this.venueCategory = venueCategory;
    this.predictedVenueCategory = predictedVenueCategory;
  }

  public int getCityIndex() { return this.cityIndex; }
  public int getListIndex() { return this.listIndex; }
  public double getVenueCategory() { return this.venueCategory; }
  public double getPredictedVenueCategory() { return this.predictedVenueCategory; }

}
