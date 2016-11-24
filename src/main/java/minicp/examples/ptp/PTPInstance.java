package minicp.examples.ptp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

import org.json.*;

/**
 * represent a Patient Transportation Problem - PTP
 */
public class PTPInstance {

    private String version;
    private int id;
    private String name;
    private String coordType;
    private boolean sameVehicleBackward;
    private Duration maxWaitTime;
    private int[][] distMatrix;
    private Place[] places;
    private Vehicle[] vehicles;
    private Patient[] patients;
    private int SCALING;

    private String filePath;
    private String jsonName;
    private int maxDuration;
    private int offset;
    private int nForwardTrips;
    private int nBackwardTrips;

    public record TimeWindow(LocalTime twStart, LocalTime twEnd) {

        public int minTime() {
            return toMinute(twStart);
        }

        public static TimeWindow parse(String tw) {
            String[] twSplit = tw.split(":");
            return new TimeWindow(parseLocalTime(twSplit[0]), parseLocalTime(twSplit[1]));
        }

        public Duration durationExclusive() {
            return Duration.between(twStart, twEnd);
        }

        public Duration durationInclusive() {
            return Duration.between(twStart, twEnd.plusMinutes(1));
        }

        public Duration duration() {
            return durationExclusive();
        }

    };

    public record Place(int id, double lat, double lon, int category) {

        public static Place parse(JSONObject jsonObject) {
            return new Place(jsonObject.getInt("id"), jsonObject.getDouble("lat"),
                    jsonObject.getDouble("long"), jsonObject.getInt("category"));
        }

    }

    public record Vehicle(int id, int[] canTake, int start, int end, int capacity, TimeWindow[] availability) {

        public boolean canTake(int cat) {
            for (int c: canTake) {
                if (c == cat)
                    return true;
            }
            return false;
        }

        public static Vehicle parse(JSONObject jsonObject) {
            JSONArray jsonAvailability = jsonObject.getJSONArray("availability");
            TimeWindow[] availability = new TimeWindow[jsonAvailability.length()];
            for (int i = 0 ; i < availability.length ; ++i)
                availability[i] = TimeWindow.parse((String) jsonAvailability.get(i));

            JSONArray jsonCanTake = jsonObject.getJSONArray("canTake");
            int[] canTake = new int[jsonCanTake.length()];
            for (int i = 0 ; i < canTake.length ; ++i)
                canTake[i] = (int) jsonCanTake.get(i);

            return new Vehicle(jsonObject.getInt("id"), canTake, jsonObject.getInt("start"),
                    jsonObject.getInt("end"), jsonObject.getInt("capacity"), availability);
        }

    }

    public record Patient(int id, int category, int load, int start, int dest, int end,
                          LocalTime rdvTime, Duration rdvDuration, Duration srvDuration) {

        public static Patient parse(JSONObject jsonObject) {
            return new Patient(jsonObject.getInt("id"),
                    jsonObject.getInt("category"),
                    jsonObject.getInt("load"),
                    jsonObject.getInt("start"),
                    jsonObject.getInt("destination"),
                    jsonObject.getInt("end"),
                    parseLocalTime(jsonObject, "rdvTime"),
                    parseDuration(jsonObject, "rdvDuration"),
                    parseDuration(jsonObject, "srvDuration"));
        }

    }

    /*
    public int timeOffset(Duration duration) {
        return toMinute(duration) - offset;
    }

    public int timeOffset(LocalTime localTime) {
        return toMinute(localTime) - offset;
    }

    public int minTimeOffset(TimeWindow tw) {
        return toMinute(tw.twStart) - offset;
    }

    public int maxTimeOffset(TimeWindow tw) {
        return toMinute(tw.twEnd) - offset;
    }

     */

    /**
     * parse a PTP instance
     * @param fileName path to the PTP instance
     */
    public PTPInstance(String fileName) {
        try {
            this.filePath = fileName;
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            this.jsonName = Paths.get(fileName).getFileName().toString();
            JSONObject jsonFile = new JSONObject(content);
            version = jsonFile.getString("version");
            id = jsonFile.getInt("id");
            name = jsonFile.getString("name");
            coordType = jsonFile.getString("coordType");
            sameVehicleBackward = jsonFile.getBoolean("sameVehicleBackward");
            maxWaitTime = parseDuration(jsonFile, "maxWaitTime");

            JSONArray jsonPlaces = jsonFile.getJSONArray("places");
            places = new Place[jsonPlaces.length()];
            for (int i = 0 ; i < places.length ; ++i)
                places[i] = Place.parse(jsonPlaces.getJSONObject(i));

            JSONArray jsonVehicles = jsonFile.getJSONArray("vehicles");
            vehicles = new Vehicle[jsonVehicles.length()];
            for (int i = 0 ; i < vehicles.length ; ++i)
                vehicles[i] = Vehicle.parse(jsonVehicles.getJSONObject(i));

            JSONArray jsonPatients = jsonFile.getJSONArray("patients");
            patients = new Patient[jsonPatients.length()];
            for (int i = 0 ; i < patients.length ; ++i)
                patients[i] = Patient.parse(jsonPatients.getJSONObject(i));

            distMatrix = new int[places.length][places.length];
            JSONArray jsonDistMatrix = jsonFile.getJSONArray("distMatrix");
            for (int i = 0 ; i < places.length ; ++i) {
                JSONArray jsonRow = jsonDistMatrix.getJSONArray(i);
                for (int j = 0; j < places.length ; ++j)
                    distMatrix[i][j] = jsonRow.getInt(j);
            }

            maxDuration = maxDuration();

            nForwardTrips = 0;
            nBackwardTrips = 0;
            for (Patient patient: patients) {
                if (patient.start != -1)
                    nForwardTrips += 1;
                if (patient.end != -1)
                    nBackwardTrips += 1;
            }

        } catch (IOException e) {
            System.err.println("error when loading " + fileName);
        }
    }

    /**
     * Represents a part of a solution path.
     * @param place Place id.
     * @param time Arrival time at place (can be a time window).
     * @param patient Patient serviced id.
     * @param operation 0: load forward, 1: unload forward, 2: load backward, 3: unload backward.
     */
    public record PTPStep(int place, String time, int patient, int operation) {

        public JSONObject toJson() {
            JSONObject description = new JSONObject();
            description.put("place", place);
            description.put("time", time);
            description.put("patient", patient);
            description.put("operation", operation);
            return description;
        }

    }

    public PTPSolution initializeSolution() {
        return new PTPSolution();
    }

    public PTPStep step(int place, String time, int patient, int operation) {
        return new PTPStep(place, time, patient, operation);
    }

    /**
     * solution of a PTP instance
     */
    public class PTPSolution {
        ArrayList<PTPStep> [] visits; // one array list per vehicle, denotes steps in the visit

        public PTPSolution() {
            visits = new ArrayList[vehicles.length];
            for (int i = 0 ; i < visits.length ; ++i) {
                visits[i] = new ArrayList<>();
            }
        }

        /**
         * register the visited nodes for the vehicle
         * the visit path must not contain the start node
         * @param vehicle vehicle for the visit
         * @param visits nodes id visited by the vehicle
         */
        public void addVisit(int vehicle, ArrayList<PTPStep> visits) {
            this.visits[vehicle].clear();
            this.visits[vehicle].addAll(visits);
        }

        /**
         * output the current solution to a json file
         * @param path path where to write the solution in json format
         */
        public void toJson(String path) {
            String filename = path + "/" + jsonName.replace(".json", "_sol.json");
            // get the values from the instance
            JSONObject solution = new JSONObject();
            JSONObject instance;
            try {
                // write the instance
                String content = new String(Files.readAllBytes(Paths.get(PTPInstance.this.filePath)));
                instance = new JSONObject(content);
                solution.put("instance", instance);
            } catch (IOException e) {
                System.err.println("failed to read instance data");
                return;
            }

            // write the solution related to the instance
            JSONArray paths = new JSONArray();
            for (int v = 0; v < vehicles.length ; ++v) {
                JSONObject vehiclePath = new JSONObject();
                JSONArray steps = new JSONArray();
                for (PTPStep step: visits[v]) {
                    steps.put(step.toJson());
                }
                vehiclePath.put("steps", steps);
                vehiclePath.put("vehicle", vehicles[v].id);
                paths.put(vehiclePath);
            }
            solution.put("paths", paths);
            try {
                FileWriter fw = new FileWriter(filename);
                BufferedWriter out = new BufferedWriter(fw);
                String solDesc = solution.toString(1);
                //System.out.println(solDesc);
                out.write(solDesc);
                out.close();
                fw.close();
                System.out.println("written to " + filename);
            } catch (IOException e) {
                System.err.println("failed to write solution to " + filename);
            }
        }

        /**
         * @param place Place id.
         * @param time Arrival time at place (can be a time window).
         * @param patient Patient serviced id.
         * @param operation 0: load forward, 1: unload forward, 2: load backward, 3: unload backward.
         */

    };

    /**
     * give the maximum duration of this instance, in minutes
     * @return
     */
    private int maxDuration() {
        LocalTime minStart = null;
        LocalTime maxEnd = null;
        for (Vehicle v: vehicles) {
            for (TimeWindow tw: v.availability) {
                if (minStart == null || tw.twStart.compareTo(minStart) < 0)
                    minStart = tw.twStart;
                if (maxEnd == null || tw.twEnd.compareTo(maxEnd) > 0)
                    maxEnd = tw.twEnd;
            }
        }
        return (int) Duration.between(minStart, maxEnd).toMinutes();
    }

    public LocalTime minStartTime() {
        LocalTime minStart = null;
        for (Vehicle v: vehicles) {
            for (TimeWindow tw: v.availability) {
                if (minStart == null || tw.twStart.compareTo(minStart) < 0)
                    minStart = tw.twStart;
            }
        }
        return minStart;
    }

    public LocalTime maxEndTime() {
        LocalTime maxStart = null;
        for (Vehicle v: vehicles) {
            for (TimeWindow tw: v.availability) {
                if (maxStart == null || tw.twEnd.compareTo(maxStart) > 0)
                    maxStart = tw.twEnd;
            }
        }
        return maxStart;
    }

    public static int toMinute(Duration duration) {
        return (int) duration.toMinutes();
    }

    public static int toMinute(LocalTime localTime) {
        return localTime.getMinute() + localTime.getHour() * 60;
    }

    /**
     * convert a time in minute to HH:MM format
     * @param time time value in minutes
     * @return duration as HH:MM format
     */
    public static String stringDuration(int time) {
        int hours = time / 60;
        int minutes = time % 60;
        return String.format("%02dh%02d", hours, minutes);
    }

    private static Duration parseDuration(JSONObject jsonObject, String key)  {
        return parseDuration(jsonObject.getString(key));
    }

    private static Duration parseDuration(String text)  {
        return Duration.parse(String.format("PT%sM", text.toUpperCase()));
    }

    private static LocalTime parseLocalTime(JSONObject jsonObject, String key)  {
        return parseLocalTime( jsonObject.getString(key));
    }

    private static LocalTime parseLocalTime(String text) {
        return LocalTime.parse(text.replace('h', ':'));
    }

    public int dist(int i, int j) {
        return distMatrix[i][j];
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCoordType() {
        return coordType;
    }

    public boolean isSameVehicleBackward() {
        return sameVehicleBackward;
    }

    public Duration getMaxWaitTime() {
        return maxWaitTime;
    }

    public int[][] getDistMatrix() {
        return distMatrix;
    }

    public Place[] getPlaces() {
        return places;
    }

    public Vehicle[] getVehicles() {
        return vehicles;
    }

    public Patient[] getPatients() {
        return patients;
    }

    public int getSCALING() {
        return SCALING;
    }

    public int getNForwardTrips() {
        return nForwardTrips;
    }

    public int getNBackwardTrips() {
        return nBackwardTrips;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public static void main(String[] args) {
        new PTPInstance("data/ptp/easy/PTP-RAND-1_4_2_16.json");
    }

}
