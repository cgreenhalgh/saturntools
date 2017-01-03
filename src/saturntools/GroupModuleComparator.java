package saturntools;

import java.util.HashMap;

public class GroupModuleComparator extends StandardModuleComparator {
	boolean includeGroup = true;
	public GroupModuleComparator(boolean includeGroup) {
		this.includeGroup = includeGroup;
	}
	public GroupModuleComparator() {
	}
	@Override
	public int compare(HashMap<String, String> m1,
			HashMap<String, String> m2) {
		int c = compareValues(m1, m2, LEVEL_NAME);
		if (c!=0)
			return c;
		if (includeGroup) {
			c = compareValuesByRank(m1, m2, NEWGROUP_NAME, newgroups);
			if (c!=0)
				return c;
		}
		c = compareValues(m1, m2, SEMESTER_NAME);
		if (c!=0)
			return c;
		return compareValues(m1, m2, MODULECODE_NAME);
	}
	static public int compareValuesByRank(HashMap<String, String> m1,
			HashMap<String, String> m2, String name, String rank[]) {			
		String l1 = m1.get(name);
		String l2 = m2.get(name);
		return compareStringsByRank(l1, l2, rank);
		
	}
	static public int compareStringsByRank(String l1, String l2, String rank[]) {
		int i1 = -1;
		if (l1!=null) {
			for (i1=0; i1<rank.length; i1++)
				if (l1.equals(rank[i1]))
					break;
		}
		int i2 = -1;
		if (l2!=null) {
			for (i2=0; i2<rank.length; i2++)
				if (l2.equals(rank[i2]))
					break;
		}
		if (i1<i2)
			return -1;
		if (i1>i2)
			return 1;
		return compareStrings(l1, l2);
	}

}
