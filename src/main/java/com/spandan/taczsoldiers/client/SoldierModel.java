package com.spandan.taczsoldiers.client;

import com.spandan.taczsoldiers.SoldierEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Player-shaped model that raises both arms into a rifle stance while fighting. */
public class SoldierModel extends HumanoidModel<SoldierEntity> {
    public SoldierModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(SoldierEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.isAggressive() && !entity.getMainHandItem().isEmpty()) {
            AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
        }
    }
}
