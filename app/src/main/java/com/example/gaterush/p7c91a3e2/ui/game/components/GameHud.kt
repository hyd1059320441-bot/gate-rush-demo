package com.example.gaterush.p7c91a3e2.ui.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gaterush.p7c91a3e2.domain.model.*
import com.example.gaterush.p7c91a3e2.ui.game.*
import com.pico.spatial.ui.design.*

@Composable
fun GameHud(state: GameUiState, onEvent: (GameEvent) -> Unit) {
    val game=state.game
    var restartConfirm by remember { mutableStateOf(false) }
    var exitConfirm by remember { mutableStateOf(false) }
    Column(Modifier.width(720.dp).heightIn(min=224.dp).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("火力冲门 · ${game.title}",style=PicoTheme.typography.titleMedium,
                color=PicoTheme.colorScheme.labelPrimary)
            Text("生命 ${game.hp} / 100",style=PicoTheme.typography.titleSmall,
                color=PicoTheme.colorScheme.labelPrimary)
        }
        when(game.phase) {
            Phase.LOADING -> {
                Text("正在装配角色与赛道…",color=PicoTheme.colorScheme.labelSecondary)
                Text("模型完成后可开始挑战",style=PicoTheme.typography.bodySmall,
                    color=PicoTheme.colorScheme.labelSecondary)
            }
            Phase.READY -> {
                Text(game.goal,style=PicoTheme.typography.bodyMedium,
                    color=PicoTheme.colorScheme.labelPrimary)
                Text("选中蓝色小人并左右拖动；松手后保持位置。小人自动前进、自动开火。",
                    style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
                Actions {
                    Action("开始挑战",GameEvent.Start,onEvent)
                    Action("玩法",GameEvent.Rules,onEvent)
                    Action("退出",GameEvent.Exit,onEvent)
                }
            }
            Phase.RUNNING -> {
                LinearProgressIndicator(progress={game.seconds/60f},modifier=Modifier.fillMaxWidth())
                Text("${game.seconds} / 60 秒   散射 ${game.stacks.spread}   穿透 ${game.stacks.pierce}   " +
                    "爆炸 ${game.stacks.explosive}   攻速 ${game.stacks.speed}   护盾 ${game.stacks.shield}",
                    style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelPrimary)
                Text("冰冻 ${game.stacks.ice}   电弧 ${game.stacks.chain}   燃烧 ${game.stacks.burn}  ·  弹药效果可叠加",
                    style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(if(game.warningLane>=0) "红色路线即将冲击，拖动小人躲开！"
                        else game.message.ifBlank { if(game.bossHp>0) "终点耐久 ${game.bossHp}" else
                            if(game.dragging) "拖动中 · 松手保持位置" else "黄门与补给箱均可打碎获得道具" },
                        style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelPrimary)
                    Action("暂停",GameEvent.Pause,onEvent)
                }
            }
            Phase.PAUSED -> {
                Text("已暂停 · 位置、生命与道具保留",color=PicoTheme.colorScheme.labelPrimary)
                if(restartConfirm || exitConfirm) {
                    Text(if(restartConfirm) "重新开始本关？本关生命、道具与进度将重置。"
                        else "退出挑战？本关进度将丢失。",color=PicoTheme.colorScheme.labelSecondary)
                    Actions {
                        Button(onClick={
                            onEvent(if(restartConfirm) GameEvent.Restart else GameEvent.Exit)
                            restartConfirm=false; exitConfirm=false
                        },modifier=Modifier.heightIn(min=56.dp)) { Text("确认") }
                        Button(onClick={restartConfirm=false;exitConfirm=false},
                            modifier=Modifier.heightIn(min=56.dp)) { Text("保留进度") }
                    }
                } else Actions {
                    Action("继续",GameEvent.Resume,onEvent)
                    Button(onClick={restartConfirm=true},modifier=Modifier.heightIn(min=56.dp)) { Text("重开本关") }
                    Action("玩法",GameEvent.Rules,onEvent)
                    Button(onClick={exitConfirm=true},modifier=Modifier.heightIn(min=56.dp)) { Text("退出") }
                }
            }
            Phase.RULES -> {
                Text("数字是剩余耐久。黄门打碎获奖励；绿箱可打碎获得攻速、护盾等道具，也可经过拾取。",
                    style=PicoTheme.typography.bodyMedium,color=PicoTheme.colorScheme.labelPrimary)
                Text("散射增弹道，穿透可连打，爆炸伤周围，攻速加快，护盾挡碰撞，回血 +25。" +
                    "躲开路障；60 秒内击破终点目标。",style=PicoTheme.typography.bodySmall,
                    color=PicoTheme.colorScheme.labelSecondary)
                Text("冰冻弹让敌人减速，电弧弹跳击附近 1–2 个敌人，燃烧弹持续伤害 2 秒。" +
                    "三种弹药各可叠两层，也能和散射、穿透、爆炸组合。",
                    style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
                Action("返回",GameEvent.Back,onEvent)
            }
            Phase.FAILED -> {
                Text("挑战结束 · ${game.message}",color=PicoTheme.colorScheme.labelPrimary)
                Actions { Action("再试本关",GameEvent.Restart,onEvent); Action("退出",GameEvent.Exit,onEvent) }
            }
            Phase.WON -> {
                Text("本关完成！",color=PicoTheme.colorScheme.labelPrimary)
                Actions { Action("下一关",GameEvent.Next,onEvent); Action("退出",GameEvent.Exit,onEvent) }
            }
            Phase.COMPLETE -> {
                Text("五关全部完成 · 核心击破！",color=PicoTheme.colorScheme.labelPrimary)
                Actions { Action("再玩一轮",GameEvent.Replay,onEvent); Action("退出",GameEvent.Exit,onEvent) }
            }
            Phase.LOAD_ERROR -> {
                Text("模型加载失败：${game.message}",color=PicoTheme.colorScheme.labelPrimary)
                Actions { Action("重试加载",GameEvent.RetryAssets,onEvent); Action("退出",GameEvent.Exit,onEvent) }
            }
        }
    }
}
@Composable private fun Actions(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
@Composable private fun Action(label: String,event: GameEvent,onEvent: (GameEvent)->Unit) {
    Button(onClick={onEvent(event)},modifier=Modifier.heightIn(min=56.dp)) { Text(label) }
}
