package com.coala.appliedpowah.integration;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Built-in switchable resource pack — the 1.21.1-style texture set produced by the
 * texture pipeline ({@code texture_pack_work}, see {@code asset/README.md}).
 *
 * <p>The pack content ships inside the mod jar under {@code builtin_pack_1211/} and is
 * exposed as a normal {@link PackResources} so it shows up in the resource-pack screen
 * as "Applied Powah 1.21.1 style" (never auto-enabled, never required).</p>
 *
 * <p>Missing content falls back to the mod's own resources — an empty
 * {@code builtin_pack_1211/pack.mcmeta} still registers a usable (no-op) pack.</p>
 */
public final class APBuiltinPacks {

    /** Classpath root of the built-in pack content. */
    public static final String PACK_1211_ROOT = "builtin_pack_1211/";
    public static final String PACK_1211_ID = "applied_powah/builtin_1211";

    private APBuiltinPacks() {
    }

    /** @return the 1.21.1 built-in pack, or {@code null} when its metadata is absent. */
    @Nullable
    public static Pack create1211() {
        try {
            URL root = APBuiltinPacks.class.getClassLoader().getResource("builtin_pack_1211/pack.mcmeta");
            if (root == null) {
                AppliedPowah.LOG.debug("builtin_pack_1211/pack.mcmeta not on the classpath — pack not registered");
                return null;
            }
            // Pack.ResourcesSupplier is a functional interface: open(String id) -> PackResources
            var supplier = (Pack.ResourcesSupplier) id -> new ClasspathPackResources(id);
            return Pack.readMetaAndCreate(
                    PACK_1211_ID,
                    Component.translatable("applied_powah.pack.1211"),
                    false,
                    supplier,
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN);
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("Failed to create built-in 1.21.1 pack: {}", t.toString());
            return null;
        }
    }

    /** A read-only {@link PackResources} backed by the mod classpath. */
    static final class ClasspathPackResources extends AbstractPackResources {

        private final Set<String> namespaces;

        ClasspathPackResources(String id) {
            super(id, false);
            this.namespaces = listNamespaces();
        }

        private static String resourcePath(PackType type, ResourceLocation location) {
            return PACK_1211_ROOT + type.getDirectory() + "/" + location.getNamespace()
                    + "/" + location.getPath();
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getRootResource(String... parts) {
            StringBuilder sb = new StringBuilder(PACK_1211_ROOT);
            for (String part : parts) {
                if (sb.length() > PACK_1211_ROOT.length()) {
                    sb.append('/');
                }
                sb.append(part);
            }
            URL url = APBuiltinPacks.class.getClassLoader().getResource(sb.toString());
            return url == null ? null : url::openStream;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            URL url = APBuiltinPacks.class.getClassLoader().getResource(resourcePath(type, location));
            return url == null ? null : url::openStream;
        }

        @Override
        public void listResources(PackType type, String namespace, String prefix,
                PackResources.ResourceOutput output) {
            String dir = PACK_1211_ROOT + type.getDirectory() + "/" + namespace + "/";
            for (String entry : listEntries(dir)) {
                if (!entry.startsWith(prefix) || !entry.contains(".")) {
                    continue;
                }
                String path = entry.substring(prefix.length());
                ResourceLocation id = new ResourceLocation(namespace, path);
                URL url = APBuiltinPacks.class.getClassLoader().getResource(dir + entry);
                if (url != null) {
                    output.accept(id, url::openStream);
                }
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return namespaces;
        }

        @Nullable
        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
            return super.getMetadataSection(serializer);
        }

        @Override
        public void close() {
            // nothing to release — classpath only
        }

        /** Namespaces present under the pack root (assets/…). */
        private static Set<String> listNamespaces() {
            Set<String> out = listEntries(PACK_1211_ROOT + "assets/");
            return out.stream()
                    .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                    .filter(s -> !s.isEmpty() && !s.contains("/"))
                    .collect(Collectors.toUnmodifiableSet());
        }

        /**
         * Immediate children of a classpath directory, as relative names.
         * Works both for a plain dev directory and for entries inside the mod jar.
         */
        private static Set<String> listEntries(String dir) {
            try {
                URL url = APBuiltinPacks.class.getClassLoader().getResource(dir);
                if (url == null) {
                    return Collections.emptySet();
                }
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    Path base = Paths.get(url.toURI());
                    if (!Files.isDirectory(base)) {
                        return Collections.emptySet();
                    }
                    try (var stream = Files.list(base)) {
                        return stream.map(p -> p.getFileName().toString())
                                .collect(Collectors.toUnmodifiableSet());
                    }
                }
                if ("jar".equals(protocol)) {
                    JarURLConnection connection = (JarURLConnection) url.openConnection();
                    String prefix = connection.getEntryName();
                    Map<String, Boolean> seen = new HashMap<>();
                    try (JarFile jar = getJarFile(connection)) {
                        jar.stream()
                                .map(java.util.jar.JarEntry::getName)
                                .filter(n -> n.startsWith(prefix))
                                .map(n -> n.substring(prefix.length()))
                                .filter(n -> !n.isEmpty())
                                .forEach(n -> {
                                    int slash = n.indexOf('/');
                                    seen.put(slash < 0 ? n : n.substring(0, slash + 1), Boolean.TRUE);
                                });
                    }
                    return seen.keySet();
                }
            } catch (Throwable t) {
                AppliedPowah.LOG.debug("listEntries({}) failed: {}", dir, t.toString());
            }
            return Collections.emptySet();
        }

        /** Jar files stay open for the game session; the JVM owns the filesystem. */
        private static JarFile getJarFile(JarURLConnection connection) throws IOException {
            URL jarUrl = connection.getJarFileURL();
            try {
                URI uri = jarUrl.toURI();
                String scheme = uri.getScheme();
                if (scheme.equals("file")) {
                    return new JarFile(Paths.get(uri).toFile());
                }
                return connection.getJarFile();
            } catch (URISyntaxException e) {
                return connection.getJarFile();
            } catch (FileSystemNotFoundException e) {
                // A nested jar filesystem may not be created yet.
                try {
                    FileSystems.newFileSystem(jarUrl.toURI(), Collections.emptyMap());
                } catch (Throwable ignored) {
                    // fall through to the connection's own handle
                }
                return connection.getJarFile();
            }
        }
    }
}
