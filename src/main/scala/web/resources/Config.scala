package web.resources

abstract class Config {
  lazy val closetDataTableName =
    "arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"
  lazy val closetItemTableName =
    "arn:aws:dynamodb:us-east-1:288761768209:table/closet_items"
  lazy val bucketName = "closet-assistant-image-repository"
  lazy val llmApiUrl =
    "https://l3nlilbp8mjs9q-11434.proxy.runpod.net/api/generate"
  lazy val model = "gemma3:27b"
  lazy val prompt = """
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
  "activities": ["church", "dinner", "business events"]
  "description": "A classic, single-breasted wool blazer in a deep navy blue. Versatile enough for the office or a smart casual event."
}

Now, analyze the given image and provide the JSON output."""
}
