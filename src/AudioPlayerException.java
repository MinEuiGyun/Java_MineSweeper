public class AudioPlayerException extends Exception {
    private static final long serialVersionUID = 1L;

    //_______AudioPlayerException_______
    // 오디오 재생 관련 예외 처리를 위한 생성자
    // 에러 메시지를 포함한 예외 객체 생성
    public AudioPlayerException(String message) {
        super(message);
    }

    //_______AudioPlayerException_______
    // 원인 예외와 함께 오디오 재생 예외를 생성하는 생성자
    // 에러 메시지와 원인 예외를 포함한 예외 객체 생성
    public AudioPlayerException(String message, Throwable cause) {
        super(message, cause);
    }
}