package net.bandit.darkdoppelganger.network;

import io.netty.buffer.ByteBuf;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Cutscene messages. The server decides everything; clients only turn their own view. */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CutscenePayloads {
    private CutscenePayloads() {
    }

    /** Server to client: start the intro, looking at the boss's spawn point. */
    public record Start(double x, double y, double z, int duration, int emerge)
            implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "cutscene_start"));
        public static final StreamCodec<ByteBuf, Start> CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Start::x,
                ByteBufCodecs.DOUBLE, Start::y,
                ByteBufCodecs.DOUBLE, Start::z,
                ByteBufCodecs.VAR_INT, Start::duration,
                ByteBufCodecs.VAR_INT, Start::emerge,
                Start::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: end the cutscene now (the boss is gone, or the player died or changed dimension). */
    public record End() implements CustomPacketPayload {
        public static final Type<End> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "cutscene_end"));
        public static final StreamCodec<ByteBuf, End> CODEC = StreamCodec.unit(new End());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Client handlers sit behind lambdas, so the dedicated server never loads client classes.
        registrar.playToClient(Start.TYPE, Start.CODEC,
                (payload, context) -> net.bandit.darkdoppelganger.client.CutsceneClient.start(payload));
        registrar.playToClient(End.TYPE, End.CODEC,
                (payload, context) -> net.bandit.darkdoppelganger.client.CutsceneClient.stop());
    }
}
