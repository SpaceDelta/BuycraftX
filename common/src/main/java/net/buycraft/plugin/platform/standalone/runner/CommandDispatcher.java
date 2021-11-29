package net.buycraft.plugin.platform.standalone.runner;

import net.buycraft.plugin.execution.strategy.ToRunQueuedCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {@code CommandDispatcher}s are called when Buycraft processes a command. The dispatcher will not get any other
 * information from the command.
 */
public interface CommandDispatcher {
    void dispatchCommand(String command);

    // SpaceDelta
    void dispatchCommand(@NotNull ToRunQueuedCommand command, @NotNull String formattedCommand);

    // SpaceDelta
    void dispatchCommands(@NotNull Map<ToRunQueuedCommand, String> commands);
}
