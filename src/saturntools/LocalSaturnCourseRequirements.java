/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import saturntools.SortModulesByGroup.ModuleComparator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;

/** Modules requires by course(s).
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnCourseRequirements {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnCourseRequirements.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<2) {
			System.err.println("Usage: LocalSaturnCourseRequirements <module-summary.csv> <course-page.html> ...");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulemap = ReadCsvFile.readCsvFile(new File(args[0]), "modulecode", false);
			LinkedList<HashMap<String,String>> modules = new LinkedList<HashMap<String,String>>(modulemap.values());
			java.util.Collections.sort(modules, new GroupModuleComparator(false));
			LinkedList<String> modulecodes = new LinkedList<String>();
			for (HashMap<String,String> module : modules) {
				modulecodes.add(module.get(MODULECODE_NAME));
			}
			//java.util.Collections.sort(modulecodes);
			LinkedList<String[]> moduleUse = new LinkedList<String[]>(); //[][] = new String[modulecodes.size()][];
			for (int i=0; i<modulecodes.size(); i++)  {
				String mu[] = new String[args.length];
				moduleUse.add(mu);
				mu[0] = modulecodes.get(i);
				for (int argi=1; argi<args.length; argi++) {
					mu[argi] = "";
				}
			}			
			CourseInfo courseInfos[] = new CourseInfo[args.length];
			for (int argi=1; argi<args.length; argi++) {
				CourseInfo ci = null;
				try {
					ci = CourseInfo.processCourseFile(args[argi]);
					for (int i=0; i<ci.compulsory.length; i++)
						for (String modulecode : ci.compulsory[i]) {
							int ix = modulecodes.indexOf(modulecode);
							if (ix<0) {
								ix = expandModuleUse(modulecodes,moduleUse, args.length, modulecode,modulemap,ci);
							}
							moduleUse.get(ix)[argi] = moduleUse.get(ix)[argi]+"C"+(ci.mscflag ? (4+i) : (1+i)+" ");
						}
					for (int i=0; i<ci.alternative.length; i++)
						for (String modulecode: ci.alternative[i]) {
							int ix = modulecodes.indexOf(modulecode);
							if (ix<0) {
								ix = expandModuleUse(modulecodes,moduleUse, args.length, modulecode,modulemap,ci);
							}
							moduleUse.get(ix)[argi] = moduleUse.get(ix)[argi]+"A"+(ci.mscflag ? (4+i) : (1+i)+" ");
						}
					for (int i=0; i<ci.restricted.length; i++)
						for (String modulecode: ci.restricted[i]) {
							int ix = modulecodes.indexOf(modulecode);
							if (ix<0) {
								ix = expandModuleUse(modulecodes,moduleUse, args.length, modulecode,modulemap,ci);
							}
							moduleUse.get(ix)[argi] = moduleUse.get(ix)[argi]+"O"+(ci.mscflag ? (4+i) : (1+i)+" ");
						}
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing course page "+args[argi], e);
				}
				if (ci!=null) {
					// TODO
					courseInfos[argi] = ci;
				}
			}
			System.out.print("modulecode,status,moduletitle,newgroup,credits,prerequisites,level,semester");
			for (int i=1; i<args.length; i++) 
				System.out.print(","+(courseInfos[i]!=null && courseInfos[i].coursecode!=null ? courseInfos[i].coursecode : args[i]));
			System.out.println();
			for (int i=0; i<moduleUse.size(); i++) {
				String mu[] = moduleUse.get(i);
				String modulecode = mu[0];
				HashMap<String,String> module = modulemap.get(modulecode);
				if (module==null)
				{
					logger.warn("Could not find module "+modulecode);
					System.out.print(modulecode+",,,,");
					System.out.print(",");
					System.out.print(",,");
				}
				else {
					System.out.print(modulecode+","+module.get(STATUS_NAME)+","+LocalSaturnModuleDetails.escape(module.get(MODULETITLE_NAME))+","+module.get(NEWGROUP_NAME)+",");
					System.out.print(module.get(CREDITS_NAME)+",");
					System.out.print(module.get(PREREQUISITES_NAME)+" "+module.get(COREQUISITES_NAME)+","+module.get(LEVEL_NAME)+","+module.get(SEMESTER_NAME));
					
				}
				for (int j=1; j<mu.length; j++) {
					if (j>0)
						System.out.print(",");
					if (mu[j]!=null)
						System.out.print(mu[j]);
				}
				System.out.println();
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	
	private static int expandModuleUse(LinkedList<String> modulecodes,
			LinkedList<String[]> moduleUse, int nargs, String compulsory, HashMap<String, HashMap<String, String>> modulemap, CourseInfo ci) {
		logger.warn("Could not find "+compulsory+" in module-summary");
		int i = moduleUse.size();
		String mu[] = new String[nargs];
		moduleUse.add(mu);
		mu[0] = compulsory;
		for (int argi=1; argi<nargs; argi++) {
			mu[argi] = "";
		}
		modulecodes.add(compulsory);
		ModuleInfo mi = ci.modules.get(compulsory);
		if (mi!=null) {
			HashMap<String,String> mim = new HashMap<String,String>();
			mim.put(MODULECODE_NAME, mi.modulecode);
			mim.put(COREQUISITES_NAME, "");
			mim.put(CREDITS_NAME, ""+mi.credits);
			mim.put(LEVEL_NAME, mi.modulecode.length()>3 ? mi.modulecode.substring(2,3) : "");
			mim.put(MODULETITLE_NAME, mi.moduletitle);
			mim.put(NEWGROUP_NAME, "");
			mim.put(PREREQUISITES_NAME, "");
			mim.put(SEMESTER_NAME, mi.semester);
			mim.put(STATUS_NAME, "");
			modulemap.put(compulsory, mim);
		}
		return i;
	}

	public static final String LEVEL_NAME = "level";
	public static final String STATUS_NAME = "status";
	public static final String CREDITS_NAME = "credits";
	public static final String SEMESTER_NAME = "semester";
	public static final String MODULECODE_NAME = "modulecode";
	public static final String MODULETITLE_NAME = "moduletitle";
	public static final String NEWGROUP_NAME = "newgroup";
	public static final String PREREQUISITES_NAME = "prerequisites";
	public static final String COREQUISITES_NAME = "corequisites";

}
