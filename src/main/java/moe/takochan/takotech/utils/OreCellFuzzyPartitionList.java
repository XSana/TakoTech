package moe.takochan.takotech.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.minecraft.item.ItemStack;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;
import appeng.util.item.OreHelper;
import appeng.util.item.OreReference;
import appeng.util.prioitylist.IPartitionList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

@SuppressWarnings("Guava")
public class OreCellFuzzyPartitionList implements IPartitionList<IAEItemStack> {

    private static final List<OrePrefixes> PREFIXES = new ArrayList<>();
    private static final Cache<IAEItemStack, List<IAEItemStack>> CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(24, TimeUnit.HOURS) // 过期时间
        .maximumSize(10240) // 最大缓存数量
        .build();

    static {
        PREFIXES.add(OrePrefixes.ore);
        PREFIXES.add(OrePrefixes.rawOre);
        PREFIXES.add(OrePrefixes.oreBlackgranite);
        PREFIXES.add(OrePrefixes.oreRedgranite);
        PREFIXES.add(OrePrefixes.oreMarble);
        PREFIXES.add(OrePrefixes.oreBasalt);
        PREFIXES.add(OrePrefixes.oreNetherrack);
        PREFIXES.add(OrePrefixes.oreNether);
        PREFIXES.add(OrePrefixes.oreDense);
        PREFIXES.add(OrePrefixes.oreRich);
        PREFIXES.add(OrePrefixes.oreNormal);
        PREFIXES.add(OrePrefixes.oreSmall);
        PREFIXES.add(OrePrefixes.orePoor);
        PREFIXES.add(OrePrefixes.oreEndstone);
        PREFIXES.add(OrePrefixes.oreEnd);
    }

    private final IItemList<IAEItemStack> list;

    public OreCellFuzzyPartitionList(IItemList<IAEItemStack> priorityList) {
        this.list = AEApi.instance()
            .storage()
            .createItemList();

        for (IAEItemStack priorityItem : priorityList) {
            List<IAEItemStack> expanded;
            try {
                expanded = CACHE.get(priorityItem, () -> {
                    OreReference oreRef = OreHelper.INSTANCE.isOre(priorityItem.getItemStack());
                    if (oreRef == null) {
                        return Collections.singletonList(priorityItem);
                    }
                    List<IAEItemStack> result = new ArrayList<>();
                    for (String dict : oreRef.getEquivalents()) {
                        if (!dict.startsWith("ore") && !dict.startsWith("rawOre")) {
                            result.add(priorityItem);
                            continue;
                        }

                        Materials materials = OrePrefixes.getMaterial(dict);
                        if (materials == null) continue;

                        for (OrePrefixes prefix : PREFIXES) {
                            List<ItemStack> itemStacks = GTOreDictUnificator.getOres(prefix, materials);
                            for (ItemStack is : itemStacks) {
                                result.add(AEItemStack.create(is));
                            }
                        }
                    }

                    return result;
                });
            } catch (ExecutionException e) {
                expanded = Collections.singletonList(priorityItem);
            }
            for (IAEItemStack expandedItem : expanded) {
                this.list.add(expandedItem);
            }
        }
    }

    @Override
    public boolean isListed(IAEItemStack input) {
        return list.findPrecise(input) != null;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return this.list;
    }
}
