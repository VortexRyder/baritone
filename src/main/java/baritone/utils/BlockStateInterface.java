/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.cache.CachedRegion;
import baritone.cache.WorldData;
import baritone.pathing.movement.CalculationContext;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Wraps get for chuck caching capability
 *
 * @author leijurv
 */
public class BlockStateInterface implements Helper {

    private final World world;
    private final WorldData worldData;

    private Chunk prev = null;
    private CachedRegion prevCached = null;

    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    public BlockStateInterface(World world, WorldData worldData) {
        this.worldData = worldData;
        this.world = world;
    }

    public static Block getBlock(BlockPos pos) { // won't be called from the pathing thread because the pathing thread doesn't make a single blockpos pog
        return get(pos).getBlock();
    }

    public static IBlockState get(BlockPos pos) {
        return new CalculationContext(Baritone.INSTANCE).get(pos); // immense iq
        // can't just do world().get because that doesn't work for out of bounds
        // and toBreak and stuff fails when the movement is instantiated out of load range but it's not able to BlockStateInterface.get what it's going to walk on
    }

    public IBlockState get0(int x, int y, int z) {

        // Invalid vertical position
        if (y < 0 || y >= 256) {
            return AIR;
        }

        if (!Baritone.settings().pathThroughCachedOnly.get()) {
            Chunk cached = prev;
            // there's great cache locality in block state lookups
            // generally it's within each movement
            // if it's the same chunk as last time
            // we can just skip the mc.world.getChunk lookup
            // which is a Long2ObjectOpenHashMap.get
            // see issue #113
            if (cached != null && cached.x == x >> 4 && cached.z == z >> 4) {
                return cached.getBlockState(x, y, z);
            }
            Chunk chunk = world.getChunk(x >> 4, z >> 4);
            if (chunk.isLoaded()) {
                prev = chunk;
                return chunk.getBlockState(x, y, z);
            }
        }
        // same idea here, skip the Long2ObjectOpenHashMap.get if at all possible
        // except here, it's 512x512 tiles instead of 16x16, so even better repetition
        CachedRegion cached = prevCached;
        if (cached == null || cached.getX() != x >> 9 || cached.getZ() != z >> 9) {
            if (worldData == null) {
                return AIR;
            }
            CachedRegion region = worldData.cache.getRegion(x >> 9, z >> 9);
            if (region == null) {
                return AIR;
            }
            prevCached = region;
            cached = region;
        }
        IBlockState type = cached.getBlock(x & 511, y, z & 511);
        if (type == null) {
            return AIR;
        }
        return type;
    }

    public boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        if (prevChunk != null && prevChunk.x == x >> 4 && prevChunk.z == z >> 4) {
            return true;
        }
        prevChunk = world.getChunk(x >> 4, z >> 4);
        if (prevChunk.isLoaded()) {
            prev = prevChunk;
            return true;
        }
        CachedRegion prevRegion = prevCached;
        if (prevRegion != null && prevRegion.getX() == x >> 9 && prevRegion.getZ() == z >> 9) {
            return prevRegion.isCached(x & 511, z & 511);
        }
        if (worldData == null) {
            return false;
        }
        prevRegion = worldData.cache.getRegion(x >> 9, z >> 9);
        if (prevRegion == null) {
            return false;
        }
        prevCached = prevRegion;
        return prevRegion.isCached(x & 511, z & 511);
    }
}
