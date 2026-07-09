package com.cipollomods.utilityareas;

import com.cipollomods.utilityareas.command.UACommand;
import com.cipollomods.utilityareas.data.AreaPersistenceManager;
import com.cipollomods.utilityareas.data.LootGroupManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Clase principal del mod UtilityAreas.
 * Gestiona la inicialización del mod y el registro de comandos.
 */
@Mod(UtilityAreas.MOD_ID)
@Mod.EventBusSubscriber(modid = UtilityAreas.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UtilityAreas {

    public static final String MOD_ID = "utilityareas";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UtilityAreas() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("UtilityAreas initialized!");
    }

    /**
     * Registra los comandos del mod en el dispatcher de Brigadier.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        UACommand.register(event.getDispatcher());
    }

    /**
     * Carga las áreas guardadas en areas.json cuando el servidor arranca,
     * antes de que empiece a procesar jugadores o ticks.
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LootGroupManager.getInstance().load();
        AreaPersistenceManager.getInstance().load();
    }
}