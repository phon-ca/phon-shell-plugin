package ca.phon.shell.components;

import ca.phon.shell.PhonShell;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.script.Bindings;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

/**
 * JXTreeTable component to display context variables from a JissModel ScriptContext.
 * Expands objects lazily to show their member variables.
 */
public class VariablesTreeTable extends JPanel {
    private final JXTreeTable treeTable;
    private final VariablesTreeTableModel tableModel;
    private final PhonShell phonShell;

    public VariablesTreeTable(Bindings bindings, PhonShell phonShell) {
        super(new BorderLayout());
        this.phonShell = phonShell;
        tableModel = new VariablesTreeTableModel(bindings);
        treeTable = new JXTreeTable(tableModel);
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);

        // Set custom renderer with Material Icons
        treeTable.setTreeCellRenderer(new VariablesTreeCellRenderer());

        // Add double-click listener
        treeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = treeTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        Object node = treeTable.getPathForRow(row).getLastPathComponent();
                        if (node instanceof VariablesTreeNode) {
                            String dotPath = ((VariablesTreeNode) node).getDotPath();
                            insertTextIntoConsole(dotPath);
                        }
                    }
                }
            }
        });

        // example of getting an icon
        ImageIcon materialIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                "code_braces", IconSize.SMALL, Color.DARK_GRAY);


        JScrollPane scrollPane = new JScrollPane(treeTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Insert text into the PhonShell console at the cursor position
     */
    private void insertTextIntoConsole(String text) {
        if (phonShell != null && phonShell.getConsole() != null) {
            try {
                phonShell.getConsole().getDocument().insertString(
                    phonShell.getConsole().getCaretPosition(),
                    text,
                    null
                );
                phonShell.getConsole().requestFocus();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Refresh the tree table with updated bindings
     */
    public void refresh(Bindings bindings) {
        tableModel.setBindings(bindings);
    }

    /**
     * Custom cell renderer for the tree table with Google Material Design Icons
     */
    static class VariablesTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final Map<VariablesTreeNode.NodeType, ImageIcon> ICON_CACHE = new HashMap<>();
        private static final IconManager iconManager = IconManager.getInstance();

        static {
            // Load Material Design Icons for each node type
            ICON_CACHE.put(VariablesTreeNode.NodeType.ROOT,
                iconManager.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                    "folder", IconSize.SMALL, new Color(100, 150, 200)));

            ICON_CACHE.put(VariablesTreeNode.NodeType.VARIABLE,
                iconManager.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                    "data_object", IconSize.SMALL, new Color(76, 175, 80)));

            ICON_CACHE.put(VariablesTreeNode.NodeType.FIELD,
                iconManager.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                    "label_important", IconSize.SMALL, new Color(255, 152, 0)));

            ICON_CACHE.put(VariablesTreeNode.NodeType.PROPERTY,
                iconManager.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                    "label", IconSize.SMALL, new Color(156, 39, 176)));

            ICON_CACHE.put(VariablesTreeNode.NodeType.FUNCTION,
                iconManager.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                    "functions", IconSize.SMALL, new Color(33, 150, 243)));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                       boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof VariablesTreeNode) {
                VariablesTreeNode node = (VariablesTreeNode) value;
                ImageIcon icon = ICON_CACHE.get(node.nodeType);
                if (icon != null) {
                    setIcon(icon);
                }
                setText(node.name);
            }

            return this;
        }
    }

    /**
     * TreeTableModel for variables.
     */
    static class VariablesTreeTableModel extends AbstractTreeTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Value", "Type"};
        private Bindings bindings;

        public VariablesTreeTableModel(Bindings bindings) {
            super(new VariablesTreeNode("Variables", null, null, true, bindings));
            this.bindings = bindings;
        }

        public void setBindings(Bindings bindings) {
            this.bindings = bindings;
            VariablesTreeNode root = new VariablesTreeNode("Variables", null, null, true, bindings);
            super.root = root;
            modelSupport.fireNewRoot();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(Object node, int column) {
            if (node instanceof VariablesTreeNode) {
                VariablesTreeNode varNode = (VariablesTreeNode) node;
                switch (column) {
                    case 0: // Name
                        return varNode.getName();
                    case 1: // Value
                        return varNode.getValueString();
                    case 2: // Type
                        return varNode.getTypeString();
                    default:
                        return null;
                }
            }
            return null;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof VariablesTreeNode) {
                return ((VariablesTreeNode) parent).getChildAt(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof VariablesTreeNode) {
                return ((VariablesTreeNode) parent).getChildCount();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof VariablesTreeNode && child instanceof VariablesTreeNode) {
                return ((VariablesTreeNode) parent).getIndex((VariablesTreeNode) child);
            }
            return -1;
        }
    }

    /**
     * TreeNode for variables and object members.
     */
    static class VariablesTreeNode {
        private boolean childrenLoaded = false;
        private final String name;
        private final Object value;
        private final boolean isRoot;
        private final Bindings bindings;
        private final List<VariablesTreeNode> children = new ArrayList<>();
        private final VariablesTreeNode parent;
        private final NodeType nodeType;
        private final Method method; // For method nodes

        enum NodeType {
            ROOT, VARIABLE, FIELD, PROPERTY, FUNCTION
        }

        // Root constructor
        public VariablesTreeNode(String name, Object value, Bindings bindings, boolean isRoot, Bindings rootBindings) {
            this(name, value, bindings, isRoot, rootBindings, null, NodeType.ROOT, null);
        }

        // Full constructor
        private VariablesTreeNode(String name, Object value, Bindings bindings, boolean isRoot,
                                   Bindings rootBindings, VariablesTreeNode parent, NodeType nodeType, Method method) {
            this.name = name;
            this.value = value;
            this.isRoot = isRoot;
            this.bindings = isRoot ? rootBindings : bindings;
            this.parent = parent;
            this.nodeType = nodeType;
            this.method = method;
        }

        public String getName() {
            return name;
        }

        public String getValueString() {
            if (isRoot) return "";
            if (nodeType == NodeType.FUNCTION) {
                return getMethodSignature();
            }
            if (value == null) return "null";
            if (isLeaf()) {
                try {
                    return value.toString();
                } catch (Exception e) {
                    return "<error: " + e.getMessage() + ">";
                }
            }
            return "";
        }

        public String getTypeString() {
            if (isRoot) return "";
            if (nodeType == NodeType.FUNCTION) {
                return "function";
            }
            if (value == null) return "null";
            return value.getClass().getSimpleName();
        }

        /**
         * Get the method signature for function nodes
         */
        private String getMethodSignature() {
            if (method == null) return "";

            StringBuilder sig = new StringBuilder();
            sig.append(method.getName()).append("(");

            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) sig.append(", ");
                sig.append(paramTypes[i].getSimpleName());
            }
            sig.append(")");

            return sig.toString();
        }

        /**
         * Get the dot path from root to this node
         */
        public String getDotPath() {
            List<String> pathParts = new ArrayList<>();
            VariablesTreeNode current = this;

            while (current != null && !current.isRoot) {
                if (current.nodeType == NodeType.FUNCTION) {
                    // For functions, include the method signature
                    pathParts.add(0, current.getMethodSignature());
                } else if (current.nodeType == NodeType.PROPERTY) {
                    // For properties, use the getter method call
                    String getterName = propertyNameToGetterName(current.name);
                    pathParts.add(0, getterName + "()");
                } else {
                    // For variables and fields, just use the name
                    pathParts.add(0, current.name);
                }
                current = current.parent;
            }

            return String.join(".", pathParts);
        }

        /**
         * Convert a property name back to its getter method name
         */
        private String propertyNameToGetterName(String propertyName) {
            if (propertyName == null || propertyName.isEmpty()) {
                return propertyName;
            }

            // Check if it looks like an acronym (all caps with length > 1)
            if (propertyName.length() > 1 && propertyName.equals(propertyName.toUpperCase())) {
                return "get" + propertyName;
            }

            // Capitalize first letter
            return "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        }

        public boolean isLeaf() {
            if (isRoot) return false;
            if (nodeType == NodeType.FUNCTION) return true; // Functions are leaves
            if (value == null) return true;
            Class<?> clazz = value.getClass();
            // Primitive types and common immutable types are leaves
            if (clazz.isPrimitive() 
                    || value instanceof String 
                    || value instanceof Number 
                    || value instanceof Boolean 
                    || value instanceof Character
                    || clazz.isEnum()) {
                return true;
            }
            return false;
        }

        public int getChildCount() {
            if (!childrenLoaded) {
                loadChildren();
            }
            return children.size();
        }

        public VariablesTreeNode getChildAt(int index) {
            if (!childrenLoaded) {
                loadChildren();
            }
            if (index >= 0 && index < children.size()) {
                return children.get(index);
            }
            return null;
        }

        public int getIndex(VariablesTreeNode child) {
            if (!childrenLoaded) {
                loadChildren();
            }
            return children.indexOf(child);
        }

        private void loadChildren() {
            if (childrenLoaded) return;
            children.clear();

            if (isRoot && bindings != null) {
                // Load root-level variables from bindings
                List<String> keys = new ArrayList<>(bindings.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    // Filter out 'context' variable from top-level entries
                    if ("context".equals(key)) {
                        continue;
                    }
                    Object val = bindings.get(key);
                    children.add(new VariablesTreeNode(key, val, bindings, false, null, this, NodeType.VARIABLE, null));
                }
            } else if (value != null && !isLeaf() && nodeType != NodeType.FUNCTION) {
                // Load object fields, properties, and methods
                try {
                    Class<?> clazz = value.getClass();

                    // Collect fields
                    List<Field> fields = new ArrayList<>();
                    Class<?> currentClass = clazz;
                    while (currentClass != null && currentClass != Object.class) {
                        fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                        currentClass = currentClass.getSuperclass();
                    }
                    Collections.sort(fields, Comparator.comparing(Field::getName));
                    
                    for (Field field : fields) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        if(Modifier.isPrivate(field.getModifiers())) {
                            continue;
                        }
                        try {
                            Object fieldValue = field.get(value);
                            children.add(new VariablesTreeNode(field.getName(), fieldValue, bindings, false, null, this, NodeType.FIELD, null));
                        } catch (IllegalAccessException e) {
//                            children.add(new VariablesTreeNode(field.getName(), "<inaccessible>", bindings, false, null, this, NodeType.FIELD, null));
                        }
                    }

                    // Collect methods
                    List<Method> methods = new ArrayList<>();
                    currentClass = clazz;
                    while (currentClass != null && currentClass != Object.class) {
                        methods.addAll(Arrays.asList(currentClass.getDeclaredMethods()));
                        currentClass = currentClass.getSuperclass();
                    }

                    // Process getters as properties and other methods as functions
                    Map<String, Method> getterMethods = new TreeMap<>();
                    Map<String, Method> otherMethods = new TreeMap<>();

                    for (Method m : methods) {
                        if (Modifier.isStatic(m.getModifiers())) {
                            continue;
                        }
                        if(Modifier.isPrivate(m.getModifiers())) {
                            continue;
                        }
                        String methodName = m.getName();

                        // Check if it's a getter (getXxx with no parameters and non-void return)
                        if (methodName.startsWith("get") && methodName.length() > 3
                                && m.getParameterCount() == 0 && m.getReturnType() != void.class) {
                            String propertyName = getterNameToPropertyName(methodName);
                            getterMethods.put(propertyName, m);
                        } else {
                            // All other methods (including isXxx, hasXxx, etc.)
                            otherMethods.put(methodName, m);
                        }
                    }

                    // Add properties (from getters)
                    for (Map.Entry<String, Method> entry : getterMethods.entrySet()) {
                        String propertyName = entry.getKey();
                        Method getter = entry.getValue();
                        if(Modifier.isPrivate(getter.getModifiers())) {
                            continue;
                        }
                        try {
//                            getter.setAccessible(true);
                            Object propertyValue = getter.invoke(value);
                            children.add(new VariablesTreeNode(propertyName, propertyValue, bindings, false, null, this, NodeType.PROPERTY, getter));
                        } catch (Exception e) {
//                            children.add(new VariablesTreeNode(propertyName, "<error>", bindings, false, null, this, NodeType.PROPERTY, getter));
                        }
                    }

                    // Add functions (other methods)
                    for (Map.Entry<String, Method> entry : otherMethods.entrySet()) {
                        String methodName = entry.getKey();
                        Method method = entry.getValue();
                        if(Modifier.isStatic(method.getModifiers())) {
                            continue;
                        }
                        children.add(new VariablesTreeNode(methodName, null, bindings, false, null, this, NodeType.FUNCTION, method));
                    }

                } catch (Exception e) {
                    // If we can't introspect, just don't add children
                }
            }
            
            // Mark children as loaded after successful processing
            childrenLoaded = true;
        }

        /**
         * Convert getter method name to property name
         * e.g., getName -> name, getURL -> URL (for acronyms)
         */
        private String getterNameToPropertyName(String getterName) {
            if (!getterName.startsWith("get") || getterName.length() <= 3) {
                return getterName;
            }

            String withoutGet = getterName.substring(3);

            // Check if the remaining part is all uppercase (acronym)
            if (withoutGet.length() > 1 && withoutGet.equals(withoutGet.toUpperCase())) {
                // It's an acronym like URL, ID, etc. - keep it as is
                return withoutGet;
            }

            // Normal case: lowercase the first character
            return Character.toLowerCase(withoutGet.charAt(0)) + withoutGet.substring(1);
        }
    }
}
