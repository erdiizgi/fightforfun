package fightforfun;

import java.util.ArrayList;
import java.util.Random;
import battlecode.common.*;

public class RobotPlayer {
	
	static Random rnd;
	static RobotController rc;
	static int infinity = 10000;
	
	static int MOVE = 1234;
	
	iBehaviour behaviour;
	
	public static void run(RobotController unit){
		
		rc = unit;
		rnd = new Random(rc.getID());
		
		while(true){
			try{
				switch(unit.getType())
				{
					case ARCHON:
						ArchonLogic();
						break;
						
					case GUARD:
						GuardLogic();
						break;
						
					case SCOUT:
						ScoutLogic();
						break;
						
					case TURRET:
						TurretLogic();
						break;
						
					case SOLDIER:
						SoldierLogic();
						break;
						
					case VIPER:
						ViperLogic();
						break;
						
					default:
						System.out.println("The type couldn't matched");
						break;
				}
			
			}catch(Exception e){
				e.printStackTrace();
			}

			Clock.yield();
		}
	}
	
	//This part contains the logic for Archons
	public static void ArchonLogic() throws GameActionException{
		if(rc.isCoreReady()){
			Direction randomDir = randomDirection();
			int cnt = 0;
			while((!rc.canBuild(randomDir, RobotType.SOLDIER)) || cnt<8){
				randomDir = randomDirection();
				cnt++;
			}
			
			if(randomDir.isDiagonal() && rc.canBuild(randomDir, RobotType.SOLDIER) )
				rc.build(randomDir, RobotType.SOLDIER);
			else
				rc.build(randomDir,RobotType.GUARD);
			
			if(cnt>8)
				rc.broadcastMessageSignal(MOVE, MOVE, 5);
			
		}
	}
	
	//This part contains the logic for Guards
	public static void GuardLogic() throws GameActionException{
		
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
		
		else{
			readInstructions();
		}
		
	}
	
	//This part contains the logic for Scouts
	public static void ScoutLogic(){}
	
	//This part contains the logic for Turret
	public static void TurretLogic() throws GameActionException{
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
	
	//This part contains the logic for Soldiers
	public static void SoldierLogic() throws GameActionException{
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
	}
	
	//This part contains the logic for Vipers
	public static void ViperLogic(){}
	
	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
	private static void readInstructions() throws GameActionException {
		Signal[] signals = rc.emptySignalQueue();
		
		for (Signal s : signals) {
			if (s.getTeam() != rc.getTeam()) {
				continue;
			}
			
			if (s.getMessage() == null) {
				continue;
			}
			
			int command = s.getMessage()[0];
			
			//Archon can't build any more item
			if (command == MOVE) {
				Direction randomDir = randomDirection();
				if(rc.canMove(randomDir)){
					rc.move(randomDir);
				}
			} 
		}
	}
	

}
