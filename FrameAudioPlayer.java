import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;
import java.io.File;
import java.util.List;

public class FrameAudioPlayer {

    /**
     * Plays a sequence of BufferedImage frames synchronized with audio.
     *
     * @param frames      The list of BufferedImage frames to display.
     * @param fps         Frames per second for playback.
     * @param audioFile   The audio file to play.
     */
    public static void playFramesWithAudio(List<BufferedImage> frames, int fps, String audioFile) {
        if (frames == null || frames.isEmpty()) {
            System.out.println("No frames to play!");
            return;
        }

        // Load the audio file
        Clip audioClip;
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(audioFile));
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
            return;
        }

        // Create a JFrame to display the frames
        JFrame frame = new JFrame("Frame Audio Player");
        JLabel label = new JLabel(new ImageIcon(frames.get(0))); // Start with the first frame
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Calculate time per frame in milliseconds
        long frameDurationMs = (long) (1000.0 / fps);

        // Start audio playback
        audioClip.start();

        // Playback loop
        long playbackStartTime = System.currentTimeMillis();
        int totalFrames = frames.size();
        int currentFrameIndex = 0;

        while (currentFrameIndex < totalFrames) {
            // Calculate the expected frame index based on audio playback time
            long elapsedTime = System.currentTimeMillis() - playbackStartTime;
            int expectedFrameIndex = (int) (elapsedTime / frameDurationMs);

            if (expectedFrameIndex >= totalFrames) {
                break; // Stop if we've exceeded the total frames
            }

            // Only render frames that are on time
            if (expectedFrameIndex > currentFrameIndex) {
                currentFrameIndex = expectedFrameIndex; // Skip to the correct frame
                label.setIcon(new ImageIcon(frames.get(currentFrameIndex)));
                label.repaint();
            }

            // Short sleep to prevent CPU overload
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Close the frame and stop audio playback when done
        frame.dispose();
        audioClip.stop();
        audioClip.close();
    }

    public static void main(String[] args) {
        // Test with simple generated frames (e.g., gradient frames)
        int width = 640;
        int height = 480;
        int totalFrames = 60; // 2 seconds at 30 FPS
        List<BufferedImage> testFrames = generateTestFrames(width, height, totalFrames);

        // Path to your audio file
        String audioFile = "test_audio.wav";

        // Play the frames with synchronized audio
        playFramesWithAudio(testFrames, 30, audioFile);
    }

    /**
     * Generates test frames with a simple gradient effect for demonstration.
     */
    private static List<BufferedImage> generateTestFrames(int width, int height, int totalFrames) {
        List<BufferedImage> frames = new java.util.ArrayList<>();

        for (int i = 0; i < totalFrames; i++) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // Draw a gradient that changes with the frame index
            g.setPaint(new GradientPaint(0, 0, Color.BLACK, width, height, new Color(i * 4 % 255, i * 8 % 255, i * 16 % 255)));
            g.fillRect(0, 0, width, height);

            g.dispose();
            frames.add(img);
        }
        return frames;
    }
}
