/**
 * 
 */
package saturntools;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

/** Generate a report useful for checking target students and pre-requisites, etc.
 * 
 * @author cmg
 *
 */
public class PrintModuleTargetStudentsReport {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=2) {
			System.err.println("Usage: <module-details.csv> <module-requirements.csv>");
			System.exit(-1);			
		}
		try {
			File mdf = new File(args[0]);
			System.err.println("Reading module-details from "+mdf);
			HashMap<String,HashMap<String,String>> moduledetails = ReadCsvFile.readCsvFile(mdf, "modulecode", false);
			File mrf = new File(args[1]);
			System.err.println("Reading module-requirements from "+mrf);
			HashMap<String,HashMap<String,String>> modulerequirements = ReadCsvFile.readCsvFile(mrf, "modulecode", false);
			PrintStream ps = System.out;
			ps.println("<html><head><title>Target Students Report</title></head><body>");
			ps.println("<h1>Target Students Report</h1>");
			ps.println("<table border=\"1\"><topdy>");
			
			// find courses
			TreeSet<String> coursecodes = new TreeSet<String>();
			for (String modulecode : modulerequirements.keySet()) {
				HashMap<String,String> modulerequirement = modulerequirements.get(modulecode);
				for (String column : modulerequirement.keySet()) {
					if (column.length()==4 && column.startsWith("G"))
						coursecodes.add(column);
					else
						System.err.println("Ignore heading "+column);
				}
			}			
			System.err.println("Found courses "+coursecodes+" in "+mrf);

			TreeSet<String> modulecodes = new TreeSet<String>();
			modulecodes.addAll(moduledetails.keySet());

//			// transitive pre/co-requisites
//			HashMap<String,TreeSet<String>> allprerequisites = new HashMap<String,TreeSet<String>>();
//			HashMap<String,TreeSet<String>> allcorequisites = new HashMap<String,TreeSet<String>>();
//			HashMap<String,TreeSet<String>> allprerequisitestext = new HashMap<String,TreeSet<String>>();
//			HashMap<String,TreeSet<String>> allcorequisitestext = new HashMap<String,TreeSet<String>>();
//			for (String modulecode : modulecodes) {
//				HashMap<String,String> moduledetail = moduledetails.get(modulecode);
//				String corequisites = moduledetail.get("corequisites");
//				String corequisitestext = moduledetail.get("corequisitestext");
//				String prerequisites = moduledetail.get("prerequisites");
//				String prerequisitestext = moduledetail.get("prerequisitestext");
//				
//				TreeSet<String> prereq = new TreeSet<String>();
//				String prs[] = prerequisites.split(" ");
//				for (String pr : prs) 
//					prereq.add(pr);
//				TreeSet<String> coreq = new TreeSet<String>(); 
//				String crs[] = corequisites.split(" ");
//				for (String cr : crs) 
//					coreq.add(cr);
//				
//				TreeSet<String> prereqtext = new TreeSet<String>(); 
//				prereqtext.add(prerequisitestext);
//				TreeSet<String> coreqtext = new TreeSet<String>(); 
//				coreqtext.add(corequisitestext);
//				
//				allprerequisites.put(modulecode, prereq);
//				allcorequisites.put(modulecode, coreq);
//				allprerequisitestext.put(modulecode, prereqtext);
//				allcorequisitestext.put(modulecode, coreqtext);
//				
//				// expand all existing...
//				for (String mc2 : modulecodes) {
//					
//				}
//			}
//			
			for (String modulecode : modulecodes) {
				HashMap<String,String> moduledetail = moduledetails.get(modulecode);
				HashMap<String,String> modulerequirement = modulerequirements.get(modulecode);
				
				String moduletitle = moduledetail.get("moduletitle");
				String credits = moduledetail.get("credits");
				String semester = moduledetail.get("semester");
				String status = moduledetail.get("status");
				String targetstudents = moduledetail.get("targetstudents");
				String corequisites = moduledetail.get("corequisites");
				String corequisitestext = moduledetail.get("corequisitestext");
				String prerequisites = moduledetail.get("prerequisites");
				String prerequisitestext = moduledetail.get("prerequisitestext");
				
				if (ModuleInfo.Status.dormant.toString().equals(status)){
					ps.println("<tr><td colspan=\"3\"><h3>"+modulecode+" "+moduletitle+" Dormant</h3></td></tr>");					
					continue;
				}
				
				ps.println("<tr><table border=\"1\" width=\"100%\" cols=\"3\"><tbody>");
				ps.println("<tr><td colspan=\"3\"><h3>"+modulecode+" "+moduletitle+"</h3></td></tr>");
				ps.println("<tr><td width=\"30%\">Credits: "+credits+"</td><td width=\"30%\">Semester: "+semester+"</td><td width=\"30%\">Status: "+status+"</td></tr>");
				ps.println("<tr><td colspan=\"3\">Prerequisites: "+prerequisites+"<br>"+prerequisitestext+"</td></tr>");
				ps.println("<tr><td colspan=\"3\">Corequisites: "+corequisites+"<br>"+corequisitestext+"</td></tr>");
				StringBuffer courses = new StringBuffer();
				if (modulerequirement==null)
					courses.append("Not found in "+mrf);
				else {
					for (String coursecode : coursecodes) {
						String req = modulerequirement.get(coursecode);
						if (req==null)
							continue;
						boolean compulsory=false, alternative=false, optional=false;
						if (req.contains("C"))
							compulsory = true;
						if (req.contains("A"))
							alternative = true;
						if (req.contains("O"))
							optional = true;
						String tag = null;
						if (compulsory) 
							tag = "b";
						else if (alternative)
							;//tag = "i";
						else if (optional)
							;//tag = "i";
						else 
							continue;
						courses.append(" ");
						if (tag!=null)
							courses.append("<"+tag+">");
						courses.append(coursecode);
						if (tag!=null)
							courses.append("</"+tag+">");
					}
					
				}
				// transitive
				TreeSet<String> done = new TreeSet<String>();
				done.add(modulecode);
				TreeSet<String> todo = new TreeSet<String>();
				todo.addAll(split(prerequisites));
				todo.addAll(split(corequisites));
				while (todo.size()!=0) {
					String mc = todo.first();
					todo.remove(mc);
					if (done.contains(mc))
						continue;
					done.add(mc);
					HashMap<String,String> moduledetail2 = moduledetails.get(mc);
					if (moduledetail2==null) {
						ps.println("<tr><td colspan=\"3\">"+mc+" not found</td</tr>");
					}
					else {
						String semester2 = moduledetail2.get("semester");
						String status2 = moduledetail2.get("status");
						String moduleextra = " ("+semester2+", "+status2+")";
						String corequisites2 = moduledetail2.get("corequisites");
						String corequisitestext2 = moduledetail2.get("corequisitestext");
						String prerequisites2 = moduledetail2.get("prerequisites");
						String prerequisitestext2 = moduledetail2.get("prerequisitestext");
						if ((prerequisites2!=null && prerequisites2.length()>0) || (prerequisitestext2!=null && prerequisitestext2.length()>0)) {
							ps.println("<tr><td colspan=\"3\">"+mc+ moduleextra+" prerequisites: "+prerequisites2+"<br>"+prerequisitestext2+"</td></tr>");
							moduleextra = "";
						}
						if ((corequisites2!=null && corequisites2.length()>0) || (corequisitestext2!=null && corequisitestext2.length()>0)) {
							ps.println("<tr><td colspan=\"3\">"+mc+ moduleextra+" corequisites: "+corequisites2+"<br>"+corequisitestext2+"</td></tr>");
							moduleextra = "";
						}
						if (moduleextra.length()>0) 
							ps.println("<tr><td colspan=\"3\">"+mc+ moduleextra+": no pre- or co-requisites</td></tr>");
						todo.addAll(split(prerequisites2));
						todo.addAll(split(corequisites2));
					}
				}
				
				ps.println("<tr><td colspan=\"3\">Courses: "+courses+"</td></tr>");
				ps.println("<tr><td colspan=\"3\"><b>Target Students:</b> "+targetstudents+"</td></tr>");
				ps.println("<tr><td colspan=\"3\">Notes: </td></tr>");
				ps.println("</tbody></table></tr>");
				ps.println("<tr border=\"0\"><td>&nbsp;</td></tr>");
			}
			
			ps.println("</tbody></table></body></html>");
			ps.close();
		}
		catch(Exception e) {		
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}

	}

	private static Collection<? extends String> split(String prerequisites) {
		TreeSet<String> prereq = new TreeSet<String>();
		if (prerequisites==null)
			return prereq;
		String prs[] = prerequisites.split(" ");
		for (String pr : prs) 
			if (pr.length()>0)
				prereq.add(pr);
		return prereq;
	}

}
