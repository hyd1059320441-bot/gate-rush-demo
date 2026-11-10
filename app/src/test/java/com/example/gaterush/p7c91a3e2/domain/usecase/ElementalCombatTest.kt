package com.example.gaterush.p7c91a3e2.domain.usecase

import com.example.gaterush.p7c91a3e2.data.repository.*
import com.example.gaterush.p7c91a3e2.domain.model.*
import com.example.gaterush.p7c91a3e2.domain.model.Target
import org.junit.Assert.*
import org.junit.Test

class ElementalCombatTest {
    private fun engine() = PlayGameUseCase(object:LevelRepository {
        override fun levels()=listOf(Level("test","test",emptyList()))
    }).apply { assetsReady(); start(); advance(PlayGameUseCase.STEP); bullets.clear() }
    private fun enemy(id:Long=9001,x:Float=0f,hp:Float=1000f,armoured:Boolean=false) =
        Target(id,Wave(0f,Kind.ENEMY,x,hp,armoured=armoured),.30f)
    private fun run(game:PlayGameUseCase,seconds:Float) {
        repeat((seconds*60).toInt()) { game.advance(PlayGameUseCase.STEP) }
    }
    private fun strike(game:PlayGameUseCase,bullet:Bullet) {
        game.bullets.clear()
        game.bullets.add(bullet.apply { depth=.23f })
        game.advance(PlayGameUseCase.STEP)
    }
    private fun moveAway(game:PlayGameUseCase) {
        game.beginDrag(); game.drag(-.32f); game.cancelDrag()
    }
    @Test fun iceSlowsThenExpiresAndRestoresNormalMovement() {
        val game=engine(); val target=enemy(); game.targets+=target
        strike(game,Bullet(8001,0f,0,0,ice=1)); moveAway(game)
        val depth=target.depth
        run(game,.5f)
        assertEquals(.045f,depth-target.depth,.0001f)
        run(game,1f); assertTrue(game.elapsed>target.iceUntil)
        val after=target.depth
        run(game,.3f); assertEquals(.045f,after-target.depth,.0001f)
    }
    @Test fun weakerIceRefreshCannotWeakenAnActiveStrongSlow() {
        val game=engine(); val target=enemy(); game.targets+=target
        strike(game,Bullet(8001,0f,0,0,ice=2))
        val until=target.iceUntil
        strike(game,Bullet(8002,0f,0,0,ice=1))
        assertEquals(.4f,target.slowFactor,0f)
        assertTrue(target.iceUntil>=until)
    }
    @Test fun burnHasFiveTicksThenStopsWithoutFurtherShots() {
        val game=engine(); val target=enemy(hp=100f); game.targets+=target
        strike(game,Bullet(8001,0f,0,0,burn=1))
        target.depth=1.3f; moveAway(game); game.drainFeedback()
        run(game,2.1f)
        assertEquals(82f,target.hp,0f)
        assertEquals(5,game.drainFeedback().count { it.kind=="burn" })
        run(game,1f); assertEquals(82f,target.hp,0f)
    }
    @Test fun burnRefreshKeepsDueTickAndStrongDamage() {
        val game=engine(); val target=enemy(); game.targets+=target
        strike(game,Bullet(8001,0f,0,0,burn=2)); moveAway(game)
        run(game,.2f)
        val due=target.nextBurnAt
        target.depth=.30f
        strike(game,Bullet(8002,0f,0,0,burn=1))
        assertEquals(due,target.nextBurnAt,0f); assertEquals(3f,target.burnDamage,0f)
        val before=target.hp
        run(game,.2f); assertEquals(before-3f,target.hp,0f)
    }
    @Test fun pauseFreezesBurnAndSlowAndRestartClearsThem() {
        val game=engine(); val target=enemy(); game.targets+=target
        strike(game,Bullet(8001,0f,0,0,ice=2,burn=2))
        val hp=target.hp; val depth=target.depth; val until=target.iceUntil
        game.pause(); repeat(100) { game.advance(.05f) }
        assertEquals(hp,target.hp,0f); assertEquals(depth,target.depth,0f)
        assertEquals(until,target.iceUntil,0f)
        game.start(); assertTrue(game.targets.isEmpty()); assertEquals(Stacks(),game.stacks)
    }
    @Test fun lethalBurnClearsStatusAndEmitsOnlyOneDeath() {
        val game=engine(); val target=enemy(hp=10f); game.targets+=target
        strike(game,Bullet(8001,0f,0,0,burn=1,ice=2))
        target.depth=1.3f; moveAway(game); game.drainFeedback()
        run(game,3f)
        assertFalse(target.alive); assertEquals(0f,target.hp,0f)
        assertEquals(0f,target.burnDamage,0f); assertEquals(1f,target.slowFactor,0f)
        assertTrue(game.targets.isEmpty())
        assertEquals(1,game.drainFeedback().count { it.kind=="enemy" })
    }
    @Test fun chainUsesNearestTwoDistinctEnemiesWithoutLoopingBackOrHittingCrates() {
        val game=engine(); val origin=enemy()
        val nearest=enemy(9002,.18f); val second=enemy(9003,.30f); val extra=enemy(9004,.50f)
        val crate=Target(9005,Wave(0f,Kind.CRATE,-.20f,8f,Upgrade.SHIELD),.30f)
        game.targets.addAll(listOf(origin,extra,second,crate,nearest))
        strike(game,Bullet(8001,0f,0,0,chain=2))
        assertEquals(992f,origin.hp,0f); assertEquals(992f,nearest.hp,0f)
        assertEquals(992f,second.hp,0f); assertEquals(1000f,extra.hp,0f)
        assertEquals(8f,crate.hp,0f); assertEquals(0,game.stacks.shield)
        val links=game.drainFeedback().filter { it.kind=="chain" }
        assertEquals(2,links.size)
        assertEquals(.18f,links[0].endX!!,0f); assertEquals(.30f,links[1].endX!!,0f)
    }
    @Test fun chainStopsOutsideRadiusAndRespectsArmourAndPierce() {
        for(pierce in 0..1) {
            val game=engine(); val origin=enemy()
            val armour=enemy(9002,.2f,armoured=true); val distant=enemy(9003,.6f)
            game.targets.addAll(listOf(origin,armour,distant))
            strike(game,Bullet(8001,0f,pierce,0,chain=1))
            assertEquals(if(pierce==0) 997f else 994f,armour.hp,0f)
            assertEquals(1000f,distant.hp,0f)
            assertEquals(1,game.drainFeedback().count { it.kind=="chain" })
        }
    }
    @Test fun pierceDoesNotStartAnotherChainAtTheNextDirectHit() {
        val game=engine(); val origin=enemy()
        val ahead=enemy(9002).apply { depth=.56f }
        val side=enemy(9003,.20f)
        game.targets.addAll(listOf(origin,ahead,side))
        strike(game,Bullet(8001,0f,1,0,chain=1))
        run(game,.15f)
        assertEquals(992f,ahead.hp,0f)
        assertEquals(1,game.drainFeedback().count { it.kind=="chain" })
    }
    @Test fun directSplashAndArcsAllApplyCombinedEnemyStatuses() {
        val game=engine(); val origin=enemy()
        val splash=enemy(9002,.128f); val arcOnly=enemy(9003,.30f)
        game.targets.addAll(listOf(origin,splash,arcOnly))
        strike(game,Bullet(8001,0f,2,2,ice=2,chain=2,burn=2))
        for(target in listOf(origin,splash,arcOnly)) {
            assertTrue(target.hp<1000f)
            assertEquals(.4f,target.slowFactor,0f); assertEquals(3f,target.burnDamage,0f)
            assertTrue(target.iceUntil>game.elapsed); assertTrue(target.burnUntil>game.elapsed)
        }
    }
    @Test fun elementalEffectsDoNotPersistOnGatesOrCratesOrDuplicateRewards() {
        for(kind in listOf(Kind.GATE,Kind.CRATE)) {
            val game=engine(); val target=Target(9001,Wave(0f,kind,0f,8f,Upgrade.ICE),.30f)
            game.targets+=target
            strike(game,Bullet(8001,0f,2,2,ice=2,chain=2,burn=2))
            assertEquals(1,game.stacks.ice)
            assertEquals(0f,target.burnDamage,0f); assertEquals(1f,target.slowFactor,0f)
            moveAway(game); run(game,3f)
            assertEquals(1,game.drainFeedback().count { it.kind=="reward" })
        }
    }
    @Test fun widerExplosiveCoreHitsWhereNormalCoreMisses() {
        for(explosive in 0..1) {
            val game=engine(); val target=enemy(x=.125f); game.targets+=target
            strike(game,Bullet(8001,0f,0,explosive))
            assertEquals(if(explosive==0) 1000f else 992f,target.hp,0f)
        }
    }
    @Test fun everyNewUpgradeHasIndependentCapAndImmutableBulletSnapshot() {
        var stacks=Stacks()
        repeat(8) { for(type in Upgrade.entries) stacks=stacks.add(type) }
        assertEquals(Stacks(2,2,2,3,3,2,2,2),stacks)
        val bullet=Bullet(8001,0f,stacks.pierce,stacks.explosive,stacks.ice,stacks.chain,stacks.burn)
        stacks=Stacks()
        assertEquals(ShotType.BURN,bullet.type)
        assertEquals(2,bullet.ice); assertEquals(2,bullet.chain); assertEquals(2,bullet.burn)
        assertEquals(0,stacks.burn)
    }
    @Test fun fullCampaignHasElementsAndEnoughViewsEvenWithContinuousMaximumSlow() {
        LocalLevelRepository().levels().forEachIndexed { index,level ->
            for(type in listOf(Upgrade.ICE,Upgrade.CHAIN,Upgrade.BURN))
                assertTrue(level.events.any { it.reward==type })
            repeat(601) { tenth ->
                val time=tenth/10f
                val enemies=level.events.count { it.kind==Kind.ENEMY && it.at<=time &&
                    (it.terminal || time<it.at+1.7f/(.15f*it.speed*.4f)) }
                val crates=level.events.count { it.kind==Kind.CRATE && it.at<=time &&
                    time<it.at+1.7f/(.15f*it.speed) }
                assertTrue("level ${index+1} time $time enemies $enemies",enemies<=CombatGeometry.ENEMY_SLOTS)
                assertTrue("level ${index+1} time $time crates $crates",crates<=4)
            }
        }
    }
}
