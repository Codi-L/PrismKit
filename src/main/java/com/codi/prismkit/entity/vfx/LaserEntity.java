package com.codi.prismkit.entity.vfx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class LaserEntity extends Entity {
    
    private static final EntityDataAccessor<Integer> DATA_DURATION = 
            SynchedEntityData.defineId(LaserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_DURATION =
            SynchedEntityData.defineId(LaserEntity.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<Float> DATA_LASER_HEIGHT = 
            SynchedEntityData.defineId(LaserEntity.class, EntityDataSerializers.FLOAT);
    
    public LaserEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DURATION, 40);
        this.entityData.define(DATA_MAX_DURATION, 40);
        this.entityData.define(DATA_LASER_HEIGHT, 50.0f);
    }

    public void setDuration(int ticks) {
        this.entityData.set(DATA_DURATION, ticks);
    }

    public void setMaxDuration(int ticks) {
        this.entityData.set(DATA_MAX_DURATION, ticks);
        setDuration(ticks);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    public int getMaxDuration() {
        return this.entityData.get(DATA_MAX_DURATION);
    }

    public void setLaserHeight(double height) {
        this.entityData.set(DATA_LASER_HEIGHT, (float) height);
    }

    public double getLaserHeight() {
        return this.entityData.get(DATA_LASER_HEIGHT);
    }

    @Override
    public void tick() {
        super.tick();
        
        int duration = this.getDuration();
        duration--;
        
        if (duration <= 0) {
            this.discard();
        } else {
            this.setDuration(duration);
        }
        
        this.refreshDimensions();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB normalBox = this.getBoundingBox();
        double height = this.getLaserHeight();
        return normalBox.inflate(2.0, 0, 2.0)
                        .expandTowards(0, height, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Duration")) {
            this.setMaxDuration(tag.getInt("Duration"));
        }
        if (tag.contains("LaserHeight")) {
            this.setLaserHeight(tag.getDouble("LaserHeight"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Duration", this.getDuration());
        tag.putDouble("LaserHeight", this.getLaserHeight());
    }
}
