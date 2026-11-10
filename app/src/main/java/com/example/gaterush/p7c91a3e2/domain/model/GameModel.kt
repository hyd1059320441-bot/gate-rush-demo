package com.example.gaterush.p7c91a3e2.domain.model

enum class Phase { LOADING, READY, RUNNING, PAUSED, FAILED, WON, COMPLETE, RULES, LOAD_ERROR }
enum class Kind { GATE, ENEMY, CRATE, OBSTACLE }
enum class Upgrade(val label: String, val cap: Int) {
    SPREAD("散射", 2), PIERCE("穿透", 2), EXPLOSIVE("爆炸弹", 2),
    SPEED("攻速", 3), SHIELD("护盾", 3), HEAL("回血", 0),
    ICE("冰冻弹", 2), CHAIN("电弧弹", 2), BURN("燃烧弹", 2)
}
/** Core dimensions also define the sweep; trails and collars are cosmetic. */
enum class ShotType(val color: String, val halfWidth: Float, val halfDepth: Float) {
    NORMAL("cream", .007f, .022f), PIERCE("violet", .005f, .0375f),
    EXPLOSIVE("coral", .012f, .015f), ICE("ice", .011f, .023f),
    CHAIN("electric", .007f, .032f), BURN("flame", .0095f, .020f);
    companion object {
        fun from(pierce: Int, explosive: Int, ice: Int, chain: Int, burn: Int) = when {
            burn>0 -> BURN; chain>0 -> CHAIN; ice>0 -> ICE
            explosive>0 -> EXPLOSIVE; pierce>0 -> PIERCE; else -> NORMAL
        }
    }
}
data class Wave(val at: Float, val kind: Kind, val x: Float, val hp: Float = 0f,
    val reward: Upgrade? = null, val terminal: Boolean = false,
    val armoured: Boolean = false, val speed: Float = 1f, val boss: Boolean = false)
data class Level(val title: String, val goal: String, val events: List<Wave>, val duration: Float = 60f)
data class Stacks(val spread: Int = 0, val pierce: Int = 0, val explosive: Int = 0,
    val speed: Int = 0, val shield: Int = 0, val ice: Int = 0, val chain: Int = 0,
    val burn: Int = 0) {
    fun count(type: Upgrade) = when(type) {
        Upgrade.SPREAD -> spread; Upgrade.PIERCE -> pierce; Upgrade.EXPLOSIVE -> explosive
        Upgrade.SPEED -> speed; Upgrade.SHIELD -> shield; Upgrade.HEAL -> 0
        Upgrade.ICE -> ice; Upgrade.CHAIN -> chain; Upgrade.BURN -> burn
    }
    fun add(type: Upgrade) = when(type) {
        Upgrade.SPREAD -> copy(spread = (spread + 1).coerceAtMost(type.cap))
        Upgrade.PIERCE -> copy(pierce = (pierce + 1).coerceAtMost(type.cap))
        Upgrade.EXPLOSIVE -> copy(explosive = (explosive + 1).coerceAtMost(type.cap))
        Upgrade.SPEED -> copy(speed = (speed + 1).coerceAtMost(type.cap))
        Upgrade.SHIELD -> copy(shield = (shield + 1).coerceAtMost(type.cap))
        Upgrade.ICE -> copy(ice = (ice + 1).coerceAtMost(type.cap))
        Upgrade.CHAIN -> copy(chain = (chain + 1).coerceAtMost(type.cap))
        Upgrade.BURN -> copy(burn = (burn + 1).coerceAtMost(type.cap))
        Upgrade.HEAL -> this
    }
}
class Target(val id: Long, val wave: Wave, var depth: Float = 1.65f) {
    var hp = if(wave.kind==Kind.CRATE && wave.hp<=0f) CombatGeometry.CRATE_HP else wave.hp
    var alive = true
    var previousDepth = depth
    var hitAt = -100f
    var iceUntil = -100f
    var slowFactor = 1f
    var burnUntil = -100f
    var nextBurnAt = Float.POSITIVE_INFINITY
    var burnDamage = 0f
    val width get() = CombatGeometry.halfWidth(wave)
    val halfDepth get() = CombatGeometry.halfDepth(wave)
}
class Bullet(val id: Long, val x: Float, val pierce: Int, val explosive: Int,
    val ice: Int = 0, val chain: Int = 0, val burn: Int = 0) {
    val type = ShotType.from(pierce, explosive, ice, chain, burn)
    val halfWidth get() = type.halfWidth
    val halfDepth get() = type.halfDepth
    var depth = CombatGeometry.SHOT_START_DEPTH
    var previousDepth = depth
    val hits = hashSetOf<Long>()
    var burst = false
    var chained = false
    var alive = true
}
data class Feedback(val kind: String, val x: Float, val depth: Float, val reward: Upgrade? = null,
    val shotType: ShotType = ShotType.NORMAL, val targetKind: Kind? = null,
    val endX: Float? = null, val endDepth: Float? = null)
data class GameSnapshot(val phase: Phase = Phase.LOADING, val level: Int = 0,
    val title: String = "", val goal: String = "", val hp: Int = 100, val seconds: Int = 0,
    val stacks: Stacks = Stacks(), val message: String = "", val bossHp: Int = 0,
    val dragging: Boolean = false, val warningLane: Int = -1, val generation: Int = 0)
