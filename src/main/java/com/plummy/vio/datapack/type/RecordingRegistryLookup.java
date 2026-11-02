package com.plummy.vio.datapack.type;

import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RecordingRegistryLookup implements RegistryOps.RegistryInfoLookup {

    private final HolderLookup.Provider registries;
    private final Set<String> missing = new LinkedHashSet<>();

    public RecordingRegistryLookup(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
        Optional<HolderLookup.RegistryLookup<E>> lookup = registries.lookup(registryKey);

        return lookup.map(registryLookup -> new RegistryOps.RegistryInfo<>(registryLookup, new RecordingGetter<>(registryLookup), registryLookup.registryLifecycle()));
    }

    public List<String> getMissing() {
        return List.copyOf(missing);
    }

    private class RecordingGetter<E> implements HolderGetter<E> {

        private final HolderLookup.RegistryLookup<E> lookup;

        private RecordingGetter(HolderLookup.RegistryLookup<E> lookup) {
            this.lookup = lookup;
        }

        @Override
        public Optional<Holder.Reference<E>> get(ResourceKey<E> key) {
            Optional<Holder.Reference<E>> holder = lookup.get(key);

            if (holder.isPresent()) {
                return holder;
            }

            missing.add("Unknown %s '%s'".formatted(key.registry(), key.location()));
            return Optional.of(Holder.Reference.createStandAlone(lookup, key));
        }

        @Override
        public Optional<HolderSet.Named<E>> get(TagKey<E> key) {
            Optional<HolderSet.Named<E>> tag = lookup.get(key);

            if (tag.isPresent()) {
                return tag;
            }

            missing.add("Unknown %s tag '#%s'".formatted(key.registry().location(), key.location()));
            return Optional.of(HolderSet.emptyNamed(lookup, key));
        }
    }
}
