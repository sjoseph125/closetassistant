package web.resources

abstract class Config {
  lazy val closetDataTableName =
    "arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"
  lazy val closetItemTableName =
    "arn:aws:dynamodb:us-east-1:288761768209:table/closet_items"
  lazy val bucketName = "closet-assistant-image-repository"
  lazy val llmApiUrl =
    "https://eoao8so1ckzinx-11434.proxy.runpod.net/api/generate"
  lazy val weatherApiUrl = "http://api.weatherapi.com/v1/current.json"
  lazy val weatherApiKey = "b34866764af4415fb30224545251107"
  lazy val model = "gemma3:27b"
  lazy val itemMetadataPrompt = """
    Your Role: You are an expert fashion data analyst and stylist. Your task is to analyze images of clothing items and generate a rich, structured metadata profile for each one. This data will power a smart virtual closet and outfit recommendation engine. Accuracy and consistency are paramount.

Your Task:
Based on the provided image of a clothing item, you will generate a single, valid JSON object containing a detailed analysis.

Output Schema and Rules:
Your output MUST be a single, valid JSON object and nothing else. Adhere strictly to the following fields and their allowed values:

1.  "itemName" (string): A short, descriptive name for the item (e.g., "Blue Denim Jacket", "Cashmere V-Neck Sweater").
2.  "category" (string): The main clothing category. Must be one of: "Top", "Bottom", "Dress", "Outerwear", "Shoes", "Accessory".
3.  "subCategory" (string): The specific sub-category. Examples:
     If "category" is "Top": "T-Shirt", "Blouse", "Sweater", "Tank Top", "Button-Up Shirt".
     If "category" is "Bottom": "Jeans", "Trousers", "Skirt", "Shorts", "Leggings".
     If "category" is "Outerwear": "Jacket", "Coat", "Blazer", "Cardigan", "Vest".
     If "category" is "Shoes": "Sneakers", "Boots", "Sandals", "Heels", "Loafers".
4.  "colors" (array of strings): An array of the primary color names present in the item. Use basic color names (e.g., "Black", "White", "Navy Blue", "Red", "Beige"). If there are multiple prominent colors, include them.
5.  "style" (array of strings): An array of occasion/style tags. Choose all that apply from: "Casual", "Business Casual", "Formal", "Sporty", "Loungewear", "Evening", "Trendy".
6.  "season" (array of strings): An array of appropriate seasons. Choose all that apply from: "Spring", "Summer", "Fall", "Winter". You can also use "All-Season".
7.  "warmth" (string): A score from 1 (very light, like a silk tank top) to 10 (very heavy, like a winter parka).
8.  "pattern" (string): The dominant pattern. Must be one of: "Solid", "Striped", "Floral", "Plaid", "Polka Dot", "Graphic", "Animal Print", "Abstract".
9.  "fabric" (string): The type of fabric the item is made out of, such as wool, cotton, leather, etc.
10. "activities" (array of strings): An array of activities the item could be worn at. For example, a suit could be worn to church, high-end restuarant, etc.
11. "description" (string): A brief (1-2 sentence) description of the item, as if for a user.
12. "suggestedPairingColors" (array of strings): Based on color theory, provide an array of color names that would pair well with the item's main colors. Include a mix of safe neutrals (like White, Black, Grey, Beige), analogous colors, and one or two complementary or accent colors for more stylish options. This field is for identifying other items to build an outfit.

---
Example:

Your Output:
{
  "itemName": "Navy Blue Wool Blazer",
  "category": "Outerwear",
  "subCategory": "Blazer",
  "colors": ["Navy Blue"],
  "style": ["Business Casual", "Casual", "Formal"],
  "season": ["Fall", "Winter", "Spring"],
  "warmth": 7,
  "pattern": "Solid",
  "fabric": "Wool",
  "activities": ["church", "dinner", "business events"],
  "description": "A classic, single-breasted wool blazer in a deep navy blue. Versatile enough for the office or a smart casual event.",
  "suggestedPairingColors": ["White", "Black", "Grey", "Beige", "Burgundy", "Olive Green"]
}

Now, analyze the given image and provide the JSON output."""

  lazy val userSearchPrompt =
    """**Your Role:** You are an expert Natural Language Understanding (NLU) system. Your sole purpose is to deconstruct a user's request for an outfit recommendation and convert it into a structured JSON object. You must infer context from the user's language and the provided environmental details.

**Contextual Information:**
**Your Task:**
Analyze the user's request below. Based on the request and the contextual information, generate a single, valid JSON object that captures all relevant parameters for a database query.
* **User Request:** {USER_REQUEST}

**JSON Output Schema and Rules:**
Your output MUST be a single, valid JSON object and nothing else. Do not add explanations. Adhere strictly to the following schema. If a value cannot be reasonably determined from the request, use `null`.

1. "style" (array of strings)
      - discription: "The determined style or occasion. If multiple are mentioned (e.g., for a trip), include all."
      - allowedValues: ["Casual", "Business Casual", "Formal", "Sporty", "Loungewear", "Evening", "Trendy"]
2. "season" (array of strings)
      - discription: "The determined season. Infer from explicit mentions or from dates (e.g., 'December' implies 'Winter')."
      - allowedValues: ["Spring", "Summer", "Fall", "Winter", "All-Season"]
3. "forcedItems" (array of strings)
      - discription: "A list of specific items the user insists on wearing. Extract descriptions like 'my red dress' or 'new leather jacket'."
4. "location" (array of string)
      - discription: "The geolocation of the city, state, or country mentioned for the event or trip. The format should be ["Lattitude", "Longitude"]. Defaults to the ["{LATTITUDE}", "{LONGITUDE}"] ONLY IF NO location is specified in the user's request."

---
**Examples:**

**User Request 1:** "What can I wear for a casual brunch this spring in Miami?"
**Your Output 1:**
{
  "style": ["Casual", "Evening"],
  "season": ["Spring"],
  "forcedItems": null,
  "location": ["25.7617","-80.1918"]
}

**User Request 2:** "It's supposed to be cold and rainy tomorrow in London, what should I wear to work?"
**Your Output 2:**
{
  "style": ["Business Casual"],
  "season": ["Fall", "Winter"],
  "forcedItems": null,
  "location": ["51.5074","-0.1278"]
}

**User Request 3:** "I need help figuring out what to wear with my new black boots for a night out in Takoma Park, Maryland."
**Your Output 3:**
{
  "style": ["Evening", "Trendy"],
  "season": null,
  "forcedItems": ["new black boots"],
  "location": ["38.9778","-77.0147"]
}

**User Request 4:** "I'm packing for a trip to Miami next week. I need outfits for the beach and for some nice dinners."
**Your Output 4:**
{
  "style": ["Sporty", "Evening", "Formal"],
  "season": ["Summer"],
  "forcedItems": null,
  "location": ["25.7617","-80.1918"]
}

---
**Now, process the following user request.**"""
}
