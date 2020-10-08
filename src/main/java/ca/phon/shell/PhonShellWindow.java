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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.ui.JissConsole;
import ca.phon.project.Project;
import ca.phon.session.Session;
import ca.phon.shell.actions.ExecAction;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.util.PrefHelper;


public class PhonShellWindow extends CommonModuleFrame {
	
	public final static String SCRIPT_ENGINE_PROP = PhonShellWindow.class.getName() + ".scriptEngine";
	
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
	
	@Override
	public void setJMenuBar(JMenuBar menuBar) {
		super.setJMenuBar(menuBar);
		
		final JMenu fileMenu = menuBar.getMenu(0);
		fileMenu.add(new JMenuItem(new ExecAction(this)), 1);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		final CommonModuleFrame cmf = CommonModuleFrame.getCurrentFrame();
		
		// copy project and session extensions
		putExtension(Project.class, cmf.getExtension(Project.class));
		putExtension(Session.class, cmf.getExtension(Session.class));
		
		model = new JissModel(PhonShellWindow.class.getClassLoader());
		
		String scriptEngineName = PrefHelper.get(SCRIPT_ENGINE_PROP, null);
		if(scriptEngineName != null) {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName(scriptEngineName);
			model.setScriptEngine(engine);
		}
		model.addPropertyChangeListener(JissModel.SCRIPT_ENGINE_PROP, (e) -> {
			PrefHelper.getUserPreferences().put(SCRIPT_ENGINE_PROP, 
					(model.getScriptEngine() != null ? model.getScriptEngine().getFactory().getEngineName() : null));
		});
		
		if(cmf != null) {
			model.getScriptContext().getBindings(ScriptContext.ENGINE_SCOPE).put("window", cmf);
			setParentFrame(cmf);
		}
		
		final String title = 
				(cmf != null ? String.format("PhonShell (%s)", cmf.getTitle()) : "PhonShell");
		setWindowName(title);
		
		console = new JissConsole(model);
		
		final Font consoleFont = new Font("Liberation Mono", Font.PLAIN, 13);
		console.setFont(consoleFont);
		
		console.setForeground(Color.white);
		console.setBackground(Color.black);
		console.setCaretColor(Color.white);
		
		console.setDragEnabled(true);
		console.setTransferHandler(new ScriptTransferHandler(this));
		
		final JScrollPane sp = new JScrollPane(console);
		
		add(sp, BorderLayout.CENTER);
	}
	
	public JissConsole getConsole() {
		return this.console;
	}
	
	public JissModel getModel() {
		return this.model;
	}

}
