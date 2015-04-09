package ca.phon.shell.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.SwingUtilities;

import ca.hedlund.jiss.ui.JissConsole;
import ca.hedlund.jiss.ui.bindings.RunCommand;
import ca.phon.shell.PhonShellWindow;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.NativeDialogEvent;
import ca.phon.ui.nativedialogs.NativeDialogListener;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.nativedialogs.OpenDialogProperties;

public class ExecAction extends PhonShellAction {

	private static final long serialVersionUID = -8858944660753929133L;

	private final static String TXT = "Run script...";
	
	private final static String DESC = "Browse for and execute script on disk";
	
	public ExecAction(PhonShellWindow phonShell) {
		super(phonShell);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}
	
	@Override
	public void hookableActionPerformed(ActionEvent arg0) {
		// create a list of availble engines
		final List<String> exts = new ArrayList<String>();
		
		final ScriptEngineManager manager = new ScriptEngineManager(getClass().getClassLoader());
		for(ScriptEngineFactory factory:manager.getEngineFactories()) {
			exts.addAll(factory.getExtensions());
		}
		
		final StringBuilder sb = new StringBuilder();
		for(String ext:exts) {
			if(sb.length() > 0) sb.append(";");
			sb.append(ext);
		}
		final FileFilter scriptFilter = new FileFilter("Scripts", sb.toString());
		
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setRunAsync(true);
		props.setFileFilter(scriptFilter);
		props.setAllowMultipleSelection(false);
		props.setCanChooseFiles(true);
		props.setCanChooseDirectories(false);
		props.setCanCreateDirectories(true);
		props.setListener(new NativeDialogListener() {
			
			@Override
			public void nativeDialogEvent(NativeDialogEvent arg0) {
				final PhonShellWindow window = getPhonShellWindow();
				final JissConsole console = window.getConsole();
				
				final File scriptFile = new File(arg0.getDialogData().toString());
				
                final String cmd = "::exec \"" + scriptFile.getAbsolutePath() + "\" > " + scriptFile.getName();
				SwingUtilities.invokeLater( () -> {
					(new RunCommand(console, cmd)).runCommand();
				});
			}
			
		});
		NativeDialogs.showOpenDialog(props);
	}

}
