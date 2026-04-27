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
