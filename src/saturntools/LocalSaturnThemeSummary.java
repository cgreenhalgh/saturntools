/**
 * 
 */
package saturntools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**Try to dump theme-based module summary information from Saturn. 
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnThemeSummary {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnThemeSummary.class);

    static final String EXTERNAL_TEXT = "<BR>&nbsp;<I> This Module has been identified as being particularly suitable for first year students, including those from other Schools.</I>";
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			HashMap<String,ModuleInfo> modules = new HashMap<String,ModuleInfo>();
			TreeSet<String> groupNames = new TreeSet<String>();
			for (int argi=0; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);
					modules.put(mi.modulecode, mi);
					if (mi.groupname!=null)
						groupNames.add(mi.groupname);
					if (mi.targetstudents!=null && mi.targetstudents.contains(EXTERNAL_TEXT))
						mi.targetstudents = mi.targetstudents.replace(EXTERNAL_TEXT, "");
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				if (mi!=null) {
				}
			}
			
			File out = new File("theme_summary.html");			
			System.out.println("writing to "+out);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out)));
			pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "+
					"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
			//<?xml version=\"1.0\"?>");
			pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
			pw.println("<head><title>Saturn Module Theme Summary</title>");
			pw.println("  <meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\">");
			pw.println("</head>");
			pw.println("<body>");
			pw.println("<h1>Module Theme Summary</h1>");
			pw.println("<p>Themes:</p>");
			pw.println("<ul>");
			for (String t : groupNames) {
				pw.println("<li><a href=\"#"+t+"\">"+t+"</a>");
			}
			pw.println("<li><a href=\"#Other\">Other</a>");
			pw.println("</ul>");

			for (String t : groupNames) {
				dumpTheme(pw, t, modules);
			}
			dumpTheme(pw, null, modules);

			pw.println("</body>");
			pw.println("</html>");
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}

	private static void dumpTheme(PrintWriter pw, String t, HashMap<String, ModuleInfo> modules) {
		if (t==null) {
			String t2= "Other";
			pw.println("<h2><a id=\""+t2+"\">"+t2+"</a></h2>");
		}
		else
			pw.println("<h2><a id=\""+t+"\">"+t+"</a></h2>");
		TreeSet<String> modulecodes = new TreeSet<String>();
		modulecodes.addAll(modules.keySet());
		ArrayList<ArrayList<ModuleInfo>> lms = new ArrayList<ArrayList<ModuleInfo>>();
		
		pw.println("<table border=\"1\" cols=\"4\">");
		pw.println("<thead><tr><th>Level</th><th>Module</th><th>Leads to</th><th>MSc only</th></tr></thead>");
		pw.println("<tbody>");
		for (int level=0; level<=4; level++) {
			String lname = Integer.toString(level);
			ArrayList<ModuleInfo> ms = new ArrayList<ModuleInfo>();
			lms.add(ms);
			for (ModuleInfo m : modules.values()) {
				if (lname.equals(m.level)) {
					if (t==m.groupname || (t!=null && t.equals(m.groupname))) {
						ms.add(m);
					}
				}
			}
			Collections.sort(ms, new ModuleInfoComparator());
			if (ms.size()>0) 
			{
				boolean first = true;
				for (ModuleInfo m : ms) {
					if (first) {
						first = false;
						pw.print("<tr><td rowspan=\""+ms.size()+"\">Level&nbsp;"+level+"</td>");
					}
					else
						pw.print("<tr>");
					pw.print("<td><a href=\"#"+m.modulecode+"\">"+m.modulecode+"</a> "+m.moduletitle+"</td>");
					pw.print("<td>");
					for (String pmc : modulecodes) {
						ModuleInfo pm  = modules.get(pmc);
						if (pm.prerequisitecodes.contains(m.modulecode)) {
							if (pm.groupname!=null && pm.groupname.equals(m.groupname))
								pw.print("<a href=\"#"+pm.modulecode+"\"><b>"+pm.modulecode+"</b></a> ");
							else
								pw.print("<a href=\"#"+pm.modulecode+"\">"+pm.modulecode+"</a> ");
						}
					}
					pw.print("</td><td>");
					if (m.targetstudents.startsWith("Only available to students on the MSc") || m.targetstudents.startsWith("Students on the MSc")|| m.targetstudents.startsWith("Available to MSc"))
						pw.print("X");
					pw.println("</td></tr>");
				}
			}
		}
		pw.println("</tbody></table>");

		for (int level=0; level<=4; level++) {
			ArrayList<ModuleInfo> ms = lms.get(level);
			if (ms.size()==0)
				continue;
			pw.println("<h3>Level "+level+"</h3>");
			for (ModuleInfo m : ms) {
				pw.println("<h4><a name=\""+m.modulecode+"\">"+m.modulecode+" "+m.moduletitle+"</a></h4>");
				pw.println("<table border=\"1\" width=\"100%\" cols=\"2\"><tbody>");
				/*pw.println("<tr><td>Available: "+(m.availability ? "Yes" : "No")+"</td></tr>");*/
				pw.println("<tr><td>Target students:</td><td>"+m.targetstudents+"</td></tr>");
				pw.print("<tr><td>Requires:</td><td>");
				for (String p : m.prerequisitecodes) {
					pw.print("<a href=\"#"+p+"\">"+p+"</a> ");
				}
				if (m.prerequisitestext!=null)
					pw.print(m.prerequisitestext);
				if (m.corequisitecodes.size()>0 || (m.corequisitestext!=null && !"None.".equals(m.corequisitestext.trim()))) {
					pw.print("<br/>Co-requisites: ");
					for (String p : m.corequisitecodes) {
						pw.print("<a href=\"#"+p+"\">"+p+"</a> ");
					}
					if (m.corequisitestext!=null)
						pw.print(m.corequisitestext);
				}
				pw.println("</td></tr>");
				pw.print("<tr><td>Leads&nbsp;to:</td><td>");
				for (String pmc : modulecodes) {
					ModuleInfo pm  = modules.get(pmc);
					if (pm.prerequisitecodes.contains(m.modulecode) || pm.corequisitecodes.contains(m.modulecode)) 
						pw.print("<a href=\"#"+pm.modulecode+"\">"+pm.modulecode+"</a> ");
				}
				pw.println("</td></tr>");
				pw.println("<tr><td>Summary:</td><td>"+m.summary+"</td></tr>");
				pw.println("<tr><td>Aims:</td><td>"+m.educationaims+"</td></tr>");
				pw.println("<tr><td>Outcomes:</td><td>"+m.learningoutcomes+"</td></tr>");
				pw.println("</tbody></table>");
			}
		}
	}
	static class ModuleInfoComparator implements java.util.Comparator<ModuleInfo> {

		@Override
		public int compare(ModuleInfo o1, ModuleInfo o2) {
			return o1.modulecode.compareTo(o2.modulecode);
		}
		
	}
}
