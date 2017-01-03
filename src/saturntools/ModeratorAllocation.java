/**
 * 
 */
package saturntools;

import java.io.File;
import java.util.HashMap;
import java.util.TreeSet;

/** Produce spreadsheet to help moderator allocation.
 * 
 * @author cmg
 *
 */
public class ModeratorAllocation {

	private static final String YES = "yes";
	private static final String OBSERVATION = "observation";
	private static final String ASSESSMENT = "assessment";
	private static final String PEER_REVIEW = "peer review";
	private static final String CONVENOR = "convenor";
	private static final String GROUPCODE = "groupcode";
	private static final String MODULECODE = "modulecode";	
	private static final String MODULECODES = "modulecodes";
	private static final String MODERATOR = "moderator";
	private static final String GROUPCODES = "groupcodes";
	private static final String MAYBEGROUPCODES = "maybegroupcodes";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=4) {
			System.err.println("Usage: <modulelist.csv> <groups.csv> <moderators.csv> <preferences.csv>");
			System.err.println("Where:");
			System.err.println("<modulelist.csv> has columns:");
			System.err.println("\tmodulecode [PK]: <modulecode>\n"+
					"\tassessment: C|CE|E\n"+
					"\tpeer review: yes|no\n"+
					"\tconvenor: <person> <person> ...");
			System.err.println("<groups.csv> has columns:");
			System.err.println("\tgroupcode [PK]: <code>\n"+
					"\tmodulecodes: <modulecode> <modulecode> ...");
			System.err.println("<moderators.csv> has columns:");
			System.err.println("\tmoderator [PK]: <person>\n"+
					"\tobservation: yes|no");
			System.err.println("<preferences.csv> has columns:");
			System.err.println("\tmoderator [PK]: <person>\n"+
					"\tgroupcodes: <code> <code> ...\n"+
					"\tmaybegroupcodes: <code> <code> ...\n"+
					"\tmodulecodes: <modulecode> <modulecode> ...");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulelist = ReadCsvFile.readCsvFile(new File(args[0]), MODULECODE, false);
			HashMap<String,HashMap<String,String>> groups = ReadCsvFile.readCsvFile(new File(args[1]), GROUPCODE, false);
			HashMap<String,HashMap<String,String>> moderators = ReadCsvFile.readCsvFile(new File(args[2]), MODERATOR, false);
			HashMap<String,HashMap<String,String>> preferences = ReadCsvFile.readCsvFile(new File(args[3]), MODERATOR, false);
			
			// moderators in order
			TreeSet<String> moderatorsSet = new TreeSet<String>();
			moderatorsSet.addAll(moderators.keySet());
			
			// modules in order
			TreeSet<String> modulesSet = new TreeSet<String>();
			modulesSet.addAll(modulelist.keySet());

			System.out.println("parameters");
			System.out.println("loadC,1");// A2
			String LOAD_C = "=$B$2";
			System.out.println("loadE,1");// A3
			String LOAD_E = "=$B$3";
			System.out.println("loadCE,1.5");// A4
			String LOAD_CE = "=$B$4";
			System.out.println("loadReview,0.4");// A5
			String LOAD_REVIEW = "=$B$5";
			System.out.println("loadObserve,0.3");// A6
			String LOAD_OBSERVE = "=$B$6";
			System.out.print("module,allocation,theme,assessment,review,load,MODERATOR");
			final int COLUMN_LOAD = 6;
			final int COLUMN_ALLOCATION = 2;
			final int LEADING_COLUMNS = 7;
			final int COLUMNS_PER_MODERATOR = 3;
			for (String moderator : moderatorsSet) {
				System.out.print(","+moderator);
				System.out.print(",,");
			}
			System.out.println();
			System.out.print(",,,,,,PREFERENCES");
			for (String moderator : moderatorsSet) {
				HashMap<String,String> m = preferences.get(moderator);
				if (m==null) {
					System.out.print(",N,,");
					continue;
				}
				String prefgroupcodes = m.get(GROUPCODES);
				String maybegroupcodes = m.get(MAYBEGROUPCODES);
				String prefmodulecodes = m.get(MODULECODES);
				System.out.print(",Y,"+prefgroupcodes+" "+prefmodulecodes+","+maybegroupcodes);
			}
			System.out.println();
			System.out.print(",,,,,,OBSERVERLOAD");
			for (String moderator : moderatorsSet) {
				HashMap<String,String> m = moderators.get(moderator);
				if (m!=null && YES.equals(m.get(OBSERVATION)))
					System.out.print(",,Y,"+LOAD_OBSERVE);
				else
					System.out.print(",,,");
			}
			System.out.println();
			final int LEADING_ROWS = 10;
			System.out.print(",,,,,,TOTALLOAD");
			int col = LEADING_COLUMNS-COLUMNS_PER_MODERATOR+1;
			for (String moderator : moderatorsSet) {
				col += COLUMNS_PER_MODERATOR;
				HashMap<String,String> m = moderators.get(moderator);
				System.out.print(",");
				System.out.print("="+ref(col+2,LEADING_ROWS-1)+"+SUM("+ref(col+2,LEADING_ROWS+1)+":"+ref(col+2,LEADING_ROWS+modulelist.size())+")");
				System.out.print(",,");
			}
			System.out.println();

			int row = LEADING_ROWS;
			for (String modulecode : modulesSet) {
				row++;
				// modulecode
				System.out.print(modulecode);
				HashMap<String,String> module = modulelist.get(modulecode);
				// allocation
				//TODO
				System.out.print(",\"");
				col = LEADING_COLUMNS-COLUMNS_PER_MODERATOR+1;
				for (String moderator : moderatorsSet) {
					col += COLUMNS_PER_MODERATOR;
					if (col>LEADING_COLUMNS+1)
						System.out.print("+");
					else
						System.out.print("=");
					System.out.print("IF("+ref(col+1,row)+"=\"\"Y\"\",1,0)");
				}
				System.out.print("\"");
				// theme
				String theme = getGroupcode(groups, modulecode);
				System.out.print(","+theme);
				// assessment
				String assessment = module.get(ASSESSMENT);
				System.out.print(","+assessment);
				// peer reivew
				String peerReview = module.get(PEER_REVIEW);
				System.out.print(","+peerReview);
				// load
				String load = "0";
				if ("C".equals(assessment))
					load = LOAD_C;
				else if ("CE".equals(assessment))
					load = LOAD_CE;
				else
					load = LOAD_E;
				if (peerReview.startsWith("y"))
					load += "+"+LOAD_REVIEW.substring(1);
				System.out.print(","+load);
				// blank column
				System.out.print(",");
				// each moderator...
				col = LEADING_COLUMNS-COLUMNS_PER_MODERATOR+1;
				for (String moderator : moderatorsSet) {
					col += COLUMNS_PER_MODERATOR;
					HashMap<String,String> m = preferences.get(moderator);
					if (m==null) {
						System.out.print(",,,");
						continue;
					}
					// moderator preference?
					String prefgroupcodes = m.get(GROUPCODES);
					String maybegroupcodes = m.get(MAYBEGROUPCODES);
					String prefmodulecodes = m.get(MODULECODES);
					// exclude convenors
					String convenors = module.get(CONVENOR);
					if (convenors!=null && convenors.contains(moderator)) {
						System.out.print(",X,,");
						// error :-)
						System.out.print("\"=IF("+ref(col+1, row)+"=\"\"Y\"\",1000,0)\"");
						continue;
					}
					int preference = 0;
					if ((prefgroupcodes!=null && prefgroupcodes.contains(theme)) || (prefmodulecodes!=null && prefmodulecodes.contains(modulecode)))
						preference = 2;
					else if (maybegroupcodes!=null && maybegroupcodes.contains(theme))
						preference = 1;
					System.out.print(","+(preference==0 ? "" : preference==1 ? "\"-\"" : "\"=\""));
					// allocated?
					System.out.print(",");
					// load?
					System.out.print(",\"=IF("+ref(col+1, row)+"=\"\"Y\"\","+ref(COLUMN_LOAD, row)+"/"+ref(COLUMN_ALLOCATION,row)+",0)\"");
				}
				//...
				System.out.println();
			}
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}
	private static String ref(int col, int row) {
		StringBuffer sb = new StringBuffer();
		int n1 = (col-1)/26;
		if (n1>0)
			sb.append((char)('A'+n1-1));
		int n2 = (col-1)%26;
		sb.append((char)('A'+n2));
		sb.append(row);
		return sb.toString();
	}
	private static String getGroupcode(
			HashMap<String, HashMap<String, String>> groups, String modulecode) {
		for (String group : groups.keySet()) {
			HashMap<String,String> g = groups.get(group);
			String modulecodes = g.get(MODULECODES);
			if (modulecodes!=null && modulecodes.contains(modulecode))
				return group;
		}
		return "UNKOWNN";
	}

}
