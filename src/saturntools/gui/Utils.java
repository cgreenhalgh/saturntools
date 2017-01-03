/**
 * 
 */
package saturntools.gui;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/** common GUI utils
 * @author cmg
 *
 */
public class Utils {
	private static File defaultOutputDirectory = null;
	
	public static File chooseOutputDirectory(Component parent) {
		JFileChooser outDirChooser = new JFileChooser();
		if (defaultOutputDirectory!=null)
			outDirChooser.setCurrentDirectory(defaultOutputDirectory);
		outDirChooser.setApproveButtonText("OK");
		outDirChooser.setDialogTitle("Select output directory");
		outDirChooser.setMultiSelectionEnabled(false);
		outDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int resp = outDirChooser.showDialog(parent, "OK");
		if (resp==JFileChooser.APPROVE_OPTION) {
			defaultOutputDirectory = outDirChooser.getSelectedFile();
			return outDirChooser.getSelectedFile();
		}
		return null;
	}
	private static File defaultInputDirectory = null;

	public static File [] chooseInputFiles(Component parent, File initialDir, final String[] extensions) {
		JFileChooser inFileChooser = new JFileChooser();
		if (defaultInputDirectory!=null)
			inFileChooser.setCurrentDirectory(defaultInputDirectory);
		else if (initialDir!=null)
			inFileChooser.setCurrentDirectory(initialDir);
		inFileChooser.setApproveButtonText("OK");
		inFileChooser.setDialogTitle("Select input file(s)");
		inFileChooser.setMultiSelectionEnabled(true);
		inFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (extensions!=null) 
			inFileChooser.setFileFilter(new FileFilter() {
	
				@Override
				public boolean accept(File file) {
					if (file.isDirectory())
						return true;
					String filename = file.getName();
					int ix = filename.lastIndexOf(".");
					if (ix<0)
						return false;
					String ext = filename.substring(ix+1);
					for (int i=0; i<extensions.length; i++) 
						if (extensions[i].equals(ext))
							return true;
					return false;
				}
	
				@Override
				public String getDescription() {
					StringBuffer sb = new StringBuffer();
					for (int i=0; i<extensions.length; i++) {
						if (i>0)
							sb.append(",");
						sb.append(".");
						sb.append(extensions[i]);
					}
					return sb.toString();
				}
				
			});
		int resp = inFileChooser.showDialog(parent, "Process");
		if (resp==JFileChooser.APPROVE_OPTION) {
			defaultInputDirectory = inFileChooser.getCurrentDirectory();
			return inFileChooser.getSelectedFiles();
		}
		return new File[0];
	}
}
