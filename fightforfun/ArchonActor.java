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

	private static final int CALLOUT_MOVEMENT_THRESHOLD = 20;
	private int movedCounter = 0;
	public boolean move(Direction direction, boolean canClear) throws GameActionException {
		if(super.move(direction, canClear)) {
			++movedCounter;
			return true;
		} else {
			return false;
		}
	}

	private int bigCalloutPauseCounter = 0;
	private static final int BIG_CALLOUT_PAUSE = 200;
	private static final int BIG_CALLOUT_PAUSE_CALLOUT_INCREMENT = 20;

	private int maxFriendliesSeenMult = 0;
	private static final int FRIENDLIES_SEEN_MULTIPLIER = 100;
	private static final int FRIENDLIES_SEEN_THRESHOLD_FRAC_MULT = 1;
	private static final int FRIENDLIES_SEEN_THRESHOLD_FRAC_DIV = 2;

	private final int dyingHealth = 50;
	private final int timidHealth = 300;
	private boolean underAttack = false;

	private enum BehaviourMode{
		BUILD, SUPPLEMENT_BUILD, ROAM
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

		this.exploreNeutrals();

		switch(behaviour) {
			case SUPPLEMENT_BUILD:
				if(!rc.hasBuildRequirements(RobotType.SOLDIER))
					behaviour = BehaviourMode.ROAM;
				break;
			case ROAM:
				if(rc.hasBuildRequirements(RobotType.SOLDIER)) {
					for (RobotInfo friendly : getNearbyFriendlies()) {
						if (friendly.type == RobotType.ARCHON) {
							behaviour = BehaviourMode.SUPPLEMENT_BUILD;
							break;
						}
					}
				}
			default:
				break;
		}
		switch(behaviour){
			case SUPPLEMENT_BUILD:
			case BUILD:// if we are building... then :
				if(rc.isCoreReady() && rc.hasBuildRequirements(RobotType.SOLDIER)) {
					Direction randomDir = randomDirection();
					int cnt = 0;
					while ((!rc.canBuild(randomDir, RobotType.SOLDIER)) && cnt < 8) {
						randomDir = randomDir.rotateLeft();
						cnt++;
					}

					if (cnt >= 8)
						this.orderAll(++blockedTurnCounter * blockedTurnCounter + 1, rc.getLocation());
					else {
						if (randomDir.isDiagonal() || !rc.hasBuildRequirements(RobotType.GUARD))
							rc.build(randomDir, RobotType.GUARD);
						else if (rc.canBuild(randomDir, RobotType.SOLDIER))
							rc.build(randomDir, RobotType.SOLDIER);
						blockedTurnCounter = 0;
					}
				}/* else {
					boolean isBlocked = true;
					for(Direction direction : Direction.values()) {
						RobotInfo robotAtDirection = rc.senseRobotAtLocation(rc.getLocation().add(direction));
						if(robotAtDirection == null) {
							isBlocked = false;
						}
					}

					if(isBlocked)
						this.orderAll(++blockedTurnCounter * blockedTurnCounter + 1, rc.getLocation());
				}*/
				break;
			case ROAM:
				RobotInfo[] enemies = this.getNearbyHostiles(false);
				if(rc.getHealth() < timidHealth || enemies.length > 5 || rc.isInfected()) {
					// i ll try to move away from enemies if I see them:

					if (enemies.length > 0) {
						Direction movementDirection = rc.getLocation().directionTo(enemies[0].location);
						movementDirection = movementDirection.opposite();
						if (this.move(movementDirection, false)) {

						}
					}
				}
				break;
		}

		if(!this.tryRepairAlly())
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
		bigCalloutPauseCounter = BIG_CALLOUT_PAUSE;

		mainArchon = null;
		if(rc.getRoundNum() == 0)
		{
			MapLocation[] initialLocations = rc.getInitialArchonLocations(rc.getTeam());
			if(initialLocations[initialLocations.length - 1].equals(rc.getLocation())) {
				behaviour = BehaviourMode.BUILD;
				this.rc.setIndicatorString(0, "Assigned as main archon.");
				mainArchon = new UnitInfo(rc.getID(), rc.getLocation());
				rc.broadcastMessageSignal(Protocol.prepareSignalType(Protocol.Type.KNOWLEDGE), 0, this.maxDistanceSquared(rc.getInitialArchonLocations(rc.getTeam())));
			}
			else if(initialLocations.length > 2 && initialLocations[0].equals(rc.getLocation()))
			{
				behaviour = BehaviourMode.SUPPLEMENT_BUILD;
				this.rc.setIndicatorString(0, "Assigned as supplemental archon.");
			}
			else {
				behaviour = BehaviourMode.ROAM;
				this.rc.setIndicatorString(0, "Assigned as roaming archon.");
			}
		}
		else {
			behaviour = BehaviourMode.ROAM;
			mainArchon = new UnitInfo(-1, rc.getLocation());
		}

		// send message to other archons with ours id

	}

	@Override
	protected void cut() throws GameActionException {
		super.cut();

		boolean callout = false;
		boolean bigCallout = false;

		RobotInfo[] seenHostiles = getNearbyHostiles(false);
		RobotInfo[] seenFriendlies = this.getNearbyFriendlies();
		if((seenHostiles.length > 0 && seenFriendlies.length < 5) || rc.isInfected())
		{
			callout = true;
			this.rc.setIndicatorString(1, rc.getRoundNum() + ": Calling army as we are attacked by zombies.");
		}

		if(behaviour == BehaviourMode.BUILD) {
			if (this.movedCounter > CALLOUT_MOVEMENT_THRESHOLD) {
				callout = true;
				this.rc.setIndicatorString(1, rc.getRoundNum() + ": Calling army as we have moved far.");

				this.movedCounter = 0;
			}

			if (maxFriendliesSeenMult > 0)
				--maxFriendliesSeenMult;
			int currentlySeenFriendlies = seenFriendlies.length * FRIENDLIES_SEEN_MULTIPLIER;
			if (maxFriendliesSeenMult < currentlySeenFriendlies)
				maxFriendliesSeenMult = currentlySeenFriendlies;
			else if (FRIENDLIES_SEEN_THRESHOLD_FRAC_MULT * maxFriendliesSeenMult / FRIENDLIES_SEEN_THRESHOLD_FRAC_DIV > currentlySeenFriendlies) {
				callout = true;
				this.rc.setIndicatorString(1, rc.getRoundNum() + ": Calling army as we feel lonely.");
			}

			if (!callout) {
				if (--bigCalloutPauseCounter <= 0) {
					callout = true;
					bigCallout = true;
					this.rc.setIndicatorString(1, rc.getRoundNum() + ": Calling annual army meeting. There are cupcakes.");
					bigCalloutPauseCounter = BIG_CALLOUT_PAUSE;
				}
			} else {
				if ((bigCalloutPauseCounter += BIG_CALLOUT_PAUSE_CALLOUT_INCREMENT) > BIG_CALLOUT_PAUSE) {
					bigCalloutPauseCounter = BIG_CALLOUT_PAUSE;
				}
			}

			this.rc.setIndicatorString(2, "Max seen number of friendlies: " + (maxFriendliesSeenMult / FRIENDLIES_SEEN_MULTIPLIER) + ", Big callout in: " + bigCalloutPauseCounter);
		}
		this.rc.setIndicatorString(0, "Type: " + behaviour.name());


		if(callout) {
			int radius = bigCallout ? 80 : behaviour == BehaviourMode.BUILD ? 15 : 5;
			this.orderAll(radius * radius, rc.getLocation(), Protocol.Order.ADVANCE);
		}

		if(letOthersKnowIamMain)
		{
			rc.broadcastMessageSignal(Protocol.prepareSignalType(Protocol.Type.KNOWLEDGE), 0, 80*80);
			letOthersKnowIamMain = false;
		}

		if(rc.getHealth() < dyingHealth && this.behaviour == BehaviourMode.BUILD){
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

	boolean letOthersKnowIamMain = false;
	@Override
	protected void recievedKnowledge(Signal signal) throws GameActionException {
		if(mainArchon == null)
		{
			mainArchon = new UnitInfo(signal);
			if(letOthersKnowIamMain)
				behaviour = BehaviourMode.ROAM;
		} else {
			mainArchon = new UnitInfo(rc.getID(), rc.getLocation());
			letOthersKnowIamMain = true;
			behaviour = BehaviourMode.BUILD;
		}
	}

	private boolean tryRepairAlly() throws GameActionException {
		RobotInfo[] healableAllies = this.getNearbyHealableFriendlies();
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
			try {
				rc.repair(bestLoc);
			} catch(Exception e) {}
			return true;
		}
		else
			return false;
	}
    
    private void tryMove() throws GameActionException{
    	this.exploreParts();
    	this.randomMove();
    }
    
    private void exploreParts() throws GameActionException {
		if (!rc.isCoreReady())
			return;

		MapLocation[] nearbyParts = this.getNearbyParts();

		for(MapLocation part: nearbyParts){
			if(this.blockedByRubble(part))
				continue;

			Direction directionToPart = rc.getLocation().directionTo(part);
			this.move(directionToPart, false);
		}
    }
    
    private void exploreNeutrals() throws GameActionException {
    	if (!this.canMove())
			return;

		RobotInfo[] neutrals = this.getNearbyNeutrals();
		if(neutrals.length > 0)
		{
			RobotInfo neutral = neutrals[0];
			if(neutral.location.isAdjacentTo(rc.getLocation())) {
				rc.activate(neutral.location);
			} else {
				this.move(rc.getLocation().directionTo(neutral.location), false);
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
}
