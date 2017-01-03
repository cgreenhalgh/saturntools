/**
 * 
 */
package saturntools;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * @author cmg
 *
 */
public class LocalModuleTableDependenciesGraph {
	/** logger */
	static Logger logger = Logger.getLogger(LocalModuleTableDependenciesGraph.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length!=2 && args.length!=3) {
			System.err.println("Usage: LocalModuleTableDependenciesGraph <module-summary.csv> <module-table.csv> [<course-regs.htm>]");
			System.exit(-1);
		}
		try {
			logger.info("Reading module summaries from "+args[0]);
			HashMap<String,ModuleInfo> modulemap = ModuleInfo.readSummaryFile(new File(args[0]));
			CourseInfo ci = null;
			if (args.length>2) {
				logger.info("Reading coure info from "+args[2]);
				ci = CourseInfo.processCourseFile(args[2]);
			}
			logger.info("Reading module table from "+args[1]);
			BufferedReader br = new BufferedReader(new FileReader(args[1]));
			String header = br.readLine();

			// start dot file
			System.out.println("digraph {");
			System.out.println("  rankdir=RL;");
			System.out.println("  node [ shape=box ];");

			int lineNumber = 0;
			boolean doneHeader = true;//false;
			String headers[] = null;
			Vector<String[]> lines = new Vector<String[]>();
			HashSet<String> modulecodes = new HashSet<String>();
			while (true) {
				String line = br.readLine();
				if (line==null)
					break;
				lineNumber++;
				//if (line.length()==0 || line.startsWith("#") || line.startsWith(";") || line.startsWith("//"))
				// comment/empty
				//continue;
				String values [] = ReadCsvFile.parseCsvLine(line);
				if (!doneHeader) {
					headers = values;
					doneHeader = true;
					continue;
				}
				lines.add(values);
			}
			for (String values []: lines) {
				lineNumber--;
				for (int i=0; i<values.length; i++) {
					if (values[i]!=null && values[i].length()>0) {
						String modulecode = values[i];
						ModuleInfo mi = modulemap.get(modulecode);
						if (mi==null) 
							logger.warn("Could not find a module '"+modulecode+"' in the summary");

						modulecodes.add(modulecode);
						System.out.print("  \""+values[i]+"\" [");
						if (ci!=null) {
							boolean compulsory = false, alternative = false, optional = false;
							for(int yi=0; ci.compulsory!=null && yi<ci.compulsory.length; yi++) 
								if (ci.compulsory[yi]!=null && ci.compulsory[yi].contains(modulecode))
									compulsory = true;
							for(int yi=0; ci.alternative!=null && yi<ci.alternative.length; yi++) 
								if (ci.alternative[yi]!=null && ci.alternative[yi].contains(modulecode))
									alternative = true;
							for(int yi=0; ci.restricted!=null && yi<ci.restricted.length; yi++) 
								if (ci.restricted[yi]!=null && ci.restricted[yi].contains(modulecode))
									optional = true;
							if (compulsory) 
								System.out.println(" shape=box, "+(mi!=null && mi.availability ? "fillcolor=red, style=\"bold,filled\", " : "style=dotted, "));
							else if (alternative)
								System.out.println(" shape=box, "+(mi!=null && mi.availability ? "fillcolor=orange, style=filled, " : "style=dotted, "));
							else if (optional)
								System.out.println(" shape=oval, "+(mi!=null && mi.availability ? "fillcolor=green, style=filled, " : "style=dotted, "));
							else if (mi!=null)
								System.out.println(" shape=oval, style=dotted, ");
							else
								System.out.println(" shape=plaintext, ");// defult

						}
						else {
							if (mi!=null && !mi.availability)
								System.out.println("style=dotted, ");
						}
						boolean wide = false;
						if (i+1<values.length && modulecode.equals(values[i+1])) {
							wide = true;
							values[i+1] = "";
						}
						System.out.println(" pos=\""+(120*i+(wide ? 60 : 0))+","+(50*lineNumber)+"\", width=\""+(wide ? "2.7": "0.75")+"\", height=\"0.5\"];");
					}
				}
			}
			HashSet<String> commonRequisites = new HashSet<String> ();
			commonRequisites.add("G51PRG");
			commonRequisites.add("G51MCS");
			commonRequisites.add("G52ADS");
			for (String modulecode : modulemap.keySet()) {
				if (!modulecodes.contains(modulecode))
				{
					logger.error("Module "+modulecode+" not found in table");
					continue;
				}
				ModuleInfo mi = modulemap.get(modulecode);
				for (String req : mi.prerequisitecodes) {
					if (!modulecodes.contains(req)) {
						logger.error("Prerequisite module "+req+" of "+modulecode+" not found in table");
						continue;						
					}
					System.out.println("  \""+modulecode+"\" -> \""+req+"\" ["+(commonRequisites.contains(req) ? "style=dotted":"")+"];");
				}
				for (String req : mi.corequisitecodes) {
					if (!modulecodes.contains(req)) {
						logger.error("Corequisite module "+req+" of "+modulecode+" not found in table");
						continue;						
					}
					System.out.println("  \""+modulecode+"\" -> \""+req+"\" ["+(commonRequisites.contains(req) ? "style=dotted":"")+"];");
					System.out.println("  \""+req+"\" -> \""+modulecode+"\" ["+(commonRequisites.contains(req) ? "style=dotted":"")+"];");
				}
			}

			System.out.println("}");
		}
		catch (Exception e) {
			logger.error("Error reading summary "+args[0]+" and table "+args[1], e);
		}

	}

}
