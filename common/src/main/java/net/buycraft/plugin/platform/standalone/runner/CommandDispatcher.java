package net.buycraft.plugin.platform.standalone.runner;

import net.buycraft.plugin.data.QueuedPlayer;
import net.buycraft.plugin.execution.strategy.ToRunQueuedCommand;

/**
 * {@code CommandDispatcher}s are called when Buycraft processes a command. The dispatcher will not get any other
 * information from the command.
 */
public interface CommandDispatcher {
    void dispatchCommand(String command);

    // SpaceDelta
    void dispatchCommand(ToRunQueuedCommand command, String formattedCommand);
}
