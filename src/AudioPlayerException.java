public class AudioPlayerException extends Exception {
    private static final long serialVersionUID = 1L;

    // 메소드 이름: AudioPlayerException
    // 메소드 기능1: 메시지를 받아 예외를 생성
    public AudioPlayerException(String message) {
        super(message);
    }

    // 메소드 이름: AudioPlayerException
    // 메소드 기능1: 메시지와 원인 예외를 받아 예외를 생성
    // 메소드 기능2: 원인 예외를 포함한 예외를 생성
    public AudioPlayerException(String message, Throwable cause) {
        super(message, cause);
    }
}