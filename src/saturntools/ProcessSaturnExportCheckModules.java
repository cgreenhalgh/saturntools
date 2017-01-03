/**
 * 
 */
package saturntools;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import saturntools.ProcessSaturnExportMarks.Mark;
import saturntools.ProcessSaturnExportStudents.Qualification;
import saturntools.ProcessSaturnExportStudents.Student;
import saturntools.ProcessSaturnExportStudents.TranscriptMarkComparator;

/**
 * Program to check Module enrolment (for current session) for students based on Degree, supplementary regs, 
 * UNQF, etc.
 * 
 * @author cmg
 *
 */
public class ProcessSaturnExportCheckModules extends
		ProcessSaturnExportStudents {

	private static final String VERSION_STRING = "0.1 2012-09-24 16:20";
	private static final String FD_ATTENDANCE_STATUS = "Attendance status";
	static final String AS_STUDYING_AT_THE_UNIVERSITY = "Studying at the University";
	static final String FD_SPECIAL_STUDENT_STATUS = "Special Student Status";
	static final String SSS_NOT_APPLICABLE = "Not applicable.";
	static final String FD_LOCAL_EMAIL = "Local email";
	
	static final String SEMESTER_AUTUMN = "Autumn";
	static final String SEMESTER_SPRING = "Spring";
	static final String SEMESTER_FULL_YEAR = "Full Year";
	
	static CourseInfo getCourseInfo(List<CourseInfo> cis, Student s, Qualification qual) {
		for (CourseInfo ci : cis) {
			for (int j=0; j<qual.fullNames.length; j++) {
				String title = qual.fullNames[j]+" "+s.courseTitle;
				if (title.equals(ci.title))
					return ci;
			}
		}
		return null;
	}
	
	static final String COMMENT_IF_NOT_ALREADY_TAKEN = "if not taken already";
	private static final int MAX_SEMESTER_CREDITS = 70;
	
	static class ModuleWhitelist {
		public String moduleprefix;
		public int minLevel, maxLevel;
		public String comment;
		public TreeSet<String> coursecodes;
		public ModuleWhitelist(String moduleprefix, 
				int minLevel, int maxLevel, String comment) {
			super();
			this.moduleprefix = moduleprefix;
			this.minLevel = minLevel;
			this.maxLevel = maxLevel;
			this.comment = comment;
		}
	}
	
	static ModuleWhitelist whitelists[] = new ModuleWhitelist[] {
		new ModuleWhitelist("G51IAI", 2,4, COMMENT_IF_NOT_ALREADY_TAKEN),
		new ModuleWhitelist("G51FUN", 2,4, COMMENT_IF_NOT_ALREADY_TAKEN),
		new ModuleWhitelist("G51DBS", 2,4, COMMENT_IF_NOT_ALREADY_TAKEN),
		new ModuleWhitelist("G51WPS", 2,4, COMMENT_IF_NOT_ALREADY_TAKEN),
		new ModuleWhitelist("G54ALG", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54ARC", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54MXR", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54ACC", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54CCS", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54CPL", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54DIA", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54DMT", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54FOP", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54FPP", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54MDP", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54ORM", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54PDC", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54SIM", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G54VIS", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G64ADS", 3,3, "if Part I>=55%"),
		new ModuleWhitelist("G52HCI", 2,2, "should be in spec"),
		new ModuleWhitelist("G52", 3,4, COMMENT_IF_NOT_ALREADY_TAKEN),
		new ModuleWhitelist("G53", 4,4, COMMENT_IF_NOT_ALREADY_TAKEN),
		new ModuleWhitelist("HG1M11", 2,4, "if advanced maths"),
		new ModuleWhitelist("HG1M12", 2,4, "if advanced maths"),
		new ModuleWhitelist("HG2M03", 2,4, "if advanced maths"),
		new ModuleWhitelist("HG2M13", 2,4, "if advanced maths"),
		new ModuleWhitelist("HG1FNC", 2,3, "if not A-level/Foundation maths"),
	};
	
	static ModuleWhitelist blacklists[] = new ModuleWhitelist[] {
		new ModuleWhitelist("G51PRG", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51OOP", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51CSA", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51UST", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51MCS", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51APS", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51FSE", 2,4, "qualifying year only"),
		new ModuleWhitelist("G51REQ", 2,4, "qualifying year only"),
		new ModuleWhitelist("HG1M01", 2,4, "wrong target students"),
		new ModuleWhitelist("HG2M02", 2,4, "wrong target students"),
		new ModuleWhitelist("HG0FAM", 1,4, "foundation level only"),
		new ModuleWhitelist("G53ORO", 1,4, "not available 2013/14"),
		new ModuleWhitelist("G53VIS", 1,4, "not available 2013/14"),
		new ModuleWhitelist("N12503", 1,4, "overlaps G51DBS"),
		new ModuleWhitelist("G64DBS", 1,4, "MSc only"),
		new ModuleWhitelist("G64ICP", 1,4, "MSc only"),
		new ModuleWhitelist("G54PRG", 1,4, "MSc only"),
	};
	static String checkWhitelist(String modulecode, int stageLevel) {
		return checkList(modulecode, stageLevel, whitelists);
	}
	static String checkBlacklist(String modulecode, int stageLevel) {
		return checkList(modulecode, stageLevel, blacklists);
	}
	static String checkList(String modulecode, int stageLevel, ModuleWhitelist list[]) {
		for (ModuleWhitelist mw : list) {
			if (stageLevel<mw.minLevel || stageLevel>mw.maxLevel)
				continue;
			if (modulecode.startsWith(mw.moduleprefix))
				return mw.comment;
		}
		return null;
	}
	
	static class ModuleEquivalence {
		String modulecode;
		TreeSet<String> modulecodes;
		public ModuleEquivalence(String modulecode, String [] modulecodes) {
			super();
			this.modulecode = modulecode;
			this.modulecodes = new TreeSet<String>();
			for (String mc : modulecodes)
				this.modulecodes.add(mc);
			this.modulecodes.add(modulecode);
		}
	}
	static ModuleEquivalence[] moduleEquavalences = new ModuleEquivalence[] {
		new ModuleEquivalence("G51IRB", new String[] { "G52IRB" }),
		new ModuleEquivalence("G51FSE", new String[] { "G51ISE" }),
		new ModuleEquivalence("G52PAS", new String[] { "G52AIM" }),
		new ModuleEquivalence("G52APT", new String[] { "G52AIP" }),
		new ModuleEquivalence("G53ARB", new String[] { "G52ARB" }),
		new ModuleEquivalence("G53CMP", new String[] { "G52CMP" }),
		new ModuleEquivalence("G53DVA", new String[] { "G52DOA" }),
		new ModuleEquivalence("G53DOC", new String[] { "G52DOC" }),
		new ModuleEquivalence("G54HPA", new String[] { "G52HPA" }),
		new ModuleEquivalence("G52IIP", new String[] { "G52IVG" }),
		new ModuleEquivalence("G52SEM", new String[] { "G52LSS", "G53LSS" }),
		new ModuleEquivalence("G52IFR", new String[] { "G52MC2" }),
		new ModuleEquivalence("G53GRA", new String[] { "G53AGR" }),
		new ModuleEquivalence("G54DTP", new String[] { "G54CFR", "G53CFR" }),
		new ModuleEquivalence("G53CCT", new String[] { "G53DBC" }),
		new ModuleEquivalence("G54DIA", new String[] { "G53DIA" }),
		new ModuleEquivalence("G54ALG", new String[] { "G54AOR" }),
		new ModuleEquivalence("G54ORM", new String[] { "G54AOR" }),
		// 2009/10 & earlier new ModuleEquivalence("G54PRG", new String[] { "G64ICP" }),
		new ModuleEquivalence("G54IHC", new String[] { "G64IHF" }),
		new ModuleEquivalence("G53NMD", new String[] { "G64PMM" }),
		new ModuleEquivalence("G54ADM", new String[] { "G64UET" }),
		new ModuleEquivalence("G52CPP", new String[] { "G52CFJ" }),
	};
	static String getModuleEquivalent(String modulecode) {
		for (ModuleEquivalence me : moduleEquavalences)
			if (me.modulecodes.contains(modulecode))
				return me.modulecode;
		return modulecode;
	}
	static String[][] moduleOverlaps = new String[][] {
		new String[] { "G52CPP", "G64OOP" },
		new String[] { "G52HCI", "G54IHC" },
		new String[] { "G51DBS", "G64DBS" },
		new String[] { "G51IRB", "G53ARS" },
	};
	static class StudentCohortComparator implements java.util.Comparator<Student> {

		@Override
		public int compare(Student o1, Student o2) {
			int cy = o1.yearOfCourse.compareTo(o2.yearOfCourse);
			if (cy!=0)
				return cy;
			int cc = o1.courseCode.compareTo(o2.courseCode);
			if (cc!=0)
				return cc;
			int ch = o1.qualification.compareTo(o2.qualification);
			if (ch!=0)
				return ch;
			int cs = o1.id.compareTo(o2.id);
			return cs;
		}
		
	}
	
	static void printWarnings(PrintWriter pw, List<String> warnings) {
		if (warnings.size()>0)
			pw.println("<p>Notes and warnings</p>");
		for (String w : warnings) {
			pw.println("<p><b>"+w+"</b></p>");
		}
		pw.println();
	}
	static void addWarning(List<String> warnings, String w) {
		warnings.add(w);
	}

	static void printReadingFile(PrintWriter pw, File file, String content) {
		String msg = "Reading "+content+" from "+file+", size "+file.length()+", last modified "+new Date(file.lastModified());
		pw.println("<p>"+msg+"</p>");
		System.out.println(msg);
	}
	static void printEnrolmentMarkSet(PrintWriter pw, String title, List<Mark> smarks, boolean endTable) {
		int levelcredits[] = getLevelCredits(smarks);
		int totalcredits = getTotalCredits(smarks);
		if (smarks.size()==0) {
			pw.println("<p>No enrolments found</p>");
			return;
		}
		pw.println("<p>Enrolments for "+title+"</p>");
		pw.println("<table border='1'>");		
		pw.print("<tr><th>credits</th><th>level0</th><th>level1</th><th>level2</th><th>level3</th><th>level4</th><th>level5</th></tr>");
		pw.print("<tr><td>"+totalcredits+"</td><td>"+suppressZero(levelcredits[0])+"</td><td>"+suppressZero(levelcredits[1])+"</td><td>"+suppressZero(levelcredits[2])+"</td><td>"+suppressZero(levelcredits[3])+"</td><td>"+suppressZero(levelcredits[4])+"</td><td>"+suppressZero(levelcredits[5])+"</td></tr>");
		pw.println("</table>");

		pw.println("<p>Specific modules</p>");
		
		pw.println("<table border='1'>");		
		pw.println("<tr>");
		for (Mark m : smarks) 
			pw.print("<th>"+m.module+"</th>");
		pw.println("</tr>");
		// credits
		pw.print("<tr>");
		for (Mark m : smarks) 
			pw.print("<td>"+m.credit+"</td>");
		pw.println("</tr>");
		// semester
		pw.print("<tr>");
		for (Mark m : smarks) 
			pw.print("<td>"+m.semester+"</td>");
		pw.println("</tr>");
		if(endTable)
			pw.println("</table>");
	}

	private static String suppressZero(int i) {
		if (i==0)
			return "";
		return Integer.toString(i);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length<3) {
			System.err.println("Usage: <student-further-details> <supp-regs-folder> <current-modules3> [<previous-modules3> ... ]");
			System.exit(-1);
		}
		try {
			String date = new Date().toGMTString();
			PrintWriter pw = new PrintWriter(new FileWriter("out_check_modules.html"));
			printHeader(pw);
			pw.println("<p>Generated by saturntools.ProcessSaturnExportCheckModules, version "+VERSION_STRING+", "+date+"</p>");

			HashMap<String,HashMap<String,String>> furtherdetails = new HashMap<String,HashMap<String,String>>();
			File sdfile = new File(args[0]);
			printReadingFile(pw, sdfile, "student further details");
			furtherdetails.putAll(ReadCsvFile.readCsvFile(sdfile, "student_id", false));

			File regsDir = new File(args[1]);
			if (!regsDir.exists() || !regsDir.isDirectory()) {
				System.err.println("Regulations directory not found: "+regsDir);
				System.exit(-1);
			}
			File regsFiles [] = regsDir.listFiles();
			List<CourseInfo> cis = new LinkedList<CourseInfo> ();
			for (int ri=0; ri<regsFiles.length; ri++) {
				printReadingFile(pw, regsFiles[ri], "supplementary regulations");
				CourseInfo ci = CourseInfo.processCourseFile(regsFiles[ri]);
				pw.println("<p>Read regs for "+ci.coursecode+" "+ci.title+" ("+ci.year+")</p>");
				cis.add(ci);
			}
			
			List<Mark> newmarks = new LinkedList<Mark>();
			File newmarksf = new File(args[2]);
			printReadingFile(pw, newmarksf, "saturn enrolments (modules3 export)");
			newmarks.addAll(ProcessSaturnExportMarks.readMarks(newmarksf));				
			//ProcessSaturnExportMarks.discardDuplicateMarks(allmarks);
			Map<String,List<Mark>> studentnewmarks = ProcessSaturnExportMarks.getMarksByStudent(newmarks);
			
			int newyear = newmarks.size()>0 ? newmarks.get(0).year : -1;
			
			List<Mark> diffnewmarks = new LinkedList<Mark>();
			List<Mark> oldmarks = new LinkedList<Mark>();
			for (int ai=3; ai<args.length; ai++) {
				File f = new File(args[ai]);
				printReadingFile(pw, f, "saturn marks (modules3 export)");
				oldmarks.addAll(ProcessSaturnExportMarks.readMarks(f));	
				if (ai==3) {
					int oldyear = oldmarks.size()>0 ? oldmarks.get(0).year : -1;
					if (oldyear==newyear) {
						diffnewmarks.addAll(oldmarks);
						oldmarks.clear();
						pw.println("<p>CHecking changes in registration (only) with "+f+"</p>");
					}
				}
			}
			ProcessSaturnExportMarks.discardDuplicateMarks(oldmarks);
			Map<String,List<Mark>> studentoldmarks = ProcessSaturnExportMarks.getMarksByStudent(oldmarks);
			ProcessSaturnExportMarks.discardDuplicateMarks(diffnewmarks);
			Map<String,List<Mark>> studentdiffnewmarks = ProcessSaturnExportMarks.getMarksByStudent(diffnewmarks);

			TreeSet<String> students = new TreeSet<String>();
			students.addAll(furtherdetails.keySet());
			System.out.println("Further details has "+students.size()+" students");

			pw.println();

			ArrayList<Student> studentList = new ArrayList<Student> ();
			for (String sid : students) {
				HashMap<String,String> sfd = furtherdetails.get(sid);
				if (sfd==null) {
					pw.println("\"Warning: could not find furtherdetails for student "+sid+"\"");
					pw.println();
					continue;
				}
				Student s= new Student();
				s.id = sid;
				s.courseCode = sfd.get(FD_COURSE_CODE);
				s.dateOfEntry = sfd.get(FD_DATE_OF_ENTRY);
				s.firstNames = sfd.get(FD_FIRST_NAMES);
				s.modeOfStudy = sfd.get(FD_MODE_OF_STUDY);
				//modesOfStudy.add(s.modeOfStudy);
				s.qualification = sfd.get(FD_DEGREE_QUAL_AIMS);
				//qualifications.add(s.qualification);
				s.registered = sfd.get(FD_REGISTERED).startsWith("Y");
				s.surname = sfd.get(FD_SURNAME);
				s.yearOfCourse = sfd.get(FD_YEAR_OF_COURSE);
				s.courseTitle = sfd.get(FD_DEGREE_TITLE);
				s.attendanceStatus = sfd.get(FD_ATTENDANCE_STATUS);
				s.specialStudentStatus = sfd.get(FD_SPECIAL_STUDENT_STATUS);
				s.email = sfd.get(FD_LOCAL_EMAIL);
				//yearsOfCourse.add(s.yearOfCourse);
				studentList.add(s);
			}
			Collections.sort(studentList, new StudentCohortComparator());
			String yearOfCourse = "";
			String courseCode = "";
			String qualification = "";
			
			
			
			for (Student s : studentList) {
				String sid = s.id;
				LinkedList<String> warnings = new LinkedList<String>();
								
				if (!yearOfCourse.equals(s.yearOfCourse)) {
					yearOfCourse = s.yearOfCourse;
					pw.println("<hr>");
					pw.println("<h2>Year of Course "+yearOfCourse+"</h2>");
					//pw.println();
				}
				if (!courseCode.equals(s.courseCode) || !qualification.equals(s.qualification)) {
					courseCode = s.courseCode;
					qualification = s.qualification;
					pw.println("<hr>");
					pw.println("<h3>Course "+courseCode+" "+qualification+" "+s.courseTitle+"</h3>");
				}
				
				// TODO
				List<Mark> snewmarks = studentnewmarks.get(sid);
				if (snewmarks==null) {
					//pw.println("\"Warning: could not find any enrolments for student "+sid+" ("+s+")\"");
					snewmarks = new LinkedList<Mark>();
				}
				List<Mark> sdiffnewmarks = studentdiffnewmarks.get(sid);
				if (sdiffnewmarks==null) {
					//if (snewmarks.size()>0)
					//	pw.println("\"Warning: could not find any previous enrolments for student "+sid+" ("+s+")\"");
					sdiffnewmarks = new LinkedList<Mark>();
				}
				List<Mark> soldmarks = studentoldmarks.get(sid);
				if (soldmarks!=null) {
					Collections.sort(soldmarks, new TranscriptMarkComparator());
				}
				else
					soldmarks = new LinkedList<Mark>();
				
				pw.println("<hr>");
				pw.println("<h4>Module enrolment report for "+s.surname+", "+s.firstNames+" - "+date+"</h4>");
				
				// general info table
				pw.println("<p>General information</p>");
				pw.println("<table border='1'>");
				pw.println("<tr><th>id</th><th>surname</th><th>firstNames</th><th colspan='2'>email</th></tr>");
				pw.println("<tr><td>"+s.id+"</td><td>"+s.surname+"</td><td>"+s.firstNames+"</td><td colspan='2'>"+s.email+"</td></tr>");
				pw.println("<tr><th>course</th><th>qual</th><th colspan='3'>coursetitle</th>");
				pw.println("<tr><td>"+s.courseCode+"</td><td>"+s.qualification+"</td><td colspan='3'>"+s.courseTitle+"</td></tr>");
				pw.println("<tr><th>mode</th><th>yearOnCourse</th><th>registered</th><th>attendance</th><th>special</th></tr>");
				pw.println("<tr><td>"+s.modeOfStudy+"</td><td>"+s.yearOfCourse+"</td><td>"+s.registered+"</td><td>"+s.attendanceStatus+"</td><td>"+s.specialStudentStatus+"</td></tr>");
				pw.println("</table>");
				//,course,qual,coursetitle,mode,yearOnCourse,registered,attendance,special,email");
				//level1credits,level2credits,level3credits,level4credits,level5credits");
				// level credits
				/*{
					int levelcredits [] = new int[6];
					for (Mark m : snewmarks) {
						int level = getModuleLevel(m.module);
						levelcredits[level] += m.credit;
					}
					pw.print(s.id+",\""+s.surname+"\",\""+s.firstNames+"\","+s.courseCode+","+s.qualification+","+s.courseTitle+","+s.modeOfStudy+","+s.yearOfCourse+","+s.registered);
					for (int i=1; i<=5; i++) 
						pw.print(","+levelcredits[i]);
					pw.println();
				}*/
				//pw.println(s.id+",\""+s.surname+"\",\""+s.firstNames+"\","+s.courseCode+","+s.qualification+",\""+s.courseTitle+"\","+s.modeOfStudy+","+s.yearOfCourse+","+s.registered+","+s.attendanceStatus+","+s.specialStudentStatus+","+s.email);

				// first we need to find the qualification...
				Qualification qual = getQualification(s.qualification);
				if (qual==null) {
					addWarning(warnings, "Warning: could not find qualification "+s.qualification+" for student "+sid);
					printWarnings(pw, warnings);
					continue;
				}

				if (!MOS_FULL_TIME.equals(s.modeOfStudy)) {
					addWarning(warnings, "Warning: cannot handle non-FT study ("+s.modeOfStudy+") for student "+sid);
					printWarnings(pw, warnings);
					continue;
				}
				
				// check Attendance status, e.g. Studying at the University vs Resit - not in attendance
				if (!AS_STUDYING_AT_THE_UNIVERSITY.equals(s.attendanceStatus)) {
					addWarning(warnings, "Warning: Attendance status non-standard: "+s.attendanceStatus);
				}
				// check Special Student Status, e.g. Not applicable., Incoming UNMC Mobility Scheme (Exchange), UNNC Defined Programme, Outgoing UNMC Mobility Scheme, ...
				if (!SSS_NOT_APPLICABLE.equals(s.specialStudentStatus)) {
					addWarning(warnings, "Note: Special student status non-standard: "+s.specialStudentStatus);
				}
				
				// past session marks/credits
				TreeSet<Integer> years = new TreeSet<Integer>();
				for (Mark m : soldmarks) {
					years.add(m.year);
				}
				if (years.size()>0) {
					pw.println("<p>Past study on local student records</p>");
					pw.println("<table border='1'>");					
					pw.println("<tr><th>session</th><th>credits</th><th>average</th><th>level0</th><th>level1</th><th>level2</th><th>level3</th><th>level4</th><th>level5</th></tr>");
					for (Integer year : years) {
						pw.print("<tr><td>"+year+"</td>");
						int credits = 0;
						double total = 0;
						for (Mark m: soldmarks) {
							if (m.year==year) {
								credits += m.credit;
								if (m.bestmark()!=null) 
									total += m.credit*m.bestmark();
							}
						}
						pw.print("<td>"+credits+"</td>");
						if (credits>0)
							pw.print("<td>"+Math.round(total/credits)+"</td>");
						else
							pw.print("<td></td>");
						for (int l=0; l<=5; l++) {
							credits = 0;
							for (Mark m: soldmarks) {
								if (m.year==year && getModuleLevel(m.module)==l) {
									credits += m.credit;
								}
							}
							pw.print("<td>"+(credits>0 ? ""+credits : "")+"</td>");
						}
						pw.println("</tr>");
					}
					pw.println("</table>");
				}				
				// check yearOfCourse vs Stages of Qualification
				int stageLevel = guessStageLevel(snewmarks);
				Stage stage = null;
				if (stageLevel<0) {
					addWarning(warnings, "Warning: cannot guess stage from module enrolment");
					if (MOS_FULL_TIME.equals(s.modeOfStudy)) {
						// guess from yourOnCourse?
						try {
							int yoc = Integer.parseInt(s.yearOfCourse);
							int si = 0;
							while(si<qual.stages.length && qual.stages[si].optional)
								si++;
							if (si+yoc-1 < qual.stages.length) {
								stage = qual.stages[si+yoc-1];
								stageLevel = stage.requirements[stage.requirements.length-1].level;
							}
						}
						catch (Exception e) {
							System.err.println("Malformed yearOnCourse "+s.yearOfCourse+" for student "+s.id);
						}
					}
					if (stage==null) {
						printEnrolmentMarkSet(pw, "Unknown", snewmarks, true);
						printWarnings(pw, warnings);
						continue;
					}
				}
				if (sdiffnewmarks.size()>0) {
					TreeSet<String> addmodulecodes = new TreeSet<String>();
					for (Mark m : snewmarks) 
						addmodulecodes.add(m.module);
					TreeSet<String> delmodulecodes = new TreeSet<String>();
					for (Mark m : sdiffnewmarks) {
						if (addmodulecodes.contains(m.module))
							addmodulecodes.remove(m.module);
						else
							delmodulecodes.add(m.module);
					}
					if (addmodulecodes.size()>0) {
						StringBuilder sb = new StringBuilder();
						for (String modulecode : addmodulecodes) {
							sb.append(modulecode);
							sb.append(" ");
						}
						addWarning(warnings, "Has recently enrolled in: "+sb);
					}
					if (delmodulecodes.size()>0) {
						StringBuilder sb = new StringBuilder();
						for (String modulecode : delmodulecodes) {
							sb.append(modulecode);
							sb.append(" ");
						}
						addWarning(warnings, "Has recently unenrolled from: "+sb);
					}
				}
				
				if (stage==null) {
					// guess from level
					for (int si=0; si<qual.stages.length; si++) {
						Stage ts = qual.stages[si];
						int sl = ts.getStageLevel();
						if (sl==stageLevel)
							stage = ts;
					}
				}
				if (stage==null) {
					printEnrolmentMarkSet(pw, "Unknown (level "+stageLevel+")", snewmarks, true);
					addWarning(warnings, "Warning: could not find stage for level "+stageLevel);
					printWarnings(pw, warnings);
					continue;
				}
				printEnrolmentMarkSet(pw, stage.name, snewmarks, false);
				
				// stage requirements
				boolean ok = true;
				boolean requirementsOk = satifiesReqirements(stage.requirements, snewmarks);
				if (!requirementsOk) {
					addWarning(warnings, "Warning: does not satisfy "+stage.name+" requirements for credits/levels ("+requirementsText(stage.requirements)+")");
					//pw.println();
					//continue;
					ok = false;
				}
				int totalcredits = getTotalCredits(snewmarks);
				if (totalcredits>stage.requirements[0].credits) {
					addWarning(warnings, "Warning: too many credits for "+stage.name+" ("+totalcredits+" vs "+stage.requirements[0].credits+")");
					//pw.println();
					//continue;
					ok = false;
				}
				
				boolean possibleDirectEntry = false;
				// overall award requirements (e.g. MSci level 4 credits)
				if (stage==qual.stages[qual.stages.length-1]) {
					// final stage of award
					ArrayList<Mark> allmarks = new ArrayList<Mark>();
					allmarks.addAll(snewmarks);
					allmarks.addAll(soldmarks);
					if (!satifiesReqirements(qual.requirements, allmarks)) {
						// handle direct entry/UNNC into Part I
						int allcredits = getTotalCredits(allmarks);
						// check stages, skip optional, add fake mark for stage, re-check
						for (int si=0; si<qual.stages.length-1; si++) {
							Stage ss = qual.stages[si];
							if (ss.optional)
								continue;
							if (ss.requirements[0].credits+allcredits>qual.requirements[0].credits)
								// too many
								break;
							int credits = 0;
							for (int ri=ss.requirements.length-1; ri>=0; ri--) {
								Requirement sr = ss.requirements[ri];
								if (sr.credits>credits) {
									Mark direct = new Mark();
									direct.module = "DI"+sr.level+"ECT";
									direct.credit = sr.credits-credits;
									credits += direct.credit;		
									allmarks.add(direct);
								}
							}
							allcredits += credits;
							if (satifiesReqirements(qual.requirements, allmarks)) {
								possibleDirectEntry = true;
								addWarning(warnings, "Note: satisfies overall requirements for credits/level if direct entry after "+ss.name);
								break;
							}
						}
						if (!possibleDirectEntry) {
							addWarning(warnings, "Warning: does not satisfy overall requirements for credits/level ("+requirementsText(qual.requirements)+")");						
							ok = false;
						}
					} 
					int allcredits = getTotalCredits(allmarks);
					if (allcredits>qual.requirements[0].credits) {
						addWarning(warnings, "Warning: too many credits overall ("+allcredits+" vs "+qual.requirements[0].credits+")");
						//pw.println();
						//continue;
						ok = false;
					}
				}
				
				// course-specific options, etc.
				CourseInfo ci = getCourseInfo(cis, s, qual);
				if (ci==null) {
					// end enrolment table
					pw.println("</table>");
					addWarning(warnings, "Warning: cannot find regulations for "+qual.title+" "+s.courseTitle);
					printWarnings(pw, warnings);
					continue;
				}
				
				int stagei = 0;
				if (!ci.mscflag) {
					if (stageLevel<1 || stageLevel>ci.compulsory.length) {
						// end enrolment table
						pw.println("</table>");
						addWarning(warnings, "Warning: apparent stage level "+stageLevel+" incompatible with regs for "+ci.title);
						printWarnings(pw, warnings);
						continue;
					}
					stagei = stageLevel-1;
				}
				else {
					if (stageLevel!=4) {
						// end enrolment table
						pw.println("</table>");
						addWarning(warnings, "Warning: apparent stage level "+stageLevel+" incompatible with MSc regs for "+ci.title);
						printWarnings(pw, warnings);
						continue;						
					}
					stagei = 0;
				}
				
				// like printEnrolment
				pw.print("<tr>");
				
				TreeSet<String> compulsory= new TreeSet<String>();
				compulsory.addAll(ci.compulsory[stagei]);
				TreeSet<String> modules = new TreeSet<String>();

				for (Mark m : snewmarks) {
					if (compulsory.contains(m.module)) {
						compulsory.remove(m.module);
						pw.print("<td>Compulsory</td>");
						//modules.remove(m.module);
					}
					else if (ci.alternative[stagei].contains(m.module)) {
						pw.print("<td>Alternative</td>");
						//modules.remove(m.module);
					}
					else if (ci.restricted[stagei].contains(m.module)) {
						pw.print("<td>Optional</td>");
						//modules.remove(m.module);						
					}
					else {
						pw.print("<td>Not in spec</td>");
						modules.add(m.module);
					}

					String module = m.module;
					// duplicate/equivalents
					String equivcode = getModuleEquivalent(module);
					for (Mark som : soldmarks) {
						String ec = getModuleEquivalent(som.module);
						if (ec.equals(equivcode)) {
							if (module.equals(som.module))
								addWarning(warnings, "Warning: student has taken the same module in session "+som.year+"/"+(som.year+1)+" "+som.module);
							else
								addWarning(warnings, "Warning: student has taken equivalent module in session "+som.year+"/"+(som.year+1)+" "+som.module+" cf "+module);
						}
					}
					for (Mark som : snewmarks) {
						if (som==m)
							continue;
						String ec = getModuleEquivalent(som.module);
						if (ec.equals(equivcode)) {
							addWarning(warnings, "Warning: student is taking equivalent module in current session "+som.module+" cf "+module);
						}
					}
					for (String [] ms : moduleOverlaps) {
						boolean found = false;
						for (String mc : ms)
							if (mc.equals(equivcode)) {
								found = true;
								break;
							}
						if (found) {
							//System.out.println("Found moduleOverlaps for "+equivcode);
							for (String mc : ms) {
								if (!mc.equals(equivcode)) {
									// other modules in overlap set
									for (Mark som : soldmarks) {
										String ec = getModuleEquivalent(som.module);
										if (ec.equals(mc)) {
											addWarning(warnings, "Warning: student has taken overlapping module in session "+som.year+"/"+(som.year+1)+" "+som.module+" cf "+module);
										}
									}						
									for (Mark som : snewmarks) {
										if (som==m)
											continue;
										String ec = getModuleEquivalent(som.module);
										if (ec.equals(mc)) {
											addWarning(warnings, "Warning: student is taking overlapping module in current session "+som.module+" cf "+module);
										}
									}						
								}
							}
						}
					}
					
}
				pw.println("</tr>");
				// enrolment table
				pw.println("</table>");
				
				for (String module : compulsory) {
					addWarning(warnings, "Warning: missing compulsory module "+module);
				}
				
				// optional modules
				for (String module : modules) {
					
					String comment = checkBlacklist(module, stageLevel);
					if (comment!=null) {
						addWarning(warnings, "Warning: blacklisted option "+module+" "+comment);
					} else {
						comment = checkWhitelist(module, stageLevel);
						if (comment!=null) {
							if (COMMENT_IF_NOT_ALREADY_TAKEN.equals(comment)) {
								if (soldmarks.size()>0)
									addWarning(warnings, "Note: option "+module+" "+comment+" (OK from local records)");
								else
									addWarning(warnings, "Note: option "+module+" "+comment+" (no local records to check)");
							}
							else
								addWarning(warnings, "Note: option "+module+" "+comment);
						}
						else {
							// another stage option?
							boolean oscompulsory = false, osoptional = false;
							int os = -1;
							for (int si=0; si<ci.compulsory.length; si++) {
								if (ci.compulsory[si].contains(module)) {
									oscompulsory = true;
									os = si;
									break;
								}
								if (ci.alternative[si].contains(module) || ci.restricted[si].contains(module)) {
									osoptional = true;
									os= si;
								}
							}
							if (oscompulsory) 
								addWarning(warnings, "Warning: non-standard option "+module+" is compulsory in stage "+os);
							else if (osoptional) 
								addWarning(warnings, "Note: non-standard option "+module+" is option in stage "+os);
							else
								addWarning(warnings, "Warning: non-standard option "+module);
						}
					}
				}

				// semester split
				int autumnCredits = 0, springCredits = 0;
				for (Mark m : snewmarks) {
					if (SEMESTER_AUTUMN.equals(m.semester))
						autumnCredits += m.credit;
					else if (SEMESTER_SPRING.equals(m.semester)) 
						springCredits += m.credit;
					else if (SEMESTER_FULL_YEAR.equals(m.semester)) {
						autumnCredits += m.credit/2;
						springCredits += m.credit/2;
					}	
				}
				if (autumnCredits>MAX_SEMESTER_CREDITS) {
					addWarning(warnings, "Warning: more than "+MAX_SEMESTER_CREDITS+" credits in Autumn ("+autumnCredits+" credits)");
				}
				if (springCredits>MAX_SEMESTER_CREDITS) {
					addWarning(warnings, "Warning: more than "+MAX_SEMESTER_CREDITS+" credits in Spring ("+springCredits+" credits)");
				} else if (springCredits==MAX_SEMESTER_CREDITS) {
					addWarning(warnings, "Note: doing "+springCredits+" credits in Spring");					
				}
				
				
				if (modules.size()==0 && ok)
					addWarning(warnings, "OK for current stage "+stage.name);
				
				printWarnings(pw, warnings);
			}// for sid		
			pw.println("</body></html>");
			pw.close();
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}

	}
	private static void printHeader(PrintWriter pw) {
		// TODO Auto-generated method stub
		pw.println("<html><head><title>check modules output</title></head><body>");
	}

}
