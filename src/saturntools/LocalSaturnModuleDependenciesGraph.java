/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.HashMap;
import java.io.IOException;

/** Dump DOT graph file of dependencies, ie pre & co-requisites. 
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleDependenciesGraph {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleDependenciesGraph.class);
	/** main */
	public static void main(String [] args) {
		try {
			System.out.println("digraph {");
			System.out.println("  rankdir=RL;");
			System.out.println("  node [ shape=box ];");
			TreeSet<String> requisites = new TreeSet<String>();
			TreeSet<String> modulecodes = new TreeSet<String>();
			HashMap<String,ModuleInfo> moduleinfos = new HashMap<String,ModuleInfo>();
			TreeSet<String> semesters = new TreeSet<String>();
			// rank by semesters
			for (int argi=0; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);

					modulecodes.add(mi.modulecode);
					semesters.add(mi.level+mi.semester);
					moduleinfos.put(mi.modulecode, mi);
					for (String requisite : mi.prerequisitecodes) {
						requisites.add(requisite);
					}
					for (String requisite : mi.corequisitecodes) {
						requisites.add(requisite);
					}
					
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
			}
			//System.out.print("modulecode");
			//for (String requisite : requisites) {
			//System.out.print(","+requisite);
			//}
			System.out.println(" {");
			String [] semestersInOrder = semesters.toArray(new String[semesters.size()]);
			for (int i=1; i<semestersInOrder.length; i++) {
				System.out.println("    \""+semestersInOrder[i-1]+"\" -> \""+semestersInOrder[i]+"\"");
			}
			System.out.println("  }");
			for (String semester: semesters) {
				System.out.println("  { rank = same; \""+semester+"\"; ");
				for (String modulecode : modulecodes) {
					ModuleInfo mi = moduleinfos.get(modulecode);
					String s = mi.level+mi.semester;
					if (!semester.equals(s))
						continue;
					System.out.println("    \""+modulecode+"\"");
				}
				System.out.println("  }");	
			}
			// depedencies
			for (String modulecode : modulecodes) {
				boolean output = false;
				for (String requisite : requisites) {
					ModuleInfo mi = moduleinfos.get(modulecode);
					if (mi.prerequisitecodes.contains(requisite)) {
						output = true;
						System.out.println(modulecode+" -> "+requisite+";");
					}
					else if (mi.corequisitecodes.contains(requisite)) {
						output = true;
						System.out.println(modulecode+" -> "+requisite+";");
						System.out.println(requisite+" -> "+modulecode+";");
					}
				}	
				if (output)
					System.out.println(modulecode+" [];");
			}
			System.out.println("}");
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	/** transitive */
	static boolean transitiveRequisite(String modulecode, String requisite, HashMap<String,ModuleInfo> moduleinfos) {
		ModuleInfo mi = moduleinfos.get(modulecode);
		if (mi==null) {
			logger.warn("Could not find pre-requisite info for "+modulecode);
			return false;
		}
		if (mi.prerequisitecodes.contains(requisite))
			return true;
		else if (mi.corequisitecodes.contains(requisite))
			return true;
		for (String requisite2 : mi.prerequisitecodes) {
			if (transitiveRequisite(requisite2, requisite, moduleinfos))
				return true;
		}
		for (String requisite2 : mi.corequisitecodes) {
			if (transitiveRequisite(requisite2, requisite, moduleinfos))
				return true;
		}
		return false;
	}
}
