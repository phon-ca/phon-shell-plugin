package ca.phon.shell;

import ca.phon.app.session.editor.EditorView;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.project.Project;
import ca.phon.project.ProjectPaths;
import ca.phon.shell.actions.ExecAction;
import ca.phon.shell.actions.PhonShellScriptAction;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.RecentFiles;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.apache.commons.io.FilenameUtils;

import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhonShellEditorView extends EditorView {

    public final static String VIEW_NAME = "PhonShell";

    public final static String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":terminal";

    private PhonShell phonShell;

    private IconStrip iconStrip;
    private FlatButton selectScriptButton;
    private File selectedScriptFile;
    private FlatButton runScriptButton;

    private PhonShellRecentFiles recentFiles;

    public PhonShellEditorView(SessionEditor editor) {
        super(editor);

        this.recentFiles = PhonShellRecentFiles.getInstance();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        phonShell = new PhonShell();

        selectedScriptFile = recentFiles.getFileCount() > 0 ? recentFiles.getFileAt(0) : null;
        recentFiles.addRecentFilesListener( () -> {
            SwingUtilities.invokeLater( () -> {
                if(recentFiles.getFileCount() > 0) {
                    setSelectedScriptFile(recentFiles.getFileAt(0));
                } else {
                    setSelectedScriptFile(null);
                }
            });
        });

        iconStrip = new IconStrip();
        add(iconStrip, BorderLayout.NORTH);
        setupToolbar();

        final JScrollPane sp = new JScrollPane(phonShell);
        add(sp, BorderLayout.CENTER);

        SwingUtilities.invokeLater( () -> {
            phonShell.getModel().getScriptContext()
                    .getBindings(ScriptContext.ENGINE_SCOPE).put("editor", getEditor());
            phonShell.getModel().getScriptContext()
                    .getBindings(ScriptContext.ENGINE_SCOPE).put("project", getEditor().getProject());
            phonShell.getModel().getScriptContext()
                    .getBindings(ScriptContext.ENGINE_SCOPE).put("session", getEditor().getSession());
            phonShell.getModel().getScriptContext()
                    .getBindings(ScriptContext.ENGINE_SCOPE).put("window", CommonModuleFrame.getCurrentFrame());
        });
    }

    private void setupToolbar() {
        final PhonUIAction<Void> selectScriptAction = PhonUIAction.runnable(this::showScriptMenu);
        if(recentFiles.getFileCount() > 0) {
            final File mostRecentScript = recentFiles.getFileAt(0);
            selectScriptAction.putValue(PhonUIAction.NAME, mostRecentScript.getName());
        } else {
            selectScriptAction.putValue(PhonUIAction.NAME, "Select script");
        }
        selectScriptAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select a script to run");
        selectScriptAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        selectScriptAction.putValue(FlatButton.ICON_NAME_PROP, "code");
        selectScriptAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        selectScriptButton = new FlatButton(selectScriptAction);
        iconStrip.add(selectScriptButton, IconStrip.IconStripPosition.LEFT);

        final PhonUIAction<Void> runScriptAction = PhonUIAction.runnable(this::runSelectedScript);
        runScriptAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Run selected script");
        runScriptAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        runScriptAction.putValue(FlatButton.ICON_NAME_PROP, "play_arrow");
        runScriptAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        runScriptButton = new FlatButton(runScriptAction);
        runScriptAction.setEnabled(selectedScriptFile != null);
        iconStrip.add(runScriptButton, IconStrip.IconStripPosition.LEFT);
    }

    private void setSelectedScriptFile(File selectedScriptFile) {
        this.selectedScriptFile = selectedScriptFile;
        if(selectedScriptFile != null) {
            selectScriptButton.setText(selectedScriptFile.getName());
        } else {
            selectScriptButton.setText("Select script");
        }
        runScriptButton.getAction().setEnabled(selectedScriptFile != null);
    }

    private void runSelectedScript() {
        if(selectedScriptFile != null && selectedScriptFile.exists()) {
            final PhonShellScriptAction scriptAction = new PhonShellScriptAction(phonShell, selectedScriptFile.getAbsolutePath(), false);
            scriptAction.actionPerformed(null);
        }
    }

    private void showScriptMenu() {
        final JPopupMenu popupMenu = new JPopupMenu();
        setupScriptMenu(new MenuBuilder(popupMenu));
        popupMenu.show(selectScriptButton, 0, selectScriptButton.getHeight());
    }

    private void setupScriptMenu(MenuBuilder menuBuilder) {
        // add recent files
        if(recentFiles.getFileCount() > 0) {
            for(int i = 0; i < recentFiles.getFileCount(); i++) {
                final File scriptFile = recentFiles.getFileAt(i);
                final PhonShellScriptAction scriptAction = new PhonShellScriptAction(phonShell, scriptFile.getAbsolutePath(), false);
                scriptAction.putValue(PhonShellScriptAction.NAME, scriptFile.getName());
                scriptAction.putValue(PhonShellScriptAction.SHORT_DESCRIPTION, scriptFile.getAbsolutePath());
                menuBuilder.addItem(".", scriptAction);
            }
            menuBuilder.addSeparator(".", "recentFilesSep");
        }

        List<String> exts = new ArrayList<>();
        final ScriptEngineManager manager = new ScriptEngineManager();
        for(ScriptEngineFactory factory:manager.getEngineFactories()) {
            for(String extension:factory.getExtensions()) {
                exts.add(extension);
            }
        }

        // add user script folder scripts
        final JMenu userScriptsMenu = menuBuilder.addMenu(".", "User Scripts");
        setupScriptsMenuRecursive(new MenuBuilder(userScriptsMenu), new File(PhonShellMenuExtPt.USER_SCRIPT_FOLDER), exts);

        // add project script folder scripts
        final Project project = getEditor().getProject();
        if(project != null) {
            final ProjectPaths projectPaths = project.getExtension(ProjectPaths.class);
            if(projectPaths != null) {
                final String projectScriptPath =
                        projectPaths.getLocation() + File.separator + PhonShellMenuExtPt.PROJECT_SCRIPT_FOLDER;
                final JMenu projectScriptsMenu = menuBuilder.addMenu(".", "Project Scripts");
                setupScriptsMenuRecursive(new MenuBuilder(projectScriptsMenu), new File(projectScriptPath), exts);
            }
        }

        menuBuilder.addSeparator(".", "userScriptsSep");

        final ExecAction execAction = new ExecAction(phonShell);
        menuBuilder.addItem(".", execAction);
    }

    private void setupScriptsMenuRecursive(MenuBuilder menuBuilder, File folder, List<String> exts) {
        if(!folder.exists() || !folder.isDirectory()) return;

        for(File file:folder.listFiles()) {
            if(file.isHidden() || file.getName().startsWith(".")
                    || file.getName().startsWith("__") || file.getName().startsWith("~") || file.getName().endsWith("~")) continue;
            if(file.isDirectory()) {
                JMenu subMenu = menuBuilder.addMenu(".", file.getName());
                setupScriptsMenuRecursive(new MenuBuilder(subMenu), file, exts);
            } else {
                final String ext = FilenameUtils.getExtension(file.getName());
                if(exts.contains(ext)) {
                    final PhonShellScriptAction act = new PhonShellScriptAction(phonShell, file.getAbsolutePath(), false);
                    act.putValue(PhonShellScriptAction.NAME, file.getName());
                    act.putValue(PhonShellScriptAction.SHORT_DESCRIPTION, file.getAbsolutePath());
                    JMenuItem scriptItem = new JMenuItem(act);
                    menuBuilder.addItem(".", scriptItem);
                }
            }
        }
    }

    @Override
    public String getName() {
        return VIEW_NAME;
    }

    @Override
    public ImageIcon getIcon() {
        return IconManager.getInstance().getFontIcon(VIEW_ICON, IconSize.MEDIUM, Color.darkGray);
    }

    @Override
    public JMenu getMenu() {
        final JMenu menu = new JMenu(VIEW_NAME);

        final MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupScriptMenu(menuBuilder);

        return menu;
    }

}
