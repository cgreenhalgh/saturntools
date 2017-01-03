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
public class LocalSaturnCourseSummary {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnCourseSummary.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<1) {
			System.err.println("Usage: LocalSaturnCourseRequirements <course-page.html> ...");
			System.exit(-1);
		}
		try {
			System.out.println("coursecode,coursetitle,year,filemodified");
			for (int argi=0; argi<args.length; argi++) {
				CourseInfo ci = null;
				try {
					ci = CourseInfo.processCourseFile(args[argi]);
					System.out.println(ci.coursecode+","+LocalSaturnModuleSummary.escape(ci.title)+",'"+ci.year+","+ci.filemodified);
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing course page "+args[argi], e);
				}
				if (ci!=null) {
					// TODO
				}
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	
}
