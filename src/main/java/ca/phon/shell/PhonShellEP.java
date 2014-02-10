package ca.phon.shell;

import java.util.Map;

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

	public String getName() {
		return EP_NAME;
	}

	public void pluginStart(Map<String, Object> arg0) {
		final PhonShellWindow phonShell = new PhonShellWindow();
		phonShell.pack();
		phonShell.setLocationByPlatform(true);
		phonShell.setVisible(true);
	}

}
