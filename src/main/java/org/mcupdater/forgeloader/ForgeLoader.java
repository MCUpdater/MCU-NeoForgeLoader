package org.mcupdater.forgeloader;

import net.minecraftforge.installer.SimpleInstaller;
import net.minecraftforge.installer.actions.*;
import net.minecraftforge.installer.json.InstallV1;
import net.minecraftforge.installer.json.OptionalLibrary;
import net.minecraftforge.installer.json.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ForgeLoader {

	public static void main(String[] args) throws ClassNotFoundException {
		File installPath = new File(args[0]);
		String side = args[1];

		File profiles = new File(installPath, "launcher_profiles.json");
		if (!profiles.exists()) {
			InputStream inStream = ForgeLoader.class.getResourceAsStream("/launcher_profiles.json");
			try {
				Files.copy(inStream, profiles.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<OptionalListEntry> optionals = new ArrayList<>();
		Action action;

		InstallV1 profile = Util.loadInstallProfile();
		ProgressCallback monitor;
		try {
			monitor = ProgressCallback.withOutputs(new OutputStream[]{System.out, getLog()});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			monitor = ProgressCallback.withOutputs(new OutputStream[]{System.out});
		}
		switch(side.toLowerCase()) {
			case "client":
				action = new ClientInstall(profile, monitor);
				break;
			case "server":
				action = new ServerInstall(profile, monitor);
				break;
			default:
				action = new ExtractAction(profile, monitor);
		}
		Predicate<String> optPred = input -> {
			Optional<OptionalListEntry> ent = optionals.stream().filter(e -> e.lib.getArtifact().equals(input)).findFirst();
			return !ent.isPresent() || ent.get().isEnabled();
		};
		try {
			String path = ClientInstall.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			action.run(installPath, optPred, new File(path));
			Path versions = installPath.toPath().resolve("versions");
			if (versions.resolve(profile.getMinecraft()).resolve(profile.getMinecraft() + ".jar").toFile().exists() && !versions.resolve(profile.getVersion()).resolve(profile.getVersion() + ".jar").toFile().exists()) {
				Files.copy(versions.resolve(profile.getMinecraft()).resolve(profile.getMinecraft() + ".jar"), versions.resolve(profile.getVersion()).resolve(profile.getVersion() + ".jar"));
			}
		} catch (ActionCanceledException | IOException e) {
			e.printStackTrace();
		}
	}

	private static OutputStream getLog() throws FileNotFoundException {
		File f = new File(SimpleInstaller.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		File output;
		if (f.isFile()) {
			output = new File(f.getName() + ".log");
		} else {
			output = new File("installer.log");
		}

		return new BufferedOutputStream(new FileOutputStream(output));
	}

	private static class OptionalListEntry {
		OptionalLibrary lib;
		private boolean enabled;

		public boolean isEnabled() {
			return this.enabled;
		}
	}
}
