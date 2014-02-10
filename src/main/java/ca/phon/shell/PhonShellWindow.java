package ca.phon.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JScrollPane;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.ui.JissConsole;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.decorations.DialogHeader;


public class PhonShellWindow extends CommonModuleFrame {
	
	/**
	 * shell model
	 */
	private JissModel model;
	
	/**
	 * console
	 */
	private JissConsole console;
	
	public PhonShellWindow() {
		super("PhonShell");
		
		init();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		final DialogHeader header = new DialogHeader("PhonShell", "Interactive scripting shell");
		add(header, BorderLayout.NORTH);
		
		model = new JissModel(PhonShellWindow.class.getClassLoader());
		console = new JissConsole(model);
		
		final Font consoleFont = new Font("Liberation Mono", Font.PLAIN, 13);
		console.setFont(consoleFont);
		
		console.setForeground(Color.white);
		console.setBackground(Color.black);
		console.setCaretColor(Color.white);
		
		final JScrollPane sp = new JScrollPane(console);
		
		add(sp, BorderLayout.CENTER);
	}

}
