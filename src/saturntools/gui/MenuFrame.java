/**
 * 
 */
package saturntools.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import saturntools.PasswordUtils;

/**
 * @author cmg
 *
 */
public class MenuFrame extends JFrame {

	private static final int VERY_WIDE = 1000;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new MenuFrame().setVisible(true);
	}
	
	public MenuFrame() {
		super("Choose Task");
		final PrintStream error = System.err;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel p = new JPanel();
		BoxLayout layout = new BoxLayout(p, BoxLayout.Y_AXIS);
		p.setLayout(layout);
		addOption(p, "Check Marks Spreadsheet", new Runnable() { 
			public void run() {
				PasswordUtils.resetInput();
				CheckMarksSpreadsheet.doit(error);
			}
		});
		addOption(p, "Check Module Distributions", new Runnable() { 
			public void run() {
				PasswordUtils.resetInput();
				CheckModuleDistributions.doit(error);
			}
		});
		addOption(p, "About...", new Runnable() { 
			public void run() {
				JOptionPane.showMessageDialog(MenuFrame.this, "Saturntools, by cmg@cs.nott.ac.uk\n"+
						"Version 1.8 2012-02-02 15:48", "About...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		add(p);
		pack();
	}

	private void addOption(JPanel p, String string, final Runnable runnable) {
		JButton button = new JButton(string);
		button.setMaximumSize(new Dimension(VERY_WIDE, button.getPreferredSize().height));
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		p.add(button);
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//MenuFrame.this.setVisible(false);
				try {
					runnable.run();
				}
				catch (Exception ex) {
					MenuFrame.this.setVisible(true);
					JOptionPane.showMessageDialog(MenuFrame.this, "Sorry: "+e, "Sorry", JOptionPane.ERROR_MESSAGE);
				}
				//MenuFrame.this.setVisible(true);
			}
			
		});
	}

}
