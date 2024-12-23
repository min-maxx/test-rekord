package com.vsct.vsc.aftersale.extension;

import org.assertj.core.api.Assertions;
import org.junit.platform.commons.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.backup;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class GoldenHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoldenHandler.class);
    private final boolean updateGolden;
    private final Path goldenFilePath;
    private int expectedHttpStatus = 200;

    public GoldenHandler(boolean updateGolden, Path goldenFilePath) {
        this.updateGolden = updateGolden;
        this.goldenFilePath = goldenFilePath;
        LOGGER.debug("MVC - updateGolden = " + updateGolden);

    }

    public void expectToMatch(MvcResult mvcResult) throws IOException {
        MockHttpServletResponse response = mvcResult.getResponse();
        //mode RECORD → on écrase/créé le fichier golden
        //mode REPLAY → on assert le mvcResult, error si pas de fichier

        if (updateGolden) {
            Preconditions.condition(response.getStatus() == this.expectedHttpStatus, "Response status should be " + expectedHttpStatus + " but is " + response.getStatus() + ". Use GoldenHandler.expectHttpStatus to set up desired status (by default is 200).");
            initGoldenFile();
            writeGolden(response);
        } else {
            checkGoldenFile();
            MockHttpServletResponse golden = readGolden();
            assertThat(mvcResult.getResponse().getStatus()).isEqualTo(golden.getStatus());

            String goldenJson = golden.getContentAsString(StandardCharsets.UTF_8);
            String actualJson = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThatJson(actualJson).isEqualTo(goldenJson);
        }
    }

    private void writeGolden(MockHttpServletResponse response) throws IOException {
        String httpCode = String.valueOf(response.getStatus());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        List<String> lines = List.of(httpCode, body);
        LOGGER.debug("MVC - write Golden: " + lines);
        Files.write(goldenFilePath, lines);
    }

    private void initGoldenFile() throws IOException {
        File file = goldenFilePath.toFile();
        if (file.exists()) {
            Path backup = backup(file);
            LOGGER.debug("MVC - Back up file  = " + backup);
        }
        file.getParentFile().mkdirs();
        file.createNewFile();
        LOGGER.debug("MVC - File created = " + file.toPath());
    }

    private void checkGoldenFile() {

        File goldenFile = goldenFilePath.toFile();
        boolean fileExist = goldenFile.exists();
        LOGGER.debug("MVC - File = " + goldenFilePath + ", exist = " + fileExist);
        if (!updateGolden && !fileExist) {
            Assertions.fail("MVC - Golden File should exist but it not. = " + goldenFilePath + ". " +
                    "Use @Tag(RecordExtensions.RECORD) or @Tag(RecordExtensions.RECORD_GOLDEN_MVC_ONLY) to generate it.");
        }
    }

    private MockHttpServletResponse readGolden() throws IOException {
        List<String> lines = Files.readAllLines(goldenFilePath);
        LOGGER.debug("MVC - read Golden: " + lines);
        int httpCode = Integer.parseInt(lines.get(0));
        String body = String.join("", lines.subList(1, lines.size()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(httpCode);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().print(body);
        return response;
    }

    /**
     * permet de configurer la précondition pour enregistrer un Golden
     * Par défaut c'est 200.
     *
     * @param status
     * @return this
     */
    public GoldenHandler expectHttpStatus(int status) {
        this.expectedHttpStatus = status;
        return this;
    }


}
