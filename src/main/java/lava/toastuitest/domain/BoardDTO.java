package lava.toastuitest.domain;

public class BoardDTO {

    private Long id;
    private String content;
    private String videoUrl;
    private String vimeoId;  // 추가된 vimeoId

    // 생성자
    public BoardDTO(Long id, String content, String videoUrl, String vimeoId) {
        this.id = id;
        this.content = content;
        this.videoUrl = videoUrl;
        this.vimeoId = vimeoId; // vimeoId 추가
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getVimeoId() {
        return vimeoId;
    }
}
