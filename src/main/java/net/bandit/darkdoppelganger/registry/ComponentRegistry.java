package net.bandit.darkdoppelganger.registry;

import com.mojang.serialization.Codec;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class ComponentRegistry {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(
            Registries.DATA_COMPONENT_TYPE, DarkDoppelgangerMod.MOD_ID
    );

    public static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> buf.writeUUID(uuid),
            buf -> buf.readUUID()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> THROWER_UUID = COMPONENTS.register("thrower_uuid", () ->
            DataComponentType.<UUID>builder()
                    .persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
                    .networkSynchronized(UUID_CODEC)
                    .build()
    );

    public static void init(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
