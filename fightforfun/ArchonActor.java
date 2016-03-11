package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 10.03.2016.
 */
public class ArchonActor extends BaseActor {
    public ArchonActor(RobotController rc, int squadId) { super(rc, squadId); blockedTurnCounter = 0; }

    private int blockedTurnCounter;

    @Override
    protected void act() throws GameActionException {
        if(rc.isCoreReady() && rc.hasBuildRequirements(RobotType.SOLDIER)){
            Direction randomDir = randomDirection();
            int cnt = 0;
            while((!rc.canBuild(randomDir, RobotType.SOLDIER)) && cnt<8){
                randomDir = randomDir.rotateLeft();
                cnt++;
            }

            if(cnt>=8)
                this.orderAll(++blockedTurnCounter * blockedTurnCounter, rc.getLocation());
            else {
                if(randomDir.isDiagonal() || !rc.hasBuildRequirements(RobotType.GUARD))
                    rc.build(randomDir, RobotType.SOLDIER);
                else if(rc.canBuild(randomDir, RobotType.GUARD))
                    rc.build(randomDir,RobotType.GUARD);
                blockedTurnCounter = 0;
            }
        }
    }
}
