import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AudioPlayer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AudioPlayer.class.getName());
    private AudioInputStream audioInputStream;
    private Clip clip;
    private volatile boolean isPlaying;

    // play 메소드
    // 메소드 기능1: 주어진 파일 경로의 오디오 파일을 재생
    // 메소드 기능2: 오디오 파일을 반복 재생
    public synchronized void play(String filePath) throws AudioPlayerException {
        try {
            close();
            URL resourceUrl = getClass().getResource("/" + filePath);
            if (resourceUrl == null) {
                File file = new File(filePath);
                if (file.exists()) {
                    resourceUrl = file.toURI().toURL();
                }
            }
            if (resourceUrl == null) {
                throw new AudioPlayerException("Audio file not found: " + filePath);
            }

            LOGGER.log(Level.INFO, "Loading audio from: " + resourceUrl);
            audioInputStream = AudioSystem.getAudioInputStream(resourceUrl);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            isPlaying = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to play audio: " + e.getMessage(), e);
            throw new AudioPlayerException("Failed to play audio: " + e.getMessage(), e);
        }
    }

    // close 메소드
    // 메소드 기능1: 현재 재생 중인 오디오를 중지
    // 메소드 기능2: 오디오 스트림과 클립을 닫음
    @Override
    public synchronized void close() {
        isPlaying = false;
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing clip", e);
            } finally {
                clip = null;
            }
        }
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing audio stream", e);
            } finally {
                audioInputStream = null;
            }
        }
    }

    // stop 메소드
    // 메소드 기능1: 현재 재생 중인 오디오를 중지
    // 메소드 기능2: 오디오 스트림과 클립을 닫음
    public synchronized void stop() {
        close();
    }

    // isPlaying 메소드
    // 메소드 기능1: 오디오가 재생 중인지 여부를 반환
    public boolean isPlaying() {
        return isPlaying;
    }
}