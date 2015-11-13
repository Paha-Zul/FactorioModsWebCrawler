import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ModManagerWindow extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabs;
    private JPanel modBrowserPanel;
    private JTree tree;
    private JPanel modInfoPanel;
    private JList modInfoList;
    private JPanel modListPanel;
    private JTable browserTable;
    private JTable testTable;

    private String factorioDataPath;
    private String factorioModPath;
    private String factorioExecutablePath;
    private String factorioModManagerPath = "fm/";

    private HashMap<String, Profile.ModListJson> modListMap = new HashMap<>();
    private HashMap<String, Profile> modProfileMap = new HashMap<>();

    private Profile selectedProfile;

    public ModManagerWindow() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                if (node.isLeaf() && node.getUserObject() instanceof CheckBoxNode) {
                    CheckBoxNode checkBox = (CheckBoxNode) node.getUserObject();
                    String profileName = node.getParent().toString();
                    Profile.Mod mod = Profile.getModByNameForProfile(profileName, checkBox.getText());

                    //Set up the initial mod information.
                    Object[] modInfo = {
                            "Mod Name: " + mod.name,
                            "Author: " + mod.info.author,
                            "Version: " + mod.info.version,
                    };

                    DefaultListModel listModel = new DefaultListModel();
                    for (Object obj : modInfo)
                        listModel.addElement(obj);

                    modInfoList.setModel(listModel);

                    Runnable task = () -> Crawler.getVersionsOfMod(mod.name);
                    Thread thread = new Thread(task);
                    thread.start();

                    Timer timer = new Timer(200, null);
                    timer.addActionListener(ev -> {
                        if(thread.getState()!=Thread.State.TERMINATED)
                            timer.restart();
                        else{
                            timer.stop();

                            //Get the modVersionInfo from the crawler. If not null, add to the list.
                            String[][] modVersionInfo = Crawler.getModVersionInfo();
                            if(modVersionInfo != null) {
                                for (String[] info : modVersionInfo)
                                    listModel.addElement(info[0]);

                                modInfoList.setModel(listModel);
                            }
                            System.out.println("Done getting version");
                        }
                    });
                    timer.start();
                }

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
                Runnable task = () -> Crawler.processFilter("http://www.factoriomods.com/recently-updated", 1);
                Thread thread = new Thread(task);
                thread.start();
                final javax.swing.Timer timer = new javax.swing.Timer(100, null);
                timer.addActionListener(ev -> {
                    if(thread.getState()!=Thread.State.TERMINATED)
                        timer.restart();
                    else{
                        System.out.println("Done with page");
                        timer.stop();

                        ArrayList<String[]> modInfoListFromBrowser = Crawler.getSpecificFilterModsInfo();
                        DefaultTableModel model = new DefaultTableModel(new String[]{"","","",""}, 0);
                        for(String[] info : modInfoListFromBrowser)
                            model.addRow(info);

                        browserTable.setModel(model);
                    }
                });

                timer.start();
            }
        });

        initModListTab();

    }

    private void onOK() {
        //Get the file for the mod directory.
        File modDir = new File(this.factorioModPath);
        deleteAllFiles(modDir);

        //We get the dir of the profile of mods and get the list of files inside the dir.
        //We get the selected node. If it's a leaf, try to get the parent (which is most likely the profile)
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)this.tree.getLastSelectedPathComponent();
        if(selectedNode.isLeaf()) selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();

        File profileDir = null;
        if(!selectedNode.isRoot()) {
            profileDir = new File(this.factorioDataPath + factorioModManagerPath + selectedNode);
            copyAllEnabledMods(profileDir, modDir, selectedNode);

            //Run the factorio executable.
            try {
                Process p = Runtime.getRuntime().exec(this.factorioExecutablePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO Deal with not having the right thing selected.

        //writeModsToJson(FileUtils.findFileWithPartName(profileDir, "mod-list.json"), modListMap.get(profileDir.getName()));
        Profile selectedProfile = this.modProfileMap.get(profileDir.getName());
        selectedProfile.writeModList(selectedNode.children());

        // add your code here
        dispose();
    }

    private void writeModsToJson(File file, Profile.ModListJson modList){
        modList.mods.add(0, new Profile.Mod("base", true)); //Hack the base mod into there.
        FileUtils.writeObjectToJson(file, modList);
    }

    /**
     * Deletes all files excluding .json files in a directory.
     * @param dir The directory to delete files in.
     */
    private void deleteAllFiles(File dir){
        File[] currentModList = dir.listFiles();

        for(File file : currentModList){
            if(file.getName().endsWith(".json")) continue;
            file.setWritable(true); //We can't delete if read only, so try and make writeable.
            if(!file.delete()) //Delete the file.
                System.out.println("Something went wrong, the file "+file.getName()+" couldn't be deleted.");
        }
    }

    private void copyAllEnabledMods(File profileModDir, File factorioModDir, DefaultMutableTreeNode selectedNode){
        //Let's copy all enabled mods!

        //Get the selected node.
        Enumeration<DefaultMutableTreeNode> children = selectedNode.children(); //Get it's children.
        while(children.hasMoreElements()){
            DefaultMutableTreeNode node = children.nextElement(); //Get the next element.
            CheckBoxNode checkBox = (CheckBoxNode)node.getUserObject(); //Get the checkbox.
            String name = checkBox.getText(); //Get the text from the checkbox.
            if(name.equals("base") || !checkBox.isSelected()) continue; //If it is the "base" mod, ignore it (it's always on)

            //Get the file with the name of the mod and then copy it from the profile dir to the mods dir.
            File file = FileUtils.findFileWithPartName(profileModDir, ((CheckBoxNode)node.getUserObject()).getText());
            try {
                Files.copy(file.toPath(), Paths.get(factorioModDir.getPath()+"/"+file.getPath().substring(file.getPath().lastIndexOf('\\'))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    public void initModListTab(){
        //Try to find a default path to Factorio.exe
        factorioExecutablePath = System.getenv("ProgramFiles")+"/Factorio/bin/x64/Factorio.exe";
        if(!new File(factorioExecutablePath).exists()) {
            factorioExecutablePath = System.getenv("ProgramFiles") + "/Factorio/bin/x86/Factorio.exe";
            if(!new File(factorioExecutablePath).exists()){
                //TODO Disable play button?
                this.buttonOK.setEnabled(false);
            }
        }

        //The factorio data path
        this.factorioDataPath = System.getProperty("user.home") + "/AppData" + "/Roaming/Factorio/";
        this.factorioModPath = this.factorioDataPath + "/mods/"; //The mods path.

        //The mod manager path.
        String modManPath = this.factorioDataPath + this.factorioModManagerPath +"/";
        File modManagerDir = new File(modManPath); //The actual file.

        /*
            Here we will go into the mod directory (/fm/) and load all folders inside the directory to
            become their own profiles. Each profile will handle loading their own files (mods) and we will use
            those mod files to build a tree with checkboxes.
         */

        if(!modManagerDir.exists())
            modManagerDir.mkdir();
        else{
            HashMap<String, Boolean> enabledMods = new HashMap<>();

            //Get the list of folder (files) in this dir. Also, make a tree model.
            File[] files = modManagerDir.listFiles();
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("profiles");
            TreeModel model = new DefaultTreeModel(root);

            if(files != null && files.length > 0) {

                //For each dir under the 'fm' dir, we want to add a tree node to the tree (with a checkbox)
                for (File profileDir : files) {
                    DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(profileDir.getName()); //Add the name to the tree.
                    root.add(profileNode); //Add it.

                    //This gets the modList from the mod-list.json and dictates which mods are selected/unselected.
                    Profile.ModListJson modList = FileUtils.getJsonObject(FileUtils.findFileWithPartName(profileDir, "mod-list"), Profile.ModListJson.class);
                    for(Profile.Mod mod : modList.mods) enabledMods.put(mod.name, mod.enabled);
                    modListMap.put(profileDir.getName(), modList); //Add a modName - modList link.

                    //Then we want to load a new profile, get the mods, and add a tree node to the tree for each one.
                    Profile profile = new Profile(profileDir); //Make a profile object.
                    ArrayList<Profile.Mod> mods = profile.getMods().mods; //Get the list of Mods it detected.
                    modProfileMap.put(profileDir.getName(), profile); //Put the profile into the map.

                    for (Profile.Mod mod : mods) { //For each mod, add the name to the tree.
                        if(mod.name.equals("base")) continue;
                        profileNode.add(new DefaultMutableTreeNode(new CheckBoxNode(mod.name, mod.enabled)));
                    }
                }
            }else{
                root.add(new DefaultMutableTreeNode("Something went wrong, no files in the fm dir?"));

            }

            tree.setModel(model);
            tree.setCellRenderer(new CheckBoxNodeRenderer());
            tree.setCellEditor(new CheckBoxNodeEditor(tree));
            tree.setEditable(true);

        }
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

    class CheckBoxNode {
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
