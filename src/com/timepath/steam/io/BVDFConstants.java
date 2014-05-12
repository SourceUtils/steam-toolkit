package com.timepath.steam.io;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BVDFConstants {

    private static final Logger LOG = Logger.getLogger(BVDFConstants.class.getName());

    private BVDFConstants() {
    }

    public enum Universe {
        INVALID(0),
        PUBLIC(1),
        BETA(2),
        INTERNAL(3),
        DEV(4);
        private final int id;

        Universe(int i) {
            id = i;
        }

        public static String getName(int i) {
            Universe[] search = Universe.values(); for(Universe search1 : search) {
                if(search1.id == i) {
                    return search1.name();
                }
            } return "UNKNOWN (" + i + ')';
        }
    }

    public enum AppInfoState {
        UNAVAILBALE(1),
        AVAILABLE(2);
        private final int id;

        AppInfoState(int i) {
            id = i;
        }

        public static String getName(int i) {
            AppInfoState[] search = AppInfoState.values(); for(AppInfoState search1 : search) {
                if(search1.id == i) {
                    return search1.name();
                }
            } LOG.log(Level.WARNING, "Unknown {0}: {1}", new Object[] {
                    AppInfoState.class.getSimpleName(), i
            }); return "UNKNOWN (" + i + ')';
        }
    }

    /**
     * Can be found in steamclient native library, EAppInfoSection
     */
    public enum Section {
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
        private final int id;

        Section(int i) {
            id = i;
        }

        public static String get(int i) {
            Section[] search = Section.values(); for(Section search1 : search) {
                if(search1.id == i) {
                    return search1.name();
                }
            } return "UNKNOWN (" + i + ')';
        }
    }

    public enum SteamAppState {
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
        private final int id;

        SteamAppState(int i) {
            id = i;
        }

        public static String get(int i) {
            for(SteamAppState s : SteamAppState.values()) {
                if(s.id == i) {
                    return s.name();
                }
            } return "UNKNOWN (" + i + ')';
        }
    }

    public enum AppInfoSectionPropagationType {
        Invalid(0),
        Public(1),
        OwnersOnly(2),
        ServerOnly(3),
        ClientOnly(4),
        ServerAndWGOnly(5);
        private final int id;

        AppInfoSectionPropagationType(int i) {
            id = i;
        }

        public static String get(int i) {
            for(AppInfoSectionPropagationType s : AppInfoSectionPropagationType.values()) {
                if(s.id == i) {
                    return s.name();
                }
            } return "UNKNOWN (" + i + ')';
        }
    }
}
