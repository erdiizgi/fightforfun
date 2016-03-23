package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */

public class GuardActor extends CombatantActor {
    public GuardActor(RobotController rc, int squadId) { super(rc, squadId); }

    int followedArchonID;

    @Override
    protected void init() throws GameActionException {
        super.init();

        RobotInfo[] closeRobots = rc.senseNearbyRobots(2, rc.getTeam());
        for(RobotInfo archonCandidate : closeRobots) {
            if(archonCandidate.type == RobotType.ARCHON) {
                followedArchonID = archonCandidate.ID;
                break;
            }
        }
    }

    @Override
    protected int optimalAttackRangeStart() {
        return 0;
    }

    @Override
    protected boolean prefersZombies() {
        return true;
    }
}
