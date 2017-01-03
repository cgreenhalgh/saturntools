/**
 * 
 */
package saturntools.gui;

import java.io.File;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

/** Simple GUI for saturntools.CheckMarksSpreadsheet.
 * 
 * @author cmg
 *
 */
public class CheckModuleDistributions {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		doit(System.err);
	}
	public static void doit(PrintStream error) {
		try {
			ConsoleFrame frame = new ConsoleFrame(error, "Check Module Distributions") {
				@Override
				protected void callMain(String args[]) {
					saturntools.ProcessSaturnExportMarks.main(args);			
				}
			};
			frame.runAsProcessor();
		}
		catch (Exception e) {
			error.println("Error: "+e);
			e.printStackTrace(error);
		}
	}

}
