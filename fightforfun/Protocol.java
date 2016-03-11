package fightforfun;

import battlecode.common.*;

/**
 * Created by MichalMojzik on 10.03.2016.
 */
public class Protocol {

    public enum Type
    {
        INITIALIZE,
        ORDER,
        KNOWLEDGE,
        UNKNOWN,

        PLAIN
    }

    private static final int TYPE_MASK = 0b00000011;
    private static final int TYPE_SHIFT = 30;
    private static final int LOCATION_X_SHIFT = 22;
    private static final int LOCATION_X_MASK = 0b11111111;
    private static final int LOCATION_Y_SHIFT = 14;
    private static final int LOCATION_Y_MASK = 0b11111111;
    private static final int SQUAD_OR_ROBOT_MASK = 0b1;
    private static final int SQUAD_OR_ROBOT_SHIFT = 13;
    private static final int ROBOT_ID_MASK = 0b00011111_11111111;
    private static final int ROBOT_ID_SHIFT = 0;
    private static final int SQUAD_ID_MASK = ROBOT_ID_MASK;
    private static final int SQUAD_ID_SHIFT = ROBOT_ID_SHIFT;

    public static int prepareSignalType(Type type) {
        return type.ordinal() << TYPE_SHIFT;
    }
    public static int prepareBroadcastSignalData() {
        return SQUAD_ID_MASK | (1 << SQUAD_OR_ROBOT_MASK);
    }
    public static int prepareSquadIDSignalData(int squadId) {
        return (squadId << SQUAD_ID_MASK) | (1 << SQUAD_OR_ROBOT_MASK);
    }
    public static int prepareRobotIDSignalData(int robotId) {
        return (robotId << ROBOT_ID_SHIFT);
    }
    public static int prepareMapLocationSignalData(MapLocation location) {
        return ((location.x & LOCATION_X_MASK) << LOCATION_X_SHIFT) | ((location.y & LOCATION_Y_MASK) << LOCATION_Y_SHIFT);
    }

    public static Type getSignalType(Signal s)
    {
        int[] contents = s.getMessage();
        return contents == null ? Type.PLAIN : getSignalType(contents[0]);
    }
    public static Type getSignalType(int firstWord)
    {
        return Type.values()[(firstWord >>> TYPE_SHIFT) & TYPE_MASK];
    }
    public static int getSignalDataSquadId(int firstWord) {
        return (firstWord >>> SQUAD_ID_SHIFT) & SQUAD_ID_MASK;
    }
    public static int getSignalDataRobotId(int firstWord) {
        return (firstWord >>> ROBOT_ID_SHIFT) & ROBOT_ID_MASK;
    }
    public static int getBroadcastSquadId() {
        return SQUAD_ID_MASK;
    }
    public static MapLocation getSignalDataMapLocation(int firstWord, MapLocation reference) {
        return new MapLocation(((firstWord >>> LOCATION_X_SHIFT) & LOCATION_X_MASK) + (reference.x & ~LOCATION_X_MASK), ((firstWord >>> LOCATION_Y_SHIFT) & LOCATION_Y_MASK) + (reference.y & ~LOCATION_Y_MASK));
    }
    public static boolean isSquadIdSignalData(int firstWord)
    {
        return ((firstWord >>> SQUAD_OR_ROBOT_SHIFT) & SQUAD_OR_ROBOT_MASK) == 1;
    }
}
