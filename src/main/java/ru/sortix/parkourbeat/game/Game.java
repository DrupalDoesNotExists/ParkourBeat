package ru.sortix.parkourbeat.game;

import lombok.Getter;
import lombok.NonNull;
import me.bomb.amusic.AMusic;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.sortix.parkourbeat.game.movement.GameMoveHandler;
import ru.sortix.parkourbeat.levels.Level;
import ru.sortix.parkourbeat.levels.LevelsManager;
import ru.sortix.parkourbeat.levels.ParticleController;
import ru.sortix.parkourbeat.levels.settings.GameSettings;
import ru.sortix.parkourbeat.levels.settings.LevelSettings;
import ru.sortix.parkourbeat.levels.settings.WorldSettings;

public class Game {

    private final LevelsManager levelsManager;
    private LevelSettings levelSettings;
    @Getter private Player player;
    @Getter private State currentState;
    private GameMoveHandler gameMoveHandler;

    public Game(LevelsManager levelsManager) {
        currentState = State.PREPARING;
        player = null;
        levelSettings = null;
        gameMoveHandler = null;
        this.levelsManager = levelsManager;
    }

    public void prepare(Player player, String levelName) {
        currentState = State.PREPARING;
        this.player = player;

        Level level = levelsManager.loadLevel(levelName);
        levelSettings = level.getLevelSettings();
        WorldSettings worldSettings = levelSettings.getWorldSettings();
        GameSettings gameSettings = levelSettings.getGameSettings();
        ParticleController particleController = levelSettings.getParticleController();

        if (!particleController.isLoaded()) {
            particleController.loadParticleLocations(worldSettings.getWaypoints());
        }

        player.teleport(worldSettings.getSpawn());
        this.gameMoveHandler = new GameMoveHandler(this);

        if (gameSettings.getSongName() != null
                && !gameSettings.getSongPlayListName().equals(AMusic.getPackName(player))) {
            this.getPlugin()
                    .getServer()
                    .getScheduler()
                    .scheduleSyncDelayedTask(
                            this.getPlugin(),
                            () -> AMusic.loadPack(player, gameSettings.getSongPlayListName(), false),
                            20L);
        } else {
            currentState = State.READY;
        }
    }

    public void start() {
        if (currentState != State.READY) {
            return;
        }
        levelSettings.getParticleController().startSpawnParticles(player);
        if (levelSettings.getGameSettings().getSongName() != null) {
            AMusic.setRepeatMode(player, null);
            AMusic.playSound(player, levelSettings.getGameSettings().getSongName());
        }
        Plugin plugin = this.levelsManager.getPlugin();
        for (Player onlinePlayer : player.getWorld().getPlayers()) {
            player.hidePlayer(plugin, onlinePlayer);
        }

        currentState = State.RUNNING;
    }

    @NotNull public LevelSettings getLevelSettings() {
        return levelSettings;
    }

    @NotNull public GameMoveHandler getGameMoveHandler() {
        return gameMoveHandler;
    }

    public void setCurrentState(State currentState) {
        player.sendMessage("State: " + currentState);
        this.currentState = currentState;
    }

    public void stopGame(StopReason reason) {
        player.teleport(levelSettings.getWorldSettings().getSpawn());
        player.setHealth(20);
        player.setGameMode(GameMode.ADVENTURE);
        if (reason == StopReason.WRONG_DIRECTION) {
            player.sendTitle("§cНельзя бежать назад", null, 10, 10, 10);
        } else if (reason == StopReason.LOOSE) {
            player.sendTitle("§cВы проиграли", null, 10, 10, 10);
        } else if (reason == StopReason.FINISH) {
            player.sendTitle("§aВы прошли уровень", null, 10, 10, 10);
        }
        AMusic.stopSound(player);
        player.playSound(player.getLocation(), Sound.ENTITY_SILVERFISH_DEATH, 1, 1);
        levelSettings.getParticleController().stopSpawnParticles(player);
        gameMoveHandler.getAccuracyChecker().reset();

        Plugin plugin = this.getPlugin();
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            player.showPlayer(plugin, onlinePlayer);
        }

        currentState = State.READY;
    }

    @NonNull public Plugin getPlugin() {
        return this.levelsManager.getPlugin();
    }

    public void endGame() {
        endGame(true);
    }

    public void endGame(boolean unloadLevel) {
        player.setHealth(20);
        AMusic.stopSound(player);
        World world = levelSettings.getWorldSettings().getWorld();
        if (unloadLevel && world.getPlayers().isEmpty()) levelsManager.unloadLevel(world.getName());

        Plugin plugin = this.getPlugin();
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            player.showPlayer(plugin, onlinePlayer);
        }

        levelSettings.getParticleController().stopSpawnParticles(player);
    }

    public enum State {
        PREPARING,
        READY,
        RUNNING,
    }

    public enum StopReason {
        FINISH,
        LOOSE,
        WRONG_DIRECTION
    }
}
