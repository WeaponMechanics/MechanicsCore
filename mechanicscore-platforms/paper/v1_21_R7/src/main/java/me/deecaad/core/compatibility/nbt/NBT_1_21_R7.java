package me.deecaad.core.compatibility.nbt;

import com.cjcrafter.foliascheduler.util.FieldAccessor;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import com.google.common.collect.Lists;
import me.deecaad.core.utils.StringUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class NBT_1_21_R7 extends NBT_Persistent {

    @Override
    public @NotNull String getNBTDebug(@NotNull org.bukkit.inventory.ItemStack bukkitStack) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(bukkitStack);
        if (nmsStack.isEmpty()) {
            return "null";
        }

        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        net.minecraft.nbt.Tag rawTag;
        try {
            RegistryOps<Tag> registryOps = nmsServer.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            rawTag = ItemStack.CODEC.encodeStart(registryOps, nmsStack).getOrThrow();
        } catch (IllegalStateException ex) {
            return "null";
        }

        if (!(rawTag instanceof net.minecraft.nbt.CompoundTag compoundTag)) {
            return "null";
        }

        TagColorVisitor visitor = new TagColorVisitor();
        visitor.visitCompound(compoundTag);
        return visitor.build();
    }

    private static class TagColorVisitor extends StringTagVisitor {

        private static final String BRACE_COLORS = "f780";
        private static final String VALUE_COLORS = "6abcdef";
        private final StringBuilder builder;

        private final int indents;
        private final int colorOffset;

        public TagColorVisitor() {
            this(0, 0);
        }

        public TagColorVisitor(int indents, int colorOffset) {
            FieldAccessor field = ReflectionUtil.getField(StringTagVisitor.class, StringBuilder.class);
            this.builder = (StringBuilder) field.get(this);
            this.indents = indents;
            this.colorOffset = colorOffset;
        }

        @Override
        public void visitCompound(CompoundTag compound) {
            String braceColor = "&" + BRACE_COLORS.charAt(indents % BRACE_COLORS.length());
            builder.append(braceColor).append("{\n");
            List<String> list = Lists.newArrayList(compound.keySet());
            Collections.sort(list);

            for (int i = 0; i < list.size(); i++) {

                if (i != 0)
                    builder.append('\n');

                builder.append(StringUtil.repeat("  ", indents));

                String key = list.get(i);
                Tag value = Objects.requireNonNull(compound.get(key), "This is impossible");
                String color = "&" + VALUE_COLORS.charAt((i + colorOffset) % VALUE_COLORS.length());

                builder.append(color);
                handleKeyEscape(key);
                TagColorVisitor indentVisitor = new TagColorVisitor(value instanceof CompoundTag ? indents + 1 : indents, colorOffset + i);
                builder.append("&f&l: ").append(color);
                value.accept(indentVisitor);
                builder.append(indentVisitor.build());
            }

            builder.append(braceColor).append("}\n");
        }

        private static final Pattern UNQUOTED_KEY_MATCH = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
        private void handleKeyEscape(String key) {
            if (!key.equalsIgnoreCase("true") && !key.equalsIgnoreCase("false") && UNQUOTED_KEY_MATCH.matcher(key).matches()) {
                this.builder.append(key);
            } else {
                StringTag.quoteAndEscape(key, this.builder);
            }

        }
    }
}
