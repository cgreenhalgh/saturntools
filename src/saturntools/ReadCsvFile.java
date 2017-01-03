/**
 * 
 */
package saturntools;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

/** Utility to read a CSV file as a sort of DB/
 * @author cmg
 *
 */
public class ReadCsvFile {
	/** logger */
	static Logger logger = Logger.getLogger(ReadCsvFile.class);
	/** pseudo-column name for line number */
	public static String LINE_NUMBER_COLUMN = "linenumber";
	/** read a CSV file, take column names from header row */
	public static HashMap<String,HashMap<String,String>> readCsvFile(File file, String primaryKeyName, boolean keyToLowerCase) throws IOException {
		return readCsvFile(file, primaryKeyName, keyToLowerCase, null);
	}
	public static HashMap<String,HashMap<String,String>> readCsvFile(File file, String primaryKeyName, boolean keyToLowerCase, List<String> keysInOrder) throws IOException {
		HashMap<String,HashMap<String,String>> db = new HashMap<String,HashMap<String,String>>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String columns [] = null;
		int headerIndex = -1;
		int lineNumber = 0;
		while(true) {
			String line = br.readLine();
			if (line==null) 
				break;
			lineNumber++;
			if (line.length()==0 || line.startsWith("#") || line.startsWith(";") || line.startsWith("//"))
				// comment/empty
				continue;
			String values [] = parseCsvLine(line);
			if (columns==null) {
				columns = values;
				// treat column names as all lower case?
				//for (int i=0; i<columns.length; i++) 
				//	columns[i] = columns[i].toLowerCase();
				for (headerIndex=0; headerIndex<columns.length; headerIndex++)
					if (primaryKeyName.equals(columns[headerIndex]))
						break;
				if (headerIndex>=columns.length)
					throw new IOException("Could not find primary key "+primaryKeyName+" in file "+file+" header line: "+line);
				continue;
			}
			HashMap<String,String> record = new HashMap<String,String>();
			for (int i=0; i<values.length && i<columns.length; i++) {
				record.put(columns[i], values[i]);
				if (values.length>columns.length) {
					logger.warn("Ignoring trailing values on line "+lineNumber);
				}
				record.put(LINE_NUMBER_COLUMN, Integer.toString(lineNumber));
			}
			if (headerIndex>=values.length)
				logger.error("Line "+lineNumber+" had no primary key value - ignored: "+line);
			else {
				String key = values[headerIndex];
				if (key.length()==0 || key==null) 
					logger.error("Line "+lineNumber+" had no primary key value - ignored: "+line);
				else {
					if (keyToLowerCase)
						key = key.toLowerCase();
					db.put(key, record);
					if (keysInOrder!=null)
						keysInOrder.add(key);
				}
			}
		}
		br.close();
		if (columns==null)
			throw new IOException("Did not find header in "+file);
		return db;
	}
	/** read a CSV file, take column names from header row */
	public static List<HashMap<String,String>> readCsvFile(File file) throws IOException {
		LinkedList<HashMap<String,String>> db = new LinkedList<HashMap<String,String>>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String columns [] = null;
		//int headerIndex = -1;
		int lineNumber = 0;
		while(true) {
			String line = br.readLine();
			if (line==null) 
				break;
			lineNumber++;
			if (line.length()==0 || line.startsWith("#") || line.startsWith(";") || line.startsWith("//"))
				// comment/empty
				continue;
			String values [] = parseCsvLine(line);
			if (columns==null) {
				columns = values;
				// treat column names as all lower case?
				//for (int i=0; i<columns.length; i++) 
				//	columns[i] = columns[i].toLowerCase();
				continue;
			}
			HashMap<String,String> record = new HashMap<String,String>();
			for (int i=0; i<values.length && i<columns.length; i++) {
				record.put(columns[i], values[i]);
				if (values.length>columns.length) {
					logger.warn("Ignoring trailing values on line "+lineNumber);
				}
				record.put(LINE_NUMBER_COLUMN, Integer.toString(lineNumber));
			}
			db.add(record);
		}
		br.close();
		if (columns==null)
			throw new IOException("Did not find header in "+file);
		return db;
	}
    /** parse csv line with escaping (, -> "...,..."; "  -> "...""...")
     */
	static String[] parseCsvLine(String line) 
	{
		int pos = 0;
		Vector vals = new Vector();
		while(pos<line.length()) 
		{
			StringBuffer text = new StringBuffer();
			boolean quoted = false;
			boolean justUnquoted = false;
			for (; pos<line.length() && 
			!(line.charAt(pos)==',' && !quoted); pos++) 
			{

				if (line.charAt(pos)=='"') 
				{
					if (justUnquoted)
						text.append('"');
					quoted = !quoted;
					justUnquoted = !quoted;
				}
				else 
				{
					justUnquoted = false;
					text.append(line.charAt(pos));
				}
			}
			vals.addElement(text.toString());
			if (pos<line.length() && line.charAt(pos)==',') 
			{
				pos++;
				if (pos>=line.length())
					// trailing empty value
					vals.addElement("");
			}
		}
		return (String[])vals.toArray(new String[vals.size()]);
    }
}
