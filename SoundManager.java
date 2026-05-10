import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

/**
 * SoundManager.java — Low-level audio engine for Tetris.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. SINGLETON PATTERN (Creational)
 * ──────────────────────────────────
 * WHERE : SoundManager class — getInstance() method
 * WHY : The original used static fields and static methods, meaning
 * audio state (backgroundMusic clip, clipTimePosition) was
 * shared as raw global variables. Problems with static state:
 * - Cannot be mocked or replaced in tests
 * - Cannot implement an interface
 * - Hidden global state is hard to reason about
 * Singleton gives one controlled instance with the same
 * "global access" convenience but proper OO structure.
 * The static fields (backgroundMusic, clipTimePosition) are
 * now instance fields on the single managed object.
 * CATEGORY: Creational — controls how the one instance is created.
 *
 * 2. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ───────────────────────────────────────────────
 * WHERE : SoundManager vs SoundEffects
 * WHY : SoundManager is ONLY responsible for:
 * - Loading audio clips from disk
 * - Playing, stopping, pausing, resuming clips
 * - Applying pitch and volume scaling
 * It does NOT decide which game event maps to which file —
 * that is SoundEffects' responsibility. Each class has one
 * reason to change.
 *
 * 3. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ──────────────────────────────────────
 * WHERE : ComboSoundConfig inner class
 * WHY : The original playComboSound() had pitch and volume scaling
 * logic hardcoded as magic numbers inside the method body:
 * float pitchMultiplier = 1.0f + (0.15f * (linesCleared - 1));
 * float extraDecibels = (linesCleared - 1) * 2.0f;
 * To change the scaling formula you had to edit the method —
 * open for modification (bad). ComboSoundConfig encapsulates
 * these values as named fields. The method is now closed for
 * modification; callers pass a config to vary behaviour.
 *
 * 4. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ──────────────────────────────────────────────
 * WHERE : Static forwarding methods preserved for SoundEffects
 * WHY : SoundEffects (high-level) calls SoundManager (low-level).
 * DIP says high-level modules should not depend on low-level
 * details. The static forwarding methods keep the existing
 * API intact while the Singleton instance owns the real state,
 * allowing SoundManager to be replaced or subclassed without
 * touching SoundEffects.
 *
 * 5. DRY — named constants replace magic numbers
 * ──────────────────────────────────────────────
 * WHERE : ComboSoundConfig default values
 * WHY : 0.15f (pitch step) and 2.0f (dB step) were inline magic
 * numbers. Now they are named constants — easy to find,
 * easy to tune, and self-documenting.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class SoundManager {

    // ═════════════════════════════════════════════════════════════════════════
    // SINGLETON — one managed audio engine instance
    // ═════════════════════════════════════════════════════════════════════════

    /** The single instance — null until first access (lazy initialisation). */
    private static SoundManager instance = null;

    /**
     * Returns the single shared SoundManager instance.
     * Creates it on first call (lazy Singleton).
     *
     * PATTERN — Singleton (Creational):
     * All static forwarding methods at the bottom delegate here.
     * Callers can also obtain the instance directly for testing/injection.
     */
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Private constructor — no code outside this class can call new SoundManager().
     */
    private SoundManager() {
    }

    // ─── Instance state (was static global state — now owned by Singleton) ───
    // SOLID/S: state is encapsulated in one place, not scattered as class-level
    // statics.
    private Clip backgroundMusic = null;
    private long clipTimePosition = 0; // microsecond position saved on pause

    // ═════════════════════════════════════════════════════════════════════════
    // ComboSoundConfig — encapsulates scaling parameters (OCP)
    // ─────────────────────────────────────────────────────────────────────────
    // PATTERN : Value Object / Configuration Object
    // PRINCIPLE: OCP — playComboSoundImpl() is closed for modification.
    // To change scaling behaviour, pass a different config;
    // the method body itself never needs to change.
    // DRY — magic numbers replaced with named, documented fields.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Encapsulates pitch and volume scaling parameters for combo sounds.
     *
     * DEFAULT values match the original hardcoded behaviour exactly —
     * no functional change, just better structure.
     */
    public static class ComboSoundConfig {

        // Pitch: each additional line beyond the first adds this fraction to speed.
        // 1 line = 1.0x (normal), 2 lines = 1.15x, 4 lines = 1.45x
        public final float pitchStepPerLine; // original: 0.15f

        // Volume: each additional line beyond the first adds this many decibels.
        // 0.0 dB = normal; +2 dB ≈ slightly louder per extra line
        public final float decibelStepPerLine; // original: 2.0f

        /**
         * Full constructor — allows custom scaling values.
         * Useful if a future mode wants different audio feedback intensity.
         */
        public ComboSoundConfig(float pitchStepPerLine, float decibelStepPerLine) {
            this.pitchStepPerLine = pitchStepPerLine;
            this.decibelStepPerLine = decibelStepPerLine;
        }

        /**
         * Default config — reproduces the original hardcoded behaviour.
         * SoundEffects uses this so existing game audio is unchanged.
         */
        public static ComboSoundConfig defaultConfig() {
            return new ComboSoundConfig(0.15f, 2.0f);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Instance methods — the real audio engine logic
    // ═════════════════════════════════════════════════════════════════════════

    // ── Background Music ─────────────────────────────────────────────────────

    /**
     * Loads and starts looping background music from the given file path.
     * Stops any currently playing music before starting the new track.
     *
     * SRP: only responsible for loading and starting a clip — path
     * resolution is the caller's concern (SoundEffects owns paths).
     */
    public void playMusicImpl(String filePath) {
        try {
            File musicFile = new File(filePath);
            if (musicFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicFile);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInput);
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); // loop forever
                backgroundMusic.start();
            } else {
                System.out.println("[SoundManager] Music file not found: " + filePath);
            }
        } catch (Exception e) {
            System.out.println("[SoundManager] Error playing music: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * Stops and releases the background music clip.
     * Safe to call even when no music is playing.
     */
    public void stopMusicImpl() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.close(); // releases the audio resource
        }
    }

    /**
     * Pauses background music, saving the exact playback position.
     * Paired with resumeMusicImpl() to continue from the same point.
     */
    public void pauseMusicImpl() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            clipTimePosition = backgroundMusic.getMicrosecondPosition(); // save position
            backgroundMusic.stop();
        }
    }

    /**
     * Resumes background music from the saved pause position.
     * Restores looping so music continues indefinitely after resume.
     */
    public void resumeMusicImpl() {
        if (backgroundMusic != null && !backgroundMusic.isRunning()) {
            backgroundMusic.setMicrosecondPosition(clipTimePosition); // restore position
            backgroundMusic.start();
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); // re-enable looping
        }
    }

    // ── One-shot Sound Effects ────────────────────────────────────────────────

    /**
     * Loads and plays a single sound effect at normal pitch and volume.
     * Each call creates a new Clip so multiple sounds can overlap freely.
     *
     * NOTE: "OOP-project/" prefix is prepended here to maintain the
     * original path resolution behaviour unchanged.
     */
    public void playSoundImpl(String filePath) {
        try {
            File soundFile = new File(filePath);
            if (soundFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            } else {
                System.out.println("[SoundManager] Sound file not found: "
                        + soundFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("[SoundManager] Error playing sound: " + filePath);
            e.printStackTrace();
        }
    }

    // ── Combo Sound Effects ───────────────────────────────────────────────────

    /**
     * Plays a sound effect with pitch and volume scaled by combo count.
     *
     * OCP: scaling behaviour is controlled by {@code config}, not by
     * hardcoded values inside this method. The method body is closed
     * for modification — pass a different ComboSoundConfig to vary it.
     *
     * @param filePath     path to the audio file
     * @param linesCleared combo count (drives scaling intensity)
     * @param config       pitch and volume scaling parameters
     */
    public void playComboSoundImpl(String filePath, int linesCleared, ComboSoundConfig config) {
        try {
            File soundFile = new File(filePath);
            if (soundFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);

                // ── 1. Scale pitch (sample rate) ──────────────────────────────
                // Each extra line beyond the first increases playback speed.
                // config.pitchStepPerLine controls the intensity (default 0.15).
                if (clip.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
                    FloatControl pitchControl = (FloatControl) clip.getControl(FloatControl.Type.SAMPLE_RATE);
                    float baseRate = pitchControl.getValue();
                    float pitchMultiplier = 1.0f + (config.pitchStepPerLine * (linesCleared - 1));
                    pitchControl.setValue(baseRate * pitchMultiplier);
                }

                // ── 2. Scale volume (master gain in dB) ───────────────────────
                // Each extra line beyond the first adds decibels.
                // config.decibelStepPerLine controls the intensity (default 2.0).
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float extraDecibels = (linesCleared - 1) * config.decibelStepPerLine;
                    // Safety clamp — never exceed the driver's maximum gain
                    float finalVolume = Math.min(extraDecibels, volumeControl.getMaximum());
                    volumeControl.setValue(finalVolume);
                }

                clip.start();

            } else {
                System.out.println("[SoundManager] Combo sound file not found: " + filePath);
            }
        } catch (Exception e) {
            System.out.println("[SoundManager] Error playing combo sound: " + filePath);
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STATIC FORWARDING METHODS — backward-compatible API for SoundEffects
    // ─────────────────────────────────────────────────────────────────────────
    // SoundEffects calls SoundManager.playMusic(...), SoundManager.playSound(...)
    // etc. as static calls. These forwarders preserve that API exactly,
    // routing through the Singleton instance internally.
    //
    // BENEFIT: SoundEffects requires zero changes. The Singleton pattern
    // and instance state are fully applied under the hood.
    // ═════════════════════════════════════════════════════════════════════════

    /** @see #playMusicImpl(String) */
    public static void playMusic(String filePath) {
        getInstance().playMusicImpl(filePath);
    }

    /** @see #stopMusicImpl() */
    public static void stopMusic() {
        getInstance().stopMusicImpl();
    }

    /** @see #pauseMusicImpl() */
    public static void pauseMusic() {
        getInstance().pauseMusicImpl();
    }

    /** @see #resumeMusicImpl() */
    public static void resumeMusic() {
        getInstance().resumeMusicImpl();
    }

    /** @see #playSoundImpl(String) */
    public static void playSound(String filePath) {
        getInstance().playSoundImpl(filePath);
    }

    /**
     * Plays a combo sound using the default scaling config.
     * Matches the original method signature exactly — no call-site changes needed.
     *
     * @see #playComboSoundImpl(String, int, ComboSoundConfig)
     */
    public static void playComboSound(String filePath, int linesCleared) {
        // Uses ComboSoundConfig.defaultConfig() so existing behaviour is preserved.
        getInstance().playComboSoundImpl(filePath, linesCleared, ComboSoundConfig.defaultConfig());
    }

    /**
     * Overloaded version — allows callers to pass custom scaling config.
     * OCP: new scaling behaviour is added by passing a different config,
     * not by modifying the method.
     */
    public static void playComboSound(String filePath, int linesCleared, ComboSoundConfig config) {
        getInstance().playComboSoundImpl(filePath, linesCleared, config);
    }
}