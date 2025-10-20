package ca.phon.shell;

import ca.phon.util.RecentFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Recent files for PhonShell scripts.
 */
public class PhonShellRecentFiles extends RecentFiles {

    private final static String recentFilesKey = "PhonShell.recentFiles";
    private final static int maxRecentFiles = 10;

    private static PhonShellRecentFiles instance;

    private final List<RecentFilesListener> listeners;

    public static PhonShellRecentFiles getInstance() {
        if(instance == null) {
            instance = new PhonShellRecentFiles();
        }
        return instance;
    }

    private PhonShellRecentFiles() {
        super(recentFilesKey, maxRecentFiles, true);
        this.listeners = new ArrayList<>();
    }

    @Override
    public void addToHistory(File workspaceFolder) {
        super.addToHistory(workspaceFolder);
    }

    @Override
    public void removeFromHistory(File f) {
        super.removeFromHistory(f);
    }

    public void addRecentFilesListener(RecentFilesListener l) {
    	this.listeners.add(l);
        fireRecentFilesChanged();
    }

    public void removeRecentFilesListener(RecentFilesListener l) {
    	this.listeners.remove(l);
        fireRecentFilesChanged();
    }

    public void fireRecentFilesChanged() {
    	for(RecentFilesListener l:this.listeners) {
    		l.recentFilesChanged();
    	}
    }

    public static interface  RecentFilesListener {
    	public void recentFilesChanged();
    }

}

