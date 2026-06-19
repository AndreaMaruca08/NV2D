package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;

/**
 * <p>Specific Logger class for the NV2DLIB</p>
 */
@EngineCore
@SuppressWarnings("unused")
public final class NvLogger {
    private static final int NOT_INIT = -111;

    private static String appName = "NvGame";
    private static int majorVersion = NOT_INIT;
    private static int minorVersion = NOT_INIT;
    private static int patch = NOT_INIT;

    private static final String RED = "\u001b[31m";
    private static final String GREEN = "\u001b[32m";
    private static final String YELLOW = "\u001b[33m";
    private static final String MAGENTA = "\u001b[35m";
    private static final String RESET = "\u001b[0m ";

    public static void initialize(
            String appName,
            int majorVersion,
            int minorVersion,
            int patch
    ){
        NvLogger.appName = appName;
        NvLogger.majorVersion = majorVersion;
        NvLogger.minorVersion = minorVersion;
        NvLogger.patch = patch;
        logEngine("Engine: NV2DLIB ver " + majorVersion + "." + minorVersion + "." + patch + " | Logger started successfully");
        logEngine("Engine credit: https://github.com/AndreaMaruca08/NeverNester2dLib");
    }
    public static void initialize(){
        NvLogger.initialize("NvGame", 0, 0, 0);
    }

    private static String baseText = null;
    private static String getDefaultMessage(){
        if(baseText != null)
            return baseText;
        checkInitialized();
        baseText = appName + " | ";
        return baseText;
    }

    private static void checkInitialized(){
        if(majorVersion == NOT_INIT || minorVersion == NOT_INIT || patch == NOT_INIT) {
            initialize();
            logWarn("Logger not initialized");
        }
    }

    public static void logInfo(String info){
        System.out.println(getDefaultMessage() + GREEN+"[INFO]"+RESET + info);
    }
    public static void logInfo(NvComp comp, String info){
        logInfo(comp.getClass().getName() + ": " + info);
    }

    public static void logErr(String info){
        System.out.println(getDefaultMessage() +RED+"[ERR]"+ RESET + info);
    }
    public static void logErr(NvComp comp, String info){
        logErr(comp.getClass().getName() + ": " + info);
    }

    public static void logWarn(String info){
        System.out.println(getDefaultMessage() +YELLOW+"[WARN]" + RESET + info);
    }
    public static void logWarn(NvComp comp, String info){
        logWarn(comp.getClass().getName() + ": " + info);
    }

    public static void logEngine(String info){
        System.out.println(getDefaultMessage() +MAGENTA+"[ENGINE]" + RESET + info);
    }
    public static void logEngine(NvComp comp, String info){
        logEngine(comp.getClass().getName() + ": " + info);
    }
}
