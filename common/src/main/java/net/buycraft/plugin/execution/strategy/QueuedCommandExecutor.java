package net.buycraft.plugin.execution.strategy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.buycraft.plugin.IBuycraftPlatform;
import net.buycraft.plugin.platform.NoBlocking;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class QueuedCommandExecutor implements CommandExecutor, Runnable {
    private static final long MAXIMUM_NOTIFICATION_TIME = TimeUnit.MILLISECONDS.toNanos(5);
    private final IBuycraftPlatform platform;
    private final boolean blocking;
    // private final Set<ToRunQueuedCommand> commandQueue = new LinkedHashSet<>(); // SpaceDelta :: change to uuid index
    private final Multimap<String, ToRunQueuedCommand> commandQueue = ArrayListMultimap.create();

    private final PostCompletedCommandsTask completedCommandsTask;
    private int runMaxCommandsBlocking = 10;

    public QueuedCommandExecutor(IBuycraftPlatform platform, PostCompletedCommandsTask completedCommandsTask) {
        this.platform = platform;
        this.blocking = !platform.getClass().isAnnotationPresent(NoBlocking.class);
        this.completedCommandsTask = completedCommandsTask;
    }

    @Override
    public void queue(ToRunQueuedCommand command) {
        synchronized (commandQueue) {
            commandQueue.put(command.getPlayer().getUuid(), command);
        }
    }

    @Override
    public void run() {
        // List<ToRunQueuedCommand> runThisTick = new ArrayList<>();
        Multimap<String, ToRunQueuedCommand> runThisTick = ArrayListMultimap.create();
        synchronized (commandQueue) {
            ArrayList<Integer> queuedCommandIds = new ArrayList<>();
            Set<ToRunQueuedCommand> removeSet = new HashSet<ToRunQueuedCommand>();

            for (ToRunQueuedCommand command : commandQueue.values()) {
                if (queuedCommandIds.contains(command.getCommand().getId())) {
                    removeSet.add(command);
                    continue;
                }
                queuedCommandIds.add(command.getCommand().getId());

                if (command.canExecute(platform)) {
                    runThisTick.put(command.getPlayer().getUuid(), command);
                    // runThisTick.add(command); // SpaceDelta :: change to uuid index
                    //it.remove();
                    removeSet.add(command);
                }

                if (blocking && runThisTick.size() >= runMaxCommandsBlocking) {
                    break;
                }
            }

            // commandQueue.removeAll(removeSet); // SpaceDelta :: change to uuid index
            removeSet.forEach(command -> commandQueue.remove(command.getPlayer().getUuid(), command));
        }

        long start = System.nanoTime();
        for (Map.Entry<String, Collection<ToRunQueuedCommand>> entry : runThisTick.asMap().entrySet()) {
            final String playerId = entry.getKey();

            platform.log(Level.INFO, String.format("Dispatching %d commands for player '%s'.", entry.getValue().size(), playerId));

            Map<ToRunQueuedCommand, String> queuedCommands = Maps.newHashMap();
            entry.getValue().forEach(command -> {
                if (completedCommandsTask.getRetained().contains(command.getCommand().getId())) {
                    synchronized (commandQueue) {
                        commandQueue.remove(command.getPlayer().getUuid(), command);
                    }

                    return;
                }

                if (command.canExecute(platform)) {

                    String finalCommand = platform.getPlaceholderManager().doReplace(command.getPlayer(), command.getCommand());
                    platform.log(Level.INFO, String.format("Preparing command '%s' for player '%s'.", finalCommand, playerId));
                    try {
                        queuedCommands.put(command, finalCommand);
                        completedCommandsTask.add(command.getCommand().getId());
                    } catch (Exception e) {
                        platform.log(Level.SEVERE, String.format("Could not dispatch command '%s' for player '%s'. " +
                                "This is typically a plugin error, not an issue with BuycraftX.", finalCommand, command.getPlayer().getName()), e);
                    }

                }
            });

            platform.log(Level.INFO, "Dispatching " + queuedCommands.values() + " for " + playerId);
            platform.dispatchCommands(queuedCommands);
        }

        long fullTime = System.nanoTime() - start;
        if (fullTime > MAXIMUM_NOTIFICATION_TIME) {
            // Make the time much nicer.
            BigDecimal timeMs = new BigDecimal(fullTime).divide(new BigDecimal("1000000"), 2, BigDecimal.ROUND_CEILING);
            if (blocking) {
                platform.log(Level.INFO, "Command execution took " + timeMs.toPlainString() + "ms to complete. " +
                        "This may indicate an issue with one of your server's plugins, which can cause lag.");
            } else {
                platform.log(Level.INFO, "Command execution took " + timeMs.toPlainString() + "ms to complete. " +
                        "This may indicate an issue with one of your server's plugins, which will slow command execution.");
            }
        }
    }

    public void setRunMaxCommandsBlocking(final int runMaxCommandsBlocking) {
        this.runMaxCommandsBlocking = runMaxCommandsBlocking;
    }
}
