const express = require("express");

const app = express();
app.use(express.json({ limit: "1mb" }));

const PORT = process.env.PORT || 3000;
const MODEL = process.env.GEMINI_MODEL || "gemini-3.8-flash";

app.get("/", (req, res) => {
  res.send("Krushna AI Server चालू आहे! 🤖");
});

app.post("/chat", async (req, res) => {
  try {
    const message = req.body?.message;

    if (typeof message !== "string" || !message.trim()) {
      return res.status(400).json({
        reply: "कृपया तुमचा प्रश्न विचारा."
      });
    }

    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey) {
      return res.status(500).json({
        reply: "Render मध्ये GEMINI_API_KEY सापडली नाही."
      });
    }

    let response;
    let data;

    for (let attempt = 1; attempt <= 3; attempt++) {
      response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": apiKey
          },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{
                text:
                  "You are Krushna AI, a helpful assistant. " +
                  "Reply in the user's language. " +
                  "If the user speaks Marathi, reply in Marathi. " +
                  "Be clear, friendly and accurate."
              }]
            },
            contents: [{
              role: "user",
              parts: [{ text: message.trim() }]
            }]
          }),
          signal: AbortSignal.timeout(30000)
        }
      );

      data = await response.json();

      if (response.ok) break;

      console.error("Gemini error:", response.status, data);

      if (![429, 500, 502, 503, 504].includes(response.status)) {
        break;
      }

      if (attempt < 3) {
        await new Promise(resolve =>
          setTimeout(resolve, attempt * 2000)
        );
      }
    }

    if (!response.ok) {
      return res.status(502).json({
        reply:
          "Gemini Error " + response.status +
          ": " + (data?.error?.message || "पुन्हा प्रयत्न करा.")
      });
    }

    const reply = data?.candidates?.[0]?.content?.parts
      ?.map(part => part.text || "")
      .join("")
      .trim();

    return res.json({
      reply: reply || "AI कडून उत्तर मिळाले नाही."
    });

  } catch (error) {
    console.error("Chat error:", error);

    return res.status(500).json({
      reply: "Krushna AI मध्ये कनेक्शनची समस्या आली."
    });
  }
});

app.post("/code", (req, res) => {
  const command = req.body?.command;

  if (typeof command !== "string" || !command.trim()) {
    return res.status(400).json({
      success: false,
      message: "Code command द्या."
    });
  }

  res.json({
    success: true,
    message: "Krushna AI ला code command मिळाली.",
    command: command.trim()
  });
});

app.listen(PORT, () => {
  console.log("Krushna AI Server started on port " + PORT);
});
