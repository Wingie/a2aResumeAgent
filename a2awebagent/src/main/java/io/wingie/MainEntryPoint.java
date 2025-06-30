package io.wingie;

import io.github.vishalmysore.a2a.domain.JsonRpcRequest;
import io.wingie.playwright.PlaywrightTaskController;
import io.github.vishalmysore.a2a.server.A2ATaskController;
import io.github.vishalmysore.common.server.JsonRpcController;
import io.github.vishalmysore.a2a.server.DyanamicTaskContoller;
import com.t4a.predict.PredictionLoader;
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
public class MainEntryPoint extends JsonRpcController {

    private static final Logger log = Logger.getLogger(MainEntryPoint.class.getName());

    @Autowired
    PlaywrightTaskController playwrightTaskController;
    
    @Autowired 
    MCPController customMCPController;
    
    private DyanamicTaskContoller dynamicTaskController;

    @Autowired
    public MainEntryPoint(ApplicationContext applicationContext, MCPController customMCPController) {
        // Don't call super() to avoid default MCPToolsController creation
        // Instead, manually initialize what we need
        PredictionLoader.getInstance(applicationContext);
        this.dynamicTaskController = new DyanamicTaskContoller();
        this.customMCPController = customMCPController;
        
        // Initialize our custom controller (this will use PostgreSQL caching)
        customMCPController.init();
        
        log.info("MainEntryPoint initialized with cached MCPController");
    }

    @GetMapping
    public ModelAndView forwardToIndex() {

        return new ModelAndView("forward:/index.html");
    }

    @PostMapping
    public Object handleRpc(@RequestBody JsonRpcRequest request) {
        log.info(request.toString());
        Object obj = super.handleRpc(request);
        return obj;
    }

    @Override
    public A2ATaskController getTaskController() {
        return playwrightTaskController;
    }
    
    // Override to return our cached controller instead of the default one
    @Override
    public io.github.vishalmysore.mcp.server.MCPToolsController getMCPToolsController() {
        return customMCPController;
    }
}
