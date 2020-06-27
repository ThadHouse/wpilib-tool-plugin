package edu.wpi.first.tools;

public class PlatformMapper {
    private NativePlatforms currentPlatform;

    public synchronized NativePlatforms getCurrentPlatform() {
        if (currentPlatform != null) {
            return currentPlatform;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        String os = "";
    
        if(osName.contains("windows")) {
            os = "win";
        } else if(osName.contains("mac")) {
            os = "mac";
        } else if(osName.contains("linux")) {
            os = "linux";
        } else {
            throw new UnsupportedOperationException("Unknown OS: " + osName);
        }
    
        String osArch = System.getProperty("os.arch");
        String arch = "";
    
        if(osArch.contains("x86_64") || osArch.contains("amd64")) {
            arch = "64";
        } else if(osArch.contains("x86")) {
            arch = "32";
        } else {
            throw new UnsupportedOperationException(osArch);
        }
    
        currentPlatform = NativePlatforms.forName(os + arch);
        return currentPlatform;
    }
    
    public String getWpilibClassifier() {
        NativePlatforms platform = getCurrentPlatform();
        switch(platform) {
            case WIN32: return "windowsx86";
            case WIN64: return "windowsx86-64";
            case MAC: return "osxx86-64";
            case LINUX: return "linuxx86-64";
            default: throw new IllegalArgumentException();
        }
    }
    
    public String getJavaCppClassifier() {
        NativePlatforms platform = getCurrentPlatform();
        switch(platform) {
            case WIN32: return "windows-x86";
            case WIN64: return "windows-x86_64";
            case MAC: return "macosx-x86_64";
            case LINUX: return "linux-x86_64";
            default: throw new IllegalArgumentException();
        }
    }
    
    public String getJavaFxClassifier() {
        NativePlatforms platform = getCurrentPlatform();
        switch(platform) {
            case WIN32: return "win32";
            case WIN64: return "win";
            case MAC: return "mac";
            case LINUX: return "linux";
            default: throw new IllegalArgumentException();
        }
    }
}