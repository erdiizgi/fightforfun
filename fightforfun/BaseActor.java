package fightforfun;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Created by MichalMojzik on 10.03.2016.
 */

public abstract class BaseActor {
    protected Random rng;
    protected RobotController rc;
    private int squadId;
    protected Direction[] directions = {
    		Direction.NORTH, 
    		Direction.NORTH_EAST, 
    		Direction.EAST, 
    		Direction.SOUTH_EAST,
            Direction.SOUTH, 
            Direction.SOUTH_WEST, 
            Direction.WEST, 
            Direction.NORTH_WEST};
    
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
    protected void orderAll(int distanceSquared, MapLocation location) throws GameActionException { this.orderAll(distanceSquared, location, Protocol.Order.SCATTER); }
    protected void orderAll(int distanceSquared, MapLocation location, Protocol.Order order) throws GameActionException {
        this.rc.setIndicatorString(1, "Ordering all.");
        this.rc.broadcastMessageSignal(
                Protocol.prepareSignalType(Protocol.Type.ORDER)
                | Protocol.prepareMapLocationSignalData(location)
                | Protocol.prepareBroadcastSignalData(),
                Protocol.prepareOrderSignalPayload(order),
                distanceSquared);
    }
    protected void orderSquad(int distanceSquared, int squadId, MapLocation location) throws GameActionException { this.orderSquad(distanceSquared, squadId, location, Protocol.Order.SCATTER); }
    protected void orderSquad(int distanceSquared, int squadId, MapLocation location, Protocol.Order order) throws GameActionException {
        this.rc.setIndicatorString(1, "Ordering squad " + squadId + ".");
        this.rc.broadcastMessageSignal(
                Protocol.prepareSignalType(Protocol.Type.ORDER)
                | Protocol.prepareMapLocationSignalData(location)
                | Protocol.prepareSquadIDSignalData(squadId),
                Protocol.prepareOrderSignalPayload(order),
                distanceSquared);
    }
    protected void orderRobot(int distanceSquared, int robotId, MapLocation location) throws GameActionException { this.orderRobot(distanceSquared, robotId, location, Protocol.Order.SCATTER); }
    protected void orderRobot(int distanceSquared, int robotId, MapLocation location, Protocol.Order order) throws GameActionException {
        this.rc.setIndicatorString(1, "Ordering robot " + robotId + ".");
        this.rc.broadcastMessageSignal(
                Protocol.prepareSignalType(Protocol.Type.ORDER)
                | Protocol.prepareMapLocationSignalData(location)
                | Protocol.prepareRobotIDSignalData(robotId),
                Protocol.prepareOrderSignalPayload(order),
                distanceSquared);
    }

    protected void pinged(int robotId, MapLocation from) throws GameActionException {}
    protected void ordered(int robotId, MapLocation where) throws GameActionException { this.rc.setIndicatorString(0, "Being ordered by " + robotId + "."); }
    protected void orderedToScatter(int robotId, MapLocation fromWhere) throws GameActionException { this.ordered(robotId, fromWhere); }
    protected void orderedToProtect(int robotId, MapLocation where) throws GameActionException { this.ordered(robotId, where); }
    protected void orderedToAttack(int robotId, MapLocation where) throws GameActionException { this.ordered(robotId, where); }

    protected void init() throws GameActionException {}
    protected void flap() throws GameActionException {}
    protected void act() throws GameActionException {}
    protected void cut() throws GameActionException {}

    private void signalProcessing() throws GameActionException
    {
        for(Signal s : this.rc.emptySignalQueue()) {
            if(s.getTeam() == rc.getTeam()) {
                int[] contents = s.getMessage();
                if(contents == null) {
                    this.rc.setIndicatorString(0, "Pinged by " + s.getRobotID() + ".");
                    this.pinged(s.getRobotID(), s.getLocation());
                } else {
                    switch(Protocol.getSignalType(contents[0]))
                    {
                        case INITIALIZE:
                            if(this.isTargeted(contents[0])) {
                                this.rc.setIndicatorString(0, "Received initialization signal.");
                                this.squadId = contents[1];
                            }
                            break;
                        case ORDER:
                            if(this.isTargeted(contents[0])) {
                                final MapLocation location = Protocol.getSignalDataMapLocation(contents[0], rc.getLocation());
                                switch(Protocol.getSignalOrder(contents[1]))
                                {
                                    case SCATTER:
                                        this.rc.setIndicatorString(0, "Received scatter away from " + location + " order from " + s.getRobotID() + ".");
                                        this.orderedToScatter(s.getRobotID(), location);
                                        break;
                                    case PROTECT:
                                        this.rc.setIndicatorString(0, "Received protect " + location + " order from " + s.getRobotID() + ".");
                                        this.orderedToProtect(s.getRobotID(), location);
                                        break;
                                    case ATTACK:
                                        this.rc.setIndicatorString(0, "Received attack " + location + " order from " + s.getRobotID() + ".");
                                        this.orderedToAttack(s.getRobotID(), location);
                                        break;
                                    default:
                                        this.rc.setIndicatorString(0, "Received unknown order from " + s.getRobotID() + ".");
                                        this.ordered(s.getRobotID(), location);
                                }
                            }
                            break;
                        case KNOWLEDGE:
                            this.rc.setIndicatorString(0, "Received knowledge.");
                            break;
                        default:
                            this.rc.setIndicatorString(0, "Received unknown signal " + (contents[0] >>> 30) + ".");
                            /* unknown signal type */
                            break;
                    }
                }
            }
        }
    }

    public void loop() {
        try {
            this.init();

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

    protected boolean blockedByRubble(Direction direction) {
        return rc.senseRubble(rc.getLocation().add(direction)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH;
    }
    public boolean canMove() {
        return rc.isCoreReady();
    }
    public boolean move(Direction direction, boolean canClear) throws GameActionException {
        if(!canMove())
            return false;

        Direction movementDirection;
        if(rc.canMove(movementDirection = direction) ||
                rc.canMove(movementDirection = direction.rotateLeft()) ||
                rc.canMove(movementDirection = direction.rotateRight())
        ) {
            rc.move(movementDirection);
            return true;
        } else {
            if (canClear)
                clearRubble(direction);
            return false;
        }
    }
    public boolean clearRubble(Direction direction) throws GameActionException {
        if(!rc.isCoreReady())
            return false;

        Direction clearingDirection;
        if( blockedByRubble(clearingDirection = direction) ||
            blockedByRubble(clearingDirection = direction.rotateLeft()) ||
            blockedByRubble(clearingDirection = direction.rotateRight())
        ) {
            rc.clearRubble(clearingDirection);
            return true;
        } else {
            return false;
        }
    }

    public RobotInfo[] getNearbyHostiles(boolean preferZombies)
    {
        return this.sortEnemies(rc.senseHostileRobots(rc.getLocation(), rc.getType().sensorRadiusSquared), preferZombies);
    }
    public RobotInfo[] sortEnemies(RobotInfo[] enemies, boolean preferZombies)
    {
        final boolean zombiesPreferred = preferZombies;
        Arrays.sort(enemies, new Comparator<RobotInfo>() {
            @Override
            public int compare(RobotInfo o1, RobotInfo o2) {
                if(zombiesPreferred) {
                    if(o1.team == Team.ZOMBIE) {
                        if (o2.team != Team.ZOMBIE) {
                            return -1;
                        }
                    } else if (o2.team == Team.ZOMBIE) {
                        return 1;
                    }
                }
                return
                    rc.getLocation().distanceSquaredTo(o2.location)
                  - rc.getLocation().distanceSquaredTo(o1.location);
            }
        });
        return enemies;
    }

    public boolean canAttack() {
        return rc.isWeaponReady();
    }
    public boolean attack(RobotInfo enemy) throws GameActionException {
        if(!canAttack())
            return false;

        if(rc.canAttackLocation(enemy.location)) {
            rc.attackLocation(enemy.location);
            return true;
        }
        else
            return false;
    }
}
