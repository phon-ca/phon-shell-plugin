/*
 * Copyright (C) 2012-2018 Gregory Hedlund
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

import java.util.Map;

import javax.swing.SwingUtilities;

import ca.hedlund.jiss.ui.bindings.RunCommand;
import ca.phon.plugin.IPluginEntryPoint;
import ca.phon.plugin.PhonPlugin;

/**
 * Display the interactive console.
 *
 */
@PhonPlugin(name="PhonShell",
			author="Greg J. Hedlund",
			version="1.0",
			minPhonVersion="1.5.3")
public class PhonShellEP implements IPluginEntryPoint {
	
	final static String EP_NAME="PhonShell";
	
	public final static String SCRIPT_PROP = PhonShellEP.class.getName() + ".script";

	public String getName() {
		return EP_NAME;
	}

	public void pluginStart(final Map<String, Object> arg0) {
		final Runnable onEDT = new Runnable() {
			
			@Override
			public void run() {
				final PhonShellWindow phonShell = new PhonShellWindow();
				phonShell.pack();
				phonShell.setLocationByPlatform(true);
				phonShell.setVisible(true);
				
				if(arg0.get(SCRIPT_PROP) != null) {
					final String cmd = "::exec \"" + arg0.get(SCRIPT_PROP) + "\"";
					(new RunCommand(phonShell.getConsole(), cmd)).runCommand();
				}
			}
		};
		if(SwingUtilities.isEventDispatchThread()) 
			onEDT.run();
		else
			SwingUtilities.invokeLater(onEDT);
	}

}
