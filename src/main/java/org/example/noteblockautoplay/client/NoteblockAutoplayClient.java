package org.example.noteblockautoplay.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class NoteblockAutoplayClient implements ClientModInitializer {

    public static final Logger logger = LogUtils.getLogger();

    Map<Integer, List<BlockPos>> noteMap = new HashMap<>();
    List<Integer> sequence = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Регистрация клавиш
        KeyBinding keyBind1 = new KeyBinding("key.mod.play_sequence", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, KeyBinding.UI_CATEGORY);
        KeyBinding keyBind2 = new KeyBinding("key.mod.play_next_note", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, KeyBinding.UI_CATEGORY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBind1.wasPressed()) {
                // Загрузить файл и сохранить последовательность
                sequence = readMusicFile(client.runDirectory.toPath().resolve("config"));
                scanNoteBlocksInChunk(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player);
            }

            while (keyBind2.wasPressed()) {
                // Воспроизвести следующую ноту
                playNextNote(client.world, client.player, sequence);
            }
        });
    }


    // Примерный код для сканирования нотных блоков в чанке
    public void scanNoteBlocksInChunk(World world, ClientPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        List<BlockPos> noteBlocks = new ArrayList<>();

        // Проходим через все блоки в чанке
        for (int dy = -5; dy < 5; dy++) {
            for (int dx = -5; dx < 5; dx++) {
                for (int dz = -5; dz < 5; dz++) {
                    BlockPos blockPos = player.getBlockPos().add(dx, dy, dz);
                    Block block = world.getBlockState(blockPos).getBlock();
                    // Если блок является нотным
                    if (block instanceof NoteBlock) {
                        noteBlocks.add(blockPos);
                    }
                }
            }
        }


        analyzeNoteBlocks(world, noteBlocks);
    }

    public void analyzeNoteBlocks(World world, List<BlockPos> noteBlocks) {

        for (BlockPos blockPos : noteBlocks) {
            BlockState blockState = world.getBlockState(blockPos);
            int note = blockState.get(NoteBlock.NOTE);

            // Добавляем позицию блока в соответствующий список
            noteMap.computeIfAbsent(note, k -> new ArrayList<>()).add(blockPos);
        }

        // Теперь у нас есть карта с нотами и позициями блоков
    }

    public List<Integer> readMusicFile(Path configPath) {
        List<Integer> sequence = new ArrayList<>();
        Path filePath = configPath.resolve("music.txt");

        try {
            List<String> lines = Files.readAllLines(filePath);

            for (String line : lines) {
                sequence.add(Integer.parseInt(line.trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sequence;
    }

    public void playNextNote(World world, PlayerEntity player, List<Integer> sequence) {
        if (sequence.isEmpty()) return;
        int nextNote = sequence.removeFirst(); // Получаем следующую ноту и удаляем её из списка
        List<BlockPos> possibleBlocks = noteMap.get(nextNote);
        if (possibleBlocks != null && !possibleBlocks.isEmpty()) {
            // Находим ближайший блок
            BlockPos nearestBlock = findNearestBlock(player.getBlockPos(), possibleBlocks);

            // Воспроизводим ноту (кликаем по блоку)
            if (nearestBlock != null) {
                MinecraftClient.getInstance().interactionManager.attackBlock(nearestBlock, Direction.UP);
            }
        }
    }

    private BlockPos findNearestBlock(BlockPos playerPos, List<BlockPos> blockPositions) {
        return blockPositions.stream()
                .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(playerPos)))
                .orElse(null);
    }


}
