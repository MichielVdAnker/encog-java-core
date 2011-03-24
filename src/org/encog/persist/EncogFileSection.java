package org.encog.persist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.encog.app.analyst.AnalystError;
import org.encog.mathutil.matrices.Matrix;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.NumberList;

public class EncogFileSection {
	
	private final String sectionName;
	private final String subSectionName;
	private final List<String> lines = new ArrayList<String>();
	
	public EncogFileSection(String sectionName, String subSectionName) {
		super();
		this.sectionName = sectionName;
		this.subSectionName = subSectionName;
	}
	
	public String getSectionName() {
		return sectionName;
	}
	
	public String getSubSectionName() {
		return subSectionName;
	}
	
	public List<String> getLines() {
		return lines;
	}

	public Map<String, String> parseParams() {
		Map<String,String> result = new HashMap<String,String>();

		for(String line: this.lines) {
			line = line.trim();
			if(line.length()>0 ) {
				int idx = line.indexOf('=');
				if( idx==-1 ) {
					throw new AnalystError("Invalid setup item: " + line);
				}
				String name = line.substring(0,idx).trim();
				String value = line.substring(idx+1).trim();
								
				result.put(name, value);
			}
		}
		
		return result;
	}
	
	public static int parseInt(Map<String,String> params, String name) {
		String value = null;
		try {
			value = params.get(name);
			if( value==null ) {
				throw new PersistError("Missing property: " + name);
			}
			
			return Integer.parseInt(value);
			
		} catch(NumberFormatException ex) {
			throw new PersistError("Field: " + name + ", " + "invalid integer: " + value);
		}
	}
	
	public static double parseDouble(Map<String,String> params, String name) {
		String value = null;
		try {
			value = params.get(name);
			if( value==null ) {
				throw new PersistError("Missing property: " + name);
			}
			
			return CSVFormat.EG_FORMAT.parse(value);
			
		} catch(NumberFormatException ex) {
			throw new PersistError("Field: " + name + ", " + "invalid integer: " + value);
		}
	}

	public static Matrix parseMatrix(Map<String, String> params,
			String name) {
		
		if( !params.containsKey(name) ) {
			throw new PersistError("Missing property: " + name );
		}
		
		String line = params.get(name);

		double[] d = NumberList.fromList(CSVFormat.EG_FORMAT, line);
		int rows = (int)d[0];
		int cols = (int)d[1];
		
		Matrix result = new Matrix(rows,cols);
		
		int index = 2;
		for(int r = 0;r<rows;r++)
		{
			for(int c = 0; c< cols; c++)
			{
				result.set(r,c,d[index++]);
			}
		}
		
		return result;
	}
	
}