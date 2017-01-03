/**
 * 
 */
package saturntools;

import java.io.File;
import java.util.HashMap;
import java.util.TreeSet;

import saturntools.ModuleInfo.SummaryHeading;

/**
 * @author cmg
 *
 */
public class CsvDiff {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length<3) {
			System.err.println("Usage: java "+CsvDiff.class.getName()+" <module_summary1.csv> <module_summary2.csv> <keycolumn> <column> ...");
			System.exit(-1);			
		}
		try {
			String key = args[2];
			TreeSet<String> allKeys = new TreeSet<String>();
			HashMap<String,HashMap<String,String>> files[] = (HashMap<String,HashMap<String,String>>[])new HashMap<?,?>[2];
			for (int fi=0; fi<2; fi++) {
				System.err.println("Read file "+args[fi]);
				files[fi] = ReadCsvFile.readCsvFile(new File(args[fi]), key, false);
				allKeys.addAll(files[fi].keySet());
			}
			String columns [] = new String[args.length-3];
			for (int ci=0; ci+3<args.length; ci++)
				columns[ci] = args[ci+3];
			System.out.print(key+","+args[0]+","+args[1]);
			for (int ci=0; ci<columns.length; ci++)
				System.out.print(","+columns[ci]);
			System.out.println();
			for (String keyVal : allKeys) {
				System.out.print(keyVal);
				HashMap<String,String> rows[] = (HashMap<String,String>[])new HashMap<?,?>[2];
				for (int fi=0; fi<2; fi++)	{
					rows[fi] = files[fi].get(keyVal);
					if (rows[fi]!=null)
						System.out.print(",Y");
					else
						System.out.print(",N");
				}
				String [] values = new String[2];
				for (int ci=0; ci<columns.length; ci++) {
					for (int fi=0; fi<2; fi++)	{
						values[fi] = rows[fi]!=null ? rows[fi].get(columns[ci]) : "";
						if (values[fi]==null)
							values[fi] = "";
					}	
					System.out.print(",");
					if (!values[0].trim().equals(values[1].trim())) {
						System.out.print(CsvUtils.escape(values[0]+" -> "+values[1]));
					}
				}
//				System.out.print(","+rows[0]+","+rows[1]);
				System.out.println();
			}
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
			System.exit(-2);
		}
	}

}
