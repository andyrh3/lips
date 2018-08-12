package LIPS;

import API.models.Airport;
import API.models.Month;
import API.models.Schedule;
import API.models.wizzair.Payload;
import API.models.wizzair.PayloadFlight;
import Helpers.DateHelper;
import Helpers.ExcelHelper;
import Helpers.FileHelper;
import Helpers.TCALocationsHelper;
import LIPS.models.*;
import LIPSin.models.TpOpRoute;
import LIPSin.models.TpOpSeason;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static Helpers.FileHelper.writeObjectToJSONFile;
import static org.apache.http.protocol.HTTP.USER_AGENT;

public class App
{
    private static Logger logger = Logger.getLogger(App.class);
    private static final int SUMMER_START_MONTH = 5;
    private static final int WINTER_START_MONTH = 11;
    private static final int SEASON_MONTH_COUNT = 5;
    private static final DateTimeFormatter timestampDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter excelDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    //private static final DateTimeFormatter lipsInDateFormatter = DateTimeFormatter.ofPattern("d MM yyyy");
    private static final DateTimeFormatter apiDateFormatter = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu");
    private static final String[] weekDayShortName = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private static final String[] weekDayLongName = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
    private static final List<String> allUkAirports = Arrays.asList("ABZ","BFS","BHD","BHX","BOH","BRS","CVT","CWL","DSA","EDI","EMA","EXT","GLA","HUY","LBA","LCY","LDY","LGW","LHR","LPL","LTN","MAN","MME","NCL","NQY","NWI","PIK","SEN","SOU","STN");

    private static String rootPath;
    static {
        try {
            rootPath = new File(".").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String jsonPath = rootPath + File.separator + "json" + File.separator;
    private static String cachePath = jsonPath + "cache" + File.separator;

    private static int weeksToDeparture = 8;

    private static List<String> processAirlines = new ArrayList<>();
    private static List<String> processDestinations = new ArrayList<>();
    private static List<String> processOrigins = new ArrayList<>();

    private static boolean forceAllUkAirports = false;

	/*    */
	//TCX airport mappings
    private static final Map<String, String> airportMapping = new TCALocationsHelper().getAirportCodesToNames();

    //Register Airlines
    private static LinkedHashMap<String, OpAirline> airlines = new LinkedHashMap<>();
    static {
        /*
        - Jet2 EXS DONE
        - Ryanair RYR DONE
        - TUI TOM DONE
        - Flybe BEE DONE
        - Easyjet EZY DONE
        - Wizz Air
        - Blue Air
        - Primera Air
        -
        */
        airlines.put("EZY", new OpAirline("Easyjet","EZY","http://www.easyjet.com/ejcms/cache15m/api/routedates/get/"));
        airlines.put("EXS", new OpAirline("Jet2", "EXS", "https://reservations.jet2.com/jet2.Reservations.web.portal/StandardSearchPageSmall.aspx/getFlyDates"));
        airlines.put("RYR", new OpAirline("Ryanair","RYR","https://api.ryanair.com/timetable/3/schedules/"));
        airlines.put("TOM", new OpAirline("Thomson","TOM","https://www.tui.co.uk/searchpanel/traveldates/fo"));
        airlines.put("BEE", new OpAirline("Flybe","BEE","https://www.flybe.com/timetableClassic/timetable.jsp"));
        airlines.put("BMS", new OpAirline("Blue Air","BMS","https://webapi.blueairweb.com/api/RetrieveSchedule"));
        airlines.put("WZZ", new OpAirline("Wizz Air","WZZ","https://be.wizzair.com/8.2.0/Api/search/timetable"));
        airlines.put("PRI", new OpAirline("Primera Air","PRI","https://primeraair.co.uk/api/data"));
        airlines.put("TCX", new OpAirline("Thomas Cook","TCX",""));

        //Set airline specific date formats
        airlines.get("TOM").setDateFormatter("dd-MM-yyyy");
        airlines.get("BEE").setDateFormatter("dd-MMM-yy");
    }

    private static LinkedHashMap<String, Schedule> airlineSchedules = new LinkedHashMap<>();

    public static void main(String[] args) {

        System.out.println("####### Application: LIPS Opportunity Finder");
        System.out.println("####### Version: 1.0.1");
        System.out.println("####### Author: Andrew Harrison");
        System.out.println("####### Contact: andrew.harrison@thomascook.com");
        System.out.println("####### Updated: 14 January 2018");
        System.out.println();
        System.out.println();

        //Example args: --weekstodept 8 --dests PMI --origs STN --airlines PRI

        String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.properties";
        PropertyConfigurator.configure(log4jConfigFile);

        LinkedHashMap<String, String> processedArgs = processArgs(args);

        //Used to calculate the TCX compete OpRanges SET THIS TO 6 WEEKS FOR PRODUCTION - pass in as runtime parameter?
        if(processedArgs.containsKey("weekstodept")){
            weeksToDeparture = Integer.parseInt(processedArgs.get("weekstodept"));
        }
        if(processedArgs.containsKey("dests")){
            processDestinations.addAll(Arrays.stream(processedArgs.get("dests").split(",")).collect(Collectors.toList()));
        }
        if(processedArgs.containsKey("origs")){
            processOrigins.addAll(Arrays.stream(processedArgs.get("origs").split(",")).collect(Collectors.toList()));
        }
        if(processedArgs.containsKey("airlines")){
            processAirlines.addAll(Arrays.stream(processedArgs.get("airlines").split(",")).collect(Collectors.toList()));
        }

        // create a scanner so we can read the command-line input
        Scanner scanner = new Scanner(System.in);

        //  prompt for weeks to departure
        System.out.print("Run everything - really? This will take some time. (N/Y)... ");
        String runEverything = scanner.next();

        if(runEverything.equalsIgnoreCase("N")) {

            //  prompt for weeks to departure
            System.out.print("Weeks to departure is defaulted to 8 weeks. Change? (N/Y)... ");
            String updateWTD = scanner.next();

            if (updateWTD.equalsIgnoreCase("Y")) {
                System.out.print("Enter weeks to departure: ");
                weeksToDeparture = scanner.nextInt();
            }

            // prompt restriction on airlines
            System.out.print("Restrict airline? (N/Y)... ");
            String restrictAirlines = scanner.next();

            if (restrictAirlines.equalsIgnoreCase("Y")) {
                System.out.print("Restrict airlines by comma separated airlines codes e.g. EZY,EXS,RYR,TOM,BEE or hit enter to process all...");
                processAirlines.addAll(Arrays.stream(scanner.next().split(",")).distinct().collect(Collectors.toList()));
            }

            // prompt restriction on destinations
            System.out.print("Restrict destinations? (N/Y)... ");
            String restrictDestinations = scanner.next();

            if (restrictDestinations.equalsIgnoreCase("Y")) {
                System.out.print("Restrict destinations by comma separated airport codes e.g. AYT,DLM or hit enter to process all...");
                processDestinations.addAll(Arrays.stream(scanner.next().split(",")).distinct().collect(Collectors.toList()));
            }

            // prompt restriction on destinations
            System.out.print("Restrict origins? (N/Y)... ");
            String restrictOrigins = scanner.next();

            if (restrictOrigins.equalsIgnoreCase("Y")) {
                System.out.print("Restrict origins by comma separated airport codes e.g. MAN,LGW or hit enter to process all...");
                processOrigins.addAll(Arrays.stream(scanner.next().split(",")).distinct().collect(Collectors.toList()));
            }

            // force all uk origins against merged destinations
            System.out.print("Force UK origins (answer Y when you want to run all 30 UK origins)? (N/Y)... ");
            String forceAllUkOrigins = scanner.next();

            if (forceAllUkOrigins.equalsIgnoreCase("Y")) {
                forceAllUkAirports = true;
            }

        }

        scanner.close();

        System.out.print("");
        System.out.print("");
        System.out.print("########### PROGRAM RUNNING... ###########");
        System.out.print("");
        System.out.print("");

        LinkedHashMap<String, AbstractMap.SimpleEntry> bibleWorkbooks = new LinkedHashMap<>();

        //Fetch bible workbooks
        fetchBibleWorkbooks(bibleWorkbooks);

        //Get TCA route list from cache file or build it if stale
        LinkedHashMap<String, List<String>> tcaUkRoutes = new TCALocationsHelper().getTcaUkRoutes();
        writeObjectToJSONFile(tcaUkRoutes, new File(jsonPath + "tca-routes.json"));
        //writeObjectToJSONFile(tcaUkRoutes, new File(jsonPath + "condor-es-routes.json"));

        //Get FORCED routes - these are routes that TCA/Condor and Tour Ops do not service but are served by 3rd party airlines and so should be processed.
        String filePathString = jsonPath + "forced-routes.json";
        ObjectMapper mapper = new ObjectMapper();
        File f = new File(filePathString);
        LinkedHashMap<String, List<String>> forcedRoutesFromJsonFile = null;
        try {
            logger.info("Fetching forced-routes.json data from local cache!!!");
            forcedRoutesFromJsonFile = mapper.readValue(f, new TypeReference<Map<String, List<String>>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        LinkedHashMap<String, List<String>> forcedRoutes = forcedRoutesFromJsonFile;

        //Main data structure for building LIPS excel opportunities
        LinkedHashMap<String, OpSeason> LIPS = new LinkedHashMap<>();

        //Main data structure for LIPSin excel
        ArrayList<TpOpSeason> LIPSin;

        //Iterate through work workbook files
        //Iterator<Map.Entry<String, String>> workbookIterator = bibles.entrySet().iterator();


        bibleWorkbooks.forEach((String workbookSeason, AbstractMap.SimpleEntry workbook) -> {
            logger.info("Processing " + workbook.getKey());
            String workbookName = workbook.getKey().toString();
            int seasonStartMonth = (workbookSeason.contains("S"))?SUMMER_START_MONTH:WINTER_START_MONTH;
            int seasonStartYear = Integer.parseInt("20" + workbookSeason.replaceAll("\\D+",""));
            LocalDate seasonStartDate = LocalDate.of(seasonStartYear, seasonStartMonth, 1);
            LocalDate seasonEndDate = seasonStartDate.plusMonths(SEASON_MONTH_COUNT);
            seasonEndDate = seasonEndDate.withDayOfMonth(seasonEndDate.lengthOfMonth());
            OpSeason opSeason = new OpSeason(workbookName, workbookSeason, Date.valueOf(seasonStartDate), Date.valueOf(seasonEndDate));
            LIPS.put(workbookSeason, opSeason);

            //Bible Routes some not serviced by TCA
            LinkedHashMap<String, List<String>> bibleRoutes = fetchBibleRoutes(workbookName);
            writeObjectToJSONFile(bibleRoutes, new File(jsonPath + "bible-routes.json"));

            LinkedHashMap<String, List<String>> mergedRoutes  = TCALocationsHelper.mergeRoutes(bibleRoutes, tcaUkRoutes);
            mergedRoutes = TCALocationsHelper.mergeRoutes(mergedRoutes, forcedRoutes);
            writeObjectToJSONFile(mergedRoutes, new File(jsonPath + "merged-routes.json"));

            //TCA Routes not serviced by Tour Ops
            LinkedHashMap<String, List<String>> charteredRoutes  = TCALocationsHelper.charteredRoutes(bibleRoutes, tcaUkRoutes);
            writeObjectToJSONFile(charteredRoutes, new File(jsonPath + "chartered-routes.json"));

            //Force all UK departures
            if(forceAllUkAirports){
                for(Map.Entry<String, List<String>> route : mergedRoutes.entrySet()){
                    route.getValue().clear();
                    route.getValue().addAll(allUkAirports);
                }
            }

            /*Read in the workbook here instead of multiple times within the mergedRoutes loop!!*/
            FileInputStream fis = null;
            XSSFWorkbook wb = null;
            String applicationPath = "";
            //Get Application path
            try{
                applicationPath = new File(".").getCanonicalPath();
            }catch(IOException ioe){
                ioe.printStackTrace();
            }
            try {
                fis = new FileInputStream(new File(applicationPath + File.separator + "excels" + File.separator + workbookName));
                wb = new XSSFWorkbook(fis);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for ( Map.Entry<String, List<String>> entry : mergedRoutes.entrySet()) {
                String destAirportCode = entry.getKey();
                //RESTRICT destinations!!!!!!!
                if(processDestinations.isEmpty() || processDestinations.contains(destAirportCode)) {
                    OpAirport opDestAirport = new OpAirport(destAirportCode, "back");
                    opSeason.addAirport(opDestAirport);
                    for (String origAirportCode : entry.getValue()){
                        //RESTRICT destination origins!!!!!!!
                        if(processOrigins.isEmpty() || processOrigins.contains(origAirportCode)) {
                            logger.info(">>>>>>>>> PROCESSING ROUTE: " + origAirportCode + "-" + destAirportCode + " <<<<<<<<");
                            OpAirport opOrigAirport = new OpAirport(origAirportCode, "out");
                            opDestAirport.addAirport(opOrigAirport);
                            //Fetch all airline schedules for season and route!!!!
                            getRouteAirlineSchedules(airlines, workbookSeason, opSeason, origAirportCode, destAirportCode);
                            if (wb != null) {
                                traverseFlightBible(wb, opSeason, opOrigAirport, opDestAirport);
                            }
                            //####### Process NON-COMPETE OpDay!!!! #######
                            setAirlineNonCompeteOperatingDays(opOrigAirport, opSeason);
                        }
                    }
                }
            }

        });
        //Output formatted JSON of object structure
        writeObjectToJSONFile(LIPS, new File(jsonPath + "lips.json"));

        LIPSin = buildLIPSinData(LIPS, airlines);
        writeObjectToJSONFile(LIPSin, new File(jsonPath + "lipsin.json"));

        logger.info("Completed LIPSin data build. Generating excel file...");

        generateLIPSinFile(LIPSin);
        //generateLIPSFiles(LIPS);
    }

    private static LinkedHashMap<String, String> processArgs(String[] args){
        LinkedHashMap<String, String> argsMap = new LinkedHashMap<>();
        String currentArgKey = null;
        for(String arg : args){
            if(arg.contains("-") || arg.contains("--")){
                currentArgKey = arg.replaceAll("-","").trim().toLowerCase();
            }else{
                if(currentArgKey!=null) {
                    argsMap.put(currentArgKey, arg);
                }
            }
        }
        return argsMap;
    }

    private static void fetchBibleWorkbooks(LinkedHashMap<String, AbstractMap.SimpleEntry> bibleWorkbooks){
        //Stream in Bible workbooks and store them
        //File names need to begin with 3 letter seasons i.e. W18, S19
        FileInputStream fis = null;
        try {
            String biblesInPath = new File(".").getCanonicalPath() + File.separator + "excels";
            File biblesIn = new File(biblesInPath);
            for (File file : Objects.requireNonNull(biblesIn.listFiles())) {
                if(file.getName().endsWith(".xlsx")){
                    if(!file.isHidden()) {
                        fis = new FileInputStream(file);
                        bibleWorkbooks.put(file.getName().substring(0, 3), new AbstractMap.SimpleEntry<>(file.getName(),new XSSFWorkbook(fis)) );
                    }
                }
            }
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
        try {
            if(fis != null) {
                fis.close();
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private static void getRouteAirlineSchedules(LinkedHashMap<String, OpAirline> airlines, String season, OpSeason opSeason, String origAirportCode, String destAirportCode){
        //Reset the airlineSchedules to remove previous route schedule data!!!
        airlineSchedules.clear();
        for(Map.Entry<String, OpAirline> airline : airlines.entrySet()){
            if(processAirlines.isEmpty() || processAirlines.contains(airline.getKey())) {
                String filePathString = cachePath + "schedules" + File.separator + season + "-" + airline.getKey() + "-" + origAirportCode + "-" + destAirportCode + ".txt";
                File f = new File(filePathString);
                filePathString = cachePath + "schedules" + File.separator + season + "-" + airline.getKey() + "-" + origAirportCode + "-" + destAirportCode + ".json";
                File j = new File(filePathString);
                LocalDateTime fileLastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
                if (!f.exists() || fileLastModified.isAfter(LocalDateTime.now().plusMonths(1))) {
                    //refresh the schedules cache file if a month old
                    logger.info("Caching schedule data for: " + airline.getValue().getName() + " schedule for " + season + "-" + origAirportCode + "-" + destAirportCode + " as it was last modified: " + fileLastModified + " - please wait! ...");
                    ArrayList<NameValuePair> airlineParams = new ArrayList<>();
                    URL airlineUrl;
                    Schedule schedule;
                    LocalDate currentDate;
                    //Fetch airline schedules for route
                    switch (airline.getKey()) {
                        //Fetch Ryanair schedule
                        case "RYR":
                            logger.info("Capturing " + airline.getValue().getName() + " schedule for " + season + "-" + origAirportCode + "-" + destAirportCode);
                            schedule = new Schedule();
                            schedule.ScheduleStarted = true;
                            schedule.ScheduleEnded = false;
                            schedule.ScheduleUnknown = false;
                            currentDate = LocalDate.now().withDayOfMonth(1);
                            LocalDate seasonEndDate = DateHelper.convertDateToLocalDate(opSeason.getEndDate());
                            airlineParams.add(new BasicNameValuePair("destination", destAirportCode));
                            airlineParams.add(new BasicNameValuePair("origin", origAirportCode));
                            while (currentDate.isBefore(seasonEndDate) || currentDate.isEqual(seasonEndDate)) {
                                //https://api.ryanair.com/timetable/3/schedules/ALC/LGW/years/2018/months/2
                                List<String> existingParams = airlineParams.stream().map(NameValuePair::getName).collect(Collectors.toList());
                                if (existingParams.contains("years")) {
                                    airlineParams.set(existingParams.indexOf("years"), new BasicNameValuePair("years", Integer.toString(currentDate.getYear())));
                                } else {
                                    airlineParams.add(new BasicNameValuePair("years", Integer.toString(currentDate.getYear())));

                                }
                                if (existingParams.contains("months")) {
                                    airlineParams.set(existingParams.indexOf("months"), new BasicNameValuePair("months", Integer.toString(currentDate.getMonthValue())));
                                } else {
                                    airlineParams.add(new BasicNameValuePair("months", Integer.toString(currentDate.getMonthValue())));
                                }
                                try {
                                    JsonObject json = airlineRouteScheduleJsonRequest(airline.getValue(), airlineParams, null);
                                    Month month = new Month(currentDate.getYear(), currentDate.getMonthValue());
                                    schedule.addMonth(month);
                                    //json.get("days").getAsJsonArray().iterator().forEachRemaining(je -> je.getAsJsonObject().get());
                                    List<String> days = new ArrayList<>();
                                    json.get("days").getAsJsonArray().forEach(jsonElement -> days.add(jsonElement.getAsJsonObject().get("day").getAsString()));
                                    month.FlightDates.addAll(days.stream().map(Integer::parseInt).sorted().collect(Collectors.toList()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                currentDate = currentDate.plusMonths(1);
                            }
                            FileHelper.writeObjectToFile(schedule, f);
                            FileHelper.writeObjectToJSONFile(schedule, j);
                            airlineSchedules.put(airline.getKey(), schedule);
                            logger.info("Captured " + airline.getValue().getName() + " schedule for " + season + "-" + origAirportCode + "-" + destAirportCode);
                            break;
                        //Fetch EasyJet schedule
                        case "EZY":
                            logger.info("Capturing " + airline.getValue().getName() + " schedule for " + season + "-" + origAirportCode + "-" + destAirportCode);
                            airlineParams.add(new BasicNameValuePair("destinationIata", destAirportCode));
                            airlineParams.add(new BasicNameValuePair("originIata", origAirportCode));
                            try {
                                airlineUrl = airline.getValue().buildApiQueryURL(airlineParams).toURL();
                                ObjectMapper objectMapper = new ObjectMapper();
                                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                schedule = objectMapper.readValue(IOUtils.toString(airlineUrl.openStream(), "UTF-8"), Schedule.class);
                                //Cache schedule to file
                                FileHelper.writeObjectToFile(schedule, f);
                                FileHelper.writeObjectToJSONFile(schedule, j);
                                if (!schedule.ScheduleUnknown) {
                                    airlineSchedules.put(airline.getKey(), schedule);
                                } else {
                                    logger.info(airline.getValue().getName() + " does not operate " + origAirportCode + "-" + destAirportCode + " for season " + season);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        //Fetch Thomson (TUI) schedule
                        case "TOM":
                            logger.info("Capturing " + airline.getValue().getName() + " schedule for " + season + "-" + origAirportCode + "-" + destAirportCode + " for season " + season);
                            airlineParams.add(new BasicNameValuePair("to[]", destAirportCode));
                            airlineParams.add(new BasicNameValuePair("from[]", origAirportCode));
                            try {
                                JsonObject json = airlineRouteScheduleJsonRequest(airline.getValue(), airlineParams, null);
                                //Shoehorn the json response into Easyjet schedule pojo
                                schedule = buildAirlineSchedule(airline.getValue(), json, opSeason);
                                FileHelper.writeObjectToFile(schedule, f);
                                FileHelper.writeObjectToJSONFile(schedule, j);
                                airlineSchedules.put(airline.getKey(), schedule);
                                logger.info("Captured " + airline.getValue().getName() + " schedule for " + season + "-" + origAirportCode + "-" + destAirportCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        /*Fetch Jet2 schedule*/
                        case "EXS":
                            //Do airline scrape as no rest exists
                            //Have to fetch airline stations
                            //Fetch Jet2 schedule - use number index to identify airports - we have stored these in the airline object's airport mappings :-)
                            logger.info("Trying to Capture " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode + " for season " + season);
                            try {
                                //Need to fetch airline airport mapping as JET2 use numbers to identify airports instead of iata codes!!!
                                getAirlineAirportMapping(airline.getValue());
                                airlineParams.add(new BasicNameValuePair("destAirportId", destAirportCode));
                                airlineParams.add(new BasicNameValuePair("origAirportId", origAirportCode));
                                JsonObject json = airlineRouteScheduleJsonRequest(airline.getValue(), airlineParams, null);
                                //Shoehorn the json response into Easyjet schedule pojo
                                //Need to cache data as: key = airline + route pairs value = Schedule??? Too Big vs connectivity to airline website?
                                schedule = buildAirlineSchedule(airline.getValue(), json, opSeason);
                                airlineSchedules.put(airline.getKey(), schedule);
                                logger.info("Captured " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        /*Fetch Primera Air schedule*/
                        case "PRI":
                            //Do airline scrape as no rest exists
                            logger.info("Trying to Capture " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode + " for season " + season);
                            try {
                                //{"method": 3, "wid": 975396, "departure": "KEF", "destination": "BHX", "channel": "ASG"}
                                airlineParams.add(new BasicNameValuePair("destination", destAirportCode));
                                airlineParams.add(new BasicNameValuePair("departure", origAirportCode));
                                airlineParams.add(new BasicNameValuePair("channel", "ASG"));
                                airlineParams.add(new BasicNameValuePair("wid", "975396"));
                                airlineParams.add(new BasicNameValuePair("method", "3"));
                                JsonObject json = airlineRouteScheduleJsonRequest(airline.getValue(), airlineParams, null);
                                //Shoehorn the json response into Easyjet schedule pojo
                                //Need to cache data as: key = airline + route pairs value = Schedule??? Too Big vs connectivity to airline website?
                                schedule = buildAirlineSchedule(airline.getValue(), json, opSeason);
                                FileHelper.writeObjectToFile(schedule, f);
                                FileHelper.writeObjectToJSONFile(schedule, j);
                                airlineSchedules.put(airline.getKey(), schedule);
                                logger.info("Captured " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        /*Fetch Blue Air schedule*/
                        case "BMS":
                            //Do airline scrape as no rest exists
                            logger.info("Trying to Capture " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode + " for season " + season);
                            try {
                                //{"method": 3, "wid": 975396, "departure": "KEF", "destination": "BHX", "channel": "ASG"}
                                airlineParams.add(new BasicNameValuePair("d", destAirportCode));
                                airlineParams.add(new BasicNameValuePair("o", origAirportCode));
                                JsonObject json = airlineRouteScheduleJsonRequest(airline.getValue(), airlineParams, null);
                                //Shoehorn the json response into Easyjet schedule pojo
                                //Need to cache data as: key = airline + route pairs value = Schedule??? Too Big vs connectivity to airline website?
                                schedule = buildAirlineSchedule(airline.getValue(), json, opSeason);
                                FileHelper.writeObjectToFile(schedule, f);
                                FileHelper.writeObjectToJSONFile(schedule, j);
                                airlineSchedules.put(airline.getKey(), schedule);
                                logger.info("Captured " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        /*Fetch Blue Air schedule*/
                        case "WZZ":
                            //Do airline scrape as no rest exists
                            logger.info("Trying to Capture " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode + " for season " + season);
                            try {
                                airlineParams.add(new BasicNameValuePair("destination", destAirportCode));
                                airlineParams.add(new BasicNameValuePair("origin", origAirportCode));

                                LocalDate opSeasonEndDate = DateHelper.convertDateToLocalDate(opSeason.getEndDate());
                                //LocalDate tempLocalDate = LocalDate.parse("31/12/2018", DateTimeFormatter.ofPattern("d/MM/yyyy"));

                                //Wizz Air only returns back 42 days of results so we must start from today and plus days until we reach end of season.
                                LocalDate startDate = LocalDate.now().atStartOfDay().toLocalDate();
                                LocalDate endDate = startDate.plusDays(42);
                                if(endDate.isAfter(opSeasonEndDate)){
                                    endDate = opSeasonEndDate;
                                }

                                ArrayList<LocalDate> wizzLocalDates = new ArrayList<>();

                                while(
                                        ( endDate.isBefore(opSeasonEndDate) ||
                                        endDate.isEqual(opSeasonEndDate) ) &&
                                        ( startDate.isBefore(endDate) ||
                                        startDate.isEqual(endDate) )
                                ){
                                    logger.info(startDate + ":" + endDate);
                                    //Build payload
                                    Payload payload = new Payload();
                                    PayloadFlight payloadFlight = new PayloadFlight(origAirportCode, destAirportCode, startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                                    payload.addFlight(payloadFlight);

                                    //Build JSON payload and then ObjectMapper a request into wzzSchedule Class
                                    JsonObject scheduleJsonResp = airlineRouteScheduleJsonRequest(airline.getValue(), airlineParams, new Gson().toJson(payload));
                                    //JSON from String to Object
                                    API.models.wizzair.Schedule tempSchedule = new ObjectMapper().readValue(scheduleJsonResp.toString(), API.models.wizzair.Schedule.class);

                                    wizzLocalDates.addAll((ArrayList<LocalDate>) tempSchedule.getOutboundFlights().stream().map(flight -> flight.getDepartureDateAsLocalDate()).collect(Collectors.toList()));

                                    if(
                                        ( endDate.plusDays(42).isBefore(opSeasonEndDate) ||
                                        endDate.plusDays(42).isEqual(opSeasonEndDate) )
                                    ){
                                        startDate = endDate.plusDays(1);
                                        endDate = endDate.plusDays(43);
                                    }else{
                                        startDate = endDate.plusDays(1);
                                        endDate = opSeasonEndDate;
                                    }
                                }
                                //Schedule shoehorn
                                schedule = new Schedule();
                                schedule.ScheduleStarted = true;
                                schedule.ScheduleEnded = false;
                                schedule.ScheduleUnknown = false;
                                for(LocalDate localDate : wizzLocalDates){
                                    buildAirlineScheduleMonth(schedule, localDate).FlightDates.add(localDate.getDayOfMonth());
                                }
                                FileHelper.writeObjectToFile(schedule, f);
                                FileHelper.writeObjectToJSONFile(schedule, j);
                                airlineSchedules.put(airline.getKey(), schedule);
                                logger.info("Captured " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case "BEE":
                            //Do airline scrape as no rest exists
                            airlineParams.add(new BasicNameValuePair("selDest", destAirportCode));
                            airlineParams.add(new BasicNameValuePair("selDep", origAirportCode));
                            try {
                                airlineUrl = airline.getValue().buildApiQueryURL(airlineParams).toURL();
                                HttpClient clientGet = HttpClientBuilder.create().build();
                                HttpGet get = new HttpGet(airlineUrl.toString());
                                HttpResponse res = clientGet.execute(get);
                                schedule = new Schedule();
                                schedule.ScheduleStarted = true;
                                schedule.ScheduleEnded = false;
                                schedule.ScheduleUnknown = false;
                                Month month;
                                Document doc = Jsoup.parse(IOUtils.toString(res.getEntity().getContent(), String.valueOf(StandardCharsets.UTF_8)));
                                String selDest = doc.select("select[name=selDest] option[selected]").text();
                                //If selDest is empty then destination is not serviced from the origin
                                Elements datesTableRows = null;
                                if (!selDest.equals("")) {
                                    try {
                                        datesTableRows = doc.select("td[class*=sectionbar2]:contains(" + selDest + ")").first().parent().parent().children();
                                    } catch (IllegalArgumentException iae) {
                                        iae.printStackTrace();
                                    } catch (NullPointerException npe) {
                                        npe.printStackTrace();
                                        logger.error("flybe ERROR: No table schedule found for: " + origAirportCode + "-" + destAirportCode);
                                    }

                                    if (datesTableRows != null) {
                                        logger.info("Capturing " + airline.getValue().getName() + " schedule for " + origAirportCode + "-" + destAirportCode + " for season " + season);
                                        //Search for the table for the destination. If it's not found then no schedule is available
                                        int rowCount = 0;
                                        for (Element datesTableRow : datesTableRows) {
                                            int weekDayCount = 0;
                                            ArrayList<Integer> daysOfWeekForDateRange = new ArrayList<>();
                                            if (rowCount > 2) {
                                                for (Element datesTableRowCol : datesTableRow.select("td[class*=daysTableText]")) {
                                                    if (weekDayCount < 7) {
                                                        //Collect weekdays
                                                        if (!datesTableRowCol.text().equals(".") && !datesTableRowCol.text().trim().equals("")) {
                                                            daysOfWeekForDateRange.add(weekDayCount);
                                                        }
                                                    } else {
                                                        if (daysOfWeekForDateRange.size() > 0) {
                                                            //Collect date Ranges in last column
                                                            LocalDate dateRangeStartDate = LocalDate.parse(datesTableRowCol.text().split(" - ")[0], DateTimeFormatter.ofPattern("dd-MMM-yy"));
                                                            LocalDate dateRangeEndDate = LocalDate.parse(datesTableRowCol.text().split(" - ")[1], DateTimeFormatter.ofPattern("dd-MMM-yy"));
                                                            while (dateRangeStartDate.isBefore(dateRangeEndDate) || dateRangeStartDate.isEqual(dateRangeEndDate)) {
                                                                if (daysOfWeekForDateRange.contains(dateRangeStartDate.getDayOfWeek().ordinal())) {
                                                                    month = schedule.getMonth(dateRangeStartDate.getYear(), dateRangeStartDate.getMonthValue());
                                                                    if (month == null) {
                                                                        month = new Month(dateRangeStartDate.getYear(), dateRangeStartDate.getMonthValue());
                                                                        schedule.addMonth(month);
                                                                    }
                                                                    month.FlightDates.add(dateRangeStartDate.getDayOfMonth());
                                                                }
                                                                dateRangeStartDate = dateRangeStartDate.plusDays(1);
                                                            }
                                                        }
                                                    }
                                                    weekDayCount++;
                                                }
                                            }
                                            rowCount++;
                                        }
                                        //Sort the dates
                                        schedule.Months.forEach(m -> Collections.sort(m.FlightDates));
                                    } else {
                                        logger.info(airline.getValue().getName() + " does not operate " + origAirportCode + "-" + destAirportCode + " for season " + season);
                                        schedule.ScheduleUnknown = true;
                                    }
                                    //Add BEE schedule to airlines
                                    if (!schedule.ScheduleUnknown && schedule.Months.size() > 0) {
                                        FileHelper.writeObjectToFile(schedule, f);
                                        FileHelper.writeObjectToJSONFile(schedule, j);
                                        airlineSchedules.put(airline.getKey(), schedule);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }

                } else {
                    logger.info("Fetching schedule data from local cache for: " + airline.getValue().getName() + " for " + origAirportCode + "-" + destAirportCode);
                    Schedule schedule = (Schedule) FileHelper.readObjectFromFile(f);
                    if (schedule != null && !schedule.ScheduleUnknown) {
                        airlineSchedules.put(airline.getKey(), schedule);
                    }
                }
            }
        }
        logger.info("#### COMPLETED all available airline schedules for " + origAirportCode + "-" + destAirportCode + " for season " + season + "####");
    }

    private static Month buildAirlineScheduleMonth(Schedule schedule, LocalDate localDate) {
        Month month = schedule.getMonth(localDate.getYear(), localDate.getMonthValue());
        if (month == null) {
            month = new Month(localDate.getYear(), localDate.getMonthValue());
            schedule.addMonth(month);
        }
        return month;
    }

    private static Schedule buildAirlineSchedule(OpAirline airline, JsonObject json, OpSeason opSeason){
        Schedule schedule = new Schedule();
        schedule.ScheduleStarted = true;
        schedule.ScheduleEnded = false;
        schedule.ScheduleUnknown = false;
        switch(airline.getCode()){
            /* Thomson */
            case "TOM" :
                for (JsonElement date : json.get("dates").getAsJsonArray()) {
                    LocalDate localDate = LocalDate.parse(date.getAsString(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    buildAirlineScheduleMonth(schedule, localDate).FlightDates.add(localDate.getDayOfMonth());
                }
                break;
            /* Primera Air */
            case "PRI" :
                for (JsonElement date : json.getAsJsonArray("Data")){
                    if(Integer.parseInt(date.getAsJsonObject().get("availability").getAsString())!=0) {
                        LocalDate localDate = LocalDate.parse(date.getAsJsonObject().get("flightDate").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        buildAirlineScheduleMonth(schedule, localDate).FlightDates.add(localDate.getDayOfMonth());
                    }
                }
                break;
            /* Blue Air */
            case "BMS" :
                for (JsonElement jsonSchedule : json.getAsJsonObject("basicSchedule").getAsJsonArray("schedules")){
                    LocalDate scheduleStartDate = LocalDate.parse(jsonSchedule.getAsJsonObject().get("key").getAsJsonObject().get("start").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    LocalDate scheduleEndDate = LocalDate.parse(jsonSchedule.getAsJsonObject().get("key").getAsJsonObject().get("end").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
                    //If the schedule start date is before today set it to today
                    if(scheduleStartDate.isBefore(today)){
                        scheduleStartDate = today;
                    }
                    //If BMS schedule is within the current opSeason then build schedule
                    if(
                            ( scheduleStartDate.isEqual(DateHelper.convertDateToLocalDate(opSeason.getStartDate())) || scheduleStartDate.isAfter(DateHelper.convertDateToLocalDate(opSeason.getStartDate()))) &&
                            ( scheduleEndDate.isEqual(DateHelper.convertDateToLocalDate(opSeason.getEndDate())) || scheduleEndDate.isBefore(DateHelper.convertDateToLocalDate(opSeason.getEndDate())))
                    ) {
                        Month month = null;
                        while(scheduleStartDate.isBefore(scheduleEndDate) || scheduleStartDate.isEqual(scheduleEndDate)){
                            jsonSchedule.getAsJsonObject().get("value").getAsJsonArray();  //.get("dayOfWeek");
                            List<String> operatedDaysOfWeek = StreamSupport.stream(jsonSchedule.getAsJsonObject().get("value").getAsJsonArray().spliterator(), false).map(value -> value.getAsJsonObject().get("dayOfWeek").getAsString().toUpperCase()).collect(Collectors.toList());
                            if(operatedDaysOfWeek.contains(scheduleStartDate.getDayOfWeek().name())){
                                month = buildAirlineScheduleMonth(schedule, scheduleStartDate);
                                month.FlightDates.add(scheduleStartDate.getDayOfMonth());
                            }
                            //Forward the date on
                            scheduleStartDate = scheduleStartDate.plusDays(1);
                        }
                    }
                }
                break;
			/* Jet2 */
			case "EXS" : 	String[] schedules = json.get("d").getAsString().split("\\|");
							boolean monthIncremented = true;
							//Remove trailing json garbage
							schedules = Arrays.copyOf(schedules, schedules.length-1);
							for(String scheduleStr: schedules){
								String[] scheduleParts = scheduleStr.split(",");
								LocalDate dateRangeStartDate = LocalDate.parse(scheduleParts[0], DateTimeFormatter.ofPattern("d/MM/yyyy"));
								LocalDate dateRangeEndDate = LocalDate.parse(scheduleParts[1], DateTimeFormatter.ofPattern("d/MM/yyyy"));
								String dateRangeFlyDays = scheduleParts[2];
								if(dateRangeFlyDays.contains("1")){
									//Get all weekday fly day indexes
									List<Character> chars = dateRangeFlyDays.chars().mapToObj(e->(char)e).collect(Collectors.toList());
									//Get operating weekday names
									ArrayList<String> operatingDays = new ArrayList<>();
									int idx = 0;
									for(char character : chars){
										if(character=='1'){
											operatingDays.add(weekDayLongName[idx]);
										}
										idx++;
									}
									//Rotate through the date range and build months with flydays
									LocalDate opRangeCurrentDate = LocalDate.of(dateRangeStartDate.getYear(), dateRangeStartDate.getMonth(), dateRangeStartDate.getDayOfMonth());
									Month ScheduleMonth = null;
									ArrayList<Integer> ScheduleMonthFlydaysTemp = null;

									while(opRangeCurrentDate.isBefore(dateRangeEndDate) || opRangeCurrentDate.isEqual(dateRangeEndDate)){
										//Initialise Schedule month with cached Month from schedule if different from current ScheduleMonth
										if(monthIncremented==false && (ScheduleMonth==null || ScheduleMonth.MonthNumber!=opRangeCurrentDate.getMonthValue()) ){
											ScheduleMonth = schedule.getMonth(opRangeCurrentDate.getYear(), opRangeCurrentDate.getMonthValue());
											if(ScheduleMonth!=null ) {
                                                ScheduleMonthFlydaysTemp = ScheduleMonth.FlightDates;
                                            }
										}
										//Create new month if the month increments or on initial month
										if(monthIncremented || ScheduleMonth==null){
											ScheduleMonth = new Month();
											ScheduleMonth.MonthNumber = opRangeCurrentDate.getMonthValue();
											ScheduleMonth.YearNumber = opRangeCurrentDate.getYear();
											ScheduleMonth.Gap = "false";
											schedule.addMonth(ScheduleMonth);
											ScheduleMonthFlydaysTemp = new ArrayList<>();
											monthIncremented=false;
										}
										DayOfWeek currentDayOfWeek = opRangeCurrentDate.getDayOfWeek();
										if(operatingDays.contains(currentDayOfWeek.name())){
											ScheduleMonthFlydaysTemp.add(opRangeCurrentDate.getDayOfMonth());
										}
										if(
										        //Fixed issue where incrementing the day fell into the next year and therefore the month = 1 which is less than the current month!!!
										        opRangeCurrentDate.plusDays(1).getMonthValue() != opRangeCurrentDate.getMonthValue()
                                        ){
											monthIncremented=true;
											//Add flydays to schedule applicable month before moving on to the next month
											ScheduleMonth.FlightDates = ScheduleMonthFlydaysTemp;
										}
										//Increment the marker
										opRangeCurrentDate = opRangeCurrentDate.plusDays(1);
									}
									//Add flydays to schedule applicable month before moving on to the next schedule too
									ScheduleMonth.FlightDates = ScheduleMonthFlydaysTemp;
								}
							}
							break;

        }
        return schedule;
    }

	/* Jet2 method */
	private static void getAirlineAirportMapping(OpAirline airline) throws Exception {
        String filePathString = cachePath + "mapping" + File.separator + airline.getCode() + "-airport-mapping.txt";
        File f = new File(filePathString);
        filePathString = cachePath + "mapping" + File.separator + airline.getCode() + "-airport-mapping.json";
        File j = new File(filePathString);
        LocalDateTime fileLastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
        if(!f.exists() || fileLastModified.isAfter(LocalDateTime.now().plusMonths(1)) ) {
            Connection connection = Jsoup.connect(airline.getUrl()).userAgent(USER_AGENT);
            Document htmlDocument = connection.get();
            logger.info("\n'GET' request to URL : " + airline.getUrl());
            int airlineAirportCount = 0;
            LinkedHashMap<String, Airport> airportMapping = new LinkedHashMap<>();
            //Jet2 Store airline airport mappings
            switch (airline.getCode()) {
                case "EXS":
                    Element airlineOutboundDropdown = htmlDocument.getElementById("ctl00_centralDynamicContent_originatingAirportDropDown");
                    for (Element element : airlineOutboundDropdown.children()) {
                        if (airlineAirportCount > 0) {
                            String airportName = WordUtils.capitalizeFully(StringUtils.lowerCase(element.text()));
                            String code = getTCXAirportCodeByName(airportName);
                            if (code.isEmpty() || code.equalsIgnoreCase("XXX")) {
                                //Otherwise try to recover from TCX airport mapping
                                logger.warn(airline.getName() + ": no IATA code found for " + airportName + ". from TCX airport name/iata code mapping. Using Jet2.");
                                //Try to recover the airport code from the airline dropdown option text
                                Pattern regex = Pattern.compile("\\b[A-Z]{3}\\b");
                                Matcher m = regex.matcher(element.text());
                                if (m.find()) {
                                    code = m.group();
                                }
                            }
                            airportMapping.put(code, new Airport(airportName, code, element.val()));
                            logger.info(airline.getName() + ":" + airportName + ":" + code + ":" + element.val());
                        }
                        airlineAirportCount++;
                    }
                    airline.setAirportMapping(airportMapping);
                    //Cache to file
                    FileHelper.writeObjectToFile(airline,f);
                    FileHelper.writeObjectToJSONFile(airline,j);
                    break;
            }
            logger.info(airline.getName() + " airport mapping DONE! " + airlineAirportCount + " mapped");
        }
        else{
            //Recover cached airport mapping!!!
            OpAirline cachedOpAirlineAirportMapping = (OpAirline)  FileHelper.readObjectFromFile(f);
            airline.setAirportMapping(cachedOpAirlineAirportMapping.getAirportMapping());
            logger.info("Recovered airport mapping for " + airline.getName() + " from cache ");
        }
	}

	private static String getTCXAirportCodeByName(String airportName){
		try{
			return airportMapping.entrySet().stream().filter(airport -> airport.getValue().contains(airportName) || airportName.contains(airport.getValue()) || airport.getValue().contains(airportName.replaceAll("Intl","International"))).map(Map.Entry::getKey).findFirst().get();
		}catch(NoSuchElementException nsee){
			logger.warn("No iata found - " + nsee.getMessage());
			return "XXX";
		}
	}


    // HTTP POST request
    private static JsonObject airlineRouteScheduleJsonRequest(OpAirline airline, ArrayList<NameValuePair> params, String payload) throws Exception {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        String url = airline.getUrl();
        String aSReqPayload = "";
        Connection aSReq = Jsoup.connect(url).ignoreContentType(true);
        StringBuilder urlIncParams = new StringBuilder(url);
        AtomicInteger paramCount = new AtomicInteger();
        aSReq.headers(
            new HashMap<String, String>()
            {
                {
                    put("Content-Type", "application/json");
                }
            }
        );

        //POST - where params need to be posted as json payloads (JET2)
        switch (airline.getName()){
            case "Jet2" :
                                aSReq.request().removeHeader("Accept-Encoding").removeHeader("User-Agent");
                                aSReq.method(Connection.Method.POST);
                                aSReqPayload = "{" + params.stream().map(a -> "\"" + a.getName() + "\":\"" + (airline.getAirportMapping().getOrDefault(a.getValue(), new Airport("","",""))).getMapping() + "\"").collect(Collectors.joining(",")) + "}";
                                aSReq.requestBody(aSReqPayload);
                                //Jet2 payload example: {origAirportId:'16',destAirportId:'4'}
                                break;
            case "Primera Air" :
                                aSReq.request().removeHeader("Accept-Encoding").removeHeader("User-Agent");
                                aSReq.method(Connection.Method.POST);
                                aSReqPayload = "{" + params.stream().map(a -> "\"" + a.getName() + "\":\"" + a.getValue() + "\"").collect(Collectors.joining(",")) + "}";
                                aSReq.requestBody(aSReqPayload);
                                //Jet2 payload example: {origAirportId:'16',destAirportId:'4'}
                                break;

            case "Wizz Air" :   aSReq.request().removeHeader("Accept-Encoding").removeHeader("User-Agent");
                                aSReq.method(Connection.Method.POST);
                                //pre-constructed payload
                                aSReq.requestBody(payload);
                                break;

            case "Ryanair" :    aSReq.method(Connection.Method.GET);
                                params.forEach(param -> {
                                    if(!param.getName().equalsIgnoreCase("origin") && !param.getName().equalsIgnoreCase("destination")){
                                        urlIncParams.append(param.getName() + "/");
                                    }
                                    urlIncParams.append(param.getValue());
                                    if(paramCount.get() < (params.size()-1)){
                                        urlIncParams.append("/");
                                    }
                                    paramCount.getAndIncrement();
                                });
                                aSReq.url(urlIncParams.toString());
                                break;

            default :
                //Airlines such as Blue Air uses typical GET request
                aSReq.method(Connection.Method.GET);
                StringBuilder urlIncQuery = new StringBuilder(url + "?");
                params.forEach(param -> urlIncQuery.append(param.getName()).append("=").append(param.getValue()).append("&"));
                aSReq.url(urlIncQuery.toString());
                break;
        }

        String aSRes = aSReq.execute().body();
        try {
            jsonObject = new JsonParser().parse(aSRes).getAsJsonObject();
        }
        catch (IllegalStateException ise){
            ise.printStackTrace();
        }
        try{
            jsonArray = new JsonParser().parse(aSRes).getAsJsonArray();
        }catch (IllegalStateException ise){
            ise.printStackTrace();
        }

        if(!jsonArray.isJsonNull()){
            jsonObject.add("dates",jsonArray);
        }
        //logger.info(json.get("d").getAsString());
        return jsonObject;
    }

    private static LinkedList<OpRange> getAirlineOpRanges(OpAirline airline, OpRange opRange, boolean isGap){
        LinkedList<OpRange> opRanges = new LinkedList<>();
        LocalDate opRangeStartDate = DateHelper.convertDateToLocalDate(opRange.getStartDate());
        LocalDate opRangeEndDate = DateHelper.convertDateToLocalDate(opRange.getEndDate());
        Schedule airlineSchedule = airlineSchedules.get(airline.getCode());
        OpRange airlineOpRange = new OpRange();
        airlineOpRange.setGap(isGap);
        boolean startDateSet = false;

        //FOR TESTING: Line 574 getAirlineOpRanges breakpoint:
        //airline.code.equals("TOM") && DateHelper.convertDateToLocalDate(opRange.getStartDate()).getDayOfWeek().name.equals("FRIDAY")

        if(airlineSchedule!=null && !airlineSchedule.ScheduleUnknown) {
            while (opRangeStartDate.isBefore(opRangeEndDate.plusDays(7)) || opRangeStartDate.isEqual(opRangeEndDate.plusDays(7))) {
                Month month = airlineSchedule.getMonth(opRangeStartDate.getYear(), opRangeStartDate.getMonthValue());
                int opDayValue = opRangeStartDate.getDayOfMonth();
                if (
                        month != null && month.FlightDates.contains(opDayValue) && !opRangeStartDate.isAfter(opRangeEndDate)
                        ) {
                    if (!startDateSet) {
                        airlineOpRange.setStartDate(DateHelper.convertLocalDateToDate(opRangeStartDate));
                        startDateSet = true;
                    } else if (month.FlightDates.contains(opDayValue)) {
                        airlineOpRange.setEndDate(DateHelper.convertLocalDateToDate(opRangeStartDate));
                    }
                }
                else if(startDateSet){
                    //No month available on airline schedule so must be the end of current airline opRange!!!
                    if(airlineOpRange.getEndDate()==null || airlineOpRange.getEndDate().before(airlineOpRange.getStartDate())){
                        airlineOpRange.setEndDate(airlineOpRange.getStartDate());
                    }
                    logger.info("##### Add OpRange [" + airlineOpRange.getStartDate() + "-" + airlineOpRange.getEndDate() + "] for OpDay [" + DateHelper.convertDateToLocalDate(opRange.getStartDate()).getDayOfWeek() + "] to " + airline.getName());
                    opRanges.add(SerializationUtils.clone(airlineOpRange));
                    startDateSet = false;
                }
                opRangeStartDate = opRangeStartDate.plusDays(7);
            }
        }
        return opRanges;
    }

    private static void traverseFlightBible(XSSFWorkbook wb, OpSeason opSeason, OpAirport opOrigAirport, OpAirport opDestAirport){

        Sheet sheet = wb.getSheet(opDestAirport.getCode());
        String currentOutAirport = "";
        int currentOpDayWeekDayIndex = -1;
        String currentOpDay = "";
        OpRange opRange = new OpRange(opSeason.getStartDate(), opSeason.getStartDate());
        //boolean routeFirstRow;
        LocalDate weeksToDepartureDate = LocalDate.now().plusWeeks(weeksToDeparture);
        LocalDate opDayCurrentOccurrenceDate = null;

        if(sheet!=null) {
            logger.info("EXCEL: Processing Destination Airport: " + opDestAirport.getCode());
            long rowCount = 0;
            boolean opDayChanged = false;
            for (Row row : sheet) {
                rowCount++;
                if (
                        !StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(0))) && ExcelHelper.getCellValueAsString(row.getCell(0)).equals(opOrigAirport.getCode()) ||
                                (StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(0))) || ExcelHelper.getCellValueAsString(row.getCell(0)).trim().getBytes().length > 3) && currentOutAirport.equals(opOrigAirport.getCode())
                        ) {
                    if (!StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(0))) && ExcelHelper.getCellValueAsString(row.getCell(0)).equals(opOrigAirport.getCode())) {
                        logger.info("EXCEL: Processing Origin Airport: " + ExcelHelper.getCellValueAsString(row.getCell(0)));
                    }
                    //Reset the opRange for the next operating day e.g. MONDAY
                    if (!StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(2)))) {
                        currentOpDay = ExcelHelper.getCellValueAsString(row.getCell(2)).substring(0,3);
                        currentOpDayWeekDayIndex = ArrayUtils.indexOf(weekDayShortName, currentOpDay);
                        logger.info("EXCEL: Processing operation day: " + currentOpDay);
                        opDayChanged = true;
                        opRange.setStartDate(opSeason.getStartDate());
                        opRange.setEndDate(opSeason.getEndDate());
                        opRange.setGap(false);
                        //Reset the airlines associated with this opRange else they will be carried over from the previous opRanges
                        opRange.clearAirlines();
                        opDayCurrentOccurrenceDate = null;
                    }

                    if (!currentOpDay.equals("") && currentOpDayWeekDayIndex!=-1) {
                        //####### Process COMPETE ROUTE operating days!!!! #######
                        OpDay opDay = opOrigAirport.getOpDay(currentOpDay);
                        if (opDay == null) {
                            opOrigAirport.putOpDay(weekDayShortName[currentOpDayWeekDayIndex], weekDayLongName[currentOpDayWeekDayIndex], currentOpDayWeekDayIndex);
                            opDay = opOrigAirport.getOpDay(weekDayShortName[currentOpDayWeekDayIndex]);
                        }
                        if (opDayCurrentOccurrenceDate == null) {
                            opDayCurrentOccurrenceDate = DateHelper.convertDateToLocalDate(opRange.getStartDate()).with(TemporalAdjusters.nextOrSame(DayOfWeek.of((currentOpDayWeekDayIndex + 1))));
                        }

                        LocalDate rowStartDate = null;
                        LocalDate rowEndDate = null;
                        if (!StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(5))) && !StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(6)))) {

                            if(row.getCell(5).getCellType() == 0){
                                rowStartDate = LocalDate.parse(ExcelHelper.getCellValueAsString(row.getCell(5)), apiDateFormatter);
                            }else{
                                //USER INPUT ERROR!!! capture date cells that have been input with a single quote which format them as text!!!!
                                rowStartDate = LocalDate.parse(row.getCell(5).toString(), excelDateFormatter);
                            }

                            if(row.getCell(6).getCellType() == 0){
                                rowEndDate = LocalDate.parse(ExcelHelper.getCellValueAsString(row.getCell(6)), apiDateFormatter);
                            }else{
                                //USER INPUT ERROR!!! capture date cells that have been input with a single quote which format them as text!!!!
                                rowEndDate = LocalDate.parse(row.getCell(6).toString(), excelDateFormatter);
                            }

                        }

                        //For duplicate OpDay rows check to see if the new row start date is after those already stored in OpDay
                        if (
                                ( rowStartDate != null && rowEndDate != null && (opDay.getOpRanges().isEmpty() || DateHelper.convertDateToLocalDate(opDay.getOpRanges().getLast().getEndDate()).isBefore(rowStartDate)) )
                                //TODO: IS IT FIXED??? Need to fix BUG that doesn't pick up end opportunities!!!! GLA-CFU!!!
                                || (rowStartDate == null && rowEndDate == null && !opDay.getOpRanges().isEmpty())
                            ) {

                            if (rowStartDate != null) {
                                logger.info("EXCEL: Processing Row Date Range: " + ExcelHelper.getCellValueAsString(row.getCell(5)) + "-" + ExcelHelper.getCellValueAsString(row.getCell(6)));

                                //Create opRange that may exist before the row's start date for the current opDay
                                while (
                                        opDayCurrentOccurrenceDate.isBefore(rowEndDate) ||
                                                opDayCurrentOccurrenceDate.isEqual(rowEndDate)
                                        ) {
                                    if (
                                            rowStartDate.isAfter(opDayCurrentOccurrenceDate) ||
                                                    opDayCurrentOccurrenceDate.isBefore(weeksToDepartureDate)
                                            ) {
                                        //Create any GAP opRange prior to the row's operating start date!!
                                        opRange.setStartDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));
                                        opRange.setEndDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));
                                        while (
                                                opDayCurrentOccurrenceDate.plusDays(7).isBefore(rowStartDate) ||
                                                        opDayCurrentOccurrenceDate.plusDays(7).isBefore(weeksToDepartureDate)
                                                ) {
                                            opDayCurrentOccurrenceDate = opDayCurrentOccurrenceDate.plusDays(7);
                                        }
                                        opRange.setEndDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));
                                        opDayCurrentOccurrenceDate = opDayCurrentOccurrenceDate.plusDays(7);
                                        opRange.setGap(true);
                                        opDay.addOpRange(SerializationUtils.clone(opRange));
                                    } else {
                                        //Handle row opRanges!!!
                                        opRange.setStartDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));
                                        opRange.setEndDate(DateHelper.convertLocalDateToDate(rowEndDate));
                                        opRange.setGap(false);
                                        opDay.addOpRange(SerializationUtils.clone(opRange));
                                        opDayCurrentOccurrenceDate = rowEndDate;
                                        opDayCurrentOccurrenceDate = opDayCurrentOccurrenceDate.plusDays(7);
                                    }

                                }

                            } else {
                                //The date range cells are empty so this signifies the end of an OpDay
                                //Let's check that there's no post opRange and if there is add it to the OpDay
                                if (
                                        opDayCurrentOccurrenceDate.isBefore(DateHelper.convertDateToLocalDate(opSeason.getEndDate()))
                                                || opDayCurrentOccurrenceDate.isEqual(DateHelper.convertDateToLocalDate(opSeason.getEndDate()))
                                        ) {
                                    //opDayCurrentOccurrenceDate = opDayCurrentOccurrenceDate.plusDays(7);
                                    opRange.setStartDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));
                                    opRange.setEndDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));

                                    while (opDayCurrentOccurrenceDate.plusDays(7).isBefore(DateHelper.convertDateToLocalDate(opSeason.getEndDate()))) {
                                        opDayCurrentOccurrenceDate = opDayCurrentOccurrenceDate.plusDays(7);
                                        opRange.setEndDate(DateHelper.convertLocalDateToDate(opDayCurrentOccurrenceDate));
                                    }
                                    opDayCurrentOccurrenceDate = opDayCurrentOccurrenceDate.plusDays(7);
                                    opRange.setGap(true);
                                    opDay.addOpRange(SerializationUtils.clone(opRange));
                                }
                                /*####### Loop through the OpDay OpRanges and add airline schedules #######*/
                                if(opDayChanged) {
                                    for (OpRange opDayOpRange : opDay.getOpRanges()) {
                                        if (opDayOpRange.isGap()) {
                                            addThirdPartyAirlineSchedules(opDayOpRange);
                                        }
                                    }
                                    opDayChanged = false;
                                }
                            }
                        }

                    }
                }
                if (!StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(0))) &&
                        ExcelHelper.getCellValueAsString(row.getCell(0)).trim().getBytes().length == 3
                        ) {
                    currentOutAirport = ExcelHelper.getCellValueAsString(row.getCell(0));
                }
            }
            logger.info("EXCEL: Finish traversing sheet for : " + opDestAirport.getCode() + " with row count of " + rowCount);
        }
        else{
            logger.info("EXCEL: NO SHEET AVAILABLE FOR: " + opDestAirport.getCode());
        }

    }

    private static void setAirlineNonCompeteOperatingDays(OpAirport opOrigAirport, OpSeason opSeason){
        /*
         * Method used where day of week is not serviced by Thomas Cook or for non-compete routes (Route is empty of opDays or simply is not present in the bible)
         */
        //Get a list of dayOfweeks that are not present in the current opOrigAirport
        String opDays = opOrigAirport.getOpDays().values().stream().map(OpDay::getName).collect(Collectors.joining(","));
        List<DayOfWeek> dayOfWeeks = Arrays.stream(DayOfWeek.values()).filter(dow -> !opDays.contains(dow.name())).collect(Collectors.toList());

        for(DayOfWeek dayOfWeek : dayOfWeeks){
            OpDay opDay = new OpDay(dayOfWeek);
            LocalDate startDate = DateHelper.convertDateToLocalDate(opSeason.getStartDate());
            LocalDate endDate = DateHelper.convertDateToLocalDate(opSeason.getEndDate());
            LocalDate firstOccurrenceOpDayDate = startDate
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(opDay.getName())));
            LocalDate lastOccurrenceOpDayDate = endDate
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(opDay.getName())));
            logger.info("Creating NON-COMPETE opRange [" + firstOccurrenceOpDayDate + " - " + lastOccurrenceOpDayDate + "] for " + opOrigAirport + " OpDay [" + opDay.getName() + "]");
            OpRange nonCompeteOpRange = new OpRange(DateHelper.convertLocalDateToDate(firstOccurrenceOpDayDate), DateHelper.convertLocalDateToDate(lastOccurrenceOpDayDate));
            nonCompeteOpRange.setGap(true);
            //Add all 3rd party schedules
            addThirdPartyAirlineSchedules(nonCompeteOpRange);
            if(nonCompeteOpRange.getAirlines().size()>0) {
                opDay.addOpRange(nonCompeteOpRange);
            }
            if(opDay.getOpRanges().size()>0){
                opOrigAirport.addOpDay(weekDayShortName[dayOfWeek.ordinal()], opDay);
            }
        }

        opOrigAirport.setOpDays(opOrigAirport.getOpDays().entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().getOrdinal())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
        logger.info("Completed NON-COMPETE OpRanges on " + opOrigAirport.getCode());
    }

    private static void addThirdPartyAirlineSchedules(OpRange nonCompeteOpRange){
        for(Map.Entry<String, OpAirline> airline : airlines.entrySet()){
            if(!airline.getKey().equals("TCX") &&
                (processAirlines.isEmpty() || processAirlines.contains(airline.getKey()))
            ){
                OpAirline opRangeAirline = SerializationUtils.clone(airline.getValue());
                LinkedList<OpRange> opRanges = getAirlineOpRanges(airline.getValue(), nonCompeteOpRange, true);
                opRangeAirline.setOpRanges(opRanges);
                if(opRangeAirline.getOpRanges().size()>0){
                    nonCompeteOpRange.addAirline(opRangeAirline);
                }
            }
        }
    }

    private static void addGapRowCells(Sheet sheet, Row row, ArrayList<ArrayList<String>> headers, LinkedHashMap<String, XSSFCellStyle> LIPStyles, boolean rowSeparator){
        for(ArrayList<String> header : headers){
            int headerRowColCount = 0;
            for(@SuppressWarnings("unused") String headerCol : header){
                Cell cell = row.createCell(headerRowColCount);
                cell.setCellType(Cell.CELL_TYPE_STRING);
                if(headers.get(0).get(headerRowColCount).isEmpty()){
                    if(rowSeparator){
                        cell.setCellStyle(LIPStyles.get("greyCellStyleWithBottomBorder"));
                    }else{
                        cell.setCellStyle(LIPStyles.get("greyCellStyle"));
                    }
                    sheet.setColumnWidth(cell.getColumnIndex(), 400);
                }else{
                    if(rowSeparator){
                        cell.setCellStyle(LIPStyles.get("baseCellStyleWithBottomBorder"));
                    }else{
                        cell.setCellStyle(LIPStyles.get("baseCellStyle"));
                    }
                }
                headerRowColCount++;
            }
        }
    }

    private static LinkedHashMap<String, List<String>> fetchBibleRoutes(String flightBibleName){
        FileInputStream fis;
        XSSFWorkbook wb;
        String applicationPath = "";
        LinkedHashMap<String, List<String>> bibleRoutes = new LinkedHashMap<>();
        //Get Application path
        try{
            applicationPath = new File(".").getCanonicalPath();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        try {
            fis = new FileInputStream(new File(applicationPath + File.separator + "excels" + File.separator + flightBibleName));
            wb = new XSSFWorkbook(fis);
            for (int i = 0; i < wb.getNumberOfSheets(); i++)
            {
                Sheet sheet = wb.getSheetAt(i);
                if(sheet.getSheetName().matches("[A-Z]{3}")) {
                    List<String> origins = new ArrayList<>();
                    for (Row row : sheet) {
                        if (
                                !StringUtils.isBlank(ExcelHelper.getCellValueAsString(row.getCell(0)))
                                        && ExcelHelper.getCellValueAsString(row.getCell(0)).matches("[A-Z]{3}")
                                ) {
                            origins.add(ExcelHelper.getCellValueAsString(row.getCell(0)));
                        }
                    }
                    if(origins.size()>0) {
                        bibleRoutes.put(sheet.getSheetName().trim(), origins);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return bibleRoutes;
    }

    private static void generateLIPSFiles(LinkedHashMap<String, OpSeason> LIPS){

        ArrayList<ArrayList<String>> headers = new ArrayList<>();
        ArrayList<String> mainHeaders = new ArrayList<>(Arrays.asList("DEP","OP DAY","START","END","","TCX SEATS","-","","TCX",""));
        ArrayList<String> subHeaders = new ArrayList<>(Arrays.asList("","","","","","MAINSTREAM","SEAT ONLY","","",""));
        airlines.keySet().forEach((k) -> {
            if(!k.equals("TCX")) {
                mainHeaders.add(k);
                mainHeaders.add("");
                subHeaders.add("");
                subHeaders.add("");
            }
        });
        headers.add(mainHeaders);
        headers.add(subHeaders);

        String crLf = Character.toString((char)13) + Character.toString((char)10);

        logger.info("###########!!!!!!! Attempting generation of LIPS files !!!!!!!###########");

        //Output LIPS spreadsheet
        for(Map.Entry<String, OpSeason> season : LIPS.entrySet()){
            //LIPSout.createSheet()
            XSSFWorkbook LIPSout = new XSSFWorkbook();

            LinkedHashMap<String, XSSFCellStyle> LIPStyles = new LinkedHashMap<>();

            //Create workbook styles
            XSSFCellStyle baseCellStyle = LIPSout.createCellStyle();
            baseCellStyle.setAlignment(HorizontalAlignment.CENTER);
            LIPStyles.put("baseCellStyle", baseCellStyle);

            XSSFCellStyle baseCellStyleWithBottomBorder = LIPSout.createCellStyle();
            baseCellStyleWithBottomBorder.setAlignment(HorizontalAlignment.CENTER);
            baseCellStyleWithBottomBorder.setBorderBottom(XSSFCellStyle.BORDER_THIN);
            LIPStyles.put("baseCellStyleWithBottomBorder", baseCellStyleWithBottomBorder);

            XSSFCellStyle greyCellStyle = LIPSout.createCellStyle();
            greyCellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(216, 216, 216)));
            greyCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
            greyCellStyle.setAlignment(HorizontalAlignment.CENTER);
            LIPStyles.put("greyCellStyle", greyCellStyle);

            XSSFCellStyle greyCellStyleWithBottomBorder = LIPSout.createCellStyle();
            greyCellStyleWithBottomBorder.setFillForegroundColor(new XSSFColor(new java.awt.Color(216, 216, 216)));
            greyCellStyleWithBottomBorder.setFillPattern(CellStyle.SOLID_FOREGROUND);
            greyCellStyleWithBottomBorder.setAlignment(HorizontalAlignment.CENTER);
            greyCellStyleWithBottomBorder.setBorderBottom(XSSFCellStyle.BORDER_THIN);
            LIPStyles.put("greyCellStyleWithBottomBorder", greyCellStyleWithBottomBorder);

            XSSFCellStyle greenCellStyle = LIPSout.createCellStyle();
            greenCellStyle.setAlignment(HorizontalAlignment.CENTER);
            greenCellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(146, 208, 80)));
            greenCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
            LIPStyles.put("greenCellStyle", greenCellStyle);

            XSSFCellStyle greenCellStyleWithBottomBorder = LIPSout.createCellStyle();
            greenCellStyleWithBottomBorder.setFillForegroundColor(new XSSFColor(new java.awt.Color(146, 208, 80)));
            greenCellStyleWithBottomBorder.setFillPattern(CellStyle.SOLID_FOREGROUND);
            greenCellStyleWithBottomBorder.setAlignment(HorizontalAlignment.CENTER);
            greenCellStyleWithBottomBorder.setBorderBottom(XSSFCellStyle.BORDER_THIN);
            LIPStyles.put("greenCellStyleWithBottomBorder", greenCellStyleWithBottomBorder);

            String lipsFileName = "generated/LIPSout_" + season.getKey() + "_" + LocalDateTime.now().format(timestampDateFormatter) + ".xlsx";

            try (FileOutputStream outputStream = new FileOutputStream(lipsFileName)) {
                for(Map.Entry<String, OpAirport> destination: season.getValue().getDestinations().entrySet()){

                    int originsOpDaysSum = destination.getValue().getOrigins().values().stream().mapToInt(e -> e.getOpDays().size()).sum();
                    //Check to see if current destination has any origins with opDays, if not then don't create the empty destination sheet!!!
                    if(originsOpDaysSum>0) {
                        Sheet sheet = LIPSout.createSheet(destination.getKey());

                        //Build header rows START
                        int sheetRowCount = 0;
                        for (ArrayList<String> header : headers) {
                            Row headerRow = sheet.createRow(sheetRowCount);
                            int headerRowColCount = 0;
                            for (String headerCol : header) {
                                Cell cell = headerRow.createCell(headerRowColCount);
                                cell.setCellType(Cell.CELL_TYPE_STRING);
                                cell.setCellValue(headerCol);
                                if (headers.get(0).get(headerRowColCount).isEmpty()) {
                                    cell.setCellStyle(LIPStyles.get("greyCellStyle"));
                                    sheet.setColumnWidth(cell.getColumnIndex(), 400);
                                } else {
                                    cell.setCellStyle(LIPStyles.get("baseCellStyle"));
                                }
                                headerRowColCount++;
                            }
                            if (sheetRowCount == 0) {
                                ExcelHelper.mergeCellsAndAlignCenter(LIPSout, headerRow.getCell(5), headerRow.getCell(6));
                            }
                            sheetRowCount++;
                        }
                        sheet.createFreezePane(1, 2);
                        //Build Header rows END

                        //Build Data rows START
                        String currentOriginAirportCode = "";
                        String currentOpDay = "";
                        for (Map.Entry<String, OpAirport> originAirport : destination.getValue().getAirports().entrySet()) {
                            boolean rowSeparator = false;
                            int opDayCount = 0;
                            long opDaysThatHaveOperationCount = originAirport.getValue().getOpDays().values().stream().filter(o -> o.getOpRanges().size() > 0).count();
                            for (Map.Entry<String, OpDay> opDay : originAirport.getValue().getOpDays().entrySet()) {
                                //Use TCX as row generator
                                if (opDay.getValue().getOpRanges().size() > 0) {
                                    opDayCount++;
                                    for (OpRange opRange : opDay.getValue().getOpRanges()) {
                                        Row dataRow = sheet.createRow(sheetRowCount);
                                        int dataRowCellCount = 0;
                                        for (String header : headers.get(0)) {
                                            Cell dataCell = dataRow.createCell(dataRowCellCount);
                                            dataCell.setCellStyle(LIPStyles.get("baseCellStyle"));
                                            //logger.info("Creating cell under header: " + header);
                                            if (header.isEmpty()) {
                                                dataCell.setCellStyle(LIPStyles.get("greyCellStyle"));
                                            }
                                            switch (dataRowCellCount) {
                                                case 0:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    if (currentOriginAirportCode.isEmpty() || !currentOriginAirportCode.equals(originAirport.getKey())) {
                                                        dataCell.setCellValue(originAirport.getKey());
                                                    }
                                                    break;
                                                case 1:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    if (currentOpDay.isEmpty() || !currentOpDay.equals(opDay.getKey())) {
                                                        dataCell.setCellValue(opDay.getKey());
                                                    }
                                                    break;
                                                case 2:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    dataCell.setCellValue(DateHelper.convertDateToLocalDate(opRange.getStartDate()).format(excelDateFormatter));
                                                    if (opRange.isGap()) {
                                                        dataCell.setCellStyle(LIPStyles.get("greenCellStyle"));
                                                    }
                                                    break;
                                                case 3:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    dataCell.setCellValue(DateHelper.convertDateToLocalDate(opRange.getEndDate()).format(excelDateFormatter));
                                                    if (opRange.isGap()) {
                                                        dataCell.setCellStyle(LIPStyles.get("greenCellStyle"));
                                                    }
                                                    break;
                                                case 5:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    dataCell.setCellValue(opRange.getMainstreamAlloc() + "/" + opRange.getMainstreamLoad());
                                                    break;
                                                case 6:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    dataCell.setCellValue(opRange.getSeatonlyAlloc() + "/" + opRange.getSeatonlyLoad());
                                                    break;

                                                case 8:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    if (!opRange.isGap()) {
                                                        dataCell.setCellValue("X");
                                                    }
                                                    break;

                                                default:
                                                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                                                    if (opRange.getAirlines().containsKey(header)) {
                                                        StringBuilder sb = new StringBuilder();
                                                        int airlineOpRangeCount = 0;
                                                        for (OpRange airlineOpRange : opRange.getOpAirline(header).getOpRanges()) {
                                                            if (airlineOpRangeCount > 0) {
                                                                sb.append(",").append(crLf);
                                                            }
                                                            //Only print out airline opRange if it is within the current opRange
                                                            if (
                                                                    (airlineOpRange.getStartDate().equals(opRange.getStartDate()) || airlineOpRange.getStartDate().after(opRange.getStartDate())) &&
                                                                            (airlineOpRange.getEndDate().equals(opRange.getEndDate()) || airlineOpRange.getEndDate().before(opRange.getEndDate()))
                                                                    ) {
                                                                sb.append(DateHelper.convertDateToLocalDate(airlineOpRange.getStartDate()).format(excelDateFormatter)).append(" - ").append(DateHelper.convertDateToLocalDate(airlineOpRange.getEndDate()).format(excelDateFormatter));
                                                                airlineOpRangeCount++;
                                                            }
                                                        }
                                                        dataCell.setCellValue(sb.toString());
                                                        if (opRange.isGap()) {
                                                            dataCell.setCellStyle(LIPStyles.get("greenCellStyle"));
                                                        }
                                                    }
                                                    break;

                                            }
                                            dataRowCellCount++;
                                        }
                                        currentOriginAirportCode = originAirport.getKey();
                                        currentOpDay = opDay.getKey();
                                        sheetRowCount++;
                                    }
                                    if (opDayCount == opDaysThatHaveOperationCount) {
                                        rowSeparator = true;
                                    }
                                    Row gapRow = sheet.createRow(sheetRowCount);
                                    addGapRowCells(sheet, gapRow, headers, LIPStyles, rowSeparator);
                                    sheetRowCount++;
                                }
                            }
                        }
                        //Build Data rows END
                    }
                }
                ExcelHelper.autoSizeColumns(LIPSout);
                LIPSout.write(outputStream);
                outputStream.close();
                logger.info("\t\tLIPS excel file generated: " + lipsFileName);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ArrayList<TpOpSeason> buildLIPSinData(LinkedHashMap<String, OpSeason> LIPS, LinkedHashMap<String, OpAirline> airlines){
        ArrayList<TpOpSeason> LIPSin = new ArrayList<>();
        for(Map.Entry<String, OpSeason> season : LIPS.entrySet()) {
            //Use a unique key here to identify airline destination origins that operate the same days!!! Example: TOM-AYT-0101001
            TpOpSeason tpOpSeason = new TpOpSeason(season.getValue().getName(), season.getKey(), season.getValue().getStartDate(), season.getValue().getEndDate());
            for(Map.Entry<String, OpAirport> destination : season.getValue().getDestinations().entrySet()) {
                for(Map.Entry<String, OpAirline> opAirline : airlines.entrySet()) {
                    if(processAirlines.contains(opAirline.getKey())) {
                        for (Map.Entry<String, OpAirport> origin : destination.getValue().getOrigins().entrySet()) {
                            TpOpRoute tpOpRoute = new TpOpRoute(destination.getKey());
                            for (Map.Entry<String, OpDay> opDay : origin.getValue().getOpDays().entrySet()) {
                                for (OpRange opRange : opDay.getValue().getOpRanges()) {
                                    for (Map.Entry<String, OpAirline> airline : opRange.getAirlines().entrySet()) {
                                        if (airline.getKey().equalsIgnoreCase(opAirline.getKey())) {
                                            //OpRanges exist so store OpDay
                                            if (airline.getValue().getOpRanges().size() > 0) {
                                                tpOpRoute.setOpDaysBit(opDay.getKey());
                                                tpOpRoute.setAirline(airline.getKey());
                                                if (!tpOpRoute.getOrigins().contains(origin.getKey())) {
                                                    tpOpRoute.addOrigin(origin.getKey());
                                                }
                                            }
                                            //Expand date range to accommodate new opDays?
                                            java.util.Date opRangeStartDate = airline.getValue().getOpRanges().getFirst().getStartDate();
                                            java.util.Date opRangeEndDate = airline.getValue().getOpRanges().getLast().getEndDate();
                                            if (tpOpRoute.getStartDate() == null || DateHelper.convertDateToLocalDate(opRangeStartDate).isBefore(DateHelper.convertDateToLocalDate(tpOpRoute.getStartDate()))) {
                                                tpOpRoute.setStartDate(opRangeStartDate);
                                            }
                                            if (tpOpRoute.getEndDate() == null || DateHelper.convertDateToLocalDate(opRangeEndDate).isAfter(DateHelper.convertDateToLocalDate(tpOpRoute.getEndDate()))) {
                                                tpOpRoute.setEndDate(opRangeEndDate);
                                            }
                                        }
                                    }
                                }
                            }
                            //Store the tpOpRoute along with the opRouteKey "TOM-AYT-0101001" if the key already exists just add the new origin to the existing tpOpRoute!
                            String tpOpRouteOpDaysKey = opAirline.getKey() + "-" + destination.getKey() + "-" + Arrays.stream(tpOpRoute.getOpDaysBits()).mapToObj(String::valueOf).collect(Collectors.joining());
                            if (!tpOpRouteOpDaysKey.contains("0000000")) {
                                if (!tpOpSeason.getTpOpRoutes().containsKey(tpOpRouteOpDaysKey)) {
                                    tpOpSeason.addTpOpRoute(tpOpRouteOpDaysKey, tpOpRoute);
                                } else {
                                    tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).addOrigins(tpOpRoute.getOrigins());
                                    //Expand date ranges to accommodate new origin!!!
                                    if (tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).getStartDate() == null || DateHelper.convertDateToLocalDate(tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).getStartDate()).isAfter(DateHelper.convertDateToLocalDate(tpOpRoute.getStartDate()))) {
                                        tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).setStartDate(tpOpRoute.getStartDate());
                                    }
                                    if (tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).getEndDate() == null || DateHelper.convertDateToLocalDate(tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).getEndDate()).isBefore(DateHelper.convertDateToLocalDate(tpOpRoute.getEndDate()))) {
                                        tpOpSeason.getTpOpRoutes().get(tpOpRouteOpDaysKey).setEndDate(tpOpRoute.getEndDate());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LIPSin.add(tpOpSeason);
        }
        return LIPSin;
    }

    private static void generateLIPSinFile(ArrayList<TpOpSeason> LIPSinData){

        ArrayList<ArrayList<String>> headers = new ArrayList<>();
        ArrayList<String> mainHeaders = new ArrayList<>(Arrays.asList("Airline","Date Range","Departure Points","Destination","Sun","Mon","Tue","Wed","Thu","Fri","Sat"));
        headers.add(mainHeaders);
        //String crLf = Character.toString((char)13) + Character.toString((char)10);
        XSSFWorkbook LIPSin = new XSSFWorkbook();
        LinkedHashMap<String, XSSFCellStyle> LIPStyles = new LinkedHashMap<>();

        //Create workbook styles
        XSSFCellStyle baseCellStyle = LIPSin.createCellStyle();
        baseCellStyle.setAlignment(HorizontalAlignment.CENTER);
        LIPStyles.put("baseCellStyle", baseCellStyle);

        XSSFCellStyle greenCellStyle = LIPSin.createCellStyle();
        greenCellStyle.setAlignment(HorizontalAlignment.CENTER);
        greenCellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(146, 208, 80)));
        greenCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        LIPStyles.put("greenCellStyle", greenCellStyle);

        String lipsFileName = "generated/LIPSin_" + LocalDateTime.now().format(timestampDateFormatter) + ".xlsx";

        logger.info("###########!!!!!!! Attempting generation of LIPS Input file !!!!!!!###########");

        //Output LIPS spreadsheet
        for(TpOpSeason season : LIPSinData){

            Sheet sheet = LIPSin.createSheet(season.getSeason());
            sheet.createFreezePane(2, 1);

            //Build header rows START
            int sheetRowCount = 0;
            for (ArrayList<String> header : headers) {
                Row headerRow = sheet.createRow(sheetRowCount);
                int headerRowColCount = 0;
                for (String headerCol : header) {
                    Cell cell = headerRow.createCell(headerRowColCount);
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    cell.setCellValue(headerCol);
                    cell.setCellStyle(LIPStyles.get("baseCellStyle"));
                    headerRowColCount++;
                }
                sheetRowCount++;
            }
            //Build Header rows END

            //sheet.createFreezePane(1, 2);

            //Build Data rows START
            for(Map.Entry<String, TpOpRoute> tpOpRouteEntry: season.getTpOpRoutes().entrySet()) {

                Row dataRow = sheet.createRow(sheetRowCount);
                int dataRowCellCount = 0;
                for (int i =0; i<4; i++) {
                    Cell dataCell = dataRow.createCell(dataRowCellCount);
                    dataCell.setCellStyle(LIPStyles.get("baseCellStyle"));
                    switch (dataRowCellCount) {
                        case 0:
                            dataCell.setCellType(Cell.CELL_TYPE_STRING);
                            dataCell.setCellValue(tpOpRouteEntry.getValue().getAirline());
                            break;
                        case 1:
                            dataCell.setCellType(Cell.CELL_TYPE_STRING);
                            String dateRange = DateHelper.convertDateToLocalDate(tpOpRouteEntry.getValue().getStartDate()).format(excelDateFormatter) + " - " + DateHelper.convertDateToLocalDate(tpOpRouteEntry.getValue().getEndDate()).format(excelDateFormatter);
                            dataCell.setCellValue(dateRange);
                            break;
                        case 2:
                            dataCell.setCellType(Cell.CELL_TYPE_STRING);
                            String origins = tpOpRouteEntry.getValue().getOrigins().stream().collect(Collectors.joining(","));
                            dataCell.setCellValue(origins);
                            //dataCell.setCellStyle(LIPStyles.get("greenCellStyle"));
                            break;
                        case 3:
                            dataCell.setCellType(Cell.CELL_TYPE_STRING);
                            dataCell.setCellValue(tpOpRouteEntry.getValue().getDestination());
                            //dataCell.setCellStyle(LIPStyles.get("greenCellStyle"));
                            break;
                    }
                    dataRowCellCount++;
                }

                //Add Operating days
                for(int opDayBit : tpOpRouteEntry.getValue().getOpDaysBits()){
                    Cell dataCell = dataRow.createCell(dataRowCellCount);
                    dataCell.setCellType(Cell.CELL_TYPE_STRING);
                    String value = opDayBit==0?"":"X";
                    dataCell.setCellValue(value);
                    if(!value.isEmpty()){
                        dataCell.setCellStyle(LIPStyles.get("greenCellStyle"));
                    }
                    dataRowCellCount++;
                }

                sheetRowCount++;
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(lipsFileName)) {
            ExcelHelper.autoSizeColumns(LIPSin);
            LIPSin.write(outputStream);
            outputStream.close();
            logger.info("\t\tLIPSin excel file generated: " + lipsFileName);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

}
