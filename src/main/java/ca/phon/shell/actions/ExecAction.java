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
package ca.phon.shell.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.SwingUtilities;

import ca.hedlund.jiss.ui.JissConsole;
import ca.hedlund.jiss.ui.bindings.RunCommand;
import ca.phon.shell.PhonShell;
import ca.phon.shell.PhonShellRecentFiles;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.NativeDialogEvent;
import ca.phon.ui.nativedialogs.NativeDialogListener;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.nativedialogs.OpenDialogProperties;

public class ExecAction extends PhonShellAction {

	private static final long serialVersionUID = -8858944660753929133L;

	private final static String TXT = "Run script...";
	
	private final static String DESC = "Browse for and execute script on disk";
	
	public ExecAction(PhonShell phonShell) {
		super(phonShell);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}
	
	@Override
	public void hookableActionPerformed(ActionEvent arg0) {
		// create a list of availble engines
		final List<String> exts = new ArrayList<String>();
		
		final ScriptEngineManager manager = new ScriptEngineManager(getClass().getClassLoader());
		for(ScriptEngineFactory factory:manager.getEngineFactories()) {
			exts.addAll(factory.getExtensions());
		}
		
		final StringBuilder sb = new StringBuilder();
		for(String ext:exts) {
			if(sb.length() > 0) sb.append(";");
			sb.append(ext);
		}
		final FileFilter scriptFilter = new FileFilter("Scripts", sb.toString());
		
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setRunAsync(true);
		props.setFileFilter(scriptFilter);
		props.setAllowMultipleSelection(false);
		props.setCanChooseFiles(true);
		props.setCanChooseDirectories(false);
		props.setCanCreateDirectories(true);
		props.setListener(new NativeDialogListener() {
			
			@Override
			public void nativeDialogEvent(NativeDialogEvent arg0) {
				final PhonShell phonShell = getPhonShell();
				final JissConsole console = phonShell.getConsole();
				
				final File scriptFile = new File(arg0.getDialogData().toString());
                PhonShellRecentFiles.getInstance().addToHistory(scriptFile);
				
                final String cmd = "::exec \"" + scriptFile.getAbsolutePath() + "\" > " + scriptFile.getName();
				SwingUtilities.invokeLater( () -> {
					(new RunCommand(console, cmd)).runCommand();
				});
			}
			
		});
		NativeDialogs.showOpenDialog(props);
	}

}
