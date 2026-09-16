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
      return res.json({ reply: "कृपया प्रश्न विचारा." });
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
          contents: [{
            parts: [{
              text:
                "You are Krushna AI. Reply clearly. " +
                "If user speaks Marathi, reply in Marathi.\n\n" +
                "User: " + message
            }]
          }]
        })
      }
    );

    const data = await response.json();

    if (!response.ok) {
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

    res.json({
      reply: reply || "AI कडून उत्तर मिळाले नाही."
    });

  } catch (error) {
    console.error(error);

    res.status(500).json({
      reply: "Online AI connection error आला."
    });
  }
});

app.post("/code", async (req, res) => {
  try {
    const command = req.body.command || "";

    if (!command.trim()) {
      return res.json({
        success: false,
        message: "Code command द्या."
      });
    }

    /*
     * पुढच्या टप्प्यात येथे controlled
     * code-generation workflow जोडू.
     *
     * GitHub token APK मध्ये ठेवायचा नाही.
     */

    res.json({
      success: true,
      message:
        "Code command मिळाली: " + command
    });

  } catch (error) {
    console.error(error);

    res.status(500).json({
      success: false,
      message: "Code system error."
    });
  }
});

app.listen(PORT, () => {
  console.log(
    "Krushna AI Server started on port " + PORT
  );
});
