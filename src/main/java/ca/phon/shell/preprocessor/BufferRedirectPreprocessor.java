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
package ca.phon.shell.preprocessor;

import java.io.IOException;
import java.io.OutputStreamWriter;
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
	
	private final static String BUFFER_REGEX = ".*\\p{Space}((>{1,2})\\p{Space}([a-zA-Z][a-zA-Z0-9-.]+))$";

	@Override
	public boolean preprocessCommand(JissModel jissModel, String orig,
			StringBuffer cmd) {
		final Pattern pattern = Pattern.compile(BUFFER_REGEX, Pattern.DOTALL | Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(orig);
		if(matcher.matches()) {
			final String type = matcher.group(2);
			final String name = matcher.group(3);
			
			final AtomicReference<LogBuffer> bufferRef = new AtomicReference<LogBuffer>();
			final Runnable onEDT = new Runnable() {
				
				@Override
				public void run() {
					final BufferWindow window = BufferWindow.getBufferWindow();
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
					window.showWindow();
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
			try {
				jissModel.getScriptContext().setWriter(new OutputStreamWriter(logBuffer.getStdOutStream(), "UTF-8"));
				jissModel.getScriptContext().setErrorWriter(new OutputStreamWriter(logBuffer.getStdErrStream(), "UTF-8"));
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
			
			if(cmd.toString().endsWith(matcher.group(1)))
				cmd.delete(matcher.start(1), cmd.length());
		}
		
		return false;
	}

}
