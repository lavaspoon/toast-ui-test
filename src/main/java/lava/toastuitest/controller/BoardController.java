package lava.toastuitest.controller;

import lava.toastuitest.domain.Board;
import lava.toastuitest.service.BoardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 데이터 저장
    @PostMapping
    public void addContent(@RequestBody String content) {
        boardService.save(content);
    }

    // 데이터 조회
    @GetMapping
    public List<Board> getContents() {
        return boardService.findAll();
    }
}
