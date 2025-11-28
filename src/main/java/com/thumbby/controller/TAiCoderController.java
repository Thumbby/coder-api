package com.thumbby.controller;

import com.thumbby.bean.CoderCommonRespondBean;
import com.thumbby.bean.CoderExecuteQuery;
import com.thumbby.config.CoderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/coder")
public class TAiCoderController {

    private static final Logger logger = LoggerFactory.getLogger(TAiCoderController.class);

    @Autowired
    private CoderConfig coderConfig;

    @Deprecated
    @PostMapping("/run")
    public CoderCommonRespondBean run(@RequestBody CoderExecuteQuery req) {
        req.setStream(false);
        return execute(req);
    }

    @Deprecated
    @PostMapping(value = "/streamrun", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRun(@RequestBody CoderExecuteQuery req) {
        req.setStream(true);
        return streamExecute(req);
    }

    @PostMapping("/execute")
    public Object unifiedExecute(@RequestBody CoderExecuteQuery req) {
        if (req.isStream()) {
            return streamExecute(req);
        } else {
            return execute(req);
        }
    }

    private CoderCommonRespondBean execute(CoderExecuteQuery req) {
        String prompt = req.getPrompt();
        Process process = null;
        try {
            process = createAndWriteProcess(prompt);

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("【CLI】:{}", line);
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return CoderCommonRespondBean.fail("CLI exit error, exitCode=" + exitCode, output.toString());
            }
            return CoderCommonRespondBean.success("success", output.toString());
        } catch (IOException | InterruptedException e) {
            return CoderCommonRespondBean.fail("internal server error:" + e.getMessage(), null);
        } finally {
            if (process != null) {
                process.destroy();
                logger.info("CLI destoryed");
            }
        }
    }
    
    /**
     * execute in stream
     */
    private SseEmitter streamExecute(CoderExecuteQuery req) {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            Process process = null;
            try {
                String prompt = req.getPrompt();
                process = createAndWriteProcess(prompt);

                try (InputStream inputStream = process.getInputStream()) {
                    byte[] buffer = new byte[32];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        String chunk = new String(buffer, 0, bytesRead);
                        logger.info("【CLI】:{}", chunk);
                        CoderCommonRespondBean response = CoderCommonRespondBean.success("success", chunk);
                        emitter.send(SseEmitter.event()
                                .data(response)
                                .id(UUID.randomUUID().toString())
                                .name("success"));
                    }
                }
                
                int exitCode = process.waitFor();
                logger.info("CLI exited，exitCode={}", exitCode);
                if (exitCode != 0) {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(CoderCommonRespondBean.fail("CLI exit error, exitCode=" + exitCode, null))
                                .name("error"));
                    } catch (IllegalStateException ignore) {}
                }

                emitter.send(SseEmitter.event()
                        .data(CoderCommonRespondBean.success("complete", ""))
                        .name("complete"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                if (process != null) {
                    process.destroy();
                    logger.info("CLI destroyed");
                }
            }
        }).start();
        return emitter;
    }
    
    /**
     * create the CLI terminal and write input
     */
    private Process createAndWriteProcess(String prompt) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                coderConfig.getNodeDirectory(),
                coderConfig.getCliDirectory()
        );
        pb.directory(new File(coderConfig.getWorkingDirectory()));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // write input to CLI
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(prompt);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            process.destroy();
            throw e;
        }
        
        return process;
    }

}
