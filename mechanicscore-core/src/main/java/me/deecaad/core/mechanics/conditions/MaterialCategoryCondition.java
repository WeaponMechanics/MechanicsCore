package me.deecaad.core.mechanics.conditions;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaterialCategoryCondition extends Condition {

    private MaterialCategory category;

    public MaterialCategoryCondition() {
    }

    public MaterialCategoryCondition(MaterialCategory category) {
        this.category = category;
    }

    @Override
    public boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        if (subject == null)
            return false;
        return category.test(subject.location().getBlock());
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "material_category");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/material-category";
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        MaterialCategory category = data.of("Category").assertExists().getEnum(MaterialCategory.class).get();
        return applyParentArgs(data, new MaterialCategoryCondition(category));
    }

    public enum MaterialCategory {

        ALL {
            @Override
            public boolean test(Block block) {
                return true;
            }
        },
        AIR {
            @Override
            public boolean test(Block block) {
                return !FLUID.test(block) && !CAVE_AIR.test(block) && !VOID_AIR.test(block);
            }
        },
        FLUID {
            @Override
            public boolean test(Block block) {
                if (!MinecraftVersions.UPDATE_AQUATIC.isAtLeast())
                    return block.isLiquid();

                if (block.isLiquid())
                    return true;
                else if (block.getBlockData() instanceof Waterlogged)
                    return ((Waterlogged) block.getBlockData()).isWaterlogged();
                else
                    return false;
            }
        },
        CAVE_AIR {
            @Override
            public boolean test(Block block) {
                return MinecraftVersions.UPDATE_AQUATIC.isAtLeast() && block.getType() == Material.CAVE_AIR;
            }
        },
        VOID_AIR {
            @Override
            public boolean test(Block block) {
                return MinecraftVersions.UPDATE_AQUATIC.isAtLeast() && block.getType() == Material.VOID_AIR;
            }
        };

        public abstract boolean test(Block block);
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
