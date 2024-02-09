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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.ui.JissConsole;
import ca.phon.project.Project;
import ca.phon.session.Session;
import ca.phon.shell.actions.ExecAction;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.PrefHelper;

/**
 * Window providing a PhonShell context.
 */
public class PhonShellWindow extends CommonModuleFrame {
	
	private PhonShell phonShell;
	
	public PhonShellWindow() {
		super("PhonShell");
		
		init();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	@Override
	public void setJMenuBar(JMenuBar menuBar) {
		super.setJMenuBar(menuBar);
		
		final JMenu fileMenu = menuBar.getMenu(0);
		fileMenu.add(new JMenuItem(new ExecAction(phonShell)), 1);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		phonShell = new PhonShell();

		final JScrollPane sp = new JScrollPane(phonShell);
		add(sp, BorderLayout.CENTER);

		final CommonModuleFrame cmf = CommonModuleFrame.getCurrentFrame();
		setParentFrame(cmf);

		// copy project and session extensions
		putExtension(Project.class, cmf.getExtension(Project.class));
		putExtension(Session.class, cmf.getExtension(Session.class));

		final String title = 
				(cmf != null ? String.format("PhonShell (%s)", cmf.getTitle()) : "PhonShell");
		setWindowName(title);
	}

	public PhonShell getPhonShell() {
		return this.phonShell;
	}
	
}
