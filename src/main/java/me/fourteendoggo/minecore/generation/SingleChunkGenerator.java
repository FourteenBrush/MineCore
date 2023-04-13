package me.fourteendoggo.minecore.generation;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class SingleChunkGenerator extends ChunkGenerator {
    private final AtomicBoolean firstChunk = new AtomicBoolean(true);

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        if (!firstChunk.compareAndSet(true, false)) {
            unit.modifier().setBlock(unit.absoluteStart(), Block.GOLD_BLOCK);
            return;
        }

        super.generate(unit);
        firstChunk.set(false);
    }
}
