/**
 * 
 */
package saturntools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/** Info and utils for group def file. Columns: groupcode, grouptitle, modulecodes (space-separated)
 * @author cmg
 *
 */
public class GroupInfo {
	/** logger */
    static Logger logger = Logger.getLogger(GroupInfo.class);
	public String groupcode;
	public String grouptitle;
	public TreeSet<String> modulecodes = new TreeSet<String>();
	
	public static Map<String,GroupInfo> readGroupInfos(File groupfile) throws IOException {
		logger.info("Read groups from "+groupfile);
		HashMap<String,HashMap<String,String>> groupmap = ReadCsvFile.readCsvFile(groupfile, "groupcode", false);
		HashMap<String,GroupInfo>  gis = new HashMap<String,GroupInfo> ();
		for (String groupcode : groupmap.keySet()) {
			HashMap<String,String> info = groupmap.get(groupcode);
			GroupInfo gi = new GroupInfo();
			gi.groupcode = groupcode;
			gi.grouptitle = info.get("grouptitle");
			String modulecodes = info.get("modulecodes");
			if (modulecodes!=null) {
				String mcs [] = modulecodes.split(" ");
				for (String mc : mcs) {
					if (mc.length()>0)
						gi.modulecodes.add(mc);
				}
			}
			gis.put(groupcode, gi);
		}
		return gis;
	}
	
	public static String getGroupcodeForTitle(Map<String,GroupInfo> groups, String title) {
		for (GroupInfo gi : groups.values())
			if (title.equalsIgnoreCase(gi.grouptitle))
				return gi.groupcode;
		return null;
	}
	public static String getGroupcodeForModule(Map<String,GroupInfo> groups, String modulecode) {
		for (GroupInfo gi : groups.values())
			if (gi.modulecodes.contains(modulecode))
				return gi.groupcode;
		return null;
	}
}
