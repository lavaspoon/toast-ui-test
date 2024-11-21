package lava.toastuitest.controller;

import lava.toastuitest.domain.Board;
import lava.toastuitest.domain.BoardDTO;
import lava.toastuitest.service.BoardService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public Board addContent(@RequestParam("content") String content,
                            @RequestParam("video") MultipartFile videoFile) throws IOException {
        return boardService.save(content, videoFile);
    }

    @GetMapping
    public List<BoardDTO> getAllBoards() {
        return boardService.findAll();
    }

}