package io.wingie;

import io.github.vishalmysore.a2a.domain.AgentCard;
import io.github.vishalmysore.a2a.server.RealTimeAgentCardController;
import io.github.vishalmysore.a2a.server.SpringAwareAgentCardController;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.logging.Logger;

@RestController
@RequestMapping(RealTimeAgentCardController.WELL_KNOWN_PATH)
public class AgentCardController extends SpringAwareAgentCardController {

    private static final Logger log = Logger.getLogger(AgentCardController.class.getName());

    @Autowired
    public AgentCardController(ApplicationContext context) {
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
            "Specializes in web automation, travel research, and intelligent agent systems. " +
            "Connect with me on LinkedIn: https://www.linkedin.com/in/wingstonsharon/");
        
        log.info(card.getUrl());
        return ResponseEntity.ok(card);

    }

}
