package ca.phon.shell;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.ui.bindings.RunCommand;
import ca.phon.ui.dnd.FileTransferHandler;

public class ScriptTransferHandler extends FileTransferHandler {

	private static final long serialVersionUID = 1L;
	
	private PhonShellWindow window;
	
	public ScriptTransferHandler(PhonShellWindow window) {
		super();
		this.window = window;
	}
	
	public PhonShellWindow getWindow() {
		return this.window;
	}

	@Override
	public boolean importData(JComponent comp, Transferable t) {
		try {
			final File file = getFile(t);
			if(file != null) {
				final String name = file.getName();
				int dotIdx = name.lastIndexOf('.');
				if(dotIdx > 0) {
					final String ext = name.substring(dotIdx+1);
					
					// check to see if there is a script engine
					// which can process the file
					final ScriptEngineManager manager = new ScriptEngineManager(JissModel.class.getClassLoader());
					ScriptEngine engine = null;
					for(ScriptEngineFactory factory:manager.getEngineFactories()) {
						if(factory.getExtensions().contains(ext)) {
							engine = factory.getScriptEngine();
							break;
						}
					}
					
					// if we have an engine, set the prompt
					if(engine != null) {
						final String cmd = "::exec \"" + file.getAbsolutePath() + "\"";
						SwingUtilities.invokeLater( () -> {
							(new RunCommand(getWindow().getConsole(), cmd)).runCommand();
						});
						return true;
					}
				}
			}
		} catch (IOException e) {
			try {
				final String txt = t.getTransferData(DataFlavor.stringFlavor).toString();
				getWindow().getConsole().replaceSelection(txt);
			} catch (UnsupportedFlavorException | IOException e1) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
}
