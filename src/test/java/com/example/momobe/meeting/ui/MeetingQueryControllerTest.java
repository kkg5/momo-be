package com.example.momobe.meeting.ui;

import com.example.momobe.common.config.SecurityTestConfig;
import com.example.momobe.common.resolver.JwtArgumentResolver;
import com.example.momobe.meeting.dao.MeetingQueryRepository;
import com.example.momobe.meeting.domain.enums.DatePolicy;
import com.example.momobe.meeting.dto.MeetingResponseDto;
import com.example.momobe.meeting.enums.MeetingConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.example.momobe.common.config.ApiDocumentUtils.getDocumentRequest;
import static com.example.momobe.common.config.ApiDocumentUtils.getDocumentResponse;
import static com.example.momobe.common.enums.PageConstants.*;
import static com.example.momobe.common.enums.TestConstants.*;
import static com.example.momobe.meeting.domain.enums.Category.MENTORING;
import static com.example.momobe.meeting.domain.enums.MeetingState.OPEN;
import static com.example.momobe.meeting.domain.enums.PricePolicy.HOUR;
import static com.example.momobe.meeting.enums.MeetingConstant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingQueryController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityTestConfig.class)
@AutoConfigureRestDocs
class MeetingQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtArgumentResolver jwtArgumentResolver;
    @MockBean
    private MeetingQueryRepository meetingQueryRepository;

    @Test
    public void meetingQuery() throws Exception {
        // given
        MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
                ID1, MENTORING, ID1, NICKNAME, TISTORY_URL, TITLE1, CONTENT1, ADDRESS1, OPEN, DatePolicy.FREE, 1000L, NOTICE
        );
        PageRequest pageRequest = PageRequest.of(PAGE - 1, SIZE);

        given(meetingQueryRepository.findAll(eq(TITLE1), eq(MENTORING), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(meetingResponseDto), pageRequest, 1L));

        // when
        ResultActions actions = mockMvc.perform(
                get("/meetings")
                        .param("keyword", TITLE1)
                        .param("category", String.valueOf(MENTORING))
                        .param("page", String.valueOf(PAGE))
                        .param("size", String.valueOf(SIZE))
        );

        // then
        actions.andExpect(status().isOk())
                .andDo(document("meeting/query",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestParameters(
                                parameterWithName("keyword").description("검색어"),
                                parameterWithName("category").description("카테고리"),
                                PWN_PAGE, PWN_SIZE
                        ),
                        responseFields(
                                FWP_CONTENT, FWP_CONTENT_MEETING_ID, FWP_CONTENT_CATEGORY,
                                FWP_CONTENT_HOST, FWP_CONTENT_HOST_USER_ID, FWP_CONTENT_HOST_NICKNAME, FWP_CONTENT_HOST_IMAGE_URL,
                                FWP_CONTENT_TITLE, FWP_CONTENT_CONTENT, FWP_CONTENT_ADDRESS, FWP_CONTENT_MEETING_STATE,
                                FWP_CONTENT_IS_OPEN, FWP_CONTENT_DATE_POLICY, FWP_CONTENT_PRICE, FWP_CONTENT_NOTICE,
                                FWP_PAGE_INFO, FWP_PAGE, FWP_SIZE, FWP_TOTAL_ELEMENTS, FWP_TOTAL_PAGES
                        )

                ));
    }

}