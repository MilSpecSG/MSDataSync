package rocks.milspecsg.msdatasync.service.implementation.data;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import rocks.milspecsg.msdatasync.MSDataSync;
import rocks.milspecsg.msdatasync.model.core.Member;
import rocks.milspecsg.msdatasync.model.core.SerializedItemStack;
import rocks.milspecsg.msdatasync.service.data.ApiInventorySerializer;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MSInventorySerializer extends ApiInventorySerializer<Member, Player, Key, User> {

    private static char SEPARATOR = '_';

    @Override
    public CompletableFuture<Boolean> serialize(Member member, Player player) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                // this really should not take longer than 10 seconds
                // usually less than 1
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        });
        Task.builder().execute(() -> {

            try {
                List<SerializedItemStack> itemStacks = new ArrayList<>();
                Sponge.getServer().getPlayer(player.getUniqueId()).ifPresent(p -> {
                    p.getInventory().offer(
                        ItemStack.builder()
                            .itemType(ItemTypes.STICK)
                            .add(Keys.DISPLAY_NAME, Text.of("This", TextColors.AQUA, Text.of(" is a stick", TextColors.BLUE)))
                            .add(Keys.ITEM_LORE, Arrays.asList(Text.of("lore1", TextColors.BLACK), Text.of("lore2"), Text.of("lore3")))
                            .add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(
                                Enchantment.builder()
                                    .type(EnchantmentTypes.KNOCKBACK)
                                    .level(50)
                                    .build(),
                                Enchantment.builder()
                                    .type(EnchantmentTypes.SHARPNESS)
                                    .level(30)
                                    .build()
                            ))
                            .build()
                    );

                    for (Inventory slot : p.getInventory().slots()) {
                        SerializedItemStack serializedItemStack = new SerializedItemStack();
                        ItemStack before = slot.peek().orElse(ItemStack.empty());
                        serializedItemStack.properties = serialize(before.toContainer().getValues(false));
                        itemStacks.add(serializedItemStack);
                    }
                });
                member.itemStacks = itemStacks;
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(false);
                return;
            }
            future.complete(true);
        }).submit(MSDataSync.plugin);
        return future;
    }

    private static Map<String, Object> serialize(Map<DataQuery, Object> values) {
        Map<String, Object> result = new HashMap<>();
        values.forEach((dq, o) -> {
            String s = dq.asString(SEPARATOR);
//            System.out.println("Class for " + s + ": " + o.getClass().getCanonicalName());
            if (o instanceof Map) {
                Object m = serialize((Map<DataQuery, Object>) o);
//                System.out.println("Finished (M), S: " + s + ", M: " + m.toString());
                result.put(s, m);
            } else if (o instanceof List) {
                Object dc = serialize(((List<DataContainer>) o).get(0).getValues(false));
//                System.out.println("Finished (DC), S: " + s + ", DC: " + dc.toString());
                result.put(s, dc);
            } else {
//                System.out.println("Not going deeper, S: " + s + ", O: " + o.toString());
                result.put(s, o);
            }
        });
        return result;
    }

    private static Map<DataQuery, Object> deserialize(Map<String, Object> values) {
        Map<DataQuery, Object> result = new HashMap<>();
        values.forEach((s, o) -> {
            DataQuery dq = DataQuery.of(SEPARATOR, s);
            if (o instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) o;
                Map<DataQuery, Object> r1 = new HashMap<>();
                m.forEach((s1, m1) -> {
                    Object value = m1;
                    try {
                        Map<DataQuery, Object> v = deserialize((Map<String, Object>) m1);
                        DataContainer dc = DataContainer.createNew(DataView.SafetyMode.ALL_DATA_CLONED);
                        v.forEach(dc::set);
                        value = ImmutableList.of(dc);
                    } catch (ClassCastException ignored) {
                    }
                    r1.put(DataQuery.of(SEPARATOR, s1), value);
                });
                result.put(dq, r1);
            } else {
                result.put(dq, o);
            }
        });
        return result;
    }

    @Override
    public CompletableFuture<Boolean> deserialize(Member member, Player player) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                // this really should not take longer than 10 seconds
                // usually less than 1
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        });
        Task.builder().execute(() -> {
            try {
                Sponge.getServer().getPlayer(player.getUniqueId()).ifPresent(p -> {
                    p.getInventory().clear();
                    Iterator<Inventory> slots = p.getInventory().slots().iterator();
                    for (SerializedItemStack stack : member.itemStacks) {
                        if (slots.hasNext()) {
                            DataContainer dc = DataContainer.createNew(DataView.SafetyMode.ALL_DATA_CLONED);
                            deserialize(stack.properties).forEach(dc::set);
                            ItemStack is = ItemStack.builder().fromContainer(dc).build();
                            slots.next().offer(is);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(false);
                return;
            }
            future.complete(true);
        }).submit(MSDataSync.plugin);
        return future;
    }
}