package com.example.gaterush.p7c91a3e2.domain.model

/** Metres in the fitted course: one contract for the rendered bodies and swept hits. */
object CombatGeometry {
    const val RUNNER_SCALE=.70f
    const val SHOT_HEIGHT=.112f
    const val SHOT_START_DEPTH=.090f
    const val BULLET_HALF_WIDTH=.007f
    const val BULLET_HALF_DEPTH=.022f
    const val CRATE_HP=40f
    const val ENEMY_SLOTS=28
    const val RUNNER_HALF_WIDTH=.045f
    fun bodyScale(kind:Kind)=when(kind) {
        Kind.GATE->1.15f
        Kind.ENEMY->1.75f
        Kind.CRATE->1.50f
        Kind.OBSTACLE->1f
    }
    fun scale(wave:Wave)=bodyScale(wave.kind)*
        if(wave.terminal) if(wave.boss) 3.2f else 2f else 1f
    fun halfWidth(wave:Wave)=scale(wave)*when(wave.kind) {
        Kind.GATE->.100f
        Kind.ENEMY->.065f
        Kind.CRATE->.050f
        Kind.OBSTACLE->.075f
    }
    // Gate/enemy source bounds were measured in the prior imported scene.
    fun halfDepth(wave:Wave)=scale(wave)*when(wave.kind) {
        Kind.GATE->.04025f
        Kind.ENEMY->.0388f
        Kind.CRATE->.050f
        Kind.OBSTACLE->.030f
    }
    fun shootable(kind:Kind)=kind==Kind.GATE || kind==Kind.ENEMY || kind==Kind.CRATE
}
