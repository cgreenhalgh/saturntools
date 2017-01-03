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
public class MergeNewgroup2 {
	/** logger */
    static Logger logger = Logger.getLogger(MergeNewgroup2.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=2) {
			System.err.println("Usage: MergeNewgroup <module-newgroup.csv> <module-summary.csv>");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulemap = ReadCsvFile.readCsvFile(new File(args[0]), "modulecode", false);
			HashMap<String,ModuleInfo> moduleinfos = ModuleInfo.readSummaryFile(new File(args[1]));

			// merge in extra info
			for (String modulecode : moduleinfos.keySet()) {
				ModuleInfo mi = moduleinfos.get(modulecode);
				//logger.info("Found module "+modulecode+": "+mi.modulecode);
				HashMap<String,String> values = modulemap.get(modulecode);
				if (values==null)
					logger.error(args[0]+" missing module "+modulecode);
				else
					mi.newgroup = values.get(GROUP_NAME);
			}
			ModuleInfo.printSummary(moduleinfos.values());
		}
		catch (Exception e) {
			logger.error("Processing "+args[0], e);
		}
	}
	public static final String GROUP_NAME = "newgroup";	// group
}
