package com.example.momobe.reservation.ui;

import com.example.momobe.common.config.SecurityTestConfig;
import com.example.momobe.common.enums.TestConstants;
import com.example.momobe.common.exception.enums.ErrorCode;
import com.example.momobe.common.exception.ui.ExceptionController;
import com.example.momobe.common.resolver.JwtArgumentResolver;
import com.example.momobe.meeting.domain.MeetingNotFoundException;
import com.example.momobe.payment.domain.enums.PayState;
import com.example.momobe.payment.domain.enums.PayType;
import com.example.momobe.reservation.application.ReserveService;
import com.example.momobe.reservation.domain.ReservationNotPossibleException;
import com.example.momobe.reservation.dto.in.RequestReservationDto;
import com.example.momobe.reservation.dto.out.PaymentResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.apache.bcel.generic.ObjectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static com.example.momobe.common.enums.TestConstants.*;
import static com.example.momobe.common.exception.enums.ErrorCode.*;
import static com.example.momobe.payment.domain.enums.PayState.*;
import static com.example.momobe.payment.domain.enums.PayType.CARD;
import static org.aspectj.apache.bcel.generic.ObjectType.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureRestDocs
@Import(SecurityTestConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
@WebMvcTest({ReservationController.class, ExceptionController.class})
class ReservationControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    ReserveService reserveService;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    JwtArgumentResolver jwtArgumentResolver;

    @Test
    @DisplayName("유효성 검사에 실패할 경우 400 반환")
    void postReservation_fail1() throws Exception {
        //given
        RequestReservationDto request = RequestReservationDto.builder()
                .dateInfo(RequestReservationDto.ReservationDateDto.builder()
                        .build())
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isBadRequest())
                .andDo(document("postReservation/400",
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("비어있을 수 없습니다."),
                                        fieldWithPath("dateInfo.startTime").description("비어있을 수 없습니다."),
                                        fieldWithPath("dateInfo.endTime").description("비어있을 수 없습니다."),
                                        fieldWithPath("amount").description("비어있을 수 없습니다."),
                                        fieldWithPath("reservationMemo").description("예약자가 남기는 메모")
                                )
                        )
                );
    }

    @Test
    @DisplayName("해당 예약 시간에 예약이 불가능한 경우 409 반환")
    void postReservation_fail2() throws Exception {
        //given
        given(reserveService.reserve(anyLong(), any(RequestReservationDto.class), any()))
                .willThrow(new ReservationNotPossibleException(FULL_OF_PEOPLE, "인원이 가득 찼습니다."));

        RequestReservationDto request = RequestReservationDto.builder()
                .dateInfo(RequestReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("postReservation/409/full",
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        requestFields(
                                fieldWithPath("dateInfo.reservationDate").description("예약일"),
                                fieldWithPath("dateInfo.startTime").description("예약 시작 시간"),
                                fieldWithPath("dateInfo.endTime").description("예약 종료 시간"),
                                fieldWithPath("amount").description("비용"),
                                fieldWithPath("reservationMemo").description("예약자가 남기는 메모")
                        )
                )
                );
    }

    @Test
    @DisplayName("요청 시간과 예약 가능한 시간이 일치하지 않음 (시간대 자체가 올바르지 않음) 409 반환")
    void postReservation_fail3() throws Exception {
        //given
        given(reserveService.reserve(anyLong(), any(RequestReservationDto.class), any()))
                .willThrow(new ReservationNotPossibleException(INVALID_RESERVATION_TIME));

        RequestReservationDto request = RequestReservationDto.builder()
                .dateInfo(RequestReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("postReservation/409/invalid",
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("예약할 수 없는 예약일/시간 (요청 자체가 올바르지 않음을 의미)"),
                                        fieldWithPath("dateInfo.startTime").description("예약할 수 없는 예약일/시간 (요청 자체가 올바르지 않음을 의미)"),
                                        fieldWithPath("dateInfo.endTime").description("예약할 수 없는 예약일/시간 (요청 자체가 올바르지 않음을 의미)"),
                                        fieldWithPath("amount").description("비용"),
                                        fieldWithPath("reservationMemo").description("예약자가 남기는 메모")
                                )
                        )
                );
    }

    @Test
    @DisplayName("예약 요청 시 신청한 금액과 실제 결제해야할 금액이 일치하지 않을 경우 409 반환")
    void postReservation_fail4() throws Exception {
        //given
        given(reserveService.reserve(anyLong(), any(RequestReservationDto.class), any()))
                .willThrow(new ReservationNotPossibleException(AMOUNT_DOSE_NOT_MATCH));

        RequestReservationDto request = RequestReservationDto.builder()
                .dateInfo(RequestReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("postReservation/409/money",
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("예약일"),
                                        fieldWithPath("dateInfo.startTime").description("예약 시작 시간"),
                                        fieldWithPath("dateInfo.endTime").description("예약 종료 시간"),
                                        fieldWithPath("amount").description("서버에서 계산한 금액과 일치하지 않음"),
                                        fieldWithPath("reservationMemo").description("예약자가 남기는 메모")
                                )
                        )
                );
    }

    @Test
    @DisplayName("정상 요청의 경우")
    void postReservation_success() throws Exception {
        //given
        RequestReservationDto request = RequestReservationDto.builder()
                .dateInfo(RequestReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        PaymentResponseDto response = PaymentResponseDto.builder()
                .orderName(CONTENT1)
                .paySuccessYn(BEFORE.getValue())
                .orderId(ID)
                .createDate(LocalDate.now().toString())
                .successUrl("/testpage")
                .failUrl("/testpage")
                .customerName(NICKNAME)
                .amount(1000L)
                .customerEmail(EMAIL1)
                .payType(CARD.getValue())
                .build();

        String json = objectMapper.writeValueAsString(request);
        given(reserveService.reserve(anyLong(), any(RequestReservationDto.class), any())).willReturn(response);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isCreated())
                .andDo(document("postReservation/201",
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("예약일"),
                                        fieldWithPath("dateInfo.startTime").description("예약 시작 시간"),
                                        fieldWithPath("dateInfo.endTime").description("예약 종료 시간"),
                                        fieldWithPath("amount").description("서버에서 계산한 금액과 일치하지 않음"),
                                        fieldWithPath("reservationMemo").description("예약자가 남기는 메모")
                                ),
                        responseFields(
                                fieldWithPath("payType").type(STRING).description("결제 형태"),
                                fieldWithPath("amount").type(LONG).description("결제 금액"),
                                fieldWithPath("orderId").type(STRING).description("고유 결제 아이디"),
                                fieldWithPath("orderName").type(STRING).description("결제 항목 이름"),
                                fieldWithPath("customerEmail").type(STRING).description("고객 이메일 주소"),
                                fieldWithPath("customerName").type(STRING).description("고객 이름"),
                                fieldWithPath("successUrl").type(STRING).description("성공 시 이동 url"),
                                fieldWithPath("failUrl").type(STRING).description("실패 시 이동 url"),
                                fieldWithPath("createDate").type(STRING).description("결제 생성일"),
                                fieldWithPath("paySuccessYn").type(STRING).description("결제 완료 여부")
                        )
                        )
                );
    }

    @Test
    @DisplayName("요청한 meeting을 찾지 못할 경우 404 반환")
    void postReservation_fail5() throws Exception {
        //given
        given(reserveService.reserve(anyLong(), any(RequestReservationDto.class), any()))
                .willThrow(new MeetingNotFoundException(DATA_NOT_FOUND));

        RequestReservationDto request = RequestReservationDto.builder()
                .dateInfo(RequestReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isNotFound())
                .andDo(document("postReservation/404",
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("예약일"),
                                        fieldWithPath("dateInfo.startTime").description("시작 시간"),
                                        fieldWithPath("dateInfo.endTime").description("마지막 시간"),
                                        fieldWithPath("amount").description("비용"),
                                        fieldWithPath("reservationMemo").description("예약자가 남기는 메모")
                                )
                        )
                );
    }
}