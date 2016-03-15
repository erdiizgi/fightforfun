package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 10.03.2016.
 */

public class Actor {
    public static BaseActor CastRobot(RobotController rc) {
        try {
            switch (rc.getType()) {
                case ARCHON:
                    return new ArchonActor(rc, 0);
                case GUARD:
                    return new GuardActor(rc, 1);
                case TURRET:
                    return new TurretActor(rc, 2);
                case SOLDIER:
                    return new SoldierActor(rc, 3);
                case VIPER:
                	return new ViperActor(rc, 4);
                case SCOUT:
                    return new ScoutActor(rc, 5);
                default:
                    rc.setIndicatorString(1, "!! Casting failed !!");
                    while(true) { Clock.yield(); }
            }
        //}catch(GameActionException actionException) {
        //    actionException.printStackTrace();
        }catch(Exception regularException) {
            regularException.printStackTrace();
            return null;
        }
    }
}
