/**
 * 
 */
package saturntools;

import java.io.File;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/** year module summary file with extra columns 'group' (CS module group) and 'acm' (ACM cs curriculum category) by group/acm with level/semester columns.
 * @author cmg
 *
 */
public class SortModulesByGroup {
	/** logger */
    static Logger logger = Logger.getLogger(SortModulesByGroup.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=2) {
			System.err.println("Usage: SortModulesByGroup <module-newgroup.csv> <module-summary.csv>");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulemap = ReadCsvFile.readCsvFile(new File(args[0]), "modulecode", false);
			HashMap<String,ModuleInfo> moduleinfos = ModuleInfo.readSummaryFile(new File(args[1]));
			System.out.println(GROUP_NAME+/*ACM*/",level0autumn,level0spring,level1autumn,level1spring,level2autumn,level2spring,level3autumn,level3spring,level4autumn,level4spring,level4summer");
			// merge in extra info
			for (String modulecode : modulemap.keySet()) {
				ModuleInfo mi = moduleinfos.get(modulecode);
				if (mi==null) {
					logger.info("Module "+modulecode+" in modulemap but not summary - removing");
					continue;
				}
				HashMap<String,String> values = modulemap.get(modulecode);
				if (mi.level==null)
					logger.warn("No level for "+modulecode);
				else
					values.put(LEVEL_NAME, mi.level);
				if (mi.semester==null)
					logger.warn("No semester for "+modulecode);
				else
					values.put(SEMESTER_NAME, mi.semester);
			}
			for (String modulecode : moduleinfos.keySet())
				if (!modulemap.containsKey(modulecode))
					logger.error(args[0]+" missing module "+modulecode);
			LinkedList<HashMap<String,String>> modules = new LinkedList<HashMap<String,String>>(modulemap.values());
			java.util.Collections.sort(modules, new ModuleComparator());
			String values[] = new String[11];
			boolean hasValue = false;
			String currentGroup = null, currentRow = null;
			for (HashMap<String,String> module : modules) {
				String group = module.get(GROUP_NAME);
				String row = module.get(ROW_NAME);
				//String acm = module.get(ACM_NAME);
				if (compareStrings(group,currentGroup)!=0 || compareStrings(currentRow, row)!=0
						// || compareStrings(acm, currentAcm)!=0
						) {
					if (hasValue) {
						printLine(values);
						hasValue = false;
						values = new String[12];
					}
					if (compareStrings(group, currentGroup)!=0) {
						System.out.println(""); 
						values[0] = currentGroup = group;
						hasValue = true;
					}
					currentRow = row;
					//values[1] = currentAcm = acm;
				}
				try {
					String modulecode = module.get(MODULECODE_NAME);
					int column = 2*Integer.parseInt(module.get(LEVEL_NAME))+1;
					if (column<0)
						column = 0;
					String semester = module.get(SEMESTER_NAME).toLowerCase();
					boolean full_year = false;
					if ("spring".equals(semester))
						column = column+1;
					else if ("full year".equals(semester))
						full_year = true;
					if ("summer".equals(semester))
						column = column+2;
					if (values[column]!=null ||(full_year && values[column+1]!=null))
					{
						// flush old
						printLine(values);
						hasValue = false;
						values = new String[12];
					}
					values[column] = modulecode;
					if (full_year) 
						values[column+1] = modulecode;					
					hasValue = true;
				}
				catch (Exception e) {
					logger.error("Error placing module "+module, e);
				}
			}
			if (hasValue) {
				printLine(values);
				hasValue = false;
			}
		}
		catch (Exception e) {
			logger.error("Processing "+args[0], e);
		}
	}
	public static void printLine(String [] values) {
		for (int i=0; i<values.length; i++) {
			if(i>0)
				System.out.print(",");
			if (values[i]!=null)
				System.out.print(values[i]);
		}
		System.out.println();
	}
	public static final String GROUP_NAME = "newgroup";	// group
	//public static final String ACM_NAME = "acm";
	public static final String LEVEL_NAME = "level";
	public static final String SEMESTER_NAME = "semester";
	public static final String MODULECODE_NAME = "modulecode";
	public static final String ROW_NAME = "row";
	static class ModuleComparator implements Comparator<HashMap<String,String>> {

		//@Override
		public int compare(HashMap<String, String> m1,
				HashMap<String, String> m2) {
			int c = GroupModuleComparator.compareValuesByRank(m1, m2, GROUP_NAME, GroupModuleComparator.newgroups);
			//if (c!=0)
			//	return c;
			//c = compareValues(m1, m2, ACM_NAME);
			if (c!=0)
				return c;
			c = compareValues(m1, m2, ROW_NAME);
			if (c!=0)
				return c;
			c = compareValues(m1, m2, LEVEL_NAME);
			if (c!=0)
				return c;
			c = compareValues(m1, m2, SEMESTER_NAME);
			if (c!=0)
				return c;
			return 0;
		}
		public int compareValues(HashMap<String, String> m1,
				HashMap<String, String> m2, String name) {
			String l1 = m1.get(name);
			String l2 = m2.get(name);
			return compareStrings(l1, l2);
		}
	}
	public static int compareStrings(String l1, String l2) {
		if (l1==l2) 
			return 0;
		if (l1==null)
			return -1;
		if (l2==null)
			return 1;
		return l1.compareTo(l2);
		
	}
}
