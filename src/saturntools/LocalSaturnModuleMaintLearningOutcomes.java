/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

/** Try to dump module information from Saturn. More info than the summary!
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleMaintLearningOutcomes {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleMaintLearningOutcomes.class);
	/** main */
	public static void main(String [] args) {
		try {
			System.out.println("outcometype,modulecode,id,text");
			for (int argi=0; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url, true);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				if (mi!=null) {
					for (int si=0; si<mi.outcomeSections.length; si++) {
						for (CourseInfo.Outcome outcome : mi.outcomeSections[si].outcomes) {
							System.out.println(""+((char)('A'+si))+","+mi.modulecode+","+LocalSaturnModuleDetails.escape(outcome.id)+","+LocalSaturnModuleDetails.escape(outcome.text));
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
}
