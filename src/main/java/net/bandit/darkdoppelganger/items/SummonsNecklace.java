package net.bandit.darkdoppelganger.items;


import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.bandit.darkdoppelganger.curios.CurioNecklaceItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;



import static io.redspace.ironsspellbooks.api.registry.AttributeRegistry.*;

public class SummonsNecklace  extends CurioNecklaceItem {
    private static final int MAX_MANA_BOOST = 225;
    private static final double SUMMON_BOOST = 0.25;

    public SummonsNecklace(Properties properties) {
        super(properties);

        withAttributes(
                new AttributeContainer(MAX_MANA, MAX_MANA_BOOST, AttributeModifier.Operation.ADD_VALUE),
                new AttributeContainer(SUMMON_DAMAGE, SUMMON_BOOST, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        );
    }
}

