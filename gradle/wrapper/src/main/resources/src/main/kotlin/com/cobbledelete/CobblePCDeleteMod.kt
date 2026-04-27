package com.cobbledelete

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.pc.PCBox
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object CobblePCDeleteMod : ModInitializer {

    private val logger = LoggerFactory.getLogger("cobble-pc-delete")

    override fun onInitialize() {
        logger.info("Cobble PC Delete initialising...")
        registerCommands()
        logger.info("Cobble PC Delete ready.")
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("pcdelete")
                    .then(
                        literal("box")
                            .then(
                                argument("boxNumber", IntegerArgumentType.integer(1, 40))
                                    .executes { ctx -> deleteBox(ctx) }
                            )
                    )
                    .then(
                        literal("species")
                            .then(
                                argument("speciesName", StringArgumentType.word())
                                    .executes { ctx -> deleteSpecies(ctx) }
                            )
                    )
                    .then(
                        literal("below")
                            .then(
                                argument("level", IntegerArgumentType.integer(1, 100))
                                    .executes { ctx -> deleteBelowLevel(ctx) }
                            )
                    )
                    .then(
                        literal("duplicates")
                            .executes { ctx -> deleteDuplicates(ctx) }
                    )
                    .then(
                        literal("all")
                            .executes { ctx -> deleteAll(ctx) }
                    )
            )
        }
    }

    private fun getPC(ctx: CommandContext<ServerCommandSource>): PCStore? {
        val player = ctx.source.player
        if (player == null) {
            ctx.source.sendError(Text.literal("Only players can use this command."))
            return null
        }
        return try {
            Cobblemon.storage.getPC(player.uuid)
        } catch (e: Exception) {
            ctx.source.sendError(Text.literal("Could not access your PC: ${e.message}"))
            null
        }
    }

    private fun collectPokemon(pc: PCStore, predicate: (Pokemon) -> Boolean): List<Triple<PCBox, Int, Pokemon>> {
        val results = mutableListOf<Triple<PCBox, Int, Pokemon>>()
        for (box in pc) {
            for (slot in 0 until box.size) {
                val pokemon = box[slot] ?: continue
                if (predicate(pokemon)) results.add(Triple(box, slot, pokemon))
            }
        }
        return results
    }

    private fun removePokemon(targets: List<Triple<PCBox, Int, Pokemon>>): Int {
        var count = 0
        for ((box, slot, _) in targets) {
            box[slot] = null
            count++
        }
        return count
    }

    private fun deleteBox(ctx: CommandContext<ServerCommandSource>): Int {
        val pc = getPC(ctx) ?: return 0
        val boxIndex = IntegerArgumentType.getInteger(ctx, "boxNumber") - 1
        if (boxIndex >= pc.boxes.size) {
            ctx.source.sendError(Text.literal("Box ${boxIndex + 1} does not exist."))
            return 0
        }
        val box = pc.boxes[boxIndex]
        var count = 0
        for (slot in 0 until box.size) {
            if (box[slot] != null) { box[slot] = null; count++ }
        }
        ctx.source.sendFeedback({ Text.literal("§aDeleted §e$count§a Pokemon from Box ${boxIndex + 1}.") }, false)
        return count
    }

    private fun deleteSpecies(ctx: CommandContext<ServerCommandSource>): Int {
        val pc = getPC(ctx) ?: return 0
        val speciesName = StringArgumentType.getString(ctx, "speciesName").lowercase()
        val targets = collectPokemon(pc) { it.species.name.lowercase() == speciesName }
        if (targets.isEmpty()) {
            ctx.source.sendFeedback({ Text.literal("§eNo ${speciesName.replaceFirstChar { it.uppercase() }} found in your PC.") }, false)
            return 0
        }
        val count = removePokemon(targets)
        ctx.source.sendFeedback({ Text.literal("§aDeleted §e$count§a ${speciesName.replaceFirstChar { it.uppercase() }}(s).") }, false)
        return count
    }

    private fun deleteBelowLevel(ctx: CommandContext<ServerCommandSource>): Int {
        val pc = getPC(ctx) ?: return 0
        val threshold = IntegerArgumentType.getInteger(ctx, "level")
        val targets = collectPokemon(pc) { it.level < threshold }
        if (targets.isEmpty()) {
            ctx.source.sendFeedback({ Text.literal("§eNo Pokemon below level $threshold found.") }, false)
            return 0
        }
        val count = removePokemon(targets)
        ctx.source.sendFeedback({ Text.literal("§aDeleted §e$count§a Pokemon below level $threshold.") }, false)
        return count
    }

    private fun deleteDuplicates(ctx: CommandContext<ServerCommandSource>): Int {
        val pc = getPC(ctx) ?: return 0
        val all = collectPokemon(pc) { true }
        val bestBySpecies = mutableMapOf<String, Triple<PCBox, Int, Pokemon>>()
        for (entry in all) {
            val key = entry.third.species.name.lowercase()
            val existing = bestBySpecies[key]
            if (existing == null || entry.third.level > existing.third.level) bestBySpecies[key] = entry
        }
        val keepers = bestBySpecies.values.map { it.third.uuid }.toSet()
        val targets = all.filter { it.third.uuid !in keepers }
        if (targets.isEmpty()) {
            ctx.source.sendFeedback({ Text.literal("§eNo duplicates found.") }, false)
            return 0
        }
        val count = removePokemon(targets)
        ctx.source.sendFeedback({ Text.literal("§aDeleted §e$count§a duplicates. Kept the highest-level of each species.") }, false)
        return count
    }

    private fun deleteAll(ctx: CommandContext<ServerCommandSource>): Int {
        val pc = getPC(ctx) ?: return 0
        var count = 0
        for (box in pc) {
            for (slot in 0 until box.size) {
                if (box[slot] != null) { box[slot] = null; count++ }
            }
        }
        ctx.source.sendFeedback({ Text.literal("§cDeleted §e$count§c Pokemon from your entire PC.") }, false)
        return count
    }
}
