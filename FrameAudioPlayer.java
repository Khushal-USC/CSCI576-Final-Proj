import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameAudioPlayer {

    /**
     * Plays a sequence of BufferedImage frames synchronized with audio and supports frame-by-frame navigation.
     *
     * @param frames    The list of BufferedImage frames to display.
     * @param fps       Frames per second for playback.
     * @param audioFile The audio file to play.
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

        // Control Buttons for play/pause and replay
        JPanel controlPanel = new JPanel();
        JButton playPauseButton = new JButton("Play");
        JButton replayButton = new JButton("Replay");
        controlPanel.add(playPauseButton);
        controlPanel.add(replayButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);

        // State variables
        AtomicBoolean isPlaying = new AtomicBoolean(false); // Start paused
        AtomicBoolean isReplayRequested = new AtomicBoolean(false);
        int totalFrames = frames.size();
        int[] currentFrameIndex = {0}; // Track the current frame index

        // Key Listener for frame-by-frame navigation
        frame.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (!isPlaying.get()) { // Only allow frame-by-frame navigation when paused
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT) {
                        // Go to the next frame
                        if (currentFrameIndex[0] < totalFrames - 1) {
                            currentFrameIndex[0]++;
                            label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                            label.repaint();
                        }
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) {
                        // Go to the previous frame
                        if (currentFrameIndex[0] > 0) {
                            currentFrameIndex[0]--;
                            label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                            label.repaint();
                        }
                    }
                }
            }
        });

        // Play/Pause Button Logic
        playPauseButton.addActionListener(e -> {
            if (isPlaying.get()) {
                // Pause the video
                isPlaying.set(false);
                playPauseButton.setText("Play");
                audioClip.stop();
            } else {
                // Resume the video
                isPlaying.set(true);
                playPauseButton.setText("Pause");
                if (currentFrameIndex[0] == 0 || isReplayRequested.get()) {
                    audioClip.setFramePosition(0); // Restart audio if at the beginning
                    isReplayRequested.set(false);
                }
                audioClip.start();
            }
        });

        // Replay Button Logic
        replayButton.addActionListener(e -> {
            isReplayRequested.set(true);
            isPlaying.set(true);
            playPauseButton.setText("Pause");
            audioClip.stop();
            audioClip.setFramePosition(0); // Restart audio
            currentFrameIndex[0] = 0; // Restart from the first frame
            label.setIcon(new ImageIcon(frames.get(0)));
            label.repaint();
            audioClip.start();
        });

        // Frame Duration
        long frameDurationMs = (long) (1000.0 / fps);

        // Playback loop
        new Thread(() -> {
            while (true) {
                if (isReplayRequested.get()) {
                    // Reset for replay
                    currentFrameIndex[0] = 0;
                    isReplayRequested.set(false);
                }

                if (isPlaying.get() && currentFrameIndex[0] < totalFrames) {
                    // Display the current frame
                    label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                    label.repaint();

                    // Wait for the next frame
                    try {
                        Thread.sleep(frameDurationMs);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    currentFrameIndex[0]++;
                } else if (currentFrameIndex[0] >= totalFrames) {
                    // Stop playback at the end
                    isPlaying.set(false);
                    playPauseButton.setText("Play");
                    audioClip.stop();
                }
            }
        }).start();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
