package fightforfun;

import battlecode.common.Signal;
import battlecode.common.MapLocation;
/**
 * Created by marci on 22/03/16.
 */
public class UnitInfo implements Comparable<UnitInfo> {
    public int id;
    public MapLocation location;
    public UnitInfo(Signal signal){
        id = signal.getRobotID();
        location = signal.getLocation();
    }
    public UnitInfo(int IDin, MapLocation locationIn){ id = IDin; location = locationIn;}

    @Override
    public int compareTo(UnitInfo o) {
        if(this.id == o.id) return 0;
        if (this.id > o.id) return 1;
        else return -1;
    }
}
