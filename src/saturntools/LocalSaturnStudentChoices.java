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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.StringTokenizer;

/** Modules taken (and avaialble to but not taken) by students on courses.
 * Based on saturn module info (summary) in pre-requisites, course regs (options), and student exports for 3 years.
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnStudentChoices {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnStudentChoices.class);
    static boolean includeKnownCoursesOnly = true;
    static boolean includeCsModulesOnly = false;
	/** main */
	public static void main(String [] args) {
		if (args.length<5) {
			System.err.println("Usage: LocalSaturnCourseRequirements <module-summary.csv> <student-export1-yr1.csv> ... <course-page.html> ...");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulemap = ReadCsvFile.readCsvFile(new File(args[0]), "modulecode", false);
			LinkedList<HashMap<String,String>> modules = new LinkedList<HashMap<String,String>>(modulemap.values());
			java.util.Collections.sort(modules, new GroupModuleComparator());
			LinkedList<String> modulecodes = new LinkedList<String>();
			for (HashMap<String,String> module : modules) {
				
				modulecodes.add(module.get(MODULECODE_NAME));
			}
			int numExports = 0;
			LinkedList<CourseInfo> cis = new LinkedList<CourseInfo>();
			for (int argi=1; argi<args.length; argi++) {
				String filename = args[argi];
				int ix = filename.lastIndexOf(".");
				if (ix<0) {
					System.err.println("Error: file without extension: "+filename);
					System.exit(-1);
				}
				String extension = filename.substring(ix+1);
				if (extension.toLowerCase().equals("csv")) 
					numExports++;
				else
					break;
			}
			HashMap<String,CourseInfo> courses = new HashMap<String,CourseInfo>();
			TreeSet<String> courseCodes = new TreeSet<String>();
			for (int argi=1+numExports; argi<args.length; argi++) {
				CourseInfo ci = null;
				try {
					ci = CourseInfo.processCourseFile(args[argi]);
					cis.add(ci);
					courses.put(ci.course, ci);
					courseCodes.add(ci.course.toLowerCase());
				} catch (Exception e) {
					logger.error("Processing course page "+args[argi], e);
				}
				if (ci!=null) {
					// TODO
				}
			}
			Vector<ModuleEntryInfo> meis = new Vector<ModuleEntryInfo>();
			for (int i=0; i<numExports; i++) {
				logger.info("Reading export "+args[1+i]);
				meis.addAll(readStudentModuleExport(args[1+i]));
			}
			
			logger.info("Finding students");
			HashMap<String,String> studentCourses = new HashMap<String,String>();
			HashMap<String,Integer> studentYear = new HashMap<String,Integer>();
			// for each student, what year did they do a module in?
			HashMap<String,HashMap<String,String>> modulesDone = new HashMap<String,HashMap<String,String>>();
			TreeSet<String> moremodulecodes = new TreeSet<String>();
			for (int i=0; i<meis.size(); i++) {
				ModuleEntryInfo mei = meis.get(i);
				if (includeKnownCoursesOnly && !courseCodes.contains(mei.course.toLowerCase()))
					continue;
				studentCourses.put(mei.student, mei.course);
				if (!studentYear.containsKey(mei.student) || mei.yearOfCourse>studentYear.get(mei.student))
					studentYear.put(mei.student, mei.yearOfCourse);
				if ((!moremodulecodes.contains(mei.modulecode) && !modulecodes.contains(mei.modulecode)) &&
						(!includeCsModulesOnly || mei.modulecode.toLowerCase().startsWith("g4") || mei.modulecode.toLowerCase().startsWith("g5")))
					moremodulecodes.add(mei.modulecode);
				HashMap<String,String> studentModules = modulesDone.get(mei.student);
				if (studentModules==null) {
					studentModules = new HashMap<String,String>();
					modulesDone.put(mei.student, studentModules);
				}
				studentModules.put(mei.modulecode, mei.year+" "+mei.yearOfCourse);
			}
			// add the extra ones
			modulecodes.addAll(moremodulecodes);
			logger.info("Doing output");
			LinkedList<String> students = new LinkedList<String>(studentCourses.keySet());
			java.util.Collections.sort(students, new StudentComparator(studentCourses, studentYear, modulesDone, modulecodes));
			// TODO sort better?!
			System.out.print("modulecode");
			String last = null;
			for (String student : students) {
				System.out.print(",");
				String course = studentCourses.get(student);
				if (!course.equals(last)) {
					System.out.print(course);
					last = course;
				}
			}
			System.out.println();
			System.out.print("year");
			last = null;
			String lastyear = null;
			for (String student : students) {
				System.out.print(",");				
				String year = studentYear.get(student).toString();
				String course = studentCourses.get(student);
				if (!course.equals(last) || !year.equals(lastyear)) {
					System.out.print("y"+year);
					last = course;
					lastyear = year;
				}
			}
			System.out.println();
			System.out.print("student");
			for (String student : students) {
				System.out.print(","+student);				
			}
			System.out.println();
			
			for (String modulecode : modulecodes) {
				System.out.print(modulecode);
				HashMap<String,String> module = modulemap.get(modulecode);
				TreeSet<String> requisites = new TreeSet<String>();
				// TODO parse to requisites
				if (module!=null) {
					String prerequisites = module.get(PREREQUISITES_NAME);
					StringTokenizer toks = new StringTokenizer(prerequisites, " ");
					while(toks.hasMoreTokens())
						requisites.add(toks.nextToken().trim());
					String corequisites = module.get(COREQUISITES_NAME);
					toks = new StringTokenizer(prerequisites, " ");
					while(toks.hasMoreTokens())
						requisites.add(toks.nextToken().trim());
				}
				for (String student : students) {
					System.out.print(",");
					HashMap<String,String> studentModules = modulesDone.get(student);
					if (studentModules.containsKey(modulecode))
						System.out.print(studentModules.get(modulecode));
					else {
						// TODO - options
						// was it an option/compulsory for them?
						String course = studentCourses.get(student);
						CourseInfo ci = courses.get(course);
						if (ci!=null) {
							boolean include = false;
							for (int i=0; !include && i<ci.compulsory.length; i++)
								if (ci.compulsory[i].contains(modulecode))
									include = true;
							for (int i=0; !include && i<ci.alternative.length; i++)
								if (ci.alternative[i].contains(modulecode))
									include = true;
							for (int i=0; !include && i<ci.restricted.length; i++)
								if (ci.restricted[i].contains(modulecode))
									include = true;
							if (include) {
								// do they have pre-requisites?
								// check requisites
								boolean ok = true;
								for (String requisite: requisites) {
									if (!studentModules.containsKey(requisite)) {
										// could they still do it in the correct year?, if so its ok
										int level = requisite.charAt(2)-'0';
										if (studentYear.get(student)<level)
											continue;
										ok = false;
									}
								}
								if (ok)
									System.out.print("o");
								else
									System.out.print("N");
							}
							else
								System.out.print("_");
						}
						else
							System.out.print("?");
					}
				}
				System.out.println();
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	static class StudentComparator implements Comparator<String> {

		// prefered course order
		static String [] coursesInOrder = new String[] {
			"G400", "G601", "G4G7", "G4H6", "GN42", "GNK1", "GG14", "GG41"
		};
		
		HashMap<String,String> studentCourses;
		HashMap<String,Integer> studentYear;
		HashMap<String,HashMap<String,String>> modulesDone;
		LinkedList<String> modulecodes;
		/** cons */
		StudentComparator(HashMap<String,String> studentCourses, HashMap<String,Integer> studentYear, 
				HashMap<String,HashMap<String,String>> modulesDone, LinkedList<String> modulecodes) {
			this.studentCourses = studentCourses;
			this.studentYear = studentYear;
			this.modulesDone = modulesDone;
			this.modulecodes = modulecodes;
		}
		//@Override
		public int compare(String s1, String s2) {
			String c1 = studentCourses.get(s1);
			String c2 = studentCourses.get(s2);
			int c = GroupModuleComparator.compareStringsByRank(c1, c2, coursesInOrder);
			if (c!=0)
				return c;
			Integer y1 = studentYear.get(s1);
			Integer y2 = studentYear.get(s2);
			c = StandardModuleComparator.compareStrings(y1.toString(), y2.toString());
			if (c!=0)
				return c;
			// now by module choice(s)
			HashMap<String,String> m1 = modulesDone.get(s1);
			HashMap<String,String> m2 = modulesDone.get(s2);
			for (String modulecode : modulecodes) {
				String d1 = m1.containsKey(modulecode) ? "1" : "0";
				String d2 = m2.containsKey(modulecode) ? "1" : "0";
				c = StandardModuleComparator.compareStrings(d1, d2);
				if (c!=0)
					return c;
			}
			return StandardModuleComparator.compareStrings(s1,s2);
		}
		
	}
	public static final String LEVEL_NAME = "level";
	public static final String SEMESTER_NAME = "semester";
	public static final String MODULECODE_NAME = "modulecode";
	public static final String MODULETITLE_NAME = "moduletitle";
	public static final String NEWGROUP_NAME = "newgroup";
	public static final String PREREQUISITES_NAME = "prerequisites";
	public static final String COREQUISITES_NAME = "corequisites";

	// export 1
	/** header constant */
	static final String MODULE_CODE = "Module_mnem";
	/** header constant */
	static final String YEAR_OF_COURSE = "Year_of_course";
	/** header constant */
	static final String YEAR = "Year";
	/** header constant */
	static final String STUDENT_ID = "Student_ID";
	/** header constant */
	static final String COURSE_CODE = "UCAS_Course";

	static class ModuleEntryInfo {
		String student;
		String course;
		int yearOfCourse;
		String year;
		String modulecode;
	}
	
	static Vector<ModuleEntryInfo> readStudentModuleExport(String filename) throws IOException {
		logger.info("Reading export "+filename);
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String header = br.readLine();
		if (header==null) {
			System.err.println("Could not read header line from "+filename);
			System.exit(-1);
		}
		//"Student_ID","Surname","Forename","UCAS_Course","Year_of_course","Module_mnem","Year","Module_Title","Semester","Credit","Resit"

		Vector<ModuleEntryInfo> lines = new Vector<ModuleEntryInfo>();
		Vector<String> headers = CsvUtils.parseCsvLine(header);
		int moduleCodeIndex = headers.indexOf(MODULE_CODE);
		if (moduleCodeIndex<0) {
			System.err.println("Could not find column '"+MODULE_CODE+"' in "+filename);
			System.exit(-1);			
		}
		int modulecodeIndex = headers.indexOf(MODULE_CODE);
		if (modulecodeIndex<0) {
			System.err.println("Could not find column '"+MODULE_CODE+"' in "+filename);
			System.exit(-1);			
		}
		int yearOfCourseIndex = headers.indexOf(YEAR_OF_COURSE);
		if (yearOfCourseIndex<0) {
			System.err.println("Could not find column '"+YEAR_OF_COURSE+"' in "+filename);
			System.exit(-1);			
		}
		int yearIndex = headers.indexOf(YEAR);
		if (yearIndex<0) {
			System.err.println("Could not find column '"+YEAR+"' in "+filename);
			System.exit(-1);			
		}
		int studentIndex = headers.indexOf(STUDENT_ID);
		if (studentIndex<0) {
			System.err.println("Could not find column '"+STUDENT_ID+"' in "+filename);
			System.exit(-1);			
		}
		int courseIndex = headers.indexOf(COURSE_CODE);
		if (courseIndex<0) {
			System.err.println("Could not find column '"+COURSE_CODE+"' in "+filename);
			System.exit(-1);			
		}
		int courseCodeIndex = headers.indexOf(COURSE_CODE);
		if (courseCodeIndex<0) {
			System.err.println("Could not find column '"+COURSE_CODE+"' in "+filename);
			System.exit(-1);			
		}
		int count = 0;
		while (true) {
			String line = br.readLine();
			if (line==null)
				break;
			count++;
			Vector<String> values = CsvUtils.parseCsvLine(line);
			try {
				ModuleEntryInfo mei = new ModuleEntryInfo();
				mei.modulecode = values.elementAt(moduleCodeIndex);
				mei.student = values.elementAt(studentIndex);
				mei.course = values.elementAt(courseIndex);
				mei.year = values.elementAt(yearIndex);
				mei.yearOfCourse = Integer.parseInt(values.elementAt(yearOfCourseIndex));
				lines.add(mei);
			}
			catch (Exception e) {
				System.err.println("Error handling line "+line+": "+e);
				e.printStackTrace(System.err);
			}
		}
		logger.info("# read "+count+" entries in total");
		return lines;

	}
}
