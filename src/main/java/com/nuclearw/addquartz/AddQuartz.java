package com.nuclearw.addquartz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import net.minecraft.server.v1_5_R2.WorldGenMinable;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_5_R2.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class AddQuartz extends JavaPlugin implements Listener, Runnable {
	private static WorldGenMinable orePopulator = new WorldGenMinable(Material.QUARTZ_ORE.getId(), 13, Material.NETHERRACK.getId());
	private static Random random = new Random();

	private static LinkedBlockingQueue<ChunkLocation> chunksToProcess = new LinkedBlockingQueue<ChunkLocation>();
	private static HashSet<String> processList = new HashSet<String>();
	private static HashSet<String> processedList = new HashSet<String>();
	private static HashSet<String> fullWorldList = new HashSet<String>();

	private static File processedFile;
	private static int processedCount = 0;
	private static int totalChunksChecked = 0;
	private static final int quartzId = Material.QUARTZ_ORE.getId();

	@Override
	public void onEnable() {
		getDataFolder().mkdir();

		processedFile = new File(getDataFolder(), "addQuartzProcessed");

		saveDefaultConfig();

		processList.addAll(getConfig().getStringList("worlds"));

		try {
			Files.touch(processedFile);
			BufferedReader reader = Files.newReader(processedFile, Charsets.UTF_8);

			String line;
			while((line = reader.readLine()) != null) {
				processedList.add(line);
			}

			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

		getServer().getPluginManager().registerEvents(this, this);

		getServer().getScheduler().runTaskTimer(this, this, 20, 20);
	}

	@Override
	public void onDisable() {
		saveProcessed();
	}

	@Override
	public void run() {
		if(chunksToProcess.isEmpty()) return;

		int processed = 0;
		while(!chunksToProcess.isEmpty() && processed < 10) {
			ChunkLocation location = chunksToProcess.poll();
			
			// Exclude chunks that were added to the queue multiple times
			if(!processedList.contains(location.toString())) {
			
				World world = getServer().getWorld(location.world);
				// check if chunk exists
				if(world.loadChunk(location.x, location.z, false)) {
					Chunk chunk = world.getChunkAt(location.x, location.z);
					if(!containsQuartz(chunk)) {
		
						int cx = location.x * 16;
						int cz = location.z * 16;
			
						for(int i = 0; i < 16; i++) {
				            int x = cx + random.nextInt(16);
				            int y = random.nextInt(108) + 10;
				            int z = cz + random.nextInt(16);
				            orePopulator.a(((CraftWorld) world).getHandle(), random, x, y, z);
				        }
					
					}
					
					processedList.add(location.toString());
					processed++;
					
					// add surrounding chunks to queue for full population of all chunks
					if(fullWorldList.contains(location.world)) {
						addLocationToQueue(new ChunkLocation(location.world,location.x,location.z+1));
						addLocationToQueue(new ChunkLocation(location.world,location.x,location.z-1));
						addLocationToQueue(new ChunkLocation(location.world,location.x+1,location.z));
						addLocationToQueue(new ChunkLocation(location.world,location.x-1,location.z));
					}
					
					if(!fullWorldList.isEmpty()) {
						totalChunksChecked++;
						if(totalChunksChecked%500==0) {
							getLogger().info("AddQuartz checked "+totalChunksChecked+" chunks.");
						}
					}
					
				}
			}
		}
		
		if(chunksToProcess.isEmpty() && !fullWorldList.isEmpty()) {
			getLogger().info("AddQuartz finished full generation.");
		}

		if(processed > 0) {
			processedCount++;
		}

		if(processedCount > 10) {
			saveProcessed();
			processedCount = 0;
		}
	}

	private void addLocationToQueue(ChunkLocation chunkLocation) {
		final String chunkString = chunkLocation.toString();
		if(!processedList.contains(chunkString)) {
			chunksToProcess.add(chunkLocation);
		}
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if(!event.getWorld().getEnvironment().equals(World.Environment.NETHER)) return;
		if(!processList.contains(event.getWorld().getName())) return;
		if(processedList.contains(event.getWorld().getName() + ":" + event.getChunk().getX() + ":" + event.getChunk().getZ())) return;

		chunksToProcess.add(new ChunkLocation(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ()));
	}
	
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("addquartz")){
			if(args.length==1) {
				String worldName = args[0];
				if(processList.contains(worldName)) {
					fullWorldList.add(worldName);
					
					// Start populating the chunksToProcess with spawn chunk
					Chunk chunk = getServer().getWorld(worldName).getSpawnLocation().getChunk();
					// Clear list to ensure that the world is fully connected
					processedList.clear();
					chunksToProcess.add(new ChunkLocation(worldName, chunk.getX(), chunk.getZ()));
					
					getLogger().info("AddQuartz full generation started for "+worldName);
				}
			}
			return true;
		}
		return false;
	}
	
	private static boolean containsQuartz(Chunk chunk) {
		BlockState[] blocks = chunk.getTileEntities();
		for(BlockState block : blocks) {
			if(block.getTypeId() == quartzId) {
				return true;
			}
		}
		return false;
	}

	private static void saveProcessed() {
		try {
			BufferedWriter writer = Files.newWriter(processedFile, Charsets.UTF_8);
			for(String line : processedList) {
				writer.write(line + "\n");
			}

			writer.flush();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private class ChunkLocation {
		protected final int x, z;
		protected final String world;

		public ChunkLocation(String world, int x, int z) {
			this.x = x;
			this.z = z;
			this.world = world;
		}

		@Override
		public String toString() {
			return world + ":" + x + ":" + z;
		}
	}
}
