package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */

public class GuardActor extends BaseActor {
    public GuardActor(RobotController rc, int squadId) { super(rc, squadId); }

    private Direction scatterDirection;

    @Override
    protected void orderedToScatter(int robotId, MapLocation fromWhere) throws GameActionException {
        scatterDirection = fromWhere.directionTo(rc.getLocation());
    }

    @Override
    protected void act() throws GameActionException {

        RobotInfo[] enemies = this.getNearbyHostiles(true);
        if(scatterDirection != null)
            if(this.move(scatterDirection, false))
                scatterDirection = null;

        if(this.canAttack()) {
            for (RobotInfo enemy : enemies) {
                if (this.attack(enemy))
                    break;
            }
        }

        if(this.canMove()) {
            for (RobotInfo enemy : enemies) {
                int distanceSquared = enemy.location.distanceSquaredTo(rc.getLocation());
                Direction movementDirection;
                if(distanceSquared > rc.getType().attackRadiusSquared)
                    movementDirection = rc.getLocation().directionTo(enemy.location);
                else if(distanceSquared <= rc.getType().attackRadiusSquared)
                    movementDirection = enemy.location.directionTo(rc.getLocation());
                else
                    break;

                if (this.move(movementDirection, true))
                    break;
            }
        }

        if(scatterDirection != null)
            this.clearRubble(scatterDirection);
    }
}
