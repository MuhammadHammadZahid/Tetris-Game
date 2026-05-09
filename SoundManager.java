import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class SoundManager {

    // This variable holds your background music so you can target it later to stop it
    private static Clip backgroundMusic;
    private static long clipTimePosition = 0; // To save the position when pausing

    public static void pauseMusic() {
        // Check if music exists and is actually playing
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            
            // 1. Save the exact microsecond position
            clipTimePosition = backgroundMusic.getMicrosecondPosition();
            
            // 2. Stop the audio
            backgroundMusic.stop();
        }
    }

    public static void resumeMusic() {
        // Check if music exists and is currently stopped
        if (backgroundMusic != null && !backgroundMusic.isRunning()) {
            
            // 1. Fast-forward the track to the saved position
            backgroundMusic.setMicrosecondPosition(clipTimePosition);
            
            // 2. Start the audio again
            backgroundMusic.start();
            
            // 3. Make sure it goes back to looping!
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void playComboSound(String filePath, int linesCleared) {
        try {
            File soundFile = new File(filePath);
            if (soundFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);

                // --- 1. SCALE THE PITCH (Speed) ---
                // Check if the computer's audio driver supports changing pitch
                if (clip.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
                    FloatControl pitchControl = (FloatControl) clip.getControl(FloatControl.Type.SAMPLE_RATE);
                    float baseRate = pitchControl.getValue(); 
                    
                    // Increase pitch by 10% for every extra line cleared beyond 1
                    // 1 line = 1.0 (normal), 2 lines = 1.1 (faster), 4 lines = 1.3 (fastest)
                    float pitchMultiplier = 1.0f + (0.15f * (linesCleared - 1));
                    pitchControl.setValue(baseRate * pitchMultiplier);
                }

                // --- 2. SCALE THE VOLUME ---
                // Check if the audio driver supports changing volume
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    
                    // Gain is calculated in Decibels (dB). 
                    // 0.0f is normal volume. +6.0f is roughly twice as loud.
                    // Let's add 2.0 decibels of volume for every extra line cleared.
                    float extraDecibels = (linesCleared - 1) * 2.0f;
                    
                    // Safety check: Make sure we don't exceed the maximum allowed volume!
                    float finalVolume = Math.min(extraDecibels, volumeControl.getMaximum());
                    volumeControl.setValue(finalVolume);
                }

                clip.start(); 
                
            } else {
                System.out.println("Cannot find file: " + filePath);
            }
        } catch (Exception e) {
            System.out.println("Error playing scaled sound.");
            e.printStackTrace();
        }
    }

    public static void playMusic(String filePath) {
        try {
            File musicFile = new File(filePath);
            
            if (musicFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicFile);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInput);
                
                // This is the magic line that makes it loop forever!
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                
                backgroundMusic.start(); 
            } else {
                System.out.println("Cannot find file: " + filePath);
            }
        } catch (Exception e) {
            System.out.println("Error playing music (" + filePath + ").");
            e.printStackTrace();
        }
    }

    public static void stopMusic() {
        // Check if there is actually music playing before trying to stop it
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.close(); // Frees up memory
        }
    }

    public static void playSound(String filePath) {
        try {
            File soundFile = new File("OOP-project/" + filePath);
            if (soundFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start(); 
            } else {
                System.out.println("Cannot find file: " + soundFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error playing sound (" + filePath + ").");
            e.printStackTrace();
        }
    }
}
