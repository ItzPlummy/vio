package com.plummy.vio.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class VioPackSource implements RepositorySource {

    public static final String PACK_ID = Vio.MODID;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;

    public VioPackSource(Path root) {
        this.root = root;
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> onLoad) {
        try {
            prepare();
        } catch (IOException e) {
            Vio.getLogger().error("Failed to prepare Vio's data pack at %s".formatted(root), e);
            return;
        }

        Pack pack = Pack.readMetaAndCreate(
                new PackLocationInfo(PACK_ID, Component.literal("Vio"), PackSource.WORLD, Optional.empty()),
                new PathPackResources.PathResourcesSupplier(root),
                PackType.SERVER_DATA,
                new PackSelectionConfig(true, Pack.Position.TOP, false)
        );

        if (pack == null) {
            Vio.getLogger().error("Vio's data pack at %s couldn't be read, so it isn't loaded".formatted(root));
            return;
        }

        onLoad.accept(pack);
    }

    private void prepare() throws IOException {
        Files.createDirectories(root.resolve("data"));

        Path metadata = root.resolve("pack.mcmeta");

        if (Files.exists(metadata)) {
            return;
        }

        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
        pack.addProperty("description", "Vio's data pack");

        JsonObject json = new JsonObject();
        json.add("pack", pack);

        Files.writeString(metadata, GSON.toJson(json), StandardCharsets.UTF_8);
    }
}
