package test;

/**
 * Represents a venue in the test data
 * @author rajarshd
 *
 */
public class TestSample {

  private int listIndex; // city of the venue
  private int obsIndex;  // index of the observation within the list
  private double obsCategory; // the observed category of the venue  QUESTION: WHY DOUBLE?

  public TestSample(int listIndex, int obsIndex, double obsCategory)
  {
    this.listIndex = listIndex;
    this.obsIndex = obsIndex;
    this.obsCategory = obsCategory;
  }

  public int getObsIndex() { return obsIndex; }
  public int getListIndex() { return listIndex; }
  public Double getObsCategory() { return obsCategory; }

  public String toJson() {
    return "{" + "listIndex:" + String.valueOf(listIndex) + "," +
                 "obsIndex:" + String.valueOf(obsIndex) + "," +
                 "obsCategory:" + String.valueOf(obsCategory) + "}";
  }


  @Override
  // WARNING: we only check listIndex and cityIndex for equality of a TestSample
  public boolean equals(Object obj) 
  {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof TestSample))return false;  
    TestSample s = (TestSample) obj;
    return ( s.listIndex == listIndex && s.obsIndex == obsIndex );
  }

  /**
   *
   */
  @Override
  // WARNING: we only check listIndex and cityIndex for equality of a TestSample
  public int hashCode() {
    String s = listIndex + ":" + obsIndex;
    return s.hashCode();
  }

}
