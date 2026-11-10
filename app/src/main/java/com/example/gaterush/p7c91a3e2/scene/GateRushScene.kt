package com.example.gaterush.p7c91a3e2.scene

import android.content.Context
import android.util.Log
import com.example.gaterush.p7c91a3e2.domain.model.*
import com.example.gaterush.p7c91a3e2.domain.usecase.PlayGameUseCase
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.System as EcsSystem
import com.pico.spatial.core.ecs.simulation.*
import com.pico.spatial.core.math.*
import kotlin.math.*
import kotlinx.coroutines.yield

/** Stable stage, one uniform fit, pooled views driven only by authoritative simulation. */
class GateRushScene(context:Context,val game:PlayGameUseCase,private val publish:()->Unit):AutoCloseable {
    val root=Entity().apply { setName("placementOwner"); components.set(TransformComponent()) }
    private val fittedSceneRoot=group(root,"fittedSceneRoot").apply {
        pose().setScaleVector(Vector3(FIT)).setPosition(Vector3(0f,.75f,-1.02f))
    }
    private val kit=SceneResources(fittedSceneRoot)
    private val audio=GameAudio(context.applicationContext)
    private var foreground=true
    private var containerFocused=true
    private var heardShotAt=-100f
    private var audioPhase=Phase.LOADING
    private val runner=group(fittedSceneRoot,"Runner")
    private val body=group(runner,"RunnerBody").apply {
        pose().setScaleVector(Vector3(CombatGeometry.RUNNER_SCALE))
    }
    val avatarHit=group(runner,"AvatarDragTarget")
    private lateinit var leftHip:Entity
    private lateinit var rightHip:Entity
    private lateinit var gun:Entity
    private lateinit var muzzle:Entity
    private lateinit var shield:Entity
    private val upgrades=mutableMapOf<Upgrade,List<Entity>>()
    private data class Slot(val kind:Kind,val root:Entity,val visual:Entity,
        val digits:DurabilityDigits?,val labels:Map<Upgrade,Entity>,
        val armour:Entity?,val frost:Entity?,val flame:Entity?,var id:Long=-1L)
    private val slots=mutableListOf<Slot>()
    private data class BulletView(val root:Entity,val core:Entity,val trail:Entity,
        val collar:Entity,var id:Long=-1L)
    private val bulletViews=mutableListOf<BulletView>()
    private data class Arc(val root:Entity,val segments:List<Entity>,var age:Float=1f)
    private val arcs=mutableListOf<Arc>()
    private var arcCursor=0
    private val seenShots=mutableSetOf<ShotType>()
    private val seenEffects=mutableSetOf<String>()
    private data class Prop(val root:Entity,val x:Float,val depth:Float,val ratio:Float,
        val height:Float=0f,val rotate:Boolean=false)
    private val props=mutableListOf<Prop>()
    private data class Particle(val entity:Entity,var age:Float=10f,var duration:Float=.5f,
        var x:Float=0f,var y:Float=0f,var depth:Float=0f,var vx:Float=0f,
        var vy:Float=0f,var vd:Float=0f,var ring:Boolean=false)
    private val particles=mutableListOf<Particle>()
    private val warnings=mutableListOf<Entity>()
    var initialized=false; private set
    private var closed=false
    private var hudClock=0f
    private var animationClock=0f
    private var deathClock=0f
    private var generation=-1
    private var previousPhase=Phase.LOADING
    private var particleCursor=0
    private var lastStack=Stacks()
    private var lastLog=0f
    suspend fun initialize(panel:Entity?) {
        check(!initialized)
        panel?.let { root.addChild(it); it.pose().setPosition(Vector3(0f,.55f,-.75f)) }
        fittedSceneRoot.enabled=false
        kit.preload()
        if(closed) return
        ToyEnvironment(root,kit).build()
        buildCourse()
        buildRunner()
        yield()
        repeat(4) { buildSlot(Kind.GATE,it) }
        repeat(CombatGeometry.ENEMY_SLOTS) { buildSlot(Kind.ENEMY,it) }
        repeat(4) { buildSlot(Kind.CRATE,it) }
        repeat(3) { buildSlot(Kind.OBSTACLE,it) }
        ShotType.entries.forEach { kit.color(it.color) }
        repeat(96) { i ->
            val owner=group(fittedSceneRoot,"Bullet$i").apply { enabled=false }
            val core=kit.physical(owner,"ShotCore",0f,0f,0f,.014f,.014f,.044f,"cream",kit.sphere)
            val trail=kit.physical(owner,"ShotTrail",0f,0f,.035f,.005f,.005f,.048f,"cream",kit.sphere)
            val collar=kit.physical(owner,"ShotCollar",0f,0f,0f,.028f,.028f,.005f,"ice",kit.ring)
                .apply { enabled=false }
            bulletViews+=BulletView(owner,core,trail,collar)
        }
        repeat(24) { i ->
            val owner=group(fittedSceneRoot,"Arc$i").apply { enabled=false }
            val segments=List(2) { part ->
                kit.physical(owner,"ArcSegment$part",0f,0f,0f,.004f,.004f,.04f,"electric")
            }
            arcs+=Arc(owner,segments)
        }
        repeat(160) { i ->
            particles+=Particle(kit.physical(fittedSceneRoot,"Particle$i",0f,0f,0f,
                .012f,.012f,.012f,"cream",if(i%8==0) kit.ring else kit.sphere)
                .apply { enabled=false })
        }
        buildScenery()
        props.forEach { it.root.pose().setPosition(Vector3(it.x,it.height,-it.depth)/FIT) }
        fittedSceneRoot.enabled=true
        val bounds=fittedSceneRoot.getVisualBounds(root)
        check(!bounds.isEmpty())
        Log.i("GateRush","SCENE_BOUNDS owner=placementOwner fit=$FIT min=${bounds.min} max=${bounds.max} " +
            "rootStable=true visual=UNOBSERVED")
        initialized=true
        GateRushSystem.runtime=this; EcsSystem.register(GateRushSystem::class.java)
        game.assetsReady(); publish()
        Log.i("GateRush","GAME_READY models=7 slots=${slots.size} enemySlots=${CombatGeometry.ENEMY_SLOTS} " +
            "scenery=${props.size} bullets=96 particles=160 arcSlots=24 shotStyles=6")
    }
    private fun buildCourse() {
        kit.physical(fittedSceneRoot,"RoundedCourse",0f,-.025f,-.73f,.84f,.05f,1.8f,"road")
        kit.physical(fittedSceneRoot,"CourseUnderside",0f,-.05f,-.73f,.80f,.018f,1.77f,"ink")
        for(side in listOf(-1f,1f)) {
            kit.physical(fittedSceneRoot,"Rail$side",side*.415f,.035f,-.73f,.018f,.03f,1.8f,"sky")
            kit.physical(fittedSceneRoot,"SafetyStripe$side",side*.38f,.002f,-.73f,.014f,.004f,1.8f,"sun")
        }
        repeat(3) { lane ->
            warnings+=kit.physical(fittedSceneRoot,"WarningLane$lane",(lane-1)*.23f,.005f,-.7f,
                .20f,.007f,1.65f,"coral").apply { enabled=false }
        }
        repeat(12) { i -> for(x in listOf(-.115f,.115f)) {
            val dash=kit.physical(fittedSceneRoot,"FlowDash${i}_$x",x,.003f,-i*.16f,
                .008f,.006f,.08f,"cream")
            props+=Prop(dash,x,i*.16f,1f,.003f)
        } }
    }
    private fun buildRunner() {
        kit.model(body,"UpperBody","RunnerUpperImage",Vector3(.12f,.14f,.09f),180f)
            .pose().setPosition(Vector3(0f,.095f,0f)/FIT)
        leftHip=group(body,"LeftHip").apply { pose().setPosition(Vector3(-.020f,.095f,0f)/FIT) }
        rightHip=group(body,"RightHip").apply { pose().setPosition(Vector3(.020f,.095f,0f)/FIT) }
        kit.model(leftHip,"LeftLeg","RunnerSingleLegImage",Vector3(.043f,.095f,.055f),180f,true)
        kit.model(rightHip,"RightLeg","RunnerSingleLegImage",Vector3(.043f,.095f,.055f),180f,true)
        gun=group(body,"BlasterRecoil").apply { pose().setPosition(Vector3(0f,.148f,-.04f)/FIT) }
        kit.model(gun,"GeneratedBlaster","Blaster",Vector3(.045f,.055f,.105f),-90f)
        muzzle=kit.physical(gun,"MuzzleFlash",0f,.022f,-.072f,.027f,.027f,.025f,"sun",kit.sphere)
            .apply { enabled=false }
        upgrades[Upgrade.SPREAD]=listOf(-1f,1f).map { side ->
            kit.physical(gun,"SpreadPod$side",side*.030f,.023f,-.040f,.018f,.020f,.045f,"mint")
                .apply { enabled=false }
        }
        upgrades[Upgrade.PIERCE]=listOf(kit.physical(gun,"PierceCollar",0f,.024f,-.059f,
            .035f,.035f,.012f,"violet",kit.ring).apply { enabled=false })
        upgrades[Upgrade.EXPLOSIVE]=listOf(kit.physical(gun,"BlastCollar",0f,.024f,-.066f,
            .039f,.039f,.012f,"coral",kit.ring).apply { enabled=false })
        upgrades[Upgrade.SPEED]=listOf(kit.physical(gun,"RateCoil",0f,.042f,-.025f,
            .030f,.009f,.030f,"sun",kit.sphere).apply { enabled=false })
        for((type,color) in listOf(Upgrade.ICE to "ice",Upgrade.CHAIN to "electric",Upgrade.BURN to "flame")) {
            val side=when(type) { Upgrade.ICE->-1f; Upgrade.BURN->1f; else->0f }
            upgrades[type]=listOf(kit.physical(gun,"${type.name}Cell",side*.026f,.045f,-.036f,
                .012f,.014f,.023f,color,kit.sphere).apply { enabled=false })
        }
        val small=CombatGeometry.RUNNER_SCALE
        shield=kit.physical(runner,"ShieldOrbit",0f,.08f*small,0f,
            .145f*small,.012f*small,.145f*small,"sky",kit.ring)
            .apply { pose().setEulerAngles(EulerAngles(90f,0f,0f)); enabled=false }
        avatarHit.pose().setPosition(Vector3(0f,.12f*small,0f)/FIT)
        avatarHit.components.set(CollisionComponent(listOf(kit.hitShape),kit.physics))
        avatarHit.components.set(InteractableComponent())
        val bounds=body.getVisualBounds(runner)
        check(abs(bounds.min.y*FIT)<.001f)
        Log.i("GateRush","RUNNER_NEUTRAL footBottom=${bounds.min.y*FIT} height=${bounds.size.y*FIT} " +
            "bodyScale=$small shotHeight=${CombatGeometry.SHOT_HEIGHT} " +
            "hipJoints=2 torsoYaw=180 legYaw=180 blasterYaw=-90 grounding=PASS visual=UNOBSERVED")
        muzzle.enabled=true
        val tip=muzzle.getVisualBounds(runner)
        muzzle.enabled=false
        check(!tip.isEmpty() && tip.min.y*FIT<=CombatGeometry.SHOT_HEIGHT &&
            tip.max.y*FIT>=CombatGeometry.SHOT_HEIGHT)
        check(-CombatGeometry.SHOT_START_DEPTH+CombatGeometry.BULLET_HALF_DEPTH>=tip.min.z*FIT &&
            -CombatGeometry.SHOT_START_DEPTH-CombatGeometry.BULLET_HALF_DEPTH<=tip.max.z*FIT)
        Log.i("GateRush","SHOT_ALIGNMENT owner=Runner muzzleMin=${tip.min*FIT} muzzleMax=${tip.max*FIT} " +
            "bulletY=${CombatGeometry.SHOT_HEIGHT} startDepth=${CombatGeometry.SHOT_START_DEPTH} " +
            "numeric=PASS visual=UNOBSERVED")
    }
    private fun buildSlot(kind:Kind,index:Int) {
        val root=group(fittedSceneRoot,"${kind.name}Slot$index").apply {
            pose().setScaleVector(Vector3(CombatGeometry.bodyScale(kind)))
        }
        val visual=group(root,"Visual")
        var armour:Entity?=null
        val digits=if(CombatGeometry.shootable(kind)) DurabilityDigits(kit,root,
            if(kind==Kind.CRATE) .055f else .067f,if(kind==Kind.CRATE) .056f else .06f) else null
        val labels=mutableMapOf<Upgrade,Entity>()
        when(kind) {
            Kind.GATE -> {
                kit.model(visual,"GateModel","GateFrame",Vector3(.20f,.27f,.09f))
                kit.physical(visual,"GateInfill",0f,.086f,.030f,.151f,.09f,.010f,"sun")
                kit.label(root,"hp",.135f,.063f,.09f)
            }
            Kind.ENEMY -> {
                kit.model(visual,"EnemyModel","HoverEnemy",Vector3(.13f,.14f,.12f))
                armour=kit.physical(visual,"ArmourBelt",0f,.044f,0f,.14f,.018f,.11f,"sun",kit.ring)
                    .apply { pose().setEulerAngles(EulerAngles(90f,0f,0f)); enabled=false }
            }
            Kind.CRATE -> kit.model(visual,"CrateModel","SupplyCrate",Vector3(.105f,.105f,.1f))
            Kind.OBSTACLE -> {
                kit.physical(visual,"Barrier",0f,.048f,0f,.15f,.095f,.06f,"coral")
                for(side in listOf(-1f,1f)) kit.physical(visual,"BarrierStripe$side",side*.034f,
                    .05f,.033f,.018f,.065f,.004f,"cream").pose().setEulerAngles(EulerAngles(0f,0f,25f))
            }
        }
        if(kind==Kind.GATE || kind==Kind.CRATE) Upgrade.entries.forEach {
            labels[it]=kit.label(root,it.name.lowercase(),if(kind==Kind.GATE) .020f else .125f,
                .064f,if(kind==Kind.GATE) .16f else .13f).apply { enabled=false }
        }
        val bounds=visual.getVisualBounds(fittedSceneRoot)
        check(!bounds.isEmpty())
        if(CombatGeometry.shootable(kind)) {
            val minimum=bounds.min.y*FIT+if(kind==Kind.ENEMY) .027f else 0f
            val maximum=bounds.max.y*FIT+if(kind==Kind.ENEMY) .003f else 0f
            val radius=ShotType.entries.maxOf { it.halfWidth }
            check(CombatGeometry.SHOT_HEIGHT-radius>=minimum &&
                CombatGeometry.SHOT_HEIGHT+radius<=maximum) {
                "$kind body misses shot plane: $minimum..$maximum"
            }
        }
        Log.i("GateRush","TARGET_GEOMETRY kind=$kind scale=${CombatGeometry.bodyScale(kind)} " +
            "owner=fittedSceneRoot min=${bounds.min*FIT} max=${bounds.max*FIT} " +
            "shotHeight=${CombatGeometry.SHOT_HEIGHT} numeric=PASS visual=UNOBSERVED")
        // Status decorations are siblings of Visual, so they do not alter measured hit bodies.
        val frost=if(kind==Kind.ENEMY) kit.physical(root,"FrostOrbit",0f,.075f,0f,
            .152f,.012f,.152f,"ice",kit.ring).apply {
                pose().setEulerAngles(EulerAngles(90f,0f,0f)); enabled=false
            } else null
        val flame=if(kind==Kind.ENEMY) kit.physical(root,"BurnGlow",.068f,.10f,.025f,
            .023f,.045f,.023f,"flame",kit.sphere).apply { enabled=false } else null
        root.enabled=false; slots+=Slot(kind,root,visual,digits,labels,armour,frost,flame)
    }
    private fun buildScenery() {
        for(side in listOf(-1f,1f)) {
            repeat(7) { i ->
                val lamp=kit.model(fittedSceneRoot,"Lamp${side}_$i","Lamp",Vector3(.065f,.23f,.065f))
                props+=Prop(lamp,side*.47f,i*.28f,1f)
            }
            repeat(5) { i ->
                val pylon=group(fittedSceneRoot,"Pylon${side}_$i")
                kit.physical(pylon,"PylonBase",0f,.037f,0f,.10f,.075f,.10f,"cream")
                kit.physical(pylon,"RoundShrub",0f,.11f,0f,.12f,.11f,.12f,"mint",kit.sphere)
                props+=Prop(pylon,side*.64f,i*.46f,.72f)
            }
            repeat(3) { i ->
                val pod=group(fittedSceneRoot,"FarPod${side}_$i")
                kit.physical(pod,"PodBody",0f,.08f,0f,.16f,.16f,.16f,"violet",kit.sphere)
                kit.physical(pod,"PodCap",0f,.17f,0f,.13f,.034f,.13f,"cream")
                props+=Prop(pod,side*.86f,i*.65f,.4f,rotate=true)
            }
        }
    }
    fun update(context:SceneUpdateContext) {
        if(!initialized || closed || root.scene!=context.scene) return
        val dt=context.deltaTime.coerceIn(0f,.05f)
        val before=game.phase
        game.advance(dt)
        if(generation!=game.generation) {
            generation=game.generation; animationClock=0f; deathClock=0f; lastLog=-10f
            heardShotAt=-100f
            particles.forEach { it.age=10f; it.entity.enabled=false }
            arcs.forEach { it.age=1f; it.root.enabled=false }
            bulletViews.forEach { it.id=-1L; it.root.enabled=false }
            seenShots.clear(); seenEffects.clear()
        }
        val running=game.phase==Phase.RUNNING
        synchronizeAudioState()
        if(audioPhase!=game.phase) {
            if(game.phase==Phase.WON || game.phase==Phase.COMPLETE) audio.play("clear")
            audioPhase=game.phase
        }
        if(running && game.lastShotAt>=0f && game.lastShotAt!=heardShotAt) {
            heardShotAt=game.lastShotAt; audio.play("shot",game.x)
        }
        if(running) animationClock+=dt
        if(game.phase==Phase.FAILED && game.hp<=0) deathClock=min(.7f,deathClock+dt)
        runner.pose().setPosition(Vector3(game.x,0f,-min(game.elapsed/3f,1f)*.035f)/FIT)
        val cycle=animationClock*(2f*PI.toFloat()/.48f)
        val bob=if(running) abs(sin(cycle))*.009f*CombatGeometry.RUNNER_SCALE else 0f
        val hitAge=game.elapsed-game.lastHitAt
        val roll=if(hitAge<.24f && game.hp>0) sin(hitAge/.24f*PI.toFloat())*10f else 0f
        body.pose().setPosition(Vector3(0f,bob-deathClock/.7f*.04f*CombatGeometry.RUNNER_SCALE,0f)/FIT)
            .setEulerAngles(EulerAngles(if(deathClock>0f) deathClock/.7f*80f else if(running) -6f else 0f,0f,roll))
        val swing=if(running) sin(cycle)*28f else 0f
        leftHip.pose().setEulerAngles(EulerAngles(swing,0f,0f))
        rightHip.pose().setEulerAngles(EulerAngles(-swing,0f,0f))
        val shotAge=game.elapsed-game.lastShotAt
        val recoil=if(running && shotAge<.1f) .009f*(1-shotAge/.1f) else 0f
        gun.pose().setPosition(Vector3(0f,.148f,-.04f+recoil)/FIT)
        muzzle.enabled=running && shotAge<.06f
        if(lastStack!=game.stacks) {
            lastStack=game.stacks
            upgrades.forEach { (type,parts) -> parts.forEach { it.enabled=game.stacks.count(type)>0 } }
        }
        shield.enabled=game.stacks.shield>0
        shield.pose().setEulerAngles(EulerAngles(90f,animationClock*100f,0f))
        warnings.forEachIndexed { i,e -> e.enabled=running && game.warningLane==i }
        props.forEach { prop ->
            val depth=((prop.depth-game.elapsed*.15f*prop.ratio+.15f)%1.95f+1.95f)%1.95f-.15f
            prop.root.pose().setPosition(Vector3(prop.x,prop.height,-depth)/FIT)
            if(prop.rotate) prop.root.pose().setEulerAngles(EulerAngles(0f,animationClock*18f,0f))
        }
        slots.forEach { slot ->
            val target=game.targets.firstOrNull { it.id==slot.id && it.wave.kind==slot.kind }
                ?: game.targets.firstOrNull { t -> t.wave.kind==slot.kind && slots.none { it.id==t.id } }
            if(target==null) { slot.root.enabled=false; slot.id=-1L }
            else {
                slot.id=target.id; slot.root.enabled=true
                slot.root.pose().setPosition(Vector3(target.wave.x,
                    if(slot.kind==Kind.ENEMY) .015f+sin(animationClock*5.7f+target.id)*.012f else 0f,
                    -target.depth-min(game.elapsed/3f,1f)*.035f)/FIT)
                    .setScaleVector(Vector3(CombatGeometry.scale(target.wave)))
                val hitScale=if(game.elapsed-target.hitAt<.12f) .88f else 1f
                slot.visual.pose().setScaleVector(Vector3(1f,hitScale,1f))
                slot.digits?.set(target.hp)
                slot.labels.forEach { (type,label) -> label.enabled=target.wave.reward==type }
                slot.armour?.enabled=target.wave.armoured
                slot.frost?.enabled=game.elapsed<target.iceUntil
                slot.frost?.pose()?.setEulerAngles(EulerAngles(90f,animationClock*70f,0f))
                slot.flame?.enabled=game.elapsed<target.burnUntil
                slot.flame?.pose()?.setScaleVector(Vector3(.023f,.04f+sin(animationClock*12f)*.006f,.023f)/FIT)
                if(game.elapsed<target.iceUntil && seenEffects.add("ice"))
                    Log.i("GateRush","ELEMENT_ACTIVE kind=ice factor=${target.slowFactor} target=${target.id}")
                if(game.elapsed<target.burnUntil && seenEffects.add("burn"))
                    Log.i("GateRush","ELEMENT_ACTIVE kind=burn tick=${target.burnDamage} target=${target.id}")
            }
        }
        bulletViews.forEachIndexed { i,view ->
            val bullet=game.bullets.getOrNull(i)
            view.root.enabled=bullet!=null && running
            if(bullet!=null) {
                if(view.id!=bullet.id) { view.id=bullet.id; configureBullet(view,bullet) }
                view.root.pose().setPosition(Vector3(bullet.x,CombatGeometry.SHOT_HEIGHT,
                    -bullet.depth-min(game.elapsed/3f,1f)*.035f)/FIT)
            }
        }
        game.drainFeedback().forEach(::emit)
        if(running || game.phase==Phase.FAILED || game.phase==Phase.WON || game.phase==Phase.COMPLETE)
            { updateParticles(dt); updateArcs(dt) }
        avatarHit.enabled=running
        hudClock+=dt
        if(hudClock>=.1f || before!=game.phase || previousPhase!=game.phase) {
            hudClock=0f; publish(); previousPhase=game.phase
        }
        if(game.elapsed-lastLog>=10f || before!=game.phase) {
            lastLog=game.elapsed
            Log.i("GateRush","FRAME phase=${game.phase} level=${game.levelIndex+1} t=${game.elapsed} " +
                "hp=${game.hp} x=${game.x} targets=${game.targets.size} bullets=${game.bullets.size} gait=$swing")
        }
    }
    private fun configureBullet(view:BulletView,bullet:Bullet) {
        val type=bullet.type
        view.core.components.set(ModelComponent(
            if(type==ShotType.ICE || type==ShotType.CHAIN) kit.cube else kit.sphere,kit.color(type.color)))
        view.core.pose().setScaleVector(Vector3(type.halfWidth*2,type.halfWidth*2,type.halfDepth*2)/FIT)
        val tail=when(type) {
            ShotType.PIERCE->.065f; ShotType.EXPLOSIVE->.022f; ShotType.BURN->.055f; else->.045f
        }
        view.trail.pose().setPosition(Vector3(0f,0f,type.halfDepth+tail*.5f)/FIT)
            .setScaleVector(Vector3(.004f,.004f,tail)/FIT)
        view.trail.components[ModelComponent::class.java]?.materials?.set(0,
            kit.color(if(bullet.chain>0) "electric" else if(bullet.burn>0) "flame" else type.color))
        view.collar.enabled=bullet.ice>0 || type==ShotType.EXPLOSIVE || type==ShotType.CHAIN
        view.collar.components[ModelComponent::class.java]?.materials?.set(0,
            kit.color(if(bullet.ice>0) "ice" else type.color))
        if(seenShots.add(type)) Log.i("GateRush","SHOT_STYLE type=$type coreWidth=${type.halfWidth*2} " +
            "coreLength=${type.halfDepth*2} ice=${bullet.ice} chain=${bullet.chain} burn=${bullet.burn}")
    }
    private fun emitArc(event:Feedback) {
        val endX=event.endX ?: return
        val endDepth=event.endDepth ?: return
        val arc=arcs[arcCursor++%arcs.size]
        arc.age=0f; arc.root.enabled=true
        val middleX=(event.x+endX)*.5f+.018f
        val middleDepth=(event.depth+endDepth)*.5f-.015f
        placeArcSegment(arc.segments[0],event.x,event.depth,middleX,middleDepth)
        placeArcSegment(arc.segments[1],middleX,middleDepth,endX,endDepth)
    }
    private fun placeArcSegment(segment:Entity,sx:Float,sd:Float,ex:Float,ed:Float) {
        val dx=ex-sx; val dd=ed-sd
        segment.pose().setPosition(Vector3((sx+ex)*.5f,CombatGeometry.SHOT_HEIGHT,
            -(sd+ed)*.5f-min(game.elapsed/3f,1f)*.035f)/FIT)
            .setScaleVector(Vector3(.004f,.004f,max(.001f,hypot(dx,dd)))/FIT)
            .setEulerAngles(EulerAngles(0f,atan2(dx,-dd)*180f/PI.toFloat(),0f))
    }
    private fun updateArcs(dt:Float) {
        arcs.forEach { arc ->
            arc.age+=dt
            arc.root.enabled=arc.age<.14f
        }
    }
    private fun emit(event:Feedback) {
        if(event.kind=="chain") {
            emitArc(event)
            if(seenEffects.add("chain")) Log.i("GateRush","ELEMENT_ACTIVE kind=chain links=pooled " +
                "from=${event.x},${event.depth} to=${event.endX},${event.endDepth}")
            return
        }
        val cue=when(event.kind) {
            "heal","pickup","reward"->"pickup"
            "crate"->"gate"
            "burn"->""
            else->event.kind
        }
        audio.play(cue,event.x)
        val color=when(event.kind) {
            "gate"->"sun"; "enemy","blast","hurt"->"coral"; "heal","pickup","reward"->"mint"
            "crate"->"mint"; "shield"->"sky"; "burn"->"flame"
            "impact"->event.shotType.color; else->"cream"
        }
        val count=when(event.kind) { "gate"->18; "enemy","crate"->14; "impact"->6; "burn"->3; else->8 }
        val lifetime=when(event.kind) { "impact"->.15f; "blast","burn"->.25f; else->.5f }
        repeat(count) { i ->
            val p=particles[particleCursor++%particles.size]
            p.age=0f; p.duration=lifetime; p.x=event.x
            p.y=if(event.kind in setOf("heal","shield","reward","pickup","hurt"))
                .10f*CombatGeometry.RUNNER_SCALE else CombatGeometry.SHOT_HEIGHT
            p.depth=event.depth+min(game.elapsed/3f,1f)*.035f
            val a=(i*2.39996f)+animationClock
            p.vx=cos(a)*.18f; p.vd=sin(a)*.18f; p.vy=.13f+(i%3)*.035f
            p.ring=p.entity.getName().removePrefix("Particle").toInt()%8==0
            p.entity.components[ModelComponent::class.java]?.let { it.materials[0]=kit.color(color) }
            p.entity.enabled=true
        }
        if(event.kind!="impact" && event.kind!="burn")
            Log.i("GateRush","VFX kind=${event.kind} count=$count x=${event.x} depth=${event.depth}")
    }
    private fun updateParticles(dt:Float) {
        particles.forEach { p ->
            if(p.age>=p.duration) { p.entity.enabled=false; return@forEach }
            p.age+=dt
            val t=p.age; val fraction=(t/p.duration).coerceIn(0f,1f)
            p.entity.pose().setPosition(Vector3(p.x+p.vx*t,p.y+p.vy*t-.22f*t*t,-p.depth+p.vd*t)/FIT)
            val size=if(p.ring) .04f+fraction*.16f else .013f*(1-fraction)
            p.entity.pose().setScaleVector(Vector3(size,size,if(p.ring) size*.18f else size)/FIT)
                .setEulerAngles(EulerAngles(if(p.ring) 0f else t*260f,0f,t*180f))
        }
    }
    fun setForeground(value:Boolean) {
        foreground=value
        synchronizeAudioState()
    }
    fun setContainerFocused(value:Boolean) {
        if(containerFocused==value) return
        containerFocused=value
        synchronizeAudioState()
        Log.i("GateRush","STAGE_FOCUS value=$value")
    }
    fun synchronizeAudioState() {
        if(closed) return
        audio.setActive(foreground && containerFocused && when(game.phase) {
            Phase.RUNNING,Phase.FAILED,Phase.WON,Phase.COMPLETE->true
            else->false
        })
    }
    override fun close() {
        if(closed) return
        closed=true; game.pause()
        audio.close()
        if(GateRushSystem.runtime===this) { GateRushSystem.runtime=null; EcsSystem.unregister(GateRushSystem::class.java) }
        root.destroy(); kit.close()
        Log.i("GateRush","SCENE_DISPOSED")
    }
    companion object { const val FIT=.14f }
}
class GateRushSystem:EcsSystem() {
    override fun update(context:SceneUpdateContext) { runtime?.update(context) }
    companion object { var runtime:GateRushScene?=null }
}
