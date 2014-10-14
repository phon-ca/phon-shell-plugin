package ca.phon.shell;

import java.util.Map;

import javax.swing.SwingUtilities;

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
				}
			}
		};
		if(SwingUtilities.isEventDispatchThread()) 
			onEDT.run();
		else
			SwingUtilities.invokeLater(onEDT);
	}

}
