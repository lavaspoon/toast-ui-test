package lava.toastuitest.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="TB_VIDEO")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_nm")
    private String videoNm;

    @Column(name = "vimeo_id")
    private String vimeoId;

    @Column(name = "vimeo_url")
    private String vimeoUrl;

    @Column(name = "vimeo_status")
    private String vimeoStatus;

    @OneToOne(mappedBy = "video")
    @JsonIgnore // 무한 참조 방지
    private Board board;
}
