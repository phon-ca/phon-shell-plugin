package ca.phon.shell.preprocessor;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.JissPreprocessor;

public class PluginPreprocessor implements JissPreprocessor {

	@Override
	public boolean preprocessCommand(JissModel jissModel, String orig, StringBuffer cmd) {
		final String c = cmd.toString();
		
		if(c.startsWith("%")) {
			final String parts[] = c.substring(1).split("\\p{Space}");
			
			if(parts.length > 0) {
				final String pluginName = parts[0];
				
				// TODO finish
			}
		}
		
		return false;
	}

}
