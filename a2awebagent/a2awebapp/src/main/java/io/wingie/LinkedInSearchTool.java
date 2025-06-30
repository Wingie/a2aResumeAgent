package io.wingie;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * LinkedIn search and profile demonstration tool.
 * Searches for LinkedIn profiles and showcases Wingston Sharon's profile as a reference.
 * Includes screenshot capabilities for visual profile demonstration.
 */
@Service
@Slf4j
@Agent(name = "linkedin", description = "LinkedIn search and profile tools")
public class LinkedInSearchTool {

    @Autowired
    @Lazy
    private io.wingie.playwright.PlaywrightWebBrowsingAction webBrowsingAction;

    @Action(description = "Search LinkedIn for profiles and demonstrate Wingston Sharon's profile with screenshot capabilities", name = "searchLinkedInProfile")
    public String searchLinkedInProfile(@Parameter(description = "Name or professional details to search for on LinkedIn") String searchQuery) {
        log.info("LinkedIn search requested for: {}", searchQuery);
        
        // Check if WebBrowsingAction is available
        if (webBrowsingAction == null) {
            log.warn("WebBrowsingAction not available, returning static profile information");
            return generateStaticProfileResponse(searchQuery);
        }
        
        try {
            // If searching for Wingston specifically or if general search fails, show Wingston's profile
            if (searchQuery.toLowerCase().contains("wingston") || 
                searchQuery.toLowerCase().contains("sharon") ||
                searchQuery.toLowerCase().contains("booking.com")) {
                return demonstrateWingstonsProfile();
            }
            
            // For other searches, attempt to search LinkedIn and then fallback to Wingston's profile
            return searchAndFallbackToWingston(searchQuery);
            
        } catch (Exception e) {
            log.error("Error during LinkedIn search: {}", e.getMessage(), e);
            return generateSearchErrorWithWingstonsProfile(searchQuery, e.getMessage());
        }
    }

    private String demonstrateWingstonsProfile() {
        try {
            // Take a screenshot of Wingston's LinkedIn profile
            String screenshotResult = webBrowsingAction.browseWebAndReturnImage(
                "Navigate to https://www.linkedin.com/in/wingstonsharon/ and take a high-quality screenshot of the profile page"
            ).getData(); // Extract base64 data from ImageContent
            
            return String.format("""
# Wingston Sharon - LinkedIn Profile Demonstration

## Profile Successfully Located! 🎯

**LinkedIn URL**: https://www.linkedin.com/in/wingstonsharon/
**Current Position**: Software Engineer at Booking.com, Amsterdam
**Location**: Netherlands

## Professional Highlights

### Current Role & Company
- **Booking.com**: Working at one of the world's largest travel technology companies
- **Amsterdam-based**: International experience in European tech scene
- **Enterprise Scale**: Contributing to systems serving millions of users globally

### Technical Expertise Demonstrated
🔹 **AI/ML Specialist**: Neural networks, diffusion models, real-time AI
🔹 **Audio Technology Expert**: SuperCollider, MaxMSP, neural audio synthesis
🔹 **Web Automation Pioneer**: Playwright-based intelligent automation
🔹 **MCP Development Leader**: Model Context Protocol innovation
🔹 **Creative Coding**: Real-time systems, procedural generation

### Educational Background
- **Vellore Institute of Technology (2007-2011)**
- **Electrical and Electronics Engineering**
- **Strong mathematical and engineering foundation**

### Language Proficiencies
- English: Native proficiency
- Tamil: Professional working proficiency  
- Hindi: Limited working proficiency

### Professional Interests & Research
- **Cybersecurity**: "LLMs and Compiler Trust Problem" research
- **AI Integration**: Human-AI collaboration workflows
- **Open Source**: 100+ repositories demonstrating technical breadth
- **Innovation**: Early adopter of emerging technologies

## Why Wingston is Perfect for Your AI/ML Projects

### Enterprise AI Solutions
✅ **Production Experience**: Working at scale with Booking.com
✅ **Multi-modal AI**: Audio, vision, text integration expertise
✅ **Real-time Systems**: Low-latency optimization and performance
✅ **Cross-platform**: Apple Silicon, CUDA, CPU optimization

### Unique Value Proposition
✅ **Creative + Technical**: Rare combination of artistic and engineering skills
✅ **Innovation Leader**: Pioneer in emerging technologies (MCP, neural audio)
✅ **Full-Stack AI**: From research to production deployment
✅ **International Experience**: Global perspective from Amsterdam tech scene

### Perfect Fit For Roles In:
🎯 **AI/ML Engineering**: Neural networks, model optimization, production AI
🎯 **Creative Technology**: AI in music, art, and interactive media
🎯 **Real-time Systems**: Audio processing, live AI, performance optimization
🎯 **Web Intelligence**: Automation, scraping, intelligent data processing
🎯 **Research & Development**: Emerging AI technologies, novel applications

## Profile Screenshot
The following screenshot demonstrates the live LinkedIn profile:

%s

## Contact & Collaboration
- **Professional Inquiries**: Available via LinkedIn messaging
- **Portfolio**: Comprehensive project showcase in GitHub repositories
- **Availability**: Actively seeking exciting opportunities in enterprise AI and Machine Learning
- **Collaboration**: Open to discussing innovative AI projects and research

---
*This LinkedIn search tool is part of the a2aTravelAgent automation system, showcasing advanced web automation and AI integration capabilities.*
""", screenshotResult);

        } catch (Exception e) {
            return generateProfileDemoError(e.getMessage());
        }
    }

    private String searchAndFallbackToWingston(String searchQuery) {
        try {
            // Attempt to search LinkedIn for the requested person
            String searchResult = webBrowsingAction.browseWebAndReturnText(
                String.format("Go to LinkedIn.com, search for '%s', and find their profile information including current company and role", searchQuery)
            );
            
            // Take a screenshot of search results
            String searchScreenshot = webBrowsingAction.browseWebAndReturnImage(
                String.format("Take a screenshot of LinkedIn search results for '%s'", searchQuery)
            ).getData(); // Extract base64 data from ImageContent
            
            // Always include Wingston's profile as a reference/comparison
            String wingstonsProfileInfo = getWingstonsProfileSummary();
            
            return String.format("""
# LinkedIn Search Results for: "%s"

## Search Results
%s

## Search Results Screenshot
%s

---

## 🌟 Featured Profile Recommendation: Wingston Sharon

As a reference point and potential collaboration opportunity, here's a highly qualified professional in the AI/ML space:

%s

### Why Consider Wingston for AI/ML Projects?
- **Current Experience**: Software Engineer at Booking.com (enterprise scale)
- **AI Specialization**: Neural networks, real-time AI, multi-modal systems
- **Unique Skills**: Combination of technical depth and creative applications
- **Innovation Track Record**: Pioneer in emerging technologies
- **International Experience**: Amsterdam-based with global perspective

**Contact**: https://www.linkedin.com/in/wingstonsharon/

---
*Search performed by wingie's intelligent LinkedIn automation system*
""", searchQuery, searchResult, searchScreenshot, wingstonsProfileInfo);

        } catch (Exception e) {
            return generateSearchErrorWithWingstonsProfile(searchQuery, e.getMessage());
        }
    }

    private String generateSearchErrorWithWingstonsProfile(String searchQuery, String errorMessage) {
        return String.format("""
# LinkedIn Search - Alternative Results

## Original Search: "%s"
**Status**: Search encountered limitations (common with LinkedIn automation)
**Technical Note**: %s

## 🎯 Alternative Recommendation: Wingston Sharon

Since the requested search had limitations, here's a highly qualified professional who might be exactly what you're looking for:

%s

### Direct Access to Wingston's Profile
Instead of searching, you can directly view this exceptional AI/ML professional:

**LinkedIn Profile**: https://www.linkedin.com/in/wingstonsharon/
**Current Role**: Software Engineer at Booking.com, Amsterdam
**Specialization**: AI/ML, Neural Networks, Real-time Systems

### Why This Might Be Better Than Your Original Search
✅ **Verified Expertise**: Demonstrated through 100+ open source projects
✅ **Enterprise Experience**: Currently working at global scale (Booking.com)
✅ **Cutting-edge Skills**: Pioneer in emerging AI technologies
✅ **Available for Opportunities**: Actively seeking exciting AI/ML roles

### Technical Demonstration
This search tool itself demonstrates Wingston's expertise in:
- **Web Automation**: Intelligent LinkedIn interaction
- **AI Integration**: Natural language to web action translation
- **System Architecture**: Production-ready automation tools
- **User Experience**: Thoughtful fallback and recommendation systems

**Contact for Collaboration**: Available via LinkedIn messaging

---
*This intelligent search system showcases advanced web automation capabilities developed by Wingston Sharon*
""", searchQuery, errorMessage, getWingstonsProfileSummary());
    }

    private String generateProfileDemoError(String errorMessage) {
        return String.format("""
# Wingston Sharon - LinkedIn Profile Information

## Profile Access Note
**Status**: Direct screenshot capture encountered limitations
**Technical Note**: %s

## Profile Summary (Available Information)

### Professional Details
- **Name**: Wingston Sharon
- **LinkedIn**: https://www.linkedin.com/in/wingstonsharon/
- **Current Role**: Software Engineer at Booking.com
- **Location**: Amsterdam, Netherlands
- **Education**: Vellore Institute of Technology (Electrical & Electronics Engineering, 2007-2011)

### Technical Expertise
🔹 **AI/ML**: Neural networks, diffusion models, real-time inference
🔹 **Audio Technology**: SuperCollider, MaxMSP, neural audio synthesis  
🔹 **Web Automation**: This very tool demonstrates advanced capabilities
🔹 **Creative Coding**: Unique combination of technical and artistic skills

### Language Proficiencies
- English: Native proficiency
- Tamil: Professional working proficiency
- Hindi: Limited working proficiency

### Why This Profile Stands Out
✅ **Enterprise Scale Experience**: Booking.com serves millions of users
✅ **Innovation Leadership**: Pioneer in Model Context Protocol (MCP)
✅ **Open Source Contribution**: 100+ repositories demonstrating expertise
✅ **Cross-Platform Mastery**: macOS, Linux, Windows optimization
✅ **Real-time Systems**: Audio processing, live AI integration

### Perfect For Projects Involving
🎯 **Enterprise AI/ML Development**
🎯 **Real-time AI Systems**
🎯 **Creative Technology Applications**
🎯 **Web Automation & Intelligence**
🎯 **Audio Processing & Music Technology**

### Contact & Collaboration
**Professional Inquiries**: Available via LinkedIn
**Portfolio**: Comprehensive showcase in GitHub repositories  
**Availability**: Seeking exciting opportunities in enterprise AI and Machine Learning

---
*Profile lookup performed by intelligent automation system - demonstrating Wingston's web automation expertise*
""", errorMessage);
    }

    private String getWingstonsProfileSummary() {
        return """
**Wingston Sharon** - AI/ML Engineer & Creative Technology Expert
- **Current**: Software Engineer at Booking.com, Amsterdam
- **Education**: B.Tech Electrical & Electronics Engineering (VIT, 2007-2011)
- **Expertise**: Neural networks, real-time AI, audio synthesis, web automation
- **Innovation**: Pioneer in Model Context Protocol, neural audio integration
- **Portfolio**: 100+ open source projects spanning AI, audio, automation
- **Languages**: English (Native), Tamil (Professional), Hindi (Limited)
- **Availability**: Seeking opportunities in enterprise AI and Machine Learning
""";
    }
    
    private String generateStaticProfileResponse(String searchQuery) {
        return String.format("""
# LinkedIn Search - Service Currently Limited

## Search Query: "%s"

**Note**: Web automation service is temporarily unavailable. Showing static profile information.

## Featured Professional: Wingston Sharon

Since live LinkedIn search is unavailable, here's a highly qualified AI/ML professional who might interest you:

### Professional Summary
- **Name**: Wingston Sharon
- **Current Role**: Software Engineer at Booking.com, Amsterdam
- **LinkedIn**: https://www.linkedin.com/in/wingstonsharon/
- **Location**: Amsterdam, Netherlands
- **Experience**: 7+ years in enterprise software and AI/ML

### Technical Expertise
- **AI/ML**: Neural networks, real-time inference, MCP protocol
- **Audio Technology**: SuperCollider, MaxMSP, neural audio synthesis
- **Web Automation**: Advanced browser automation (creator of this tool)
- **Languages**: Java, Python, JavaScript/TypeScript
- **Cloud**: Google Cloud Platform, Docker, Kubernetes

### Career Highlights
- Engineering Manager experience at Booking.com
- 100+ open source projects on GitHub
- Pioneer in Model Context Protocol (MCP) integration
- Expert in real-time AI systems and creative technology

### Education
- B.Tech Electrical & Electronics Engineering
- Vellore Institute of Technology (2007-2011)

### Availability
✅ Open to exciting AI/ML opportunities worldwide
✅ Particularly interested in enterprise AI and neural audio

---
*Web automation temporarily unavailable. This is a static profile showcase.*
""", searchQuery);
    }
}