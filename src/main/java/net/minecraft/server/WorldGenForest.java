package net.minecraft.server;

import java.util.Random;

// CraftBukkit start
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.MaterialData;
// CraftBukkit end

public class WorldGenForest extends WorldGenerator {

    public WorldGenForest(boolean flag) {
        super(flag);
    }

    public boolean a(World world, Random random, int i, int j, int k) {
        // CraftBukkit start
        // sk: The idea is to have (our) WorldServer implement
        // BlockChangeDelegate and then we can implicitly cast World to
        // WorldServer (a safe cast, AFAIK) and no code will be broken. This
        // then allows plugins to catch manually-invoked generation events
        return this.generate((BlockChangeDelegate) world, random, i, j, k, null, null, world.getWorld());
    }

    public boolean generate(BlockChangeDelegate world, Random random, int i, int j, int k, StructureGrowEvent event, ItemStack itemstack, CraftWorld bukkitWorld) {
        // CraftBukkit end
        int l = random.nextInt(3) + 5;
        boolean flag = true;

        if (j >= 1 && j + l + 1 <= world.getHeight()) { // CraftBukkit
            int i1;
            int j1;
            int k1;
            int l1;

            for (i1 = j; i1 <= j + 1 + l; ++i1) {
                byte b0 = 1;

                if (i1 == j) {
                    b0 = 0;
                }

                if (i1 >= j + 1 + l - 2) {
                    b0 = 2;
                }

                for (j1 = i - b0; j1 <= i + b0 && flag; ++j1) {
                    for (k1 = k - b0; k1 <= k + b0 && flag; ++k1) {
                        if (i1 >= 0 && i1 < world.getHeight()) { // CraftBukkit
                            l1 = world.getTypeId(j1, i1, k1);
                            if (l1 != 0 && l1 != Block.LEAVES.id) {
                                flag = false;
                            }
                        } else {
                            flag = false;
                        }
                    }
                }
            }

            if (!flag) {
                return false;
            } else {
                i1 = world.getTypeId(i, j - 1, k);
                if ((i1 == Block.GRASS.id || i1 == Block.DIRT.id) && j < world.getHeight() - l - 1) { // CraftBukkit
                    // Craftbukkit start
                    if (event == null) {
                        world.setRawTypeId(i, j - 1, k, Block.DIRT.id);
                    } else {
                        BlockState dirtState = bukkitWorld.getBlockAt(i, j - 1, k).getState();
                        dirtState.setTypeId(Block.DIRT.id);
                        event.getBlocks().add(dirtState);
                    }
                    // Craftbukkit end
                    int i2;

                    for (i2 = j - 3 + l; i2 <= j + l; ++i2) {
                        j1 = i2 - (j + l);
                        k1 = 1 - j1 / 2;

                        for (l1 = i - k1; l1 <= i + k1; ++l1) {
                            int j2 = l1 - i;

                            for (int k2 = k - k1; k2 <= k + k1; ++k2) {
                                int l2 = k2 - k;

                                if ((Math.abs(j2) != k1 || Math.abs(l2) != k1 || random.nextInt(2) != 0 && j1 != 0) && !Block.o[world.getTypeId(l1, i2, k2)]) {
                                    // Craftbukkit start
                                    if (event == null) {
                                        this.a(world, l1, i2, k2, Block.LEAVES.id, 2);
                                    } else {
                                        BlockState leavesState = bukkitWorld.getBlockAt(l1, i2, k2).getState();
                                        leavesState.setTypeId(Block.LEAVES.id);
                                        leavesState.setData(new MaterialData(Block.LEAVES.id, (byte) 2));
                                        event.getBlocks().add(leavesState);
                                    }
                                    // Craftbukkit end
                                }
                            }
                        }
                    }

                    for (i2 = 0; i2 < l; ++i2) {
                        j1 = world.getTypeId(i, j + i2, k);
                        if (j1 == 0 || j1 == Block.LEAVES.id) {
                            // Craftbukkit start
                            if (event == null) {
                                this.a(world, i, j + i2, k, Block.LOG.id, 2);
                            } else {
                                BlockState logState = bukkitWorld.getBlockAt(i, j + i2, k).getState();
                                logState.setTypeId(Block.LOG.id);
                                logState.setData(new MaterialData(Block.LOG.id, (byte) 2));
                                event.getBlocks().add(logState);
                            }
                            // Craftbukkit end
                        }
                    }
                    // Craftbukkit start
                    if (event != null) {
                        Bukkit.getPluginManager().callEvent(event);
                        if (!event.isCancelled()) {
                            for (BlockState state : event.getBlocks()) {
                                state.update(true);
                            }
                            if (event.isFromBonemeal() && itemstack != null) {
                                --itemstack.count;
                            }
                        }
                    }
                    // Craftbukkit end
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }
}
