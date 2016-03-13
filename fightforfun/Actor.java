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
                    return new GuardActor(rc, 0);
                case TURRET:
                    return new TurretActor(rc, 0);
                case SOLDIER:
                    return new SoldierActor(rc, 0);
                case VIPER:
                	return new ViperActor(rc, 0);
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
