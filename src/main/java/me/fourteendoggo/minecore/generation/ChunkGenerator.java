package me.fourteendoggo.minecore.generation;

import de.articdive.jnoise.generators.noisegen.opensimplex.FastSimplexNoiseGenerator;
import de.articdive.jnoise.pipeline.JNoise;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.generator.UnitModifier;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ChunkGenerator implements Generator {
    public static final JNoise NOISE;
    public static final AtomicReference<JNoise> TREE_NOISE;

    static {
        FastSimplexNoiseGenerator noiseGenerator = FastSimplexNoiseGenerator.newBuilder()
                .setSeed((long) Math.floor(Math.random() * 10000)).build();
        NOISE = JNoise.newBuilder().fastSimplex(noiseGenerator).scale(1 / 180.0).build();
        TREE_NOISE = new AtomicReference<>(JNoise.newBuilder().white(999999).build());
    }

    private final AtomicLong generatedYet = new AtomicLong(0);

    void generateOld(GenerationUnit unit) {
        UnitModifier modifier = unit.modifier();
        modifier.fillBiome(Biome.PLAINS);

        Point start = unit.absoluteStart();

        for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {

                int absX = start.blockX() + x;
                int absZ = start.blockZ() + z;
                int height = getHeight(absX, absZ);

                Point pos = new Vec(absX, height, absZ);
                Point pos1 = pos.add(1, 0, 1);

                // regular terrain
                modifier.fill(pos.withY(0), pos1, Block.STONE);
                modifier.fill(pos.withY(pos.y() - 7), pos1, Block.DIRT);
                modifier.fill(pos.withY(pos.y() - 1), pos1, Block.GRASS_BLOCK);
                modifier.fill(pos.withY(0), pos1.withY(1), Block.BEDROCK);


                /*
                // water
                if (pos.y() < 61) {
                    modifier.fill(pos, pos1.withY(61), Block.WATER);
                    modifier.fill(pos.withY(0), pos1, Block.AIR);
                    return;
                }
                 */

                // oak trees
                if (TREE_NOISE.get().evaluateNoise(pos.x(), pos.z()) > 0.94) {
                    unit.fork(setter -> spawnTree(setter, pos));
                }
            }
        }
    }

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        UnitModifier modifier = unit.modifier();
        modifier.fillBiome(Biome.PLAINS);

        Point start = unit.absoluteStart();
        Point end = unit.absoluteEnd();

        for (int x = start.blockX(); x < end.blockX(); x++) {
            for (int z = start.blockZ(); z < end.blockZ(); z++) {

                double heightDelta = NOISE.evaluateNoise(x, 0, z);
                int height = (int) (64 - heightDelta * 16);
                int bottom = 0;
                int dirt = height + 5;
                int grass = dirt + 1;

                for (int y = bottom; y < height; y++) {
                    modifier.setBlock(x, y, z, Block.STONE);
                }

                for (int y = height; y < dirt; y++) {
                    modifier.setBlock(x, y, z, Block.DIRT);
                }

                for (int y = dirt; y < grass; y++) {
                    modifier.setBlock(x, y, z, Block.GRASS_BLOCK);
                }

                /*
                if (height < 64) {
                    // Too low for a tree
                    // However we can put water here
                    for (int y = height; y < 64; y++) {
                        modifier.setBlock(x, y, z, Block.WATER);
                    }
                    continue;
                }

                if (TREE_NOISE.get().evaluateNoise(x, 0, z) > 0.9) {
                    Point treePos = new Vec(x, grass, z);
                    unit.fork(setter -> spawnTree(setter, treePos));
                }
                 */

            }
        }
    }

    private int getHeight(int x, int z) {
        double preHeight = NOISE.evaluateNoise(x / 16.0, z / 16.0);
        return (int) ((preHeight > 0 ? preHeight * 6 : preHeight * 4) + 64);
    }

    private void spawnTree(Block.Setter setter, Point pos) {
        int trunkX = pos.blockX();
        int trunkBottomY = pos.blockY();
        int trunkZ = pos.blockZ();

        for (int i = 0; i < 2; i++) {
            setter.setBlock(trunkX + 1, trunkBottomY + 3 + i, trunkZ, Block.OAK_LEAVES);
            setter.setBlock(trunkX - 1, trunkBottomY + 3 + i, trunkZ, Block.OAK_LEAVES);
            setter.setBlock(trunkX, trunkBottomY + 3 + i, trunkZ + 1, Block.OAK_LEAVES);
            setter.setBlock(trunkX, trunkBottomY + 3 + i, trunkZ - 1, Block.OAK_LEAVES);

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    setter.setBlock(trunkX + x, trunkBottomY + 2 + i, trunkZ - z, Block.OAK_LEAVES);
                }
            }
        }

        setter.setBlock(trunkX, trunkBottomY, trunkZ, Block.OAK_LOG);
        setter.setBlock(trunkX, trunkBottomY + 1, trunkZ, Block.OAK_LOG);
        setter.setBlock(trunkX, trunkBottomY + 2, trunkZ, Block.OAK_LOG);
        setter.setBlock(trunkX, trunkBottomY + 3, trunkZ, Block.OAK_LOG);
        setter.setBlock(trunkX, trunkBottomY + 4, trunkZ, Block.OAK_LEAVES);
    }

    private void generateTree(Block.Setter setter, Point origin) {
        setter.setBlock(origin.add(0, -1, 0), Block.DIRT);
        setter.setBlock(origin.add(0, -1, 0), Block.DIRT);
        setter.setBlock(origin.add(0, 0, 0), Block.OAK_LOG);
        setter.setBlock(origin.add(0, 1, 0), Block.OAK_LOG);
        setter.setBlock(origin.add(0, 2, 0), Block.OAK_LOG);
        setter.setBlock(origin.add(0, 3, 0), Block.OAK_LOG);
        setter.setBlock(origin.add(1, 1, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 1, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 1, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 1, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 1, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 1, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 1, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 1, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 1, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 1, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 1, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 1, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 1, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 1, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 1, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 1, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 1, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 1, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 1, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 1, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 1, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 1, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 1, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 1, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 2, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 2, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 2, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 2, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 2, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 2, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 2, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 2, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 2, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 2, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 2, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 2, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 2, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 2, 2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 2, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 2, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 2, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 2, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 2, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 2, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(2, 2, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 2, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 2, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-2, 2, -2), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 3, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 3, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 3, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 3, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 3, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 3, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 3, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 3, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(1, 4, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 4, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 4, 0), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 4, 1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(0, 4, -1), Block.OAK_LEAVES);
        setter.setBlock(origin.add(-1, 4, -1), Block.OAK_LEAVES);
    }
}
