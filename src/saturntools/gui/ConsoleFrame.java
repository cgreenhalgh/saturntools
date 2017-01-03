/**
 * 
 */
package saturntools.gui;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.swing.JFrame;

import saturntools.FileUtils;

/**
 * @author cmg
 *
 */
public class ConsoleFrame extends JFrame {
	private ConsolePanel consolePanel;
	private static final int PREFERRED_WIDTH = 1000;
	private static final int PREFERRED_HEIGHT = 700;
	private PrintStream error;
	/**
	 * @param arg0
	 * @throws HeadlessException
	 * @throws IOException 
	 */
	public ConsoleFrame(PrintStream error, String arg0) throws HeadlessException, IOException {
		super(arg0);
		this.error = error;
		consolePanel = new ConsolePanel(error);
		add(consolePanel);
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
		pack();
	}
	public InputStream getInput() {
		return consolePanel.getInput();
	}
	public PrintStream getOutput() {
		return consolePanel.getOutput();
	}
	
	protected void callMain(String [] args) {		
	}
	
	public void runAsProcessor() {
		Thread t = new Thread() { 
			public void run() {
				runAsProcessorInternal();
			}
		};
		t.start();
	}
	protected void runAsProcessorInternal() {
		try {
			//ConsoleFrame frame = new ConsoleFrame(error, "Check Module Distributions");
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setVisible(true);

			File dir = Utils.chooseOutputDirectory(this);
			if (dir==null)
				return;
			FileUtils.setOutputDirectory(dir);
			
			File files[] = Utils.chooseInputFiles(this, dir, new String[] { "xls", "txt", "csv" });
			if (files==null || files.length==0)
				return;

			System.setOut(this.getOutput());
			System.setErr(this.getOutput());
			System.setIn(this.getInput());
			
			final String args[] = new String[files.length];
			for (int i=0; i<files.length; i++)
				args[i] = files[i].getAbsolutePath();
			
			callMain(args);
		
			error.println("Done");
			System.err.println("Done");
			this.getOutput().close();
		}
		catch (Exception e) {
			error.println("Error: "+e);
			e.printStackTrace(error);
		}

	}


}
