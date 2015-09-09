package ca.phon.shell;

import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.plugin.PhonPlugin;
import ca.phon.plugin.PluginAction;
import ca.phon.project.Project;
import ca.phon.shell.actions.PhonShellScriptAction;
import ca.phon.shell.scriptlibrary.ScriptLibraryManager;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.PrefHelper;

@PhonPlugin(name="PhonShell",
			author="Greg J. Hedlund",
			version="1.0",
			minPhonVersion="1.5.3")
public class PhonShellMenuExtPt implements IPluginExtensionPoint<IPluginMenuFilter>, IPluginExtensionFactory<IPluginMenuFilter>, IPluginMenuFilter {

	// location of PhonShell script for user
	final private static String USER_SCRIPT_FOLDER = 
			PrefHelper.getUserDataFolder() + File.separator + "PhonShell";
	
	// location of PhonShell scripts in project
	final private static String PROJECT_SCRIPT_FOLDER =
			"__res" + File.separator + "PhonShell";
	
	private static ScriptLibraryManager libraryManager = new ScriptLibraryManager();
	
	@Override
	public Class<?> getExtensionType() {
		return IPluginMenuFilter.class;
	}

	@Override
	public IPluginExtensionFactory<IPluginMenuFilter> getFactory() {
		return this;
	}

	@Override
	public IPluginMenuFilter createObject(Object... arg0) {
		return this;
	}

	@Override
	public void filterWindowMenu(final Window owner, JMenuBar menuBar) {
		JMenu pluginsMenu = null;
		for(int i = 0; i < menuBar.getMenuCount(); i++) {
			final JMenu menu = menuBar.getMenu(i);
			if(menu.getText().equals("Plugins")) {
				pluginsMenu = menu;
				break;
			}
		}
		
		if(pluginsMenu != null) {
			final PluginAction pluginAction = new PluginAction("PhonShell");
			pluginAction.putValue(PhonUIAction.NAME, "PhonShell");
			pluginAction.putValue(PluginAction.SHORT_DESCRIPTION, "Phon interactive scripting shell.");
			final JMenuItem pluginItem = new JMenuItem(pluginAction);
			
			if(pluginsMenu.getItemCount() > 0) {
				pluginsMenu.addSeparator();
			}
			pluginsMenu.add(pluginItem);
			
			final JMenu scriptsMenu = new JMenu("PhonShell scripts");
			scriptsMenu.addMenuListener(new MenuListener() {
				
				@Override
				public void menuSelected(MenuEvent e) {
					setupScriptsMenu(owner, scriptsMenu);
				}
				
				@Override
				public void menuDeselected(MenuEvent e) {
					
				}
				
				@Override
				public void menuCanceled(MenuEvent e) {
					
				}
			});
			pluginsMenu.add(scriptsMenu);
			
			final JMenu scriptLibrary = new JMenu("Online Script Library");
			scriptLibrary.addMenuListener(new MenuListener() {
				
				@Override
				public void menuSelected(MenuEvent e) {
					setupScriptLibraryMenu(owner, scriptLibrary);
				}
				
				@Override
				public void menuDeselected(MenuEvent e) {
					
				}
				
				@Override
				public void menuCanceled(MenuEvent e) {
					
				}
			});
			pluginsMenu.add(scriptLibrary);
		}
		
	}
	
	private void setupScriptLibraryMenu(final Window owner, final JMenu scriptLibraryMenu) {
		scriptLibraryMenu.removeAll();
		
		final Runnable onEDTThread = () -> {
			scriptLibraryMenu.removeAll();
			libraryManager.buildMenu(owner, scriptLibraryMenu);
		};
		
		Runnable onBgThread = () -> {
			if(libraryManager.getLibrary() == null) {
				try {
					libraryManager.loadLibrary();
				} catch (IOException e) {
					Toolkit.getDefaultToolkit().beep();
					ToastFactory.makeToast(e.getMessage()).start(scriptLibraryMenu);
				}
			}
		};
		onBgThread.run();
		onEDTThread.run();
	}
	
	private void setupScriptsMenu(Window window, JMenu scriptsMenu) {
		scriptsMenu.removeAll();
		
		List<String> exts = new ArrayList<>();
		final ScriptEngineManager manager = new ScriptEngineManager();
		for(ScriptEngineFactory factory:manager.getEngineFactories()) {
			for(String extension:factory.getExtensions()) {
				exts.add(extension);
			}
		}
		
		JMenuItem headerItem = new JMenuItem("User scripts");
		headerItem.setToolTipText("Scripts in folder: " + USER_SCRIPT_FOLDER);
		headerItem.setEnabled(false);
		scriptsMenu.add(headerItem);
		setupScriptsMenuRecursive(scriptsMenu, new File(USER_SCRIPT_FOLDER), exts);
		
		if(window instanceof CommonModuleFrame) {
			CommonModuleFrame cmf = (CommonModuleFrame)window;
			if(cmf.getExtension(Project.class) != null) {
				Project project = cmf.getExtension(Project.class);
				
				final String projectScriptPath = 
						project.getLocation() + File.separator + PROJECT_SCRIPT_FOLDER;
				headerItem = new JMenuItem("Project scripts");
				headerItem.setToolTipText("Scripts in folder: " + projectScriptPath);
				headerItem.setEnabled(false);
				scriptsMenu.add(headerItem);
				setupScriptsMenuRecursive(scriptsMenu, new File(projectScriptPath), exts);
			}
		}
	}
	
	private void setupScriptsMenuRecursive(JMenu menu, File folder, List<String> exts) {
		if(!folder.exists() || !folder.isDirectory()) return;
		
		for(File file:folder.listFiles()) {
			if(file.isHidden() || file.getName().startsWith(".")
					|| file.getName().startsWith("__") || file.getName().startsWith("~") || file.getName().endsWith("~")) continue;
			if(file.isDirectory()) {
				JMenu subMenu = new JMenu(file.getName());
				setupScriptsMenuRecursive(subMenu, file, exts);
				if(subMenu.getItemCount() > 0)
					menu.add(subMenu);
			} else {
				final String ext = getExtension(file);
				if(exts.contains(ext)) {
					final PhonShellScriptAction act = new PhonShellScriptAction(file.getAbsolutePath());
					act.putValue(PhonShellScriptAction.NAME, file.getName());
					act.putValue(PhonShellScriptAction.SHORT_DESCRIPTION, file.getAbsolutePath());
					act.setUseBuffer(true);
					JMenuItem scriptItem = new JMenuItem(act);
					menu.add(scriptItem);
				}
			}
		}
	}
	
	private String getExtension(File f) {
		int dotIdx = f.getName().indexOf('.');
		if(dotIdx > 0)
			return f.getName().substring(dotIdx+1);
		else
			return "";
	}
	
}
