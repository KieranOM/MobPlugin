package nukkitcoders.mobplugin.utils;

import cn.nukkit.level.Sound;
import cn.nukkit.level.Position;
import cn.nukkit.level.Level;
import cn.nukkit.level.Explosion;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockTNT;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.block.BlockUpdateEvent;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.math.*;
import cn.nukkit.network.protocol.ExplodePacket;
import cn.nukkit.utils.Hash;
import it.unimi.dsi.fastutil.longs.LongArraySet;

import java.util.ArrayList;
import java.util.List;

public class FireBallExplosion extends Explosion {

    private final Level level;
    private final Position source;
    private final double size;
    private final Object what;
    private List<Block> affectedBlocks = new ArrayList<>();

    public FireBallExplosion(Position center, double size, Entity what) {
        super(center, size, what);
        this.level = center.getLevel();
        this.source = center;
        this.size = Math.max(size, 0);
        this.what = what;
    }

    @Override
    public boolean explodeA() {
        if (this.size < 0.1) return false;
        Vector3 vector = new Vector3(0, 0, 0);
        Vector3 vBlock = new Vector3(0, 0, 0);
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15) {
                        vector.setComponents((double) i / (double) 15 * 2d - 1, (double) j / (double) 15 * 2d - 1, (double) k / (double) 15 * 2d - 1);
                        double len = vector.length();
                        vector.setComponents((vector.x / len) * 0.3d, (vector.y / len) * 0.3d, (vector.z / len) * 0.3d);
                        double pointerX = this.source.x;
                        double pointerY = this.source.y;
                        double pointerZ = this.source.z;
                        for (double blastForce = this.size * (Utils.random.nextInt(700, 1301)) / 1000d; blastForce > 0; blastForce -= 0.3d * 0.75d) {
                            int x = (int) pointerX;
                            int y = (int) pointerY;
                            int z = (int) pointerZ;
                            vBlock.x = pointerX >= x ? x : x - 1;
                            vBlock.y = pointerY >= y ? y : y - 1;
                            vBlock.z = pointerZ >= z ? z : z - 1;
                            if (vBlock.y < 0 || vBlock.y > 255) {
                                break;
                            }
                            Block block = this.level.getBlock(vBlock);
                            if (block.getId() != 0) {
                                blastForce -= (block.getResistance() / 5 + 0.3d) * 0.3d;
                                if (blastForce > 0) {
                                    if (block.getResistance() < 20) {
                                        if (!this.affectedBlocks.contains(block)) {
                                            this.affectedBlocks.add(block);
                                        }
                                    }
                                }
                            }
                            pointerX += vector.x;
                            pointerY += vector.y;
                            pointerZ += vector.z;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean explodeB() {
        LongArraySet updateBlocks = new LongArraySet();
        List<Vector3> send = new ArrayList<>();
        Vector3 source = (new Vector3(this.source.x, this.source.y, this.source.z)).floor();
        double yield = (1d / this.size) * 100d;
        if (this.what instanceof Entity) {
            EntityExplodeEvent ev = new EntityExplodeEvent((Entity) this.what, this.source, this.affectedBlocks, yield);
            this.level.getServer().getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                return false;
            } else {
                yield = ev.getYield();
                this.affectedBlocks = ev.getBlockList();
            }
        }
        double explosionSize = this.size * 2d;
        double minX = NukkitMath.floorDouble(this.source.x - explosionSize - 1);
        double maxX = NukkitMath.ceilDouble(this.source.x + explosionSize + 1);
        double minY = NukkitMath.floorDouble(this.source.y - explosionSize - 1);
        double maxY = NukkitMath.ceilDouble(this.source.y + explosionSize + 1);
        double minZ = NukkitMath.floorDouble(this.source.z - explosionSize - 1);
        double maxZ = NukkitMath.ceilDouble(this.source.z + explosionSize + 1);
        AxisAlignedBB explosionBB = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        Entity[] list = this.level.getNearbyEntities(explosionBB, this.what instanceof Entity ? (Entity) this.what : null);
        for (int i = 0; i < list.length; ++i) {
            Entity entity = list[i];
            double distance = entity.distance(this.source) / explosionSize;
            if (distance <= 1) {
                Vector3 motion = entity.subtract(this.source).normalize();
                int exposure = 1;
                double impact = (1 - distance) * exposure;
                int damage = (int) (((impact * impact + impact) / 2) * 5 * explosionSize + 1);
                if (this.what instanceof Entity) {
                    entity.attack(new EntityDamageByEntityEvent((Entity) this.what, entity, DamageCause.ENTITY_EXPLOSION, damage));
                } else if (this.what instanceof Block) {
                    entity.attack(new EntityDamageByBlockEvent((Block) this.what, entity, DamageCause.BLOCK_EXPLOSION, damage));
                } else {
                    entity.attack(new EntityDamageEvent(entity, DamageCause.BLOCK_EXPLOSION, damage));
                }
                entity.setMotion(motion.multiply(impact));
            }
        }
        ItemBlock air = new ItemBlock(new BlockAir());
        for (int i = 0; i < this.affectedBlocks.size(); ++i) {
            Block block = this.affectedBlocks.get(i);
            if (block.getId() == Block.TNT) {
                ((BlockTNT) block).prime(Utils.rand(10, 30), this.what instanceof Entity ? (Entity) this.what : null);
            } else if (Math.random() * 100 < yield) {
                Item[] drops = block.getDrops(air);
                for (int j = 0; j < drops.length; ++j) {
                    Item drop = drops[j];
                    this.level.dropItem(block.add(0.5, 0.5, 0.5), drop);
                }
            }
            this.level.setBlockAt((int) block.x, (int) block.y, (int) block.z, BlockID.AIR);
            Vector3 pos = new Vector3(block.x, block.y, block.z);
            BlockFace[] sides = BlockFace.values();
            for (int j = 0; j < sides.length; ++j) {
                BlockFace side = sides[j];
                Vector3 sideBlock = pos.getSide(side);
                long index = Hash.hashBlock((int) sideBlock.x, (int) sideBlock.y, (int) sideBlock.z);
                if (!this.affectedBlocks.contains(sideBlock) && !updateBlocks.contains(index)) {
                    BlockUpdateEvent ev = new BlockUpdateEvent(this.level.getBlock(sideBlock));
                    this.level.getServer().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        ev.getBlock().onUpdate(Level.BLOCK_UPDATE_NORMAL);
                    }
                    updateBlocks.add(index);
                }
            }
            send.add(new Vector3(block.x - source.x, block.y - source.y, block.z - source.z));
        }
        ExplodePacket pk = new ExplodePacket();
        pk.x = (float) this.source.x;
        pk.y = (float) this.source.y;
        pk.z = (float) this.source.z;
        pk.radius = (float) this.size;
        pk.records = send.toArray(new Vector3[0]);
        this.level.addChunkPacket((int) source.x >> 4, (int) source.z >> 4, pk);
        this.level.addSound(new Vector3(this.source.x, this.source.y, this.source.z), Sound.RANDOM_EXPLODE);
        return true;
    }
}