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

/** Learning outcomes by course(s).
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnCourseLearningOutcomes {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnCourseLearningOutcomes.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<1) {
			System.err.println("Usage: LocalSaturnCourseRequirements <course-page.html> ...");
			System.exit(-1);
		}
		try {
			CourseInfo courseInfos[] = new CourseInfo[args.length];
			for (int argi=0; argi<args.length; argi++) {
				CourseInfo ci = null;
				try {
					ci = CourseInfo.processCourseFile(args[argi]);
				} catch (Exception e) {
					logger.error("Processing course page "+args[argi], e);
				}
				if (ci!=null) {
					// TODO
					courseInfos[argi] = ci;
				}
			}
			System.out.println("outcometype,id,preface,text,course");
			for (int i=0; i<courseInfos.length; i++) {
				CourseInfo ci = courseInfos[i];
				if (ci==null) 
					continue;
				for (int si=0; si<CourseInfo.NUM_OUTCOME_SECTIONS; si++) {
					for (CourseInfo.Outcome outcome : ci.outcomeSections[si].outcomes) {
						System.out.println(((char)('A'+si))+","+
								LocalSaturnModuleDetails.escape(outcome.id!=null ? outcome.id : "")+","+
								LocalSaturnModuleDetails.escape(ci.outcomeSections[si].preface)+","+
								LocalSaturnModuleDetails.escape(outcome.text)+","+
								LocalSaturnModuleDetails.escape(ci.coursecode));
					}
				}
			}
			
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
}
