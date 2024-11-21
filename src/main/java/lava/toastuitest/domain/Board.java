package lava.toastuitest.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="TB_BOARD")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="video_id", referencedColumnName = "id")
    private Video video;

    public Board(String content) {
        this.content = content;
    }
}
