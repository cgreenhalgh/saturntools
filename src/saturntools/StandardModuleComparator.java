package saturntools;

import java.util.Comparator;
import java.util.HashMap;

public class StandardModuleComparator  implements Comparator<HashMap<String,String>> {
	public static final String LEVEL_NAME = "level";
	public static final String SEMESTER_NAME = "semester";
	public static final String MODULECODE_NAME = "modulecode";
	public static final String MODULETITLE_NAME = "moduletitle";
	public static final String NEWGROUP_NAME = "newgroup";
	public static final String PREREQUISITES_NAME = "prerequisites";
	public static final String COREQUISITES_NAME = "corequisites";
	public static final String [] newgroups = new String[] { 
		"CN", "OR", "M", "PF", "PL", "SE", "IM", "NC", "DB", "HC", "GV", "OSA", "IS", "SP", "P"
	};
	//@Override
	public int compare(HashMap<String, String> m1,
			HashMap<String, String> m2) {
		int c = compareValues(m1, m2, LEVEL_NAME);
		if (c!=0)
			return c;
		c = compareValues(m1, m2, SEMESTER_NAME);
		if (c!=0)
			return c;
		return compareValues(m1, m2, MODULECODE_NAME);
	}
	public static int compareValues(HashMap<String, String> m1,
			HashMap<String, String> m2, String name) {
		String l1 = m1.get(name);
		String l2 = m2.get(name);
		return compareStrings(l1, l2);
	}
	public static int compareStrings(String l1, String l2) {
		if (l1==l2) 
			return 0;
		if (l1==null)
			return -1;
		if (l2==null)
			return 1;
		return l1.compareTo(l2);
		
	}

}
