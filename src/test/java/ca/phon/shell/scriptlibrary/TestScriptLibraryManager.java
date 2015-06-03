package ca.phon.shell.scriptlibrary;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestScriptLibraryManager {

	@Test
	public void testDefaultLibraryAvailable() throws IOException {
		final ScriptLibraryManager manager = new ScriptLibraryManager();
		final Library library = manager.loadLibrary();
		
		Assert.assertNotNull(library);
		System.out.println(library.getRoot().getHref());
	}
	
}
