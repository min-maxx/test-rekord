//package com.vsct.vsc.aftersale;
//
//import com.vsct.vsc.aftersale.extension.GoldenExtension;
//import com.vsct.vsc.aftersale.extension.GoldenHandler;
//import com.vsct.vsc.aftersale.extension.RecordIbatisExtension;
//import com.vsct.vsc.aftersale.extension.RecordLocalDateTimeExtension;
//import com.vsct.vsc.aftersale.extension.RecordRedisTemplateExtension;
//import com.vsct.vsc.aftersale.extension.RecordRedisTemplateExtension.RedisTemplateBeanConfiguration;
//import com.vsct.vsc.aftersale.extension.RecordRestTemplateExtension;
//import com.vsct.vsc.aftersale.extension.RecordRestTemplateExtension.RestTemplatesConfig;
//import com.vsct.vsc.aftersale.extension.RecordUuidExtension;
//import com.vsct.vsc.aftersale.pao.adapters.primaries.PaoRestTemplateFactory;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@ExtendWith(SpringExtension.class)
//@ExtendWith({RecordRedisTemplateExtension.class,
//        RecordIbatisExtension.class,
//        RecordRestTemplateExtension.class,
//        RecordUuidExtension.class,
//        RecordLocalDateTimeExtension.class,
//        GoldenExtension.class})
//@SpringBootTest(classes = VscAfterSaleApplication.class)
//@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
//@TestPropertySource(
//        locations = {
//                "file:../properties/props-vsa/vsc-aftersale/application.properties",
//                "file:../properties/props-vsa/vsc-aftersale/application-development.properties",
//                "file:../properties/props-vsa/vsc-aftersale/application-usine3.properties"}
//        ,
//        properties = {
//                "spring.profiles.active=development, usine3",
//                "spring.main.lazy-initialization=true"
//        }
//)
//@Import(RedisTemplateBeanConfiguration.class)
//public class ContractTest {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(ContractTest.class);
//
//    protected static final String RR = "RR";
//    protected static final String BOOST = "boost";
//    protected static final String RESARAIL_INVENTORY_CODE = "WDI";
//    protected static final String TGV_INVENTORY_CODE = "S3P";
//    protected static final String RESARAIL_DV_NAME = "RESARAIL";
//    protected static final String TGV_DV_NAME = "BOOST";
//
//    @Autowired
//    ApplicationContext applicationContext;
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Autowired
//    PaoRestTemplateFactory paoRestTemplateFactory;
//
//    protected GoldenHandler goldenHandler;
//
//    @BeforeEach
//    void setUp(GoldenHandler goldenHandler, RestTemplatesConfig restTemplatesConfig) {
//        this.goldenHandler = goldenHandler;
//
//
//        if (restTemplatesConfig.getRestTemplates().isEmpty()) {
//            Map<String, List<RestTemplate>> restTemplates = new HashMap<>();
//            List<RestTemplate> paoRestTemplate = new ArrayList<>();
//            paoRestTemplate.add(paoRestTemplateFactory.getDefaultRestTemplate());
//            paoRestTemplateFactory.getRestTemplatesPerService().forEach((s, restTemplate) -> paoRestTemplate.add(restTemplate));
//            restTemplates.put("pao", paoRestTemplate);
//            restTemplatesConfig.handle(restTemplates);
//            System.out.println("restTemplates config = " + restTemplates);
//        }
//
//    }
//
//}
