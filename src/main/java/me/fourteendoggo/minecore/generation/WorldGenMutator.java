package me.fourteendoggo.minecore.generation;

import de.articdive.jnoise.core.api.transformers.SimpleTransformer;
import de.articdive.jnoise.generators.noisegen.perlin.PerlinNoiseGenerator;
import de.articdive.jnoise.pipeline.JNoise;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class WorldGenMutator {
    private static final SimpleTransformer[] SHADOWED_WORLD_TRANSFORMERS;
    private static final JNoise EMPTY_TREE_NOISE = JNoise.newBuilder().perlin(PerlinNoiseGenerator.newBuilder().build()).build(); // value doesn't matter
    private static final JNoise WORKING_TREE_NOISE = ChunkGenerator.TREE_NOISE.get();

    static {
        SHADOWED_WORLD_TRANSFORMERS = getFieldValue(ChunkGenerator.NOISE, "simpleTransformers", SimpleTransformer[].class);

        SimpleTransformer firstTransformer = SHADOWED_WORLD_TRANSFORMERS[0];
        double scaleX = getFieldValue(firstTransformer, "scaleX", Double.class);
        // inject proxied transformer
        SHADOWED_WORLD_TRANSFORMERS[0] = new ScaleTransformerProxy(scaleX);
    }

    public static double changeNoiseScale(double increment) {
        ScaleTransformerProxy proxy = (ScaleTransformerProxy) SHADOWED_WORLD_TRANSFORMERS[0];
        proxy.incrementScale(increment);
        return proxy.scaleX; // all the same so doesn't matter which one we return
    }

    public static boolean setTreeGeneration() {
        AtomicReference<JNoise> currentTreeNoise = ChunkGenerator.TREE_NOISE;
        if (currentTreeNoise.get() == EMPTY_TREE_NOISE) {
            currentTreeNoise.set(WORKING_TREE_NOISE);
            return true;
        }
        currentTreeNoise.set(EMPTY_TREE_NOISE);
        return false;
    }

    private static <T> T getFieldValue(Object object, String fieldName, Class<T> type) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            T value = type.cast(field.get(object));
            field.setAccessible(false);
            return value;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // mutable ScaleTransformer
    private static class ScaleTransformerProxy implements SimpleTransformer {
        private double scaleX, scaleY, scaleZ, scaleW;

        public ScaleTransformerProxy(double scale) {
            this(scale, scale, scale, scale);
        }

        public ScaleTransformerProxy(double scaleX, double scaleY, double scaleZ, double scaleW) {
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.scaleW = scaleW;
        }

        public void incrementScale(double increment) {
            scaleX = scaleY = scaleZ = scaleW = scaleX + increment;
        }

        @Override
        public double transformX(double x) {
            return x * scaleX;
        }

        @Override
        public double transformY(double y) {
            return y * scaleY;
        }

        @Override
        public double transformZ(double z) {
            return z * scaleZ;
        }

        @Override
        public double transformW(double w) {
            return w * scaleW;
        }
    }
}
