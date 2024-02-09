package ca.phon.shell;

import ca.hedlund.jiss.JissModel;
import ca.hedlund.jiss.ui.JissConsole;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.PrefHelper;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import java.awt.*;

/**
 * PhonShell is a scripting shell for Phon.
 */
public class PhonShell extends JComponent {

    public final static String SCRIPT_ENGINE_PROP = PhonShellWindow.class.getName() + ".scriptEngine";

    /**
     * shell model
     */
    private JissModel model;

    /**
     * console
     */
    private JissConsole console;

    public PhonShell() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        final CommonModuleFrame cmf = CommonModuleFrame.getCurrentFrame();

//        // copy project and session extensions
//        putExtension(Project.class, cmf.getExtension(Project.class));
//        putExtension(Session.class, cmf.getExtension(Session.class));

        model = new JissModel(PhonShell.class.getClassLoader());

        String scriptEngineName = PrefHelper.get(SCRIPT_ENGINE_PROP, null);
        if(scriptEngineName != null) {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName(scriptEngineName);
            if(engine != null)
                model.setScriptEngine(engine);
        }
        model.addPropertyChangeListener(JissModel.SCRIPT_ENGINE_PROP, (e) -> {
            PrefHelper.getUserPreferences().put(SCRIPT_ENGINE_PROP,
                    (model.getScriptEngine() != null ? model.getScriptEngine().getFactory().getNames().get(0) : null));
        });

        if(cmf != null) {
            model.getScriptContext().getBindings(ScriptContext.ENGINE_SCOPE).put("window", cmf);
        }

        console = new JissConsole(model);

        final Font consoleFont = FontPreferences.getMonospaceFont();
        console.setFont(consoleFont);

        console.setForeground(Color.white);
        console.setBackground(Color.black);
        console.setCaretColor(Color.white);

        console.setDragEnabled(true);
        console.setTransferHandler(new ScriptTransferHandler(this));

        add(console, BorderLayout.CENTER);
    }

    public JissConsole getConsole() {
        return this.console;
    }

    public JissModel getModel() {
        return this.model;
    }

}
