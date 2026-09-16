const express = require("express");

const app = express();

app.use(express.json());

const PORT = process.env.PORT || 3000;

app.get("/", (req, res) => {
  res.send("Krushna AI Server चालू आहे! 🤖");
});

app.post("/chat", async (req, res) => {
  try {
    const message = req.body.message || "";

    if (!message.trim()) {
      return res.json({
        reply: "कृपया प्रश्न विचारा."
      });
    }

    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey) {
      return res.status(500).json({
        reply: "Gemini API key server मध्ये सापडली नाही."
      });
    }

    const response = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": apiKey
        },
        body: JSON.stringify({
          contents: [
            {
              parts: [
                {
                  text:
                    "You are Krushna AI, a helpful AI assistant. " +
                    "Answer clearly and naturally. " +
                    "If the user speaks Marathi, reply in Marathi. " +
                    "User message: " +
                    message
                }
              ]
            }
          ]
        })
      }
    );

    const data = await response.json();

    if (!response.ok) {
      console.error("Gemini HTTP Error:", response.status);
      console.error("Gemini Error Data:", data);

      return res.status(500).json({
        reply:
          "Gemini Error " +
          response.status +
          ": " +
          (data?.error?.message || "Unknown error")
      });
    }

    const reply =
      data?.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!reply) {
      return res.json({
        reply: "AI कडून रिकामे उत्तर मिळाले."
      });
    }

    res.json({
      reply: reply
    });

  } catch (error) {
    console.error("Server error:", error);

    res.status(500).json({
      reply: "Online AI मध्ये connection error आला."
    });
  }
});

app.listen(PORT, () => {
  console.log(
    "Krushna AI Server started on port " + PORT
  );
});
