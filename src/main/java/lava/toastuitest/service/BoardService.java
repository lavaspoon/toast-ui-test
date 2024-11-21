package lava.toastuitest.service;

import jakarta.transaction.Transactional;
import lava.toastuitest.domain.Board;
import lava.toastuitest.repository.BoardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    // 데이터 저장
    public Board save(String content) {
        return boardRepository.save(new Board(content));
    }

    // 모든 데이터 조회
    public List<Board> findAll() {
        return boardRepository.findAll();
    }
}