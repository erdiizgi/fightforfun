package fightforfun;

import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController unit){

		BaseActor actor = Actor.CastRobot(unit);
		if(actor == null)
			return;
		actor.loop();

		unit.setIndicatorString(2, "!! Robot ended its run() method. !!");
		while(true) { Clock.yield(); }
	}
}
