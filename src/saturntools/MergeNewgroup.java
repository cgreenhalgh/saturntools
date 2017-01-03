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
public class MergeNewgroup {
	/** logger */
    static Logger logger = Logger.getLogger(MergeNewgroup.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=2) {
			System.err.println("Usage: MergeNewgroup <groups.csv> <module-summary.csv>");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> groupmap = ReadCsvFile.readCsvFile(new File(args[0]), "groupcode", false);
			HashMap<String,ModuleInfo> moduleinfos = ModuleInfo.readSummaryFile(new File(args[1]));

			// merge in extra info
			for (String modulecode : moduleinfos.keySet()) {
				ModuleInfo mi = moduleinfos.get(modulecode);
				mi.newgroup = null;
				//logger.info("Found module "+modulecode+": "+mi.modulecode);
				for (String groupcode : groupmap.keySet()) {
					String modulecodes = groupmap.get(groupcode).get("modulecodes");
					if (modulecodes!=null && modulecodes.contains(modulecode)) {
						mi.newgroup = groupcode;
						break;
					}
				}
				if (mi.newgroup==null)
					logger.error(args[0]+" missing module "+modulecode);
			}
			ModuleInfo.printSummary(moduleinfos.values());
		}
		catch (Exception e) {
			logger.error("Processing "+args[0], e);
		}
	}
	public static final String GROUP_NAME = "newgroup";	// group
}
