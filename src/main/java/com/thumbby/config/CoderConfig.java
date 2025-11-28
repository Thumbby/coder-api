package com.thumbby.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class CoderConfig {
    @Value("${coder.cli.node-directory}")
    private String nodeDirectory;
    @Value("${coder.cli.cli-directory}")
    private String cliDirectory;
    @Value("${coder.cli.working-directory}")
    private String workingDirectory;
}
