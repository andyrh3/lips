package Helpers;

import Helpers.JSONmodels.Airport;
import Helpers.JSONmodels.All;
import Helpers.JSONmodels.Country;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static Helpers.FileHelper.*;

public class TCALocationsHelper {

    public TCALocationsHelper() { }

    private static Logger logger = Logger.getLogger(TCALocationsHelper.class);

    private static String rootPath;
    static {
        try {
            rootPath = new File(".").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String allJsonPath = rootPath + File.separator + "json" + File.separator + "all.json";
    private static String cachePath = rootPath + File.separator + "json" + File.separator + "cache" + File.separator;

    private static String readFile(String inputPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputPath)) {
            return IOUtils.toString(fis, String.valueOf(Charset.defaultCharset()));
        }
    }

    private static void writeFile(String outputPath, String data) throws IOException {
        try (FileOutputStream fw = new FileOutputStream(outputPath)) {
            fw.write(data.getBytes());
        }
    }

    private ObjectMapper jacksonMapper = new ObjectMapper();

    private All all;
    {
        File f = new File(allJsonPath);
        LocalDateTime filterFileLastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
        JSONObject json;
        if(!f.exists() || filterFileLastModified.isAfter(LocalDateTime.now().plusMonths(1)) ) {
            logger.info("Attempting to fetch and cache all.json location data from www.thomascookairlines.co.uk ...");
            try {
                //json = new JSONObject(IOUtils.toString(new URL("https://www.condor.com/tca/rest/de/locations/standard/all").openStream(), "UTF-8"));
                json = new JSONObject(IOUtils.toString(new URL("https://www.thomascookairlines.com/tca/rest/tcauk/en/locations/standard/all").openStream(), "UTF-8"));
                writeFile(allJsonPath, json.toString());
            }catch (IOException | JSONException ioe){
                ioe.printStackTrace();
            }
        }

        try {
            logger.info("Fetching all.json location data from local cache!!!");
            all = jacksonMapper.readValue(readFile(allJsonPath), All.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

/*    public List<String> getTcaAirportCodes() {
        return this.all.getData().getCountries().stream().flatMap(country -> country.getAirports().stream()).map(Airport::getCode).sorted().collect(Collectors.toList());
    }
*/
    public Map<String, String>  getAirportCodesToNames() {
        return this.all.getData().getCountries().stream().flatMap(country -> country.getAirports().stream()).collect(Collectors.toMap(Airport::getCode, Airport::getName));
    }

    private List<Country> getTcaUKLocations() {
        return this.all.getData().getCountries().stream().filter(country -> country.getCode().equals("GB")).collect(Collectors.toList());
    }

    private List<String> getTcaOSAirports() {
        return this.all.getData().getCountries().stream().filter(country -> !country.getCode().equals("GB")).flatMap(country -> country.getAirports().stream()).map(Airport::getCode).sorted().collect(Collectors.toList());
    }

    public LinkedHashMap<String, List<String>> getTcaUkRoutes() {
        String filePathString = cachePath + "routes.txt";
        LinkedHashMap<String, List<String>> tcaUkRoutes;
        File f = new File(filePathString);
        LocalDateTime routeFileLastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
        if(!f.exists() || routeFileLastModified.isAfter(LocalDateTime.now().plusMonths(1)) ) {
            //refresh the routes.json cache file if a month old
            logger.info("Refreshing TCA UK routing data as it was last updated " + routeFileLastModified + " - please be patient this may take a while! ...");
            tcaUkRoutes = buildTcaUkRoutes();
            writeObjectToFile(tcaUkRoutes,f);
        }
        else{
            logger.info("Fetching TCA UK routes.json data from local cache!!!");
            tcaUkRoutes = (LinkedHashMap<String, List<String>>) readObjectFromFile(f);
        }
        return tcaUkRoutes;
    }

    private LinkedHashMap<String, List<String>>  buildTcaUkRoutes() {
        LinkedHashMap<String, List<String>> routeList = new LinkedHashMap<>();
        getTcaOSAirports().forEach(osAirportCode -> {
            int[] bits = getLocationBits(osAirportCode);
            if(bits!=null) {
                ArrayList<String> routes = new ArrayList<>();
                AtomicInteger locationIdx = new AtomicInteger(-1);
                getTcaUKLocations().forEach(country -> {
                    locationIdx.getAndIncrement();
                    //Collect valid country airports
                    country.getAirports().forEach(airport -> {
                        locationIdx.getAndIncrement();
                        if (bits[(locationIdx.get())] == 1) {
                            routes.add(airport.getCode());
                        }
                    });
                    //Collect valid region airports
                    country.getRegions().forEach(region -> {
                        locationIdx.getAndIncrement();
                        region.getAirports().forEach(airport -> {
                            locationIdx.getAndIncrement();
                            if (bits[(locationIdx.get())] == 1) {
                                routes.add(airport.getCode());
                            }
                        });
                    });
                });
                //Filter out destinations that do not have any origin airports
                if(routes.size()>0) {
                    routeList.put(osAirportCode, routes.stream().distinct().sorted().collect(Collectors.toList()));
                }
            }
        });
        return routeList;
    }

    private static int[] getLocationBits(String airportCode){
        //Get routing bits for passed airportCode from cached file if exists else from Thomas Cook Airlines website and then cache to file.
        String filePathString = cachePath + "filters" + File.separator + airportCode + "-origins.json";
        File f = new File(filePathString);
        LocalDateTime filterFileLastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
        JSONObject json = null;
        if(!f.exists() || filterFileLastModified.isAfter(LocalDateTime.now().plusMonths(1)) ) {
            try {
                //json = new JSONObject(IOUtils.toString(new URL("https://www.condor.com/tca/rest/de/locations/standard/compact?origin=&destination=" + airportCode + "&trip=oneway,roundtrip").openStream(), "UTF-8"));
                json = new JSONObject(IOUtils.toString(new URL("https://www.thomascookairlines.com/tca/rest/tcauk/en/locations/standard/compact?origin=&destination=" + airportCode + "&trip=oneway,roundtrip").openStream(), "UTF-8"));
                writeFile(filePathString, json.toString());
                logger.info("Caching filter bits for " + airportCode + " to file:" + filePathString);
            }catch (IOException | JSONException ioe){
                ioe.printStackTrace();
            }
        }else{
            json = getJson(f);
        }
        String operationFlags = null;
        try {
            operationFlags = json != null ? json.getJSONObject("data").getJSONObject("outbound").getJSONObject("origin").getString("filter") : null;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(operationFlags!=null) {
            return CharBuffer.wrap(operationFlags).chars().map(i -> Integer.parseInt(Character.toString((char) i))).toArray();
        }
        return null;
    }

    public static JSONObject getJson(File f) {
        InputStream is;
        JSONObject json = null;
        try {
            is = new FileInputStream(f);
            String jsonContent = IOUtils.toString(is, "UTF-8");
            json = new JSONObject(jsonContent);
        }catch (IOException | JSONException ioe){
            ioe.printStackTrace();
        }
        return json;
    }

    public static LinkedHashMap<String, List<String>> mergeRoutes(LinkedHashMap<String, List<String>> routes, LinkedHashMap<String, List<String>> mergedRoutes) {
        for (Map.Entry<String, List<String>> entry : routes.entrySet() ) {
            //System.out.println("KEY:" + entry.getKey() + "::" + entry.getValue());
            if(!mergedRoutes.containsKey(entry.getKey())){
                //Bible destination is not airline so add it and it's origin airports
                mergedRoutes.put(entry.getKey(), entry.getValue());
            }else{
                //Add bible origins to the existing destination
                List<String> tcaUKRoutesDestinationOrigins = mergedRoutes.get(entry.getKey());
                for (String originAirportCode : entry.getValue()){
                    if(!tcaUKRoutesDestinationOrigins.contains(originAirportCode)){
                        tcaUKRoutesDestinationOrigins.add(originAirportCode);
                    }
                }
                tcaUKRoutesDestinationOrigins.sort(String.CASE_INSENSITIVE_ORDER);
            }
        }
        return mergedRoutes;
    }

    public static LinkedHashMap<String, List<String>> charteredRoutes(LinkedHashMap<String, List<String>> bibleRoutes, LinkedHashMap<String, List<String>> tcaRoutes) {
        LinkedHashMap<String, List<String>> charteredRoutes = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : tcaRoutes.entrySet() ) {
            if(!bibleRoutes.containsKey(entry.getKey())){
                charteredRoutes.put(entry.getKey(),entry.getValue());
            }else {
                List<String> destinationOrigins = new ArrayList<>();
                for (String originAirportCode : entry.getValue()) {
                    if (!bibleRoutes.get(entry.getKey()).contains(originAirportCode)){
                        destinationOrigins.add(originAirportCode);
                    }
                }
                if(destinationOrigins.size()>0){
                    destinationOrigins.sort(String.CASE_INSENSITIVE_ORDER);
                    charteredRoutes.put(entry.getKey(),destinationOrigins);
                }
            }
        }
        return charteredRoutes;
    }

}
