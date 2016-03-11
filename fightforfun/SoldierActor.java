package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */
public class SoldierActor extends BaseActor {
    public SoldierActor(RobotController rc, int squadId) { super(rc, squadId); }

    private MapLocation scatterLocation;

    @Override
    protected void ordered(int robotId, MapLocation where) throws GameActionException {
        //Archon can't build any more item, scatter
        scatterLocation = where;
        rc.setIndicatorString(2, "Moving away from " + where.x + "," + where.y);
        super.ordered(robotId, where);
    }

    @Override
    protected void act() throws GameActionException {
        //Get the list of enemies nearby
        RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), rc.getType().sensorRadiusSquared);

        //If there is an enemy
        if(enemyArray.length>0){
            if(rc.isWeaponReady()){
                //look for adjacent enemies to attack
                for(RobotInfo oneEnemy:enemyArray){
                    if(rc.canAttackLocation(oneEnemy.location)){
                        rc.setIndicatorString(0,"trying to attack");
                        rc.attackLocation(oneEnemy.location);
                        break;
                    }
                }
            }

            //could not find any enemies adjacent to attack
            //try to move toward them
            if(rc.isCoreReady()){
                MapLocation goal = enemyArray[0].location;
                Direction toEnemy = rc.getLocation().directionTo(goal);
                if(rc.canMove(toEnemy)){
                    rc.setIndicatorString(0,"moving to enemy");
                    rc.move(toEnemy);
                }else{
                    MapLocation ahead = rc.getLocation().add(toEnemy);
                    if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
                        rc.clearRubble(toEnemy);
                    }
                }
            }
        }
        else if(this.scatterLocation != null && rc.isCoreReady()){
            Direction direction;
            if(rc.canMove(direction = this.scatterLocation.directionTo(rc.getLocation()))
                    || rc.canMove(direction = this.scatterLocation.directionTo(rc.getLocation()).rotateLeft())
                    || rc.canMove(direction = this.scatterLocation.directionTo(rc.getLocation()).rotateRight())
                    ) {
                rc.move(direction);
                this.scatterLocation = null;
            }
        }
    }
}
