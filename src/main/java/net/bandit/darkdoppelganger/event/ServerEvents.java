package net.bandit.darkdoppelganger.event;


//@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ServerEvents {
//    @SubscribeEvent
//    public static void onSpellCasted(SpellPreCastEvent event) {
//        if(event.getEntity().getType() != EntityRegistry.DARK_DOPPELGANGER.get()){
//            if(event.getSpellId().equals(SpellRegistry.ROOT_SPELL.get().getSpellId()) || event.getSpellId().equals(SpellRegistry.SLOW_SPELL.get().getSpellId())
//                    || event.getSpellId().equals(SpellRegistry.BLIGHT_SPELL.get().getSpellId()) || event.getSpellId().equals(SpellRegistry.ACID_ORB_SPELL.get().getSpellId())){
//                if(event.getEntity() != null){
//                    boolean doppelClose = event.getEntity().level().getEntitiesOfClass(DarkDoppelgangerEntity_tyros_class.class, event.getEntity().getBoundingBox().inflate(50, 50, 50)).isEmpty();
//                    if(!doppelClose){
//                        event.setCanceled(true);
//                        if(event.getEntity() instanceof ServerPlayer player){
//                            if(event.getSpellId().equals(SpellRegistry.ROOT_SPELL.get().getSpellId()) || event.getSpellId().equals(SpellRegistry.BLIGHT_SPELL.get().getSpellId()) || event.getSpellId().equals(SpellRegistry.SLOW_SPELL.get().getSpellId())){
////                                Messages.message(new ClientboundSyncTargetingData(SpellRegistry.getSpell(event.getSpellId()), new ArrayList<>()), player);
//                            }
//                            if(event.getSpellId().equals(SpellRegistry.SLOW_SPELL.get().getSpellId())){
//                                event.getEntity().level().getEntitiesOfClass(TargetedAreaEntity.class, event.getEntity().getBoundingBox().inflate(50, 50, 50)).forEach(Entity::discard);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//    //This logic essentially protects the "original" DarkDoppelganger from taking damage as long as there are clones of itself nearby.
//    @SubscribeEvent
//    public static void onLivingHurt(LivingHurtEvent event){
//        if(event.getEntity().getType() == EntityRegistry.DARK_DOPPELGANGER.get()){
//            DarkDoppelgangerEntity_tyros_class doppel = (DarkDoppelgangerEntity_tyros_class) event.getEntity();
//            if(!doppel.isClone){
//                boolean noClones = event.getEntity().level().getEntitiesOfClass(DarkDoppelgangerEntity_tyros_class.class, event.getEntity().getBoundingBox().inflate(20, 10, 20), (target) -> target.isClone).isEmpty();
//                if(!noClones){
//                    event.setCanceled(true);
//                }
//            }
//        }
//    }
}

