/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

/** Reads a CSV file derived from the Workload questionaire and outputs a line per option to include in survey monkey.
 * Example input:
<pre>
Student module options 2011/12,,,,,,,,,,,,,,
Theme: Foundations of Computer Science,,,,,,,,,,,,,,
G50ALG,Introduction to Algorithms,n,,,,,,,,,,,,
G51APS,Algorithmic Problem Solving,n,,,,,,,,,,,,
G52IFR,Introduction to Formal Reasoning,y,,,,,,,,,,,,
G52MAL,Machines and their Languages,y,,,,,,,,,,,,
G54INF,Infinite Data and Programming,y,Non-default-URL,,,,,,,,,,,
</pre>
 * 
 * @author cmg
 *
 */
public class ProcessStudentOptionsList {

	private static final String URL_PREFIX = "http://modulecatalogue.nottingham.ac.uk/Nottingham/asp/FindModule.asp?mnem=";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length!=1) {
			System.err.println("Usage: <optionlist.csv>");
			System.err.println("Example csv:");
			System.err.println("Student module options 2011/12,,,,,,,,,,,,,,\nTheme: Foundations of Computer Science,,,,,,,,,,,,,,\"G50ALG,Introduction to Algorithms,n,,,,,,,,,,,,\nG51APS,Algorithmic Problem Solving,n,,,,,,,,,,,,\nG52IFR,Introduction to Formal Reasoning,y,,,,,,,,,,,,\nG52MAL,Machines and their Languages,y,,,,,,,,,,,,\nG54INF,Infinite Data and Programming,y,Non-default-URL,,,,,,,,,,,");
			System.exit(-1);
		}
		try {
			String heading = "";
			
			for (int level= -1; level<=5; level++) {
				System.out.println();
				if (level<0)
					System.out.println("All options");
				else 
					System.out.println("Level "+level+" options");
				System.out.println();
				
				BufferedReader br = new BufferedReader(new FileReader(args[0]));

				while(true) {
					String line = br.readLine();
					if (line==null)
						break;
					Vector<String> values = CsvUtils.parseCsvLine(line);
					if (values.size()==0)
						continue;
					if (values.size()<3 || values.get(1).length()==0) {
						// subhead
						heading = "<hr/><h3>"+values.get(0)+"</h3><hr/>";
					}
					else {
						String code = values.get(0);
						int modulelevel = code.charAt(2)-'0';
						if (modulelevel<0 || modulelevel>5) 
							System.err.println("Warning: invalid level "+modulelevel+" for "+code);
						if (level>=0 && modulelevel!=level)
							continue;
						String name = values.get(1);
						boolean include = !(values.get(2).length()>0 && values.get(2).toLowerCase().charAt(0)=='n');
						if (!include) {
							System.err.println("Skip "+code);
							continue;
						}
						String url = values.size()>3 && values.get(3).length()>0 ? values.get(3) : null;
						if (url==null)
							url = URL_PREFIX+code;
						System.out.println(heading+"<a href=\""+url+"\" target=\"modulespec\">"+code+"</a> "+name+"");
						heading = "";
					}
				}
			}
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}

}
