package com.example.gaterush.p7c91a3e2.scene

import com.pico.spatial.core.ecs.Entity
import kotlin.math.ceil

/** Readable domain digits, shared unit geometry; no per-frame mesh/texture allocation. */
internal class DurabilityDigits(kit:SceneResources,parent:Entity,y:Float,z:Float) {
    private val root=group(parent,"DurabilityDigits")
    private val segments=Array(4) { digit ->
        Array(7) { segment ->
            val horizontal=segment in setOf(0,3,6)
            val offsetX=when(segment) { 1,2->.009f; 4,5->-.009f; else->0f }
            val offsetY=when(segment) { 0->.034f; 1,5->.0255f; 2,4->.0085f; 3->0f; else->.017f }
            kit.physical(root,"Digit${digit}_$segment",(digit-1.5f)*.025f+offsetX,
                y+offsetY,z,if(horizontal) .018f else .004f,
                if(horizontal) .004f else .014f,.004f,"ink")
        }
    }
    private var previous=-1
    fun set(hp:Float) {
        val value=ceil(hp).toInt().coerceIn(0,9999)
        if(value==previous) return
        previous=value
        val text=value.toString().padStart(4,' ')
        segments.forEachIndexed { i,parts ->
            val char=text[i]
            val mask=if(char==' ') 0 else MASKS[char-'0']
            parts.forEachIndexed { s,e -> e.enabled=(mask and (1 shl s))!=0 }
        }
    }
    companion object { private val MASKS=intArrayOf(63,6,91,79,102,109,125,7,127,111) }
}
