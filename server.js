const express = require("express");

const app = express();

app.use(express.json());

app.get("/", (req, res) => {
  res.send("Krushna AI Server चालू आहे!");
});

app.post("/chat", async (req, res) => {
  try {
    const message = req.body.message || "";

    if (!message.trim()) {
      return res.json({
        reply: "कृपया प्रश्न विचारा."
      });
    }

    // आत्ता सुरक्षित test reply
    res.json({
      reply: "Krushna AI ला तुमचा प्रश्न मिळाला: " + message
    });

  } catch (error) {
    console.error(error);

    res.status(500).json({
      reply: "Server मध्ये error आला."
    });
  }
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log("Krushna AI Server started on port " + PORT);
});
