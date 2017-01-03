/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;

/** Dump modules and themes lectured by each convenor.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleConvenorHistory {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleConvenorHistory.class);
    static class ConvenorInfo {
    	String convenor;
    	TreeSet<String> themes = new TreeSet<String>();
    	TreeSet<String> modules = new TreeSet<String>();    	
    }
	/** main */
	public static void main(String [] args) {
		if (args.length<2) {
			System.err.println("Usage: MergeNewgroup <groups.csv> <modulespec.html> ...");
			System.exit(-1);
		}
		try {
			Map<String,ConvenorInfo> convenors = new HashMap<String,ConvenorInfo>();
			HashMap<String,HashMap<String,String>> groupmap = ReadCsvFile.readCsvFile(new File(args[0]), "groupcode", false);
			TreeSet<String> convenornames = new TreeSet<String>();
			for (int argi=1; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);
					
					mi.newgroup = null;
					//logger.info("Found module "+modulecode+": "+mi.modulecode);
					for (String groupcode : groupmap.keySet()) {
						String modulecodes = groupmap.get(groupcode).get("modulecodes");
						if (modulecodes!=null && modulecodes.contains(mi.modulecode)) {
							mi.newgroup = groupcode;
							break;
						}
						String oldmodulecodes = groupmap.get(groupcode).get("oldmodulecodes");
						if (oldmodulecodes!=null && oldmodulecodes.contains(mi.modulecode)) {
							mi.newgroup = groupcode;
							break;
						}
					}
					if (mi.newgroup==null) {
						logger.error(args[0]+" missing module "+mi.modulecode);
						mi.newgroup = mi.modulecode;
					}

					String convenornamesarray[] = LocalSaturnModuleConvenorsToImsEnterprise.splitModuleConvenorsNoTitles(mi.convenor).split(";");
					for (int i=0; i<convenornamesarray.length; i++) {
						String convenorname = convenornamesarray[i].trim();
						if (convenorname.length()==0)
							continue;
						ConvenorInfo ci = convenors.get(convenorname);
						if (ci==null) {
							ci = new ConvenorInfo();
							ci.convenor = convenorname;
							convenors.put(convenorname, ci);
							convenornames.add(convenorname);
						}
						ci.modules.add(mi.modulecode);
						if (mi.newgroup!=null)
							ci.themes.add(mi.newgroup);
					}
					
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				//if (mi!=null)
				//System.out.println(mi.modulecode + ",\"" + mi.moduletitle+"\",'"+mi.year+","+mi.semester+","+(mi==null ? "-" : (!mi.availability ? "Suspended" : (mi.convenor.length()==0 ? "Unknown convenor" : LocalSaturnModuleConvenorsToImsEnterprise.splitModuleConvenors(mi.convenor)))));
			}
			System.out.println("convenor,themes,modules");
			for (String convenorname : convenornames) {
				ConvenorInfo ci  = convenors.get(convenorname);
				System.out.print(ci.convenor+",");
				for (String theme : ci.themes)
					System.out.print(theme+" ");
				System.out.print(",");
				for (String module : ci.modules)
					System.out.print(module+" ");
				System.out.println();
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
}
