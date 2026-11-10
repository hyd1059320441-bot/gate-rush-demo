package com.example.gaterush.p7c91a3e2.domain.usecase

import com.example.gaterush.p7c91a3e2.data.repository.LevelRepository
import com.example.gaterush.p7c91a3e2.domain.model.*
import com.example.gaterush.p7c91a3e2.domain.model.Target
import kotlin.math.*

/** Fixed-step authoritative simulation. Rendering never decides hits or rewards. */
class PlayGameUseCase(repository: LevelRepository) {
    val levels = repository.levels().also { require(it.isNotEmpty()) }
    var phase = Phase.LOADING; private set
    var levelIndex = 0; private set
    var elapsed = 0f; private set
    var hp = 100; private set
    var x = 0f; private set
    var targetX = 0f; private set
    var stacks = Stacks(); private set
    var generation = 0; private set
    var dragging = false; private set
    var lastShotAt = -100f; private set
    var lastHitAt = -100f; private set
    var warningLane = -1; private set
    var message = ""; private set
    val targets = mutableListOf<Target>()
    val bullets = mutableListOf<Bullet>()
    private val feedback = ArrayDeque<Feedback>()
    private var accumulator = 0f
    private var cursor = 0
    private var serial = 0L
    private var cadence = 0f
    private var messageUntil = 0f
    private var terminalDefeated = false
    private var nextPulse = 50f
    private var pulseAt = Float.POSITIVE_INFINITY
    private var previousPhase = Phase.READY
    private val granted = hashSetOf<Long>()
    fun assetsReady() { if(phase == Phase.LOADING || phase == Phase.LOAD_ERROR) phase = Phase.READY }
    fun assetsFailed(reason: String) { phase = Phase.LOAD_ERROR; message = reason; cancelDrag() }
    fun loading() { phase = Phase.LOADING; message = "" }
    fun start() {
        if(phase !in setOf(Phase.READY,Phase.PAUSED,Phase.FAILED)) return
        reset(); phase = Phase.RUNNING
    }
    private fun reset() {
        hp=100; x=0f; targetX=0f; stacks=Stacks(); elapsed=0f; accumulator=0f
        targets.clear(); bullets.clear(); feedback.clear(); granted.clear(); cursor=0
        cadence=0f; lastShotAt=-100f; lastHitAt=-100f; message=""; messageUntil=0f
        terminalDefeated=false; dragging=false; warningLane=-1
        nextPulse=50f; pulseAt=Float.POSITIVE_INFINITY; generation++
    }
    fun pause() { if(phase == Phase.RUNNING) { phase=Phase.PAUSED; accumulator=0f }; cancelDrag() }
    fun resume() { if(phase == Phase.PAUSED) { phase=Phase.RUNNING; accumulator=0f } }
    fun rules() { if(phase == Phase.READY || phase == Phase.PAUSED) { previousPhase=phase; phase=Phase.RULES } }
    fun back() { if(phase == Phase.RULES) phase=previousPhase }
    fun next() { if(phase == Phase.WON && levelIndex < levels.lastIndex) { levelIndex++; reset(); phase=Phase.READY } }
    fun replay() { if(phase == Phase.COMPLETE) { levelIndex=0; reset(); phase=Phase.READY } }
    fun beginDrag() { if(phase == Phase.RUNNING) dragging=true }
    fun drag(deltaMeters: Float) {
        if(dragging && phase == Phase.RUNNING && deltaMeters.isFinite())
            targetX=(targetX+deltaMeters).coerceIn(-.32f,.32f)
    }
    fun cancelDrag() { dragging=false }
    fun snapshot() = GameSnapshot(phase,levelIndex,levels[levelIndex].title,levels[levelIndex].goal,
        hp,elapsed.toInt(),stacks,message,
        targets.firstOrNull { it.alive && it.wave.terminal }?.hp?.let { ceil(it).toInt() } ?: 0,
        dragging,warningLane,generation)
    fun drainFeedback(): List<Feedback> = feedback.toList().also { feedback.clear() }
    fun advance(frameSeconds: Float) {
        if(phase != Phase.RUNNING || !frameSeconds.isFinite() || frameSeconds <= 0f) return
        accumulator += min(frameSeconds,.05f)
        while(accumulator >= STEP && phase == Phase.RUNNING) {
            accumulator-=STEP; tick(STEP)
        }
    }
    private fun tick(dt: Float) {
        elapsed += dt
        x += (targetX-x) * (1-exp(-dt/.055f))
        val events=levels[levelIndex].events
        while(cursor<events.size && events[cursor].at<=elapsed) {
            val wave=events[cursor++]
            targets.add(Target(++serial,wave))
            if(wave.boss) nextPulse=wave.at+6f
        }
        targets.filter { it.alive }.forEach {
            it.previousDepth=it.depth
            val slow=if(elapsed<it.iceUntil) it.slowFactor else 1f
            it.depth -= .15f * it.wave.speed * slow * dt
            if(it.wave.terminal) it.depth=max(it.depth,.75f)
        }
        targets.filter { it.alive && it.wave.kind==Kind.ENEMY }.forEach { target ->
            // Refreshing burn never postpones its next tick; at most one due tick per fixed step.
            if(target.nextBurnAt<=elapsed+STEP*.25f &&
                target.nextBurnAt<=target.burnUntil+STEP*.25f) {
                damage(target,target.burnDamage,ShotType.BURN)
                feedback.add(Feedback("burn",target.wave.x,target.depth,shotType=ShotType.BURN,
                    targetKind=Kind.ENEMY))
                if(target.alive) target.nextBurnAt+=.4f
            }
        }
        cadence-=dt
        if(cadence<=0f) {
            cadence += 1f/min(8f,4f*1.25f.pow(stacks.speed))
            val offsets=when(stacks.spread) {
                1 -> listOf(-.08f,0f,.08f); 2 -> listOf(-.16f,-.08f,0f,.08f,.16f)
                else -> listOf(0f)
            }
            offsets.forEach { offset ->
                if(bullets.size<96) bullets.add(Bullet(++serial,x+offset,stacks.pierce,stacks.explosive,
                    stacks.ice,stacks.chain,stacks.burn))
            }
            lastShotAt=elapsed
        }
        // Relative-motion sweep; resolve damage before the runner crosses an object.
        bullets.forEach { bullet ->
            if(!bullet.alive) return@forEach
            bullet.previousDepth=bullet.depth; bullet.depth+=1.8f*dt
            val hits=targets.filter { target ->
                target.alive && CombatGeometry.shootable(target.wave.kind) &&
                target.id !in bullet.hits &&
                abs(bullet.x-target.wave.x)<=target.width+bullet.halfWidth &&
                bullet.previousDepth <= target.previousDepth+target.halfDepth+bullet.halfDepth &&
                bullet.depth >= target.depth-target.halfDepth-bullet.halfDepth
            }.sortedBy { it.depth-it.halfDepth }
            for(target in hits) {
                if(!bullet.alive || !target.alive) continue
                bullet.hits.add(target.id)
                damage(target,if(target.wave.armoured && bullet.pierce==0) 4f else 8f,bullet.type)
                applyStatus(target,bullet)
                feedback.add(Feedback("impact",bullet.x,max(0f,target.depth-target.halfDepth),
                    shotType=bullet.type,targetKind=target.wave.kind))
                if(bullet.explosive>0 && !bullet.burst) {
                    bullet.burst=true
                    val radius=if(bullet.explosive==1) .13f else .18f
                    val splash=if(bullet.explosive==1) 4f else 6f
                    feedback.add(Feedback("blast",target.wave.x,target.depth))
                    targets.filter { it.alive && it.id!=target.id &&
                        CombatGeometry.shootable(it.wave.kind) &&
                        hypot(it.wave.x-target.wave.x,it.depth-target.depth)<=radius
                    }.forEach {
                        damage(it,if(it.wave.armoured) splash*.5f else splash,bullet.type)
                        applyStatus(it,bullet)
                    }
                }
                chain(target,bullet)
                if(bullet.hits.size>=1+bullet.pierce) bullet.alive=false
            }
            if(bullet.depth>1.85f) bullet.alive=false
        }
        targets.filter { it.alive && !it.wave.terminal }.forEach { target ->
            if(target.depth<=.05f && target.previousDepth>=-.05f) {
                if(abs(target.wave.x-x)<=target.width+CombatGeometry.RUNNER_HALF_WIDTH) {
                    if(target.wave.kind==Kind.CRATE) grant(target.id,target.wave.reward)
                    else hit(if(target.wave.kind==Kind.GATE) 15 else 20)
                    feedback.add(Feedback(if(target.wave.kind==Kind.CRATE) "pickup" else
                        if(target.wave.kind==Kind.GATE) "gate" else "enemy",target.wave.x,0f,target.wave.reward))
                }
                target.alive=false
            }
            if(target.depth<-.1f) target.alive=false
        }
        bossPulse()
        targets.removeAll { !it.alive }; bullets.removeAll { !it.alive }
        if(elapsed>messageUntil) message=""
        if(hp<=0) { phase=Phase.FAILED; message="生命耗尽"; cancelDrag(); bullets.clear() }
        else if(elapsed+STEP*.25f>=levels[levelIndex].duration) {
            phase=if(terminalDefeated) {
                if(levelIndex==levels.lastIndex) Phase.COMPLETE else Phase.WON
            } else Phase.FAILED
            message=if(terminalDefeated) "核心击破！" else "终点目标未击破"
            cancelDrag(); bullets.clear(); warningLane=-1
        }
    }
    private fun applyStatus(target: Target,bullet: Bullet) {
        if(!target.alive || target.wave.kind!=Kind.ENEMY) return
        if(bullet.ice>0) {
            val factor=if(bullet.ice==1) .60f else .40f
            target.slowFactor=if(elapsed<target.iceUntil) min(target.slowFactor,factor) else factor
            target.iceUntil=max(target.iceUntil,elapsed+if(bullet.ice==1) 1.4f else 2f)
        }
        if(bullet.burn>0) {
            val active=elapsed<target.burnUntil
            val strength=if(bullet.burn==1) 2f else 3f
            target.burnDamage=if(active) max(target.burnDamage,strength) else strength
            if(!active) target.nextBurnAt=elapsed+.4f
            target.burnUntil=elapsed+2f
        }
    }
    private fun chain(origin: Target,bullet: Bullet) {
        if(bullet.chain<=0 || bullet.chained) return
        bullet.chained=true
        val visited=bullet.hits.toMutableSet()
        var source=origin
        val radius=if(bullet.chain==1) .32f else .52f
        repeat(bullet.chain.coerceAtMost(2)) {
            val next=targets.filter { it.alive && it.wave.kind==Kind.ENEMY && it.id !in visited &&
                hypot(it.wave.x-source.wave.x,it.depth-source.depth)<=radius }
                .minWithOrNull(compareBy<Target>(
                    { hypot(it.wave.x-source.wave.x,it.depth-source.depth) },{ it.id }))
                ?: return
            visited.add(next.id)
            feedback.add(Feedback("chain",source.wave.x,source.depth,shotType=ShotType.CHAIN,
                targetKind=Kind.ENEMY,endX=next.wave.x,endDepth=next.depth))
            val amount=if(bullet.chain==1) 6f else 8f
            damage(next,if(next.wave.armoured && bullet.pierce==0) amount*.5f else amount,ShotType.CHAIN)
            applyStatus(next,bullet)
            source=next
        }
    }
    private fun damage(target: Target,amount: Float,shotType: ShotType = ShotType.NORMAL) {
        if(!target.alive) return
        target.hp=max(0f,target.hp-amount); target.hitAt=elapsed
        if(target.hp<=0f) {
            target.alive=false
            target.iceUntil=-100f; target.burnUntil=-100f
            target.slowFactor=1f; target.burnDamage=0f; target.nextBurnAt=Float.POSITIVE_INFINITY
            val kind=when(target.wave.kind) { Kind.GATE->"gate"; Kind.CRATE->"crate"; else->"enemy" }
            feedback.add(Feedback(kind,target.wave.x,target.depth,shotType=shotType,
                targetKind=target.wave.kind))
            if(target.wave.kind==Kind.GATE || target.wave.kind==Kind.CRATE)
                grant(target.id,target.wave.reward)
            if(target.wave.terminal) { terminalDefeated=true; warningLane=-1; pulseAt=Float.POSITIVE_INFINITY }
        }
    }
    private fun grant(id: Long,reward: Upgrade?) {
        if(reward==null || !granted.add(id)) return
        if(reward==Upgrade.HEAL) {
            val before=hp; hp=min(100,hp+25); message="回血 +${hp-before}"
        } else {
            val before=stacks.count(reward); stacks=stacks.add(reward)
            message=if(before==reward.cap) "${reward.label} MAX" else "${reward.label} +1 / ${stacks.count(reward)}"
        }
        messageUntil=elapsed+1.4f
        feedback.add(Feedback(if(reward==Upgrade.HEAL) "heal" else "reward",x,0f,reward))
    }
    private fun hit(damage: Int) {
        if(elapsed-lastHitAt<.7f) return
        lastHitAt=elapsed
        if(stacks.shield>0) {
            stacks=stacks.copy(shield=stacks.shield-1)
            message="护盾抵挡"; feedback.add(Feedback("shield",x,0f))
        } else {
            hp=max(0,hp-damage); feedback.add(Feedback("hurt",x,0f)); message="生命 -$damage"
        }
        messageUntil=elapsed+1.4f
    }
    private fun bossPulse() {
        val boss=targets.firstOrNull { it.alive && it.wave.boss } ?: return
        if(elapsed>=nextPulse && warningLane<0) {
            warningLane=listOf(-.23f,0f,.23f).indices.minBy { abs(x-listOf(-.23f,0f,.23f)[it]) }
            pulseAt=elapsed+1.2f; nextPulse=elapsed+if(boss.hp<boss.wave.hp*.5f) 2f else 3f
        }
        if(elapsed>=pulseAt && warningLane>=0) {
            val lane=listOf(-.23f,0f,.23f)[warningLane]
            if(abs(x-lane)<.115f) hit(20)
            feedback.add(Feedback("blast",lane,0f)); warningLane=-1; pulseAt=Float.POSITIVE_INFINITY
        }
    }
    companion object { const val STEP=1f/60f }
}
