/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import saturntools.ModuleInfo.GroupRole;
import saturntools.ModuleInfo.Status;
import saturntools.ModuleInfo.SummaryHeading;
import saturntools.SortModulesByGroup.ModuleComparator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.TreeSet;

/** Modules requires by course(s).
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleTable {
	private static final String MODULECODE = "modulecode";
	private static final Object GROUPROLE = "grouprole";
	private static final String[] PART_NAME = new String[] {
		"Foundation",
		"Qualifying",
		"Part I",
		"Part II",
		"Part III/MSc", "MSc Dissertation"
	};
	private static final String UNKNOWN = "Unknown";
	static boolean SHOW_ROLE = false;
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleTable.class);
    
    static class PrerequisiteFootnote {
    	public String code;
    	public boolean compulsory;
    	public String footnote;
    }
    
	/** main */
	public static void main(String [] args) {
		if (args.length<3) {
			System.err.println("Usage: LocalSaturnModuleTable <module-summary.csv> <groups.csv> <extramoduleinfo.csv> [<course-page.html> ...]");			
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
			{
				// merge extrainfo, in particular grouprole?!
				HashMap<String,HashMap<String,String>> extrainfomap = ReadCsvFile.readCsvFile(new File(args[2]), MODULECODE, false);
				for (String modulecode : moduleinfos.keySet()) {
					ModuleInfo mi = moduleinfos.get(modulecode);
					HashMap<String,String> extrainfo = extrainfomap.get(modulecode);
					if (extrainfo==null)
						continue;
					String extrarole = extrainfo.get(GROUPROLE);
					if (extrarole!=null && extrarole.indexOf("/")>=0)
						extrarole = extrarole.substring(0, extrarole.indexOf("/"));
					if (extrarole!=null && extrarole.length()==0)
						extrarole = null;
					if (extrarole!=null) {
						try {
							mi.grouprole = GroupRole.valueOf(extrarole);
						}
						catch (Exception e) {
							logger.error("Unknown group role '"+extrarole+"' for module "+modulecode);
						}
					}
				}

			}
			for (int argi=3; argi<args.length; argi++) {
				CourseInfo ci = null;
				ci = CourseInfo.processCourseFile(args[argi]);

				for (String modulecode : ci.modules.keySet()) {
					if (!moduleinfos.containsKey(modulecode)) {
						moduleinfos.put(modulecode, ci.modules.get(modulecode));
						logger.info("Using minimal information on module "+modulecode+" from course file");
					}
				}
				
				File file = new File(ci.coursecode+"_table.html");
				logger.info("Output table for "+ci.coursecode+" to "+file);
				
				PrintStream pw = new PrintStream(new FileOutputStream(file));
				pw.println("<html><head><title>Module table</title></head><body>");
				pw.println("<h1>Module table for "+ci.coursecode+" "+ci.title+"</h1>");
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

				if (ci.mscflag)
					logger.info("Msc");
				else
					logger.info("Stages: "+ci.compulsory.length);
				
				// update options from moduleinfos if it was a requirements file?!
				if (modulerequirements.size()>0) {
					modulerequirementstages:
					for (int stagei=0; stagei<ci.compulsory.length && (!ci.mscflag || stagei<1); stagei++)
					{
						int parti = ci.mscflag ? 4+stagei : 1+stagei;
						TreeSet<String> compulsory = new TreeSet<String>();
						TreeSet<String> alternative = new TreeSet<String>();
						TreeSet<String> optional = new TreeSet<String>();
						for (String modulecode : modulerequirements.keySet()) {
							HashMap<String,String> modulerequirement = modulerequirements.get(modulecode);
							String options = modulerequirement.get(ci.coursecode);
							if (options==null)
								break modulerequirementstages;
							if (options.contains("C"+(parti)))
								compulsory.add(modulecode);
							if (options.contains("A"+(parti)))
								alternative.add(modulecode);
							if (options.contains("O"+(parti)))
								optional.add(modulecode);
						}

						ci.compulsory[stagei] = compulsory;
						ci.alternative[stagei] = alternative;
						ci.restricted[stagei] = optional;						
						logger.info("Module info file overrides options for course "+ci.coursecode+" stage "+stagei);
						logger.info("- compulsory="+ci.compulsory[stagei]);
						logger.info("- alternative="+ci.alternative[stagei]);
						logger.info("- restricted="+ci.restricted[stagei]);
					}
				}
				
				HashMap<String,TreeSet<String>> modulesIncludes = new HashMap<String,TreeSet<String>>();
				
				// pre-requisites
				HashMap<String,PrerequisiteFootnote> prerequisites = new HashMap<String,PrerequisiteFootnote>();				
				int footnote = 0;
				Vector<PrerequisiteFootnote> footnotes = new Vector<PrerequisiteFootnote>();
				
				for (int stagei=0; stagei<ci.compulsory.length && (!ci.mscflag || stagei<1); stagei++)
				{
					int parti = ci.mscflag ? 4+stagei : 1+stagei;
					//for (int parti=0; parti<=4; parti++) {
					String partname = PART_NAME[parti];
					TreeSet<String> semesters = new TreeSet<String>();
					// find module(s) at this part...
					// and their semester(s)
					for (String modulecode : moduleinfos.keySet()) {
						ModuleInfo mi = moduleinfos.get(modulecode);
						if (!includeModule(mi, parti, ci, stagei))
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

						int credits[][] = new int[3][3];
						
						for (String group : groups) {
							pw.print("<td>");
							boolean aboveLevel = false, belowLevel = false;
							int iunknown = 0;
							for (String modulecode : modulecodes) {
								ModuleInfo mi = moduleinfos.get(modulecode);
								if (!includeModule(mi, parti, ci, stagei)) {
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
								int level = mi.modulecode.length()>=3 ? (mi.modulecode.charAt(2)-'0') : 0;
								int ilevel = 0;
								if (level < parti) {
									belowLevel = true;
									ilevel = 0;
								}
								else {
									if (belowLevel) {
										belowLevel = false;
										pw.println("<hr>");
									}
									if (level > parti) {
										ilevel = 2;
										if (!aboveLevel) {
											aboveLevel = true;
											pw.println("<hr>");
										}
									}
									else 
										ilevel = 1;
								}
								
								boolean compulsory = (stagei<ci.compulsory.length && ci.compulsory[stagei].contains(mi.modulecode));
								boolean alternative = (stagei<ci.alternative.length && ci.alternative[stagei].contains(mi.modulecode));
								boolean restricted = (stagei<ci.restricted.length && ci.restricted[stagei].contains(mi.modulecode));
								String preformat = "", postformat = "";
								int usefulcredits = mi.status==null || mi.status==Status.available ? mi.credits : 0;
								if (compulsory) {
									preformat = "<b>";
									postformat = "</b>";
									credits[ilevel][0] += usefulcredits;
								}
								else if (alternative) {
									preformat = "<b><i>";
									postformat = "</b></i>";
									credits[ilevel][1] += usefulcredits;
								}
								else
								{
									preformat = "<i>";
									postformat = "</i>";									
									credits[ilevel][2] += usefulcredits;
								}
								ModuleInfo.Status status = mi.status;
								if (status==null)
									status = Status.unknown;
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
								// pre-requisites (footnotes for non-compulsory)
								TreeSet<String> prereqs = new TreeSet<String>();
								prereqs.addAll(mi.prerequisitecodes);
								prereqs.addAll(mi.corequisitecodes);
								for (String pc : prereqs) {
									PrerequisiteFootnote pf = prerequisites.get(pc);
									if (pf==null) {
										pf = new PrerequisiteFootnote();
										pf.code = pc;
										prerequisites.put(pc, pf);
										// compulsory?
										for (int si=0; si<ci.compulsory.length && (!ci.mscflag || si<1); si++)
											if (ci.compulsory[si].contains(pc)) {
												pf.compulsory = true;
												break;
											}
										if (!pf.compulsory) {
											pf.footnote = new Character((char)('a'+footnote)).toString();
											footnote++;								
											footnotes.add(pf);
										}
									}
									if (!pf.compulsory) {
										postformat = postformat+"<sup>"+pf.footnote+"</sup>";
									}
								}
								// module!
								String roletext = SHOW_ROLE && mi.grouprole!=null && mi.grouprole!=GroupRole.unknown ? "<br><span class=\"role\">"+mi.grouprole+"</span>" : "";
								String newline = group.equals(UNKNOWN) && ((++iunknown) % 3)!=0 ? "&nbsp;" : "<br>";
								pw.print(preformat+mi.modulecode+postformat+"&nbsp;("+mi.credits+")"+roletext+newline);
								
								TreeSet<String> minc = modulesIncludes.get(mi.modulecode);
								if (minc==null) {
									minc = new TreeSet<String>();
									modulesIncludes.put(mi.modulecode, minc);
								}
								minc.add(partname+" "+semester+" "+group);
							}
							if (belowLevel) {
								belowLevel = false;
								pw.println("<hr>");
							}
							pw.print("</td>");
						}
						// semester credits
						pw.print("<td>");
						boolean needsRule = false;
						for (int ilevel=0; ilevel<3; ilevel++) {
							if (ilevel>0) 
								pw.println("<hr>");
							if (credits[ilevel][0]>0 || credits[ilevel][1]>0 || credits[ilevel][2]>0) {
								needsRule = true;
								pw.println("<b>"+(credits[ilevel][0]>0 ? ""+credits[ilevel][0] : "_")+"</b>&nbsp;/&nbsp;"+
										"<b><i>"+(credits[ilevel][1]>0 ? ""+credits[ilevel][1] : "_")+"</i></b>&nbsp;/&nbsp;"+
										"<i>"+(credits[ilevel][2]>0 ? ""+credits[ilevel][2] : "_")+"</i>");										
							}
						}
						pw.print("</td>");
						
						pw.println("</tr>");
					}	
				}

				pw.println("</tbody></table>");
				pw.println("<p>Key: <b>Compulsory</b> (bold), <b><i>Alternative</i></b> (bold-italic) - check regulations, <i>Restricted/Optional</i> (italic) - check regulations, <strike>Suspended or dormant</strike> (strike-through), &quot;(?)&quot; - status unknown</p>");
				if (footnotes.size()>0) {
					pw.println("<p>Optional pre-requisistes:</p><ul>");
					for (PrerequisiteFootnote pf : footnotes) 
						pw.println("<li>"+pf.footnote+" "+pf.code);
					pw.println("</ul>");
				}
				pw.println("</body></html>");
				pw.close();
				
				logger.info("Modules included: "+modulesIncludes);
			}

		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	private static boolean includeModule(ModuleInfo mi, int parti, CourseInfo ci, int stagei) {
		try {
			if (ci!=null) {
				boolean compulsory = (stagei<ci.compulsory.length && ci.compulsory[stagei].contains(mi.modulecode));
				boolean alternative = (stagei<ci.alternative.length && ci.alternative[stagei].contains(mi.modulecode));
				boolean restricted = (stagei<ci.restricted.length && ci.restricted[stagei].contains(mi.modulecode));
				if (!compulsory && !alternative && !restricted)
					return false;
				return true;
			}
			int level = Integer.parseInt(mi.level);
			return level==parti;
		}
		catch (Exception e) {
			logger.error("Parsing level '"+mi.level+"' of module "+mi.modulecode);
			return parti==0;
		}
	}
}
