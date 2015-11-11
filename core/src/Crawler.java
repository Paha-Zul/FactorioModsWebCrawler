import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Paha on 11/10/2015.
 * Crawls a website for info.
 */
public class Crawler {

    private static volatile String[] filterNames;
    private static volatile ArrayList<ArrayList<String[]>> modInfos = new ArrayList<>();

    public static void main(String[] args) throws SQLException, IOException {
        processPage("http://www.factoriomods.com/", false);
        System.out.println("done");
    }

    private static ArrayList<ArrayList<String[]>> processPage(String url, boolean multi){
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
                    Runnable task2 = () -> getModsInFilter(filter, k);
                    threads[i] = new Thread(task2);
                    threads[i].start();
                }else{
                    getModsInFilter(filter, i);
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

        printToFile(filterNames, modInfos);

        return modInfos;
    }

    private static void getModsInFilter(Element filter, int index){
        String filterURL = filter.child(0).attr("abs:href"); //Get the absolute address for the filter.
        filterNames[index] = filter.child(0).text(); //Get the filter name "ie: science"
        modInfos.add(getModInfo(filterURL)); //Get the mod info from the web address.
    }

    private static ArrayList<String[]> getModInfo(String url){
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
                    infoString[0] = title.text();
                }

                //Get the last time it was updated
                Elements modInfo = mod.select(".mod-info");

                //For each .mod-info class, get some vital info.
                for(Element info : modInfo) {
                    Elements dates = info.getElementsByAttribute("datetime");
                    infoString[1] = "N/A";
                    for(Element date : dates)
                        infoString[1] = date.text();

                    Element version = info.child(2);
                    infoString[2] = "N/A";
                    if(version.children().size() > 0) infoString[2] = version.child(0).text(); //Version

                    Elements authors = info.getElementsByAttribute("href");
                    infoString[3] = "N/A";
                    for(Element author : authors)
                        if(author.attr("href").contains("authors")) {
                            infoString[3] = author.text();
                            break;
                        }
                }

                names.add(infoString);
                i++;
            }

            Elements nextButtons = doc.select("[rel=next"); //Get all the 'mod' elements
            for(Element next : nextButtons){
                if (next.text().contains("Next"))
                    names.addAll(getModInfo(next.attr("abs:href")));
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
