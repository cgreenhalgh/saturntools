/**
 * 
 */
package saturntools;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.TreeSet;


/**
 * @author cmg
 *
 */
public class ExportModules3Summary {

	static class Entry {
		String moduleCode;
		String moduleTitle;
		int level;
		String courseCode;
		String studentId;
	}
	
	/** Read file from Saturn2 Student Exports, Modules 3 option, and generate Module summary table
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=1) {
			System.err.println("Usage: java ExportModules3Summary <saturn-modules3-export-file.txt>");
			System.exit(-1);
		}
		try {
			System.out.println("# Generating summary from "+args[0]+", "+(new Date()));
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			String header = br.readLine();
			if (header==null) {
				System.err.println("Could not read header line from "+args[0]);
				System.exit(-1);
			}
			Vector<String> headers = CsvUtils.parseCsvLine(header);
			int moduleCodeIndex = headers.indexOf(MODULE_CODE);
			if (moduleCodeIndex<0) {
				System.err.println("Could not find column '"+MODULE_CODE+"' in "+args[0]);
				System.exit(-1);			
			}
			int studentIdIndex = headers.indexOf(STUDENT_ID);
			if (moduleCodeIndex<0) {
				System.err.println("Could not find column '"+STUDENT_ID+"' in "+args[0]);
				System.exit(-1);			
			}
			int moduleTitleIndex = headers.indexOf(MODULE_TITLE);
			if (moduleTitleIndex<0) {
				System.err.println("Could not find column '"+MODULE_TITLE+"' in "+args[0]);
				System.exit(-1);			
			}
			int courseCodeIndex = headers.indexOf(COURSE_CODE);
			if (courseCodeIndex<0) {
				System.err.println("Could not find column '"+COURSE_CODE+"' in "+args[0]);
				System.exit(-1);			
			}
			TreeSet<String> courseCodes = new TreeSet<String>();
			// module code -> course code -> number
			HashMap<String,HashMap<String,Integer>> moduleNumbers = new HashMap<String,HashMap<String,Integer>>();
			HashMap<String,String> moduleTitles = new HashMap<String,String>();
			// studentId -> set of module levels
			HashMap<String,Vector<Integer>> studentLevels = new HashMap<String,Vector<Integer>>();
			int count = 0;
			int maxLevel = 0;
			Vector<Entry> entries = new Vector<Entry>();
			while (true) {
				String line = br.readLine();
				if (line==null)
					break;
				count++;
				Vector<String> values = CsvUtils.parseCsvLine(line);
				try {
					Entry e = new Entry();
					String moduleCode = values.elementAt(moduleCodeIndex);
					e.moduleCode = moduleCode;					
					String moduleTitle = values.elementAt(moduleTitleIndex);
					e.moduleTitle = moduleTitle;
					moduleTitles.put(moduleCode, moduleTitle);
					String courseCode = values.elementAt(courseCodeIndex);
					e.courseCode =  courseCode;
					String studentId = values.elementAt(studentIdIndex);
					e.studentId =  studentId;
					try {
						e.level = Integer.valueOf(moduleCode.substring(2, 3));
					}
					catch (Exception e2) {
						System.err.println("Could not get module level from code: "+moduleCode);
					}
					Vector<Integer> levels = studentLevels.get(studentId);
					if(levels==null) {
						levels = new Vector<Integer>();
						studentLevels.put(studentId, levels);
					}
					levels.add(e.level);
					if (e.level>maxLevel)
						maxLevel = e.level;
					entries.add(e);
					if (!moduleNumbers.containsKey(moduleCode))
						moduleNumbers.put(moduleCode, new HashMap<String,Integer>());
					HashMap<String,Integer> courseNumbers = moduleNumbers.get(moduleCode);
					if (!courseNumbers.containsKey(courseCode))
						courseNumbers.put(courseCode, 0);
					courseNumbers.put(courseCode, courseNumbers.get(courseCode)+1);		
					courseCodes.add(courseCode);
				}
				catch (Exception e) {
					System.err.println("Error handling line "+line+": "+e);
					e.printStackTrace(System.err);
				}
			}
			System.out.println("# read "+count+" entries in total");
			
			// split out numbers by likely student level
			HashMap<String,Integer> studentLevel = new HashMap<String,Integer>();
			for (String studentId : studentLevels.keySet()) {
				Vector<Integer> ls = studentLevels.get(studentId);
				int size = ls.size();
				Collections.sort(ls);
				int level = ls.get(size/2);
				studentLevel.put(studentId,level);
				//System.err.println("Student "+studentId+", level "+level+": "+ls);
			}
			// module code -> course code -> number
			HashMap<String,HashMap<String,int[]>> moduleNumbersByLevel = new HashMap<String,HashMap<String,int[]>>();
			for (Entry e : entries) {
				if (!moduleNumbersByLevel.containsKey(e.moduleCode))
					moduleNumbersByLevel.put(e.moduleCode, new HashMap<String,int[]>());
				HashMap<String,int[]> courseNumbers = moduleNumbersByLevel.get(e.moduleCode);
				if (!courseNumbers.containsKey(e.courseCode))
					courseNumbers.put(e.courseCode, new int[maxLevel+1]);
				courseNumbers.get(e.courseCode)[studentLevel.get(e.studentId)]++;
			}
			
			System.out.print("\"\",\"\"");
			for (String courseCode : courseCodes) {
				System.out.print(",\""+courseCode+"\"");
				for (int i=0; i<=maxLevel; i++)
					System.out.print(",");
			}
			System.out.println(",");
			System.out.print("\"Module Code\",\"Module Title\"");
			for (String courseCode : courseCodes) {
				System.out.print(",");
				for (int i=0; i<=maxLevel; i++)
					System.out.print(",\""+i+"\"");
			}
			System.out.println(",\"Total\"");
			// each module
			TreeSet<String> moduleCodes = new TreeSet<String>(moduleNumbers.keySet());
			for (String moduleCode : moduleCodes) {
				System.out.print("\""+moduleCode+"\",\""+moduleTitles.get(moduleCode)+"\"");
				int total = 0;
				HashMap<String,Integer> courseNumbers = moduleNumbers.get(moduleCode);
				HashMap<String,int[]> courseNumbersByLevel = moduleNumbersByLevel.get(moduleCode);
				for (String courseCode : courseCodes) {
					int number = 0;
					if (courseNumbers.containsKey(courseCode))
						number = courseNumbers.get(courseCode);
					total += number;
					System.out.print(","+number);
					for (int i=0; i<=maxLevel; i++) {
						int nl = 0;
						if (courseNumbersByLevel!=null && courseNumbersByLevel.containsKey(courseCode))
							nl = courseNumbersByLevel.get(courseCode)[i];
						System.out.print(","+nl);
					}
				}
				System.out.println(","+total);
			}
			System.out.flush();
			System.err.println("Done");
			System.exit(0);
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}
	/** header constant */
	static final String MODULE_CODE = "Module Code";
	/** header constant */
	static final String MODULE_TITLE = "Module Title";
	/** header constant */
	static final String COURSE_CODE = "Ucas Course";
	/** header constant */
	static final String STUDENT_ID = "StudentID";
}
