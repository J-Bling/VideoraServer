//package com.server.video.service;
//
//import com.server.enums.ErrorCode;
//import com.server.enums.ReviewCode;
//import com.server.entity.video.Video;
//import com.server.exception.ApiException;
//import com.server.dao.video.VideoDao;
//import com.server.dao.stats.VideoStatsDao;
//import com.server.service.stats.UserStatsService;
//import com.server.service.videoservice.impl.VideoUploadService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.nio.file.Files;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class VideoUploadServiceTest {
//
//    @Mock
//    private VideoDao videoDao;
//
//    @Mock
//    private VideoStatsDao videoStatsDao;
//
//    @Mock
//    private UserStatsService userStatsService;
//
//    @InjectMocks
//    private VideoUploadService videoUploadService;
//
//    private File tempDir;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        tempDir = Files.createTempDirectory("test-cover").toFile();
//        setField(videoUploadService, "VIDEO_COVER_BASE_URL", tempDir.getAbsolutePath() + File.separator);
//        setField(videoUploadService, "IMAGE_COVER_PREFIX", "http://cover.example.com/");
//    }
//
//    private void setField(Object target, String fieldName, Object value) throws Exception {
//        Field field = VideoUploadService.class.getDeclaredField(fieldName);
//        field.setAccessible(true);
//        field.set(target, value);
//    }
//
//    @Test
//    void createVideoInit_Success() throws Exception {
//        Video video = new Video();
//        video.setAuthor(123);
//
//        MultipartFile imageFile = mock(MultipartFile.class);
//        when(imageFile.isEmpty()).thenReturn(false);
//        when(imageFile.getOriginalFilename()).thenReturn("test.jpg");
//        when(videoDao.insertVideo(any(Video.class))).thenReturn(1);
//
//        Integer result = videoUploadService.createVideoInit(video, imageFile);
//
//        assertEquals(1, result);
//        assertEquals(ReviewCode.REVIEWING.getCode(), video.getReview_status());
//        assertTrue(video.getCover_url().startsWith("http://cover.example.com/"));
//        assertTrue(video.getCover_url().endsWith("test.jpg"));
//
//        verify(videoDao).insertVideo(video);
//        verify(videoStatsDao).createVideoStatsTable(1);
//        verify(userStatsService).CountVideo(123, 1);
//
//        // 验证文件是否保存
//        String suffix = video.getCover_url().replace("http://cover.example.com/", "");
//        File savedFile = new File(tempDir, suffix);
//        assertTrue(savedFile.exists());
//    }
//
//    @Test
//    void createVideoInit_NullImageFile_ThrowsApiException() {
//        Video video = new Video();
//        ApiException exception = assertThrows(ApiException.class,
//                () -> videoUploadService.createVideoInit(video, null));
//        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
//    }
//
//    @Test
//    void createVideoInit_InvalidFileExtension_ThrowsApiException() {
//        Video video = new Video();
//        MultipartFile imageFile = mock(MultipartFile.class);
//        when(imageFile.isEmpty()).thenReturn(false);
//        when(imageFile.getOriginalFilename()).thenReturn("test.txt");
//
//        ApiException exception = assertThrows(ApiException.class,
//                () -> videoUploadService.createVideoInit(video, imageFile));
//        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
//    }
//
//    @Test
//    void createVideoInit_OriginalFilenameNull_ThrowsApiException() {
//        Video video = new Video();
//        MultipartFile imageFile = mock(MultipartFile.class);
//        when(imageFile.isEmpty()).thenReturn(false);
//        when(imageFile.getOriginalFilename()).thenReturn(null);
//
//        ApiException exception = assertThrows(ApiException.class,
//                () -> videoUploadService.createVideoInit(video, imageFile));
//        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
//    }
//
//    @Test
//    void createVideoInit_FileTransferFails_ThrowsIOException() throws Exception {
//        Video video = new Video();
//        MultipartFile imageFile = mock(MultipartFile.class);
//        when(imageFile.isEmpty()).thenReturn(false);
//        when(imageFile.getOriginalFilename()).thenReturn("test.jpg");
//        doThrow(IOException.class).when(imageFile).transferTo(any(File.class));
//
//        assertThrows(IOException.class,
//                () -> videoUploadService.createVideoInit(video, imageFile));
//    }
//}
