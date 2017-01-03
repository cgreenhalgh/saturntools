/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import saturntools.LocalSaturnStudentChoices.ModuleEntryInfo;
import saturntools.ModuleInfo.GroupRole;
import saturntools.ModuleInfo.Status;
import saturntools.ModuleInfo.SummaryHeading;
import saturntools.SortModulesByGroup.ModuleComparator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.TreeSet;

/** All modules.
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleSummaryTable {
	private static final String MODULECODE = "modulecode";
	private static final Object GROUPROLE = "grouprole";
	private static final String[] PART_NAME = new String[] {
		"Foundation",
		"Qualifying",
		"Part I",
		"Part II",
		"Part III/MSc"
	};
	private static final String UNKNOWN = "Unknown";
	static boolean SHOW_ROLE = false;
	private static class ExtraInfo {
		int requisitefor; // count
		char coursestatus[];// C / A / O
		int numbers[];
		int extranumbers;
	}
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleSummaryTable.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<2) {
			System.err.println("Usage: LocalSaturnModuleSummaryTable <module-summary.csv> <groups.csv> [<module3export.csv>] [<code>:<course-spec.html> ...]");			
			System.exit(-1);
		}
		try {
			HashMap<String,ModuleInfo> moduleinfos = ModuleInfo.readSummaryFile(new File(args[0]));
			HashMap<String,HashMap<String,String>> modulerequirements = ReadCsvFile.readCsvFile(new File(args[0]), SummaryHeading.modulecode.name(), false);
			//TreeSet<String> groups = new TreeSet<String>();
			LinkedList<String> groups = new LinkedList<String>();
			HashMap<String,HashMap<String,String>> groupmap = ReadCsvFile.readCsvFile(new File(args[1]), "groupcode", false, groups);
			{
				// merge newgroup?!
				for (String modulecode : moduleinfos.keySet()) {
					ModuleInfo mi = moduleinfos.get(modulecode);
					mi.newgroup = null;
					//logger.info("Found module "+modulecode+": "+mi.modulecode);
					for (String groupcode : groupmap.keySet()) {
						String modulecodes = groupmap.get(groupcode).get("modulecodes");
						if (modulecodes!=null && modulecodes.contains(modulecode)) {
							mi.newgroup = groupcode;
							break;
						}
					}
					if (mi.newgroup==null) {
						logger.error(args[1]+" missing module "+modulecode);
						if (modulerequirements.containsKey(modulecode)) {
							// try...
							mi.newgroup = modulerequirements.get(modulecode).get("newgroup");
							if (mi.newgroup!=null && !groups.contains(mi.newgroup))
								groups.add(mi.newgroup);
						}
					}					
				}

				//groups.addAll(groupmap.keySet());
				//groups.add(UNKNOWN);
				if (!groups.contains(UNKNOWN))
					groups.add(UNKNOWN);						
			}

			HashMap<String,ExtraInfo> modextramap = new HashMap<String,ExtraInfo>();
			// each module...
			for (String modulecode : moduleinfos.keySet()) {
				ModuleInfo mi = moduleinfos.get(modulecode);				
				ExtraInfo modextra = new ExtraInfo();
				modextramap.put(modulecode, modextra);
			}			
			// each module's pre-reqs
			for (String modulecode : moduleinfos.keySet()) {
				ModuleInfo mi = moduleinfos.get(modulecode);
				for (String req : mi.prerequisitecodes) {
					ExtraInfo modextra = modextramap.get(req);
					if (modextra!=null)
						modextra.requisitefor++;
					else
						logger.error("Could not find pre-req "+req);
				}
				for (String req : mi.corequisitecodes) {
					ExtraInfo modextra = modextramap.get(req);
					if (modextra!=null)
						modextra.requisitefor++;
					else
						logger.error("Could not find pre-req "+req);
				}
			}			
			// each course's reqs
			Vector<String> coursecodes = new Vector<String>();
			HashMap<String,TreeSet<String>> coursenames = new HashMap<String,TreeSet<String>>();
			Vector<CourseInfo> cis = new Vector<CourseInfo>();
			
			for (int argi=2; argi<args.length; argi++) {
				CourseInfo ci = null;
				String code = args[argi];
				String file = args[argi];
				int ix = args[argi].indexOf(':');
				if (ix>=0) {
					code = code.substring(0,ix);
					file = file.substring(ix+1);
				}
				else
					continue;
				if (coursecodes.contains(code)) {
					TreeSet<String> names = coursenames.get(code);
					names.add(file);
					continue;
				}
				coursecodes.add(code);
				TreeSet<String> names = new TreeSet<String>();
				coursenames.put(code,names);
				int ix2 = file.indexOf(".");
				if (ix2<0)
					names.add(file);
				else
					names.add(file.substring(0,ix2));
				ci = CourseInfo.processCourseFile(file);
				cis.add(ci);
			}
			for (String modulecode : moduleinfos.keySet()) {
				ExtraInfo modextra = modextramap.get(modulecode);
				modextra.coursestatus = new char[coursecodes.size()];
				modextra.numbers = new int[coursecodes.size()];
			}			
			for (int courseix=0; courseix<cis.size(); courseix++) {
				CourseInfo ci = cis.get(courseix);
				for (int stagei=0; stagei<ci.compulsory.length && (!ci.mscflag || stagei<1); stagei++)
				{
					for (String modulecode : ci.compulsory[stagei]) {
						ExtraInfo modextra = modextramap.get(modulecode);
						if (modextra!=null)
							modextra.coursestatus[courseix] = 'C';						
					}
					for (String modulecode : ci.alternative[stagei]) {
						ExtraInfo modextra = modextramap.get(modulecode);
						if (modextra!=null && modextra.coursestatus[courseix]!='C')
							modextra.coursestatus[courseix] = 'O';						
					}
					for (String modulecode : ci.restricted[stagei]) {
						ExtraInfo modextra = modextramap.get(modulecode);
						if (modextra!=null && modextra.coursestatus[courseix]!='C')
							modextra.coursestatus[courseix] = 'O';						
					}
				}
			}
			// numbers? module -> course code -> number
			HashMap<String,HashMap<String,Integer>> students = new HashMap<String,HashMap<String,Integer>>();
			if (args.length>2 && !args[2].contains(":")) {
				logger.info("Read student numbers from module 3 export "+args[2]);
				BufferedReader br = new BufferedReader(new FileReader(args[2]));
				String header = br.readLine();
				if (header==null) {
					logger.error("Could not read header line from "+args[2]);
					System.exit(-1);
				}
				// ... Ucas Course, ... Module Code, ...
				Vector<String> headers = CsvUtils.parseCsvLine(header);
				int moduleIndex = headers.indexOf("Module Code");
				if (moduleIndex<0) {
					System.err.println("Could not find module code column");
					System.exit(-1);			
				}
				int courseIndex = headers.indexOf("Ucas Course");
				if (courseIndex<0) {
					System.err.println("Could not find course column");
					System.exit(-1);			
				}
				while(true) {
					String line = br.readLine();
					if (line==null)
						break;
					Vector<String> values = CsvUtils.parseCsvLine(line);
					if (values.size()>courseIndex && values.size()>moduleIndex) {
						String module = values.get(moduleIndex);
						ExtraInfo modextra = modextramap.get(module);
						if (modextra==null)
							continue;
						String course = values.get(courseIndex);
						boolean done = false;
						for (int ci=0; ci<coursecodes.size(); ci++) {
							String cc = coursecodes.get(ci);
							if (coursenames.containsKey(cc) && coursenames.get(cc).contains(course)) {
								modextra.numbers[ci]++;
								done = true;
								break;
							}
						}
						if (!done) {
							modextra.extranumbers++;
							logger.info("Did not find course "+course);
						}
					}
				}
			}
			
			// output table
			PrintStream pw = new PrintStream(System.out);
			pw.println("<html><head><title>Module table</title></head><body>");
			pw.println("<h1>Module table for all modules</h1>");
			pw.println("<table border=\"1\" class=\"generaltable\" width=\"80%\" cellpadding=\"5\" cellspacing=\"1\">");
			pw.println("<thead><tr><th>Semester</th>");
			for(String group : groups) {
				if (group.equals(UNKNOWN))
					pw.println("  <th width='20%'>"+group+"</th>");
				else
					pw.println("  <th>"+group+"</th>");
					
			}
			pw.println("<th>Credits</th></tr></thead>");
			pw.println("<tbody>");

				
			HashMap<String,TreeSet<String>> modulesIncludes = new HashMap<String,TreeSet<String>>();

			for (int parti=0; parti<PART_NAME.length; parti++)
			{
				//for (int parti=0; parti<=4; parti++) {
				String partname = PART_NAME[parti];
				TreeSet<String> semesters = new TreeSet<String>();
				// find module(s) at this part...
				// and their semester(s)
				for (String modulecode : moduleinfos.keySet()) {
					ModuleInfo mi = moduleinfos.get(modulecode);
					if (!includeModule(mi, parti))
						continue;
					if (mi.semester!=null && mi.semester.length()==0)
						mi.semester = null;
					if (mi.semester!=null)
						semesters.add(mi.semester);
					else {
						logger.error("NO semester for module "+modulecode+": "+mi);
						semesters.add(UNKNOWN);
					}
					if (mi.newgroup!=null && !groups.contains(mi.newgroup))
						logger.error("Module "+modulecode+" has unknown group "+mi.newgroup);
				}
				
				TreeSet<String> modulecodes = new TreeSet<String>();
				modulecodes.addAll(moduleinfos.keySet());
				
				// then for each semester with modules...
				for (String semester : semesters) {
					// output them by theme...
					pw.println("<tr><td>"+partname+"<br>"+semester+"</td>");
					int credits = 0;
					
					for (String group : groups) {
						pw.print("<td>");
						for (String modulecode : modulecodes) {
							ModuleInfo mi = moduleinfos.get(modulecode);
							if (!includeModule(mi, parti)) {
								//logger.info("Ignore "+modulecode+" at stage "+stagei+", "+semester);
								continue;
							}
							if (mi.semester==null && !UNKNOWN.equals(semester)) {
								//logger.info("Ignore "+modulecode+" ("+mi.semester+") in semester "+semester);
								continue;
							}
							else if (mi.semester!=null && !mi.semester.equals(semester)) {
								//logger.info("Ignore "+modulecode+" ("+mi.semester+") in semester "+semester);
								continue;
							}
							if ((mi.newgroup==null || mi.newgroup.length()==0) && !UNKNOWN.equals(group)) {
								//logger.info("Ignore "+modulecode+" ("+mi.newgroup+") in group "+group);
								continue;
							}
							else if (mi.newgroup!=null && mi.newgroup.length()>0 && !mi.newgroup.equals(group)) {
								//logger.info("Ignore "+modulecode+" ("+mi.newgroup+") in group "+group);
								continue;
							}
							credits += mi.credits;
							ModuleInfo.Status status = mi.status;
							if (status==null)
								status = Status.unknown;
							String preformat = "", postformat= "";
							switch (status) {
							case suspended:
							case dormant:
								preformat = preformat+"<strike>";
								postformat = "</strike>"+postformat;
								break;
							case available:
								break;
							case unknown:
								//preformat = preformat+"?";
								if (!group.equals(UNKNOWN))
									postformat = "(?)"+postformat;
								break;
							}
							// module!
							String roletext = SHOW_ROLE && mi.grouprole!=null && mi.grouprole!=GroupRole.unknown ? "<br><span class=\"role\">"+mi.grouprole+"</span>" : "";
							String modextratext = "";
							if (args.length>2) {
								ExtraInfo modextra = modextramap.get(mi.modulecode);
								if (modextra!=null) {
									int total=0;
									for (int courseix=0; courseix<coursecodes.size(); courseix++) {
										total += modextra.numbers[courseix];
									}
									modextratext = modextratext+"<br><span style='font-size:80%'>"+total+"+"+modextra.extranumbers+"&nbsp;";
									if (modextra.requisitefor>0)
										modextratext = modextratext+"Req&nbsp;for&nbsp;"+modextra.requisitefor;
									modextratext = modextratext+"</span>";
									modextratext = modextratext+"<br><span style='font-size:80%'>";
									for (int courseix=0; courseix<coursecodes.size(); courseix++) {
										switch(modextra.coursestatus[courseix]) {
										case 'C':
											modextratext = modextratext+"<b>"+coursecodes.get(courseix)+"</b>";
											break;
										case 'O':
											modextratext = modextratext+"<i>"+coursecodes.get(courseix)+"</i>";
											break;
										default:
											if (modextra.numbers[courseix]>0)
												modextratext = modextratext+"<strike>"+coursecodes.get(courseix)+"</strike>";
											else
												modextratext = modextratext+"&nbsp;";
											break;
										}
										if (modextra.numbers[courseix]>0)
											modextratext = modextratext+modextra.numbers[courseix];
										if ((courseix % 6)==5 && courseix+1<coursecodes.size())
											modextratext = modextratext+"<br>";
									}
									modextratext = modextratext+"</span>";
								}
							}
							pw.print(preformat+mi.modulecode+postformat+"&nbsp;("+mi.credits+")"+roletext+modextratext+"<br>");
								
							TreeSet<String> minc = modulesIncludes.get(mi.modulecode);
							if (minc==null) {
								minc = new TreeSet<String>();
								modulesIncludes.put(mi.modulecode, minc);
							}
							minc.add(partname+" "+semester+" "+group);
						}
						pw.print("</td>");
					}
					// semester credits
					pw.print("<td>"+credits+"</td>");
					pw.println("</tr>");
				}	
			}

			pw.println("</tbody></table>");
			pw.println("<p>Key: <strike>Suspended or dormant</strike> (strike-through), &quot;(?)&quot; - status unknown");
			for (String cc : coursecodes) {
				pw.print(cc+": ");
				for (String name : coursenames.get(cc)) {
					pw.print(name+" ");
				}
			}
			pw.println("</p>");
			pw.println("</body></html>");
			pw.close();
				
			logger.info("Modules included: "+modulesIncludes);

		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	private static boolean includeModule(ModuleInfo mi, int parti) {
		try {
			int level = Integer.parseInt(mi.level);
			return level==parti;
		}
		catch (Exception e) {
			logger.error("Parsing level '"+mi.level+"' of module "+mi.modulecode);
			return parti==0;
		}
	}
}
