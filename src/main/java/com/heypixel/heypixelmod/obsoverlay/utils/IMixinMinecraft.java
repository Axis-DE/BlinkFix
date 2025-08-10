package com.heypixel.heypixelmod.obsoverlay.utils;

public interface IMixinMinecraft {
   int getSkipTicks();

   void setSkipTicks(int var1);

   void processSkippedTick();
}
