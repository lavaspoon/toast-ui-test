package lava.toastuitest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lava.toastuitest.domain.Board;
import lava.toastuitest.domain.BoardDTO;
import lava.toastuitest.domain.Video;
import lava.toastuitest.repository.BoardRepository;
import lava.toastuitest.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final VideoRepository videoRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(BoardService.class);

    @Value("${vimeo.access.token}")
    private String vimeoAccessToken;

    public BoardService(BoardRepository boardRepository, VideoRepository videoRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.boardRepository = boardRepository;
        this.videoRepository = videoRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // 게시글 목록 조회
    public List<BoardDTO> findAll() {
        List<Board> boards = boardRepository.findAll();
        List<BoardDTO> boardDTOs = new ArrayList<>();

        for (Board board : boards) {
            String videoUrl = (board.getVideo() != null) ? board.getVideo().getVimeoUrl() : null;
            String vimeoId = (board.getVideo() != null) ? board.getVideo().getVimeoId() : null;
            boardDTOs.add(new BoardDTO(board.getId(), board.getContent(), videoUrl, vimeoId));
        }

        return boardDTOs;
    }

    @Transactional
    public Board save(String content, MultipartFile videoFile) throws IOException {
        // Step 1: 비메오 업로드 요청
        String vimeoId = uploadToVimeo(videoFile);
        String vimeoUrl = "https://vimeo.com/" + vimeoId;

        // Step 2: Video 엔티티 저장
        Video video = new Video();
        video.setVideoNm(videoFile.getOriginalFilename());
        video.setVimeoId(vimeoId);
        video.setVimeoUrl(vimeoUrl);
        Video savedVideo = videoRepository.save(video);

        // Step 3: Board 엔티티 저장
        Board board = new Board(content);
        board.setVideo(savedVideo);

        // Step 4: Transcode 상태 확인 후 완료될 때까지 기다리기
        waitForTranscodeCompletion(video);

        return boardRepository.save(board);
    }

    private String uploadToVimeo(MultipartFile videoFile) throws IOException {
        // Step 1: 비메오 업로드 URL 요청
        String createVideoUrl = "https://api.vimeo.com/me/videos";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vimeoAccessToken);
        headers.set("Content-Type", "application/json");

        String createPayload = "{\"upload\":{\"approach\":\"tus\",\"size\":\"" + videoFile.getSize() + "\"}}";
        HttpEntity<String> request = new HttpEntity<>(createPayload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(createVideoUrl, request, String.class);
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        String uploadLink = responseJson.get("upload").get("upload_link").asText();
        String vimeoId = responseJson.get("uri").asText().split("/")[2];

        // Step 2: 비디오 업로드
        headers = new HttpHeaders();
        headers.set("Tus-Resumable", "1.0.0");
        headers.set("Content-Type", "application/offset+octet-stream");
        headers.set("Upload-Offset", "0");

        HttpEntity<byte[]> videoRequest = new HttpEntity<>(videoFile.getBytes(), headers);
        restTemplate.patchForObject(uploadLink, videoRequest, String.class);

        return vimeoId;
    }

    private void waitForTranscodeCompletion(Video video) throws IOException {
        String checkTranscodeUrl = "https://api.vimeo.com/videos/" + video.getVimeoId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vimeoAccessToken);

        // 최대 대기 시간: 30분
        long timeout = 30 * 60 * 1000;  // 30분을 밀리초로 변환
        long startTime = System.currentTimeMillis();  // 시작 시간 기록

        boolean isComplete = false;
        while (!isComplete) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (elapsedTime > timeout) {
                throw new IOException("Transcoding did not complete within 30 minutes. Timeout occurred.");
            }

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = null;
            try {
                response = restTemplate.exchange(checkTranscodeUrl, HttpMethod.GET, request, String.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                // 요청 제한에 도달하면 1분간 대기 후 재시도
                try {
                    Thread.sleep(60000);  // 1분 대기
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for transcode completion", ex);
                }
                continue;  // 요청을 다시 시도
            }

            JsonNode responseJson = objectMapper.readTree(response.getBody());

            // 'transcode' 객체가 존재하는지 먼저 확인
            JsonNode transcodeNode = responseJson.path("transcode");
            if (transcodeNode.isMissingNode()) {
                logger.warn("Transcode node is missing in the response for Vimeo ID: {}", video.getVimeoId());
            } else {
                String transcodeStatus = transcodeNode.path("status").asText();
                // 30초마다 상태를 로그로 찍음
                logger.info("Transcode status: {}", transcodeStatus);

                if ("complete".equalsIgnoreCase(transcodeStatus)) {
                    // Transcode 완료 시 Video 엔티티의 상태를 "complete"로 업데이트
                    video.setVimeoStatus("complete");
                    videoRepository.save(video);
                    isComplete = true;
                }
            }

            if (!isComplete) {
                try {
                    Thread.sleep(30000); // 30초마다 상태를 확인
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for transcode completion", e);
                }
            }
        }
    }
}
