package ca.phon.shell;

import java.awt.Window;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.plugin.PhonPlugin;
import ca.phon.plugin.PluginAction;
import ca.phon.ui.action.PhonUIAction;

@PhonPlugin(name="PhonShell",
			author="Greg J. Hedlund",
			version="1.0",
			minPhonVersion="1.5.3")
public class PhonShellMenuExtPt implements IPluginExtensionPoint<IPluginMenuFilter>, IPluginExtensionFactory<IPluginMenuFilter>, IPluginMenuFilter {

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
	public void filterWindowMenu(Window owner, JMenuBar menuBar) {
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
		}
	}
}
