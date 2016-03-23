package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */
public class CombatantActor extends BaseActor {
    public CombatantActor(RobotController rc, int squadId) { super(rc, squadId); }

    protected boolean prefersZombies() {
        return false;
    }

    protected int optimalAttackRangeStart() {
        return rc.getType().attackRadiusSquared / 2;
    }
    protected int optimalAttackRangeEnd() {
        return rc.getType().attackRadiusSquared;
    }
    protected int backUpAttackRange() {
        return 0;
    }

    protected enum State {
        NORMAL,
        IDLE,
        BACKING_UP
    }

    protected State currentState;

    private Direction scatterDirection;

    private static final int APPROCH_FAIL_THRESHOLD = 3;
    private int approachFailedCounter = 0;
    private MapLocation approachLocation;
    private int retransmitPauseCountdown = 0;
    private static final int RETRANSMIT_PAUSE = 10;

    @Override
    protected void init() throws GameActionException {
        this.currentState = State.NORMAL;
    }

    @Override
    protected void pinged(int robotId, MapLocation from) throws GameActionException {
        if(approachLocation == null) {
            approachLocation = from;
            if(retransmitPauseCountdown <= 0) {
                this.ping(rc.getType().attackRadiusSquared);
                retransmitPauseCountdown = RETRANSMIT_PAUSE;
            }
        }
    }

    @Override
    protected void orderedToScatter(int robotId, MapLocation fromWhere) throws GameActionException {
        scatterDirection = fromWhere.directionTo(rc.getLocation());
        approachLocation = null;
    }

    @Override
    protected void orderedToAdvance(int robotId, MapLocation where) throws GameActionException {
        rc.setIndicatorString(1, "Advancing to " + where);
        approachLocation = where;
    }

    @Override
    protected void act() throws GameActionException {

        if(retransmitPauseCountdown > 0)
            --retransmitPauseCountdown;

        RobotInfo[] enemies = this.getNearbyHostiles(prefersZombies());
        if(scatterDirection != null) {
            if (this.move(scatterDirection, false)) {
                scatterDirection = null;
            }
        }

        for (RobotInfo enemy : enemies) {
            int distanceSquared = enemy.location.distanceSquaredTo(rc.getLocation());
            if (distanceSquared < backUpAttackRange() || (distanceSquared < optimalAttackRangeStart() && currentState == State.BACKING_UP)) {
                if(this.move(enemy.location.directionTo(rc.getLocation()), false)) {
                    currentState = State.BACKING_UP;
                }
            }
        }
        if(this.canMove() && currentState == State.BACKING_UP)
            currentState = State.NORMAL;

        if(this.canAttack()) {
            for (RobotInfo enemy : enemies) {
                if (this.attack(enemy)) {
                    break;
                }
            }
        }

        if(this.canMove()) {
            for (RobotInfo enemy : enemies) {
                int distanceSquared = enemy.location.distanceSquaredTo(rc.getLocation());
                Direction movementDirection;

                if (distanceSquared > optimalAttackRangeEnd())
                    movementDirection = rc.getLocation().directionTo(enemy.location);
                else
                    break;

                if (this.move(movementDirection, true)) {
                    break;
                }
            }
        }

        if(scatterDirection != null) {
            this.clearRubble(scatterDirection);
            currentState = State.IDLE;
        } else if(currentState == State.NORMAL || currentState == State.IDLE) {
            if(enemies.length > 0) {
                currentState = State.NORMAL;
            } else {
                currentState = State.IDLE;
            }
        }

        if(enemies.length > 0) {
            this.ping(rc.getType().sensorRadiusSquared);
        }

        if(currentState == State.IDLE && approachLocation != null && this.canMove()) {
            Direction approachDirection = rc.getLocation().directionTo(approachLocation);
            int distanceBefore = rc.getLocation().distanceSquaredTo(approachLocation);
            if(this.move(approachDirection, false)) {
                if(approachFailedCounter > 0)
                    --approachFailedCounter;
                int distanceAfter = rc.getLocation().distanceSquaredTo(approachLocation);
                if(distanceBefore < distanceAfter) {
                    approachLocation = null;
                }
            }
            else
            {
                if(!this.clearRubble(approachDirection)) {
                    if(++this.approachFailedCounter > APPROCH_FAIL_THRESHOLD) {
                        approachFailedCounter = 0;
                        approachLocation = null;
                    }
                }
            }
            currentState = State.NORMAL;
        }

        rc.setIndicatorString(2, currentState.toString());
    }
}
