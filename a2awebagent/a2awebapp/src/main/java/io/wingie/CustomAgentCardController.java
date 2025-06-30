package io.wingie;

import io.github.vishalmysore.a2a.domain.AgentCard;
import io.github.vishalmysore.a2a.server.RealTimeAgentCardController;
import io.github.vishalmysore.a2a.server.SpringAwareAgentCardController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RealTimeAgentCardController.WELL_KNOWN_PATH)
@Slf4j
public class CustomAgentCardController extends SpringAwareAgentCardController {

    @Autowired
    public CustomAgentCardController(ApplicationContext context) {
        super(context);
    }

    @GetMapping(value = RealTimeAgentCardController.AGENT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentCard> getAgentCardForMyApp() {

        AgentCard card = getCachedAgentCard();
        card.getCapabilities().setStreaming(true);
        
        // Update card with resume information for Wingston Sharon
        card.setName("Wingston Sharon - Web Automation Agent");
        card.setDescription("AI-powered web automation agent by Wingston Sharon. " +
            "Seeking exciting opportunities in enterprise AI and Machine Learning. " +
            "Specializes in web automation, AI research (Red Teaming and Existential Risk)," + 
            "and working with enterprise grade intelligent self learning agent systems at scale." +
            "Connect with me on LinkedIn: https://www.linkedin.com/in/wingstonsharon/");
        
        log.info(card.getUrl());
        return ResponseEntity.ok(card);

    }

}
