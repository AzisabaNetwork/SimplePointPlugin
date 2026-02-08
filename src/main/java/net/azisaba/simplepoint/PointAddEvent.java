package net.azisaba.simplepoint;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class PointAddEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID uuid;
    private final String pointId; // ポイントID (例: event_point)
    private final int amount;     // 加算量

    public PointAddEvent(UUID uuid, String pointId, int amount) {
        this.uuid = uuid;
        this.pointId = pointId;
        this.amount = amount;
    }

    public UUID getUuid() {
        return uuid;
    }

    // ★これが不足していたメソッドです
    public String getPointId() {
        return pointId;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}