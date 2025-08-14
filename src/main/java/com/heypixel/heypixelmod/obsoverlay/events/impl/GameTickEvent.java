package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public class GameTickEvent implements Event {
    // 可以加属性，比如 tick 数
    private final int tickCount;

    public GameTickEvent(int tickCount) {
        this.tickCount = tickCount;
    }

    public int getTickCount() {
        return tickCount;
    }
}