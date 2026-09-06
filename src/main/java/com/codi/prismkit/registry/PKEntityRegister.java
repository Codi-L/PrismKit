package com.codi.prismkit.registry;

import com.codi.prismkit.PrismKit;
import com.codi.prismkit.entity.vfx.LaserEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PKEntityRegister {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, PrismKit.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<LaserEntity>> LASER =
            ENTITY_TYPES.register("laser", () -> EntityType.Builder.<LaserEntity>of(
                            LaserEntity::new,
                            MobCategory.MISC
                    )
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(PrismKit.MOD_ID + ":laser"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
