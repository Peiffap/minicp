package minicp.examples;

import java.io.FileWriter;
import java.util.ArrayList;

public class MakeGraphFile {
    public static void writeGraphFile(int nVehicles, DialARide.RideStop depot, ArrayList<DialARide.RideStop> pickupRideStops, ArrayList<DialARide.RideStop> dropRideStops, int[] xBest){
        try {
            FileWriter f = new FileWriter("out");
            f.write(nVehicles + "\n");
            f.write("depot " + depot.pos_x + " " + depot.pos_y + "\n");
            for (int i = 0; i < pickupRideStops.size(); i++)
                f.write("pickup " + i +" " + pickupRideStops.get(i).pos_x + " " + pickupRideStops.get(i).pos_y + "\n");
            for (int i = 0; i < dropRideStops.size(); i++)
                f.write("drop " + i +" " + dropRideStops.get(i).pos_x + " " + dropRideStops.get(i).pos_y + "\n");
            for (int i = 0; i < nVehicles; i++) {
                int currentPos = i;
                while(xBest[currentPos] >= nVehicles){
                    f.write(i + " " + currentPos + " " + xBest[currentPos] + "\n");
                    currentPos = xBest[currentPos];
                }
                f.write(i + " " + currentPos + " " + xBest[currentPos] + "\n");
            }
            f.close();
        } catch (Exception e) {

        }
    }
}