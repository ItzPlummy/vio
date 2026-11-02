package com.plummy.vio.mixin;

import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.datapack.DatapackStorage;
import com.plummy.vio.datapack.VioPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPacksSource.class)
public abstract class ServerPacksSourceMixin {

    @Inject(
            method = "createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;",
            at = @At("RETURN")
    )
    private static void vio$addPackSource(LevelStorageSource.LevelStorageAccess level, CallbackInfoReturnable<PackRepository> callback) {
        if (ServerConfig.DATAPACK_ENABLED.get()) {
            callback.getReturnValue().addPackFinder(new VioPackSource(DatapackStorage.getRoot(level.getLevelPath(new LevelResource("serverconfig")))));
        }
    }
}
