package ca.phon.shell.components;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
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

    public VariablesTreeTable(Bindings bindings) {
        super(new BorderLayout());
        tableModel = new VariablesTreeTableModel(bindings);
        treeTable = new JXTreeTable(tableModel);
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);
        JScrollPane scrollPane = new JScrollPane(treeTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refresh the tree table with updated bindings
     */
    public void refresh(Bindings bindings) {
        tableModel.setBindings(bindings);
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
            setRoot(root);
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

        public VariablesTreeNode(String name, Object value, Bindings bindings, boolean isRoot, Bindings rootBindings) {
            this.name = name;
            this.value = value;
            this.isRoot = isRoot;
            this.bindings = isRoot ? rootBindings : bindings;
        }

        public String getName() {
            return name;
        }

        public String getValueString() {
            if (isRoot) return "";
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
            if (value == null) return "null";
            return value.getClass().getSimpleName();
        }

        public boolean isLeaf() {
            if (isRoot) return false;
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
            childrenLoaded = true;
            children.clear();

            if (isRoot && bindings != null) {
                // Load root-level variables from bindings
                List<String> keys = new ArrayList<>(bindings.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    Object val = bindings.get(key);
                    children.add(new VariablesTreeNode(key, val, bindings, false, null));
                }
            } else if (value != null && !isLeaf()) {
                // Load object fields
                try {
                    Class<?> clazz = value.getClass();
                    List<Field> fields = new ArrayList<>();
                    
                    // Get all fields including inherited ones
                    Class<?> currentClass = clazz;
                    while (currentClass != null && currentClass != Object.class) {
                        fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                        currentClass = currentClass.getSuperclass();
                    }
                    
                    // Sort fields by name
                    Collections.sort(fields, Comparator.comparing(Field::getName));
                    
                    for (Field field : fields) {
                        // Skip static fields
                        if (Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        
                        field.setAccessible(true);
                        try {
                            Object fieldValue = field.get(value);
                            children.add(new VariablesTreeNode(field.getName(), fieldValue, bindings, false, null));
                        } catch (IllegalAccessException e) {
                            children.add(new VariablesTreeNode(field.getName(), "<inaccessible>", bindings, false, null));
                        }
                    }
                } catch (Exception e) {
                    // If we can't introspect, just don't add children
                }
            }
        }
    }
}
