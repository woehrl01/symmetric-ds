package org.jumpmind.symmetric4.model;

import org.jumpmind.util.AbstractVersion;

/**
 * Follow the Apache versioning scheme documented <a
 * href="http://apr.apache.org/versioning.html">here</a>.
 */
final public class Version {

    private static AbstractVersion version = new AbstractVersion() {
        @Override
        protected String getArtifactName() {
            return "symmetric-4";
        }
    };

    public static String version() {
        return version.version();
    }

    public static String versionWithUnderscores() {
        return version.versionWithUnderscores();
    }

    public static int[] parseVersion(String version) {
        return Version.version.parseVersion(version);
    }    

    public static boolean isOlderVersion(String version) {
        return isOlderThanVersion(version, version());
    }

    public static boolean isOlderThanVersion(String checkVersion, String targetVersion) {
        return version.isOlderThanVersion(checkVersion, targetVersion);
    }
       
    public static boolean isOlderMinorVersion(String version) {
        return isOlderMinorVersion(version, version());
    }

    public static boolean isOlderMinorVersion(String checkVersion, String targetVersion) {
        return version.isOlderMinorVersion(checkVersion, targetVersion);
    }

    public static long getBuildTime() {
        return version.getBuildTime();
    }

}