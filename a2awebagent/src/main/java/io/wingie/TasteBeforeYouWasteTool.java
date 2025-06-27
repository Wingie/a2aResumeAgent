package io.wingie;

import com.t4a.annotations.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Taste Before You Waste - Food Safety and Consumption Guidance Tool
 * This tool searches tastebeforeyouwaste.org for information about food safety,
 * expiration dates, and whether food is still safe to consume.
 */
@Component
public class TasteBeforeYouWasteTool {

    @Autowired
    private WebBrowsingAction webBrowsingAction;

    @Action(description = "Search tastebeforeyouwaste.org for food safety information and consumption guidance")
    public String askTasteBeforeYouWaste(String foodQuestion) throws java.io.IOException {
        try {
            // First, try to search the site for the specific food item or question
            String searchResult = performFoodSafetySearch(foodQuestion);
            
            // If we get a good result, return it with additional context
            return formatFoodSafetyResponse(foodQuestion, searchResult);
            
        } catch (Exception e) {
            return generateFoodSafetyError(foodQuestion, e.getMessage());
        }
    }

    private String performFoodSafetySearch(String foodQuestion) throws java.io.IOException {
        try {
            // Navigate to tastebeforeyouwaste.org and search for the food item
            String searchQuery = extractFoodItemFromQuestion(foodQuestion);
            
            String searchInstruction = String.format("""
                Navigate to https://tastebeforeyouwaste.org and search for information about '%s'. 
                Look for specific guidance about:
                - Whether the food is safe to eat
                - How to tell if it's gone bad
                - Storage recommendations
                - Expiration date guidance
                - Signs of spoilage to watch for
                Extract all relevant food safety information found.
                """, searchQuery);
            
            return webBrowsingAction.browseWebAndReturnText(searchInstruction);
            
        } catch (Exception e) {
            // Fallback: try to get general information from the homepage
            try {
                return webBrowsingAction.browseWebAndReturnText(
                    "Navigate to https://tastebeforeyouwaste.org and get general food safety information and guidance"
                );
            } catch (java.io.IOException fallbackException) {
                return "Unable to access tastebeforeyouwaste.org: " + fallbackException.getMessage();
            }
        }
    }

    private String extractFoodItemFromQuestion(String question) {
        // Simple extraction of likely food items from the question
        String cleanQuestion = question.toLowerCase();
        
        // Common food safety question patterns
        if (cleanQuestion.contains("milk")) return "milk";
        if (cleanQuestion.contains("bread")) return "bread";
        if (cleanQuestion.contains("cheese")) return "cheese";
        if (cleanQuestion.contains("meat")) return "meat";
        if (cleanQuestion.contains("chicken")) return "chicken";
        if (cleanQuestion.contains("beef")) return "beef";
        if (cleanQuestion.contains("pork")) return "pork";
        if (cleanQuestion.contains("fish")) return "fish";
        if (cleanQuestion.contains("eggs")) return "eggs";
        if (cleanQuestion.contains("vegetables")) return "vegetables";
        if (cleanQuestion.contains("fruits")) return "fruits";
        if (cleanQuestion.contains("leftovers")) return "leftovers";
        if (cleanQuestion.contains("yogurt")) return "yogurt";
        if (cleanQuestion.contains("butter")) return "butter";
        if (cleanQuestion.contains("pasta")) return "pasta";
        if (cleanQuestion.contains("rice")) return "rice";
        if (cleanQuestion.contains("tomatoes")) return "tomatoes";
        if (cleanQuestion.contains("lettuce")) return "lettuce";
        if (cleanQuestion.contains("bananas")) return "bananas";
        if (cleanQuestion.contains("apples")) return "apples";
        if (cleanQuestion.contains("potatoes")) return "potatoes";
        
        // Return the original question if no specific food item detected
        return question;
    }

    private String formatFoodSafetyResponse(String originalQuestion, String searchResult) {
        return String.format("""
# Taste Before You Waste - Food Safety Guidance 🥗

## Your Question: "%s"

## Food Safety Information from tastebeforeyouwaste.org:

%s

---

## 🔍 About Taste Before You Waste

**Website**: https://tastebeforeyouwaste.org
**Mission**: Helping reduce food waste by providing reliable information about food safety and consumption

### Key Food Safety Principles:
✅ **Use Your Senses**: Look, smell, and taste (when safe) to assess food quality
✅ **Understand Dates**: "Best by" vs "Use by" vs "Sell by" dates have different meanings
✅ **Proper Storage**: Correct storage can extend food life significantly
✅ **Know the Signs**: Learn specific spoilage indicators for different foods
✅ **When in Doubt**: Some foods are safer to discard than risk illness

### General Guidelines:
🍞 **Dry Goods**: Often safe well past printed dates if stored properly
🥛 **Dairy**: Trust your senses - smell and texture changes are key indicators
🥩 **Proteins**: More strict timelines - follow temperature and time guidelines
🥬 **Produce**: Visual inspection and texture are primary indicators
🍝 **Leftovers**: Generally 3-4 days in refrigerator, sooner for high-risk items

### Emergency Food Safety Resources:
- **USDA Food Safety Hotline**: 1-888-MPHotline (1-888-674-6854)
- **FDA Food Code**: Official food safety guidelines
- **Local Health Department**: For specific regional guidance

---

## 💡 Pro Tips from Food Safety Experts:

### Temperature Guidelines:
- **Refrigerator**: Keep at 40°F (4°C) or below
- **Freezer**: Keep at 0°F (-18°C) or below
- **Danger Zone**: 40-140°F (4-60°C) - bacteria multiply rapidly

### Storage Best Practices:
- **First In, First Out (FIFO)**: Use older items before newer ones
- **Proper Containers**: Airtight storage prevents contamination
- **Separate Raw/Cooked**: Prevent cross-contamination
- **Label Everything**: Date when opened/prepared

### Red Flags - Discard Immediately:
⚠️ **Unusual odors** (especially sour, rotten, or chemical smells)
⚠️ **Visible mold** (except on hard cheeses where it can be cut off)
⚠️ **Slimy texture** on proteins or vegetables
⚠️ **Off colors** or significant color changes
⚠️ **Swollen containers** (cans, jars, packages)
⚠️ **Past use-by dates** on high-risk items (meat, dairy, prepared foods)

---

*This food safety information is provided by the a2aTravelAgent automation system, demonstrating intelligent web research capabilities for practical everyday questions.*

**Disclaimer**: This information is for general guidance only. When in doubt about food safety, consult professional resources or discard questionable items. Food poisoning can be serious.
""", originalQuestion, searchResult);
    }

    private String generateFoodSafetyError(String question, String errorMessage) {
        return String.format("""
# Taste Before You Waste - Food Safety Guidance 🥗

## Your Question: "%s"

## Search Status
**Note**: Direct website search encountered limitations
**Technical Details**: %s

## General Food Safety Guidelines

Since we couldn't access specific information from tastebeforeyouwaste.org, here are general food safety principles:

### 🔍 The "Taste Before You Waste" Philosophy

**Website**: https://tastebeforeyouwaste.org
**Core Principle**: Use your senses to make informed decisions about food safety

### Universal Food Safety Rules:

#### 1. **Trust Your Senses**
👁️ **Look**: Check for unusual colors, mold, or texture changes
👃 **Smell**: Off odors are often the first sign of spoilage
👅 **Taste**: When safe to do so, a small taste can confirm quality

#### 2. **Understand Date Labels**
- **"Best By"**: Quality may decline but food is often still safe
- **"Use By"**: Recommended for best quality and safety
- **"Sell By"**: For store inventory, not consumer safety
- **"Freeze By"**: Latest date to freeze for best quality

#### 3. **High-Risk vs Low-Risk Foods**

**Higher Risk (Be More Cautious):**
🥩 Raw meat, poultry, seafood
🥛 Dairy products (milk, soft cheeses)
🥗 Pre-cut fruits and vegetables
🍝 Cooked leftovers
🥚 Eggs and egg-based products

**Lower Risk (Often Safe Past Dates):**
🍞 Bread and baked goods
🥫 Canned goods (undamaged cans)
🍝 Dry pasta and grains
🧂 Salt, sugar, honey
🥜 Nuts and dried fruits

#### 4. **Storage Temperature Guidelines**
- **Refrigerator**: 40°F (4°C) or below
- **Freezer**: 0°F (-18°C) or below
- **Room Temperature**: Limit time in 40-140°F danger zone

#### 5. **When to Discard Immediately**
❌ **Visible mold** (except hard cheeses - cut 1 inch around mold)
❌ **Slimy or sticky texture** on proteins
❌ **Sour or rotten odors**
❌ **Swollen packaging** (cans, jars)
❌ **Significant color changes**

### 📞 Emergency Resources:
- **USDA Meat & Poultry Hotline**: 1-888-MPHotline
- **FDA Food Information Line**: 1-888-SAFEFOOD
- **Poison Control**: 1-800-222-1222

### 💡 Money-Saving Tips:
✅ **Plan meals** to use ingredients before spoilage
✅ **Store properly** to extend shelf life
✅ **Freeze extras** before they go bad
✅ **Learn preservation** techniques (pickling, dehydrating)
✅ **Compost safely** when food is beyond consumption

---

**For Specific Questions**: Visit https://tastebeforeyouwaste.org directly for detailed food-specific guidance.

**Disclaimer**: When in doubt, prioritize safety over saving money. Food poisoning can have serious health consequences.

*This general guidance is provided by the a2aTravelAgent intelligent automation system.*
""", question, errorMessage);
    }

    @Action(description = "Get screenshot of tastebeforeyouwaste.org homepage with visual food safety guide")
    public String getTasteBeforeYouWasteScreenshot() throws java.io.IOException {
        try {
            return webBrowsingAction.browseWebAndReturnImage(
                "Navigate to https://tastebeforeyouwaste.org and take a high-quality screenshot of the homepage showing food safety guidance and visual guides"
            );
        } catch (Exception e) {
            return String.format("""
# Taste Before You Waste - Website Screenshot

**Status**: Screenshot capture encountered limitations
**Details**: %s

**Alternative**: Visit https://tastebeforeyouwaste.org directly to view:
- Visual food safety guides
- Interactive food database
- Storage recommendations
- Expiration date guidance

*Screenshot attempt performed by a2aTravelAgent web automation system*
""", e.getMessage());
        }
    }
}