package com.example.gaterush.p7c91a3e2.data.repository

import com.example.gaterush.p7c91a3e2.domain.model.*

interface LevelRepository { fun levels(): List<Level> }

/** Original course objectives plus short, low-health formations for continuous combat. */
class LocalLevelRepository : LevelRepository {
    override fun levels() = listOf(
        Level("01 / 首次破门", "击破第一扇门，获得散射。", listOf(
            g(0,0f,24,Upgrade.SPREAD), c(5,-.23f,Upgrade.SHIELD), e(9,0f,24),
            g(14,.23f,40,Upgrade.SPEED), c(20,0f,Upgrade.HEAL), e(24,-.23f,40),
            g(29,0f,48,Upgrade.PIERCE), e(34,.23f,48), c(38,0f,Upgrade.SHIELD),
            e(43,0f,56), e(48,0f,64,true))),
        Level("02 / 火力岔路", "选择路线，组合散射与穿透。", listOf(
            g(0,-.23f,32,Upgrade.SPREAD), g(0,.23f,32,Upgrade.PIERCE),
            c(5,0f,Upgrade.SPEED), e(10,-.23f,56), e(10,.23f,56),
            g(15,0f,64,Upgrade.EXPLOSIVE), c(21,-.23f,Upgrade.HEAL), e(25,0f,88),
            g(30,.23f,80,Upgrade.SPREAD), c(36,0f,Upgrade.SHIELD),
            e(40,-.23f,96), e(48,0f,112,true))),
        Level("03 / 机动避障", "躲开路障与快敌，保持火力。", listOf(
            g(0,0f,40,Upgrade.SPEED), c(4,-.23f,Upgrade.SHIELD), o(8,0f),
            e(12,.23f,64,speed=1.3f), g(16,-.23f,64,Upgrade.SPREAD), o(22,.23f),
            c(24,0f,Upgrade.HEAL), e(28,-.23f,80,speed=1.3f),
            g(32,0f,88,Upgrade.EXPLOSIVE), o(38,-.23f), c(41,0f,Upgrade.PIERCE),
            e(48,0f,144,true))),
        Level("04 / 装甲突破", "穿透破甲，爆炸弹清理敌群。", listOf(
            g(0,0f,48,Upgrade.PIERCE), g(4,.23f,56,Upgrade.SPEED),
            c(8,0f,Upgrade.EXPLOSIVE), e(12,-.23f,88,armoured=true),
            e(12,0f,88,armoured=true), g(18,0f,88,Upgrade.SPREAD),
            c(22,-.23f,Upgrade.HEAL), e(26,.23f,112,armoured=true),
            g(30,-.23f,96,Upgrade.PIERCE), c(35,0f,Upgrade.SHIELD), o(40,0f),
            e(48,0f,192,true,armoured=true))),
        Level("05 / 核心决战", "叠加火力，躲开预警，击破核心。", listOf(
            g(0,0f,40,Upgrade.SPREAD), c(4,0f,Upgrade.SPEED),
            g(8,-.23f,48,Upgrade.PIERCE), c(12,0f,Upgrade.EXPLOSIVE),
            e(16,.23f,96), g(20,0f,72,Upgrade.SPEED), c(24,-.23f,Upgrade.SHIELD),
            c(28,0f,Upgrade.HEAL), g(32,.23f,96,Upgrade.EXPLOSIVE),
            e(36,0f,104,armoured=true), e(44,0f,1280,true,boss=true)))
    ).mapIndexed { levelIndex,level ->
        val formations=buildList {
            val last=if(levelIndex==4) 39 else 43
            for((formation,t) in (3..last step 4).withIndex()) {
                val lanes=if(levelIndex>=3) listOf(-.23f,0f,.23f)
                    else listOf(0f,if(formation%2==0) .23f else -.23f)
                lanes.forEach { lane ->
                    if(level.events.none { kotlin.math.abs(it.at-t)<.75f &&
                            kotlin.math.abs(it.x-lane)<.01f }) {
                        val hp=32+levelIndex*8
                        val speed=if(levelIndex<2) 1f else 1.05f+(formation%3)*.05f
                        add(e(t,lane,hp,speed=speed))
                    }
                }
            }
        }
        val elements=when(levelIndex) {
            0 -> listOf(c(8,0f,Upgrade.ICE),c(18,0f,Upgrade.CHAIN),c(25,0f,Upgrade.BURN))
            1 -> listOf(c(6,0f,Upgrade.ICE),c(17,.23f,Upgrade.CHAIN),c(31,0f,Upgrade.BURN))
            2 -> listOf(c(6,0f,Upgrade.ICE),c(18,0f,Upgrade.CHAIN),c(34,.23f,Upgrade.BURN))
            3 -> listOf(c(6,0f,Upgrade.ICE),c(16,.23f,Upgrade.CHAIN),c(29,0f,Upgrade.BURN))
            else -> listOf(c(6,0f,Upgrade.ICE),c(18,0f,Upgrade.CHAIN),c(30,0f,Upgrade.BURN),
                c(34,0f,Upgrade.ICE),c(38,0f,Upgrade.CHAIN),c(40,0f,Upgrade.BURN))
        }
        level.copy(events=(level.events+formations+elements).sortedBy { it.at })
    }
    private fun g(t: Int,x: Float,hp: Int,r: Upgrade) = Wave(t.toFloat(),Kind.GATE,x,hp.toFloat(),r)
    private fun c(t: Int,x: Float,r: Upgrade) =
        Wave(t.toFloat(),Kind.CRATE,x,CombatGeometry.CRATE_HP,reward=r)
    private fun e(t: Int,x: Float,hp: Int,terminal: Boolean=false,armoured: Boolean=false,
        speed: Float=1f,boss: Boolean=false) = Wave(t.toFloat(),Kind.ENEMY,x,hp.toFloat(),
        terminal=terminal,armoured=armoured,speed=speed,boss=boss)
    private fun o(t: Int,x: Float) = Wave(t.toFloat(),Kind.OBSTACLE,x)
}
