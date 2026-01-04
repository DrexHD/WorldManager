package me.drex.worldmanager.save;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public class PortalBehaviour {
    public static final Identifier NETHER_PORTAL_ID = Identifier.withDefaultNamespace("nether_portal");
    public static final Identifier END_PORTAL_ID = Identifier.withDefaultNamespace("end_portal");
    private static final Set<Identifier> PORTALS = new HashSet<>();

    public static void init() {
        registerPortal(NETHER_PORTAL_ID);
        registerPortal(END_PORTAL_ID);
    }

    public static void registerPortal(Identifier id) {
        PORTALS.add(id);
    }

    public static Set<Identifier> getPortals() {
        return PORTALS;
    }
}
