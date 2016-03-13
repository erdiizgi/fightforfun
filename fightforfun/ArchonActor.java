package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 10.03.2016.
 */
public class ArchonActor extends BaseActor {
    public ArchonActor(RobotController rc, int squadId) { super(rc, squadId); blockedTurnCounter = 0; }

    private int blockedTurnCounter;
    protected static Direction[] directions = {
    		Direction.NORTH, 
    		Direction.NORTH_EAST, 
    		Direction.EAST, 
    		Direction.SOUTH_EAST,
            Direction.SOUTH, 
            Direction.SOUTH_WEST, 
            Direction.WEST, 
            Direction.NORTH_WEST};

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
                    rc.build(randomDir, RobotType.GUARD);
                else if(rc.canBuild(randomDir, RobotType.SOLDIER))
                    rc.build(randomDir,RobotType.SOLDIER);
                blockedTurnCounter = 0;
            }
        }
        
        this.tryRepairAlly();
        this.tryConvertNeutrals();
        this.tryMove();
    }
    
    private void tryRepairAlly() throws GameActionException {
		RobotInfo[] healableAllies = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, rc.getTeam());
		MapLocation bestLoc = null;
		double lowestHealth = 10000;
		for (RobotInfo ally : healableAllies) {
			if (ally.type == RobotType.ARCHON) continue;
			if (ally.health < ally.maxHealth && ally.health < lowestHealth) {
				bestLoc = ally.location;
				lowestHealth = ally.health;
			}
		}
		if (bestLoc != null) {
			rc.repair(bestLoc);
		}
	}
    
    private void tryMove() throws GameActionException{
    	this.exploreNeutrals();
    	this.exploreParts();
    	this.randomMove();    	
    }
    
    private void exploreParts(){
		if (!rc.isCoreReady()) return;
		MapLocation[] adjacentParts = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
		System.out.println(adjacentParts.length);
			int min = 10000;
			MapLocation bestPartLoc = null;
	
		for(MapLocation part: adjacentParts){
			if(part.distanceSquaredTo(rc.getLocation()) < min)
				bestPartLoc = part;
		}
	
		if(bestPartLoc != null){
			if (rc.canMove(rc.getLocation().directionTo(bestPartLoc)) && rc.isCoreReady()) {
					try {
						rc.move(rc.getLocation().directionTo(bestPartLoc));
					} catch (GameActionException e) {
						e.printStackTrace();
					}
			}	
		}	
    }
    
    private void exploreNeutrals(){
    	if (!rc.isCoreReady()) return;
    		RobotInfo[] adjacentNeutrals = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.NEUTRAL);
    	int min = 10000;
    	MapLocation bestNeutralLoc = null;
    	
    	for(RobotInfo r : adjacentNeutrals){
    		if(r.location.distanceSquaredTo(rc.getLocation()) < min)
    			bestNeutralLoc = r.location;
    	}

	    if(bestNeutralLoc != null){
	    	if (rc.canMove(rc.getLocation().directionTo(bestNeutralLoc)) && rc.isCoreReady()) {
	    		try {
	    			rc.move(rc.getLocation().directionTo(bestNeutralLoc));
	    		} catch (GameActionException e) {
	    			e.printStackTrace();
	    		}
	    	}	
	    }
    }
    
    //TODO randommove is not so good, because it moves around, we should use signals from scouts
    private void randomMove(){
    	if(Math.random() < 0.5){
			Direction d = directions[(int) (8 * Math.random())];
			if (rc.canMove(d) && rc.isCoreReady()) {
				try {
					rc.move(d);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
    }
    
    private void tryConvertNeutrals() throws GameActionException {
		if (!rc.isCoreReady()) return;
		RobotInfo[] adjacentNeutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		for (RobotInfo neutral : adjacentNeutrals) {
			rc.activate(neutral.location);
			return;
		}
	}
}
