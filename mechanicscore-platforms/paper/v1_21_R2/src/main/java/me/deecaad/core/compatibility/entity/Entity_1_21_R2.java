package me.deecaad.core.compatibility.entity;

import com.cjcrafter.foliascheduler.util.FieldAccessor;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
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

public class Entity_1_21_R2 implements EntityCompatibility {

    private static final FieldAccessor itemsById = ReflectionUtil.getField(SynchedEntityData.class, SynchedEntityData.DataItem[].class);
    private static final FieldAccessor itemsField;
    private static final FieldAccessor armorField;
    private static final FieldAccessor offHandField;
    private static final FieldAccessor combinedField;

    static {
        Class<?> playerInventoryClass = ReflectionUtil.getMinecraftClass("world.entity.player", "PlayerInventory");
        Class<?> nonNullListClass = ReflectionUtil.getMinecraftClass("core", "NonNullList");

        itemsField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass, 0);
        armorField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass, 1);
        offHandField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass, 2);
        combinedField = ReflectionUtil.getField(playerInventoryClass, List.class, 3); // index 3 since nonNonList is a List
    }

    @Override
    public void injectInventoryConsumer(@NotNull Player player, @NotNull EquipmentChangeConsumer consumer) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        Inventory inventory = handle.getInventory();

        NonNullList<net.minecraft.world.item.ItemStack> items = new NonNullListProxy(36, 0, inventory, consumer);
        NonNullList<net.minecraft.world.item.ItemStack> armor = new NonNullListProxy(4, 36, inventory, consumer);
        NonNullList<net.minecraft.world.item.ItemStack> offHand = new NonNullListProxy(1, 40, inventory, consumer);

        for (int i = 0; i < inventory.items.size(); i++)
            items.set(i, inventory.items.get(i));
        for (int i = 0; i < inventory.armor.size(); i++)
            armor.set(i, inventory.armor.get(i));
        for (int i = 0; i < offHand.size(); i++)
            offHand.set(i, inventory.offhand.get(i));

        // Have to use reflection here since these fields are final
        itemsField.set(inventory, items);
        armorField.set(inventory, armor);
        offHandField.set(inventory, offHand);
        combinedField.set(inventory, ImmutableList.of(items, armor, offHand));
    }

    @Override
    public FakeEntity generateFakeEntity(Location location, EntityType type, Object data) {
        return new FakeEntity_1_21_R2(location, type, data);
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

        // 1.19.3 changed this to a record
        list.set(0, new SynchedEntityData.DataValue<>(item.id(), item.serializer(), data));
    }


    /**
     * Wraps an {@link Inventory}'s {@link NonNullList} to add a callback for modifications.
     */
    public static class NonNullListProxy extends NonNullList<net.minecraft.world.item.ItemStack> {
        private static final FieldAccessor itemField = ReflectionUtil.getField(net.minecraft.world.item.ItemStack.class, Item.class);
        private final int offset;
        private final Inventory inventory;
        private final EquipmentChangeConsumer consumer;

        public NonNullListProxy(int size, int offset, Inventory inventory, EquipmentChangeConsumer consumer) {
            super(generate(size), net.minecraft.world.item.ItemStack.EMPTY);
            this.offset = offset;
            this.inventory = inventory;
            this.consumer = consumer;
        }

        @Override
        public @NotNull net.minecraft.world.item.ItemStack set(int index, net.minecraft.world.item.ItemStack newItem) {
            EquipmentSlot slot = switch (offset + index) {
                case 36 -> EquipmentSlot.FEET;
                case 37 -> EquipmentSlot.LEGS;
                case 38 -> EquipmentSlot.CHEST;
                case 39 -> EquipmentSlot.HEAD;
                case 40 -> EquipmentSlot.OFF_HAND;
                default -> {
                    int hotbar = inventory.selected;
                    if ((offset + index) == hotbar)
                        yield EquipmentSlot.HAND;
                    yield null;
                }
            };

            // Exit early... not a change we care about
            if (slot == null)
                return super.set(index, newItem);

            net.minecraft.world.item.ItemStack oldItem = get(index);

            if (newItem.getCount() == 0 && itemField.get(newItem) != null) {
                newItem.setCount(1);
                consumer.accept(CraftItemStack.asBukkitCopy(oldItem), CraftItemStack.asBukkitCopy(newItem), slot);
                newItem.setCount(0);
            }

            else if (oldItem.getCount() == 0 && itemField.get(oldItem) != null) {
                oldItem.setCount(1);
                consumer.accept(CraftItemStack.asBukkitCopy(oldItem), CraftItemStack.asBukkitCopy(newItem), slot);
                oldItem.setCount(0);
            }

            else if (!net.minecraft.world.item.ItemStack.matches(oldItem, newItem)) {
                consumer.accept(CraftItemStack.asBukkitCopy(oldItem), CraftItemStack.asBukkitCopy(newItem), slot);
            }

            return super.set(index, newItem);
        }

        private static List<net.minecraft.world.item.ItemStack> generate(int size) {
            net.minecraft.world.item.ItemStack[] items = new net.minecraft.world.item.ItemStack[size];
            Arrays.fill(items, net.minecraft.world.item.ItemStack.EMPTY);
            return Arrays.asList(items);
        }
    }
}