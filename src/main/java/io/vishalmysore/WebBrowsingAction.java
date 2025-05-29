package io.vishalmysore;

import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.detect.ActionCallback;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;


import java.io.IOException;

@Service
@Log
@Agent(groupName = "web browsing", groupDescription = "actions related to web browsing and searching and validation of web pages ")
public class WebBrowsingAction {
    PlaywrightScriptProcessor script;

    public WebBrowsingAction() {

        script = new PlaywrightScriptProcessor();
    }

    private ActionCallback callback;
    private AIProcessor processor;

    @Action(description = "perform actions on the web with selenium and return text")
    public String browseWebAndReturnText(String webBrowsingSteps) throws IOException {
        CustomScriptResult result = new CustomScriptResult();
        PWScreenShotAndTextCallback seleniumCallBack = new PWScreenShotAndTextCallback(result, processor);
        if (processor != null) {
            try {
                StringBuffer seperatedWebBrowsingSteps = new StringBuffer(processor.query("Separate the web browsing steps into individual steps  just give me steps without any additional text or bracket. MOST IMP - 1) make sure each step can be processed by Playwright browse, 2) urls should always start with http or https , 3) Do not give steps such as 'open the browser' as i am using headless browser {" + webBrowsingSteps + "}"));
                //you can create your own selenium processor which implements SeleniumProcessor
                //SeleniumScriptProcessor script = new SeleniumScriptProcessor(new MyOwnSeleniumScriptProcessor());

                script.process(seperatedWebBrowsingSteps, seleniumCallBack);
                return result.getLastData();
            } catch (AIProcessingException e) {
                throw new RuntimeException(e);
            }


        }
        return "processing issues";// Process the file

    }

    @Action(description = "perform actions on the web with selenium and return image")
    public String browseWebAndReturnImage(String webBrowsingSteps) throws IOException {
        CustomScriptResult result = new CustomScriptResult();
        PWScreenShotAndTextCallback seleniumCallBack = new PWScreenShotAndTextCallback(result, processor);
        if (processor != null) {
            try {
                StringBuffer seperatedWebBrowsingSteps = new StringBuffer(processor.query("Separate the web browsing steps into individual steps  just give me steps without any additional text or bracket. MOST IMP - 1) make sure each step can be processed by Playwright browse, 2) urls should always start with http or https , 3) Do not give steps such as 'open the browser' as i am using headless browser {" + webBrowsingSteps + "}"));

                script.process(seperatedWebBrowsingSteps, seleniumCallBack);
                return result.getLastScreenshotAsBase64();
            } catch (AIProcessingException e) {
                throw new RuntimeException(e);
            }


        }
        return "processing issues";// Process the file

    }
}
