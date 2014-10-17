package ca.phon.shell.actions;

import java.lang.ref.WeakReference;

import ca.phon.app.hooks.HookableAction;
import ca.phon.shell.PhonShellWindow;

public abstract class PhonShellAction extends HookableAction {

	private static final long serialVersionUID = 4985986930914930136L;

	private final WeakReference<PhonShellWindow> phonShellRef;
	
	public PhonShellAction(PhonShellWindow phonShell) {
		super();
		this.phonShellRef = new WeakReference<PhonShellWindow>(phonShell);
	}
	
	public PhonShellWindow getPhonShellWindow() {
		return phonShellRef.get();
	}
	
}
