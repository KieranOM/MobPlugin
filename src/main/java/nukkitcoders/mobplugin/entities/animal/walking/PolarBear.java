package nukkitcoders.mobplugin.entities.animal.walking;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PolarBear extends WalkingMonster {

    public static final int NETWORK_ID = 28;

    private int angry = 0;

    public PolarBear(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public String getName() {
        return "Polar Bear";
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.65f;
        }
        return 1.3f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.7f;
        }
        return 1.4f;
    }

    @Override
    public double getSpeed() {
        return 1.25;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setDamage(new float[] { 0, 4, 6, 9 });
        this.setMaxHealth(30);
        if (this.namedTag.contains("Angry")) {
            this.angry = this.namedTag.getInt("Angry");
        }
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        this.namedTag.putInt("Angry", this.angry);
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        if (distance <= 100 && this.isAngry() && creature instanceof PolarBear && !((PolarBear) creature).isAngry()) {
            ((PolarBear) creature).setAngry(1000);
        }
        return this.isAngry() && super.targetOption(creature, distance);
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.attackDelay > 23 && this.distanceSquared(player) < 1.44) {
            this.attackDelay = 0;
            HashMap<EntityDamageEvent.DamageModifier, Float> damage = new HashMap<>();
            damage.put(EntityDamageEvent.DamageModifier.BASE, this.getDamage());

            if (player instanceof Player) {
                @SuppressWarnings("serial")
                HashMap<Integer, Float> armorValues = new HashMap<Integer, Float>() {

                    {
                        put(Item.LEATHER_CAP, 1f);
                        put(Item.LEATHER_TUNIC, 3f);
                        put(Item.LEATHER_PANTS, 2f);
                        put(Item.LEATHER_BOOTS, 1f);
                        put(Item.CHAIN_HELMET, 1f);
                        put(Item.CHAIN_CHESTPLATE, 5f);
                        put(Item.CHAIN_LEGGINGS, 4f);
                        put(Item.CHAIN_BOOTS, 1f);
                        put(Item.GOLD_HELMET, 1f);
                        put(Item.GOLD_CHESTPLATE, 5f);
                        put(Item.GOLD_LEGGINGS, 3f);
                        put(Item.GOLD_BOOTS, 1f);
                        put(Item.IRON_HELMET, 2f);
                        put(Item.IRON_CHESTPLATE, 6f);
                        put(Item.IRON_LEGGINGS, 5f);
                        put(Item.IRON_BOOTS, 2f);
                        put(Item.DIAMOND_HELMET, 3f);
                        put(Item.DIAMOND_CHESTPLATE, 8f);
                        put(Item.DIAMOND_LEGGINGS, 6f);
                        put(Item.DIAMOND_BOOTS, 3f);
                    }
                };

                float points = 0;
                Item[] items = ((Player) player).getInventory().getArmorContents();
                for (int i = 0; i < items.length; ++i) {
                    Item item = items[i];
                    points += armorValues.getOrDefault(item.getId(), 0f);
                }

                damage.put(EntityDamageEvent.DamageModifier.ARMOR,
                        (float) (damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0f) - Math.floor(damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1f) * points * 0.04)));
            }
            player.attack(new EntityDamageByEntityEvent(this, player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage));
        }
    }

    public boolean isAngry() {
        return this.angry > 0;
    }

    public void setAngry(int val) {
        this.angry = val;
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        super.attack(ev);

        if (!ev.isCancelled() && ev instanceof EntityDamageByEntityEvent) {
            if (((EntityDamageByEntityEvent) ev).getDamager() instanceof Player) {
                this.setAngry(1000);
            }
        }

        return true;
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        if (this.lastDamageCause instanceof EntityDamageByEntityEvent && !this.isBaby()) {
            drops.add(Item.get(Item.RAW_FISH, 0, Utils.rand(0, 2)));
            drops.add(Item.get(Item.RAW_SALMON, 0, Utils.rand(0, 2)));
        }

        return drops.toArray(new Item[0]);
    }

    @Override
    public int getKillExperience() {
        return this.isBaby() ? 0 : Utils.rand(1, 3);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.angry > 0) {
            this.angry--;
        }

        return super.entityBaseTick(tickDiff);
    }
}
