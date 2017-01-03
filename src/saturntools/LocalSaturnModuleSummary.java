/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;

/** Try to dump module information from Saturn. In particular, most interesting information.
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleSummary {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleSummary.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<2) {
			System.out.println("Usage: <groups.csv> <modulespec.html> ...");
		}
		try {
			File groups = new File(args[0]);
			System.err.println("Reading groups from "+groups);
			Map<String,GroupInfo> gis = GroupInfo.readGroupInfos(groups);
			
			System.out.println("modulecode,moduletitle,year,level,semester,credits,available,status,newgroup,groupname,convenor,prerequisites,prerequisitestext,corequisites,corequisitestext,targetstudents,assessments,exampercentage,examdurationhours,examrequirements");
			for (int argi=1; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);
					if (mi.groupname!=null)
						mi.newgroup = GroupInfo.getGroupcodeForTitle(gis, mi.groupname);
					else {
						mi.newgroup = GroupInfo.getGroupcodeForModule(gis, mi.modulecode);
					}
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				if (mi!=null) {
					System.out.print(mi.modulecode+","+escape(mi.moduletitle)+",'"+mi.year+","+mi.level+","+mi.semester+","+mi.credits+","+(mi.availability ? "Y" : "N")+","+mi.status+","+(mi.newgroup!=null ? mi.newgroup : "")+","+(mi.groupname!=null ? mi.groupname : "")+","+mi.convenor+",\"");
					for (String prerequisite: mi.prerequisitecodes)
						System.out.print(prerequisite+" ");
					System.out.print("\",\""+(mi.prerequisitestext==null ? "" : mi.prerequisitestext)+"\",\"");
					for (String corequisite: mi.corequisitecodes)
						System.out.print(corequisite+" ");
					System.out.println("\",\""+(mi.corequisitestext==null ? "" : mi.corequisitestext)+"\","+escape(mi.targetstudents)+","+mi.assessments+","+mi.exampercentage+","+mi.examdurationhours+","+mi.examrequirements);
				}
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	static String escape(String s) {
		return LocalSaturnModuleDetails.escape(s);
	}
}
