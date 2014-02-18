package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class PrintCSV {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader("Data/cities_sim/city_87_venue_ids.txt"));
			ArrayList<String> venues = new ArrayList<String>();
			String line;					
			while((line = reader.readLine())!=null) //each line represents a city or a document etc....
			{		
				venues.add(line);				 
			}
			reader = new BufferedReader(new FileReader("tables/table_configuration86.txt"));
			PrintStream out = new PrintStream("staten_island_clusers.csv");
			int counter = 0;
			while((line = reader.readLine())!=null) //each line represents a city or a document etc....
			{		
				String[] split =	 line.split("\t");
				String venue_ids = split[1];
				split = venue_ids.split(",");								
				for(int i=0;i<split.length;i++)
				{
					int venue_index = Integer.parseInt(split[i]);
					out.println(venues.get(venue_index)+","+counter);
					
				}
				counter++;
			}
			out.close();
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		}catch(IOException ex){
			ex.printStackTrace();
		}

	}

}
