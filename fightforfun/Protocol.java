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

    public enum Order
    {
        ATTACK,
        PROTECT,
        SCATTER,
        ADVANCE,
    }

    private static int MAX_MAP_SIZE = 80;

    private static final int TYPE_SHIFT = 30;
    private static final int TYPE_MASK = 0b11;
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

    private static final int ORDER_MASK = 0b11;
    private static final int ORDER_SHIFT = 30;

    public static int prepareSignalType(Type type) {
        return (type.ordinal() & TYPE_MASK) << TYPE_SHIFT;
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
    public static int prepareOrderSignalPayload(Order order) {
        return (order.ordinal() & ORDER_MASK) << ORDER_SHIFT;
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
    private static int clampCoordinate(int coord, int reference) {
        if(coord - reference >= MAX_MAP_SIZE)
            coord -= LOCATION_X_MASK + 1;
        if(coord - reference <= -MAX_MAP_SIZE)
            coord += LOCATION_X_MASK + 1;
        return coord;
    }
    public static MapLocation getSignalDataMapLocation(int firstWord, MapLocation reference) {
        final int x = ((firstWord >>> LOCATION_X_SHIFT) & LOCATION_X_MASK) | (reference.x & ~LOCATION_X_MASK);
        final int y = ((firstWord >>> LOCATION_Y_SHIFT) & LOCATION_Y_MASK) | (reference.y & ~LOCATION_Y_MASK);
        final int clampedX = clampCoordinate(x, reference.x);
        final int clampedY = clampCoordinate(y, reference.y);
        return new MapLocation(clampedX, clampedY);
    }
    public static boolean isSquadIdSignalData(int firstWord)
    {
        return ((firstWord >>> SQUAD_OR_ROBOT_SHIFT) & SQUAD_OR_ROBOT_MASK) == 1;
    }
    public static Order getSignalOrder(int secondWord)
    {
        return Order.values()[(secondWord >>> ORDER_SHIFT) & ORDER_MASK];
    }
}
