/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

/** Try to dump module information from Saturn. In particular, history of who convened each module, when.
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleHistoryReader {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleHistoryReader.class);
	/** main */
	public static void main(String [] args) {
		try {
			for (int argi=0; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);

					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				if (mi!=null)
					System.out.println(mi.modulecode + ",\"" + mi.moduletitle+"\",'"+mi.year+","+mi.semester+","+(mi==null ? "-" : (!mi.availability ? "Suspended" : (mi.convenor.length()==0 ? "Unknown convenor" : LocalSaturnModuleConvenorsToImsEnterprise.splitModuleConvenors(mi.convenor)))));
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
}
