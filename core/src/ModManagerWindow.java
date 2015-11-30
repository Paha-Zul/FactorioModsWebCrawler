import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import java.util.Vector;

public class ModManagerWindow extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabs;
    private JPanel modBrowserPanel;
    private JTree modTree;
    private JPanel modInfoPanel;
    private JList modInfoList;
    private JPanel modListPanel;
    private JTable browserTable;

    private ModManagerWindowController windowController;
    private ModManagerWindowModel windowModel;

    private Profile selectedProfile;

    public ModManagerWindow() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.windowController = new ModManagerWindowController(this);
        this.windowModel = new ModManagerWindowModel(this);

        this.windowController.setModManagerWindowModel(this.windowModel);
        this.windowModel.setModManagerWindowController(this.windowController);

        this.windowController.configurePaths();


        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        //An event that happens when we select something in the modTree.
        modTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

            //Gotta make sure it's a leaf AND a checkbox
            if (node.isLeaf() && node.getUserObject() instanceof CheckBoxNode) {
                this.windowController.setModInformation(node, this.modInfoList);
            }

        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tabs.addChangeListener((ChangeEvent e) -> {
            if(tabs.getSelectedComponent().getName() != null && tabs.getSelectedComponent().getName().equals("browser")){
                this.windowController.downloadRecentModList();
            }
        });

        this.modInfoList.addListSelectionListener(ev -> {
            if(ev.getValueIsAdjusting()) {
                this.windowController.downloadVersionOfMod(this.modInfoList.getSelectedValue().toString(),
                        this.modInfoList.getSelectedIndex());
            }
        });

        initModListTab();

    }

    private void onOK() {
        if(this.windowController.launchFactorio((DefaultMutableTreeNode) this.modTree.getLastSelectedPathComponent()))
            dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        ModManagerWindow dialog = new ModManagerWindow();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    /**
     * Initializes the ModList tab and populates it with the mod list.
     */
    public void initModListTab(){
        TreeModel model = this.windowController.initModList();

        modTree.setModel(model);
        modTree.setCellRenderer(new CheckBoxNodeRenderer());
        modTree.setCellEditor(new CheckBoxNodeEditor(modTree));
        modTree.setEditable(true);

    }

    public void setModBrowserTableModel(DefaultTableModel model){
        this.browserTable.setModel(model);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    class CheckBoxNodeRenderer implements TreeCellRenderer {
        private JCheckBox leafRenderer = new JCheckBox();

        private DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();

        Color selectionBorderColor, selectionForeground, selectionBackground,
                textForeground, textBackground;

        protected JCheckBox getLeafRenderer() {
            return leafRenderer;
        }

        public CheckBoxNodeRenderer() {
            Font fontValue;
            fontValue = UIManager.getFont("Tree.font");
            if (fontValue != null) {
                leafRenderer.setFont(fontValue);
            }
            Boolean booleanValue = (Boolean) UIManager
                    .get("Tree.drawsFocusBorderAroundIcon");
            leafRenderer.setFocusPainted((booleanValue != null)
                    && (booleanValue.booleanValue()));

            selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
            selectionForeground = UIManager.getColor("Tree.selectionForeground");
            selectionBackground = UIManager.getColor("Tree.selectionBackground");
            textForeground = UIManager.getColor("Tree.textForeground");
            textBackground = UIManager.getColor("Tree.textBackground");
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {

            Component returnValue;
            if (leaf) {

                String stringValue = tree.convertValueToText(value, selected,
                        expanded, leaf, row, false);
                leafRenderer.setText(stringValue);
                leafRenderer.setSelected(false);

                leafRenderer.setEnabled(tree.isEnabled());

                if (selected) {
                    leafRenderer.setForeground(selectionForeground);
                    leafRenderer.setBackground(selectionBackground);
                } else {
                    leafRenderer.setForeground(textForeground);
                    leafRenderer.setBackground(textBackground);
                }

                if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                    Object userObject = ((DefaultMutableTreeNode) value)
                            .getUserObject();
                    if (userObject instanceof CheckBoxNode) {
                        CheckBoxNode node = (CheckBoxNode) userObject;
                        leafRenderer.setText(node.getText());
                        leafRenderer.setSelected(node.isSelected());
                    }
                }
                returnValue = leafRenderer;
            } else {
                returnValue = nonLeafRenderer.getTreeCellRendererComponent(tree,
                        value, selected, expanded, leaf, row, hasFocus);
            }
            return returnValue;
        }
    }

    class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {

        CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();

        ChangeEvent changeEvent = null;

        JTree tree;

        public CheckBoxNodeEditor(JTree tree) {
            this.tree = tree;
        }

        public Object getCellEditorValue() {
            JCheckBox checkbox = renderer.getLeafRenderer();
            CheckBoxNode checkBoxNode = new CheckBoxNode(checkbox.getText(),
                    checkbox.isSelected());
            return checkBoxNode;
        }

        public boolean isCellEditable(EventObject event) {
            boolean returnValue = false;
            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                TreePath path = tree.getPathForLocation(mouseEvent.getX(),
                        mouseEvent.getY());
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if ((node != null) && (node instanceof DefaultMutableTreeNode)) {
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                        Object userObject = treeNode.getUserObject();
                        returnValue = ((treeNode.isLeaf()) && (userObject instanceof CheckBoxNode));
                    }
                }
            }
            return returnValue;
        }

        public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                    boolean selected, boolean expanded, boolean leaf, int row) {

            Component editor = renderer.getTreeCellRendererComponent(tree, value,
                    true, expanded, leaf, row, true);

            // editor always selected / focused
            ItemListener itemListener = itemEvent -> {
                if (stopCellEditing()) {
                    fireEditingStopped();
                }
            };
            if (editor instanceof JCheckBox) {
                ((JCheckBox) editor).addItemListener(itemListener);
            }

            return editor;
        }
    }

    public static class CheckBoxNode {
        String text;

        boolean selected;

        public CheckBoxNode(String text, boolean selected) {
            this.text = text;
            this.selected = selected;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean newValue) {
            selected = newValue;
        }

        public String getText() {
            return text;
        }

        public void setText(String newValue) {
            text = newValue;
        }

        public String toString() {
            return getClass().getName() + "[" + text + "/" + selected + "]";
        }
    }

    class NamedVector extends Vector {
        String name;

        public NamedVector(String name) {
            this.name = name;
        }

        public NamedVector(String name, Object elements[]) {
            this.name = name;
            for (Object element : elements) {
                add(element);
            }
        }

        public String toString() {
            return "[" + name + "]";
        }
    }
}
