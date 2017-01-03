/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

/** Try to dump module information from Saturn. More info than the summary!
 * Variant to read from local files.
 * 
 * @author cmg
 *
 */
public class LocalSaturnModuleDetails {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleDetails.class);
	/** main */
	public static void main(String [] args) {
		try {
			// TODO education aims, learning outcomes, offering school, prerequisites text, corequisites text, summary of content
			System.out.println("modulecode,moduletitle,year,level,semester,credits,available,status,targetstudents,offeringschool,convenor,prerequisites,prerequisitestext,corequisites,corequisitestext,assessments,exampercentage,examdurationhours,examrequirements,cw1type,cw1weight,cw1requirements,cw2type,cw2weight,cw2requirements,cw3type,cw3weight,cw3requirements,cw4type,cw4weight,cw4requirements,summary,educationaims,learningoutcomes,lastupdated,filemodified");
			for (int argi=0; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);

					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				if (mi!=null) {
					// TODO status
					// TODO targetstudents
					// TODO cw[1-4]type, cw[1-4]weight, cw[1-4]requirements
					// TODO offeringschool
					// TODO prerequisitestext, corequisitestext
					// TODO summary,educationaims,learningoutcomes
					System.out.print(mi.modulecode+","+escape(mi.moduletitle)+",'"+mi.year+","+mi.level+","+mi.semester+","+mi.credits+","+(mi.availability ? "Y" : "N")+","+mi.status+","+escape(mi.targetstudents)+","+escape(mi.offeringschool)+","+mi.convenor+",\"");
					for (String prerequisite: mi.prerequisitecodes)
						System.out.print(prerequisite+" ");
					System.out.print("\","+escape(mi.prerequisitestext)+",\"");
					for (String corequisite: mi.corequisitecodes)
						System.out.print(corequisite+" ");
					System.out.print("\","+escape(mi.corequisitestext)+","+mi.assessments+","+mi.exampercentage+","+mi.examdurationhours+","+mi.examrequirements+",");
					for (int i=0; i<4; i++) {
						if (i<mi.assessmentInfos.size()) {
							ModuleInfo.AssessmentInfo ai = mi.assessmentInfos.get(i);
							System.out.print(escape(ai.type)+","+ai.weight+","+escape(ai.requirements)+",");
						}
						else
							System.out.print(",,,");
					}
					System.out.println(escape(mi.summary)+","+escape(mi.educationaims)+","+escape(mi.learningoutcomes)+","+mi.lastupdated+","+mi.filemodified);
				}
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	static String escape(String s) {
		if (s==null)
			return "";
		if (!s.contains(",") && !s.contains("\n") && !s.contains("\f") && !s.contains("\r") && !s.startsWith("\""))
			return s;
		if (!s.contains("\"") && !s.contains("\n") && !s.contains("\f") && !s.contains("\r"))
			return "\""+s+"\"";
		StringBuffer b = new StringBuffer();
		b.append("\"");
		for (int i=0; i<s.length();i++) {
			char c= s.charAt(i);
			switch (c) {
			case '\n':
			case '\f':
			case '\r':
				b.append(" ");// whitespace??!
				break;
			case '"':
				b.append('"');
			//case '\\':
				//b.append('\\');
				// fallthrough
			default:
				if (Character.isISOControl(c) || ((int)c)>127)
					b.append('#');
				else
					b.append(c);
			}			
		}
		b.append("\"");
		return b.toString();
	}
}
