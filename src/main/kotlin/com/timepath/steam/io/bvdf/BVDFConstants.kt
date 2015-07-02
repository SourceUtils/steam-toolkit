package com.timepath.steam.io.bvdf


import java.util.logging.Level
import java.util.logging.Logger

public object BVDFConstants {

    private val LOG = Logger.getLogger(javaClass<BVDFConstants>().getName())

    public enum class Universe private constructor(private val id: Int) {
        INVALID(0),
        PUBLIC(1),
        BETA(2),
        INTERNAL(3),
        DEV(4);

        companion object {

            public fun getName(i: Int): String {
                val search = Universe.values()
                for (it in search) {
                    if (it.id == i) {
                        return it.name()
                    }
                }
                LOG.log(Level.WARNING, "Unknown {0}: {1}", arrayOf<Any>(javaClass<Universe>().getSimpleName(), i))
                return "UNKNOWN (" + i + ')'
            }
        }
    }

    public enum class AppInfoState private constructor(private val id: Int) {
        UNAVAILBALE(1),
        AVAILABLE(2);

        companion object {

            public fun getName(i: Int): String {
                val search = AppInfoState.values()
                for (search1 in search) {
                    if (search1.id == i) {
                        return search1.name()
                    }
                }
                LOG.log(Level.WARNING, "Unknown {0}: {1}", arrayOf<Any>(javaClass<AppInfoState>().getSimpleName(), i))
                return "UNKNOWN (" + i + ')'
            }
        }
    }

    /**
     * Can be found in steamclient native library, EAppInfoSection
     */
    public enum class Section private constructor(private val id: Int) {
        UNKNOWN(0),
        ALL(1),
        COMMON(2),
        EXTENDED(3),
        CONFIG(4),
        STATS(5),
        INSTALL(6),
        DEPOTS(7),
        VAC(8),
        DRM(9),
        UFS(10),
        OGG(11),
        ITEMS(12),
        POLICIES(13),
        SYSREQS(14),
        COMMUNITY(15),
        SERVERONLY(16),
        SERVERANDWGONLY(17);

        companion object {

            public fun get(i: Int): String {
                val search = Section.values()
                for (it in search) {
                    if (it.id == i) {
                        return it.name()
                    }
                }
                LOG.log(Level.WARNING, "Unknown {0}: {1}", arrayOf<Any>(javaClass<Section>().getSimpleName(), i))
                return "UNKNOWN (" + i + ')'
            }
        }
    }

    public enum class SteamAppState private constructor(private val id: Int) {
        Invalid(0x00000000),
        Uninstalled(0x00000001),
        UpdateRequired(0x00000002),
        FullyInstalled(0x00000004),
        Encrypted(0x00000008),
        Locked(0x00000010),
        FilesMissing(0x00000020),
        AppRunning(0x00000040),
        FilesCorrupt(0x00000080),
        UpdateRunning(0x00000100),
        UpdatePaused(0x00000200),
        UpdateStarting(0x00000400),
        Uninstalling(0x00000800),
        Reconfiguring(0x00001000),
        Preallocating(0x00002000),
        Downloading(0x00004000),
        Staging(0x00008000),
        Comitting(0x00010000),
        Validating(0x00020000),
        UpdateStopping(0x00040000);

        companion object {

            public fun get(i: Int): String {
                for (s in SteamAppState.values()) {
                    if (s.id == i) {
                        return s.name()
                    }
                }
                LOG.log(Level.WARNING, "Unknown {0}: {1}", arrayOf<Any>(javaClass<SteamAppState>().getSimpleName(), i))
                return "UNKNOWN (" + i + ')'
            }
        }
    }

    public enum class AppInfoSectionPropagationType private constructor(private val id: Int) {
        Invalid(0),
        Public(1),
        OwnersOnly(2),
        ServerOnly(3),
        ClientOnly(4),
        ServerAndWGOnly(5);

        companion object {

            public fun get(i: Int): String {
                for (it in AppInfoSectionPropagationType.values()) {
                    if (it.id == i) {
                        return it.name()
                    }
                }
                LOG.log(Level.WARNING, "Unknown {0}: {1}", arrayOf<Any>(javaClass<AppInfoSectionPropagationType>().getSimpleName(), i))
                return "UNKNOWN (" + i + ')'
            }
        }
    }
}
