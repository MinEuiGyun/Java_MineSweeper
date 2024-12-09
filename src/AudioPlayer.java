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

    //_______play_______
    // 지정된 오디오 파일을 재생
    // 파일을 찾아서 연속 재생 모드로 실행
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

    //_______close_______
    // 오디오 재생을 중단하고 리소스 해제
    // 클립과 오디오 스트림을 안전하게 종료
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

    //_______stop_______
    // 오디오 재생을 중단
    // close 메소드를 호출하여 재생 중단 및 리소스 정리
    public synchronized void stop() {
        close();
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}