package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */
public class SoldierActor extends BaseActor {
    private int unactiveTurns;
    private final int unactiveTurnsTreshold = 80;
    public SoldierActor(RobotController rc, int squadId) { super(rc, squadId); }

    private Direction scatterDirection;

    @Override
    protected void orderedToScatter(int robotId, MapLocation fromWhere) throws GameActionException {
        scatterDirection = fromWhere.directionTo(rc.getLocation());
    }

    @Override
    protected void init() throws GameActionException {
        super.init();
        unactiveTurns = 0;
    }



    @Override
    protected void act() throws GameActionException {

        ++unactiveTurns;

        RobotInfo[] enemies = this.getNearbyHostiles(false);
        if(scatterDirection != null)
            if(this.move(scatterDirection, false)) {
                unactiveTurns = 0;
                scatterDirection = null;
            }

        if(this.canAttack()) {
            for (RobotInfo enemy : enemies) {
                if (this.attack(enemy)) {
                    unactiveTurns = 0;
                    break;
                }
            }
        }

        if(this.canMove()) {
            for (RobotInfo enemy : enemies) {
                int distanceSquared = enemy.location.distanceSquaredTo(rc.getLocation());
                Direction movementDirection;
                if(distanceSquared >= rc.getType().attackRadiusSquared)
                    movementDirection = rc.getLocation().directionTo(enemy.location);
                else if(distanceSquared < rc.getType().attackRadiusSquared / 2)
                    movementDirection = enemy.location.directionTo(rc.getLocation());
                else
                    break;

                if (this.move(movementDirection, true)) {
                    unactiveTurns = 0;
                    break;
                }
            }
        }

        if(scatterDirection != null) {
            this.clearRubble(scatterDirection);
            unactiveTurns = 0;
        }

        if(unactiveTurns > unactiveTurnsTreshold){
            unactiveTurns = 0;
            this.randomMove();
        }

        if(unactiveTurns > 1) {
            this.rc.setIndicatorString(0, "Unactive for" + unactiveTurns + "turns.");
        }
    }
}
