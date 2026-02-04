package net.azisaba.simplepoint;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public class PointAddEvent extends Event { // ★ここが重要
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID uuid;
    private final String id;
    private final int amount;

    public PointAddEvent(UUID uuid, String id, int amount) {
        this.uuid = uuid;
        this.id = id;
        this.amount = amount;
    }

    public UUID getUuid() { return uuid; }
    public String getId() { return id; }
    public int getAmount() { return amount; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}