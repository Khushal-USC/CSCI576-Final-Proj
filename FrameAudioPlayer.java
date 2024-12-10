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

        //load the audio file
        Clip audioClip;
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(audioFile));
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
            return;
        }

        //create a JFrame to display the frames
        JFrame frame = new JFrame("Frame Audio Player");
        //starting with the first frame
        JLabel label = new JLabel(new ImageIcon(frames.get(0))); 
        frame.getContentPane().add(label, BorderLayout.CENTER);

        //control Buttons for play/pause, replay, and frame-by-frame navigation
        JPanel controlPanel = new JPanel();
        JButton playPauseButton = new JButton("Play");
        JButton replayButton = new JButton("Replay");
        JButton nextFrameButton = new JButton("Next Frame");
        JButton previousFrameButton = new JButton("Previous Frame");
        controlPanel.add(playPauseButton);
        controlPanel.add(replayButton);
        controlPanel.add(previousFrameButton);
        controlPanel.add(nextFrameButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);

        //state variables
        //start the video paused
        AtomicBoolean isPlaying = new AtomicBoolean(false);
        AtomicBoolean isReplayRequested = new AtomicBoolean(false);
        int totalFrames = frames.size();
        //track current frame index
        int[] currentFrameIndex = {0}; 

        //key Listener for frame-by-frame navigation
        frame.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                //only allow frame-by-frame navigation when paused
                if (!isPlaying.get()) { 
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT) {
                        //go to the next frame
                        if (currentFrameIndex[0] < totalFrames - 1) {
                            currentFrameIndex[0]++;
                            label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                            label.repaint();
                        }
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) {
                        //go to the previous frame
                        if (currentFrameIndex[0] > 0) {
                            currentFrameIndex[0]--;
                            label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                            label.repaint();
                        }
                    }
                }
            }
        });

        //play/Pause Button Logic
        playPauseButton.addActionListener(e -> {
            if (isPlaying.get()) {
                //pause the video
                isPlaying.set(false);
                playPauseButton.setText("Play");
                audioClip.stop();
            } else {
                //resume the video
                isPlaying.set(true);
                playPauseButton.setText("Pause");
                if (currentFrameIndex[0] == 0 || isReplayRequested.get()) {
                    //restart audio if at the beginning
                    audioClip.setFramePosition(0); 
                    isReplayRequested.set(false);
                }
                audioClip.start();
            }
        });

        //replay Button Logic
        replayButton.addActionListener(e -> {
            isReplayRequested.set(true);
            isPlaying.set(true);
            playPauseButton.setText("Pause");
            audioClip.stop();
            //restart audio
            audioClip.setFramePosition(0);
            //restart from the first frame 
            currentFrameIndex[0] = 0;
            label.setIcon(new ImageIcon(frames.get(0)));
            label.repaint();
            audioClip.start();
        });

        //next Frame Button Logic
        nextFrameButton.addActionListener(e -> {
            //only when paused
            if (!isPlaying.get() && currentFrameIndex[0] < totalFrames - 1) { 
                currentFrameIndex[0]++;
                label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                label.repaint();
            }
        });

        //previous Frame Button Logic
        previousFrameButton.addActionListener(e -> {
            //only when paused
            if (!isPlaying.get() && currentFrameIndex[0] > 0) { 
                currentFrameIndex[0]--;
                label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                label.repaint();
            }
        });

        //frame Duration
        long frameDurationMs = (long) (1000.0 / fps);

        //playback loop
        new Thread(() -> {
            while (true) {
                if (isReplayRequested.get()) {
                    //reset for replay
                    currentFrameIndex[0] = 0;
                    isReplayRequested.set(false);
                }

                if (isPlaying.get() && currentFrameIndex[0] < totalFrames) {
                    //display the current frame
                    label.setIcon(new ImageIcon(frames.get(currentFrameIndex[0])));
                    label.repaint();

                    //wait for the next frame
                    try {
                        Thread.sleep(frameDurationMs);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    currentFrameIndex[0]++;
                } else if (currentFrameIndex[0] >= totalFrames) {
                    //stop playback at the end
                    isPlaying.set(false);
                    playPauseButton.setText("Play");
                    audioClip.stop();
                }
            }
        }).start();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        //test with simple generated frames (e.g., gradient frames)
        int width = 640;
        int height = 480;
        //2 seconds at 30FPS
        int totalFrames = 60; 
        List<BufferedImage> testFrames = generateTestFrames(width, height, totalFrames);

        //path to your audio file
        String audioFile = "test_audio.wav";

        //play the frames with synchronized audio
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

            //draw a gradient that changes with the frame index
            g.setPaint(new GradientPaint(0, 0, Color.BLACK, width, height, new Color(i * 4 % 255, i * 8 % 255, i * 16 % 255)));
            g.fillRect(0, 0, width, height);

            g.dispose();
            frames.add(img);
        }
        return frames;
    }
}
