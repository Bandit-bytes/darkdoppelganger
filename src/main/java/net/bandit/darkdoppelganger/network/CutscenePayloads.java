package net.bandit.darkdoppelganger.network;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** Cutscene messages. The server decides everything; clients only turn their own view. */
public final class CutscenePayloads {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "cutscene"),
            () -> PROTOCOL,
            // Accept clients/servers without the channel, so vanilla-side connections are not refused.
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private CutscenePayloads() {
    }

    /** Server to client: start the intro for a boss spawning at x, y, z. */
    public record Start(double x, double y, double z, int duration, int emerge) {
        void encode(FriendlyByteBuf buf) {
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeVarInt(duration);
            buf.writeVarInt(emerge);
        }

        static Start decode(FriendlyByteBuf buf) {
            return new Start(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt());
        }
    }

    /** Server to client: end the cutscene now (the boss is gone, or the player died or changed dimension). */
    public record End() {
        void encode(FriendlyByteBuf buf) {
        }

        static End decode(FriendlyByteBuf buf) {
            return new End();
        }
    }

    public static void register() {
        int id = 0;
        // Client handlers sit behind DistExecutor lambdas, so the dedicated server never loads client classes.
        CHANNEL.registerMessage(id++, Start.class, Start::encode, Start::decode,
                (payload, context) -> handle(context, () -> () -> net.bandit.darkdoppelganger.client.CutsceneClient.start(payload)),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, End.class, End::encode, End::decode,
                (payload, context) -> handle(context, () -> net.bandit.darkdoppelganger.client.CutsceneClient::stop),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static void handle(Supplier<NetworkEvent.Context> context, Supplier<Runnable> clientAction) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, clientAction));
        ctx.setPacketHandled(true);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
