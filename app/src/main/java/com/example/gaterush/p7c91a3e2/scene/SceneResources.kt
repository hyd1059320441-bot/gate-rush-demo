package com.example.gaterush.p7c91a3e2.scene

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.ecs.simulation.*
import com.pico.spatial.core.math.*
import kotlin.math.abs

/** Scene-lifetime cache. All cloned models preserve their imported transform chain. */
internal class SceneResources(private val root: Entity) : AutoCloseable {
    private val resources=mutableListOf<Resource>()
    private val templates=mutableMapOf<String,Entity>()
    private val materials=mutableMapOf<String,Material>()
    private val cache=group(root,"ModelTemplates").apply { enabled=false }
    val cube=own(MeshResource.createBox(Vector3(1f),.06f))
    val sphere=own(MeshResource.createSphere(.5f))
    val ring=own(MeshResource.createTorus(.5f,.035f))
    val hitShape=own(ShapeResource.createBox(Vector3(.16f,.28f,.16f)/GateRushScene.FIT))
    val physics=own(PhysicsMaterialResource())
    private var closed=false
    fun <T:Resource> own(value:T):T = value.also { it.toGlobal(); resources.add(it) }
    fun color(name:String):Material = materials.getOrPut(name) {
        val hex=when(name) {
            "sky"->0x40BBDD; "mint"->0x88D7B4; "coral"->0xF18482; "sun"->0xF3C65D
            "ice"->0xA9E5EC; "electric"->0xBEB0EE; "flame"->0xF5AD62
            "violet"->0xAAA0D9; "cream"->0xF7F2DE; "road"->0xB4C9CA
            "ground"->0xA5C6B7; "hill"->0x88BAAB; "hillFar"->0xB7CFC2; else->0x233B4A
        }
        own(UnlitMaterial.create()).apply {
            setBaseColor(Color4(((hex shr 16) and 255)/255f,((hex shr 8) and 255)/255f,(hex and 255)/255f,1f))
        }
    }
    fun box(parent:Entity,name:String,position:Vector3,size:Vector3,color:String,
        mesh:MeshResource=cube):Entity = group(parent,name).apply {
        pose().setPosition(position).setScaleVector(size)
        components.set(ModelComponent(mesh,this@SceneResources.color(color)))
        components.set(DynamicShadowComponent(castsShadowEnabled=false,receivesShadowEnabled=false))
    }
    fun physical(parent:Entity,name:String,x:Float,y:Float,z:Float,
        sx:Float,sy:Float,sz:Float,color:String,mesh:MeshResource=cube) =
        box(parent,name,Vector3(x,y,z)/GateRushScene.FIT,
            Vector3(sx,sy,sz)/GateRushScene.FIT,color,mesh)
    fun label(parent:Entity,key:String,y:Float,z:Float,width:Float=.16f):Entity {
        val material=materials.getOrPut("label:$key") {
            own(UnlitMaterial.create()).apply {
                setBaseColorTexture(own(TextureResource.load("labels/$key.png")))
            }
        }
        return group(parent,"Label_$key").apply {
            pose().setPosition(Vector3(0f,y,z)/GateRushScene.FIT)
                .setScaleVector(Vector3(width,.048f,.003f)/GateRushScene.FIT)
            components.set(ModelComponent(cube,material))
            components.set(DynamicShadowComponent(castsShadowEnabled=false,receivesShadowEnabled=false))
        }
    }
    suspend fun preload() {
        for(name in ASSETS) {
            val model=Entity.loadSuspend("asset://models/$name.usdz")
            if(closed) { model.destroy(); error("Scene disposed during import") }
            cache.addChild(model); templates[name]=model
            val nodes=flatten(model)
            nodes.forEach {
                it.components.remove(InteractableComponent::class.java)
                it.components.remove(CollisionComponent::class.java)
            }
            // Collect nodes before material edits; native material children may become stale.
            nodes.mapNotNull { it.components[ModelComponent::class.java] }.forEach { mc ->
                mc.materials.filterIsInstance<PhysicallyBasedMaterial>().forEach {
                    it.setRoughness(.78f); it.setMetallic(.03f)
                }
            }
            Log.i("GateRush","ASSET_READY name=$name")
        }
    }
    fun model(parent:Entity,name:String,asset:String,size:Vector3,yaw:Float=0f,
        topAnchor:Boolean=false):Entity {
        val owner=group(parent,name)
        val instance=checkNotNull(templates[asset]?.clone(Entity.CloneOptions(
            recursive=true,shouldShareMaterialInstance=true))) { "Required model missing: $asset" }
        owner.addChild(instance); instance.enabled=true
        val geometry=group(owner,"${name}Geometry")
        geometry.addChild(instance)
        geometry.pose().setEulerAngles(EulerAngles(0f,yaw,0f))
        val raw=geometry.getVisualBounds(owner)
        check(!raw.isEmpty() && raw.size.x>0f && raw.size.y>0f && raw.size.z>0f)
        val scale=minOf(size.x/raw.size.x,size.y/raw.size.y,size.z/raw.size.z)/GateRushScene.FIT
        geometry.pose().setScaleVector(Vector3(scale))
        val scaled=geometry.getVisualBounds(owner)
        geometry.pose().setPosition(Vector3(-scaled.center.x,
            if(topAnchor) -scaled.max.y else -scaled.min.y,-scaled.center.z))
        val final=geometry.getVisualBounds(owner)
        check(abs(if(topAnchor) final.max.y else final.min.y)<.0001f)
        Log.i("GateRush","ASSET_PLACEMENT name=$name source=$asset owner=$name " +
            "raw=${raw.size} scale=$scale final=${final.size} min=${final.min} max=${final.max} " +
            "numeric=PASS visual=UNOBSERVED")
        return owner
    }
    override fun close() { closed=true; resources.forEach { it.close() }; resources.clear() }
    companion object {
        val ASSETS=listOf("RunnerUpperImage","RunnerSingleLegImage","Blaster","GateFrame","HoverEnemy","SupplyCrate","Lamp")
    }
}
internal fun group(parent:Entity,name:String)=Entity().apply {
    setName(name); components.set(TransformComponent()); parent.addChild(this)
}
internal fun Entity.pose():TransformComponent =
    components[TransformComponent::class.java] ?: TransformComponent().also { components.set(it) }
internal fun flatten(root:Entity):List<Entity> = listOf(root)+root.getChildren().flatMap(::flatten)
