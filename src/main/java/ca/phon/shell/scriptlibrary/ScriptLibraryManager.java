package ca.phon.shell.scriptlibrary;

import java.awt.MenuContainer;
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
		try {
			final URL url = new URL(getLibraryLocation());
			
			// parse xml
			final JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
			final Unmarshaller unmarshaller = ctx.createUnmarshaller();
			library = (Library)unmarshaller.unmarshal(url.openStream());
		} catch (MalformedURLException | JAXBException e) {
			throw new IOException(e);
		}
		return getLibrary();
	}
	
	/**
	 * Build a menu of commands executing scripts available in the library.
	 * 
	 * @param menu
	 */
	public void buildMenu(MenuContainer menu) {
		final Library library = getLibrary();
		if(library == null) return;
		buildMenuForFolder(menu, library.getRoot());
	}
	
	public void buildMenuForFolder(MenuContainer menu, FolderType folderType) {
		for(Object obj:folderType.getGroupOrScript()) {
			if(obj instanceof GroupType) {
				final GroupType grp = (GroupType)obj;
				final JMenu grpMenu = new JMenu(grp.getName());
				buildMenuForFolder(grpMenu, grp);
				addToContainer(menu, grpMenu);
			} else {
				final ScriptType st = (ScriptType)obj;
				
				final PhonShellScriptAction act = new PhonShellScriptAction(st.getHref());
				act.putValue(PhonShellScriptAction.NAME, st.getName());
				act.putValue(PhonShellScriptAction.SHORT_DESCRIPTION, st.getDescription());
				final JMenuItem menuItem = new JMenuItem(act);
				addToContainer(menu, menuItem);
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
