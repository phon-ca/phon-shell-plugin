package ca.phon.shell;

import ca.hedlund.jiss.ProcessorEvent;
import ca.hedlund.jiss.ProcessorListener;
import ca.phon.app.session.editor.EditorView;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.project.Project;
import ca.phon.project.ProjectPaths;
import ca.phon.shell.actions.ExecAction;
import ca.phon.shell.actions.PhonShellScriptAction;
import ca.phon.shell.components.VariablesTreeTable;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.NativeDialogEvent;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.nativedialogs.OpenDialogProperties;
import ca.phon.util.PrefHelper;
import ca.phon.util.RecentFiles;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.apache.commons.io.FilenameUtils;
import org.jdesktop.swingx.JXBusyLabel;
import ca.hedlund.jiss.JissModel;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhonShellEditorView extends EditorView {

    public final static String VIEW_NAME = "PhonShell";

    public final static String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":terminal";

    public final static String CUSTOM_FOLDER_PREF = "phonshell.customScriptFolders";

    private PhonShell phonShell;

    private IconStrip iconStrip;
    private FlatButton selectScriptButton;
    private File selectedScriptFile;
    private FlatButton runScriptButton;
    private FlatButton variablesButton;
    private JXBusyLabel busyLabel;

    private VariablesTreeTable variablesTreeTable;
    private JSplitPane splitPane;
    private boolean variablesVisible = false;

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

        // Setup split pane with phonShell console and variables view
        final JScrollPane consoleScrollPane = new JScrollPane(phonShell);
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(consoleScrollPane);
        splitPane.setRightComponent(null); // Variables view initially hidden
        splitPane.setResizeWeight(0.67); // 2/3 for editor, 1/3 for variables
        splitPane.setOneTouchExpandable(true);
        
        add(splitPane, BorderLayout.CENTER);

        // Add property change listener to detect when scripts/commands finish executing
        phonShell.getModel().getProcessor().addProcessorListener(new ProcessorListener() {
            @Override
            public void processingStarted(ProcessorEvent processorEvent) {
                SwingUtilities.invokeLater( () -> {
                    busyLabel.setBusy(true);
                });
            }

            @Override
            public void processingEnded(ProcessorEvent processorEvent) {
                SwingUtilities.invokeLater( () -> {
                    busyLabel.setBusy(false);
                    if (variablesVisible && variablesTreeTable != null) {
                        Bindings bindings = phonShell.getModel().getScriptContext().getBindings(ScriptContext.ENGINE_SCOPE);
                        variablesTreeTable.refresh(bindings);
                    }
                });
            }
        });

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
        final ImageIcon scriptIcon = IconManager.getInstance().getFontIcon(
                IconManager.GoogleMaterialDesignIconsFontName, "code_blocks", IconSize.MEDIUM, Color.darkGray);
        iconStrip.add(new JLabel(scriptIcon), IconStrip.IconStripPosition.LEFT);

        final PhonUIAction<Void> selectScriptAction = PhonUIAction.runnable(this::showScriptMenu);
        if(recentFiles.getFileCount() > 0) {
            final File mostRecentScript = recentFiles.getFileAt(0);
            selectScriptAction.putValue(PhonUIAction.NAME, mostRecentScript.getName());
        } else {
            selectScriptAction.putValue(PhonUIAction.NAME, "Select script");
        }
        selectScriptAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select a script to run");
        selectScriptAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        selectScriptAction.putValue(FlatButton.ICON_NAME_PROP, "arrow_drop_down");
        selectScriptAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        selectScriptButton = new FlatButton(selectScriptAction);
        selectScriptButton.setHorizontalTextPosition(SwingConstants.LEFT);
        iconStrip.add(selectScriptButton, IconStrip.IconStripPosition.LEFT);

        final PhonUIAction<Void> runScriptAction = PhonUIAction.runnable(this::runSelectedScript);
        runScriptAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Run selected script");
        runScriptAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        runScriptAction.putValue(FlatButton.ICON_NAME_PROP, "play_arrow");
        runScriptAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        runScriptButton = new FlatButton(runScriptAction);
        runScriptAction.setEnabled(selectedScriptFile != null);
        iconStrip.add(runScriptButton, IconStrip.IconStripPosition.LEFT);

        // Add busy label to show script execution progress
        busyLabel = new JXBusyLabel();
        busyLabel.setBusy(false);
        iconStrip.add(busyLabel, IconStrip.IconStripPosition.LEFT);

        // Variables button (right side)
        final PhonUIAction<Void> variablesAction = PhonUIAction.runnable(this::toggleVariablesTreeTable);
        variablesAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show/hide context variables");
        variablesAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        variablesAction.putValue(FlatButton.ICON_NAME_PROP, "account_tree");
        variablesAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        variablesButton = new FlatButton(variablesAction);
        iconStrip.add(variablesButton, IconStrip.IconStripPosition.RIGHT);
    }

    private void toggleVariablesTreeTable() {
        variablesVisible = !variablesVisible;
        if (variablesVisible) {
            // Get context variables from JissModel
            Bindings bindings = phonShell.getModel().getScriptContext().getBindings(ScriptContext.ENGINE_SCOPE);
            
            if (variablesTreeTable == null) {
                variablesTreeTable = new VariablesTreeTable(bindings, phonShell);
            } else {
                variablesTreeTable.refresh(bindings);
            }
            
            splitPane.setRightComponent(variablesTreeTable);
            splitPane.setDividerLocation(0.67); // Set initial divider position to 2/3
        } else {
            splitPane.setRightComponent(null);
        }
        revalidate();
        repaint();
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
            executeScript(selectedScriptFile.getAbsolutePath(), false);
        }
    }
    
    /**
     * Execute a script with the given path, optionally inserting the exec command text first
     */
    private void executeScript(String scriptPath, boolean useBuffer) {
        try {
            // Insert the exec command text into the console so user can see what's being executed
            String execText = "::exec \"" + scriptPath + "\"";
            if (useBuffer) {
                execText += " > " + new File(scriptPath).getName();
            }
            execText += "\n";
            
            phonShell.getConsole().getDocument().insertString(
                phonShell.getConsole().getDocument().getLength(),
                execText,
                null
            );
            phonShell.getConsole().setCaretPosition(phonShell.getConsole().getDocument().getLength());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        // Now execute the script
        final PhonShellScriptAction scriptAction = new PhonShellScriptAction(phonShell, scriptPath, useBuffer);
        scriptAction.actionPerformed(null);
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
                final PhonUIAction<Void> scriptAction = PhonUIAction.runnable(() -> executeScript(scriptFile.getAbsolutePath(), false));
                scriptAction.putValue(PhonUIAction.NAME, scriptFile.getName());
                scriptAction.putValue(PhonUIAction.SHORT_DESCRIPTION, scriptFile.getAbsolutePath());
                menuBuilder.addItem(".", scriptAction).setEnabled(scriptFile.exists());
            }
            menuBuilder.addSeparator(".", "recentFilesSep");
            // add item to clear recent files
            final PhonUIAction<Void> clearRecentFilesAct = PhonUIAction.runnable(() -> {
                recentFiles.clearHistory();
                setSelectedScriptFile(null);
            });
            clearRecentFilesAct.putValue(PhonUIAction.NAME, "Clear recent scripts");
            menuBuilder.addItem(".", clearRecentFilesAct);
            menuBuilder.addSeparator(".", "recentFilesEndSep");
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

        // custom script folders
        final String customFoldersStr = PrefHelper.get(CUSTOM_FOLDER_PREF, "");
        if(!customFoldersStr.isEmpty()) {
            final String[] customFolders = customFoldersStr.split(";");
            for(String folderPath:customFolders) {
                final File folder = new File(folderPath);
                if (folder.exists() && folder.isDirectory()) {
                    final JMenu customScriptsMenu = menuBuilder.addMenu(".", "Scripts: " + folder.getName());
                    final MenuBuilder mb = new MenuBuilder(customScriptsMenu);
                    setupScriptsMenuRecursive(mb, folder, exts);
                    
                    if(customScriptsMenu.getItemCount() > 0) {
                        mb.addSeparator(".", "customScriptsSep");
                        final PhonUIAction<String> removeCustomFolderAct = PhonUIAction.consumer(this::removeCustomFolderAct, folderPath);
                        removeCustomFolderAct.putValue(PhonUIAction.NAME, "Remove this folder from custom script folders");
                        mb.addItem(".", removeCustomFolderAct);
                    }
                }
            }
        }
        menuBuilder.addSeparator(".", "userScriptsSep");

        final PhonUIAction<Void> addCustomFolderAct = PhonUIAction.runnable(this::addCustomScriptFolderAct);
        addCustomFolderAct.putValue(PhonUIAction.NAME, "Add custom script folder...");
        menuBuilder.addItem(".", addCustomFolderAct);

        final ExecAction execAction = new ExecAction(phonShell);
        execAction.putValue(PhonUIAction.NAME, "Browse for script...");
        menuBuilder.addItem(".", execAction);
    }

    private void removeCustomFolderAct(String folderPath) {
        List<String> updatedFolders = new ArrayList<>();

        final String customFoldersStr = PrefHelper.get(CUSTOM_FOLDER_PREF, "");
        if(!customFoldersStr.isEmpty()) {
            final String[] customFolders = customFoldersStr.split(";");
            for(String path:customFolders) {
                if(!path.equals(folderPath)) {
                    updatedFolders.add(path);
                }
            }
            final String newPrefStr = String.join(";", updatedFolders);
            PrefHelper.getUserPreferences().put(CUSTOM_FOLDER_PREF, newPrefStr);
        }
    }

    private void addCustomScriptFolderAct() {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setCanChooseFiles(false);
        props.setCanChooseDirectories(true);
        props.setAllowMultipleSelection(false);
        props.setRunAsync(true);
        props.setParentWindow(CommonModuleFrame.getCurrentFrame());
        props.setListener( (evt) -> {
            if(evt.getDialogResult() == NativeDialogEvent.OK_OPTION && evt.getDialogData() != null) {
                final String folderPath = evt.getDialogData().toString();
                List<String> updatedFolders = new ArrayList<>();
                final String customFoldersStr = PrefHelper.get(CUSTOM_FOLDER_PREF, "");

                if(customFoldersStr.isEmpty()) {
                    updatedFolders.add(folderPath);
                } else {
                    final String[] customFolders = customFoldersStr.split(";");
                    for(String path:customFolders) {
                        updatedFolders.add(path);
                    }
                    if(!updatedFolders.contains(folderPath)) {
                        updatedFolders.add(folderPath);
                    }
                }
                final String newPrefStr = String.join(";", updatedFolders);
                PrefHelper.getUserPreferences().put(CUSTOM_FOLDER_PREF, newPrefStr);
            }
        });
        NativeDialogs.showOpenDialog(props);
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
                    final PhonUIAction<Void> act = PhonUIAction.runnable(() -> {
                        PhonShellRecentFiles.getInstance().addToHistory(file);
                        executeScript(file.getAbsolutePath(), false);
                    });
                    act.putValue(PhonUIAction.NAME, file.getName());
                    act.putValue(PhonUIAction.SHORT_DESCRIPTION, file.getAbsolutePath());
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
