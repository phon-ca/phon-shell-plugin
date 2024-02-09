/*
 * Copyright (C) 2012-2018 Gregory Hedlund & Yvan Rose
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.shell;

import java.awt.datatransfer.Clipboard;
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
	
	private PhonShell phonShell;
	
	public ScriptTransferHandler(PhonShell phonShell) {
		super();
		this.phonShell = phonShell;
	}
	
	public PhonShell getPhonShell() {
		return this.phonShell;
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
							(new RunCommand(getPhonShell().getConsole(), cmd)).runCommand();
						});
						return true;
					}
				}
			}
		} catch (IOException e) {
			try {
				final String txt = t.getTransferData(DataFlavor.stringFlavor).toString();
				getPhonShell().getConsole().replaceSelection(txt);
			} catch (UnsupportedFlavorException | IOException e1) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	@Override
	public void exportToClipboard(JComponent comp, Clipboard clip, int action)
			throws IllegalStateException {
		super.exportToClipboard(comp, clip, action);
	}
	
}
