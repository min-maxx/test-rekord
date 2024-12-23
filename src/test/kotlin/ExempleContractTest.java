//package com.vsct.vsc.aftersale;
//
//import com.vsct.supervision.logdorak.Logdorak;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MvcResult;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//
//public class ExempleContractTest extends ContractTest {
//
//    @Test
//    void shouldGetOrder() throws Exception {
//        Logdorak.registerContextCorrelationId("ExchangeContractTest"); // for GalileoLocalityService
//        MvcResult mvcResult = mockMvc.perform(get("/vsa/api/order/fr_FR/PLTA/RATGDQ?withAftersaleEligibility=true")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andReturn();
//
//        goldenHandler.expectToMatch(mvcResult);
//
//    }
//
//    @Test
////    @Record
//    void shouldGetOrderWithCache() throws Exception {
//        Logdorak.registerContextCorrelationId("ExchangeContractTest"); // for GalileoLocalityService
//        MvcResult mvcResult = mockMvc.perform(get("/vsa/api/order/fr_FR/PLTA/RATGDQ?withAftersaleEligibility=true&cache=true")
////                        .header(CORRELATION_ID_COOKIE_KEY, "ExchangeContractTest")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andReturn();
//
////        String contentAsString = mvcResult.getResponse().getContentAsString();
////        System.out.println("contentAsString = " + contentAsString);
//        goldenHandler.expectToMatch(mvcResult);
//
//    }
//}
