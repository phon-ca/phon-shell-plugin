package ca.phon.shell.preprocessor;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.JissPreprocessor;
import ca.phon.app.log.BufferPanel;
import ca.phon.app.log.BufferWindow;
import ca.phon.app.log.LogBuffer;

public class BufferRedirectPreprocessor implements JissPreprocessor {
	
	private static final Logger LOGGER = Logger
			.getLogger(BufferRedirectPreprocessor.class.getName());
	
	private final static String BUFFER_REGEX = ".* (>{1,2}) ([a-zA-Z][a-zA-Z0-9-.]+)$";

	@Override
	public boolean preprocessCommand(JissModel jissModel, String orig,
			StringBuffer cmd) {
		final Pattern pattern = Pattern.compile(BUFFER_REGEX, Pattern.DOTALL);
		final Matcher matcher = pattern.matcher(orig);
		if(matcher.matches()) {
			final String type = matcher.group(1);
			final String name = matcher.group(2);
			
			final AtomicReference<LogBuffer> bufferRef = new AtomicReference<LogBuffer>();
			final Runnable onEDT = new Runnable() {
				
				@Override
				public void run() {
					final BufferWindow window = BufferWindow.getInstance();
					BufferPanel panel = window.getBuffer(name);
					
					if(type.equals(">")) {
						if(panel == null)
							panel = window.createBuffer(name);
						else
							panel.getLogBuffer().setText("");
					} else {
						if(panel == null) {
							panel = window.createBuffer(name);
						}
					}
					bufferRef.getAndSet((panel != null ? panel.getLogBuffer() : null));
					if(!window.isVisible()) {
						window.setSize(500, 600);
						window.centerWindow();
						window.setVisible(true);
					} else {
						window.requestFocus();
					}
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				onEDT.run();
			else
				try {
					SwingUtilities.invokeAndWait(onEDT);
				} catch (InvocationTargetException e) {
					LOGGER
							.log(Level.SEVERE, e.getLocalizedMessage(), e);
				} catch (InterruptedException e) {
					LOGGER
							.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			
			final LogBuffer logBuffer = bufferRef.get();
			jissModel.getScriptContext().setWriter(new PrintWriter(logBuffer.getStdOutStream()));
			jissModel.getScriptContext().setErrorWriter(new PrintWriter(logBuffer.getStdErrStream()));
			
			cmd.delete(matcher.start(1), orig.length());
		}
		
		return false;
	}

}
