package com.avairebot.shard;

import com.avairebot.AvaIre;
import com.avairebot.plugin.PluginLoader;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.annotation.Nonnull;

public class AvaireShard {

    private final AvaIre avaire;
    private final int shardId;

    @Nonnull
    protected volatile JDA jda;

    public AvaireShard(@Nonnull AvaIre avaire, int shardId) {
        this.avaire = avaire;
        this.shardId = shardId;
        AvaIre.getLogger().info("Building shard " + shardId);

        JDABuilder builder = ShardBuilder.getDefaultShardBuilder(avaire);
        for (PluginLoader plugin : avaire.getPluginManager().getPlugins()) {
            plugin.registerEventListeners(builder);
        }

        jda = buildJDA(builder);
    }

    @Nonnull
    public JDA getJDA() {
        return jda;
    }

    public int getShardId() {
        return shardId;
    }

    private JDA buildJDA(final JDABuilder builder) {
        JDA newJda = null;
        int total = avaire.getSettings().getShardCount();

        try {
            boolean success = false;
            while (!success) {
                // noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (builder) {
                    builder.useSharding(shardId, total < 1 ? 1 : total);

                    try {
                        avaire.getConnectQueue().requestCoin(shardId);

                        newJda = builder.buildAsync();
                        success = true;
                    } catch (RateLimitedException e) {
                        AvaIre.getLogger().error("Got rate limited while building bot JDA instance! Retrying...", e);
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        AvaIre.getLogger().error("Generic exception when building a JDA instance! Retrying...", e);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start JDA shard " + shardId, e);
        }

        return newJda;
    }
}