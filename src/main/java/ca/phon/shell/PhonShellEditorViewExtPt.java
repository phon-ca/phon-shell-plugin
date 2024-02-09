package ca.phon.shell;

import ca.phon.app.session.ViewPosition;
import ca.phon.app.session.editor.EditorView;
import ca.phon.app.session.editor.EditorViewCategory;
import ca.phon.app.session.editor.EditorViewInfo;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;

@EditorViewInfo(name = PhonShellEditorView.VIEW_NAME, icon = PhonShellEditorView.VIEW_ICON, category = EditorViewCategory.PLUGINS, dockPosition = ViewPosition.BOTTOM_RIGHT)
public class PhonShellEditorViewExtPt implements IPluginExtensionPoint<EditorView>, IPluginExtensionFactory<EditorView> {

    @Override
    public EditorView createObject(Object... args) {
        return new PhonShellEditorView((SessionEditor)args[0]);
    }

    @Override
    public Class<?> getExtensionType() {
        return EditorView.class;
    }

    @Override
    public IPluginExtensionFactory<EditorView> getFactory() {
        return this;
    }

}
