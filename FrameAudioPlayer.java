import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class FrameAudioPlayer {

    /**
     * Plays a sequence of BufferedImage frames synchronized with audio.
     *
     * @param frames      The list of BufferedImage frames to display.
     * @param fps         Frames per second for playback.
     * @param audioFile   The audio file to play.
     */
    public static void playFramesWithAudio(List<BufferedImage> frames, int fps, String audioFile) {

        long[] pausedDuration = {0};   // Total time spent paused
        long[] pauseStartTime = {0};   // Time when pause started
        long[] playbackStartTime = {System.currentTimeMillis()}; // Start time

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

        // Control Buttons for play, pause, and replay functionality
        JPanel controlPanel = new JPanel();
        JButton playPauseButton = new JButton();
        JButton replayButton = new JButton();

        // Define the button size
        Dimension buttonSize = new Dimension(50, 50); // Fixed size for buttons

        // Load button icons (fallback to text if icons are missing)
        try {
            ImageIcon playIcon = new ImageIcon("icons/play.png");
            ImageIcon pauseIcon = new ImageIcon("icons/pause.png");
            ImageIcon replayIcon = new ImageIcon("icons/replay.png");

            // Scale icons to fit the button size
            playPauseButton.setIcon(new ImageIcon(playIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
            replayButton.setIcon(new ImageIcon(replayIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            System.err.println("Error loading icons: " + e.getMessage());
            playPauseButton.setText("Pause");
            replayButton.setText("Replay");
        }

        // Set fixed button size
        playPauseButton.setPreferredSize(buttonSize);
        replayButton.setPreferredSize(buttonSize);
        controlPanel.add(playPauseButton);
        controlPanel.add(replayButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        // State variables for control
        AtomicBoolean isPlaying = new AtomicBoolean(true);
        AtomicBoolean isReplayRequested = new AtomicBoolean(false);

        
        
        // Play/Pause Button Logic
        playPauseButton.addActionListener(e -> {
            if (isPlaying.get()) {
                // Pause the video
                isPlaying.set(false);
                playPauseButton.setIcon(new ImageIcon("icons/play.png")); // Change to play icon
                audioClip.stop();
                pauseStartTime[0] = System.currentTimeMillis(); // Record when the pause started
            } else {
                // Resume the video
                isPlaying.set(true);
                playPauseButton.setIcon(new ImageIcon("icons/pause.png")); // Change to pause icon
                pausedDuration[0] += System.currentTimeMillis() - pauseStartTime[0]; // Accumulate paused time
                playbackStartTime[0] += System.currentTimeMillis() - pauseStartTime[0]; // Adjust playback time
                audioClip.start();
            }
        });
        

        // Replay Button Logic
        replayButton.addActionListener(e -> {
            isReplayRequested.set(true);
            isPlaying.set(true);
            playPauseButton.setIcon(new ImageIcon("icons/pause.png")); // Reset to pause icon
            audioClip.stop();
            audioClip.setFramePosition(0); // Restart audio
            pausedDuration[0] = 0; // Reset paused duration
            playbackStartTime[0] = System.currentTimeMillis(); // Reset playback time
            audioClip.start();
        });

        frame.pack();
        frame.setVisible(true);

        // Calculate time per frame in milliseconds
        long frameDurationMs = (long) (1000.0 / fps);

        // Playback loop
        int totalFrames = frames.size();
        int currentFrameIndex = 0;

        while (currentFrameIndex < totalFrames) {
            if (isReplayRequested.get()) {
                // Reset to the first frame for replay
                currentFrameIndex = 0; // Reset frame index
                playbackStartTime[0] = System.currentTimeMillis(); // Reset playback time
                pausedDuration[0] = 0; // Reset paused duration
                isReplayRequested.set(false); // Clear the replay request flag
                label.setIcon(new ImageIcon(frames.get(0))); // Show first frame
                label.repaint();
                audioClip.setFramePosition(0); // Restart audio
                if (isPlaying.get()) {
                    audioClip.start();
                }
            }
            

            if (isPlaying.get()) { // Only update frames if playing
                // Calculate the expected frame index based on audio playback time
                long elapsedTime = System.currentTimeMillis() - playbackStartTime[0] - pausedDuration[0];
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
