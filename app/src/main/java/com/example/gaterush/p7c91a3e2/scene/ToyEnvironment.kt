package com.example.gaterush.p7c91a3e2.scene

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.MaterialCullingMode
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.*
import kotlin.math.*

/** Quiet full-surround backdrop; world units stay outside the fitted gameplay hierarchy. */
internal class ToyEnvironment(private val root:Entity,private val kit:SceneResources) {
    fun build() {
        val sky=kit.own(UnlitMaterial.create()).apply {
            setBaseColor(Color4(1f,1f,1f,1f))
            setBaseColorTexture(kit.own(TextureResource.load("textures/toy-sky.png")))
            setCullingMode(MaterialCullingMode.FRONT)
        }
        group(root,"ToySky").apply {
            pose().setPosition(Vector3(0f,0f,0f)).setScaleVector(Vector3(160f))
            components.set(ModelComponent(kit.sphere,sky))
            components.set(DynamicShadowComponent(castsShadowEnabled=false,receivesShadowEnabled=false))
        }
        kit.box(root,"SoftGround",Vector3(0f,.39f,0f),Vector3(100f,.08f,100f),"ground")
        repeat(18) { i ->
            val angle=i*2f*PI.toFloat()/18f
            val radius=8.5f+(i%3)*1.6f
            kit.box(root,"DistantHill$i",Vector3(sin(angle)*radius,-.70f,cos(angle)*radius),
                Vector3(3.8f+(i%4)*.65f,2.7f+(i%3)*.45f,3.7f),
                if(i%2==0) "hill" else "hillFar",kit.sphere)
        }
        for(side in listOf(-1f,1f)) {
            repeat(5) { i ->
                val x=side*(2.1f+(i%2)*.8f)
                val z=-3.8f+i*1.65f
                kit.box(root,"GardenIsland${side}_$i",Vector3(x,.45f,z),
                    Vector3(.85f,.12f,.66f),"hillFar",kit.sphere)
                kit.box(root,"GardenStem${side}_$i",Vector3(x,.62f,z),
                    Vector3(.07f,.28f,.07f),"cream")
                kit.box(root,"GardenCrown${side}_$i",Vector3(x,.87f,z),
                    Vector3(.45f,.42f,.45f),"hill",kit.sphere)
            }
        }
        Log.i("GateRush","ENVIRONMENT_READY sky=textures/toy-sky.png culling=FRONT " +
            "ground=1 distantHills=18 gardens=10 style=soft-toy fullSurround=true")
    }
}
