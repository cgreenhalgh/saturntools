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
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.StringTokenizer;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;

/** Possible Modules choice options for course(s).
 * Variant to read from local files.
 * GUI/interactive attempt.
 * 
 * @author cmg
 *
 */
public class LocalSaturnCourseOptionGui extends LocalSaturnCourseOptions {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnCourseOptionGui.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<4) {
			System.err.println("Usage: LocalSaturnCourseOptionGui <module-summary.csv> <course-credits.csv> <module-tags.csv> <course-page.html> ...");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulemap = ReadCsvFile.readCsvFile(new File(args[0]), "modulecode", false);
			HashMap<String,HashMap<String,String>> coursecredits = ReadCsvFile.readCsvFile(new File(args[1]), "coursecode", false);
			HashMap<String,HashMap<String,String>> moduletags = ReadCsvFile.readCsvFile(new File(args[2]), "modulecode", false);
			LinkedList<HashMap<String,String>> modules = new LinkedList<HashMap<String,String>>(modulemap.values());
			java.util.Collections.sort(modules, new GroupModuleComparator(false));
			LinkedList<String> modulecodes = new LinkedList<String>();
			for (HashMap<String,String> module : modules) {
				modulecodes.add(module.get(MODULECODE_NAME));
			}
			CourseInfo courseInfos[] = new CourseInfo[args.length-3];
			for (int argi=3; argi<args.length; argi++) {
				CourseInfo ci = null;
				try {
					ci = CourseInfo.processCourseFile(args[argi]);
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing course page "+args[argi], e);
				}
				if (ci!=null) {
					// TODO
					courseInfos[argi-3] = ci;
				}
				makeGui(courseInfos, modulecodes, modulemap, coursecredits, moduletags);
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	/** make gui */
	static void makeGui(CourseInfo courseInfos[], LinkedList<String> modulecodes, HashMap<String,HashMap<String,String>> modulemap, HashMap<String,HashMap<String,String>> coursecredits, HashMap<String,HashMap<String,String>> moduletags) {
		JFrame frame = new JFrame("Course Options");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel options = new JPanel();
		GridBagLayout grid = new GridBagLayout();
		options.setLayout(grid);
		
		options.add(new JLabel("Implement me :-)"));
		// TODO
		//JChooser courseChooser = new JChooser();
		// credits/stage 1 2 3 4 5
		// module ? no 1 2 3 4
		// group ? none all
		
		JScrollPane optionsSp = new JScrollPane(options);
		optionsSp.setPreferredSize(new Dimension(800,800));
		frame.getContentPane().add(optionsSp);
		frame.pack();
		frame.setVisible(true);
	}
}
