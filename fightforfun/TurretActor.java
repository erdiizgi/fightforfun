package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */
public class TurretActor extends BaseActor {
    public TurretActor(RobotController rc, int squadId) { super(rc, squadId); }

    @Override
    protected void act() throws GameActionException {
        //Get the list of enemies nearby
        RobotInfo[] enemyArray = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared,Team.ZOMBIE);

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
        }
    }
}
