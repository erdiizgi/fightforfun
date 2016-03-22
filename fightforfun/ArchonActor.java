package fightforfun;

import battlecode.common.*;
import scala.Int;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by MichalMojzik on 10.03.2016.
 */
public class ArchonActor extends BaseActor {
    public ArchonActor(RobotController rc, int squadId) { super(rc, squadId);
		blockedTurnCounter = 0; behaviour = BehaviourMode.BUILD; }

    private int blockedTurnCounter;

	private final int dyingHealth = 100;

	private enum BehaviourMode{
		BUILD, ROAM
	}
	private BehaviourMode behaviour;
	private Collection<UnitInfo> otherArchons;
	private UnitInfo mainArchon;


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
		switch(behaviour){
			case BUILD:// if we are building... then :
				if(rc.isCoreReady() && rc.hasBuildRequirements(RobotType.SOLDIER)) {
					Direction randomDir = randomDirection();
					int cnt = 0;
					while ((!rc.canBuild(randomDir, RobotType.SOLDIER)) && cnt < 8) {
						randomDir = randomDir.rotateLeft();
						cnt++;
					}

					if (cnt >= 8)
						this.orderAll(++blockedTurnCounter * blockedTurnCounter, rc.getLocation());
					else {
						if (randomDir.isDiagonal() || !rc.hasBuildRequirements(RobotType.GUARD))
							rc.build(randomDir, RobotType.GUARD);
						else if (rc.canBuild(randomDir, RobotType.SOLDIER))
							rc.build(randomDir, RobotType.SOLDIER);
						blockedTurnCounter = 0;
					}
				}
				break;
			case ROAM:
				// i ll try to move away from enemies if I see them:
				RobotInfo[] enemies = this.getNearbyHostiles(false);

				if(enemies.length > 0 ){
					Direction movementDirection = rc.getLocation().directionTo(enemies[0].location);
					movementDirection = movementDirection.opposite();
					if (this.move(movementDirection, true)) {

					}
				}
				break;
		}
		this.tryRepairAlly();
		this.tryConvertNeutrals();
		this.tryMove();
        /*if(rc.isCoreReady() && rc.hasBuildRequirements(RobotType.SOLDIER)){
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
        this.tryMove();*/
    }

	@Override
	protected void init() throws GameActionException
	{
		mainArchon = new UnitInfo(rc.getID(), rc.getLocation());// until we know other weare mainArchon :D
		otherArchons = new ArrayList<UnitInfo>();



		// send message to other archons with ours id
		rc.broadcastMessageSignal(Protocol.prepareSignalType(Protocol.Type.KNOWLEDGE), 0, this.maxDistanceSquared(rc.getInitialArchonLocations(rc.getTeam())));

	}

	@Override
	protected void cut() throws GameActionException {
		super.cut();
		if(rc.getHealth() < dyingHealth){
			// send message to other archons with ours id
			rc.broadcastMessageSignal(Protocol.prepareSignalType(Protocol.Type.KNOWLEDGE), 0, 80*80);
			this.behaviour = BehaviourMode.ROAM;
		}
	}

	private int maxDistanceSquared(MapLocation[] locations){
		int max = Integer.MIN_VALUE;
		for (MapLocation loc : locations) {
			int currentDist = rc.getLocation().distanceSquaredTo(loc);
			if(currentDist > max){
				max = currentDist;
			}
		}
		return max;
	}

	@Override
	protected void recievedKnowledge(Signal signal) throws GameActionException {
		// if I receive knowledge of archon ID
		UnitInfo newArchon = new UnitInfo(signal);

		if(otherArchons.contains(newArchon)){
			// archon died...
			otherArchons.remove(newArchon);
		}else {
			otherArchons.add(newArchon);
		}

		// if I receive knowledge of dying archon
			// decrease archon count
			// remove given archon from archonList

		/// IF WE HAVE MINIMAL ID FROM ARCHONS:
			UnitInfo minArchon = Collections.min(otherArchons);
			if(rc.getID() <= minArchon.id){// we have minimal ID;
				behaviour = BehaviourMode.BUILD;
				this.rc.setIndicatorString(0, "Assigned as main archon.");
				mainArchon = new UnitInfo(rc.getID(), rc.getLocation());
			}else{
				behaviour = BehaviourMode.ROAM;
				this.rc.setIndicatorString(0, "Assigned as roaming archon.");
				mainArchon = minArchon;
			}
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
    
    /*//TODO randommove is not so good, because it moves around, we should use signals from scouts
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
    }*/
    
    private void tryConvertNeutrals() throws GameActionException {
		if (!rc.isCoreReady()) return;
		RobotInfo[] adjacentNeutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		for (RobotInfo neutral : adjacentNeutrals) {
			rc.activate(neutral.location);
			return;
		}
	}
}
