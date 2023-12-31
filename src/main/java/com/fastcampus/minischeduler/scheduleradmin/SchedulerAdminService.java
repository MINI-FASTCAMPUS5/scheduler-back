package com.fastcampus.minischeduler.scheduleradmin;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.fastcampus.minischeduler.core.exception.Exception400;
import com.fastcampus.minischeduler.core.utils.AES256Utils;
import com.fastcampus.minischeduler.core.utils.DateUtils;
import com.fastcampus.minischeduler.scheduleruser.Progress;
import com.fastcampus.minischeduler.scheduleruser.SchedulerUser;
import com.fastcampus.minischeduler.scheduleruser.SchedulerUserRepository;
import com.fastcampus.minischeduler.user.User;
import com.fastcampus.minischeduler.user.UserRepository;
import com.fastcampus.minischeduler.user.UserResponse;
import com.fastcampus.minischeduler.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static com.fastcampus.minischeduler.scheduleradmin.SchedulerAdminRequest.SchedulerAdminRequestDto;
import static com.fastcampus.minischeduler.scheduleradmin.SchedulerAdminResponse.SchedulerAdminResponseDto;

@Service
@RequiredArgsConstructor
public class SchedulerAdminService {

    private final AmazonS3 amazonS3;

    @PersistenceContext
    private EntityManager entityManager;

    private final HttpServletResponse httpServletResponse;
    private final UserService userService;
    private final SchedulerAdminRepository schedulerAdminRepository;
    private final SchedulerUserRepository schedulerUserRepository;
    private final UserRepository userRepository;
    private final AES256Utils aes256Utils;

    /**
     * 전체 일정 목록을 출력합니다.
     * @return schedulerAdminResponseDtoList
     */
    @Transactional
    public List<SchedulerAdminResponseDto> getSchedulerList() throws GeneralSecurityException {

        List<SchedulerAdmin> schedulers = schedulerAdminRepository.findAll();
        List<SchedulerAdminResponseDto> schedulerAdminResponseDtoList = new ArrayList<>();

        for (SchedulerAdmin scheduler : schedulers) {

            UserResponse.UserDto responseUser = new UserResponse.UserDto(scheduler.getUser());
            responseUser.setFullName(aes256Utils.decryptAES256(responseUser.getFullName()));
            responseUser.setEmail(aes256Utils.decryptAES256(responseUser.getEmail()));

            SchedulerAdminResponseDto schedulerAdminResponseDto =
                    SchedulerAdminResponseDto.builder()
                            .user(responseUser)
                            .id(scheduler.getId())
                            .scheduleStart(scheduler.getScheduleStart())
                            .scheduleEnd(scheduler.getScheduleEnd())
                            .title(scheduler.getTitle())
                            .description(scheduler.getDescription())
                            .image(scheduler.getImage())
                            .createdAt(scheduler.getCreatedAt())
                            .updatedAt(scheduler.getUpdatedAt())
                            .build();
            schedulerAdminResponseDtoList.add(schedulerAdminResponseDto);
        }
        return schedulerAdminResponseDtoList;
    }

    /**
     * year와 month로 해당하는 일정의 스케줄만 반환합니다
     * @param year
     * @param month
     * @return SchedulerAdminResponseDto
     */
    public List<SchedulerAdminResponseDto> getSchedulerListByYearAndMonth(
            Integer year,
            Integer month
    ) throws GeneralSecurityException {

        YearMonth yearMonth = YearMonth.of(year, month);

        List<SchedulerAdmin> schedulers = schedulerAdminRepository.findAll();
        List<SchedulerAdminResponseDto> schedulerAdminResponseDtoList = new ArrayList<>();

        for (SchedulerAdmin scheduler : schedulers) {
            LocalDateTime scheduleStart = scheduler.getScheduleStart();
            LocalDateTime scheduleEnd = scheduler.getScheduleEnd();
            YearMonth scheduleYearMonthStart = YearMonth.of(scheduleStart.getYear(), scheduleStart.getMonth());
            YearMonth scheduleYearMonthEnd = YearMonth.of(scheduleEnd.getYear(), scheduleEnd.getMonth());
            if (yearMonth.equals(scheduleYearMonthStart) || yearMonth.equals(scheduleYearMonthEnd)) {

                UserResponse.UserDto responseUser = new UserResponse.UserDto(scheduler.getUser());
                responseUser.setFullName(aes256Utils.decryptAES256(responseUser.getFullName()));
                responseUser.setEmail(aes256Utils.decryptAES256(responseUser.getEmail()));

                SchedulerAdminResponseDto schedulerAdminResponseDto =
                        SchedulerAdminResponseDto.builder()
                                .user(responseUser)
                                .id(scheduler.getId())
                                .scheduleStart(scheduler.getScheduleStart())
                                .scheduleEnd(scheduler.getScheduleEnd())
                                .title(scheduler.getTitle())
                                .description(scheduler.getDescription())
                                .image(scheduler.getImage())
                                .createdAt(scheduler.getCreatedAt())
                                .updatedAt(scheduler.getUpdatedAt())
                                .build();
                schedulerAdminResponseDtoList.add(schedulerAdminResponseDto);
            }
        }
        return schedulerAdminResponseDtoList;
    }

    /**
     * 일정을 등록합니다.
     * @param schedulerAdminRequestDto
     * @param image
     * @return SchedulerAdminResponseDto
     */
    @Transactional
    public SchedulerAdminResponseDto createScheduler(
            SchedulerAdminRequestDto schedulerAdminRequestDto,
            Long loginUserId,
            MultipartFile image
    ) throws GeneralSecurityException, IOException {

        User user = userRepository.findById(loginUserId)
                .orElseThrow(()-> new Exception400(loginUserId.toString(), "사용자 정보를 찾을 수 없습니다"));

        String imageUrl = null;
        if (image != null) imageUrl = userService.uploadImageToS3(image);
        if (image == null) imageUrl = "https://miniproject12storage.s3.ap-northeast-2.amazonaws.com/default.jpg";


        SchedulerAdmin scheduler = SchedulerAdmin.builder()
                .user(user)
                .scheduleStart(schedulerAdminRequestDto.getScheduleStart())
                .scheduleEnd(schedulerAdminRequestDto.getScheduleEnd())
                .title(schedulerAdminRequestDto.getTitle())
                .description(schedulerAdminRequestDto.getDescription())
                .image(imageUrl)
                .build();
        SchedulerAdmin saveScheduler = schedulerAdminRepository.save(scheduler);

        UserResponse.UserDto responseUser = new UserResponse.UserDto(saveScheduler.getUser());
        responseUser.setFullName(aes256Utils.decryptAES256(user.getFullName()));
        responseUser.setEmail(aes256Utils.decryptAES256(user.getEmail()));

        return SchedulerAdminResponseDto.builder()
                .user(responseUser)
                .id(saveScheduler.getId())
                .scheduleStart(saveScheduler.getScheduleStart())
                .scheduleEnd(saveScheduler.getScheduleEnd())
                .title(saveScheduler.getTitle())
                .description(saveScheduler.getDescription())
                .image(saveScheduler.getImage())
                .createdAt(saveScheduler.getCreatedAt())
                .updatedAt(saveScheduler.getUpdatedAt())
                .build();
    }

    /**
     * 일정을 수정합니다.
     * @param id
     * @param schedulerAdminRequestDto
     * @param image
     * @return id
     */
    @Transactional
    public Long updateScheduler(
            Long id,
            SchedulerAdminRequestDto schedulerAdminRequestDto,
            MultipartFile image
    ) throws IOException {

        SchedulerAdmin scheduler = schedulerAdminRepository.findById(id).orElseThrow(
                ()-> new Exception400(id.toString(), "스케쥴러를 찾을 수 없습니다")
        );

        if (image != null && !image.isEmpty()) {
            // 새로운 이미지가 있다면 저장소에 저장된 기존 이미지는 삭제함
            String oldImage = scheduler.getImage();
            if (oldImage != null && !oldImage.isEmpty()) {
                int slash = oldImage.lastIndexOf("/");
                String fileName = oldImage.substring(slash + 1);
                deleteImage(fileName);
            }
            String imageUrl = userService.uploadImageToS3(image);
            scheduler.update(
                    schedulerAdminRequestDto.getScheduleStart(),
                    schedulerAdminRequestDto.getScheduleEnd(),
                    schedulerAdminRequestDto.getTitle(),
                    schedulerAdminRequestDto.getDescription(),
                    imageUrl
            );
        } else {
            scheduler.update(
                    schedulerAdminRequestDto.getScheduleStart(),
                    schedulerAdminRequestDto.getScheduleEnd(),
                    schedulerAdminRequestDto.getTitle(),
                    schedulerAdminRequestDto.getDescription(),
                    scheduler.getImage()
            );
        }
        return id;
    }

    /**
     * 일정을 삭제합니다.
     * @param id
     * @throws Exception
     */
     @Transactional
     public void delete(Long id) {

         SchedulerAdmin schedulerAdmin = schedulerAdminRepository.findById(id)
                 .orElseThrow(() -> new Exception400(id.toString(), "스케줄을 찾을 수 없습니다"));
         List<SchedulerUser> schedulerUsers = schedulerUserRepository.findBySchedulerAdmin(schedulerAdmin);
         if (!schedulerUsers.isEmpty()) {
             for (SchedulerUser schedulerUser : schedulerUsers) {
                 User user = schedulerUser.getUser();
                 int ticket = user.getSizeOfTicket();
                 user.setSizeOfTicket(ticket + 1);
             }
         }
         //글 삭제시 저장된 image파일도 같이 삭제
         String image = schedulerAdmin.getImage();
         if (image != null && !image.isEmpty()) {
             int slash = image.lastIndexOf("/");
             String fileName = image.substring(slash + 1);
             deleteImage(fileName);
         }
         schedulerAdminRepository.deleteById(id);
     }

    /**
     * 사용자 별 일정을 출력합니다.
     * year와 month가 null이 아니라면 해당하는 년도와 달로 출력합니다
     * @param keyword, year, month
     * @return List<SchedulerAdminResponseDto>
     */
    @Transactional
    public List<SchedulerAdminResponseDto> getSchedulerByFullName(
            String keyword,
            Integer year,
            Integer month
    ) throws GeneralSecurityException {

        YearMonth yearMonth = null;
        if (year != null && month != null) yearMonth = YearMonth.of(year, month);
        System.out.println(keyword + aes256Utils.encryptAES256(keyword));

        List<SchedulerAdminResponseDto> schedulerAdminResponseDtoList = new ArrayList<>();

        for (SchedulerAdmin scheduler : schedulerAdminRepository.findAll()) {
            System.out.println(scheduler);
            UserResponse.UserDto responseUser = new UserResponse.UserDto(scheduler.getUser());
            responseUser.setFullName(aes256Utils.decryptAES256(responseUser.getFullName()));
            responseUser.setEmail(aes256Utils.decryptAES256(responseUser.getEmail()));
            if(responseUser.getFullName().contains(keyword)){
                SchedulerAdminResponseDto schedulerAdminResponseDto = null;
                if (yearMonth != null) {
                    LocalDateTime scheduleStart = scheduler.getScheduleStart();
                    LocalDateTime scheduleEnd = scheduler.getScheduleEnd();
                    YearMonth scheduleYearMonthStart = YearMonth.of(scheduleStart.getYear(), scheduleStart.getMonth());
                    YearMonth scheduleYearMonthEnd = YearMonth.of(scheduleEnd.getYear(), scheduleEnd.getMonth());

                    if (yearMonth.equals(scheduleYearMonthStart) || yearMonth.equals(scheduleYearMonthEnd)) {
                        schedulerAdminResponseDto =
                            SchedulerAdminResponseDto.builder()
                                    .user(responseUser)
                                    .id(scheduler.getId())
                                    .scheduleStart(scheduler.getScheduleStart())
                                    .scheduleEnd(scheduler.getScheduleEnd())
                                    .title(scheduler.getTitle())
                                    .description(scheduler.getDescription())
                                    .image(scheduler.getImage())
                                    .createdAt(scheduler.getCreatedAt())
                                    .updatedAt(scheduler.getUpdatedAt())
                                    .build();
                    }
                } else {
                    schedulerAdminResponseDto =
                        SchedulerAdminResponseDto.builder()
                                .user(responseUser)
                                .id(scheduler.getId())
                                .scheduleStart(scheduler.getScheduleStart())
                                .scheduleEnd(scheduler.getScheduleEnd())
                                .title(scheduler.getTitle())
                                .description(scheduler.getDescription())
                                .image(scheduler.getImage())
                                .createdAt(scheduler.getCreatedAt())
                                .updatedAt(scheduler.getUpdatedAt())
                                .build();
                }
                schedulerAdminResponseDtoList.add(schedulerAdminResponseDto);
            }
        }
        return schedulerAdminResponseDtoList;
    }

    /**
     * 사용자 id로 일정을 검색합니다.
     * @param id : 사용자 id
     * @return   : SchedulerAdminResponseDto
     */
    @Transactional
    public SchedulerAdminResponseDto getSchedulerById(Long id) throws GeneralSecurityException {

        SchedulerAdmin scheduler = schedulerAdminRepository.findById(id).orElseThrow(
                () -> new Exception400(id.toString(), "해당 게시글이 존재하지 않습니다.")
        );

        UserResponse.UserDto responseUser = new UserResponse.UserDto(scheduler.getUser());
        responseUser.setFullName(aes256Utils.decryptAES256(responseUser.getFullName()));
        responseUser.setEmail(aes256Utils.decryptAES256(responseUser.getEmail()));

        return SchedulerAdminResponseDto.builder()
                .user(responseUser)
                .id(scheduler.getId())
                .scheduleStart(scheduler.getScheduleStart())
                .scheduleEnd(scheduler.getScheduleEnd())
                .title(scheduler.getTitle())
                .description(scheduler.getDescription())
                .image(scheduler.getImage())
                .createdAt(scheduler.getCreatedAt())
                .updatedAt(scheduler.getUpdatedAt())
                .build();
    }

    /**
     * 사용자 id로 일정을 검색합니다.
     * @param id : 사용자 id
     * @return   : SchedulerAdmin
     */
    public SchedulerAdmin getSchedulerAdminById(Long id) {
        return schedulerAdminRepository.findById(id).orElse(null);
    }

    /**
     * token으로 사용자를 찾아 사용자가 작성한 모든 schedule을 반환합니다.
     * year와 month가 null이 아니면 각 년도와 달에 부합한 스케줄도 같이 전달합니다.
     * @return Map<String, Object>
     */
    public Map<String, Object> getSchedulerListById(
            Long loginUserId,
            Integer year,
            Integer month
    ) throws GeneralSecurityException {

        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findById(loginUserId)
                .orElseThrow(()->new IllegalArgumentException("사용자 정보를 찾을 수 없습니다"));

        List<SchedulerAdmin> schedulerAgencies = schedulerAdminRepository.findByUser(user);
        List<SchedulerAdminResponseDto> schedulerAdminResponseDtoList = new ArrayList<>();
        List<SchedulerAdminResponseDto> schedulerAdminResponseDtoListByYearAndMonth = new ArrayList<>();

        if (year != null && month != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            for (SchedulerAdmin schedulerAdmin : schedulerAgencies) {
                LocalDateTime scheduleStart = schedulerAdmin.getScheduleStart();
                LocalDateTime scheduleEnd = schedulerAdmin.getScheduleEnd();
                YearMonth scheduleYearMonthStart = YearMonth.of(scheduleStart.getYear(), scheduleStart.getMonth());
                YearMonth scheduleYearMonthEnd = YearMonth.of(scheduleEnd.getYear(), scheduleEnd.getMonth());

                UserResponse.UserDto responseUser = new UserResponse.UserDto(schedulerAdmin.getUser());
                responseUser.setFullName(aes256Utils.decryptAES256(responseUser.getFullName()));
                responseUser.setEmail(aes256Utils.decryptAES256(responseUser.getEmail()));

                if (yearMonth.equals(scheduleYearMonthStart) || yearMonth.equals(scheduleYearMonthEnd)) {
                    SchedulerAdminResponseDto schedulerAdminResponseDto =
                        SchedulerAdminResponseDto.builder()
                                .user(responseUser)
                                .id(schedulerAdmin.getId())
                                .scheduleStart(schedulerAdmin.getScheduleStart())
                                .scheduleEnd(schedulerAdmin.getScheduleEnd())
                                .title(schedulerAdmin.getTitle())
                                .description(schedulerAdmin.getDescription())
                                .image(schedulerAdmin.getImage())
                                .createdAt(schedulerAdmin.getCreatedAt())
                                .updatedAt(schedulerAdmin.getUpdatedAt())
                                .build();
                    schedulerAdminResponseDtoListByYearAndMonth.add(schedulerAdminResponseDto);
                }
            }
            response.put("schedulerAdminListByYearAndMonth", schedulerAdminResponseDtoListByYearAndMonth);
        }

        for (SchedulerAdmin schedulerAdmin : schedulerAgencies) {

            UserResponse.UserDto responseUser = new UserResponse.UserDto(schedulerAdmin.getUser());
            responseUser.setFullName(aes256Utils.decryptAES256(responseUser.getFullName()));
            responseUser.setEmail(aes256Utils.decryptAES256(responseUser.getEmail()));

            SchedulerAdminResponseDto schedulerAdminResponseDto =
                SchedulerAdminResponseDto.builder()
                        .user(responseUser)
                        .id(schedulerAdmin.getId())
                        .scheduleStart(schedulerAdmin.getScheduleStart())
                        .scheduleEnd(schedulerAdmin.getScheduleEnd())
                        .title(schedulerAdmin.getTitle())
                        .description(schedulerAdmin.getDescription())
                        .image(schedulerAdmin.getImage())
                        .createdAt(schedulerAdmin.getCreatedAt())
                        .updatedAt(schedulerAdmin.getUpdatedAt())
                        .build();
            schedulerAdminResponseDtoList.add(schedulerAdminResponseDto);
        }
        response.put("schedulerAdminList", schedulerAdminResponseDtoList);

        return response;
    }

    /**
     * 결재관리 페이지의 해당 기획사 공연에 티케팅한 사용자의 내역과
     * 승인현황 별 티케팅 수를 조회합니다.
     * @return   : 기획사 정보, 관련 티켓승인현황, 기획사 일정
     */
    @Transactional(readOnly = true)
    public SchedulerAdminResponse getAdminScheduleDetail(
            Long loginUserId,
            UserResponse.UserDto userInfo
    ) throws GeneralSecurityException {

        List<SchedulerAdminResponse.ImplScheduleDTO> scheduleDtoList = new ArrayList<>();

        for (
                SchedulerAdminResponse.ScheduleDTO scheduleDTO :
                schedulerAdminRepository.findSchedulesWithUsersById(loginUserId)
        ) {
            SchedulerAdminResponse.ImplScheduleDTO implScheduleDTO =
                    SchedulerAdminResponse.ImplScheduleDTO.builder()
                            .adminScheduleId(scheduleDTO.getAdminScheduleId())
                            .userScheduleId(scheduleDTO.getUserScheduleId())
                            .title(scheduleDTO.getTitle())
                            .description(scheduleDTO.getDescription())
                            .fullName(aes256Utils.decryptAES256(scheduleDTO.getFullName()))
                            .progress(scheduleDTO.getProgress())
                            .scheduleStart(scheduleDTO.getScheduleStart())
                            .build();

            scheduleDtoList.add(implScheduleDTO);
        }

        userInfo.setFullName(aes256Utils.decryptAES256(userInfo.getFullName()));
        userInfo.setEmail(aes256Utils.decryptAES256(userInfo.getEmail()));

        return SchedulerAdminResponse.builder()
                .implScheduleDto(scheduleDtoList)
                .countProcessDto(schedulerAdminRepository.countScheduleGroupByProgressById(loginUserId))
                .userDto(userInfo)
                .build();
    }

    @Transactional
    public UserResponse.UserDto updateUserSchedule(
            Long schedulerAdminId,
            Progress progress,
            UserResponse.UserDto responseUserInfo
    ) throws GeneralSecurityException {

        responseUserInfo.setId(responseUserInfo.getId());
        responseUserInfo.setFullName(aes256Utils.decryptAES256(responseUserInfo.getFullName()));
        responseUserInfo.setEmail(aes256Utils.decryptAES256(responseUserInfo.getEmail()));
        responseUserInfo.setRole(responseUserInfo.getRole());
        responseUserInfo.setProfileImage(responseUserInfo.getProfileImage());
        responseUserInfo.setSizeOfTicket(responseUserInfo.getSizeOfTicket());

        schedulerAdminRepository.updateUserScheduleById(schedulerAdminId, progress);

        return responseUserInfo;
    }

    /**
     * 엑셀 파일을 다운받습니다.
     * @throws Exception : 에러
     */
    public void excelDownload(Long loginUserId) throws GeneralSecurityException, IllegalAccessException, IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("티케팅 현황"); // 엑셀 시트 생성
        sheet.setDefaultColumnWidth(28); // 디폴트 너비 설정

        /**
         * header font style
         */
        XSSFFont headerXSSFFont = (XSSFFont) workbook.createFont();
        headerXSSFFont.setColor(new XSSFColor(
                new byte[]{(byte) 255, (byte) 255, (byte) 255},
                new DefaultIndexedColorMap()
        ));

        /**
         * header cell style
         */
        XSSFCellStyle headerXssfCellStyle = (XSSFCellStyle) workbook.createCellStyle();

        // 테두리 설정
        headerXssfCellStyle.setBorderLeft(BorderStyle.THIN);
        headerXssfCellStyle.setBorderRight(BorderStyle.THIN);
        headerXssfCellStyle.setBorderTop(BorderStyle.THIN);
        headerXssfCellStyle.setBorderBottom(BorderStyle.THIN);

        // 셀 배경 설정
        headerXssfCellStyle.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 108, (byte) 39, (byte) 255},
                new DefaultIndexedColorMap()
        ));
        headerXssfCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerXssfCellStyle.setFont(headerXSSFFont);

        /**
         * body cell style
         */
        XSSFCellStyle bodyXssfCellStyle = (XSSFCellStyle) workbook.createCellStyle();

        // 테두리 설정
        bodyXssfCellStyle.setBorderLeft(BorderStyle.THIN);
        bodyXssfCellStyle.setBorderRight(BorderStyle.THIN);
        bodyXssfCellStyle.setBorderTop(BorderStyle.THIN);
        bodyXssfCellStyle.setBorderBottom(BorderStyle.THIN);

        /**
         * header data
         */
        // SchedulerUser 테이블의 메타정보
        EntityType<?> entityType = entityManager.getMetamodel().entity(SchedulerUser.class);
        Row row = null;
        Cell cell = null;
        int numberOfRow = 0;

        // Header
        List<Field> fields = Arrays.asList(SchedulerUser.class.getDeclaredFields());
        fields.sort(Comparator.comparingInt(
                field -> {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null)
                        return column.columnDefinition().length();
                    else return 0;
                }));

        row = sheet.createRow(numberOfRow++); // 행 추가

        int index = 0;
        for (Field field : fields) {
            String fieldName = field.getName();

            cell = row.createCell(index);
            cell.setCellValue(fieldName);
            cell.setCellStyle(headerXssfCellStyle);
            index++;
        }

        // Body
        for (SchedulerUser schedulerUser : getAllTicketsOfThisAdmin(loginUserId)) {
            row = sheet.createRow(numberOfRow++); // 행 추가
            index = 0;
            for (Field field : schedulerUser.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                cell = row.createCell(index); // 열 추가
                if (field.get(schedulerUser) == null) cell.setCellValue("null");
                if (field.get(schedulerUser) != null) { // 데이터 추가
                    if (field.getName().equals("user")) {
                        User user = (User)field.get(schedulerUser);
                        cell.setCellValue(
                                aes256Utils.decryptAES256(user.getFullName()) + " | " +
                                aes256Utils.decryptAES256(user.getEmail())
                        );
                    } else if (field.getName().equals("schedulerAdmin")) {
                        SchedulerAdmin schedulerAdmin = (SchedulerAdmin)field.get(schedulerUser);
                        cell.setCellValue(
                                "행사번호: " + schedulerAdmin.getId() +
                                " | 제목: " + schedulerAdmin.getTitle() +
                                " | 기간: " + DateUtils.toStringFormat(schedulerAdmin.getScheduleStart()) +
                                "~ " + DateUtils.toStringFormat(schedulerAdmin.getScheduleEnd())
                        );
                    } else {
                        cell.setCellValue(field.get(schedulerUser).toString());
                    }
                }
                cell.setCellStyle(bodyXssfCellStyle); // 바디 스타일 추가
                index++;
            }
        }

        // 컨텐츠 타입과 파일명 지정
        httpServletResponse.setContentType("ms-vnd/excel");
        httpServletResponse.setHeader(
                "Content-Disposition",
                "attachment;filename=" + entityType.getName() + ".xlsx"
        );

        // Excel File Output
        workbook.write(httpServletResponse.getOutputStream());
        workbook.close();
    }

    /**
     * 매년 1월 1일 사용자의 티켓은 12개로 초기화 됩니다.
     */
    @Scheduled(cron = "0 0 0 1 1 *", zone = "Asia/Seoul")
    public void updateTicketEveryYear() {
        System.out.println("1월 1일이 되어 티켓을 12개로 초기화합니다.");
        userRepository.update12TicketsOfAllFans();
    }

    public List<SchedulerUser> getAllTicketsOfThisAdmin(Long id) {
        return schedulerAdminRepository.findAllTicketsByAdminId(id);
    }

    @Transactional
    public void deleteImage(String fileName) {
        amazonS3.deleteObject(new DeleteObjectRequest(userService.getBucketName(), fileName));
    }
}
