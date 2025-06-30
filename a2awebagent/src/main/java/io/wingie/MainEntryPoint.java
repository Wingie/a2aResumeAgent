package io.wingie;

import io.github.vishalmysore.a2a.domain.JsonRpcRequest;
import io.wingie.playwright.PlaywrightTaskController;
import io.github.vishalmysore.a2a.server.A2ATaskController;
import io.github.vishalmysore.common.server.SpringAwareJSONRpcController;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.logging.Logger;

@RestController
@RequestMapping("/")
@Primary
@Component("a2aMainEntryPoint")
public class MainEntryPoint extends SpringAwareJSONRpcController {

    private static final Logger log = Logger.getLogger(MainEntryPoint.class.getName());

    @Autowired
    PlaywrightTaskController playwrightTaskController;
    
    @Autowired 
    MCPController customMCPController;

    @Autowired
    public MainEntryPoint(ApplicationContext applicationContext) {
        super(applicationContext);
        // Replace the default MCPToolsController with our cached version
        setMcpToolsController(customMCPController);
    }

    @GetMapping
    public ModelAndView forwardToIndex() {

        return new ModelAndView("forward:/index.html");
    }

    @PostMapping
    public Object handleRpc(@RequestBody JsonRpcRequest request) {
        log.info(request.toString());
        Object obj =  super.handleRpc(request);

        return obj;

    }

    @Override
    public A2ATaskController getTaskController() {
        return playwrightTaskController;
    }
}
