package fightforfun;

import battlecode.common.*;
import java.util.Random;

/**
 * Created by MichalMojzik on 10.03.2016.
 */

public abstract class BaseActor {
    protected Random rng;
    protected RobotController rc;
    private int squadId;
    protected BaseActor(RobotController rc, int squadId) {
        this.rc = rc;
        this.rng = new Random(rc.getID());
        this.squadId = squadId;
    }

    public int getSquadId() {
        return this.squadId;
    }
    protected Direction randomDirection() {
        return Direction.values()[(int)(rng.nextDouble()*8)];
    }

    private boolean isTargeted(int firstWord) {
        if(Protocol.isSquadIdSignalData(firstWord)) {
            return Protocol.getSignalDataRobotId(firstWord) == this.rc.getID();
        } else {
            int squadId = Protocol.getSignalDataSquadId(firstWord);
            return squadId == this.getSquadId() || squadId == Protocol.getBroadcastSquadId();
        }
    }

    protected void ping(int distanceSquared) throws GameActionException {
        this.rc.broadcastSignal(distanceSquared);
    }
    protected void orderAll(int distanceSquared, MapLocation location) throws GameActionException {
        this.rc.setIndicatorString(0, "Ordering all.");
        this.rc.broadcastMessageSignal(
                Protocol.prepareSignalType(Protocol.Type.ORDER)
                | Protocol.prepareMapLocationSignalData(location)
                | Protocol.prepareBroadcastSignalData(),
                0,
                distanceSquared);
    }
    protected void orderSquad(int distanceSquared, int squadId, MapLocation location) throws GameActionException {
        this.rc.setIndicatorString(0, "Ordering squad " + squadId + ".");
        this.rc.broadcastMessageSignal(
                Protocol.prepareSignalType(Protocol.Type.ORDER)
                | Protocol.prepareMapLocationSignalData(location)
                | Protocol.prepareSquadIDSignalData(squadId),
                0,
                distanceSquared);
    }
    protected void orderRobot(int distanceSquared, int robotId, MapLocation location) throws GameActionException {
        this.rc.setIndicatorString(0, "Ordering robot " + robotId + ".");
        this.rc.broadcastMessageSignal(
                Protocol.prepareSignalType(Protocol.Type.ORDER)
                | Protocol.prepareMapLocationSignalData(location)
                | Protocol.prepareRobotIDSignalData(robotId),
                0,
                distanceSquared);
    }

    protected void pinged(int robotId, MapLocation from) throws GameActionException {}
    protected void ordered(int robotId, MapLocation where) throws GameActionException { this.rc.setIndicatorString(0, "Being ordered by " + robotId + "."); }

    protected void flap() throws GameActionException {}
    protected void act() throws GameActionException {}
    protected void cut() throws GameActionException {}

    private void signalProcessing() throws GameActionException
    {
        for(Signal s : this.rc.emptySignalQueue()) {
            if(s.getTeam() == rc.getTeam()) {
                int[] contents = s.getMessage();
                if(contents == null) {
                    this.pinged(s.getRobotID(), s.getLocation());
                } else {
                    switch(Protocol.getSignalType(contents[0]))
                    {
                        case INITIALIZE:
                            this.rc.setIndicatorString(1, "Received initialization signal.");
                            if(this.isTargeted(contents[0]))
                                this.squadId = contents[1];
                            break;
                        case ORDER:
                            this.rc.setIndicatorString(1, "Received order signal.");
                            if(this.isTargeted(contents[0]))
                                this.ordered(s.getRobotID(), Protocol.getSignalDataMapLocation(contents[0], rc.getLocation()));
                            break;
                        case KNOWLEDGE:
                            this.rc.setIndicatorString(1, "Received knowledge signal.");
                            break;
                        default:
                            this.rc.setIndicatorString(1, "Received unknown signal " + (contents[0] >>> 30) + ".");
                            /* unknown signal type */
                            break;
                    }
                }
            }
        }
    }

    public void loop() {
        try {
            while(true)
            {
                //this.rc.setIndicatorString(0, "Looping...");

                this.signalProcessing();

                this.flap();
                this.act();
                this.cut();

                Clock.yield();
            }
        }catch(GameActionException actionException) {
            actionException.printStackTrace();
        }catch(Exception regularException) {
            regularException.printStackTrace();
        }
    }
}
