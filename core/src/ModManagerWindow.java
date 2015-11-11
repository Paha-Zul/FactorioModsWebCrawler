import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.event.*;
import java.io.File;

public class ModManagerWindow extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane modListTab;
    private JPanel modBrowserTab;
    private JTree tree;

    private String modPath = "fm/";

    public ModManagerWindow() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
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
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        init();

    }

    private void onOK() {
        // add your code here
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

    public void init(){
        String factorioPath = System.getProperty("user.home") + "/AppData" + "/Roaming/Factorio/"+this.modPath+"/";
        File modManagerDir = new File(factorioPath);
        if(!modManagerDir.exists())
            modManagerDir.mkdir();
        else{
            File[] files = modManagerDir.listFiles();
            String[] profiles = new String[files.length];
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("profiles");
            TreeModel model = new DefaultTreeModel(root);

            for(int i=0;i<files.length;i++) {
                profiles[i] = files[i].getName();
                DefaultMutableTreeNode profile = new DefaultMutableTreeNode(files[i].getName());
                root.add(profile);

                File[] list = files[i].listFiles();
                if(list != null)
                    for(int k=0;k<list.length;k++){
                        profile.add(new DefaultMutableTreeNode(list[k].getName()));
                    }
            }

            tree.setModel(model);

        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
