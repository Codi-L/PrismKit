package com.codi.prismkit.registry;

import com.codi.prismkit.PrismKit;
import com.codi.prismkit.entity.vfx.LaserEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class PKEntityRegister {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PrismKit.MOD_ID);

    public static final RegistryObject<EntityType<LaserEntity>> LASER =
            ENTITY_TYPES.register("laser", () -> EntityType.Builder.<LaserEntity>of(
                    LaserEntity::new, 
                    MobCategory.MISC
            )
            .sized(1.0f, 1.0f)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build("laser"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
