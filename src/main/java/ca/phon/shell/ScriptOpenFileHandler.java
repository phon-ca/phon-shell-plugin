package ca.phon.shell;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FilenameUtils;

import ca.phon.app.actions.OpenFileHandler;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.shell.actions.PhonShellScriptAction;
import ca.phon.ui.CommonModuleFrame;

public class ScriptOpenFileHandler implements OpenFileHandler, IPluginExtensionPoint<OpenFileHandler> {

	@Override
	public Set<String> supportedExtensions() {
		ScriptEngineManager manager = new ScriptEngineManager();
		Set<String> exts = new LinkedHashSet<>();
		for(ScriptEngineFactory factory:manager.getEngineFactories()) {
			exts.addAll(factory.getExtensions());
		}
		return exts;
	}

	@Override
	public boolean canOpen(File file) throws IOException {
		String ext = FilenameUtils.getExtension(file.getName());
		return supportedExtensions().contains(ext);
	}

	@Override
	public void openFile(File file, Map<String, Object> map) throws IOException {
		PhonShellScriptAction act = new PhonShellScriptAction(file.getAbsolutePath());
		SwingUtilities.invokeLater(() -> {
			act.actionPerformed(new ActionEvent(CommonModuleFrame.getCurrentFrame(), 1, "open"));
		});
	}

	@Override
	public Class<?> getExtensionType() {
		return OpenFileHandler.class;
	}

	@Override
	public IPluginExtensionFactory<OpenFileHandler> getFactory() {
		return (args) -> this;
	}

}
