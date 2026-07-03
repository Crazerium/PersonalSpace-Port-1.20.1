package me.eigenraven.personalspace.client;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(
        modid = PersonalSpace.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PersonalAirIntakeModelAlias {
    private PersonalAirIntakeModelAlias() {
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        BakedModel inventoryModel = models.get(
                new ModelResourceLocation("minecraft", "respawn_anchor", "inventory")
        );

        BakedModel blockModel = models.get(
                new ModelResourceLocation("minecraft", "respawn_anchor", "charges=0")
        );

        if (blockModel == null) {
            blockModel = models.get(new ModelResourceLocation("minecraft", "respawn_anchor", "charges=1"));
        }

        if (blockModel == null) {
            blockModel = inventoryModel;
        }

        if (inventoryModel == null) {
            inventoryModel = blockModel;
        }

        if (blockModel == null && inventoryModel == null) {
            BakedModel portalModel = models.get(
                    new ModelResourceLocation(PersonalSpace.MODID, "personal_portal", "return_portal=false")
            );

            blockModel = portalModel;
            inventoryModel = portalModel;
        }

        if (inventoryModel != null) {
            models.put(
                    new ModelResourceLocation(PersonalSpace.MODID, "personal_air_intake", "inventory"),
                    inventoryModel
            );
        }

        if (blockModel != null) {
            models.put(
                    new ModelResourceLocation(PersonalSpace.MODID, "personal_air_intake", "return_portal=false"),
                    blockModel
            );

            models.put(
                    new ModelResourceLocation(PersonalSpace.MODID, "personal_air_intake", "return_portal=true"),
                    blockModel
            );

            models.put(
                    new ModelResourceLocation(PersonalSpace.MODID, "personal_air_intake", ""),
                    blockModel
            );
        }
    }
}