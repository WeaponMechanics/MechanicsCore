package me.deecaad.core.compatibility;

import me.deecaad.core.compatibility.block.BlockCompatibility;
import me.deecaad.core.compatibility.entity.EntityCompatibility;
import me.deecaad.core.compatibility.nbt.NBTCompatibility;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This interface outlines a version dependant api, where there is an implementing class for each
 * minecraft protocol version.
 */
public interface ICompatibility {

    /**
     * Returns the nms EntityPlayer wrapped by the given bukkit {@link Player}.
     *
     * @param player The non-null bukkit player.
     * @return The non-null nms player.
     */
    @NotNull Object getEntityPlayer(@NotNull Player player);

    /**
     * Overloaded version of {@link #sendPackets(Player, Object...)} which does not need to instantiate
     * a new array of packets every time 1 packet needs to be sent.
     *
     * @param player The non-null player to send the packet to.
     * @param packet The non-null packet to send to the player.
     */
    void sendPackets(Player player, Object packet);

    /**
     * Sends the given <code>packets</code> to the given <code>player</code>. This can be run
     * asynchronously
     *
     * @param player The non-null player to send the packet to.
     * @param packets The non-null array of non-null packets to send to the player.
     */
    void sendPackets(Player player, Object... packets);

    /**
     * Returns this version's loaded {@link NBTCompatibility}. The classes for each version can be found
     * in the nbt package.
     *
     * @return This version's non-null nbt compatibility.
     * @throws UnsupportedOperationException In minecraft protocol versions 1_13_R2 and higher.
     */
    @NotNull NBTCompatibility getNBTCompatibility();

    /**
     * Returns this version's loaded {@link EntityCompatibility}. The classes for each version can be
     * found in the entity package.
     *
     * @return This version's non-null entity compatibility.
     */
    @NotNull EntityCompatibility getEntityCompatibility();

    /**
     * Returns this version's loaded {@link BlockCompatibility}. The classes for each version can be
     * found in the block package.
     *
     * @return This version's non-null block compatibility.
     */
    @NotNull BlockCompatibility getBlockCompatibility();
}
