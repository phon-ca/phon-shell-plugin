package ca.phon.shell;

import ca.phon.app.session.editor.EditorView;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

import javax.swing.*;
import java.awt.*;

public class PhonShellEditorView extends EditorView {

    public final static String VIEW_NAME = "PhonShell";

    public final static String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":terminal";

    private PhonShell phonShell;

    public PhonShellEditorView(SessionEditor editor) {
        super(editor);

        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        phonShell = new PhonShell();

        final JScrollPane sp = new JScrollPane(phonShell);
        add(sp, BorderLayout.CENTER);
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
        return new JMenu("PhonShell");
    }

}
