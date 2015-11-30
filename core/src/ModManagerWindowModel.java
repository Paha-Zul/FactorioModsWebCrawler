import java.util.HashMap;

/**
 * Created by Paha on 11/29/2015.
 */
public class ModManagerWindowModel {
    private String factorioDataPath;
    private String factorioModPath;
    private String factorioExecutablePath;
    private String factorioModManagerPath;

    private final String factorioModManagerFolder = "fm";

    private String currentlySelectedModName = null;
    private String currentlySelectedProfile = null;

    private HashMap<String, Profile.ModListJson> modListMap = new HashMap<>();
    private HashMap<String, Profile> modProfileMap = new HashMap<>();

    private ModManagerWindow window;
    private ModManagerWindowController controller;

    public ModManagerWindowModel(ModManagerWindow window){
        this.window = window;
    }

    public void setModManagerWindowController(ModManagerWindowController controller){
        this.controller = controller;
    }

    public void setPaths(String factorioDataPath, String factorioModPath, String factorioExecutablePath, String factorioModManagerPath){
        this.factorioDataPath = factorioDataPath;
        this.factorioModPath = factorioModPath;
        this.factorioExecutablePath = factorioExecutablePath;
        this.factorioModManagerPath = factorioModManagerPath;
    }


    public String getFactorioDataPath() {
        return factorioDataPath;
    }

    public String getFactorioModPath() {
        return factorioModPath;
    }

    public String getFactorioExecutablePath() {
        return factorioExecutablePath;
    }

    public String getFactorioModManagerPath() {
        return factorioModManagerPath;
    }

    public String getFactorioModManagerFolder() {
        return factorioModManagerFolder;
    }

    public HashMap<String, Profile.ModListJson> getModListMap() {
        return modListMap;
    }

    public HashMap<String, Profile> getModProfileMap() {
        return modProfileMap;
    }

    public String getCurrentlySelectedModName() {
        return currentlySelectedModName;
    }

    public String getCurrentlySelectedProfile() {
        return currentlySelectedProfile;
    }

    public void setCurrentlySelectedProfile(String currentlySelectedProfile) {
        this.currentlySelectedProfile = currentlySelectedProfile;
    }

    public void setCurrentlySelectedModName(String currentlySelectedModName) {
        this.currentlySelectedModName = currentlySelectedModName;
    }
}
