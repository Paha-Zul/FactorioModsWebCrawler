import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Paha on 11/10/2015.
 * Crawls a website for info. Cool.
 */
public class Crawler {
    public static final int NAME=0, DATE=1, VERSION=2, AUTHOR=3;

    public enum ModInfo {
        NAME(0), DATE(1), VERSION(2), AUTHOR(3);

        private final int value;
        private ModInfo(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum ModVersionInfo {
        VERSION(0), FACT_VERSION(1), DOWNLOAD(2), INSTALL(3), RELEASE(4);

        private final int value;
        private ModVersionInfo(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static String websiteURL = "http://www.factoriomods.com";
    private static volatile String[] filterNames;
    private static volatile ArrayList<ArrayList<String[]>> modInfos = new ArrayList<>();
    private static volatile ArrayList<String[]> specificFilterModsInfo = new ArrayList<>();
    private static volatile String[][] modVersionInfo;

    public static void main(String[] args){
        //processPage("http://www.factoriomods.com/", false);
        //processFilter("http://www.factoriomods.com/recently-updated", 1);
        readVersionInfoOfMod("Ore_Expansion");
    }

    public static ArrayList<ArrayList<String[]>> processPage(String url, boolean multi){
        Document doc = null;
        int i=0;
        Thread[] threads = null;

        try {
            doc = Jsoup.connect(url).timeout(1000).get();
            Elements filters = doc.select(".category-filter"); //Get all the filters.
            filterNames = new String[filters.size()];

            if(multi)threads = new Thread[filters.size()];

            //For each filter, let's dive into the mods.
            for(Element filter : filters){
                //If we are trying to multithread, then call it on a thread
                if(multi) {
                    final int k = i;
                    Runnable task2 = () -> getModsInFilter(filter, k, 1);
                    threads[i] = new Thread(task2);
                    threads[i].start();
                }else{
                    getModsInFilter(filter, i, 1);
                }

                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(threads != null) {
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //printToFile(filterNames, modInfos);

        return modInfos;
    }

    public static ArrayList<String[]> processFilter(String URL, int pages){
        specificFilterModsInfo = getListOfModInfo(URL, pages);
        return specificFilterModsInfo;
    }

    public static ArrayList<String[]> getSpecificFilterModsInfo(){
        return specificFilterModsInfo;
    }

    public static String[][] getModVersionInfo(){
        return modVersionInfo;
    }

    /**
     * Searches the site for the mod with 'modName' and attempts to gather its version information,
     * which will be stored in modVersionInfo
     * @param modName
     */
    public static void readVersionInfoOfMod(String modName){
        int magic = 5;
        String[][] modInfo = null;
        modName = modName.replaceAll("(\\p{Ll})(\\p{Lu})","$1 $2");
        String searchPath = websiteURL + "/recently-updated?v=&q=" + modName.replaceAll(" ", "+");
        Document doc = null;
        modVersionInfo = null;

        try {
            doc = Jsoup.connect(searchPath).timeout(1000).get(); //Get the search page
            Element modTitle = doc.select(".mod-title").first(); //Get the mod titles.
            if(modTitle == null) return;

            Element modLink = modTitle.getElementsByAttribute("href").first(); //Get the mod link
            Document modDoc = Jsoup.connect(modLink.attr("abs:href")).timeout(1000).get(); //Get the mod page
            Element table = modDoc.select(".mod-downloads-table").first(); //Get the mod titles.
            Elements cols = table.select("td"); //Get the columns
            Iterator<Element> colIter = table.select("td").iterator(); //Get the iterator
            modInfo = new String[cols.size()/magic][magic]; //Make the string array.
            int i=0; //counter

            while(colIter.hasNext()){
                Element next = colIter.next();
                if(i%magic == 2){
                    modInfo[i/magic][i%magic] = next.getElementsByAttribute("href").first().attr("href");
                }else if(i%magic == 3){
                    modInfo[i/magic][i%magic] = "Install";
                }else
                    modInfo[i/magic][i%magic] = next.text();
                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        modVersionInfo = modInfo;
    }

    private static void getModsInFilter(Element filter, int index, int pages){
        String filterURL = filter.child(0).attr("abs:href"); //Get the absolute address for the filter.
        filterNames[index] = filter.child(0).text(); //Get the filter name "ie: science"
        modInfos.add(getListOfModInfo(filterURL, pages)); //Get the mod info from the web address.
    }

    /**
     * Gets the mod info of all mods in a category which is dictated by the URL passed in.
     * @param url The URL of the category.
     * @param pages The number of pages we should search into.
     * @return An Arraylist of an array of info.
     */
    private static ArrayList<String[]> getListOfModInfo(String url, int pages){
        Document doc;
        ArrayList<String[]> names = new ArrayList<>();
        int i = 0;

        try {
            System.out.println("Trying to get info from "+url);
            doc = Jsoup.connect(url).timeout(10000).get(); //Get the doc at the URL
            Elements mods = doc.select(".mod"); //Get all the 'mod' elements

            //For each mod element, get the .mod-title class.
            for(Element mod : mods){
                //Get the title
                Elements modTitle = mod.select(".mod-title");
                String[] infoString = new String[4];

                //For each .mod-title class, get the href link and get its text. This will be the name of the mod.
                for(Element title : modTitle) {
                    infoString[ModInfo.NAME.getValue()] = title.text();
                }

                //Get the last time it was updated
                Elements modInfo = mod.select(".mod-info");

                //For each .mod-info class, get some vital info.
                for(Element info : modInfo) {
                    Elements dates = info.getElementsByAttribute("datetime");
                    infoString[ModInfo.DATE.getValue()] = "N/A";
                    for(Element date : dates)
                        infoString[ModInfo.DATE.getValue()] = date.text();

                    Element version = info.child(2);
                    infoString[ModInfo.VERSION.getValue()] = "N/A";
                    if(version.children().size() > 0) infoString[ModInfo.VERSION.getValue()] = version.child(0).text(); //Version

                    Elements authors = info.getElementsByAttribute("href");
                    infoString[ModInfo.AUTHOR.getValue()] = "N/A";
                    for(Element author : authors)
                        if(author.attr("href").contains("authors")) {
                            infoString[ModInfo.AUTHOR.getValue()] = author.text();
                            break;
                        }
                }

                names.add(infoString);
                i++;
            }

            /*
                This area will take us to the next page if one exists and our 'pages' variable allows us to.
             */

            Elements nextButtons = doc.select("[rel=next"); //Get all the 'mod' elements
            for(Element next : nextButtons){
                if (next.text().contains("Next")) {
                    String nextPage = next.attr("abs:href");
                    String sub = nextPage.substring(nextPage.lastIndexOf('=')+1);
                    if(Integer.parseInt(sub) < pages)
                        names.addAll(getListOfModInfo(next.attr("abs:href"), pages));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return names;
    }

    private static void printToFile(String[] filterNames, ArrayList<ArrayList<String[]>> modInfos){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("modlist.txt", "UTF-8");
            int i=0;

            //For each filter, print all the mods.
            for(String filterName : filterNames){
                int n=0; //The counter for the current mod
                writer.append(filterName).append("\n"); //First append the name and newline.
                for(String[] modInfo : modInfos.get(i)) { //For each mod, print the info!
                    writer.append("\t").append(n+") ").append(modInfo[0]).append(", ").append(modInfo[1]).append(", ").append(modInfo[2]).append(", ").append(modInfo[3]).append("\n");
                    n++;
                }

                i++;
            }

            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
