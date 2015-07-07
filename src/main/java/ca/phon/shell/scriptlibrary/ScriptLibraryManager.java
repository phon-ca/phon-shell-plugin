package ca.phon.shell.scriptlibrary;

import java.awt.MenuContainer;
import java.awt.Window;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ca.phon.app.VersionInfo;
import ca.phon.shell.actions.PhonShellScriptAction;
import ca.phon.util.PrefHelper;

/**
 * Implementation of a library of PhonShell scripts.
 */
public class ScriptLibraryManager {
	
	public final static String SCRIPT_LIBRARY_LOCATION = 
			ScriptLibraryManager.class.getName() + ".libraryLocation";
	public final static String DEFAULT_SCRIPT_LIBRARY_LOCATION = 
			"https://phon.ca/phonshell/scripts/src/main/phonshell/library.xml";
	private String libraryLocation = 
			PrefHelper.get(SCRIPT_LIBRARY_LOCATION, DEFAULT_SCRIPT_LIBRARY_LOCATION);
	
	private Library library;
	
	public ScriptLibraryManager() {
		super();
	}
	
	public String getLibraryLocation() {
		return this.libraryLocation;
	}
	
	public void setLibraryLocation(String libraryLocation) {
		this.libraryLocation = libraryLocation;
	}
	
	public Library getLibrary() {
		return library;
	}
	
	public Library loadLibrary() throws IOException {
		if(library == null) {
			try {
				final URL url = new URL(getLibraryLocation());
				
				// parse xml
				final JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
				final Unmarshaller unmarshaller = ctx.createUnmarshaller();
				library = (Library)unmarshaller.unmarshal(url.openStream());
			} catch (MalformedURLException | JAXBException e) {
				throw new IOException(e);
			}
		}
		return getLibrary();
	}
	
	/**
	 * Build a menu of commands executing scripts available in the library.
	 * 
	 * @param menu
	 */
	public void buildMenu(Window owner, MenuContainer menu) {
		final Library library = getLibrary();
		if(library == null) return;
		buildMenuForFolder(owner, menu, library.getRoot().getHref(), library.getRoot());
	}
	
	public void buildMenuForFolder(Window owner, MenuContainer menu, String rootHref, FolderType folderType) {
		for(Object obj:folderType.getGroupOrScript()) {
			if(obj instanceof GroupType) {
				final GroupType grp = (GroupType)obj;
				final JMenu grpMenu = new JMenu(grp.getName());
				buildMenuForFolder(owner, grpMenu, rootHref, grp);
				addToContainer(menu, grpMenu);
			} else {
				final ScriptType st = (ScriptType)obj;
				final String windowClass = owner.getClass().getName();
				
				boolean includeScript = true;
				if(st.getIncludeWindow().size() > 0) {
					includeScript = false;
					for(String s:st.getIncludeWindow()) {
						includeScript |= windowClass.matches(s);
					}
				}
				for(String s:st.getExcludeWindow()) {
					if(windowClass.matches(s)) {
						includeScript = false;
						break;
					}
				}
				
				// check version requirements
				if(st.getMinVersion() != null && st.getMinVersion().length() > 0) {
					includeScript &= VersionInfo.getInstance().getLongVersion().compareTo(st.getMinVersion()) >= 0;
				}
				if(st.getMaxVersion() != null && st.getMaxVersion().length() > 0) {
					includeScript &= VersionInfo.getInstance().getLongVersion().compareTo(st.getMaxVersion()) <= 0;
				}
				
				if(includeScript) {
					final PhonShellScriptAction act = new PhonShellScriptAction(rootHref + st.getHref());
					act.putValue(PhonShellScriptAction.NAME, st.getName());
					act.putValue(PhonShellScriptAction.SHORT_DESCRIPTION, st.getDescription());
					act.setUseBuffer(st.isUseBuffer());
					final JMenuItem menuItem = new JMenuItem(act);
					addToContainer(menu, menuItem);
				}
			}
		}
	}
	
	private void addToContainer(MenuContainer menu, MenuElement ele) {
		if(menu instanceof JPopupMenu) {
			((JPopupMenu)menu).add(ele.getComponent());
		} else if(menu instanceof JMenu) {
			((JMenu)menu).add(ele.getComponent());
		}
	}
	
}
