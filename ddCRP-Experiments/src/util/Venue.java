package util;

import java.io.PrintStream;

public class Venue {
	
	int venueId;
	String venueName;
	String venueCategory;
	int venueCategoryId;
	double lat;
	double lon;
	int cityId;
	int obsId;
	String cityName;
	int tableId;
	int topicId;
	
	public int getTableId() {
		return tableId;
	}
	public void setTableId(int tableId) {
		this.tableId = tableId;
	}
	public int getVenueId() {
		return venueId;
	}
	public void setVenueId(int venueId) {
		this.venueId = venueId;
	}
	public String getVenueName() {
		return venueName;
	}
	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}
	public String getVenueCategory() {
		return venueCategory;
	}
	public void setVenueCategory(String venueCategory) {
		this.venueCategory = venueCategory;
	}
	public int getVenueCategoryId() {
		return venueCategoryId;
	}
	public void setVenueCategoryId(int venueCategoryId) {
		this.venueCategoryId = venueCategoryId;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public int getCityId() {
		return cityId;
	}
	public void setCityId(int cityId) {
		this.cityId = cityId;
	}
	public int getObsId() {
		return obsId;
	}
	public void setObsId(int obsId) {
		this.obsId = obsId;
	}
	public String getCityName() {
		return cityName;
	}
	public void setCityName(String cityName) {
		this.cityName = cityName;
	}
	public int getTopicId() {
		return topicId;
	}
	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}
	
	public void printVenueConfig(PrintStream out)
	{
		//topicId, tableId, cityId, cityName, name, category, lat, lon  
		out.println(topicId+"\t"+tableId+"\t"+cityId+"\t"+cityName+"\t"+venueName+"\t"+venueCategory+"\t"+lat+"\t"+lon);
	}

}
