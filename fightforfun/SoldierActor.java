package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 11.03.2016.
 */
public class SoldierActor extends CombatantActor {
    private int inactiveTurns;
    private static final int INACTIVE_TURNS_THRESHOLD = 80;
    private static final int INACTIVE_TURNS_MOVEMENT = 20;
    public SoldierActor(RobotController rc, int squadId)
    {
        super(rc, squadId);
        inactiveTurns = 0;
    }

    @Override
    protected int backUpAttackRange() {
        return 2;
    }

    @Override
    protected void act() throws GameActionException {
        super.act();

        if (inactiveTurns++ < 0) {
            this.randomMove();
        } else if (currentState != State.IDLE) {
            inactiveTurns = 0;
        } else {
            if (inactiveTurns == INACTIVE_TURNS_THRESHOLD) {
                inactiveTurns = -INACTIVE_TURNS_MOVEMENT;
                this.randomMove();
            }
        }
    }
}
