package net.buycraft.plugin.platform.standalone.runner;

import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.IBuycraftPlatform;
import net.buycraft.plugin.data.QueuedPlayer;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.execution.DuePlayerFetcher;
import net.buycraft.plugin.execution.strategy.ToRunQueuedCommand;
import net.buycraft.plugin.platform.NoBlocking;
import net.buycraft.plugin.platform.standalone.StandaloneBuycraftPlatform;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StandaloneBuycraftRunner {
    private final CommandDispatcher dispatcher;
    private final PlayerDeterminer determiner;
    private final String apiKey;
    private final Logger logger;
    private final ScheduledExecutorService executorService;
    private final IBuycraftPlatform platform;
    private final boolean verbose;
    private ServerInformation serverInformation;
    private DuePlayerFetcher playerFetcher;

    StandaloneBuycraftRunner(CommandDispatcher dispatcher, PlayerDeterminer determiner, String apiKey, Logger logger, ScheduledExecutorService executorService, boolean verbose) {
        this.dispatcher = dispatcher;
        this.determiner = determiner;
        this.apiKey = apiKey;
        this.logger = logger;
        this.executorService = executorService;
        this.platform = new Platform();
        this.verbose = verbose;
    }

    public void initializeTasks() {
        try {
            serverInformation = platform.getApiClient().getServerInformation().execute().body();
        } catch (IOException e) {
            throw new RuntimeException("Can't fetch account information", e);
        }
        executorService.schedule(playerFetcher = new DuePlayerFetcher(platform, verbose), 1, TimeUnit.SECONDS);
    }

    public ServerInformation getServerInformation() {
        return this.serverInformation;
    }

    public DuePlayerFetcher getPlayerFetcher() {
        return this.playerFetcher;
    }

    // SpaceDelta
    public IBuycraftPlatform getPlatform() {
        return platform;
    }

    @NoBlocking
    private class Platform extends StandaloneBuycraftPlatform {
        Platform() {
            super(BuyCraftAPI.create(apiKey, new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build()), executorService);
        }

        @Override
        public void dispatchCommand(String command) {
            dispatcher.dispatchCommand(command);
        }

        @Override
        public void dispatchCommand(@NotNull ToRunQueuedCommand command, @NotNull String formattedCommand) {
            dispatcher.dispatchCommand(command, formattedCommand);
        }

        @Override
        public void dispatchCommands(@NotNull Map<ToRunQueuedCommand, String> commands) {
            dispatcher.dispatchCommands(commands);
        }

        @Override
        public boolean isPlayerOnline(QueuedPlayer player) {
            return determiner.isPlayerOnline(player);
        }

        @Override
        public int getFreeSlots(QueuedPlayer player) {
            return determiner.getFreeSlots(player);
        }

        @Override
        public void log(Level level, String message) {
            // logger.log(level, message);
            // SpaceDelta // switch to SLF4J
            if (level.equals(Level.WARNING))
                logger.warn(message);
            else if (level.equals(Level.SEVERE))
                logger.error(message);
            else
                logger.info(message);
        }

        @Override
        public void log(Level level, String message, Throwable throwable) {
            // logger.log(level, message, throwable);
            logger.error(message, throwable); // SpaceDelta
        }

        @Override
        public ServerInformation getServerInformation() {
            return serverInformation;
        }
    }
}
