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

import ca.hedlund.jiss.ui.bindings.RunCommand;
import ca.phon.app.hooks.HookableAction;
import ca.phon.shell.PhonShell;
import ca.phon.shell.PhonShellRecentFiles;
import ca.phon.shell.PhonShellWindow;
import ca.phon.ui.CommonModuleFrame;

/**
 * Open a new PhonShell window for the current frame
 * and execute the specified script.
 *
 */
public class PhonShellScriptAction extends HookableAction {

	private final String scriptLocation;
	
	private boolean useBuffer = false;

    private PhonShell phonShellContext;
	
	public PhonShellScriptAction(String scriptLocation) {
		this.scriptLocation = scriptLocation;
	}

    public PhonShellScriptAction(PhonShell phonShellContext, String scriptLocation, boolean useBuffer) {
        this.phonShellContext = phonShellContext;
        this.scriptLocation = scriptLocation;
        this.useBuffer = useBuffer;
    }
	
	public boolean isUseBuffer() {
		return this.useBuffer;
	}
	
	public void setUseBuffer(boolean useBuffer) {
		this.useBuffer = useBuffer;
	}
	
	@Override
	public void hookableActionPerformed(ActionEvent ae) {
        PhonShell shell = phonShellContext;
        if(shell == null) {
            PhonShellWindow window = null;

            CommonModuleFrame cmf = CommonModuleFrame.getCurrentFrame();
            if (cmf instanceof PhonShellWindow) {
                window = (PhonShellWindow) cmf;
            } else {
                window = new PhonShellWindow();
                window.pack();
                window.setVisible(true);
            }
            shell = window.getPhonShell();
        }

        PhonShellRecentFiles.getInstance().addToHistory(new File(scriptLocation));
		
		(new RunCommand(shell.getConsole(), createExecCommand())).runCommand();
	}

	public String createExecCommand() {
		final StringBuilder sb = new StringBuilder();
		sb.append("::exec ").append('\"').append(scriptLocation).append('\"');
		if(useBuffer) {
			sb.append(" > ").append(getValue(NAME));
		}
		return sb.toString();
	}
	
}
