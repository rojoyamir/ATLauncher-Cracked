/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.data.minecraft.loaders.quilt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.atlauncher.constants.Constants;
import com.atlauncher.data.minecraft.Arguments;
import com.atlauncher.data.minecraft.Library;
import com.atlauncher.data.minecraft.loaders.Loader;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.NetworkClient;
import com.atlauncher.utils.Utils;
import com.atlauncher.workers.InstanceInstaller;
import com.google.gson.reflect.TypeToken;

public class QuiltLoader implements Loader {
    protected String minecraft;
    protected String loaderVersion;
    protected QuiltMetaProfile version;
    protected File tempDir;
    protected InstanceInstaller instanceInstaller;

    @Override
    public void set(Map<String, Object> metadata, File tempDir, InstanceInstaller instanceInstaller,
            LoaderVersion versionOverride) {
        this.minecraft = (String) metadata.get("minecraft");
        this.tempDir = tempDir;
        this.instanceInstaller = instanceInstaller;

        if (versionOverride != null) {
            this.loaderVersion = versionOverride.version;
        } else if (metadata.containsKey("loader")) {
            this.loaderVersion = (String) metadata.get("loader");
        } else if ((boolean) metadata.get("latest")) {
            LogManager.debug("Downloading latest Quilt version");
            this.loaderVersion = this.getLatestVersion();
        }

        this.version = this.getLoader(this.loaderVersion);
    }

    private QuiltMetaProfile getLoader(String version) {
        // The profile json is identical for client and server; the server launch jar
        // is assembled locally in makeServerLaunchJar(), so we always fetch the same.
        return NetworkClient.get(Constants.QUILT_META_API_URL + "/v3/versions/loader/"
                + this.minecraft + "/" + version + "/profile/json", QuiltMetaProfile.class);
    }

    public String getLatestVersion() {
        List<QuiltMetaVersion> versions = NetworkClient.get(
                Constants.QUILT_META_API_URL + "/v3/versions/loader/" + this.minecraft,
                new TypeToken<List<QuiltMetaVersion>>() {
                }.getType());

        if (versions == null || versions.isEmpty()) {
            return null;
        }

        return versions.get(0).loader.version;
    }

    @Override
    public List<Library> getLibraries() {
        return new ArrayList<>(this.version.libraries);
    }

    @Override
    public String getMainClass() {
        return this.version.mainClass;
    }

    @Override
    public String getServerJar() {
        return "quilt-server-launch.jar";
    }

    @Override
    public Path getServerJarPath() {
        return null;
    }

    @Override
    public void downloadAndExtractInstaller() throws Exception {

    }

    @Override
    public void runProcessors() {
        if (!this.instanceInstaller.isServer) {
            return;
        }

        makeServerLaunchJar();
    }

    private void makeServerLaunchJar() {
        File file = new File(this.instanceInstaller.root.toFile(), "quilt-server-launch.jar");
        if (file.exists()) {
            Utils.delete(file);
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

            zipOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,
                    this.version.launcherMainClass == null
                            ? "org.quiltmc.loader.impl.launch.server.QuiltServerLauncher"
                            : this.version.launcherMainClass);
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, getLibraries().stream()
                    .map(library -> instanceInstaller.root
                            .relativize(instanceInstaller.root.resolve("libraries")
                                    .resolve(library.downloads.artifact.path))
                            .normalize().toString())
                    .collect(Collectors.joining(" ")));
            manifest.write(zipOutputStream);

            zipOutputStream.closeEntry();

            zipOutputStream.close();
            outputStream.close();

            FileOutputStream propertiesOutputStream = new FileOutputStream(
                    new File(this.instanceInstaller.root.toFile(), "quilt-server-launcher.properties"));
            propertiesOutputStream.write(("serverJar=" + this.instanceInstaller.getMinecraftJar().getName() + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            propertiesOutputStream.close();
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }
    }

    @Override
    public Arguments getArguments() {
        return this.version.arguments;
    }

    @Override
    public boolean useMinecraftArguments() {
        return true;
    }

    @Override
    public boolean useMinecraftLibraries() {
        return true;
    }

    public static List<LoaderVersion> getChoosableVersions(String minecraft) {
        try {
            List<String> disabledVersions = ConfigManager.getConfigItem("loaders.quilt.disabledVersions",
                    new ArrayList<>());

            List<QuiltMetaVersion> versions = NetworkClient.get(
                    Constants.QUILT_META_API_URL + "/v3/versions/loader/" + minecraft,
                    new TypeToken<List<QuiltMetaVersion>>() {
                    }.getType());

            if (versions == null || versions.isEmpty()) {
                return null;
            }

            return versions.stream()
                    .map(version -> version.loader.version)
                    .filter(version -> !disabledVersions.contains(version))
                    .map(version -> new LoaderVersion(version, false, "Quilt"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Library> getInstallLibraries() {
        return null;
    }

    @Override
    public LoaderVersion getLoaderVersion() {
        return new LoaderVersion(this.loaderVersion, false, "Quilt");
    }
}
