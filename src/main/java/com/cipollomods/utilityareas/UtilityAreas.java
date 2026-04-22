package com.cipollomods.utilityareas;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(UtilityAreas.MOD_ID)
public class UtilityAreas {

    public static final String MOD_ID = "utilityareas";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UtilityAreas() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("UtilityAreas initialized!");
    }
}