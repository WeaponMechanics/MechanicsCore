package me.deecaad.core.compatibility.entity;

import com.cjcrafter.foliascheduler.util.FieldAccessor;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.PlayerEquipment;
import net.minecraft.world.item.Item;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Entity_1_21_R4 implements EntityCompatibility {

    private static final net.minecraft.world.entity.EquipmentSlot[] SLOTS = net.minecraft.world.entity.EquipmentSlot.values();

    private static final FieldAccessor itemsById = ReflectionUtil.getField(SynchedEntityData.class, SynchedEntityData.DataItem[].class);
    private static final FieldAccessor itemsField;
    private static final FieldAccessor entityEquipmentField;
    private static final FieldAccessor inventoryEquipmentField;

    static {
        Class<?> playerInventoryClass = ReflectionUtil.getMinecraftClass("world.entity.player", "PlayerInventory");
        Class<?> nonNullListClass = ReflectionUtil.getMinecraftClass("core", "NonNullList");

        itemsField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass);
        entityEquipmentField = ReflectionUtil.getField(LivingEntity.class, EntityEquipment.class);
        inventoryEquipmentField = ReflectionUtil.getField(playerInventoryClass, EntityEquipment.class);
    }

    @Override
    public void injectInventoryConsumer(@NotNull Player player, @NotNull EquipmentChangeConsumer consumer) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        Inventory inventory = handle.getInventory();

        NonNullList<net.minecraft.world.item.ItemStack> items = new NonNullListProxy(36, inventory, consumer);
        for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++)
            items.set(i, inventory.getNonEquipmentItems().get(i));

        EntityEquipment equipment = new EntityEquipmentProxy(handle, consumer);
        equipment.setAll(inventory.equipment);

        // Have to use reflection here since these fields are final
        itemsField.set(inventory, items);
        entityEquipmentField.set(handle, equipment);
        inventoryEquipmentField.set(inventory, equipment);
    }

    @Override
    public FakeEntity generateFakeEntity(Location location, EntityType type, Object data) {
        return new FakeEntity_1_21_R4(location, type, data);
    }

    @Override
    public void setSlot(Player bukkit, EquipmentSlot slot, @Nullable ItemStack item) {
        if (item == null) {
            item = bukkit.getEquipment().getItem(slot);
        }

        int id = bukkit.getEntityId();
        net.minecraft.world.entity.EquipmentSlot nmsSlot = switch (slot) {
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case HAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFF_HAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case BODY -> net.minecraft.world.entity.EquipmentSlot.BODY;
            case SADDLE -> net.minecraft.world.entity.EquipmentSlot.SADDLE;
        };

        List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> temp = new ArrayList<>(1);
        temp.add(new Pair<>(nmsSlot, CraftItemStack.asNMSCopy(item)));
        ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(id, temp);
        ((CraftPlayer) bukkit).getHandle().connection.send(packet);
    }

    public static List<SynchedEntityData.DataValue<?>> getEntityData(SynchedEntityData data, boolean forceUpdateAll) {
        if (!forceUpdateAll) {
            List<SynchedEntityData.DataValue<?>> dirty = data.packDirty();
            return dirty == null ? List.of() : dirty;
        }

        // 1.19.3 changed the packet arguments, so in order to unpack ALL data
        // (not just the dirty data) we need to manually get it and unpack it.
        SynchedEntityData.DataItem<?>[] metaData = (SynchedEntityData.DataItem<?>[]) itemsById.get(data);
        List<SynchedEntityData.DataValue<?>> packed = new ArrayList<>(metaData.length);
        for (SynchedEntityData.DataItem<?> element : metaData)
            packed.add(element.value());
        return packed;
    }

    @Override
    public Object generateMetaPacket(Entity bukkit) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkit).getHandle();
        return new ClientboundSetEntityDataPacket(entity.getId(), getEntityData(entity.getEntityData(), true));
    }

    @Override
    public void modifyMetaPacket(Object obj, EntityMeta meta, boolean enabled) {
        ClientboundSetEntityDataPacket packet = (ClientboundSetEntityDataPacket) obj;
        List<SynchedEntityData.DataValue<?>> list = packet.packedItems();

        if (list.isEmpty())
            return;

        // The "shared byte data" is applied to every entity, and it is always
        // the first item (It can never be the second, third, etc.). However,
        // if no modifications are made to the "shared byte data" before this
        // packet is sent, that item will not be present. This is implemented
        // in vanilla's dirty meta system.
        if (list.get(0) == null || list.get(0).value().getClass() != Byte.class)
            return;

        // noinspection unchecked
        SynchedEntityData.DataValue<Byte> item = (SynchedEntityData.DataValue<Byte>) list.get(0);
        byte data = item.value();
        data = meta.set(data, enabled);

        list.set(0, new SynchedEntityData.DataValue<>(item.id(), item.serializer(), data));
    }


    /**
     * Wraps an {@link Inventory}'s {@link NonNullList} to add a callback for modifications.
     */
    private static class NonNullListProxy extends NonNullList<net.minecraft.world.item.ItemStack> {

        private static final FieldAccessor itemField = ReflectionUtil.getField(net.minecraft.world.item.ItemStack.class, Item.class);

        private final Inventory inventory;
        private final EquipmentChangeConsumer consumer;

        public NonNullListProxy(int size, Inventory inventory, EquipmentChangeConsumer consumer) {
            super(generate(size), net.minecraft.world.item.ItemStack.EMPTY);
            this.inventory = inventory;
            this.consumer = consumer;
        }

        @Override
        public @NotNull net.minecraft.world.item.ItemStack set(int index, net.minecraft.world.item.ItemStack newItem) {
            net.minecraft.world.item.ItemStack oldItem = get(index);
            boolean isMainHand = (inventory.getSelectedSlot() == index);

            // Exit early for slots we do not care about
            if (!isMainHand)
                return super.set(index, newItem);

            if (newItem.getCount() == 0 && itemField.get(newItem) != null) {
                newItem.setCount(1);
                consumer.accept(CraftItemStack.asBukkitCopy(oldItem), CraftItemStack.asBukkitCopy(newItem), EquipmentSlot.HAND);
                newItem.setCount(0);
            }

            else if (oldItem.getCount() == 0 && itemField.get(oldItem) != null) {
                oldItem.setCount(1);
                consumer.accept(CraftItemStack.asBukkitCopy(oldItem), CraftItemStack.asBukkitCopy(newItem), EquipmentSlot.HAND);
                oldItem.setCount(0);
            }

            else if (!net.minecraft.world.item.ItemStack.matches(oldItem, newItem)) {
                consumer.accept(CraftItemStack.asBukkitCopy(oldItem), CraftItemStack.asBukkitCopy(newItem), EquipmentSlot.HAND);
            }

            return super.set(index, newItem);
        }

        private static List<net.minecraft.world.item.ItemStack> generate(int size) {
            net.minecraft.world.item.ItemStack[] items = new net.minecraft.world.item.ItemStack[size];
            Arrays.fill(items, net.minecraft.world.item.ItemStack.EMPTY);
            return Arrays.asList(items);
        }
    }


    /**
     * Wraps an {@link Inventory}'s {@link EntityEquipment}
     */
    private static class EntityEquipmentProxy extends PlayerEquipment {
        private final EquipmentChangeConsumer consumer;

        // Shadow-cache of last known equipment stacks (per NMS slot ordinal)
        private final net.minecraft.world.item.ItemStack[] lastKnown = new net.minecraft.world.item.ItemStack[SLOTS.length];

        public EntityEquipmentProxy(net.minecraft.world.entity.player.Player player, EquipmentChangeConsumer consumer) {
            super(player);
            this.consumer = consumer;

            // Initialize cache from current equipment
            for (net.minecraft.world.entity.EquipmentSlot slot : SLOTS) {
                net.minecraft.world.item.ItemStack cur = super.get(slot);
                net.minecraft.world.item.ItemStack san = sanitize(cur);
                lastKnown[slot.ordinal()] = san;
            }
        }

        private @Nullable EquipmentSlot getSlot(net.minecraft.world.entity.EquipmentSlot slot) {
            return switch (slot) {
                case FEET -> EquipmentSlot.FEET;
                case LEGS -> EquipmentSlot.LEGS;
                case CHEST -> EquipmentSlot.CHEST;
                case HEAD -> EquipmentSlot.HEAD;
                case MAINHAND -> EquipmentSlot.HAND;
                case OFFHAND -> EquipmentSlot.OFF_HAND;
                default -> null;
            };
        }

        // Treat ANY "empty" as truly empty (important for Paper 1.21.8 weirdness)
        private static net.minecraft.world.item.ItemStack sanitize(@Nullable net.minecraft.world.item.ItemStack stack) {
            if (stack == null) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            // Note: Paper 1.21.8 weirdness might show count=0 but not isEmpty(), or vice versa.
            boolean empty = stack.isEmpty();
            int count = stack.getCount();

            if (empty || count <= 0) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            return stack.copy();
        }

        @Override
        public @NotNull net.minecraft.world.item.ItemStack set(@NotNull net.minecraft.world.entity.EquipmentSlot slot, @NotNull net.minecraft.world.item.ItemStack stack) {
            EquipmentSlot bukkitSlot = getSlot(slot);

            // Old comes from cache (NOT from super.get(slot), which can already be mutated on Paper 1.21.8)
            net.minecraft.world.item.ItemStack oldSnapshot = lastKnown[slot.ordinal()];
            net.minecraft.world.item.ItemStack newSnapshot = sanitize(stack);

            // Do the real set
            net.minecraft.world.item.ItemStack oldReturn = super.set(slot, stack);

            // Update cache AFTER set
            lastKnown[slot.ordinal()] = newSnapshot;

            if (bukkitSlot != null && !net.minecraft.world.item.ItemStack.matches(oldSnapshot, newSnapshot)) {
                ItemStack oldB = CraftItemStack.asBukkitCopy(oldSnapshot);
                ItemStack newB = CraftItemStack.asBukkitCopy(newSnapshot);

                consumer.accept(oldB, newB, bukkitSlot);
            }

            return oldReturn;
        }

        @Override
        public void setAll(@NotNull EntityEquipment equipment) {
            for (net.minecraft.world.entity.EquipmentSlot slot : SLOTS) {
                EquipmentSlot bukkitSlot = getSlot(slot);

                net.minecraft.world.item.ItemStack oldSnapshot = lastKnown[slot.ordinal()];
                net.minecraft.world.item.ItemStack newSnapshot = sanitize(equipment.get(slot));

                if (bukkitSlot != null && !net.minecraft.world.item.ItemStack.matches(oldSnapshot, newSnapshot)) {
                    consumer.accept(
                            CraftItemStack.asBukkitCopy(oldSnapshot),
                            CraftItemStack.asBukkitCopy(newSnapshot),
                            bukkitSlot
                    );
                }

                lastKnown[slot.ordinal()] = newSnapshot;

                // apply sanitized (bypasses override)
                super.set(slot, newSnapshot);
            }
        }

        @Override
        public void clear() {
            for (net.minecraft.world.entity.EquipmentSlot slot : SLOTS) {
                EquipmentSlot bukkitSlot = getSlot(slot);

                net.minecraft.world.item.ItemStack oldSnapshot = lastKnown[slot.ordinal()];

                if (bukkitSlot != null && !oldSnapshot.isEmpty()) {
                    consumer.accept(CraftItemStack.asBukkitCopy(oldSnapshot), null, bukkitSlot);
                }

                lastKnown[slot.ordinal()] = net.minecraft.world.item.ItemStack.EMPTY;

                // clear everything, not just mapped slots
                super.set(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }
}
