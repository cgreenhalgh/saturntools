/**
 * 
 */
package saturntools.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/**
 * @author cmg
 *
 */
public class ConsolePanel extends JPanel {
	private JTextField inputField;
	private JTextArea outputArea;
	private PipedInputStream inputReader;
	private Writer inputWriter;
	private Reader outputReader;
	private PrintStream outputWriter;
	private PrintStream error;
	/** cons 
	 * @throws IOException */
	public ConsolePanel(PrintStream err) throws IOException {
		this.error = err;
		setLayout(new BorderLayout());
		add(new JLabel("Program output:"), BorderLayout.NORTH);
		outputArea = new JTextArea();
		Font font = new Font("monospaced", Font.PLAIN, 12);
		outputArea.setFont(font);
		outputArea.setEditable(false);
		outputArea.setFocusable(false);
		JPanel ip = new JPanel();
		ip.setLayout(new BorderLayout());
		inputField = new JTextField();
		inputField.setFont(font);
		ip.add(new JLabel("Type here: "), BorderLayout.WEST);
		ip.add(inputField, BorderLayout.CENTER);
		add(ip, BorderLayout.SOUTH);
		add(new JScrollPane(outputArea), BorderLayout.CENTER);
		
		inputReader = new PipedInputStream();
		inputWriter = new OutputStreamWriter(new PipedOutputStream(inputReader));
		
		PipedInputStream outputInputStream = new PipedInputStream();
		outputReader = new InputStreamReader(outputInputStream);
		outputWriter = new PrintStream(new PipedOutputStream(outputInputStream));
		
		inputField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				String input = inputField.getText();
				inputField.setText("");
				try {
					outputArea.append(input+"\n");

					inputWriter.write(input+"\n");
					inputWriter.flush();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(ConsolePanel.this, "Sorry, something isn't working (handling your input)", "Error", JOptionPane.ERROR_MESSAGE);
					error.println("Error handling console input:"+e);
					e.printStackTrace(error);
					try {
						inputWriter.close();
					}
					catch (Exception e2) {/*ignore*/}
				}
			}
			
		});
		
		// copy output to screen
		new Thread() {
			public void run() {
				try {
					char buf[] = new char[1000];
					while (true) {
						int cnt = outputReader.read(buf);
						if (cnt<0)
							break;
						final String text = new String(buf, 0, cnt);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								outputArea.append(text);
								try {
									outputArea.scrollRectToVisible(outputArea.modelToView(outputArea.getText().length()-1));
								} catch (BadLocationException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}								
							}
						});
					}
				}
				catch (Exception e) {
					/*ignore*/
				}
				error.println("Output thread exiting");
			}
		}.start();
	}
	private Component JScrollPane(JTextArea outputArea2) {
		// TODO Auto-generated method stub
		return null;
	}
	public PrintStream getOutput() {
		return outputWriter;
	}
	public InputStream getInput() {
		return inputReader;
	}
	public void close() {
		try {
			outputWriter.close();
			inputReader.close();
		}
		catch (Exception e) {/*ignore*/}
	}
}
