import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Paha on 11/11/2015.
 */
public class Profile {
    private static ObjectMapper mapper = new ObjectMapper();
    private static HashMap<String, Mods> modMap = new HashMap<>();

    private String filePath, fileName;
    private Mods mods;

    static{
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Profile(File profileDir){
        this.filePath = profileDir.getPath();
        this.fileName = filePath.substring(filePath.lastIndexOf('/')+1);

        File jsonFile = null;

        //Find the json file!
        File[] list = profileDir.listFiles();
        for(File file : list) {
            if(file.getName().endsWith(".json")) {
                jsonFile = file;
                break;
            }
        }

        //Parse the json file!
        if(jsonFile != null){
            try {
                this.mods = mapper.readValue(jsonFile, Mods.class); //Get the object
                Profile.modMap.put(profileDir.getName(), this.mods); //Put it in the master map.

//                for(Mod mod : this.mods.mods)
//                    mod.filePath = dir + "/" + mod.name+".zip";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for(Mod mod : this.mods.mods) {
            if(mod.name.equals("base")) continue;
            File file = FileUtils.findFileWithPartName(profileDir, mod.name);
            mod.info = getModInfo(file);
            mod.filePath = file.getPath();
            System.out.println("mod path: "+mod.filePath);
        }
    }

    public Profile(String filePath){
        this(new File(filePath));
    }

    public Mods getMods(){
        return this.mods;
    }

    private ModInfo getModInfo(File file){
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                System.out.println(zipEntry.getName());
                if(zipEntry.getName().endsWith("info.json"))
                    return mapper.readValue(zipFile.getInputStream(zipEntry), ModInfo.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Mod[] getModsForProfile(String name){
        Mods mods = Profile.modMap.get(name);
        if(mods == null) return null;
        return mods.mods;
    }

    public static class ModListJson{
        @JsonProperty
        public Mod[] mods;
    }

    public static class Mods{
        @JsonProperty
        public Mod[] mods;
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
    }

    public static class ModInfo{
        @JsonProperty
        public String name, version, title, author, homepage, description;
        @JsonProperty
        public String[] dependencies;
    }
}
