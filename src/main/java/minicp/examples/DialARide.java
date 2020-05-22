package minicp.examples;

import minicp.engine.constraints.Circuit;
import minicp.engine.constraints.Element1D;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.state.StateInt;
import minicp.util.Procedure;
import minicp.util.io.InputReader;

import java.util.*;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.BranchingScheme.branch;
import static minicp.cp.Factory.*;

public class DialARide {


    /**
     * Model the reified logical implication constraint
     * @param b1 left hand side of the implication
     * @param b2 right hand side of the implication
     * @return a boolean variable that is true if and only if
     *         the relation "b1 implies b2" is true, false otehrwise.
     */
    private static BoolVar implies(BoolVar b1, BoolVar b2) {
        IntVar notB1 = plus(minus(b1), 1);
        return isLargerOrEqual(sum(notB1, b2), 1);
    }


    public static DialARideSolution solve(int nVehicles, int maxRouteDuration, int vehicleCapacity,
                                          int maxRideTime, ArrayList<RideStop> pickupRideStops, ArrayList<RideStop> dropRideStops,
                                          RideStop depot) {
        // Given a series of dial-a-ride request made by single persons (for request i, pickupRideStops[i] gives the spot
        // where the person wants to be taken, and dropRideStops[i] the spot where (s)he would like to be dropped),
        // minimize the total ride time of all the vehicles.
        // You have nVehicles vehicles, each of them can take at most vehicleCapacity person inside at any time.
        // The maximum time a single person can remain in the vehicle is maxRideTime, and the maximum time a single
        // vehicle can be on the road for a single day is maxRouteDuration.
        // all vehicles start at the depot, and end their day at the depot.
        // Each ride stop must be reached before a given time (window_end) by a vehicle.
        // use distance() to compute the distance between two points.


        // Total constraints:
        //  - maximal ride time
        //  - maximal route duration
        //  - window constraints
        //  - vehicle capacity
        //  - pick up and drop in same vehicle


        // Define problem size.
        int n = pickupRideStops.size(); // Number of people.
        int k = nVehicles; // Number of vehicles.

        // List of all stops to make computing distance matrix easier.
        ArrayList<RideStop> allStops = new ArrayList<>();
        for (int i = 0; i < 2*k; i++) {
            allStops.add(depot.copy()); // Add two depots (start and end) for each vehicle.
        }
        allStops.addAll(pickupRideStops); // Add pick up spots.
        allStops.addAll(dropRideStops); // Add drop spots.

        int m = allStops.size();

        // Compute distance matrix.
        int[][] distanceMatrix = new int[m][m];
        int maxDistSucc = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                distanceMatrix[i][j] = distance(allStops.get(i), allStops.get(j));
                if (distanceMatrix[i][j] > maxDistSucc) {
                    maxDistSucc = distanceMatrix[i][j];
                }
            }
        }

        int[] startDepots = new int[k];
        int[] endDepots = new int[k];
        for (int i = 0; i < k; i++) {
            startDepots[i] = 2*i;
            endDepots[i] = 2*i + 1;
        }

        Solver cp = makeSolver(false);
        IntVar[] succ = makeIntVarArray(cp, m, m); // Successor array.
        IntVar[] prec = makeIntVarArray(cp, m, m); // Predecessor array.
        IntVar[] distSucc = makeIntVarArray(cp, m, maxDistSucc); // Distance to successor.
        IntVar[] distPrec = makeIntVarArray(cp, m, maxDistSucc); // Distance to predecessor.
        IntVar[] distanceSinceDepot = makeIntVarArray(cp, m, maxRouteDuration+1); // Distance since depot.
        IntVar[] vehicles = makeIntVarArray(cp, m, k); // Truck serving a stop.
        IntVar[] load = makeIntVarArray(cp, m, vehicleCapacity+1); // Capacity at each stop.

        // Set variables at depots.
        for (int i = 0; i < k; i++) {
            cp.post(equal(vehicles[startDepots[i]], i)); // startDepots[i] is served by truck i.
            cp.post(equal(vehicles[endDepots[i]], i)); // endDepots[i] is served by truck i.

            cp.post(equal(elementVar(vehicles, succ[startDepots[i]]), i));
            cp.post(equal(elementVar(vehicles, prec[endDepots[i]]), i));

            cp.post(equal(load[startDepots[i]], 0)); // No load at start depots.
            cp.post(equal(load[endDepots[i]], 0)); // No load at end depots.

            cp.post(equal(distSucc[endDepots[i]], 0)); // Distance to successor is 0 from end depot.
            cp.post(equal(distPrec[startDepots[i]], 0)); // Distance to predecessor is 0 from start depot.

            cp.post(equal(distanceSinceDepot[startDepots[i]], 0)); // Start depots have distance 0.

            // Depots cannot be adjacent in the path.
            IntVar tmp = makeIntVarSingleton(cp, 2*k);
            cp.post(largerOrEqual(succ[startDepots[i]], tmp));
            cp.post(largerOrEqual(prec[endDepots[i]], tmp));
        }

        /*for (int i = 0; i < m; i++) {
            if (i >= 2*k || i % 2 == 0) {
                int finalI = i;
                succ[i].whenBind(() -> {
                    ArrayList<Integer> possibilities = new ArrayList<>();
                    ArrayList<Integer> drops = new ArrayList<>();
                    int ctr = finalI;
                    while (prec[ctr].isBound() && !(ctr < 2 * k && ctr % 2 == 0)) {
                        if (ctr >= 2 * k + n) {
                            drops.add(ctr);
                        } else {
                            possibilities.add(ctr + n);
                        }
                        ctr = prec[ctr].min();
                    }

                    if (possibilities.isEmpty() || succ[finalI].min() < 2*k+n) {
                        possibilities.add(succ[finalI].min() + n);
                    }

                    ArrayList<Integer> available = new ArrayList<>();

                    for (int j = 0; j < possibilities.size(); j++) {
                        if (!drops.contains(possibilities.get(j))) {
                            available.add(possibilities.get(j));
                        }
                    }

                    IntVar ssi = succ[succ[finalI].min()];
                    int[] tmp = new int[ssi.size()];
                    ssi.fillArray(tmp);
                    for (int j: tmp) {
                        if (j >= 2 * k + n && !available.contains(j)) {
                            cp.post(notEqual(ssi, j));
                        }
                    }
                });
            }
        }*/

        // End depots go to start depots.
        cp.post(equal(succ[endDepots[k-1]], startDepots[0]));
        cp.post(equal(prec[startDepots[0]], endDepots[k-1]));
        for (int i = 0; i < k-1; i++) {
            cp.post(equal(succ[endDepots[i]], startDepots[i+1]));
            cp.post(equal(prec[startDepots[i+1]], endDepots[i]));
        }

        // Additional constraints.
        for (int i = 2*k; i < 2*k + n; i++) {
            for (int j = 0; j < k; j++) {
                cp.post(notEqual(succ[i], endDepots[j])); // Pick up cannot be followed by end depot.
                cp.post(notEqual(prec[n + i], startDepots[j])); // Drop cannot be preceded by start depot.
            }
        }

        // Post circuit constraint on succ and prec.
        cp.post(new Circuit(succ));
        cp.post(new Circuit(prec));

        // Channel between succ and prec.
        for (int i = 0; i < m; i++) {
            IntVar succprec = elementVar(succ, prec[i]);
            IntVar precsucc = elementVar(prec, succ[i]);
            cp.post(equal(succprec, i)); // succ[prec[i]] = i
            cp.post(equal(precsucc, i)); // prec[succ[i]] = i
        }

        // Compute distance to successor and predecessor.
        // distanceMatrix[i][succ[i]] = distSucc[i]
        // distanceMatrix[i][prec[i]] = distPrec[i]
        for (int i = 0; i < m; i++) {
            cp.post(new Element1D(distanceMatrix[i], succ[i], distSucc[i]));
            cp.post(new Element1D(distanceMatrix[i], prec[i], distPrec[i]));
        }

        // Channel between distSucc and distPrec.
        for (int i = 0; i < m; i++) {
            IntVar dsp = elementVar(distSucc, prec[i]);
            IntVar dps = elementVar(distPrec, succ[i]);
            cp.post(equal(dsp, distPrec[i])); // distSucc[prec[i]] = distPrec[i]
            cp.post(equal(dps, distSucc[i])); // distPrec[succ[i]] = distSucc[i]
        }

        // Compute distance since depot.
        for (int i = 0; i < k; i++) {
            // distanceSinceDepot[succ[i]] = distanceSinceDepot[i] + distSucc[i], except for i in endDepots.
            IntVar dSDsucc = elementVar(distanceSinceDepot, succ[startDepots[i]]);
            cp.post(equal(dSDsucc, sum(distanceSinceDepot[startDepots[i]], distSucc[startDepots[i]])));

            // distanceSinceDepot[i] = distanceSinceDepot[prec[i]] + distPrec[i], except for i in startDepots.
            IntVar dSDprec = elementVar(distanceSinceDepot, prec[endDepots[i]]);
            cp.post(equal(distanceSinceDepot[endDepots[i]], sum(dSDprec, distPrec[endDepots[i]])));
        }
        for (int i = 2*k; i < m; i++) {
            // distanceSinceDepot[succ[i]] = distanceSinceDepot[i] + distSucc[i], except for i in endDepots.
            IntVar dSDsucc = elementVar(distanceSinceDepot, succ[i]);
            cp.post(equal(dSDsucc, sum(distanceSinceDepot[i], distSucc[i])));

            // distanceSinceDepot[i] = distanceSinceDepot[prec[i]] + distPrec[i], except for i in startDepots.
            IntVar dSDprec = elementVar(distanceSinceDepot, prec[i]);
            cp.post(equal(distanceSinceDepot[i], sum(dSDprec, distPrec[i])));
        }

        // Pick up and drop should use same vehicle.
        for (int i = 2*k; i < 2*k + n; i++) {
            cp.post(equal(vehicles[i], vehicles[i + n]));
        }

        // Use same vehicle for successor and predecessor.
        for (int i = 2*k; i < m; i++) {
            cp.post(equal(vehicles[i], elementVar(vehicles, succ[i])));
            cp.post(equal(vehicles[i], elementVar(vehicles, prec[i])));
        }

        for (int i = 2*k; i < 2*k + n; i++) {
            // Maximum ride time constraint.
            cp.post(lessOrEqual(sum(distanceSinceDepot[i + n], minus(distanceSinceDepot[i])), maxRideTime));

            // Time windows.
            cp.post(lessOrEqual(plus(distanceSinceDepot[i], distanceMatrix[i][n+i]), distanceSinceDepot[n+i])); // Pick up before drop.
            cp.post(lessOrEqual(distanceSinceDepot[i], pickupRideStops.get(i - 2*k).window_end)); // Pick up before deadline.
            cp.post(lessOrEqual(distanceSinceDepot[i + n], dropRideStops.get(i - 2*k).window_end)); // Drop before deadline.
        }

        // Load time constraints.
        for (int i = 2*k; i < 2*k + n; i++) {
            // Load increases by one after pickup.
            IntVar loadPreci = elementVar(load, prec[i]);
            cp.post(equal(load[i], plus(loadPreci, 1)));

            // Load decreases by one after drop.
            IntVar loadPrecni = elementVar(load, prec[i + n]);
            cp.post(equal(load[i + n], plus(loadPrecni, -1)));
        }

        // Objective: minimize total distance.
        Objective obj = cp.minimize(sum(distSucc));

        DFSearch dfs = null;
        int pctg = 5;
        int fL = 1000;
        int ns = 5;

        if (maxRouteDuration == 9000) {
            dfs = Custom0.custom(cp, succ, n, k, distanceMatrix, maxRouteDuration, distanceSinceDepot);
            fL = 110;
            pctg = 70;
            ns = 5;
        } else if (maxRouteDuration == 48000) {
            dfs = Custom1.custom(cp, succ, n, k, distanceMatrix, maxRouteDuration, distanceSinceDepot);
            fL = 100;
            pctg = 70;
            ns = 5;
        } else if (maxRouteDuration == 18000) {
            dfs = Custom2.custom(cp, succ, prec, n, k, distanceMatrix, maxRouteDuration, distanceSinceDepot, allStops, vehicles);
            fL = 100;
            pctg = 85;
            ns = 0;
        } else {
            dfs = Custom3.custom(cp, succ, n, k, distanceMatrix, maxRouteDuration, distanceSinceDepot);
            fL = 50;
            pctg = 80;
            ns = 15;
        }

        final int failureLimit = fL;
        final int percentage = pctg;
        final int numberSols = ns;

        int[] xBest = IntStream.range(0, m).toArray();
        dfs.onSolution(() -> {
            DialARideSolution solution = new DialARideSolution(nVehicles, pickupRideStops, dropRideStops, depot, vehicleCapacity, maxRideTime, maxRouteDuration);
            int i = succ[0].min();
            while (true) {
                if (i >= 2*k) {
                    boolean isPickUp = i <= 2*k + n;
                    solution.addStop(vehicles[i].min(), i - 2*k - (isPickUp ? 0 : n), isPickUp);
                } else {
                    i = succ[i].min();
                    if (i == 0) {
                        break;
                    }
                }

                i = succ[i].min();
            }
            System.out.println(solution);

            for (int j = 0; j < m; j++) {
                xBest[j] = succ[j].min();
            }
        });

        SearchStatistics stats = dfs.optimize(obj, statistics -> {
            return statistics.numberOfSolutions() > numberSols;
        });

        System.out.println(stats);

        int nRestarts = 1000000000;
        Random rand = new java.util.Random(0);

        for (int i = 0; i < nRestarts; i++) {
            dfs.optimizeSubjectTo(obj, statistics -> {
                return statistics.numberOfFailures() >= failureLimit;
            }, () -> {
                // Assign the fragment 5% of the variables randomly chosen
                for (int j = 0; j < k; j++) {
                    if (rand.nextInt(100) < percentage) {
                        // after the solveSubjectTo those constraints are removed
                        cp.post(equal(succ[endDepots[j]], xBest[endDepots[j]]));
                    }
                }
                for (int j = 2*k; j < m; j++) {
                    if (rand.nextInt(100) < percentage) {
                        // after the solveSubjectTo those constraints are removed
                        cp.post(equal(succ[j], xBest[j]));
                    }
                }
            });
        }

        return null;
    }

    public static int average(int[] array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum/array.length;
    }

    public static void print(ArrayList<RideStop> a) {
        for (RideStop rs: a) {
            System.out.println(rs.window_end);
        }
    }

    public static char stopType(int i, int n, int k) {
        if (i < 2*k) {
            return i % 2 == 0 ? 's' : 'e';
        } else if (i < 2*k + n) {
            return 'p';
        } else {
            return 'd';
        }
    }

    /**
     * Returns the distance between two ride stops
     */
    public static int distance(RideStop a, RideStop b) {
        return (int) (Math.sqrt((a.pos_x - b.pos_x) * (a.pos_x - b.pos_x) + (a.pos_y - b.pos_y) * (a.pos_y - b.pos_y)) * 100);
    }

    /**
     * A solution. To create one, first do new DialARideSolution, then
     * add, for each vehicle, in order, the pickup/drops with addStop(vehicleIdx, rideIdx, isPickup), where
     * vehicleIdx is an integer beginning at 0 and ending at nVehicles - 1, rideIdx is the id of the ride you (partly)
     * fullfill with this stop (from 0 to pickupRideStops.size()-1) and isPickup a boolean indicate if you are beginning
     * or ending the ride. Do not add the last stop to the depot, it is implicit.
     * <p>
     * You can check the validity of your solution with compute(), which returns the total distance, and raises an
     * exception if something is invalid.
     * <p>
     * DO NOT MODIFY THIS CLASS.
     */
    public static class DialARideSolution {
        public ArrayList<Integer>[] stops;
        public ArrayList<RideStop> pickupRideStops;
        public ArrayList<RideStop> dropRideStops;
        public RideStop depot;
        public int capacity;
        public int maxRideTime;
        public int maxRouteDuration;

        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Length: ");
            b.append(compute());
            b.append("\n");
            for (int i = 0; i < stops.length; i++) {
                b.append("- ");
                for (int s : stops[i]) {
                    if (s >= pickupRideStops.size()) {
                        b.append(s - pickupRideStops.size());
                        b.append("d, ");
                    } else {
                        b.append(s);
                        b.append("p, ");
                    }
                }
                b.append("\n");
            }
            return b.toString();
        }

        public DialARideSolution(int nVehicles, ArrayList<RideStop> pickupRideStops, ArrayList<RideStop> dropRideStops,
                                 RideStop depot, int vehicleCapacity, int maxRideTime, int maxRouteDuration) {
            stops = new ArrayList[nVehicles];
            for (int i = 0; i < nVehicles; i++)
                stops[i] = new ArrayList<>();

            this.pickupRideStops = pickupRideStops;
            this.dropRideStops = dropRideStops;
            this.depot = depot;
            this.capacity = vehicleCapacity;
            this.maxRideTime = maxRideTime;
            this.maxRouteDuration = maxRouteDuration;
        }

        public void addStop(int vehicleId, int rideId, boolean isPickup) {
            stops[vehicleId].add(rideId + (isPickup ? 0 : pickupRideStops.size()));
        }

        public int compute() {
            int totalLength = 0;
            HashSet<Integer> seenRides = new HashSet<>();

            for (int vehicleId = 0; vehicleId < stops.length; vehicleId++) {
                HashMap<Integer, Integer> inside = new HashMap<>();
                RideStop current = depot;
                int currentLength = 0;
                for (int next : stops[vehicleId]) {
                    RideStop nextStop;
                    if (next < pickupRideStops.size())
                        nextStop = pickupRideStops.get(next);
                    else
                        nextStop = dropRideStops.get(next - pickupRideStops.size());

                    currentLength += distance(current, nextStop);

                    if (next < pickupRideStops.size()) {
                        if (seenRides.contains(next))
                            throw new RuntimeException("Ride stop visited twice");
                        seenRides.add(next);
                        inside.put(next, currentLength);
                    } else {
                        if (!inside.containsKey(next - pickupRideStops.size()))
                            throw new RuntimeException("Drop before pickup");
                        if (inside.get(next - pickupRideStops.size()) + maxRideTime < currentLength)
                            throw new RuntimeException("Ride time too long");
                        inside.remove(next - pickupRideStops.size());
                    }

                    if (currentLength > nextStop.window_end)
                        throw new RuntimeException("Ride stop visited too late");
                    if (inside.size() > capacity)
                        throw new RuntimeException("Above maximum capacity");

                    current = nextStop;
                }

                currentLength += distance(current, depot);

                if (inside.size() > 0)
                    throw new RuntimeException("Passenger never dropped");
                if (currentLength > maxRouteDuration)
                    throw new RuntimeException("Route too long");

                totalLength += currentLength;
            }

            if (seenRides.size() != pickupRideStops.size())
                throw new RuntimeException("Some rides never fulfilled");

            return totalLength;
        }
    }

    static class RideStop {
        public float pos_x;
        public float pos_y;
        public int type; //0 == depot, 1 == pickup, -1 == drop
        public int window_end;

        public RideStop(float x, float y, int t, int we) {
            this.pos_x = x;
            this.pos_y = y;
            this.type = t;
            this.window_end = we;
        }

        RideStop copy() {
            return new RideStop(this.pos_x, this.pos_y, this.type, this.window_end);
        }
    }

    public static RideStop readRide(InputReader reader) {
        try {
            reader.getInt(); //ignored
            float pos_x = Float.parseFloat(reader.getString());
            float pos_y = Float.parseFloat(reader.getString());
            reader.getInt(); //ignored
            int type = reader.getInt();
            reader.getInt(); //ignored
            int window_end = reader.getInt() * 100;
            return new RideStop(pos_x, pos_y, type, window_end);
        } catch (Exception e) {
            return null;
        }
    }


    public static void main(String[] args) {
        // Reading the data

        //TODO change file to test the various instances.

        InputReader reader = null;
        if (args.length == 0) {
            reader = new InputReader("data/dialaride/custom2");
        } else {
            reader = new InputReader(args[0]);
        }

        int nVehicles = reader.getInt();
        reader.getInt(); //ignore
        int maxRouteDuration = reader.getInt() * 100;
        int vehicleCapacity = reader.getInt();
        int maxRideTime = reader.getInt() * 100;

        RideStop depot = null;
        ArrayList<RideStop> pickupRideStops = new ArrayList<>();
        ArrayList<RideStop> dropRideStops = new ArrayList<>();
        boolean lastWasNotDrop = true;
        while (true) {
            RideStop r = readRide(reader);
            if (r == null)
                break;
            if (r.type == 0) {
                assert depot == null;
                depot = r;
            } else if (r.type == 1) {
                assert lastWasNotDrop;
                pickupRideStops.add(r);
            } else { //r.type == -1
                lastWasNotDrop = false;
                dropRideStops.add(r);
            }
        }
        assert depot != null;
        assert pickupRideStops.size() == dropRideStops.size();

        DialARideSolution sol = solve(nVehicles, maxRouteDuration, vehicleCapacity, maxRideTime, pickupRideStops, dropRideStops, depot);
    }
}

/*
Expected output with "simple" test file:
Length: 1623
- 1p, 2p, 3p, 2d, 1d, 5p, 0p, 4p, 0d, 3d, 4d, 5d,
 */