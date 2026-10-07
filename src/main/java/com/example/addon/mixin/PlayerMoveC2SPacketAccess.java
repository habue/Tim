package com.example.addon.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public interface PlayerMoveC2SPacketAccess {
    void setOnGround(boolean onGround);

    void setPitch(float pitch);

    void setYaw(float yaw);

    void setCause(Cause cause);

    Cause getCause();

    static ServerboundMovePlayerPacket setCause(ServerboundMovePlayerPacket packet, Cause cause) {
        ((PlayerMoveC2SPacketAccess) packet).setCause(cause);
        return packet;
    }

    static ServerboundMovePlayerPacket setCauseFrom(ServerboundMovePlayerPacket packet, ServerboundMovePlayerPacket packet2) {
        return setCause(packet, ((PlayerMoveC2SPacketAccess) packet2).getCause());
    }

    static PlayerMoveC2SPacketAccess of(ServerboundMovePlayerPacket packet) {
        return (PlayerMoveC2SPacketAccess) packet;
    }

    enum Cause {
        SET_BACK,
        PLAYER_MOVEMENT,
        HACKING_PACKETS,
        LEGACY_SNAP,
        TRIGGER_SIMULATION
    }
}
