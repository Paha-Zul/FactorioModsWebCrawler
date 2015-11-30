import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Created by Paha on 11/29/2015.
 * Processes calls from the ModManagerWindow and stores the data in ModManagerWindowModel
 */
public class ModManagerWindowController {
    ModManagerWindow window;
    ModManagerWindowModel model;

    public ModManagerWindowController(ModManagerWindow window){
        this.window = window;
    }

    public void setModManagerWindowModel(ModManagerWindowModel model){
        this.model = model;
    }

    /**
     * Configures all the paths needed for the program.
     */
    public void configurePaths(){
        //Try to find a default path to Factorio.exe
        String factorioExecutablePath = System.getenv("ProgramFiles")+"/Factorio/bin/x64/Factorio.exe";
        if(!new File(factorioExecutablePath).exists()) {
            factorioExecutablePath = System.getenv("ProgramFiles") + "/Factorio/bin/x86/Factorio.exe";
            if(!new File(factorioExecutablePath).exists()){
                //TODO Disable play button?
                System.out.println("Couldn't get a path to factorio.exe");
            }
        }

        //The factorio data path and mod path.
        String factorioDataPath = System.getProperty("user.home") + "/AppData" + "/Roaming/Factorio/";
        String factorioModPath = factorioDataPath + "/mods/"; //The mods path.

        //The mod manager path.
        String factorioModManagerPath = factorioDataPath + model.getFactorioModManagerFolder() +"/";

        this.model.setPaths(factorioDataPath, factorioModPath, factorioExecutablePath, factorioModManagerPath);
    }

    public void launchFactorio(DefaultMutableTreeNode lastSelected){
        //Get the file for the mod directory.
        File modDir = new File(this.model.getFactorioModPath());
        FileUtils.deleteAllFiles(modDir);

        //We get the dir of the profile of mods and get the list of files inside the dir.
        //We get the selected node. If it's a leaf, try to get the parent (which is most likely the profile)
        if(lastSelected.isLeaf()) lastSelected = (DefaultMutableTreeNode)lastSelected.getParent();

        File profileDir = null;
        if(!lastSelected.isRoot()) {
            profileDir = new File(this.model.getFactorioModManagerPath() + lastSelected);
            copyAllEnabledMods(profileDir, modDir, lastSelected);

            //Run the factorio executable.
            try {
                Process p = Runtime.getRuntime().exec(this.model.getFactorioExecutablePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO Deal with not having the right thing selected.

        //writeModsToJson(FileUtils.findFileWithPartName(profileDir, "mod-list.json"), modListMap.get(profileDir.getName()));
        Profile selectedProfile = this.model.getModProfileMap().get(profileDir.getName());
        selectedProfile.writeModListToJson(lastSelected.children());
    }

    public TreeModel initModList(){
        File modManagerDir = new File(this.model.getFactorioModManagerPath()); //The actual file.

        /*
            Here we will go into the mod directory (/fm/) and load all folders inside the directory to
            become their own profiles. Each profile will handle loading their own files (mods) and we will use
            those mod files to build a modTree with checkboxes.
         */

        TreeModel model = null;

        if(!modManagerDir.exists())
            modManagerDir.mkdir();
        else {
            HashMap<String, Boolean> enabledMods = new HashMap<>();

            //Get the list of folder (files) in this dir. Also, make a modTree model.
            File[] files = modManagerDir.listFiles();
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("profiles");
            model = new DefaultTreeModel(root);

            if (files != null && files.length > 0) {

                //For each dir under the 'fm' dir, we want to add a modTree node to the modTree (with a checkbox)
                for (File profileDir : files) {
                    DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(profileDir.getName()); //Add the name to the modTree.
                    root.add(profileNode); //Add it.

                    //This gets the modList from the mod-list.json and dictates which mods are selected/unselected.
                    Profile.ModListJson modList = FileUtils.getJsonObject(FileUtils.findFileWithPartName(profileDir, "mod-list"), Profile.ModListJson.class);
                    for (Profile.Mod mod : modList.mods) enabledMods.put(mod.name, mod.enabled);
                    this.model.getModListMap().put(profileDir.getName(), modList); //Add a modName - modList link.

                    //Then we want to load a new profile, get the mods, and add a modTree node to the modTree for each one.
                    Profile profile = new Profile(profileDir); //Make a profile object.
                    ArrayList<Profile.Mod> mods = profile.getMods().mods; //Get the list of Mods it detected.
                    this.model.getModProfileMap().put(profileDir.getName(), profile); //Put the profile into the map.

                    for (Profile.Mod mod : mods) { //For each mod, add the name to the modTree.
                        if (mod.name.equals("base")) continue;
                        profileNode.add(new DefaultMutableTreeNode(new ModManagerWindow.CheckBoxNode(mod.name, mod.enabled)));
                    }
                }
            } else {
                root.add(new DefaultMutableTreeNode("Something went wrong, no files in the fm dir?"));

            }
        }

        return model;
    }

    /**
     * Downloads a version of a mod.
     * @param modVersion The version text for the mod.
     * @param selectedIndex The selected index from the UI.
     */
    public void downloadVersionOfMod(String modVersion, int selectedIndex){
        //Trim whitespace and check if we have a 'v0.1' or some match.
        if (modVersion.trim().matches("v\\d+.*")) {
            selectedIndex = selectedIndex - 4; //4 is our magic number. We have 4 lines before this one so we subtract 4 to get the adjusted index.
            String[][] info = Crawler.getModVersionInfo();
            String path = this.model.getFactorioModManagerPath() + this.model.getCurrentlySelectedProfile()+"/"
                    + this.model.getCurrentlySelectedModName()+"_"+info[selectedIndex][Crawler.ModVersionInfo.VERSION.getValue()]+".zip";

            //Create a new file and try to download into it.
            File file = new File(path);
            try {
                org.apache.commons.io.FileUtils.copyURLToFile(new URL(info[selectedIndex][Crawler.ModVersionInfo.DOWNLOAD.getValue()]), file, 2000, 2000);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Index: " + info[selectedIndex][Crawler.ModVersionInfo.DOWNLOAD.getValue()]);
        }
    }

    /**
     * Pulls the recently updated mods from the mod website.
     */
    public void downloadRecentModList(){
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

                this.window.setModBrowserTableModel(model);
            }
        });

        timer.start();
    }

    /**
     * Sets the mod information for the selected mod. Also will attempt to display any recent versions of the mod
     * from the factorio mod website.
     * @param node The selected node that contains the mod name and such.
     * @param modInfoList The JList to modify with mod information.
     */
    public void setModInformation(DefaultMutableTreeNode node, JList modInfoList){
        //Get the checkbox (for its value) and the profile name. Get the actual mod with this.
        ModManagerWindow.CheckBoxNode checkBox = (ModManagerWindow.CheckBoxNode) node.getUserObject();
        String profileName = node.getParent().toString();
        Profile.Mod mod = Profile.getModByNameForProfile(profileName, checkBox.getText());
        this.model.setCurrentlySelectedModName(checkBox.getText());
        this.model.setCurrentlySelectedProfile(profileName);

        //Set up the initial mod information.
        Object[] modInfo = {
                "Mod Name: " + mod.name,
                "Author: " + mod.info.author,
                "Version: " + mod.info.version,
        };

        //Set the list model.
        DefaultListModel listModel = new DefaultListModel();
        for (Object obj : modInfo)
            listModel.addElement(obj);

        modInfoList.setModel(listModel);

        this.getRecentVersionsOfModAsync(mod, modInfoList);
    }

    /**
     * Attempts to search the mod website for the mod and pull the recent versions of the mod.
     * @param mod The Mod to search for on the website.
     * @param modInfoList The JList to populate/alter.
     */
    public void getRecentVersionsOfModAsync(Profile.Mod mod, JList modInfoList){
        //Here we set a thread task to get the version numbers for the mod. This will look at the site
        //and search for the mod, then pull all versions from it.
        Runnable task = () -> Crawler.readVersionInfoOfMod(mod.name);
        Thread thread = new Thread(task);
        thread.start();

        //Our timer that checks every 200ms if the thread has finished.
        Timer timer = new Timer(200, null);
        timer.addActionListener(ev -> {
            if (thread.getState() != Thread.State.TERMINATED)
                timer.restart();
            else {
                timer.stop();
                DefaultListModel listModel = (DefaultListModel)modInfoList.getModel();
                //Get the modVersionInfo from the crawler. If not null, add to the list.
                String[][] modVersionInfo = Crawler.getModVersionInfo();
                if (modVersionInfo != null) {
                    listModel.addElement("Recent Versions:");
                    for (String[] info : modVersionInfo) {
                        listModel.addElement("    v" + info[0] + " for " + info[1]);
                    }
                } else {
                    listModel.addElement("Couldn't find the mod on the website.");
                }
                modInfoList.setModel(listModel);
            }
        });
        timer.start();
    }

    /**
     * Copies all the mod files from the profileModDir to the factorioModDir.
     * @param profileModDir The profileDir to copy mods from.
     * @param factorioModDir The factorioDir to copy mods to.
     * @param selectedNode The TreeNode that is currently selected.
     */
    private void copyAllEnabledMods(File profileModDir, File factorioModDir, DefaultMutableTreeNode selectedNode){
        //Let's copy all enabled mods!

        //Get the selected node.
        Enumeration<DefaultMutableTreeNode> children = selectedNode.children(); //Get it's children.
        while(children.hasMoreElements()){
            DefaultMutableTreeNode node = children.nextElement(); //Get the next element.
            ModManagerWindow.CheckBoxNode checkBox = (ModManagerWindow.CheckBoxNode)node.getUserObject(); //Get the checkbox.
            String name = checkBox.getText(); //Get the text from the checkbox.
            if(name.equals("base") || !checkBox.isSelected()) continue; //If it is the "base" mod, ignore it (it's always on)

            //Get the file with the name of the mod and then copy it from the profile dir to the mods dir.
            File file = FileUtils.findFileWithPartName(profileModDir, ((ModManagerWindow.CheckBoxNode)node.getUserObject()).getText());
            try {
                Files.copy(file.toPath(), Paths.get(factorioModDir.getPath() + "/" + file.getPath().substring(file.getPath().lastIndexOf('\\'))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
