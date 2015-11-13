import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Paha on 11/11/2015.
 */
public class Profile {
    private static ObjectMapper mapper = new ObjectMapper();
    private static HashMap<String, Profile> modProfileMap = new HashMap<>();

    private HashMap<String, Mod> modMap = new HashMap<>();
    private String fileName;
    private File filePath, modListJsonFile;
    private Mods mods;

    static{
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Profile(File profileDir){
        this.filePath = profileDir;
        this.fileName = filePath.getPath().substring(filePath.getPath().lastIndexOf('/')+1);

        this.modListJsonFile = null;

        //Load the all the .zip files that have a info.json file, also, find the modListJson file!
        File[] list = profileDir.listFiles();
        this.mods = new Mods();
        this.mods.mods = new ArrayList<>();

        for(File file : list) {
            if(file.getName().endsWith(".json"))
                this.modListJsonFile = file;
            else{
                String fileNameWithoutVersion = file.getName().substring(0, file.getName().lastIndexOf('_'));
                ModInfo info = getModInfo(file);
                if(info == null) continue;                  //Not a mod?
                Mod mod = new Mod(fileNameWithoutVersion, true);    //Make a new mod.
                mod.filePath = file.getPath();
                this.mods.mods.add(mod);                    //We'll set to true for now.
                this.modMap.put(fileNameWithoutVersion, mod); //Put it in the mod map.
            }
        }

        //Load the modList.
        ModListJson modList = FileUtils.getJsonObject(FileUtils.findFileWithPartName(this.filePath, "mod-list.json"), ModListJson.class);

        //We now go through all the mods in the modList json and sync them with the mods in the folder.
        for(Mod modInModList : modList.mods){
            Mod loadedMod = this.modMap.get(modInModList.name); //Try to get the mod with the name.
            if(loadedMod == null) continue; //Must be a new mod, leave it as default 'true'
            loadedMod.enabled = modInModList.enabled; //Sync!
        }

        for(Mod mod : this.mods.mods) {
            if(mod.name.equals("base")) continue;
            File file = FileUtils.findFileWithPartName(profileDir, mod.name);
            mod.info = this.getModInfo(file);
            mod.filePath = file.getPath();
            System.out.println("mod path: "+mod.filePath);
        }

        modProfileMap.put(this.filePath.getName(), this); //Add ourselves to the master list.
    }

    public Profile(String filePath){
        this(new File(filePath));
    }

    public Mods getMods(){
        return this.mods;
    }

    /**
     * Tries to open the File as a .zip and traverse the contents for the 'info.json' file.
     * @param file The .zip file (hopefully) of the mod.
     * @return A ModInfo object if successful, null otherwise.
     */
    private ModInfo getModInfo(File file){
        try {
            ZipFile zipFile = new ZipFile(file); //Get the zipfile
            Enumeration<? extends ZipEntry> entries = zipFile.entries(); //Get all the entries

            //Loop over each entry.
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement(); //Next
                if(zipEntry.getName().endsWith("info.json")) //Only take info.json
                    return mapper.readValue(zipFile.getInputStream(zipEntry), ModInfo.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void writeModList(Enumeration<DefaultMutableTreeNode> children){
        this.mods.mods = new ArrayList<>();
        this.mods.mods.add(new Mod("base", true)); //Hack the base mod into here.

        //For each node (which is a mod), make a new list of Mods and write them to the file.
        while(children.hasMoreElements()){
            DefaultMutableTreeNode node = children.nextElement();
            ModManagerWindow.CheckBoxNode checkBoxNode = (ModManagerWindow.CheckBoxNode)node.getUserObject();
            this.mods.mods.add(new Mod(checkBoxNode.getText(), checkBoxNode.isSelected()));
        }

        FileUtils.writeObjectToJson(this.modListJsonFile, this.mods);
    }

    public static Mod[] getModsForProfile(String name){
        Mods mods = Profile.modProfileMap.get(name).mods;
        if(mods == null) return null;
        return mods.mods.toArray(new Mod[mods.mods.size()]);
    }

    public static Mod getModByNameForProfile(String profileName, String modName){
        Profile profile = Profile.modProfileMap.get(profileName);
        if(profile == null) return null;
        return profile.modMap.get(modName);
    }

    public static class ModListJson{
        @JsonProperty
        public ArrayList<Mod> mods;
    }

    public static class Mods{
        @JsonProperty
        public ArrayList<Mod> mods;
    }

    public static class Mod{
        @JsonProperty
        public String name;
        @JsonIgnore
        public String filePath;
        @JsonProperty
        public boolean enabled;
        @JsonIgnore
        public ModInfo info;

        public Mod(){

        }

        public Mod(String name, boolean enabled){
            this.name = name;
            this.enabled = enabled;
        }
    }

    public static class ModInfo{
        @JsonProperty
        public String name, version, title, author, homepage, description;
        @JsonProperty
        public String[] dependencies;
    }
}
