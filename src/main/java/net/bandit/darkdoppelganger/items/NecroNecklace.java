package net.bandit.darkdoppelganger.items;

import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.bandit.darkdoppelganger.curios.CurioNecklaceItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;


import static io.redspace.ironsspellbooks.api.registry.AttributeRegistry.*;

public class NecroNecklace extends CurioNecklaceItem {
    private static final int MAX_MANA_BOOST = 200;
    private static final double ELDRITCH_SPELL_POWER_BOOST = 0.25;

    public NecroNecklace(Properties properties) {
        super(properties);

        withAttributes(
                new AttributeContainer(MAX_MANA, MAX_MANA_BOOST, AttributeModifier.Operation.ADD_VALUE),
                new AttributeContainer(ELDRITCH_SPELL_POWER, ELDRITCH_SPELL_POWER_BOOST, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        );
    }

}
