package net.mehvahdjukaar.moonlight.core;

import net.mehvahdjukaar.moonlight.api.MoonlightRegistry;
import net.mehvahdjukaar.moonlight.api.events.IDropItemOnDeathEvent;
import net.mehvahdjukaar.moonlight.api.events.MoonlightEventsHelper;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidRegistry;
import net.mehvahdjukaar.moonlight.api.integration.CompatWoodTypes;
import net.mehvahdjukaar.moonlight.api.item.additional_placements.AdditionalItemPlacementsAPI;
import net.mehvahdjukaar.moonlight.api.map.MapDecorationRegistry;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.misc.RegistryAccessJsonReloadListener;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicResourcePack;
import net.mehvahdjukaar.moonlight.api.set.BlockSetAPI;
import net.mehvahdjukaar.moonlight.api.set.leaves.LeavesTypeRegistry;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodTypeRegistry;
import net.mehvahdjukaar.moonlight.core.misc.VillagerAIInternal;
import net.mehvahdjukaar.moonlight.core.network.ModMessages;
import net.mehvahdjukaar.moonlight.core.set.BlockSetInternal;
import net.mehvahdjukaar.moonlight.core.set.BlocksColorInternal;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Moonlight {

    public static final String MOD_ID = "moonlight";

    public static final Logger LOGGER = LogManager.getLogger("Moonlight");
    public static final boolean HAS_BEEN_INIT = true;

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    //called on mod creation
    public static void commonInit() {
        BlockSetInternal.registerBlockSetDefinition(WoodTypeRegistry.INSTANCE);
        BlockSetInternal.registerBlockSetDefinition(LeavesTypeRegistry.INSTANCE);
        //MoonlightEventsHelper.addListener( BlockSetInternal::addTranslations, AfterLanguageLoadEvent.class);
        CompatWoodTypes.init();
        MoonlightRegistry.init();

        ModMessages.registerMessages();

        VillagerAIInternal.init();
        MapDecorationRegistry.init();
        SoftFluidRegistry.init();

        PlatHelper.addCommonSetup(Moonlight::commonSetup);

        //hack
        BlockSetAPI.addDynamicRegistration((reg, wood) -> AdditionalItemPlacementsAPI.afterItemReg(),
                WoodType.class, BuiltInRegistries.BLOCK_ENTITY_TYPE);
    }
    //TODO: refactor respriter and palette to better acocunt for animated stuff

    private static void commonSetup() {
        BlocksColorInternal.setup();
    }

    @EventCalled
    public static void onPlayerCloned(Player oldPlayer, Player newPlayer, boolean wasDeath) {
        if (wasDeath && !oldPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            var inv = oldPlayer.getInventory();
            int i = 0;
            for (var v : inv.items) {
                if (v != ItemStack.EMPTY) {
                    IDropItemOnDeathEvent e = IDropItemOnDeathEvent.create(v, oldPlayer, false);
                    MoonlightEventsHelper.postEvent(e, IDropItemOnDeathEvent.class);
                    if (e.isCanceled()) {
                        newPlayer.getInventory().setItem(i, e.getReturnItemStack());
                    }
                }
                i++;
            }
        }
    }

    @EventCalled
    public static void afterDataReload(RegistryAccess registryAccess) {
        RegistryAccessJsonReloadListener.runReloads(registryAccess);
        DynamicResourcePack.clearAfterReload(false);
    }

    public static void assertInitPhase() {
        if (!PlatHelper.isInitializing()) {
            //TODO: re add once all mods are updated
            if (PlatHelper.isDev()) {
                throw new AssertionError("Method has to be called during mod initialization phase");
            }
        }
    }

}
