package io.wingie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom agent card controller - disabled by default
 * The a2ajava dependency has been removed, so this provides a simple replacement
 */
@RestController
@RequestMapping("/v1/agent")
@Slf4j
@ConditionalOnProperty(name = "app.agent-card.enabled", havingValue = "true", matchIfMissing = false)
public class CustomAgentCardController {

    public CustomAgentCardController() {
        // Simple constructor - no dependencies needed
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAgentCardForMyApp() {
        
        Map<String, Object> agentInfo = new HashMap<>();
        agentInfo.put("name", "Wingston Sharon - Web Automation Agent");
        agentInfo.put("description", "AI-powered web automation agent by Wingston Sharon. " +
            "Seeking exciting opportunities in enterprise AI and Machine Learning. " +
            "Specializes in web automation, AI research (Red Teaming and Existential Risk), " + 
            "and working with enterprise grade intelligent self learning agent systems at scale. " +
            "Connect with me on LinkedIn: https://www.linkedin.com/in/wingstonsharon/");
        agentInfo.put("version", "0.0.1");
        agentInfo.put("capabilities", Map.of("streaming", true, "web_automation", true));
        
        log.info("Agent card requested: {}", agentInfo.get("name"));
        return ResponseEntity.ok(agentInfo);
    }
}
