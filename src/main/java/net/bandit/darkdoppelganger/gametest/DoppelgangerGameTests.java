package net.bandit.darkdoppelganger.gametest;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.event.SummonDoppelganger;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-world checks for the Dark Doppelganger. Only registered when the game test namespace is
 * enabled (runGameTestServer), so they are inert in normal play.
 */
@GameTestHolder(DarkDoppelgangerMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class DoppelgangerGameTests {
    private static final String ARENA = "doppel_arena";
    // Relative y=1 is the arena floor in this GameTest version, so entities stand at y=2.
    private static final BlockPos CENTER = new BlockPos(12, 2, 12);

    /** Fraction of incoming damage the boss keeps after scaling past the max health cap. */
    private static float scale(DarkDoppelgangerEntity boss) {
        return boss.getMaxHealth() / boss.getEffectiveMaxHealth();
    }

    /** Health lost from a hit of {@code raw} after damage scaling and the boss's base armor (10, toughness 0). */
    private static float expectedLoss(DarkDoppelgangerEntity boss, float raw) {
        float scaled = raw * scale(boss);
        float reduction = Math.min(20.0F, Math.max(10.0F / 5.0F, 10.0F - scaled / 2.0F)) / 25.0F;
        return scaled * (1.0F - reduction);
    }

    // ---------------------------------------------------------------- helpers

    private static DarkDoppelgangerEntity spawnBoss(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(EntityRegistry.DARK_DOPPELGANGER.get(), pos);
    }

    private static IronGolem spawnDummy(GameTestHelper helper, BlockPos pos) {
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, pos);
        golem.setNoAi(true);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        golem.setHealth(1000.0F);
        return golem;
    }

    private static DamageSource mobHit(GameTestHelper helper, Entity attacker) {
        return helper.getLevel().damageSources().mobAttack((IronGolem) attacker);
    }

    /** Applies damage and returns how much health was actually lost. */
    private static float damage(DarkDoppelgangerEntity boss, DamageSource source, float amount) {
        boss.invulnerableTime = 0;
        float before = boss.getHealth();
        boss.hurt(source, amount);
        return before - boss.getHealth();
    }

    private static List<DarkDoppelgangerEntity> doppelsAround(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(DarkDoppelgangerEntity.class, helper.getBounds().inflate(64));
    }

    /**
     * A fake player placed in the world. The vanilla mock player goes through the real join flow,
     * where Iron's Spellbooks tries to sync config over a connection with no negotiated channels.
     * NeoForge's FakePlayer drops all packets, so it is safe here.
     */
    private static ServerPlayer mockPlayerAt(GameTestHelper helper, BlockPos relative) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "doppel-test-" + relative.getX() + "-" + relative.getZ()));
        var abs = helper.absoluteVec(relative.getCenter());
        player.moveTo(abs.x, abs.y, abs.z, 0, 0);
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    private static String describe(DarkDoppelgangerEntity boss) {
        var running = boss.goalSelector.getAvailableGoals().stream()
                .filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList();
        return " [target=" + (boss.getTarget() == null ? "none" : boss.getTarget().getType().toShortString())
                + ", sees=" + (boss.getTarget() != null && boss.getSensing().hasLineOfSight(boss.getTarget()))
                + ", casting=" + boss.isCasting() + ", noAi=" + boss.isNoAi()
                + ", goals=" + running + ", hand=" + boss.getMainHandItem().getItem()
                + ", tick=" + boss.tickCount + ", pos=" + boss.blockPosition()
                + (boss.getTarget() == null ? "" : ", targetPos=" + boss.getTarget().blockPosition() + ", clip=" + clipInfo(boss)) + "]";
    }

    private static String clipInfo(DarkDoppelgangerEntity boss) {
        var from = boss.getEyePosition();
        var to = boss.getTarget().getEyePosition();
        var hit = boss.level().clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, boss));
        return hit.getType() + "@" + hit.getBlockPos() + "=" + boss.level().getBlockState(hit.getBlockPos()).getBlock();
    }

    /** Removes everything the boss fight can leave behind, then passes the test. */
    private static void cleanupAndSucceed(GameTestHelper helper, ServerPlayer... players) {
        AABB area = helper.getBounds().inflate(64);
        List<Entity> leftovers = new ArrayList<>(helper.getLevel().getEntitiesOfClass(Entity.class, area,
                e -> !(e instanceof ServerPlayer)));
        leftovers.forEach(Entity::discard);
        for (ServerPlayer player : players) {
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    // ---------------------------------------------------------------- tests

    @GameTest(template = ARENA, batch = "doppel_heal", timeoutTicks = 200)
    public static void bossNeverHeals(GameTestHelper helper) {
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);
        boss.setHealth(1000.0F);
        boss.heal(200.0F);
        check(helper, boss.getHealth() == 1000.0F, "heal() restored health: " + boss.getHealth());

        boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 4));
        helper.runAfterDelay(80, () -> {
            check(helper, boss.getHealth() <= 1000.0F, "Regeneration healed the boss to " + boss.getHealth());
            cleanupAndSucceed(helper);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_armor", timeoutTicks = 200)
    public static void copiedArmorDoesNotProtect(GameTestHelper helper) {
        IronGolem attacker = spawnDummy(helper, new BlockPos(2, 2, 2));
        DarkDoppelgangerEntity bare = spawnBoss(helper, new BlockPos(8, 2, 12));
        DarkDoppelgangerEntity geared = spawnBoss(helper, new BlockPos(16, 2, 12));

        var protection = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.PROTECTION);
        ItemStack[] armor = {new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            armor[i].enchant(protection, 4);
            geared.setItemSlot(slots[i], armor[i]);
        }

        helper.runAfterDelay(5, () -> {
            check(helper, geared.getArmorValue() > bare.getArmorValue(), "Copied armor attributes were not applied at all");
            float bareLoss = damage(bare, mobHit(helper, attacker), 40.0F);
            float gearedLoss = damage(geared, mobHit(helper, attacker), 40.0F);
            float expected = expectedLoss(bare, 40.0F);
            check(helper, Math.abs(bareLoss - expected) < 0.05F, "Bare boss took " + bareLoss + ", expected " + expected);
            check(helper, Math.abs(gearedLoss - bareLoss) < 0.05F,
                    "Prot IV netherite changed damage taken: geared " + gearedLoss + " vs bare " + bareLoss);
            cleanupAndSucceed(helper);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_bow", timeoutTicks = 600)
    public static void bowWieldingBossShootsArrows(GameTestHelper helper) {
        DarkDoppelgangerEntity boss = spawnBoss(helper, new BlockPos(4, 2, 12));
        IronGolem target = spawnDummy(helper, new BlockPos(20, 2, 12));
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        boss.setTarget(target);
        boolean[] sawDraw = {false};

        helper.succeedWhen(() -> {
            boss.setTarget(target);
            sawDraw[0] |= boss.isUsingItem() && boss.getUseItem().is(Items.BOW);
            boolean arrowInFlight = !helper.getLevel().getEntitiesOfClass(AbstractArrow.class, helper.getBounds().inflate(32),
                    a -> a.getOwner() == boss).isEmpty();
            DamageSource last = target.getLastDamageSource();
            boolean hitByArrow = last != null && last.is(DamageTypes.ARROW) && last.getEntity() == boss;
            if (!arrowInFlight && !hitByArrow) helper.fail("No arrow fired by the boss yet" + describe(boss));
            check(helper, sawDraw[0], "Boss fired without visibly drawing the bow");
            cleanupAndSucceed(helper);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_crossbow", timeoutTicks = 600)
    public static void crossbowWieldingBossShootsArrows(GameTestHelper helper) {
        DarkDoppelgangerEntity boss = spawnBoss(helper, new BlockPos(4, 2, 12));
        IronGolem target = spawnDummy(helper, new BlockPos(20, 2, 12));
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        boss.setTarget(target);

        helper.succeedWhen(() -> {
            boss.setTarget(target);
            boolean arrowInFlight = !helper.getLevel().getEntitiesOfClass(AbstractArrow.class, helper.getBounds().inflate(32),
                    a -> a.getOwner() == boss).isEmpty();
            DamageSource last = target.getLastDamageSource();
            boolean hitByArrow = last != null && last.is(DamageTypes.ARROW) && last.getEntity() == boss;
            if (!arrowInFlight && !hitByArrow) helper.fail("No crossbow bolt fired by the boss yet" + describe(boss));
            cleanupAndSucceed(helper);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_spellpower", timeoutTicks = 100)
    public static void copiedSpellPowerIsCapped(GameTestHelper helper) {
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);
        boss.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(3.0);
        boss.getAttribute(AttributeRegistry.FIRE_SPELL_POWER).setBaseValue(2.5);
        double global = boss.getAttributeValue(AttributeRegistry.SPELL_POWER);
        double fire = boss.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER);
        check(helper, Math.abs(global - 1.3) < 1e-6, "Global spell power not capped: " + global);
        check(helper, Math.abs(fire - 1.3) < 1e-6, "Fire spell power not capped: " + fire);
        check(helper, boss.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "Cap broke a non-spell attribute");
        cleanupAndSucceed(helper);
    }

    @GameTest(template = ARENA, batch = "doppel_multiplayer", timeoutTicks = 100)
    public static void healthScalesWithPlayers(GameTestHelper helper) {
        ServerPlayer p1 = mockPlayerAt(helper, new BlockPos(2, 2, 2));
        ServerPlayer p2 = mockPlayerAt(helper, new BlockPos(22, 2, 22));
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);
        // 1500 base, +50% for the one extra player; real max health stays within Minecraft's cap
        float effective = boss.getEffectiveMaxHealth();
        check(helper, Math.abs(effective - 2250.0F) < 0.5F, "Expected 2250 effective health with 2 players, got " + effective);
        check(helper, boss.getMaxHealth() <= 1024.0F, "Max health exceeded the cap: " + boss.getMaxHealth());
        check(helper, boss.getHealth() == boss.getMaxHealth(), "Boss did not start at full health");
        cleanupAndSucceed(helper, p1, p2);
    }

    @GameTest(template = ARENA, batch = "doppel_phases", timeoutTicks = 400)
    public static void phasesRefillSpawnReflectionsAndAllowDeath(GameTestHelper helper) {
        ServerPlayer watcher = mockPlayerAt(helper, new BlockPos(2, 2, 2)); // receives title/darkness packets
        IronGolem attacker = spawnDummy(helper, new BlockPos(22, 2, 2));
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);
        float max = boss.getMaxHealth();
        float effective = boss.getEffectiveMaxHealth(); // damage is scaled when health exceeds the cap
        int settle = DarkDoppelgangerEntity.PHASE_TRANSITION_TICKS + 2;

        // Below 40% -> phase 2: three "Reflection" clones, then a window where it cannot be hurt and refills
        boss.setHealth(max * 0.45F);
        float before = boss.getHealth();
        boss.hurt(mobHit(helper, attacker), effective * 0.1F);
        check(helper, boss.isPhaseTransitioning(), "Phase 2 did not start a transition");
        check(helper, boss.getHealth() == before, "The hit that crossed into phase 2 still dealt damage");
        long reflections = doppelsAround(helper).stream()
                .filter(d -> d.isClone && d.getCustomName() != null && d.getCustomName().getString().equals("Reflection"))
                .count();
        check(helper, reflections == 3, "Expected 3 Reflection clones, found " + reflections);
        check(helper, watcher.hasEffect(MobEffects.DARKNESS), "Phase 2 did not apply Darkness to nearby players");
        check(helper, !watcher.hasEffect(MobEffects.BLINDNESS), "Phase 2 still applies Blindness");
        boss.invulnerableTime = 0;
        check(helper, !boss.hurt(mobHit(helper, attacker), 50.0F), "Boss took damage during the phase transition");

        helper.runAfterDelay(DarkDoppelgangerEntity.PHASE_TRANSITION_TICKS / 2, () -> {
            float mid = boss.getHealth();
            check(helper, mid > before && mid < max, "Health did not refill gradually: " + mid);
        });
        helper.runAfterDelay(settle, () -> {
            check(helper, !boss.isPhaseTransitioning(), "Phase 2 transition did not end");
            check(helper, boss.getHealth() == max, "Phase 2 did not refill health: " + boss.getHealth());
            check(helper, !boss.isNoGravity(), "Gravity was not restored after the transition");

            // Not killable before phase 3
            boss.invulnerableTime = 0;
            boss.hurt(mobHit(helper, attacker), effective * 0.85F);
            check(helper, boss.isPhaseTransitioning(), "Phase 3 did not start a transition");
            helper.runAfterDelay(settle, () -> {
                check(helper, boss.getHealth() == max, "Phase 3 did not refill health: " + boss.getHealth());

                // Phase 3 -> lethal damage now kills it, and its Reflections break with it
                boss.invulnerableTime = 0;
                boss.hurt(mobHit(helper, attacker), effective * 2.0F);
                helper.runAfterDelay(2, () -> {
                    check(helper, boss.isDeadOrDying() || boss.isRemoved(), "Boss survived lethal damage in phase 3");
                    long clonesLeft = doppelsAround(helper).stream().filter(d -> d.isClone).count();
                    check(helper, clonesLeft == 0, clonesLeft + " clones outlived the boss");
                    cleanupAndSucceed(helper, watcher);
                });
            });
        });
    }

    @GameTest(template = ARENA, batch = "doppel_retarget", timeoutTicks = 100)
    public static void onlyRetaliatesAgainstPlayersAndTheirPets(GameTestHelper helper) {
        ServerPlayer owner = mockPlayerAt(helper, new BlockPos(2, 2, 2));
        IronGolem stray = spawnDummy(helper, new BlockPos(4, 2, 12));
        Wolf pet = helper.spawn(EntityType.WOLF, new BlockPos(20, 2, 12));
        pet.setNoAi(true);
        pet.setTame(true, false);
        pet.setOwnerUUID(owner.getUUID());
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);

        boss.hurt(mobHit(helper, stray), 5.0F);
        check(helper, boss.getTarget() != stray, "Boss turned on an unowned mob that hit it");
        boss.invulnerableTime = 0;
        boss.hurt(helper.getLevel().damageSources().mobAttack(pet), 5.0F);
        check(helper, boss.getTarget() == pet, "Boss ignored a player's pet that hit it");
        cleanupAndSucceed(helper, owner);
    }

    @GameTest(template = ARENA, batch = "doppel_theft", timeoutTicks = 600)
    public static void mirrorTheftCapsResistanceAndSkipsEvasion(GameTestHelper helper) {
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(6, 2, 12));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 4));
        player.addEffect(new MobEffectInstance(io.redspace.ironsspellbooks.registries.MobEffectRegistry.EVASION, 6000, 2));
        IronGolem golem = spawnDummy(helper, new BlockPos(22, 2, 2));
        DarkDoppelgangerEntity boss = spawnBoss(helper, new BlockPos(18, 2, 12));
        boss.setNoAi(true); // mechanics run in tick(); no AI keeps spells from touching the player
        boss.setHealth(boss.getMaxHealth() * 0.45F);
        boss.hurt(mobHit(helper, golem), boss.getEffectiveMaxHealth() * 0.1F); // into phase 2, where theft starts

        helper.succeedWhen(() -> {
            boss.setTarget(player);
            MobEffectInstance stolen = boss.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (stolen == null) helper.fail("No Resistance stolen yet");
            check(helper, stolen.getAmplifier() <= 1, "Stolen Resistance not capped: amplifier " + stolen.getAmplifier());
            check(helper, !player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Player kept the stolen Resistance");
            check(helper, player.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.EVASION), "Evasion was stolen");
            check(helper, !boss.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.EVASION), "Boss has Evasion");
            cleanupAndSucceed(helper, player);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_boat", timeoutTicks = 100)
    public static void cannotBeTrappedInABoat(GameTestHelper helper) {
        Boat boat = helper.spawn(EntityType.BOAT, new BlockPos(12, 2, 8));
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);
        check(helper, !boss.startRiding(boat), "Boss boarded a boat");
        cleanupAndSucceed(helper);
    }

    @GameTest(template = ARENA, batch = "doppel_void", timeoutTicks = 200)
    public static void returnsFromTheVoidWhileTargetingAMob(GameTestHelper helper) {
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(4, 2, 4));
        IronGolem golem = spawnDummy(helper, new BlockPos(20, 2, 20));
        DarkDoppelgangerEntity boss = spawnBoss(helper, CENTER);
        // Keep the target on the golem, so the rescue has to find the player on its own
        boss.targetSelector.removeAllGoals(goal -> true);
        boss.setTarget(golem);
        double voidY = helper.getLevel().getMinBuildHeight() - 20;
        boolean[] dropped = {false};
        helper.runAfterDelay(5, () -> {
            boss.teleportTo(boss.getX(), voidY, boss.getZ());
            dropped[0] = true;
        });
        helper.succeedWhen(() -> {
            if (!dropped[0]) helper.fail("Boss not dropped yet");
            if (boss.getY() < helper.getLevel().getMinBuildHeight()) helper.fail("Boss still in the void at y=" + boss.getY());
            check(helper, boss.distanceToSqr(player) < 8.0 * 8.0, "Boss returned far from the player: " + boss.blockPosition());
            check(helper, !helper.getLevel().noCollision(boss.getBoundingBox().move(0.0, -1.0, 0.0)),
                    "Boss returned with no ground under it at " + boss.blockPosition());
            cleanupAndSucceed(helper, player);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_command", timeoutTicks = 200)
    public static void summonCommandWaitsWithoutBlockingTheServer(GameTestHelper helper) {
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(6, 2, 6));
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withLevel(helper.getLevel())
                .withPosition(helper.absoluteVec(CENTER.getCenter()))
                .withSuppressedOutput();

        long start = System.nanoTime();
        int result = SummonDoppelganger.summonDoppelganger(source);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        check(helper, result == 1, "Command failed");
        check(helper, elapsedMs < 1000, "Command blocked the server thread for " + elapsedMs + " ms");
        check(helper, doppelsAround(helper).isEmpty(), "Boss spawned before the countdown ended");

        helper.runAfterDelay(110, () -> {
            check(helper, doppelsAround(helper).stream().anyMatch(d -> !d.isClone),
                    "Boss did not spawn after the countdown");
            cleanupAndSucceed(helper, player);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_exposed", timeoutTicks = 400)
    public static void signatureSpellLeavesBossExposed(GameTestHelper helper) {
        IronGolem target = spawnDummy(helper, new BlockPos(20, 2, 12));
        DarkDoppelgangerEntity boss = spawnBoss(helper, new BlockPos(6, 2, 12));
        boss.setTarget(target);

        helper.runAfterDelay(50, () -> boss.initiateCastSpell(SpellRegistry.MAGIC_ARROW_SPELL.get(), 3));
        helper.succeedWhen(() -> {
            if (!boss.hasEffect(MobEffects.GLOWING)) helper.fail("Boss not exposed after its signature spell" + describe(boss));
            float baseline = expectedLoss(boss, 40.0F);
            float loss = damage(boss, mobHit(helper, target), 40.0F);
            // Big enough hits stay in the flat 8% armor band, so the exposed bonus shows as exactly 1.25x
            check(helper, Math.abs(loss / baseline - 1.25F) < 0.01F, "Exposed boss took " + loss + ", expected 1.25x of " + baseline);
            cleanupAndSucceed(helper);
        });
    }

    @GameTest(template = ARENA, batch = "doppel_adapt", timeoutTicks = 300)
    public static void learnedWeaponAttacksAreResistedGently(GameTestHelper helper) {
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(2, 2, 2));
        IronGolem golem = spawnDummy(helper, new BlockPos(22, 2, 2));
        DarkDoppelgangerEntity melee = spawnBoss(helper, new BlockPos(6, 2, 12));
        DarkDoppelgangerEntity magic = spawnBoss(helper, new BlockPos(18, 2, 12));
        // Only the damage maths is under test; spells and buffs cast during the wait would add noise
        melee.setNoAi(true);
        magic.setNoAi(true);

        // Learning only happens from phase 2 onward
        melee.hurt(mobHit(helper, golem), melee.getEffectiveMaxHealth() * 0.7F);
        magic.hurt(mobHit(helper, golem), magic.getEffectiveMaxHealth() * 0.7F);
        helper.runAfterDelay(DarkDoppelgangerEntity.PHASE_TRANSITION_TICKS + 2,
                () -> checkLearnedResistance(helper, player, melee, magic));
    }

    private static void checkLearnedResistance(GameTestHelper helper, ServerPlayer player,
                                               DarkDoppelgangerEntity melee, DarkDoppelgangerEntity magic) {
        DamageSource sword = helper.getLevel().damageSources().playerAttack(player);
        float first = damage(melee, sword, 40.0F);
        damage(melee, sword, 40.0F);
        damage(melee, sword, 40.0F); // third repeat -> learned
        float learned = damage(melee, sword, 40.0F);
        check(helper, Math.abs(learned / first - 0.8F) < 0.01F,
                "Learned melee should deal 80% (phase 2), got " + (learned / first));

        DamageSource spell = helper.getLevel().damageSources().indirectMagic(player, player);
        float firstSpell = damage(magic, spell, 40.0F);
        damage(magic, spell, 40.0F);
        damage(magic, spell, 40.0F);
        float learnedSpell = damage(magic, spell, 40.0F);
        check(helper, Math.abs(learnedSpell / firstSpell - 0.65F) < 0.01F,
                "Learned spells should deal 65% (phase 2), got " + (learnedSpell / firstSpell));
        cleanupAndSucceed(helper, player);
    }
}
