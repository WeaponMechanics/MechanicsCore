package me.deecaad.core.compatibility.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class Block_1_21_R2 implements BlockCompatibility {

    @Override
    public @NotNull Object getCrackPacket(@NotNull Block block, int crack) {
        int id = IDS.incrementAndGet();
        if (id == Integer.MAX_VALUE) {
            IDS.set(0);
        }

        return getCrackPacket(block, crack, id);
    }

    @Override
    public @NotNull Object getCrackPacket(@NotNull Block block, int crack, int id) {
        BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        return new ClientboundBlockDestructionPacket(id, pos, crack);
    }
}