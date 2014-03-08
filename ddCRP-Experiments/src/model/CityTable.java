package model;

/**
 * A model to hold the city id and the table_id within the city
 * @author rajarshd
 *
 */
public class CityTable {

	int cityId;
	
	int tableId;
	
	public CityTable(int cityId, int tableId)
	{
		this.cityId = cityId;
		this.tableId = tableId;
	}

	public CityTable(CityTable ct) {
		this.cityId = new Integer(ct.getCityId());
		this.tableId = new Integer(ct.getTableId());
	}

	public CityTable(){}
	
	public int getCityId() {
		return cityId;
	}
	public void setCityId(int cityId) {
		this.cityId = cityId;
	}
	public int getTableId() {
		return tableId;
	}
	public void setTableId(int tableId) {
		this.tableId = tableId;
	}
	
	/**
	 * Equals comparator for CityTable based on cityId and tableId
	 */
	@Override
	public boolean equals(Object obj) 
	{
		if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof CityTable))return false;	
		CityTable c = (CityTable) obj;
		return ((c.getCityId() == cityId) && (c.getTableId() == tableId));
	}

	/**
	 * Overrides hashCode to be unique by value for cityId and tableId
	 */
	@Override
	public int hashCode() {
		return 100000000 * cityId + tableId;  // will have collisions if there are > 100Mil tables used in a city
	}

	
}
