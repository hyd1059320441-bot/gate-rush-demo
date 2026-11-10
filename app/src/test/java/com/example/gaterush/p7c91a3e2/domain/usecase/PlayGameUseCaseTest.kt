package com.example.gaterush.p7c91a3e2.domain.usecase

import com.example.gaterush.p7c91a3e2.data.repository.*
import com.example.gaterush.p7c91a3e2.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PlayGameUseCaseTest {
    private fun engine(waves:List<Wave>,duration:Float=60f):PlayGameUseCase {
        val repository=object:LevelRepository {
            override fun levels()=listOf(Level("test","test",waves,duration))
        }
        return PlayGameUseCase(repository).apply { assetsReady(); start() }
    }
    private fun run(game:PlayGameUseCase,seconds:Float) {
        repeat((seconds*60).toInt()) { game.advance(1f/60f) }
    }
    @Test fun gateRewardIsImmediateAndGrantedOnceForManyHits() {
        val game=engine(listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.SPREAD)))
        run(game,2f)
        assertEquals(1,game.stacks.spread)
        assertEquals(100,game.hp)
        run(game,12f)
        assertEquals(1,game.stacks.spread)
    }
    @Test fun dragCapturesOnlyDuringPlayAndKeepsPositionAfterRelease() {
        val game=engine(emptyList())
        game.drag(.2f); assertEquals(0f,game.targetX,0f)
        game.beginDrag(); game.drag(.8f); game.cancelDrag()
        run(game,1f); assertEquals(.32f,game.x,.001f)
        game.drag(-.5f); assertEquals(.32f,game.targetX,0f)
        game.beginDrag(); game.drag(Float.NaN); assertEquals(.32f,game.targetX,0f)
    }
    @Test fun pauseDiscardsWallTimeAndPreservesStacksAndPosition() {
        val game=engine(listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.PIERCE)))
        run(game,2f); game.beginDrag(); game.drag(.2f); run(game,.4f)
        game.pause(); val elapsed=game.elapsed; val x=game.x
        game.advance(200f); assertEquals(elapsed,game.elapsed,0f); assertEquals(x,game.x,0f)
        assertEquals(1,game.stacks.pierce); assertFalse(game.dragging)
        game.resume(); game.advance(200f); assertTrue(game.elapsed-elapsed<=.051f)
    }
    @Test fun restartClearsBulletsRewardsClockAndStacks() {
        val game=engine(listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.SPEED)))
        run(game,2f); assertEquals(1,game.stacks.speed)
        game.pause(); game.start()
        assertEquals(Stacks(),game.stacks); assertEquals(0f,game.elapsed,0f)
        assertTrue(game.bullets.isEmpty()); assertTrue(game.targets.isEmpty())
        run(game,2f); assertEquals(1,game.stacks.speed)
    }
    @Test fun speedAndShieldCratesCanBeShotBeforeTheyReachRunner() {
        for(reward in listOf(Upgrade.SPEED,Upgrade.SHIELD)) {
            val game=engine(listOf(Wave(0f,Kind.CRATE,0f,reward=reward)))
            run(game,2f)
            assertEquals(1,game.stacks.count(reward))
            assertTrue(game.targets.isEmpty())
            assertEquals(1,game.drainFeedback().count { it.kind=="reward" && it.reward==reward })
            run(game,10f)
            assertEquals(1,game.stacks.count(reward))
        }
    }
    @Test fun partiallyShotCrateStillCollectsWhenPassed() {
        val game=engine(listOf(Wave(0f,Kind.CRATE,0f,10000f,Upgrade.SHIELD)))
        run(game,2f)
        assertEquals(0,game.stacks.shield); assertTrue(game.targets.single().hp<10000f)
        run(game,10f)
        assertEquals(1,game.stacks.shield); assertTrue(game.targets.isEmpty())
    }
    @Test fun multipleBulletsAndRunnerContactCannotGrantSameCrateTwice() {
        val game=engine(listOf(Wave(0f,Kind.CRATE,0f,8f,Upgrade.SPEED)))
        game.advance(1f/60f)
        game.targets.single().depth=.04f
        game.bullets.single().depth=0f
        game.bullets.add(Bullet(9999L,0f,2,0).apply { depth=0f })
        game.advance(1f/60f)
        assertEquals(1,game.stacks.speed); assertEquals(100,game.hp)
        val feedback=game.drainFeedback()
        assertEquals(1,feedback.count { it.kind=="crate" })
        assertEquals(1,feedback.count { it.kind=="reward" && it.reward==Upgrade.SPEED })
        run(game,12f); assertEquals(1,game.stacks.speed)
    }
    @Test fun bulletTouchesEnlargedEnemyEdgeButMissesOutsideIt() {
        val touching=engine(listOf(Wave(0f,Kind.ENEMY,.120f,10000f)))
        val outside=engine(listOf(Wave(0f,Kind.ENEMY,.124f,10000f)))
        run(touching,2f); run(outside,2f)
        assertTrue(touching.targets.single().hp<10000f)
        assertEquals(10000f,outside.targets.single().hp,0f)
    }
    @Test fun impactUsesBulletLaneAndTargetFrontSurface() {
        val game=engine(listOf(Wave(0f,Kind.ENEMY,.05f,10000f)))
        game.advance(1f/60f)
        game.targets.single().depth=.30f
        game.bullets.single().depth=.20f
        game.advance(1f/60f)
        val target=game.targets.single()
        val impact=game.drainFeedback().single { it.kind=="impact" }
        assertEquals(0f,impact.x,0f)
        assertEquals(target.depth-target.halfDepth,impact.depth,.00001f)
    }
    @Test fun brokenGateCannotDamageRunnerInSameTick() {
        val game=engine(listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.SPREAD)))
        game.advance(1f/60f)
        game.targets[0].depth=.04f
        game.bullets[0].depth=0f
        game.advance(1f/60f)
        assertEquals(100,game.hp); assertEquals(1,game.stacks.spread)
    }
    @Test fun terminalDeadlineFailsWithoutTargetKill() {
        val game=engine(listOf(Wave(0f,Kind.ENEMY,.3f,10000f,terminal=true)),1f)
        run(game,1.1f); assertEquals(Phase.FAILED,game.phase)
    }
    @Test fun terminalKillIsRequiredAndWinsAtDeadline() {
        val game=engine(listOf(Wave(0f,Kind.ENEMY,0f,8f,terminal=true)),2f)
        run(game,1.5f); assertEquals(Phase.RUNNING,game.phase)
        run(game,.6f); assertEquals(Phase.COMPLETE,game.phase)
    }
    @Test fun stackCapsAreIndependent() {
        var value=Stacks()
        repeat(10) { value=value.add(Upgrade.SPREAD).add(Upgrade.PIERCE).add(Upgrade.EXPLOSIVE)
            .add(Upgrade.SPEED).add(Upgrade.SHIELD) }
        assertEquals(Stacks(2,2,2,3,3),value)
        assertEquals(value,value.add(Upgrade.HEAL))
    }
    @Test fun campaignContainsFiveSortedDistinctCoursesAndTelegraphedBoss() {
        val levels=LocalLevelRepository().levels()
        assertEquals(5,levels.size)
        levels.forEach { assertEquals(it.events.sortedBy { w->w.at },it.events)
            assertEquals(1,it.events.count { w->w.terminal }); assertEquals(60f,it.duration,0f) }
        assertTrue(levels.last().events.last().boss)
    }
    @Test fun denserCampaignHasFormationsAndCannotOverflowEnemyViewsEvenWithoutKills() {
        LocalLevelRepository().levels().forEachIndexed { index,level ->
            val enemies=level.events.filter { it.kind==Kind.ENEMY }
            assertTrue("level ${index+1} density",enemies.size>=24)
            assertTrue(enemies.groupBy { it.at }.values.any { it.size>=2 })
            repeat(601) { tenth ->
                val time=tenth/10f
                val alive=enemies.count { it.at<=time &&
                    (it.terminal || time<it.at+1.7f/(.15f*it.speed)) }
                assertTrue("level ${index+1} at $time needs $alive enemy views",
                    alive<=CombatGeometry.ENEMY_SLOTS)
            }
        }
    }
    @Test fun longFramesCannotAllocateUnboundedBulletsOrFastForward() {
        val game=engine(emptyList())
        repeat(600) { game.advance(500f) }
        assertTrue(game.elapsed<31f); assertTrue(game.bullets.size<=96)
        game.advance(-1f); game.advance(Float.NaN); assertTrue(game.elapsed.isFinite())
    }
    @Test fun pierceCanKillTwoAlignedTargetsWithOneVolley() {
        val game=engine(listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.PIERCE),
            Wave(3f,Kind.ENEMY,0f,8f),Wave(3f,Kind.ENEMY,0f,8f)))
        run(game,2f); assertEquals(1,game.stacks.pierce); game.drainFeedback()
        run(game,3f)
        assertTrue(game.targets.isEmpty())
        assertEquals(2,game.drainFeedback().count { it.kind=="enemy" })
    }
    @Test fun armourReducesDirectDamageByHalf() {
        val normal=engine(listOf(Wave(0f,Kind.ENEMY,0f,10000f)))
        val armour=engine(listOf(Wave(0f,Kind.ENEMY,0f,10000f,armoured=true)))
        run(normal,3f); run(armour,3f)
        val damage=10000f-normal.targets.single().hp
        assertTrue(damage>0f)
        assertEquals(damage*.5f,10000f-armour.targets.single().hp,0f)
    }
    @Test fun explosionHitsAdjacentEnemyOutsideDirectBulletLane() {
        val waves=listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.EXPLOSIVE),
            Wave(3f,Kind.ENEMY,0f,80f),Wave(3f,Kind.ENEMY,.128f,1000f))
        val game=engine(waves)
        run(game,5f)
        assertTrue(game.targets.first { it.wave.x==.128f }.hp<1000f)
    }
    @Test fun explosionCanBreakOffLaneCrateAndGrantRewardOnlyOnce() {
        val game=engine(listOf(Wave(0f,Kind.GATE,0f,8f,Upgrade.EXPLOSIVE),
            Wave(3f,Kind.ENEMY,0f,80f),Wave(3f,Kind.CRATE,.128f,4f,Upgrade.SHIELD)))
        run(game,5f)
        assertEquals(1,game.stacks.shield)
        assertEquals(1,game.drainFeedback().count { it.kind=="reward" && it.reward==Upgrade.SHIELD })
        run(game,10f); assertEquals(1,game.stacks.shield)
    }
    @Test fun bossWarnsBeforeDamageAndSnapshotsLane() {
        val game=engine(listOf(Wave(0f,Kind.ENEMY,0f,100000f,terminal=true,boss=true)))
        run(game,6.2f); assertEquals(1,game.warningLane); assertEquals(100,game.hp)
        game.beginDrag(); game.drag(-.32f); game.cancelDrag()
        run(game,1.2f); assertEquals(100,game.hp); assertEquals(-1,game.warningLane)
    }
    @Test fun healRestoresOnlyMissingHealth() {
        val game=engine(listOf(Wave(0f,Kind.OBSTACLE,0f),
            Wave(1f,Kind.CRATE,0f,10000f,reward=Upgrade.HEAL)))
        run(game,11f); assertEquals(80,game.hp)
        run(game,2f); assertEquals(100,game.hp)
    }
    @Test fun canonicalFiveLevelCampaignCanBeCompletedWithLaneChoices() {
        val game=PlayGameUseCase(LocalLevelRepository())
        game.assetsReady()
        repeat(5) { level ->
            game.start()
            repeat(3620) {
                if(game.phase==Phase.RUNNING) {
                    val danger=game.targets.firstOrNull { t -> t.wave.kind==Kind.OBSTACLE &&
                        t.depth<.28f && abs(t.wave.x-game.x)<.14f }
                    val objective=game.targets.filter { t -> t.depth>.08f &&
                        t.wave.kind!=Kind.OBSTACLE }.minByOrNull { t -> t.depth }
                    val aim=when {
                        game.warningLane>=0 -> if(game.warningLane==0) .23f else -.23f
                        danger!=null -> if(danger.wave.x<=0f) .23f else -.23f
                        objective!=null -> objective.wave.x
                        else -> 0f
                    }
                    game.beginDrag(); game.drag(aim-game.targetX); game.cancelDrag()
                    game.advance(1f/60f)
                }
            }
            assertEquals("level ${level+1}: ${game.message}",
                if(level==4) Phase.COMPLETE else Phase.WON,game.phase)
            if(level<4) game.next()
        }
    }
}
